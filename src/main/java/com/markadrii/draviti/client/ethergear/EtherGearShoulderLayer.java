package com.markadrii.draviti.client.ethergear;

import com.markadrii.draviti.gravity.Gravity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** El Ether Gear sentado en el hombro izquierdo (como el mini golem de Seals). */
public class EtherGearShoulderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float SCALE = 0.3F;
    private final EtherGearModel model;

    public EtherGearShoulderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.model = new EtherGearModel(models.bakeLayer(EtherGearModel.LAYER));
    }

    public EtherGearModel model() { return model; }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        draw(new com.markadrii.draviti.client.render.PlayerBody.Vanilla(getParentModel()), model, pose, buffer, light, player, age);
    }

    /** El Ether Gear en el hombro (vale para el modelo normal y el de Epic Fight). */
    public static void draw(com.markadrii.draviti.client.render.PlayerBody body, EtherGearModel model, PoseStack pose,
                            MultiBufferSource buffer, int light, AbstractClientPlayer player, float age) {
        if (player.isInvisible() || !Gravity.active(player, "_eg_shoulder")) return;
        pose.pushPose();
        body.toBody(pose);
        // Pies sobre el hombro izquierdo (el modelo mide 24 px desde el origen hasta los pies)
        pose.translate(0.42F, (player.isCrouching() ? 0.2F : 0F) - 1.5F * SCALE + 0.05F, player.isCrouching() ? 0.1F : 0F);
        pose.scale(SCALE, SCALE, SCALE);
        model.setupAnim(null, 0, 0, age, 0, 0);
        model.renderToBuffer(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(EtherGearRenderer.TEXTURE)), light,
                OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        model.renderToBuffer(pose, buffer.getBuffer(RenderType.eyes(EtherGearRenderer.GLOW)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
    }
}
