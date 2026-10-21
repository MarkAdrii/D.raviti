package com.markadrii.draviti.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Pesado: la gravedad sobre el objetivo se multiplica. Cae en picado (también los que vuelan), se arrastra por el
 * suelo y no puede saltar (LivingJumpEvent en GravityEvents).
 */
public class HeavyEffect extends MobEffect {
    public HeavyEffect() {
        super(MobEffectCategory.HARMFUL, 0x3B0F6B);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "6f2b4d41-5c8a-4f8e-9e0c-2d6c3a1b7e01", -0.25,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.FLYING_SPEED, "6f2b4d41-5c8a-4f8e-9e0c-2d6c3a1b7e02", -0.6,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) { return true; }

    @Override
    public void applyEffectTick(LivingEntity e, int amplifier) {
        Vec3 v = e.getDeltaMovement();
        double pull = 0.10 + 0.06 * amplifier;
        if (!e.onGround()) e.setDeltaMovement(v.x * 0.7, Math.min(v.y, 0) - pull, v.z * 0.7);
        else e.setDeltaMovement(v.x * 0.55, Math.min(v.y, 0), v.z * 0.55);
    }
}
