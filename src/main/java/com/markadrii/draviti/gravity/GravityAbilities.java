package com.markadrii.draviti.gravity;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.AbilityBehavior;
import com.markadrii.draviti.entity.GravitySphereEntity;
import com.markadrii.draviti.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Técnicas de Shiki Granbell (Edens Zero): Satan Gravity, Magimech (Makai-ryū) y Overdrive.
 * Todos los valores (daño, alcance, éter...) se pueden cambiar en config/draviti-common.toml.
 */
public final class GravityAbilities {
    private GravityAbilities() {}

    /** Jugadores que no reciben daño de caída hasta cierto tick (Cometa, Placaje, Impacto en el aire). */
    public static final Map<UUID, Integer> SAFE_FALL = new HashMap<>();
    private static final Map<UUID, Integer> FRENZY = new HashMap<>();
    /** Jugadores en vuelo sónico (Ingravidez + W dos veces en el aire). */
    public static final java.util.Set<UUID> SONIC = new java.util.HashSet<>();
    private static final Map<UUID, Vec3> FIELD_CENTER = new HashMap<>();
    private static final Map<UUID, GravitySphereEntity> CHARGING = new HashMap<>();
    private static final Map<UUID, Integer> CHARGE_TICKS = new HashMap<>();
    /** Entidades que Antigravedad acaba de lanzar (para no lanzarlas cada tick). */
    private static final Map<UUID, Integer> REPELLED = new HashMap<>();

    static double cost(Ability a) { return a.stat("ether_cost"); }

    static void safeFall(ServerPlayer p, int ticks) {
        SAFE_FALL.put(p.getUUID(), p.tickCount + ticks);
    }

    @FunctionalInterface
    interface Act { void run(ServerPlayer p, Ability a); }

    /** Técnica de un uso: gasta su éter y se hace. */
    static AbilityBehavior action(Act act) {
        return new AbilityBehavior() {
            @Override
            public boolean tryAction(ServerPlayer p, Ability a) {
                if (!Gravity.spend(p, cost(a))) return false;
                act.run(p, a);
                return true;
            }
        };
    }

    /** Técnica con objetivo: si no miras a nadie no gasta nada. */
    @FunctionalInterface
    interface TargetAct { void run(ServerPlayer p, Ability a, LivingEntity target); }

    static AbilityBehavior targeted(TargetAct act) {
        return new AbilityBehavior() {
            @Override
            public boolean tryAction(ServerPlayer p, Ability a) {
                LivingEntity target = Gravity.lookTarget(p, a.stat("range"));
                if (target == null) {
                    p.displayClientMessage(Component.translatable("message.draviti.no_target"), true);
                    return false;
                }
                if (!Gravity.spend(p, cost(a))) return false;
                act.run(p, a, target);
                return true;
            }
        };
    }

