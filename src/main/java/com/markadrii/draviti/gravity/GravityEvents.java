package com.markadrii.draviti.gravity;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.power.Power;
import com.markadrii.draviti.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Pasivas del Dios de la Gravedad (las "Other Abilities" de Shiki): fuerza y aguante inmensos, saltos más altos
 * ajustando su peso, absorber energía cinética, regenerar éter y los cambios de estadísticas del Overdrive.
 * Los valores están en la habilidad oculta "pasivas" (config/draviti-common.toml).
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID)
public final class GravityEvents {
    private GravityEvents() {}

    private record Mod4(Attribute attr, UUID id, String name, AttributeModifier.Operation op, double base, double od, double full) {}

    /** Valores para: sin Overdrive, Overdrive y Overdrive completo. */
    private static final Mod4[] MODS = {
            new Mod4(Attributes.MOVEMENT_SPEED, UUID.fromString("b8f0e0a1-3c1e-4a0f-9f1a-1d2b3c4d5e01"), "draviti_speed",
                    AttributeModifier.Operation.MULTIPLY_BASE, 0.10, 0.35, 0.55),
            new Mod4(Attributes.ATTACK_DAMAGE, UUID.fromString("b8f0e0a1-3c1e-4a0f-9f1a-1d2b3c4d5e02"), "draviti_strength",
                    AttributeModifier.Operation.ADDITION, 3, 8, 14),
            new Mod4(Attributes.ARMOR, UUID.fromString("b8f0e0a1-3c1e-4a0f-9f1a-1d2b3c4d5e03"), "draviti_armor",
                    AttributeModifier.Operation.ADDITION, 2, 8, 14),
            new Mod4(Attributes.ARMOR_TOUGHNESS, UUID.fromString("b8f0e0a1-3c1e-4a0f-9f1a-1d2b3c4d5e04"), "draviti_toughness",
                    AttributeModifier.Operation.ADDITION, 0, 4, 8),
            new Mod4(Attributes.KNOCKBACK_RESISTANCE, UUID.fromString("b8f0e0a1-3c1e-4a0f-9f1a-1d2b3c4d5e05"), "draviti_weight",
                    AttributeModifier.Operation.ADDITION, 0.2, 0.6, 1.0),
            new Mod4(Attributes.ATTACK_SPEED, UUID.fromString("b8f0e0a1-3c1e-4a0f-9f1a-1d2b3c4d5e06"), "draviti_reflexes",
                    AttributeModifier.Operation.MULTIPLY_BASE, 0.15, 0.4, 0.6),
    };

    static Ability passives() {
        Power p = Gravity.power();
        return p == null ? null : p.ability("pasivas").orElse(null);
    }

    static double passive(String stat, double def) {
        Ability a = passives();
        return a == null ? def : a.stat(stat);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        boolean has = Gravity.has(p);
        if (p.tickCount % 10 == 0) updateAttributes(p, has);
        if (!has) return;
        GravityMovement.tick(p);
        // Regenera éter (en Overdrive no: el Overdrive lo consume)
        if (!Gravity.overdrive(p) && p.tickCount % 2 == 0) {
            int e = Gravity.ether(p);
            if (e < Gravity.ETHER_MAX) Gravity.setEther(p, e + (int) Math.round(passive("ether_regen_per_second", 60) / 10.0));
        }
    }

