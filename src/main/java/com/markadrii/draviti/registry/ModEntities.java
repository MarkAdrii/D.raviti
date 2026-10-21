package com.markadrii.draviti.registry;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.entity.GravitySphereEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Entidades del mod. */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Draviti.MOD_ID);

    public static final RegistryObject<EntityType<GravitySphereEntity>> GRAVITY_SPHERE = ENTITIES.register("gravity_sphere",
            () -> EntityType.Builder.<GravitySphereEntity>of(GravitySphereEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).clientTrackingRange(10).updateInterval(1).fireImmune()
                    .build("gravity_sphere"));
}
