package com.markadrii.draviti.client.anim;

import com.markadrii.draviti.Draviti;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Animaciones del jugador con Player Animator (assets/draviti/player_animation/*.json, formato Emotecraft).
 * Prioridad 1000 + 42: por encima de las de otros mods mientras se usa una técnica.
 */
public final class GravityAnimations {
    private static final ResourceLocation LAYER = Draviti.id("gravity");

    private GravityAnimations() {}

    public static void init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1042, player -> new ModifierLayer<>());
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> layer(AbstractClientPlayer player) {
        IAnimation raw = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
        return raw instanceof ModifierLayer<?> l ? (ModifierLayer<IAnimation>) l : null;
    }

    /** Hay una animación nuestra en marcha (Epic Fight pasa a modo normal mientras tanto). */
    public static boolean animating(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = layer(player);
        return layer != null && layer.getAnimation() != null && layer.getAnimation().isActive();
    }

    public static void play(AbstractClientPlayer player, String name) {
        ModifierLayer<IAnimation> layer = layer(player);
        if (layer == null) return;
        if ("stop".equals(name)) {
            layer.setAnimation(null);
            return;
        }
        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(Draviti.id(name));
        if (animation == null) {
            Draviti.LOGGER.warn("Animación no encontrada: {}", name);
            return;
        }
        layer.setAnimation(new KeyframeAnimationPlayer(animation));
    }
}
