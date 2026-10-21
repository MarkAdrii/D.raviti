package com.markadrii.draviti.client.fx;

import com.markadrii.draviti.client.ClientVisuals;
import com.markadrii.draviti.client.anim.GravityAnimations;
import com.markadrii.draviti.entity.GravitySphereEntity;
import com.markadrii.draviti.gravity.Fx;
import com.markadrii.draviti.gravity.Limb;
import com.markadrii.draviti.network.FxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Efectos visuales de Satan Gravity (solo cliente). Morado y negro: brillos de Lodestone (aditivos) para la energía
 * y polvo/tinta de vanilla para lo oscuro, que en aditivo no se vería.
 */
public final class GravityFx {
    // Paleta (r, g, b de 0 a 1). Cambia con la forma: turquesa pálido (normal y Overdrive, como el Ether Gear)
    // o morado (Overdrive completo). "PURPLE" es el color principal de la paleta que toque.
    static float[] PURPLE, DEEP, LIGHT, MAGENTA, CYAN;
    static final float[] WHITE = {1.00F, 1.00F, 1.00F};
    static final float[] GOLD = {1.00F, 0.80F, 0.30F};
    private static DustParticleOptions DUST_VOID, DUST_DEEP, DUST_PURPLE;

    private static final float[][] TEAL = {{0.45F, 0.90F, 0.78F}, {0.08F, 0.40F, 0.38F}, {0.84F, 1.00F, 0.95F}, {0.55F, 1.00F, 0.85F}, {0.75F, 1.00F, 0.95F}};
    private static final float[][] VIOLET = {{0.64F, 0.36F, 1.00F}, {0.32F, 0.06F, 0.62F}, {0.90F, 0.80F, 1.00F}, {0.90F, 0.30F, 1.00F}, {0.70F, 0.85F, 1.00F}};
    private static final DustParticleOptions[] DUST_TEAL = {
            new DustParticleOptions(new Vector3f(0.01F, 0.07F, 0.07F), 2.2F),
            new DustParticleOptions(new Vector3f(0.06F, 0.36F, 0.33F), 1.6F),
            new DustParticleOptions(new Vector3f(0.45F, 0.90F, 0.78F), 1.2F)};
    private static final DustParticleOptions[] DUST_VIOLET = {
            new DustParticleOptions(new Vector3f(0.05F, 0.0F, 0.09F), 2.2F),
            new DustParticleOptions(new Vector3f(0.28F, 0.05F, 0.50F), 1.6F),
            new DustParticleOptions(new Vector3f(0.62F, 0.35F, 1.0F), 1.2F)};

    static { theme(false); }

    /** Elige la paleta: morada para el Overdrive completo, turquesa para lo demás. */
    static void theme(boolean violet) {
        float[][] t = violet ? VIOLET : TEAL;
        PURPLE = t[0]; DEEP = t[1]; LIGHT = t[2]; MAGENTA = t[3]; CYAN = t[4];
        DustParticleOptions[] d = violet ? DUST_VIOLET : DUST_TEAL;
        DUST_VOID = d[0]; DUST_DEEP = d[1]; DUST_PURPLE = d[2];
    }

    /** Efectos que duran varios ticks (espiral, campo, objetivos pesados/ingrávidos, implosión). */
    private record Ongoing(int type, int entityId, Vec3 pos, float a, int duration, int[] age) {}

    private static final List<Ongoing> ONGOING = new ArrayList<>();

    private GravityFx() {}

    // =====================================================================================
    // Entrada desde el servidor
    // =====================================================================================

