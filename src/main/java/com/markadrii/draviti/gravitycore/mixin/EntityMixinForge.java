package com.markadrii.draviti.gravitycore.mixin;

import com.markadrii.draviti.gravitycore.GravityCore;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixinForge {
    @Inject(method = "tick",at = @At("RETURN"))
    private void tickEntity(CallbackInfo ci) {
        GravityCore.onTick((Entity)(Object)this);
    }
}
