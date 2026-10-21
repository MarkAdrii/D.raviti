package com.markadrii.draviti.registry;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.effect.HeavyEffect;
import com.markadrii.draviti.effect.WeightlessEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Draviti.MOD_ID);

    public static final RegistryObject<MobEffect> HEAVY = EFFECTS.register("heavy", HeavyEffect::new);
    public static final RegistryObject<MobEffect> WEIGHTLESS = EFFECTS.register("weightless", WeightlessEffect::new);

    private ModEffects() {}
}
