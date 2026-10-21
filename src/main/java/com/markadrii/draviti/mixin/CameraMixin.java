package com.markadrii.draviti.mixin;

import com.markadrii.draviti.client.GravitySwapClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gravity Swap: mueve la cámara a donde estarían los ojos con la gravedad girada (en el techo, cerca del bloque de
 * abajo; en una pared, separada de ella). Vanilla ya la ha colocado: solo se le suma la diferencia.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("TAIL"))
    private void draviti$gravitySwapEye(BlockGetter level, Entity entity, boolean detached, boolean mirror,
                                        float partialTick, CallbackInfo ci) {
        Vec3 offset = GravitySwapClient.eyeOffset(entity, partialTick);
        if (offset != null) setPosition(getPosition().add(offset));
    }
}
