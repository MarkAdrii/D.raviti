package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ClientVisuals;
import com.markadrii.draviti.gravity.Gravity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Marca del Ether Gear (Satan Gravity) en el antebrazo derecho, como la de Shiki: líneas pixeladas verde turquesa
 * pálido del codo a la muñeca y el dorso de la mano. Sale siempre que se tiene el poder (también en primera persona).
 */
public class EtherGearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation MARK = Draviti.id("textures/entity/ether_gear_mark.png");

    public EtherGearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** El servidor manda "draviti:gravity/_mark" si tiene el poder y no la ha ocultado (F dos veces). */
    public static boolean hasPower(AbstractClientPlayer p) {
        return ClientVisuals.active(p, Gravity.POWER_ID + "/_mark");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        // En el Overdrive completo el brazo se oscurece y la marca queda debajo
        if (player.isInvisible() || !hasPower(player) || Gravity.fullOverdrive(player)) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        renderPart(pose, buffer, light, overlay, m.rightArm);
    }

    static void renderArm(PoseStack pose, MultiBufferSource buffer, int light, ModelPart arm) {
        renderPart(pose, buffer, light, OverlayTexture.NO_OVERLAY, arm);
    }

    private static void renderPart(PoseStack pose, MultiBufferSource buffer, int light, int overlay, ModelPart arm) {
        // Un pelín más grande que el brazo para que no parpadee con la skin
        float x = arm.xScale, y = arm.yScale, z = arm.zScale;
        arm.xScale = x * 1.01F; arm.yScale = y * 1.005F; arm.zScale = z * 1.01F;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentCull(MARK));
        arm.render(pose, vc, light, overlay, 1, 1, 1, 1);
        // Brillo suave encima
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(MARK));
        arm.render(pose, glow, 0xF000F0, OverlayTexture.NO_OVERLAY, 0.35F, 0.35F, 0.35F, 1);
        arm.xScale = x; arm.yScale = y; arm.zScale = z;
    }
}
