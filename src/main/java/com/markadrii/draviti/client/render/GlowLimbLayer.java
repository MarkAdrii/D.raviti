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
        if (player.isInvisible()) return;
        float r = ClientVisuals.glow(player, Limb.RIGHT_ARM), l = ClientVisuals.glow(player, Limb.LEFT_ARM);
        float leg = ClientVisuals.glow(player, Limb.RIGHT_LEG);
        if (r <= 0 && l <= 0 && leg <= 0) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(texture(player)));
        float pulse = 0.75F + 0.25F * Mth.sin(age * 0.8F);
        if (r > 0) {
            float k = r * pulse;
            m.rightArm.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
            m.rightSleeve.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
        }
        if (l > 0) {
            float k = l * pulse;
            m.leftArm.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
            m.leftSleeve.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
        }
        if (leg > 0) {
            float k = leg * pulse;
            m.rightLeg.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
            m.rightPants.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
        }
    }
}