    public static void play(FxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity src = p.entityId() >= 0 ? level.getEntity(p.entityId()) : null;
        Vec3 pos = p.pos();
        theme(p.tier() >= 2);
        // Campo Gravitatorio: la esfera la dibuja FieldDomes (con o sin Photon)
        if (p.type() == Fx.FIELD)
            com.markadrii.draviti.client.render.FieldDomes.update(p.entityId(), pos, p.a(), (int) p.b(), p.tier() >= 2);
        if (PhotonBridge.available() && p.type() > Fx.GLOW && photon(p, src, pos)) return;
        switch (p.type()) {
            case Fx.ANIM -> {
                if (src instanceof AbstractClientPlayer pl && ClientVisuals.playerAnimator()) {
                    int i = (int) p.a();
                    if (i >= 0 && i < Fx.ANIMATIONS.length) GravityAnimations.play(pl, Fx.ANIMATIONS[i]);
                }
            }
            case Fx.SHAKE -> {
                if (mc.player != null) {
                    double d = mc.player.position().distanceTo(pos);
                    float k = (float) Math.max(0, 1 - d / 40.0);
                    CameraShake.add(p.a() * k, (int) p.b());
                }
            }
            case Fx.GLOW -> {
                int i = (int) p.a();
                if (i >= 0 && i < Limb.values().length) {
                    Limb limb = Limb.values()[i];
                    ClientVisuals.glow(p.entityId(), limb, (int) p.b());
                    if (PhotonBridge.available() && src != null) {
                        int t = Math.max(6, (int) p.b());
                        String v = p.tier() >= 2 ? "_p" : "";
                        if (limb == Limb.RIGHT_ARM || limb == Limb.BOTH_ARMS) PhotonBridge.on(src, "limb_glow_r" + v, 1, true, t);
                        if (limb == Limb.LEFT_ARM || limb == Limb.BOTH_ARMS) PhotonBridge.on(src, "limb_glow_l" + v, 1, true, t);
                        if (limb == Limb.RIGHT_LEG) PhotonBridge.on(src, "limb_glow_leg" + v, 1, true, t);
                    }
                }
            }
            case Fx.OVERDRIVE_CHARGE -> overdriveCharge(level, src != null ? src.position() : pos, p.a() > 0);
            case Fx.SONIC_BOOM -> sonicBoom(level, pos, p.dir());
            case Fx.DASH -> dash(level, pos, p.dir());
            case Fx.FIST_CHARGE -> fistCharge(level, pos, p.a());
            case Fx.FIST_WAVE -> fistWave(level, pos, p.dir(), p.a());
            case Fx.SHOCKWAVE -> shockwave(level, pos, p.a());
            case Fx.SPIRAL -> ONGOING.add(new Ongoing(Fx.SPIRAL, p.entityId(), pos, p.a(), (int) p.b(), new int[1]));
            case Fx.CRUSH_COLUMN -> crushColumn(level, pos, p.a());
            case Fx.BARRAGE_HIT -> barrageHit(level, pos, p.dir(), p.a() > 0);
            case Fx.SPIN_RING -> spinRing(level, pos, p.a());
            case Fx.LAUNCH -> launch(level, pos, p.a() > 0);
            case Fx.IMPACT -> impact(level, pos, p.a(), (int) p.b());
            case Fx.FIELD -> { if (p.b() < 2) field(level, pos, p.a(), p.b() > 0); }
            case Fx.DEFLECT -> deflect(level, pos, p.a());
            case Fx.HEAVY -> ONGOING.add(new Ongoing(Fx.HEAVY, p.entityId(), pos, p.a(), Math.min(200, (int) p.b()), new int[1]));
            case Fx.WEIGHTLESS -> ONGOING.add(new Ongoing(Fx.WEIGHTLESS, p.entityId(), pos, p.a(), Math.min(200, (int) p.b()), new int[1]));
            case Fx.SPHERE_BURST -> sphereBurst(level, pos, p.a(), p.b() > 0);
            case Fx.BLACK_SKY_BURST -> ONGOING.add(new Ongoing(Fx.BLACK_SKY_BURST, -1, pos, p.a(), 20, new int[1]));
            case Fx.OVERDRIVE -> overdrive(level, pos, p.a() > 0);
            case Fx.OVERDRIVE_OFF -> burst(level, pos.add(0, 1, 0), LodestoneParticleRegistry.WISP_PARTICLE, LIGHT, PURPLE, 0.3F, 25, 0.06, 0.6, 30);
            case Fx.ABSORB -> absorb(level, src, pos);
            default -> {}
        }
    }

    /** Mismos efectos hechos con Photon. true si lo ha lanzado. */
    private static boolean photon(FxPacket p, Entity src, Vec3 pos) {
        float a = p.a();
        // Variante morada en el Overdrive completo; los efectos de tamaño fijo crecen con la forma
        String v = p.tier() >= 2 ? "_p" : "";
        float k = p.tier() >= 2 ? 1.5F : p.tier() == 1 ? 1.25F : 1F;
        switch (p.type()) {
            case Fx.FIST_CHARGE -> PhotonBridge.at("fist_charge" + v, pos, (0.6F + a * 0.5F) * k);
            case Fx.FIST_WAVE -> PhotonBridge.at("fist_wave" + v, pos, p.dir(), Math.max(0.6F, a / 5F));
            case Fx.SHOCKWAVE -> PhotonBridge.at("shockwave" + v, pos, Math.max(0.3F, a / 5F));
            case Fx.SPIRAL -> PhotonBridge.at("spiral" + v, pos, a / 14F);
            case Fx.CRUSH_COLUMN -> PhotonBridge.at("crush_column" + v, pos, Math.max(0.6F, a / 8F));
            case Fx.BARRAGE_HIT -> PhotonBridge.at("barrage_hit" + v, pos, (a > 0 ? 1F : 0.6F) * k);
            case Fx.SPIN_RING -> PhotonBridge.at("spin_ring" + v, pos, a / 4.5F);
            case Fx.LAUNCH -> PhotonBridge.at((a > 0 ? "launch" : "trail") + v, pos, k);
            case Fx.IMPACT -> {
                PhotonBridge.at("impact" + v, pos, Math.max(0.4F, a / 5F));
                if (p.b() == 1) PhotonBridge.at("impact_gold" + v, pos, Math.max(0.4F, a / 5F));
            }
            case Fx.FIELD -> { if (p.b() == 0) PhotonBridge.at("field" + v, pos, a / 10F); }
            case Fx.DEFLECT -> PhotonBridge.at("deflect" + v, pos, (0.6F + a * 0.3F) * k);
            case Fx.HEAVY -> PhotonBridge.on(src, "heavy" + v, Math.max(0.6F, a / 1.8F), false, (int) p.b());
            case Fx.WEIGHTLESS -> PhotonBridge.on(src, "weightless" + v, Math.max(0.6F, a / 1.8F), false, (int) p.b());
            case Fx.SPHERE_BURST -> {
                PhotonBridge.at("sphere_burst" + v, pos, Math.max(0.4F, a / 3F));
                if (p.b() > 0) PhotonBridge.at("shockwave" + v, pos, a / 4F);
            }
            case Fx.BLACK_SKY_BURST -> PhotonBridge.at("black_sky" + v, pos, a / 8F);
            case Fx.OVERDRIVE -> PhotonBridge.on(src, (a > 0 ? "overdrive_burst_full" : "overdrive_burst") + v, 1, false, 0);
            case Fx.OVERDRIVE_CHARGE -> PhotonBridge.on(src, (a > 0 ? "overdrive_charge_full" : "overdrive_charge") + v, 1, false, 0);
            case Fx.OVERDRIVE_OFF -> PhotonBridge.on(src, "overdrive_off" + v, 1, false, 0);
            case Fx.ABSORB -> PhotonBridge.on(src, "absorb" + v, 1, false, 0);
            case Fx.SONIC_BOOM -> PhotonBridge.at("sonic_boom" + v, pos, p.dir(), k);
            case Fx.DASH -> PhotonBridge.at("dash" + v, pos, k);
            default -> { return false; }
        }
        return true;
    }