    static Vec3 flatLook(ServerPlayer p) {
        Vec3 l = p.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    /** Tamaño de las técnicas según la forma (Overdrive más grande, completo aún más). */
    static double sqrtMult(ServerPlayer p) { return Gravity.area(p); }

    // =====================================================================================
    // Satan Gravity
    // =====================================================================================

    /**
     * Ingravidez: cambia la dirección de la gravedad a su alrededor para flotar y pegarse a las paredes.
     * Salto = subir, agacharse = bajar; contra una pared se sube andando por ella (GravityClient).
     */
    public static final AbilityBehavior FLOAT = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.hasEther(p, cost(a)); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) {
            Gravity.spend(p, cost(a));
            if (!p.isCreative() && !p.isSpectator()) {
                // Sin esto el servidor echa al jugador por "volar"
                p.getAbilities().mayfly = true;
                p.onUpdateAbilities();
            }
            Gravity.fxOn(p, Fx.WEIGHTLESS, p, p.getBbHeight(), 30);
            Gravity.sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F);
            Gravity.push(p, new Vec3(0, 0.35, 0));
        }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            p.fallDistance = 0;
            if (t % 20 == 0) Gravity.setEther(p, Gravity.ether(p) - (int) a.stat("ether_per_second"));
        }

        @Override
        public boolean shouldStop(ServerPlayer p, Ability a) { return Gravity.ether(p) <= 0; }

        @Override
        public void onDeactivate(ServerPlayer p, Ability a) {
            GravityMovement.stopSonic(p);
            if (!p.isCreative() && !p.isSpectator()) {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
            safeFall(p, 60);
        }
    };

    /** Peso Infernal: aumenta el peso de todos los de alrededor sin tocarlos (los aplasta contra el suelo). */
    public static final AbilityBehavior HEAVY = new AbilityBehavior() {
        @Override
        public boolean tryAction(ServerPlayer p, Ability a) {
            var targets = Gravity.around(p, p.position().add(0, 1, 0), a.stat("radius") * sqrtMult(p));
            if (targets.isEmpty()) {
                p.displayClientMessage(Component.translatable("message.draviti.no_target"), true);
                return false;
            }
            if (!Gravity.spend(p, cost(a))) return false;
            Gravity.anim(p, "point_down");
            Gravity.glow(p, Limb.RIGHT_ARM, 14);
            int dur = a.statI("duration");
            for (LivingEntity e : targets) {
                e.addEffect(new MobEffectInstance(ModEffects.HEAVY.get(), dur, Gravity.fullOverdrive(p) ? 3 : 2));
                Gravity.setMotion(e, new Vec3(0, -1.2, 0));
                Gravity.hurt(p, e, a.statF("damage"));
                Gravity.fxOn(p, Fx.HEAVY, e, e.getBbHeight(), dur);
            }
            Gravity.fx(p, Fx.SHOCKWAVE, p.position(), (float) a.stat("radius"), 0);
            Gravity.sound(p, p.position(), SoundEvents.ANVIL_LAND, 1.2F, 0.5F);
            Gravity.sound(p, p.position(), SoundEvents.WARDEN_HEARTBEAT, 2F, 0.6F);
            return true;
        }
    };

    /** Ingravidez ajena: el objetivo (y lo que tenga cerca) pierde su peso y flota sin control. */
    public static final AbilityBehavior LIFT = targeted((p, a, target) -> {
        Gravity.anim(p, "point_down");
        Gravity.glow(p, Limb.RIGHT_ARM, 12);
        int dur = a.statI("duration");
        var list = Gravity.around(p, target.position().add(0, target.getBbHeight() / 2, 0), a.stat("radius"));
        if (!list.contains(target)) list.add(target);
        for (LivingEntity e : list) {
            e.addEffect(new MobEffectInstance(ModEffects.WEIGHTLESS.get(), dur, 0));
            Gravity.setMotion(e, new Vec3(0, 0.6 * Gravity.resist(e), 0));
            Gravity.fxOn(p, Fx.WEIGHTLESS, e, e.getBbHeight(), dur);
        }
        Gravity.sound(p, target.position(), SoundEvents.BEACON_POWER_SELECT, 1F, 0.7F);
        Gravity.sound(p, target.position(), SoundEvents.SHULKER_BULLET_HIT, 1F, 0.6F);
    });

    // =====================================================================================
    // Magimech (Makai-ryū)
    // =====================================================================================

    /** Puño Gravitatorio (Jūken): el brazo brilla y el puñetazo revienta el suelo y lo que haya delante. */
    public static final AbilityBehavior GRAVITY_FIST = action((p, a) -> {
        Gravity.anim(p, "gravity_punch");
        Gravity.glow(p, Limb.RIGHT_ARM, 16);
        Gravity.fx(p, Fx.FIST_CHARGE, Gravity.hand(p), 0.6F, 6);
        Gravity.sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.8F);
        Tasks.later(p, 5, () -> {
            double range = a.stat("range") * sqrtMult(p);
            Vec3 look = p.getLookAngle();
            for (LivingEntity e : Gravity.cone(p, range, 40)) {
                Gravity.hurt(p, e, a.statF("damage"));
                Vec3 k = look.scale(a.stat("knockback") * Gravity.mult(p) * Gravity.resist(e));
                Gravity.setMotion(e, new Vec3(k.x, Math.max(0.45, k.y + 0.45), k.z));
            }
            Vec3 front = p.position().add(flatLook(p).scale(2.5));
            Vec3 ground = Gravity.ground(p.serverLevel(), front.add(0, 1, 0), 4);
            double r = a.stat("crater") * sqrtMult(p);
            Gravity.crater(p, ground, r, 0.6);
            Gravity.fx(p, Fx.FIST_WAVE, Gravity.hand(p), look, (float) range, 0);
            Gravity.fx(p, Fx.SHOCKWAVE, ground, (float) (r + 1.5), 0);
            Gravity.shake(p, ground, 1.4F, 12);
            Gravity.sound(p, ground, SoundEvents.GENERIC_EXPLODE, 1.6F, 0.8F);
            Gravity.sound(p, ground, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.5F);
        });
    });

    /** Ráfaga de Puños (Jūken Ranbu): mantén pulsado; los puños brillan y lanzan una lluvia de golpes. */
    public static final AbilityBehavior FIST_FRENZY = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.hasEther(p, a.stat("ether_per_tick") * 10); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) {
            FRENZY.put(p.getUUID(), 0);
            Gravity.anim(p, "barrage");
        }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            FRENZY.put(p.getUUID(), t);
            Gravity.setEther(p, Gravity.ether(p) - (int) a.stat("ether_per_tick"));
            if (t % 4 == 0) Gravity.glow(p, Limb.BOTH_ARMS, 6);
            if (t % 2 != 0) return;
            Vec3 look = p.getLookAngle();
            for (LivingEntity e : Gravity.cone(p, a.stat("range"), 50)) {
                Gravity.hurt(p, e, a.statF("damage"));
                Vec3 k = look.scale(0.3 * Gravity.resist(e));
                Gravity.setMotion(e, new Vec3(k.x, 0.12, k.z));
                Gravity.fx(p, Fx.BARRAGE_HIT, e.position().add(0, e.getBbHeight() * 0.6, 0), look, 1, 0);
            }
            Gravity.fx(p, Fx.BARRAGE_HIT, Gravity.hand(p).add(look.scale(0.8)), look, 0, 0);
            Gravity.sound(p, p.position(), SoundEvents.PLAYER_ATTACK_STRONG, 0.7F, 0.8F + p.getRandom().nextFloat() * 0.6F);
        }

        @Override
        public boolean shouldStop(ServerPlayer p, Ability a) {
            Integer t = FRENZY.get(p.getUUID());
            return Gravity.ether(p) < a.stat("ether_per_tick") || (t != null && t >= a.statI("max_ticks"));
        }

        @Override
        public void onDeactivate(ServerPlayer p, Ability a) {
            Integer t = FRENZY.remove(p.getUUID());
            Gravity.anim(p, "gravity_punch");
            if (t == null || t < 6 || !p.isAlive()) return;
            // Golpe final que lo manda todo lejos
            Vec3 look = p.getLookAngle();
            for (LivingEntity e : Gravity.cone(p, a.stat("range") + 1, 50)) {
                Gravity.hurt(p, e, a.statF("final_damage"));
                Vec3 k = look.scale(a.stat("knockback") * Gravity.mult(p) * Gravity.resist(e));
                Gravity.setMotion(e, new Vec3(k.x, Math.max(0.5, k.y + 0.5), k.z));
            }
            Gravity.glow(p, Limb.RIGHT_ARM, 10);
            Gravity.fx(p, Fx.FIST_WAVE, Gravity.hand(p), look, (float) a.stat("range") + 2, 0);
            Gravity.shake(p, p.position(), 1F, 10);
            Gravity.sound(p, p.position(), SoundEvents.GENERIC_EXPLODE, 1F, 1.1F);
        }
    };

    /** Cañón Gravitatorio (Jūdan): forma una esfera de gravedad en el puño y la lanza; su peso empuja al objetivo. */
    public static final AbilityBehavior GRAVITY_CANNON = action((p, a) -> {
        Gravity.anim(p, "throw");
        Gravity.glow(p, Limb.RIGHT_ARM, 14);
        GravitySphereEntity s = GravitySphereEntity.create(p, GravitySphereEntity.CANNON, 0.25F);
        Gravity.sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.3F);
        Tasks.start(p, t -> {
            if (s.isRemoved()) return true;
            s.setSize(0.25F + t * 0.08F);
            if (t < 6) return false;
            s.setStats(a.statF("damage") * Gravity.mult(p), 1.5F, (float) (a.stat("knockback") * Gravity.mult(p)));
            s.launch(p.getLookAngle(), a.stat("speed"));
            Gravity.sound(p, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 0.5F, 1.8F);
            return true;
        });
    });

    /** Onda Gravitatoria (Jūryokuha): apoya la palma en el suelo y una espiral atrae a todos hacia él. */
    public static final AbilityBehavior GRAVITY_WAVE = action((p, a) -> {
        Gravity.anim(p, "palm_ground");
        Gravity.glow(p, Limb.RIGHT_ARM, 30);
        double r = a.stat("radius") * sqrtMult(p);
        int dur = a.statI("pull_ticks");
        Vec3 c = p.position();
        Gravity.fx(p, Fx.SPIRAL, c, (float) r, dur);
        Gravity.fx(p, Fx.SHOCKWAVE, c, 2.5F, 0);
        Gravity.sound(p, c, SoundEvents.WARDEN_SONIC_CHARGE, 1.2F, 1.4F);
        Tasks.start(p, t -> {
            Vec3 center = p.position();
            AABB box = new AABB(center, center).inflate(r);
            for (Entity e : p.level().getEntities(p, box, x -> x instanceof ItemEntity || x instanceof ExperienceOrb || Gravity.canAffect(p, x))) {
                Vec3 to = center.subtract(e.position());
                double d = to.length();
                if (d < 1.5 || d > r) continue;
                Vec3 in = to.normalize();
                Vec3 tangent = new Vec3(-in.z, 0, in.x);
                Vec3 v = in.scale(0.45).add(tangent.scale(0.3)).scale(Gravity.resist(e));
                Gravity.setMotion(e, new Vec3(v.x, Math.min(0.1, e.getDeltaMovement().y), v.z));
            }
            return t >= dur;
        });
    });

    /** Centro de Gravedad (Jūshin): sube la gravedad de un objetivo (sobre todo en el aire) y lo estrella contra el suelo. */
    public static final AbilityBehavior GRAVITY_CENTER = targeted((p, a, target) -> {
        Gravity.anim(p, "point_down");
        Gravity.glow(p, Limb.RIGHT_ARM, 14);
        Gravity.fx(p, Fx.CRUSH_COLUMN, target.position(), target.getBbHeight() + 6, 0);
        Gravity.hurt(p, target, a.statF("damage") * 0.4F);
        target.addEffect(new MobEffectInstance(ModEffects.HEAVY.get(), 50, 3));
        Gravity.setMotion(target, new Vec3(0, -3.5, 0));
        Gravity.sound(p, target.position(), SoundEvents.WARDEN_SONIC_BOOM, 1F, 0.7F);
        boolean airborne = !target.onGround();
        Tasks.start(p, t -> {
            if (!target.isAlive()) return true;
            if (t < 1 || (!target.onGround() && t < 40)) return false;
            Vec3 at = target.position();
            target.invulnerableTime = 0;
            Gravity.hurt(p, target, a.statF("damage") * (airborne ? 0.9F : 0.6F));
            Gravity.crater(p, at, a.stat("crater") * sqrtMult(p), 0.7);
            Gravity.fx(p, Fx.IMPACT, at, (float) a.stat("crater") + 1, 0);
            Gravity.shake(p, at, 1.5F, 14);
            Gravity.sound(p, at, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.7F);
            return true;
        });
    });

    /** Patada Molinete (Jūsen Kyaku): la pierna brilla y una patada giratoria en el aire barre todo alrededor. */
    public static final AbilityBehavior WINDMILL_KICK = action((p, a) -> {
        Gravity.anim(p, "spin_kick");
        Gravity.glow(p, Limb.RIGHT_LEG, 16);
        if (p.onGround()) Gravity.push(p, new Vec3(0, 0.55, 0));
        safeFall(p, 40);
        Gravity.sound(p, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        Tasks.later(p, 4, () -> {
            Vec3 c = p.position().add(0, 0.9, 0);
            double r = a.stat("radius") * sqrtMult(p);
            for (LivingEntity e : Gravity.around(p, c, r)) {
                Gravity.hurt(p, e, a.statF("damage"));
                Gravity.launchFrom(e, c, a.stat("knockback") * Gravity.mult(p), 0.55);
            }
            Gravity.fx(p, Fx.SPIN_RING, c, (float) r, 0);
            Gravity.shake(p, c, 0.8F, 8);
            Gravity.sound(p, c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.6F);
            Gravity.sound(p, c, SoundEvents.WARDEN_SONIC_BOOM, 0.4F, 1.5F);
        });
    });

    /** Cometa Gravitatorio (Jūsuisei): salta muy alto y cae con los pies cargados de gravedad, aplastando todo. */
    public static final AbilityBehavior GRAVITY_COMET = action((p, a) -> {
        Gravity.anim(p, "spin_kick");
        Gravity.glow(p, Limb.RIGHT_LEG, 30);
        Vec3 f = flatLook(p).scale(0.7);
        Gravity.setMotion(p, new Vec3(f.x, a.stat("jump"), f.z));
        safeFall(p, 140);
        Gravity.fx(p, Fx.LAUNCH, p.position(), 1, 0);
        Gravity.fx(p, Fx.SHOCKWAVE, p.position(), 2.5F, 0);
        Gravity.sound(p, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 1.6F);
        double startY = p.getY();
        Tasks.start(p, t -> {
            if (t == 12) {
                Gravity.anim(p, "dive_kick");
                Gravity.glow(p, Limb.RIGHT_LEG, 30);
                Vec3 l = flatLook(p).scale(0.6);
                Gravity.setMotion(p, new Vec3(l.x, -2.6, l.z));
            }
            if (t > 12 && t % 3 == 0) Gravity.fx(p, Fx.LAUNCH, p.position(), 0, 0);
            if (t < 13 || (!p.onGround() && t < 90)) return false;
            Vec3 at = p.position();
            double height = Math.max(0, startY + 4 - at.y) + 6;
            float extra = (float) Math.min(1.5, height / 20.0);
            double r = a.stat("radius") * sqrtMult(p) * (0.8 + extra * 0.3);
            for (LivingEntity e : Gravity.around(p, at, r)) {
                Gravity.hurt(p, e, a.statF("damage") * (1 + extra));
                Gravity.launchFrom(e, at, 1.3 * Gravity.mult(p), 0.8);
            }
            Gravity.crater(p, at.subtract(0, 0.5, 0), r * 0.8, 0.7);
            Gravity.fx(p, Fx.IMPACT, at, (float) r, 0);
            Gravity.shake(p, at, 2F, 20);
            Gravity.sound(p, at, SoundEvents.GENERIC_EXPLODE, 2.5F, 0.6F);
            Gravity.sound(p, at, SoundEvents.ANVIL_LAND, 1.2F, 0.4F);
            return true;
        });
    });

    /** Placaje Gravitatorio: gira en el aire y clava al objetivo contra el suelo con una patada hacia abajo. */
    public static final AbilityBehavior GRAVITY_TACKLE = targeted((p, a, target) -> {
        Gravity.anim(p, "spin_kick");
        Gravity.glow(p, Limb.RIGHT_LEG, 24);
        safeFall(p, 100);
        Gravity.fx(p, Fx.LAUNCH, p.position(), 1, 0);
        Gravity.sound(p, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);
        Tasks.start(p, t -> {
            if (!target.isAlive()) return true;
            Vec3 above = target.position().add(0, target.getBbHeight() + 1.2, 0);
            Vec3 to = above.subtract(p.position());
            if (to.length() > 2.5 && t < 16) {
                Gravity.setMotion(p, to.normalize().scale(Math.min(2.2, to.length() * 0.5 + 0.6)));
                return false;
            }
            Gravity.anim(p, "dive_kick");
            Gravity.hurt(p, target, a.statF("damage"));
            target.addEffect(new MobEffectInstance(ModEffects.HEAVY.get(), 30, 3));
            Gravity.setMotion(target, new Vec3(0, -3.5, 0));
            Gravity.setMotion(p, new Vec3(0, 0.25, 0));
            Vec3 ground = Gravity.ground(p.serverLevel(), target.position(), 8);
            Gravity.crater(p, ground, a.stat("crater") * sqrtMult(p), 0.8);
            Gravity.fx(p, Fx.IMPACT, ground, (float) a.stat("crater") + 1.5F, 0);
            Gravity.fx(p, Fx.CRUSH_COLUMN, ground, 4, 0);
            Gravity.shake(p, ground, 1.8F, 16);
            Gravity.sound(p, ground, SoundEvents.GENERIC_EXPLODE, 2F, 0.7F);
            return true;
        });
    });

    /**
     * Impacto Gravitatorio: absorbe la energía cinética de los golpes y caídas que recibe (barra dorada) y la convierte
     * en un puñetazo hacia abajo. Cuanta más energía, más grande.
     */
    public static final AbilityBehavior GRAVITY_IMPACT = new AbilityBehavior() {
        @Override
        public boolean tryAction(ServerPlayer p, Ability a) {
            int k = Gravity.kinetic(p);
            if (k < a.stat("min_kinetic")) {
                p.displayClientMessage(Component.translatable("message.draviti.no_kinetic", (int) a.stat("min_kinetic"), k), true);
                return false;
            }
            if (!Gravity.spend(p, cost(a))) return false;
            Gravity.setKinetic(p, 0);
            // 1000 de energía = tamaño normal máximo; con más capacidad puede pasar de ahí (hasta x4)
            float ratio = Math.min(4F, k / (float) Gravity.KINETIC_MAX);
            Gravity.glow(p, Limb.RIGHT_ARM, 30);
            Gravity.fx(p, Fx.FIST_CHARGE, Gravity.hand(p), 1.2F, 10);
            if (!p.onGround()) {
                Gravity.anim(p, "dive_kick");
                safeFall(p, 100);
                Gravity.setMotion(p, new Vec3(0, -3, 0));
                Tasks.start(p, t -> {
                    if (!p.onGround() && t < 60) return false;
                    boom(p, a, ratio);
                    return true;
                });
            } else {
                Gravity.anim(p, "slam");
                Tasks.later(p, 6, () -> boom(p, a, ratio));
            }
            return true;
        }

        private void boom(ServerPlayer p, Ability a, float ratio) {
            Vec3 at = p.position();
            double r = (a.stat("min_radius") + ratio * a.stat("max_radius")) * sqrtMult(p);
            float dmg = (float) (a.stat("min_damage") + ratio * a.stat("max_damage"));
            for (LivingEntity e : Gravity.around(p, at, r + 1)) {
                Gravity.hurt(p, e, dmg);
                Gravity.launchFrom(e, at, 1.4 + ratio * 2.2, 0.9);
            }
            Gravity.crater(p, at.subtract(0, 0.5, 0), r, 0.8);
            Gravity.fx(p, Fx.IMPACT, at, (float) r, 1);
            Gravity.shake(p, at, 1.5F + ratio * 2.5F, 20 + (int) (ratio * 20));
            Gravity.sound(p, at, SoundEvents.GENERIC_EXPLODE, 3F, 0.5F);
            Gravity.sound(p, at, SoundEvents.WARDEN_SONIC_BOOM, 2F, 0.4F);
        }
    };

    /** Campo Gravitatorio (Jūryokuba): golpea el suelo y crea un campo que aplasta a todos y anula los ataques. */
    public static final AbilityBehavior GRAVITY_FIELD = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.spend(p, cost(a)); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) {
            Vec3 c = p.position();
            FIELD_CENTER.put(p.getUUID(), c);
            Gravity.anim(p, "slam");
            Gravity.glow(p, Limb.BOTH_ARMS, 16);
            double r = a.stat("radius") * sqrtMult(p);
            Gravity.fx(p, Fx.SHOCKWAVE, c, (float) r, 0);
            Gravity.fx(p, Fx.FIELD, c, (float) r, 0);
            Gravity.shake(p, c, 1.6F, 16);
            Gravity.sound(p, c, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.5F);
            Gravity.sound(p, c, SoundEvents.WARDEN_SONIC_CHARGE, 1.5F, 0.6F);
        }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            Vec3 c = FIELD_CENTER.get(p.getUUID());
            if (c == null) return;
            double r = a.stat("radius") * sqrtMult(p);
            for (LivingEntity e : Gravity.around(p, c, r)) {
                e.addEffect(new MobEffectInstance(ModEffects.HEAVY.get(), 15, 2, false, false, true));
                if (t % 20 == 0) Gravity.hurt(p, e, a.statF("damage"));
            }
            // Los ataques que entran caen al suelo
            AABB box = new AABB(c, c).inflate(r);
            for (Projectile pr : p.level().getEntitiesOfClass(Projectile.class, box,
                    x -> x.getOwner() != p && x.position().distanceToSqr(c) < r * r)) {
                pr.setDeltaMovement(0, -0.8, 0);
                pr.hurtMarked = true;
                if (t % 5 == 0) Gravity.fx(p, Fx.DEFLECT, pr.position(), 0, 0);
            }
            if (t % 10 == 0) Gravity.fx(p, Fx.FIELD, c, (float) r, 1);
        }

        @Override
        public void onDeactivate(ServerPlayer p, Ability a) {
            Vec3 c = FIELD_CENTER.remove(p.getUUID());
            if (c != null) Gravity.fx(p, Fx.FIELD, c, (float) (a.stat("radius") * sqrtMult(p)), 2);
        }
    };

    /** Antigravedad (Hanjūryoku): todo lo que toca o entra en su campo sale despedido en otra dirección (también las balas). */
    public static final AbilityBehavior ANTI_GRAVITY = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.hasEther(p, a.stat("ether_per_second")); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) {
            Gravity.fx(p, Fx.DEFLECT, p.position().add(0, 1, 0), 2, 0);
            Gravity.sound(p, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
        }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            if (t % 20 == 0) Gravity.setEther(p, Gravity.ether(p) - (int) a.stat("ether_per_second"));
            double r = a.stat("radius");
            Vec3 c = p.position().add(0, 1, 0);
            AABB box = p.getBoundingBox().inflate(r);
            for (Projectile pr : p.level().getEntitiesOfClass(Projectile.class, box, x -> x.getOwner() != p)) {
                Vec3 v = pr.getDeltaMovement();
                Vec3 to = c.subtract(pr.position());
                if (v.lengthSqr() < 0.01 || v.dot(to) <= 0) continue;
                // Se desvía hacia fuera con un giro aleatorio
                Vec3 away = to.normalize().scale(-1);
                Vec3 rnd = new Vec3(p.getRandom().nextDouble() - 0.5, p.getRandom().nextDouble() * 0.5, p.getRandom().nextDouble() - 0.5);
                Vec3 nv = away.add(rnd).normalize().scale(Math.max(0.8, v.length()));
                pr.setDeltaMovement(nv);
                pr.hurtMarked = true;
                Gravity.fx(p, Fx.DEFLECT, pr.position(), 1, 0);
                Gravity.sound(p, pr.position(), SoundEvents.SHULKER_BULLET_HIT, 0.6F, 1.6F);
            }
            for (LivingEntity e : Gravity.around(p, c, a.stat("touch_radius"))) {
                Integer until = REPELLED.get(e.getUUID());
                if (until != null && until > p.tickCount) continue;
                REPELLED.put(e.getUUID(), p.tickCount + 20);
                repel(p, e, a.stat("knockback"));
            }
            if (t % 100 == 0) REPELLED.values().removeIf(u -> u < p.tickCount);
        }

        @Override
        public boolean shouldStop(ServerPlayer p, Ability a) { return Gravity.ether(p) <= 0; }
    };

    /** Lanza a una criatura con un campo antigravedad (también al golpearla con Antigravedad activa). */
    public static void repel(ServerPlayer p, LivingEntity e, double strength) {
        e.addEffect(new MobEffectInstance(ModEffects.WEIGHTLESS.get(), 30, 0));
        Gravity.launchFrom(e, p.position(), strength * Gravity.mult(p), 0.8);
        Gravity.fx(p, Fx.DEFLECT, e.position().add(0, e.getBbHeight() / 2, 0), 2, 0);
        Gravity.sound(p, e.position(), SoundEvents.SHULKER_BULLET_HIT, 0.8F, 0.8F);
    }

    /** Meteor Breaker: carga éter en el puño (mantén) y al soltar la esfera sale como un meteoro de gravedad. */
    public static final AbilityBehavior METEOR_BREAKER = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.spend(p, cost(a)); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) {
            Gravity.anim(p, "charge");
            CHARGING.put(p.getUUID(), GravitySphereEntity.create(p, GravitySphereEntity.METEOR, 0.3F));
            CHARGE_TICKS.put(p.getUUID(), 0);
            Gravity.sound(p, p.position(), SoundEvents.WARDEN_SONIC_CHARGE, 1F, 1.2F);
        }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            GravitySphereEntity s = CHARGING.get(p.getUUID());
            if (s == null || s.isRemoved()) return;
            float charge = Math.min(1F, t / (float) a.statI("charge_ticks"));
            CHARGE_TICKS.put(p.getUUID(), t);
            s.setSize(0.3F + charge * 1.9F);
            if (t % 4 == 0) Gravity.glow(p, Limb.RIGHT_ARM, 8);
            if (t % 10 == 0) Gravity.fx(p, Fx.FIST_CHARGE, Gravity.hand(p), 0.4F + charge, 10);
            if (t % 20 == 0) Gravity.sound(p, p.position(), SoundEvents.BEACON_AMBIENT, 1F, 0.6F + charge);
        }

        @Override
        public void onDeactivate(ServerPlayer p, Ability a) {
            GravitySphereEntity s = CHARGING.remove(p.getUUID());
            int t = CHARGE_TICKS.getOrDefault(p.getUUID(), 0);
            CHARGE_TICKS.remove(p.getUUID());
            if (s == null || s.isRemoved()) return;
            if (!p.isAlive()) { s.discard(); return; }
            float charge = Math.min(1F, t / (float) a.statI("charge_ticks"));
            float m = Gravity.mult(p);
            s.setStats(a.statF("damage") * (0.35F + 0.65F * charge) * m,
                    (float) (a.stat("radius") * (0.5 + charge) * Gravity.area(p)), (float) (1.6 + charge * 1.6));
            s.launch(p.getLookAngle(), a.stat("speed"));
            Gravity.anim(p, "gravity_punch");
            Gravity.glow(p, Limb.RIGHT_ARM, 10);
            Gravity.shake(p, p.position(), 0.6F + charge, 8);
            Gravity.sound(p, p.position(), SoundEvents.WITHER_SHOOT, 1.2F, 0.6F);
            Gravity.sound(p, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 0.8F, 1.0F);
        }
    };

    /**
     * Cielo Negro (Kokuten), la técnica definitiva: refuerza el peso de su corazón para crear una bola de gravedad que
     * no para de crecer y la estrella contra el objetivo. Necesita la barra de éter llena.
     */
    public static final AbilityBehavior BLACK_SKY = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.spend(p, Math.min(cost(a), Gravity.etherMax(p))); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) {
            Gravity.anim(p, "raise_sky");
            Gravity.glow(p, Limb.BOTH_ARMS, a.activationTicks());
            CHARGING.put(p.getUUID(), GravitySphereEntity.create(p, GravitySphereEntity.BLACK_SKY, 0.3F));
            Gravity.sound(p, p.position(), SoundEvents.WARDEN_HEARTBEAT, 3F, 0.5F);
            Gravity.sound(p, p.position(), SoundEvents.BEACON_ACTIVATE, 2F, 0.4F);
        }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            GravitySphereEntity s = CHARGING.get(p.getUUID());
            if (s == null || s.isRemoved()) return;
            float grow = Math.min(1F, t / (float) a.activationTicks());
            s.setSize(0.3F + grow * (float) a.stat("max_size"));
            p.setDeltaMovement(p.getDeltaMovement().multiply(0.5, 1, 0.5));
            if (t % 10 == 0) {
                Gravity.sound(p, p.position(), SoundEvents.WARDEN_HEARTBEAT, 2.5F, 0.5F + grow * 0.3F);
                Gravity.shake(p, s.position(), 0.3F + grow, 12);
            }
        }

        @Override
        public void onDeactivate(ServerPlayer p, Ability a) {
            // Si se corta antes de tiempo (muerte...) la bola desaparece; onActivationEnd la lanza antes de esto
            Tasks.later(p, 1, () -> {
                GravitySphereEntity s = CHARGING.get(p.getUUID());
                if (s != null && s.held()) { s.discard(); CHARGING.remove(p.getUUID()); }
            });
        }

        @Override
        public void onActivationEnd(ServerPlayer p, Ability a) {
            GravitySphereEntity s = CHARGING.remove(p.getUUID());
            if (s == null || s.isRemoved()) return;
            float m = Gravity.mult(p);
            s.setStats(a.statF("damage") * m, (float) (a.stat("radius") * Gravity.area(p)), 2.5F);
            Vec3 target = Gravity.lookPoint(p, a.stat("range"));
            s.launch(target.subtract(s.position()), a.stat("speed"));
            Gravity.anim(p, "slam");
            Gravity.sound(p, p.position(), SoundEvents.WITHER_SHOOT, 2F, 0.4F);
        }
    };

    // =====================================================================================
    // Overdrive
    // =====================================================================================

    /** Ticks que dura la carga de la transformación antes del estallido (el aspecto aparece al final). */
    public static final int TRANSFORM_TICKS = 24, TRANSFORM_TICKS_FULL = 50;

    public static int transformTicks(boolean full) { return full ? TRANSFORM_TICKS_FULL : TRANSFORM_TICKS; }

    /**
     * Transformación: se encoge concentrando el éter (no se puede mover), la tierra tiembla cada vez más y al final
     * estalla con el nuevo aspecto, empujando todo lo de alrededor.
     */
    static void transform(ServerPlayer p, boolean full) {
        int total = transformTicks(full);
        Gravity.anim(p, full ? "transform_full" : "transform");
        Gravity.glow(p, Limb.BOTH_ARMS, total);
        p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, total, 6, false, false));
        Gravity.sound(p, p.position(), SoundEvents.WARDEN_HEARTBEAT, 2F, 0.6F);
        Gravity.sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.5F, full ? 0.5F : 0.8F);
        Tasks.start(p, t -> {
            // El éter se concentra (el efecto dura 12 ticks y se repite mientras carga)
            if (t % 12 == 0 && t < total - 4) Gravity.fx(p, Fx.OVERDRIVE_CHARGE, p.position(), full ? 1 : 0, 0);
            if (t % 6 == 0 && t < total) {
                Gravity.shake(p, p.position(), 0.3F + t / (float) total * (full ? 1.4F : 0.7F), 8);
                Gravity.sound(p, p.position(), SoundEvents.WARDEN_HEARTBEAT, 1.5F, 0.6F + t / (float) total * 0.6F);
                if (full && t % 12 == 6) Gravity.fx(p, Fx.SHOCKWAVE, p.position(), 2 + t / 10F, 0);
            }
            if (t < total) return false;
            Gravity.fx(p, Fx.OVERDRIVE, p.position(), full ? 1 : 0, 0);
            Gravity.fx(p, Fx.SHOCKWAVE, p.position(), full ? 9 : 6, 0);
            Gravity.shake(p, p.position(), full ? 3F : 1.6F, full ? 34 : 20);
            if (full) {
                Gravity.sound(p, p.position(), SoundEvents.WITHER_SPAWN, 0.8F, 0.6F);
                Gravity.sound(p, p.position(), SoundEvents.WARDEN_ROAR, 1.4F, 0.7F);
            } else {
                Gravity.sound(p, p.position(), SoundEvents.BEACON_ACTIVATE, 1.5F, 0.8F);
                Gravity.sound(p, p.position(), SoundEvents.WARDEN_ROAR, 0.8F, 1.4F);
            }
            Gravity.sound(p, p.position(), SoundEvents.GENERIC_EXPLODE, full ? 2F : 1F, 0.6F);
            for (LivingEntity e : Gravity.around(p, p.position(), full ? 7 : 4.5))
                Gravity.launchFrom(e, p.position(), full ? 1.8 : 1.0, full ? 0.6 : 0.35);
            if (full) Gravity.crater(p, p.position().subtract(0, 0.5, 0), 2.5, 0.5);
            return true;
        });
    }

    /** Overdrive: el pelo y la piel se aclaran, salen marcas de éter y cuernos de energía; fuerza y técnicas al máximo. */
    public static final AbilityBehavior OVERDRIVE = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.hasEther(p, a.stat("min_ether")); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) { transform(p, false); }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            if (t % 20 == 0) Gravity.setEther(p, Gravity.ether(p) - (int) a.stat("ether_per_second"));
        }

        @Override
        public boolean shouldStop(ServerPlayer p, Ability a) { return Gravity.ether(p) <= 0; }

        @Override
        public void onDeactivate(ServerPlayer p, Ability a) {
            Gravity.fx(p, Fx.OVERDRIVE_OFF, p.position(), 0, 0);
            Gravity.sound(p, p.position(), SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.8F);
        }
    };

    /**
     * Overdrive completo: pelo blanco, brazos oscurecidos con marcas moradas y cuernos negros. Su gravedad es como un
     * agujero negro: atrae todo lo que tiene cerca.
     */
    public static final AbilityBehavior FULL_OVERDRIVE = new AbilityBehavior() {
        @Override
        public boolean canActivate(ServerPlayer p, Ability a) { return Gravity.hasEther(p, a.stat("min_ether")); }

        @Override
        public void onActivate(ServerPlayer p, Ability a) { transform(p, true); }

        @Override
        public void tick(ServerPlayer p, Ability a, int t) {
            if (t % 20 == 0) Gravity.setEther(p, Gravity.ether(p) - (int) a.stat("ether_per_second"));
            // Agujero negro: atrae criaturas, objetos y ataques
            double r = a.stat("pull_radius");
            Vec3 c = p.position().add(0, 1, 0);
            AABB box = p.getBoundingBox().inflate(r);
            for (Entity e : p.level().getEntities(p, box, x -> x instanceof ItemEntity || x instanceof ExperienceOrb
                    || x instanceof Projectile || Gravity.canAffect(p, x))) {
                Vec3 to = c.subtract(e.position());
                double d = to.length();
                if (d > r) continue;
                if (e instanceof Projectile pr && pr.getOwner() != p && d < 2.2) {
                    Gravity.fx(p, Fx.DEFLECT, pr.position(), 0, 0);
                    pr.discard(); // se lo traga
                    continue;
                }
                if (e instanceof Projectile) continue;
                if (d < 1.6) continue;
                double s = a.stat("pull_strength") * (1 - d / r) * Gravity.resist(e);
                Gravity.push(e, to.normalize().scale(s));
            }
        }

        @Override
        public boolean shouldStop(ServerPlayer p, Ability a) { return Gravity.ether(p) <= 0; }

        @Override
        public void onDeactivate(ServerPlayer p, Ability a) {
            Gravity.fx(p, Fx.OVERDRIVE_OFF, p.position(), 1, 0);
        }
    };

    public static void clear(UUID id) {
        FRENZY.remove(id);
        FIELD_CENTER.remove(id);
        GravitySphereEntity s = CHARGING.remove(id);
        if (s != null && s.held()) s.discard();
        CHARGE_TICKS.remove(id);
        SAFE_FALL.remove(id);
        SONIC.remove(id);
    }
}
