package com.markadrii.draviti.mixin;

import com.markadrii.draviti.client.GravitySwapClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gravity Swap: el movimiento del ratón (Entity.turn) gira la mirada relativa al suelo nuevo en vez de la del mundo,
 * así en el techo y en las paredes derecha/izquierda y arriba/abajo son las que esperas.
 */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void draviti$gravitySwapTurn(double yRot, double xRot, CallbackInfo ci) {
        if (GravitySwapClient.handleTurn((Entity) (Object) this, yRot, xRot)) ci.cancel();
    }
}