    /** Transformación (sin Photon): el éter se concentra en el cuerpo antes del estallido. */
    private static void overdriveCharge(ClientLevel level, Vec3 pos, boolean full) {
        float[] a = full ? PURPLE : LIGHT, b = full ? DEEP : CYAN;
        for (int i = 0; i < 40; i++) {
            Vec3 off = randomDir(level.random).scale(2.6);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.3F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(a, b))
                    .setLifetime(14)
                    .setMotion(off.scale(-0.07))
                    .enableNoClip()
                    .spawn(level, pos.x + off.x, pos.y + 1 + off.y, pos.z + off.z);
        }
        ring(level, pos.add(0, 0.05, 0), LodestoneParticleRegistry.WISP_PARTICLE, a, b, 0.0, 30, 0.3F, 0.02, 1.6);
    }

    private static void sonicBoom(ClientLevel level, Vec3 pos, Vec3 dir) {
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
        level.addParticle(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 0, 0, 0);
        level.addParticle(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 0, 0, 0);
        RandomSource r = level.random;
        for (int i = 0; i < 40; i++) {
            Vec3 v = d.scale(-0.2).add(randomDir(r).scale(0.35));
            level.addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, v.x, v.y, v.z);
        }
        burst(level, pos, LodestoneParticleRegistry.SPARK_PARTICLE, WHITE, CYAN, 0.35F, 10, 0.4, 0.4, 25);
    }

    private static void dash(ClientLevel level, Vec3 pos, Vec3 dir) {
        Vec3 c = pos.add(0, 0.9, 0);
        burst(level, c, LodestoneParticleRegistry.WISP_PARTICLE, LIGHT, PURPLE, 0.3F, 10, 0.08, 0.4, 18);
        ring(level, c, LodestoneParticleRegistry.WISP_PARTICLE, CYAN, PURPLE, 1.0, 14, 0.2F, 0);
        for (int i = 0; i < 8; i++) level.addParticle(ParticleTypes.CLOUD, c.x, c.y - 0.5, c.z, -dir.x * 0.2, 0.02, -dir.z * 0.2);
    }

    /** Cada tick de cliente (lo llama GravityClient). */
    public static void tick(ClientLevel level) {
        Iterator<Ongoing> it = ONGOING.iterator();
        while (it.hasNext()) {
            Ongoing o = it.next();
            int age = o.age[0]++;
            if (age >= o.duration) { it.remove(); continue; }
            switch (o.type) {
                case Fx.SPIRAL -> spiralTick(level, o, age);
                case Fx.HEAVY -> heavyTick(level, o, age);
                case Fx.WEIGHTLESS -> weightlessTick(level, o, age);
                case Fx.BLACK_SKY_BURST -> implosionTick(level, o, age);
                default -> {}
            }
        }
        if (ONGOING.size() > 200) ONGOING.subList(0, ONGOING.size() - 200).clear();

        // Esferas de gravedad: chispas que giran a su alrededor y polvo oscuro
        for (Entity e : level.entitiesForRendering())
            if (e instanceof GravitySphereEntity s) {
                if (PhotonBridge.available()) {
                    float sc = s.kind() == GravitySphereEntity.BLACK_SKY ? 3F : s.kind() == GravitySphereEntity.METEOR ? 1.6F : 0.8F;
                    PhotonBridge.aura(s, s.tier() >= 2 ? "sphere_orbit_p" : "sphere_orbit", true, sc, false);
                } else { theme(s.tier() >= 2); sphereTick(level, s); }
            }
    }

    public static void clear() { ONGOING.clear(); PhotonBridge.clear(); }

    // =====================================================================================
    // Efectos
    // =====================================================================================

    private static void fistCharge(ClientLevel level, Vec3 hand, float size) {
        RandomSource r = level.random;
        int n = (int) (14 + size * 16);
        for (int i = 0; i < n; i++) {
            Vec3 off = randomDir(r).scale(0.6 + size * 0.8);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.12F + size * 0.08F, 0.02F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(LIGHT, PURPLE))
                    .setLifetime(8 + r.nextInt(4))
                    .setMotion(off.scale(-0.11))
                    .enableNoClip()
                    .spawn(level, hand.x + off.x, hand.y + off.y, hand.z + off.z);
        }
        burst(level, hand, LodestoneParticleRegistry.SPARKLE_PARTICLE, WHITE, PURPLE, 0.25F + size * 0.1F, 10, 0.01, 0.05, 4);
        for (int i = 0; i < 4; i++) level.addParticle(DUST_VOID, hand.x, hand.y, hand.z, 0, 0, 0);
    }

    /** Onda del Puño Gravitatorio: un cono de distorsión morada hacia delante. */
    private static void fistWave(ClientLevel level, Vec3 from, Vec3 dir, float range) {
        RandomSource r = level.random;
        Vec3 d = dir.normalize();
        level.addParticle(ParticleTypes.SONIC_BOOM, from.x + d.x * 1.2, from.y + d.y * 1.2, from.z + d.z * 1.2, 0, 0, 0);
        level.addParticle(ParticleTypes.EXPLOSION, from.x + d.x * 2, from.y + d.y * 2, from.z + d.z * 2, 0, 0, 0);
        Vec3 up = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = d.cross(up).normalize();
        Vec3 up2 = side.cross(d).normalize();
        // Anillos que se abren hacia delante
        for (int ring = 0; ring < 4; ring++) {
            double dist = 1 + ring * range / 4.0;
            double rad = 0.3 + ring * 0.45;
            int n = 18;
            for (int i = 0; i < n; i++) {
                double a = i / (double) n * Math.PI * 2;
                Vec3 o = side.scale(Math.cos(a) * rad).add(up2.scale(Math.sin(a) * rad));
                Vec3 p = from.add(d.scale(dist)).add(o);
                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                        .setScaleData(GenericParticleData.create(0.35F, 0F).build())
                        .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                        .setColorData(color(ring % 2 == 0 ? LIGHT : PURPLE, DEEP))
                        .setLifetime(10 + ring * 2)
                        .setMotion(d.scale(0.35).add(o.scale(0.12)))
                        .enableNoClip()
                        .spawn(level, p.x, p.y, p.z);
            }
        }
        for (int i = 0; i < 30; i++) {
            Vec3 v = d.add(randomDir(r).scale(0.35)).normalize().scale(0.5 + r.nextDouble() * 0.6);
            level.addParticle(DUST_DEEP, from.x, from.y, from.z, v.x, v.y, v.z);
        }
    }

    /** Onda expansiva en el suelo con trozos del bloque de abajo. */
    private static void shockwave(ClientLevel level, Vec3 pos, float radius) {
        RandomSource r = level.random;
        Vec3 p = pos.add(0, 0.1, 0);
        int n = (int) (24 + radius * 6);
        for (int i = 0; i < n; i++) {
            double a = i / (double) n * Math.PI * 2;
            double cos = Math.cos(a), sin = Math.sin(a);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.5F, 0.1F).build())
                    .setTransparencyData(GenericParticleData.create(0.85F, 0F).build())
                    .setColorData(color(LIGHT, PURPLE))
                    .setLifetime(14)
                    .setMotion(cos * radius * 0.075, 0.01, sin * radius * 0.075)
                    .enableNoClip()
                    .spawn(level, p.x + cos * 0.5, p.y, p.z + sin * 0.5);
            level.addParticle(ParticleTypes.CLOUD, p.x + cos * 0.6, p.y, p.z + sin * 0.6, cos * radius * 0.05, 0.02, sin * radius * 0.05);
        }
        BlockState ground = level.getBlockState(BlockPos.containing(pos.x, pos.y - 0.5, pos.z));
        if (!ground.isAir()) {
            BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            for (int i = 0; i < 40 + radius * 8; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * radius;
                level.addParticle(debris, p.x + Math.cos(a) * d, p.y + 0.2, p.z + Math.sin(a) * d,
                        Math.cos(a) * 0.3, 0.4 + r.nextDouble() * 0.6, Math.sin(a) * 0.3);
            }
        }
    }

    private static void spiralTick(ClientLevel level, Ongoing o, int age) {
        Entity src = o.entityId >= 0 ? level.getEntity(o.entityId) : null;
        Vec3 c = src != null ? src.position() : o.pos;
        float radius = o.a;
        // 3 brazos de espiral que giran y se cierran hacia el centro
        for (int arm = 0; arm < 3; arm++) {
            for (int k = 0; k < 6; k++) {
                double t = k / 6.0;
                double rr = radius * (1 - t) + 0.6;
                double a = arm * Math.PI * 2 / 3 + age * 0.25 + t * 4.5;
                double x = c.x + Math.cos(a) * rr, z = c.z + Math.sin(a) * rr;
                Vec3 in = new Vec3(c.x - x, 0, c.z - z).normalize();
                Vec3 tan = new Vec3(-in.z, 0, in.x);
                Vec3 m = in.scale(0.18).add(tan.scale(0.22));
                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                        .setScaleData(GenericParticleData.create(0.3F, 0.05F).build())
                        .setTransparencyData(GenericParticleData.create(0.75F, 0F).build())
                        .setColorData(color(arm == 0 ? LIGHT : PURPLE, DEEP))
                        .setLifetime(10)
                        .setMotion(m)
                        .enableNoClip()
                        .spawn(level, x, c.y + 0.15, z);
            }
        }
        if (age % 2 == 0)
            for (int i = 0; i < 6; i++) {
                double a = level.random.nextDouble() * Math.PI * 2, rr = radius * level.random.nextDouble();
                level.addParticle(ParticleTypes.REVERSE_PORTAL, c.x + Math.cos(a) * rr, c.y + 0.3, c.z + Math.sin(a) * rr,
                        -Math.cos(a) * 0.2, 0.02, -Math.sin(a) * 0.2);
            }
    }

    /** Columna de presión que cae sobre el objetivo. */
    private static void crushColumn(ClientLevel level, Vec3 pos, float height) {
        RandomSource r = level.random;
        for (int i = 0; i < 6; i++) {
            double y = pos.y + height * (i / 6.0);
            ring(level, new Vec3(pos.x, y, pos.z), LodestoneParticleRegistry.WISP_PARTICLE, LIGHT, PURPLE, 0.6 + i * 0.05, 14, 0.25F, -0.35);
        }
        for (int i = 0; i < 40; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * 1.2;
            level.addParticle(DUST_DEEP, pos.x + Math.cos(a) * d, pos.y + height * r.nextDouble(), pos.z + Math.sin(a) * d, 0, -0.8, 0);
        }
    }

    private static void barrageHit(ClientLevel level, Vec3 pos, Vec3 dir, boolean hit) {
        RandomSource r = level.random;
        burst(level, pos, LodestoneParticleRegistry.SPARK_PARTICLE, WHITE, PURPLE, hit ? 0.3F : 0.2F, 6, 0.25, 0.25, hit ? 8 : 4);
        if (hit) {
            level.addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z, dir.x * 0.4, 0.1, dir.z * 0.4);
            ring(level, pos, LodestoneParticleRegistry.WISP_PARTICLE, LIGHT, PURPLE, 0.6, 8, 0.25F, 0);
        } else if (r.nextBoolean()) {
            level.addParticle(DUST_PURPLE, pos.x, pos.y, pos.z, dir.x * 0.3, 0, dir.z * 0.3);
        }
    }

    /** Patada Molinete: anillo que gira a la altura de la cintura. */
    private static void spinRing(ClientLevel level, Vec3 c, float radius) {
        int n = 40;
        for (int i = 0; i < n; i++) {
            double a = i / (double) n * Math.PI * 2;
            double cos = Math.cos(a), sin = Math.sin(a);
            Vec3 tan = new Vec3(-sin, 0, cos);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.4F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.85F, 0F).build())
                    .setColorData(color(i % 2 == 0 ? LIGHT : PURPLE, DEEP))
                    .setLifetime(12)
                    .setMotion(tan.scale(0.45).add(cos * 0.18, 0, sin * 0.18))
                    .enableNoClip()
                    .spawn(level, c.x + cos * radius * 0.4, c.y, c.z + sin * radius * 0.4);
            if (i % 4 == 0) level.addParticle(ParticleTypes.SWEEP_ATTACK, c.x + cos * radius * 0.7, c.y, c.z + sin * radius * 0.7, 0, 0, 0);
        }
    }

    private static void launch(ClientLevel level, Vec3 pos, boolean strong) {
        RandomSource r = level.random;
        int n = strong ? 30 : 8;
        for (int i = 0; i < n; i++)
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.35F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(LIGHT, PURPLE))
                    .setLifetime(14 + r.nextInt(8))
                    .setRandomOffset(0.35)
                    .setMotion((r.nextDouble() - 0.5) * 0.05, strong ? 0.25 : 0.05, (r.nextDouble() - 0.5) * 0.05)
                    .enableNoClip()
                    .spawn(level, pos.x, pos.y + 0.2, pos.z);
        if (strong) for (int i = 0; i < 12; i++) level.addParticle(ParticleTypes.CLOUD, pos.x, pos.y + 0.1, pos.z,
                (r.nextDouble() - 0.5) * 0.4, 0.05, (r.nextDouble() - 0.5) * 0.4);
    }

    /** Gran impacto: anillos, cúpula de polvo, rocas que saltan y un pilar de gravedad. b = 1 Impacto, 2 Cielo Negro. */
    private static void impact(ClientLevel level, Vec3 pos, float radius, int kind) {
        RandomSource r = level.random;
        shockwave(level, pos, radius * 1.4F);
        shockwave(level, pos, radius * 0.7F);
        level.addParticle(kind == 2 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 0, 0, 0);
        for (int i = 0; i < radius * 3; i++)
            level.addParticle(ParticleTypes.EXPLOSION, pos.x + (r.nextDouble() - 0.5) * radius * 1.5, pos.y + 0.3 + r.nextDouble(),
                    pos.z + (r.nextDouble() - 0.5) * radius * 1.5, 0, 0, 0);
        float[] tint = kind == 1 ? GOLD : LIGHT;
        // Pilar de luz que cae
        for (int i = 0; i < 30; i++)
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.9F + radius * 0.08F, 0.1F).setEasing(Easing.QUAD_OUT).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(tint, PURPLE))
                    .setLifetime(12 + r.nextInt(6))
                    .setMotion(0, -0.6, 0)
                    .enableNoClip()
                    .spawn(level, pos.x + (r.nextDouble() - 0.5) * 0.8, pos.y + 6 + i * 0.4, pos.z + (r.nextDouble() - 0.5) * 0.8);
        // Polvo oscuro que se queda
        for (int i = 0; i < 50 + radius * 10; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * radius;
            level.addParticle(i % 3 == 0 ? DUST_VOID : DUST_DEEP, pos.x + Math.cos(a) * d, pos.y + 0.3 + r.nextDouble() * 1.5,
                    pos.z + Math.sin(a) * d, Math.cos(a) * 0.1, 0.05, Math.sin(a) * 0.1);
            if (i % 2 == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, pos.x + Math.cos(a) * d, pos.y + 0.3, pos.z + Math.sin(a) * d,
                    Math.cos(a) * 0.05, 0.06, Math.sin(a) * 0.05);
        }
        burst(level, pos.add(0, 0.5, 0), LodestoneParticleRegistry.SPARK_PARTICLE, WHITE, tint, 0.4F, 18, 0.6, 0.5, (int) (20 + radius * 5));
    }

    /** Campo Gravitatorio: cúpula y líneas que caen; refresh = solo las líneas. */
    /** Campo Gravitatorio (sin Photon): solo el borde en el suelo; la esfera la dibuja FieldDomes. */
    private static void field(ClientLevel level, Vec3 c, float radius, boolean refresh) {
        ring(level, c.add(0, 0.1, 0), LodestoneParticleRegistry.WISP_PARTICLE, PURPLE, DEEP, 0.0, (int) (radius * 5), 0.35F, 0, radius);
    }

    private static void deflect(ClientLevel level, Vec3 pos, float size) {
        burst(level, pos, LodestoneParticleRegistry.SPARKLE_PARTICLE, WHITE, CYAN, 0.25F + size * 0.1F, 10, 0.12, 0.1, (int) (4 + size * 6));
        ring(level, pos, LodestoneParticleRegistry.WISP_PARTICLE, CYAN, PURPLE, 0.5 + size * 0.3, 12, 0.2F, 0);
    }

    private static void heavyTick(ClientLevel level, Ongoing o, int age) {
        Entity e = level.getEntity(o.entityId);
        if (e == null || !e.isAlive()) { o.age[0] = o.duration; return; }
        if (age % 3 != 0) return;
        RandomSource r = level.random;
        double w = e.getBbWidth() * 0.8;
        for (int i = 0; i < 3; i++)
            level.addParticle(DUST_DEEP, e.getX() + (r.nextDouble() - 0.5) * w * 2, e.getY() + o.a + 1.5, e.getZ() + (r.nextDouble() - 0.5) * w * 2, 0, -1.0, 0);
        ring(level, e.position().add(0, 0.05, 0), LodestoneParticleRegistry.WISP_PARTICLE, PURPLE, DEEP, w + 0.2, 10, 0.18F, 0);
    }

    private static void weightlessTick(ClientLevel level, Ongoing o, int age) {
        Entity e = level.getEntity(o.entityId);
        if (e == null || !e.isAlive()) { o.age[0] = o.duration; return; }
        if (age % 4 != 0) return;
        double h = e.getBbHeight();
        double y = e.getY() + ((age / 4) % 4) / 4.0 * h;
        ring(level, new Vec3(e.getX(), y, e.getZ()), LodestoneParticleRegistry.WISP_PARTICLE, CYAN, LIGHT, e.getBbWidth() * 0.9 + 0.2, 12, 0.15F, 0.03);
        burst(level, e.position().add(0, h / 2, 0), LodestoneParticleRegistry.SPARKLE_PARTICLE, WHITE, CYAN, 0.15F, 15, 0.02, h * 0.5, 2);
    }

    private static void sphereBurst(ClientLevel level, Vec3 pos, float radius, boolean meteor) {
        RandomSource r = level.random;
        level.addParticle(meteor ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 0, 0, 0);
        burst(level, pos, LodestoneParticleRegistry.WISP_PARTICLE, LIGHT, PURPLE, 0.5F + radius * 0.15F, 18, 0.25 + radius * 0.08, 0.3, (int) (20 + radius * 10));
        burst(level, pos, LodestoneParticleRegistry.SPARK_PARTICLE, WHITE, MAGENTA, 0.3F, 12, 0.5, 0.2, 20);
        for (int i = 0; i < 30 + radius * 10; i++) {
            Vec3 v = randomDir(r).scale(0.2 + r.nextDouble() * 0.3 * radius * 0.4);
            level.addParticle(DUST_VOID, pos.x, pos.y, pos.z, v.x, v.y, v.z);
        }
        if (meteor) shockwave(level, pos, radius * 1.3F);
    }

    /** Cielo Negro: todo se precipita hacia el punto antes del golpe final. */
    private static void implosionTick(ClientLevel level, Ongoing o, int age) {
        RandomSource r = level.random;
        double R = o.a * 2;
        for (int i = 0; i < 18; i++) {
            Vec3 d = randomDir(r);
            Vec3 p = o.pos.add(d.scale(R * (0.6 + r.nextDouble() * 0.4)));
            Vec3 m = o.pos.subtract(p).scale(0.09);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.5F, 0.05F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(i % 2 == 0 ? PURPLE : LIGHT, DEEP))
                    .setLifetime(11)
                    .setMotion(m)
                    .enableNoClip()
                    .spawn(level, p.x, p.y, p.z);
            level.addParticle(DUST_VOID, p.x, p.y, p.z, m.x * 1.2, m.y * 1.2, m.z * 1.2);
        }
        if (age % 4 == 0) level.addParticle(ParticleTypes.SONIC_BOOM, o.pos.x, o.pos.y, o.pos.z, 0, 0, 0);
    }

    /** Transformación: columna de éter que sube en espiral (clara) o un remolino negro y morado (completa). */
    private static void overdrive(ClientLevel level, Vec3 pos, boolean full) {
        RandomSource r = level.random;
        float[] a = full ? PURPLE : LIGHT, b = full ? DEEP : CYAN;
        for (int i = 0; i < 70; i++) {
            double t = i / 70.0;
            double ang = t * Math.PI * 8;
            double rad = 1.2 - t * 0.5;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.45F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(a, b))
                    .setLifetime(25 + r.nextInt(10))
                    .setMotion(-Math.sin(ang) * 0.06, 0.12, Math.cos(ang) * 0.06)
                    .enableNoClip()
                    .spawn(level, pos.x + Math.cos(ang) * rad, pos.y + t * 2.5, pos.z + Math.sin(ang) * rad);
        }
        burst(level, pos.add(0, 1.2, 0), LodestoneParticleRegistry.STAR_PARTICLE, WHITE, a, 0.4F, 30, 0.12, 0.6, 25);
        shockwave(level, pos, full ? 8 : 5);
        if (full) {
            for (int i = 0; i < 90; i++) {
                Vec3 d = randomDir(r);
                level.addParticle(DUST_VOID, pos.x + d.x * 0.5, pos.y + 1 + d.y * 0.5, pos.z + d.z * 0.5, d.x * 0.5, d.y * 0.3 + 0.1, d.z * 0.5);
            }
            level.addParticle(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 1.2, pos.z, 0, 0, 0);
        } else {
            level.addParticle(ParticleTypes.FLASH, pos.x, pos.y + 1.2, pos.z, 0, 0, 0);
        }
    }

    /** Energía cinética absorbida: chispas doradas que entran en el cuerpo. */
    private static void absorb(ClientLevel level, Entity src, Vec3 pos) {
        Vec3 c = src != null ? src.position().add(0, src.getBbHeight() / 2, 0) : pos.add(0, 1, 0);
        RandomSource r = level.random;
        for (int i = 0; i < 12; i++) {
            Vec3 off = randomDir(r).scale(1.2);
            WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.2F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(1F, 0F).build())
                    .setColorData(color(GOLD, WHITE))
                    .setLifetime(10)
                    .setMotion(off.scale(-0.11))
                    .enableNoClip()
                    .spawn(level, c.x + off.x, c.y + off.y, c.z + off.z);
        }
    }

    /** Partículas de una esfera de gravedad (cañón, meteoro, Cielo Negro). */
    private static void sphereTick(ClientLevel level, GravitySphereEntity s) {
        RandomSource r = level.random;
        float size = s.size();
        Vec3 c = s.position().add(0, 0.3, 0);
        int n = Math.max(2, (int) (size * 4));
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double rad = size * (0.7 + r.nextDouble() * 0.6);
            Vec3 p = c.add(Math.cos(a) * rad, (r.nextDouble() - 0.5) * size, Math.sin(a) * rad);
            Vec3 tan = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(0.08 + size * 0.03);
            Vec3 in = c.subtract(p).scale(0.08);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.15F + size * 0.06F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(i % 2 == 0 ? LIGHT : PURPLE, DEEP))
                    .setLifetime(8)
                    .setMotion(tan.add(in))
                    .enableNoClip()
                    .spawn(level, p.x, p.y, p.z);
        }
        if (s.kind() == GravitySphereEntity.BLACK_SKY || size > 1) {
            for (int i = 0; i < size * 2; i++) {
                Vec3 d = randomDir(r).scale(size * 2.2);
                level.addParticle(ParticleTypes.REVERSE_PORTAL, c.x + d.x, c.y + d.y, c.z + d.z, -d.x * 0.05, -d.y * 0.05, -d.z * 0.05);
            }
        }
        if (!s.held() && s.tickCount % 2 == 0) level.addParticle(DUST_VOID, c.x, c.y, c.z, 0, 0, 0);
    }

    // =====================================================================================
    // Aura de cada jugador (Overdrive, agujero negro, Antigravedad, Ingravidez, brillos)
    // =====================================================================================

    public static void ambient(ClientLevel level, net.minecraft.world.entity.player.Player p, boolean od, boolean full,
                               boolean anti, boolean floating, boolean sonic, float armGlowR, float armGlowL, float legGlow) {
        theme(full);
        RandomSource r = level.random;
        if (sonic) {
            Vec3 at = p.position().add(0, 0.9, 0);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.6F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                    .setColorData(color(LIGHT, PURPLE))
                    .setLifetime(10)
                    .setRandomOffset(0.3)
                    .enableNoClip()
                    .spawn(level, at.x, at.y, at.z);
            level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, 0, 0, 0);
        }
        int t = p.tickCount;
        Vec3 c = p.position().add(0, p.getBbHeight() * 0.55, 0);
        if (od && t % 3 == 0) {
            // Éter claro que se desprende del cuerpo
            Vec3 o = new Vec3((r.nextDouble() - 0.5) * 0.7, r.nextDouble() * p.getBbHeight(), (r.nextDouble() - 0.5) * 0.7);
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setScaleData(GenericParticleData.create(0.18F, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.7F, 0F).build())
                    .setColorData(color(full ? PURPLE : LIGHT, full ? DEEP : CYAN))
                    .setLifetime(18)
                    .setMotion(0, 0.04, 0)
                    .enableNoClip()
                    .spawn(level, p.getX() + o.x, p.getY() + o.y, p.getZ() + o.z);
        }
        if (od || full) {
            // Aura de Dragon Ball: lenguas de llama que suben pegadas al cuerpo
            float big = full ? 1.25F : 1F;
            for (int i = 0; i < (full ? 4 : 3); i++) {
                double a = r.nextDouble() * Math.PI * 2, rad = (0.3 + r.nextDouble() * 0.2) * big;
                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                        .setScaleData(GenericParticleData.create(0.35F * big, 0.12F).build())
                        .setTransparencyData(GenericParticleData.create(0.75F, 0F).build())
                        .setColorData(color(LIGHT, PURPLE))
                        .setLifetime(8 + r.nextInt(4))
                        .setMotion(0, 0.18 + r.nextDouble() * 0.08, 0)
                        .enableNoClip()
                        .spawn(level, p.getX() + Math.cos(a) * rad, p.getY() + r.nextDouble() * 0.5, p.getZ() + Math.sin(a) * rad);
            }
        }
        if (full) {
            // Agujero negro pegado al cuerpo
            for (int i = 0; i < 2; i++) {
                double a = r.nextDouble() * Math.PI * 2, rad = 1.0 + r.nextDouble() * 0.7;
                Vec3 pt = c.add(Math.cos(a) * rad, (r.nextDouble() - 0.5) * 1.4, Math.sin(a) * rad);
                Vec3 in = c.subtract(pt).normalize();
                Vec3 tan = new Vec3(-in.z, 0, in.x);
                Vec3 m = in.scale(0.08).add(tan.scale(0.14));
                level.addParticle(i == 0 ? DUST_VOID : ParticleTypes.REVERSE_PORTAL, pt.x, pt.y, pt.z, m.x, m.y, m.z);
            }
        }
        if (anti && t % 4 == 0) {
            for (int i = 0; i < 3; i++) {
                Vec3 d = randomDir(r).scale(1.7);
                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                        .setScaleData(GenericParticleData.create(0.14F, 0F).build())
                        .setTransparencyData(GenericParticleData.create(0.6F, 0F).build())
                        .setColorData(color(CYAN, WHITE))
                        .setLifetime(12)
                        .enableNoClip()
                        .spawn(level, c.x + d.x, c.y + d.y, c.z + d.z);
            }
        }
        if (floating && t % 5 == 0)
            ring(level, p.position().add(0, -0.1, 0), LodestoneParticleRegistry.WISP_PARTICLE, CYAN, PURPLE, 0.7, 10, 0.15F, -0.02);
        float yaw = (float) Math.toRadians(p.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        if (armGlowR > 0) handGlow(level, p.position().add(0, 0.8, 0).add(right.scale(0.38)).add(fwd.scale(0.2)), armGlowR);
        if (armGlowL > 0) handGlow(level, p.position().add(0, 0.8, 0).add(right.scale(-0.38)).add(fwd.scale(0.2)), armGlowL);
        if (legGlow > 0) handGlow(level, p.position().add(0, 0.2, 0).add(right.scale(0.15)), legGlow);
    }

    private static void handGlow(ClientLevel level, Vec3 at, float k) {
        RandomSource r = level.random;
        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setScaleData(GenericParticleData.create(0.25F * k, 0F).build())
                .setTransparencyData(GenericParticleData.create(0.8F, 0F).build())
                .setColorData(color(LIGHT, PURPLE))
                .setLifetime(8)
                .setRandomOffset(0.15)
                .setMotion((r.nextDouble() - 0.5) * 0.02, 0.02, (r.nextDouble() - 0.5) * 0.02)
                .enableNoClip()
                .spawn(level, at.x, at.y, at.z);
    }

    // =====================================================================================
    // Ayudas
    // =====================================================================================

    static Vec3 randomDir(RandomSource r) {
        double u = r.nextDouble() * 2 - 1, th = r.nextDouble() * Math.PI * 2;
        double s = Math.sqrt(1 - u * u);
        return new Vec3(s * Math.cos(th), u, s * Math.sin(th));
    }

    static ColorParticleData color(float[] a, float[] b) {
        return ColorParticleData.create(a[0], a[1], a[2], b[0], b[1], b[2]).build();
    }

    static void burst(ClientLevel level, Vec3 pos, RegistryObject<LodestoneWorldParticleType> type,
                      float[] from, float[] to, float scale, int lifetime, double speed, double offset, int count) {
        WorldParticleBuilder.create(type)
                .setScaleData(GenericParticleData.create(scale, 0F).build())
                .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                .setColorData(color(from, to))
                .setSpinData(SpinParticleData.createRandomDirection(level.random, 0.2F).build())
                .setLifetime(lifetime)
                .setRandomMotion(speed)
                .setRandomOffset(offset)
                .enableNoClip()
                .repeat(level, pos.x, pos.y, pos.z, count);
    }

    /** Anillo horizontal que se abre desde el centro (o fijo en el radio start si speed = 0). */
    static void ring(ClientLevel level, Vec3 center, RegistryObject<LodestoneWorldParticleType> type,
                     float[] from, float[] to, double radius, int count, float scale, double vy) {
        ring(level, center, type, from, to, radius, count, scale, vy, 0.3);
    }

    static void ring(ClientLevel level, Vec3 center, RegistryObject<LodestoneWorldParticleType> type,
                     float[] from, float[] to, double radius, int count, float scale, double vy, double start) {
        for (int i = 0; i < count; i++) {
            double a = i / (double) count * Math.PI * 2;
            double cos = Math.cos(a), sin = Math.sin(a);
            WorldParticleBuilder.create(type)
                    .setScaleData(GenericParticleData.create(scale, 0F).build())
                    .setTransparencyData(GenericParticleData.create(0.9F, 0F).build())
                    .setColorData(color(from, to))
                    .setLifetime(16)
                    .setMotion(cos * radius * 0.08, vy, sin * radius * 0.08)
                    .enableNoClip()
                    .spawn(level, center.x + cos * start, center.y, center.z + sin * start);
        }
    }

    static float lerp(float a, float b, float t) { return Mth.lerp(t, a, b); }

    @SuppressWarnings("unused")
    private static void vanilla(ClientLevel level, ParticleOptions o, Vec3 p, Vec3 v) {
        level.addParticle(o, p.x, p.y, p.z, v.x, v.y, v.z);
    }
}
