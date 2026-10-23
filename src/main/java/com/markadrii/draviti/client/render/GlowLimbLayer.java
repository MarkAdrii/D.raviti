package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ClientVisuals;
import com.markadrii.draviti.gravity.Limb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** El brazo (o la pierna) brilla de energía morada antes de una técnica: Puño, Ráfaga, Patada... */
public class GlowLimbLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Morado (Overdrive completo) y turquesa pálido (normal y Overdrive). */
    public static final ResourceLocation TEXTURE = Draviti.id("textures/entity/glow_limb.png");
    public static final ResourceLocation TEXTURE_TEAL = Draviti.id("textures/entity/glow_limb_teal.png");

    public static ResourceLocation texture(AbstractClientPlayer p) {
        return com.markadrii.draviti.gravity.Gravity.fullOverdrive(p) ? TEXTURE : TEXTURE_TEAL;
    }

    public GlowLimbLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        draw(new PlayerBody.Vanilla(getParentModel()), pose, buffer, player, age);
    }

    /** Brillo de los miembros que golpean (vale para el modelo normal y el de Epic Fight). */
    public static void draw(PlayerBody body, PoseStack pose, MultiBufferSource buffer, AbstractClientPlayer player, float age) {
        if (player.isInvisible()) return;
        float r = ClientVisuals.glow(player, Limb.RIGHT_ARM), l = ClientVisuals.glow(player, Limb.LEFT_ARM);
        float leg = ClientVisuals.glow(player, Limb.RIGHT_LEG);
        if (r <= 0 && l <= 0 && leg <= 0) return;
        RenderType type = RenderType.eyes(texture(player));
        float pulse = 0.75F + 0.25F * Mth.sin(age * 0.8F);
        if (r > 0) body.draw(pose, buffer, type, 0xF000F0, OverlayTexture.NO_OVERLAY, r * pulse, r * pulse, r * pulse, 1,
                PlayerBody.Part.RIGHT_ARM, PlayerBody.Part.RIGHT_SLEEVE);
        if (l > 0) body.draw(pose, buffer, type, 0xF000F0, OverlayTexture.NO_OVERLAY, l * pulse, l * pulse, l * pulse, 1,
                PlayerBody.Part.LEFT_ARM, PlayerBody.Part.LEFT_SLEEVE);
        if (leg > 0) body.draw(pose, buffer, type, 0xF000F0, OverlayTexture.NO_OVERLAY, leg * pulse, leg * pulse, leg * pulse, 1,
                PlayerBody.Part.RIGHT_LEG, PlayerBody.Part.RIGHT_PANTS);
    }
}
