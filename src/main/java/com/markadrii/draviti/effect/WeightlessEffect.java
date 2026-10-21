package com.markadrii.draviti.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Ingrávido: el objetivo pierde su peso y flota sin control, subiendo poco a poco. Al acabarse vuelve a caer.
 */
public class WeightlessEffect extends MobEffect {
    public WeightlessEffect() {
        super(MobEffectCategory.HARMFUL, 0xC9A6FF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) { return true; }

    @Override
    public void applyEffectTick(LivingEntity e, int amplifier) {
        Vec3 v = e.getDeltaMovement();
        // La gravedad normal resta 0.08 en travel(): la compensamos y subimos un poco
        double up = 0.08 + 0.015 + 0.01 * amplifier;
        double y = v.y < 0 ? v.y * 0.6 + up : Math.min(v.y + up * 0.5, 0.35);
        e.setDeltaMovement(v.x * 0.9, y, v.z * 0.9);
        e.fallDistance = 0;
        if (e instanceof Mob mob && !e.level().isClientSide) mob.getNavigation().stop();
    }
}