    private static void updateAttributes(ServerPlayer p, boolean has) {
        int tier = !has ? -1 : Gravity.fullOverdrive(p) ? 2 : Gravity.overdrive(p) ? 1 : 0;
        for (Mod4 m : MODS) {
            AttributeInstance inst = p.getAttribute(m.attr);
            if (inst == null) continue;
            double want = tier < 0 ? 0 : tier == 0 ? m.base : tier == 1 ? m.od : m.full;
            AttributeModifier cur = inst.getModifier(m.id);
            if (cur != null && cur.getAmount() == want) continue;
            if (cur != null) inst.removeModifier(m.id);
            if (want != 0) inst.addTransientModifier(new AttributeModifier(m.id, m.name, want, m.op));
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        // Golpes cuerpo a cuerpo del usuario: fuerza inmensa (lanza al objetivo) y Antigravedad al tocar
        if (event.getSource().getDirectEntity() instanceof ServerPlayer p && event.getSource().getEntity() == p
                && Gravity.has(p) && !Gravity.ABILITY_DAMAGE[0]) {
            LivingEntity target = event.getEntity();
            if (Gravity.active(p, "antigravedad")) {
                GravityAbilities.repel(p, target, 1.8);
            } else {
                Vec3 look = p.getLookAngle();
                double k = passive("punch_knockback", 0.8) * Gravity.mult(p) * Gravity.resist(target);
                Gravity.push(target, new Vec3(look.x * k, 0.25, look.z * k));
                if (Gravity.mult(p) > 1) Gravity.fx(p, Fx.BARRAGE_HIT, target.position().add(0, target.getBbHeight() * 0.6, 0), look, 1, 0);
            }
        }
        // Daño recibido: aguante y energía cinética que absorbe para Impacto Gravitatorio
        if (event.getEntity() instanceof ServerPlayer p && Gravity.has(p)) {
            float amount = event.getAmount();
            float reduction = (float) passive("damage_reduction", 0.2) * (Gravity.fullOverdrive(p) ? 2.5F : Gravity.overdrive(p) ? 1.8F : 1F);
            event.setAmount(amount * Math.max(0.1F, 1 - reduction));
            int gain = (int) (amount * passive("kinetic_per_damage", 25));
            if (gain > 0) {
                int before = Gravity.kinetic(p);
                Gravity.setKinetic(p, before + gain);
                if (before < Gravity.KINETIC_MAX) Gravity.fx(p, Fx.ABSORB, p.position(), gain, 0);
            }
        }
    }

    /** Matar recupera éter (más con jefes y jugadores). */
    @SubscribeEvent
    public static void kill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !Gravity.has(p) || event.getEntity() == p) return;
        LivingEntity dead = event.getEntity();
        double gain;
        if (dead instanceof Player) gain = passive("ether_per_player_kill", 200);
        else if (dead.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES) || dead.getMaxHealth() >= 150)
            gain = passive("ether_per_boss_kill", 400);
        else gain = passive("ether_per_kill", 60);
        int before = Gravity.ether(p);
        if (before >= Gravity.ETHER_MAX) return;
        Gravity.setEther(p, before + (int) gain);
        Gravity.fx(p, Fx.ABSORB, dead.position(), (float) gain, 0);
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player p) || !Gravity.has(p)) return;
        if (p instanceof ServerPlayer sp) {
            Integer until = GravityAbilities.SAFE_FALL.get(sp.getUUID());
            if (until != null && until >= sp.tickCount) {
                // La caída de una técnica también carga energía cinética
                Gravity.setKinetic(sp, Gravity.kinetic(sp) + (int) (event.getDistance() * passive("kinetic_per_block", 20) * 0.5));
                event.setCanceled(true);
                return;
            }
        }
        if (Gravity.active(p, "ingravidez") || Gravity.active(p, "vuelo_sonico")) { event.setCanceled(true); return; }
        float d = event.getDistance();
        if (p instanceof ServerPlayer sp && d > 3)
            Gravity.setKinetic(sp, Gravity.kinetic(sp) + (int) (d * passive("kinetic_per_block", 20)));
        // Ajusta su peso al caer
        float safe = (float) passive("safe_fall_blocks", 8) * (Gravity.overdrive(p) ? 2 : 1);
        event.setDistance(Math.max(0, d - safe));
    }

    /** Acrobacias: salta más ajustando su propio peso. Pesado: no se puede saltar. Corre en los dos lados. */
    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        LivingEntity e = event.getEntity();
        if (e.hasEffect(ModEffects.HEAVY.get())) {
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x, Math.min(0, v.y), v.z);
            return;
        }
        if (e instanceof Player p && Gravity.has(p) && !p.isShiftKeyDown()) {
            double bonus = passive("jump_bonus", 0.18) * (Gravity.fullOverdrive(p) ? 2.2 : Gravity.overdrive(p) ? 1.6 : 1);
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, v.y + bonus, v.z);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) Tasks.tick();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        GravityAbilities.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        Tasks.clear();
    }
}
