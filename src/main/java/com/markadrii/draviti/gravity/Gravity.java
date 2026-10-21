package com.markadrii.draviti.gravity;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.config.CommonConfig;
import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.network.FxPacket;
import com.markadrii.draviti.power.Power;
import com.markadrii.draviti.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Utilidades del poder de gravedad (servidor): éter, objetivos, empujes, daño, cráteres y efectos. */
public final class Gravity {
    public static final ResourceLocation POWER_ID = Draviti.id("gravity");
    public static final String ETHER = "ether";
    public static final String KINETIC = "kinetic";
    public static final int ETHER_MAX = 1000;
    public static final int KINETIC_MAX = 1000;

    private Gravity() {}

    public static Power power() { return Powers.get(POWER_ID); }

    public static PowerData data(Player p) { return PowerCapability.get(p).orElse(null); }

    public static boolean has(Player p) {
        PowerData d = data(p);
        return d != null && d.hasPower(POWER_ID);
    }

    /** ¿Está activa esta habilidad del poder? En el cliente vale para cualquier jugador (ClientVisuals). */
    /** Lo pone el cliente al arrancar: habilidades activas de cualquier jugador (las manda el servidor). */
    public static java.util.function.BiPredicate<Player, String> clientActive = (p, key) -> false;

    public static boolean active(Player p, String ability) {
        if (p.level().isClientSide) return clientActive.test(p, POWER_ID + "/" + ability);
        PowerData d = data(p);
        Power pw = power();
        if (d == null || pw == null || !d.hasPower(POWER_ID)) return false;
        return pw.ability(ability).map(a -> d.state(pw, a).enabled).orElse(false);
    }

    public static boolean overdrive(Player p) { return active(p, "overdrive"); }
    public static boolean fullOverdrive(Player p) { return active(p, "overdrive_completo"); }

    /** Multiplicador de fuerza: normal 1, Overdrive 1.5, Overdrive completo 2. */
    public static float mult(Player p) {
        if (fullOverdrive(p)) return 2.0F;
        if (overdrive(p)) return 1.5F;
        return 1.0F;
    }

    // ------------------------------------------------------------------ energía

    public static int ether(Player p) {
        PowerData d = data(p);
        return d == null || power() == null ? 0 : d.energy(power(), ETHER);
    }

    public static void setEther(Player p, int v) {
        PowerData d = data(p);
        if (d != null && power() != null) d.setEnergy(power(), ETHER, Mth.clamp(v, 0, ETHER_MAX));
    }

    public static int kinetic(Player p) {
        PowerData d = data(p);
        return d == null || power() == null ? 0 : d.energy(power(), KINETIC);
    }

    public static void setKinetic(Player p, int v) {
        PowerData d = data(p);
        if (d != null && power() != null) d.setEnergy(power(), KINETIC, Mth.clamp(v, 0, KINETIC_MAX));
    }

    /** Gasta éter; si no hay suficiente avisa y devuelve false. */
    public static boolean spend(ServerPlayer p, double cost) {
        int c = (int) Math.round(cost);
        if (c <= 0) return true;
        int e = ether(p);
        if (e < c) {
            p.displayClientMessage(Component.translatable("message.draviti.no_ether", c, e), true);
            return false;
        }
        setEther(p, e - c);
        return true;
    }

    /** Hay éter suficiente (sin gastarlo). */
    public static boolean hasEther(ServerPlayer p, double cost) {
        int c = (int) Math.round(cost);
        if (ether(p) >= c) return true;
        p.displayClientMessage(Component.translatable("message.draviti.no_ether", c, ether(p)), true);
        return false;
    }

    // ------------------------------------------------------------------ objetivos

    /** ¿Puede afectar a esta entidad? (no a sí mismo, ni a sus mascotas, ni a aliados; jugadores según la config). */
    public static boolean canAffect(Player attacker, Entity target) {
        if (target == attacker || !target.isAlive() || target.isSpectator()) return false;
        if (target instanceof ArmorStand stand && stand.isMarker()) return false;
        if (target instanceof OwnableEntity own && attacker.getUUID().equals(own.getOwnerUUID())) return false;
        if (attacker.isAlliedTo(target)) return false;
        if (target instanceof Player tp) {
            if (tp.isCreative() || !CommonConfig.hurtPlayers()) return false;
            return attacker.canHarmPlayer(tp);
        }
        return true;
    }

    public static boolean canHit(Player attacker, Entity target) {
        return canAffect(attacker, target) && target instanceof LivingEntity;
    }

    public static List<LivingEntity> around(Player p, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        return p.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> canHit(p, e) && e.position().add(0, e.getBbHeight() / 2, 0).distanceToSqr(center) <= radius * radius);
    }

    /** Entidades delante del jugador: dentro de range y de un cono de halfAngle grados. */
    public static List<LivingEntity> cone(Player p, double range, double halfAngle) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngle));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : around(p, eye, range + 1)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double len = to.length();
            if (len > range + e.getBbWidth()) continue;
            if (len < 1.2 || to.normalize().dot(look) >= cos) out.add(e);
        }
        return out;
    }

    /** Entidad a la que mira el jugador (hasta range bloques, sin atravesar paredes). */
    public static LivingEntity lookTarget(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB box = p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.5);
        Predicate<Entity> ok = e -> canHit(p, e);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box, e -> ok.test(e) && e.isPickable());
        if (hit != null && hit.getEntity() instanceof LivingEntity le) return le;
        // Un poco de ayuda para apuntar: la más cercana a la línea de mira
        LivingEntity best = null;
        double bestDot = Math.cos(Math.toRadians(6));
        for (LivingEntity e : around(p, eye, range)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() > range) continue;
            double dot = to.normalize().dot(p.getLookAngle());
            if (dot > bestDot && p.hasLineOfSight(e)) { bestDot = dot; best = e; }
        }
        return best;
    }

    /** Punto al que mira el jugador (bloque o a range bloques). */
    public static Vec3 lookPoint(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    }

    /** Suelo debajo de un punto (hasta depth bloques); si no hay, el propio punto. */
    public static Vec3 ground(ServerLevel level, Vec3 from, int depth) {
        BlockHitResult hit = level.clip(new ClipContext(from.add(0, 0.5, 0), from.subtract(0, depth, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return hit.getType() == HitResult.Type.MISS ? from : hit.getLocation();
    }

    // ------------------------------------------------------------------ física

    /** Cambia la velocidad de una entidad y se la manda al cliente (los jugadores mueven su propio cuerpo). */
    public static void setMotion(Entity e, Vec3 v) {
        e.setDeltaMovement(v);
        e.hasImpulse = true;
        e.hurtMarked = true;
    }

    public static void push(Entity e, Vec3 v) { setMotion(e, e.getDeltaMovement().add(v)); }

    /** Empuja hacia fuera desde center con fuerza strength (y un poco hacia arriba). */
    public static void launchFrom(Entity e, Vec3 center, double strength, double up) {
        Vec3 d = e.position().subtract(center);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1.0E-4) d = new Vec3(e.getRandom().nextDouble() - 0.5, 0, e.getRandom().nextDouble() - 0.5);
        d = d.normalize().scale(strength * resist(e));
        setMotion(e, new Vec3(d.x, up * resist(e), d.z));
    }

    /** Menos empuje a los que resisten el retroceso (jefes...). */
    public static double resist(Entity e) {
        if (e instanceof LivingEntity le)
            return 1.0 - Math.min(0.7, le.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.7);
        return 1.0;
    }

    /** true mientras hace daño una técnica (para que no se sumen las pasivas de los golpes normales). */
    public static final boolean[] ABILITY_DAMAGE = {false};

    /** Daño de una técnica (se multiplica por el Overdrive). */
    public static void hurt(ServerPlayer p, LivingEntity target, float amount) {
        hurtRaw(p, target, amount * mult(p));
    }

    /** Daño exacto (ya calculado). */
    public static void hurtRaw(ServerPlayer p, LivingEntity target, float amount) {
        target.invulnerableTime = 0;
        ABILITY_DAMAGE[0] = true;
        try {
            target.hurt(p.damageSources().playerAttack(p), amount);
        } finally {
            ABILITY_DAMAGE[0] = false;
        }
        target.setLastHurtByPlayer(p);
    }

    // ------------------------------------------------------------------ bloques

    /**
     * Cráter: rompe bloques en una media esfera aplastada bajo center (sin soltar objetos para no llenar el suelo).
     * Nunca rompe bloques con inventario (cofres...), ni irrompibles, y pregunta a las protecciones (BreakEvent).
     */
    public static void crater(ServerPlayer p, Vec3 center, double radius, double depthFactor) {
        if (!CommonConfig.breakBlocks() || radius < 0.5) return;
        ServerLevel level = p.serverLevel();
        float max = CommonConfig.maxHardness();
        int r = (int) Math.ceil(radius);
        BlockPos c = BlockPos.containing(center);
        int broken = 0;
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
                for (int y = -r; y <= 1; y++) {
                    double dy = y < 0 ? y / Math.max(0.2, depthFactor) : y * 2.0;
                    double dist = Math.sqrt(x * x + z * z + dy * dy);
                    if (dist > radius) continue;
                    // Borde irregular
                    if (dist > radius - 1 && p.getRandom().nextFloat() < 0.4F) continue;
                    BlockPos pos = c.offset(x, y, z);
                    if (breakBlock(p, level, pos, max) && ++broken > 600) return;
                }
    }

    public static boolean breakBlock(ServerPlayer p, ServerLevel level, BlockPos pos, float maxHardness) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || st.hasBlockEntity() || !st.getFluidState().isEmpty()) return false;
        float hard = st.getDestroySpeed(level, pos);
        if (hard < 0 || hard > maxHardness) return false;
        if (!level.mayInteract(p, pos)) return false;
        BlockEvent.BreakEvent ev = new BlockEvent.BreakEvent(level, pos, st, p);
        if (MinecraftForge.EVENT_BUS.post(ev)) return false;
        if (p.getRandom().nextInt(3) == 0)
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    6, 0.3, 0.3, 0.3, 0.35);
        level.destroyBlock(pos, false, p);
        return true;
    }

    // ------------------------------------------------------------------ efectos

    public static void fx(ServerPlayer p, int type, Vec3 pos, float a, float b) {
        FxPacket.send(p.serverLevel(), type, p, pos, a, b);
    }

    public static void fx(ServerPlayer p, int type, Vec3 pos, Vec3 dir, float a, float b) {
        FxPacket.send(p.serverLevel(), type, p, pos, dir, a, b);
    }

    /** Efecto pegado a otra entidad (Pesado, Ingrávido...). */
    public static void fxOn(ServerPlayer p, int type, Entity target, float a, float b) {
        FxPacket.send(p.serverLevel(), type, target, target.position(), a, b);
    }

    public static void anim(ServerPlayer p, String name) {
        FxPacket.send(p.serverLevel(), Fx.ANIM, p, p.position(), Fx.anim(name), 0);
    }

    public static void glow(ServerPlayer p, Limb limb, int ticks) {
        FxPacket.send(p.serverLevel(), Fx.GLOW, p, p.position(), limb.ordinal(), ticks);
    }

    /** Temblor para los que estén a menos de radius bloques. */
    public static void shake(ServerPlayer p, Vec3 pos, float strength, int ticks) {
        FxPacket.send(p.serverLevel(), Fx.SHAKE, p, pos, strength, ticks);
    }

    public static void sound(ServerPlayer p, Vec3 pos, SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, pos.x, pos.y, pos.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    /** Posición aproximada de la mano derecha (para efectos). */
    public static Vec3 hand(Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        return p.getEyePosition().add(look.scale(0.8)).add(right.scale(0.35)).subtract(0, 0.35, 0);
    }
}
