package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ClientVisuals;
import com.markadrii.draviti.gravity.Gravity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Aspecto del Overdrive sobre la skin del jugador (se mantiene su skin y su armadura):
 * <ul>
 *   <li>Overdrive: el pelo y la piel se aclaran, marcas de éter claras por todo el cuerpo y cuernos de energía.</li>
 *   <li>Completo: pelo blanco, brazos oscurecidos con marcas moradas y cuernos negros con las mismas marcas.</li>
 * </ul>
 */
public class OverdriveLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation MARKINGS = Draviti.id("textures/entity/od_markings.png");
    private static final ResourceLocation HAIR = Draviti.id("textures/entity/od_hair.png");
    private static final ResourceLocation ARMS = Draviti.id("textures/entity/od_arms.png");
    private static final ResourceLocation ARMS_GLOW = Draviti.id("textures/entity/od_arms_glow.png");
    private static final ResourceLocation HORN_ENERGY = Draviti.id("textures/entity/horn_energy.png");
    private static final ResourceLocation HORN_BLACK = Draviti.id("textures/entity/horn_black.png");
    private static final ResourceLocation HORN_BLACK_GLOW = Draviti.id("textures/entity/horn_black_glow.png");

    public OverdriveLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        boolean full = Gravity.fullOverdrive(player);
        if (!full && !Gravity.overdrive(player)) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        // Al transformarse aparece poco a poco
        String key = Gravity.POWER_ID + "/" + (full ? "overdrive_completo" : "overdrive");
        // El aspecto aparece de golpe al final de la transformación (cuando estalla)
        int start = com.markadrii.draviti.gravity.GravityAbilities.TRANSFORM_TICKS - 3;
        float in = Mth.clamp((ClientVisuals.activeTicks(player, key) - start + partialTick) / 5F, 0, 1);
        if (in <= 0 && !full) return;
        if (full && in <= 0) { key = Gravity.POWER_ID + "/overdrive"; full = false; in = 1; }
        float pulse = 0.8F + 0.2F * Mth.sin(age * 0.15F);

        if (!full) {
            // Piel y pelo más claros: la propia skin otra vez, sumada (solo las partes de dentro)
            boolean hat = m.hat.visible, jacket = m.jacket.visible, ls = m.leftSleeve.visible, rs = m.rightSleeve.visible,
                    lp = m.leftPants.visible, rp = m.rightPants.visible;
            m.hat.visible = m.jacket.visible = m.leftSleeve.visible = m.rightSleeve.visible = m.leftPants.visible = m.rightPants.visible = false;
            VertexConsumer lighten = buffer.getBuffer(RenderType.eyes(player.getSkinTextureLocation()));
            float k = 0.32F * in;
            m.renderToBuffer(pose, lighten, 0xF000F0, overlay, k, k, k * 1.05F, 1);
            m.hat.visible = hat; m.jacket.visible = jacket; m.leftSleeve.visible = ls; m.rightSleeve.visible = rs;
            m.leftPants.visible = lp; m.rightPants.visible = rp;

            VertexConsumer marks = buffer.getBuffer(RenderType.eyes(MARKINGS));
            float mk = in * pulse;
            m.renderToBuffer(pose, marks, 0xF000F0, overlay, mk, mk, mk, 1);
        } else {
            // Pelo blanco: sobre la skin (la cabeza, un poco más grande para que no se mezcle con ella) y sobre el gorro
            VertexConsumer hair = buffer.getBuffer(RenderType.entityTranslucentCull(HAIR));
            float hx = m.head.xScale, hy = m.head.yScale, hz = m.head.zScale;
            m.head.xScale = hx * 1.02F; m.head.yScale = hy * 1.02F; m.head.zScale = hz * 1.02F;
            m.head.render(pose, hair, light, overlay, 1, 1, 1, in);
            m.head.xScale = hx; m.head.yScale = hy; m.head.zScale = hz;
            m.hat.render(pose, hair, light, overlay, 1, 1, 1, in);
            // Brazos y piernas oscurecidos con marcas moradas
            VertexConsumer dark = buffer.getBuffer(RenderType.entityTranslucentCull(ARMS));
            for (var part : new net.minecraft.client.model.geom.ModelPart[]{m.rightArm, m.leftArm, m.rightSleeve, m.leftSleeve,
                    m.rightLeg, m.leftLeg, m.rightPants, m.leftPants})
                part.render(pose, dark, light, overlay, 1, 1, 1, in);
            VertexConsumer glow = buffer.getBuffer(RenderType.eyes(ARMS_GLOW));
            float gk = in * pulse;
            for (var part : new net.minecraft.client.model.geom.ModelPart[]{m.rightArm, m.leftArm, m.rightLeg, m.leftLeg})
                part.render(pose, glow, 0xF000F0, overlay, gk, gk, gk, 1);
        }

        // Cuernos (siguen a la cabeza)
        pose.pushPose();
        m.head.translateAndRotate(pose);
        float s = 0.4F + 0.6F * in;
        pose.translate(0, -0.25F * (1 - s), 0);
        pose.scale(s, s, s);
        var horns = HornModel.get();
        if (!full) {
            VertexConsumer e = buffer.getBuffer(RenderType.eyes(HORN_ENERGY));
            horns.render(pose, e, 0xF000F0, OverlayTexture.NO_OVERLAY, 0.85F * pulse, 0.8F * pulse, 1F * pulse, 1);
            VertexConsumer t = buffer.getBuffer(RenderType.entityTranslucent(HORN_ENERGY));
            horns.render(pose, t, 0xF000F0, OverlayTexture.NO_OVERLAY, 1, 1, 1, 0.35F);
        } else {
            VertexConsumer b = buffer.getBuffer(RenderType.entityCutoutNoCull(HORN_BLACK));
            horns.render(pose, b, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            VertexConsumer g = buffer.getBuffer(RenderType.eyes(HORN_BLACK_GLOW));
            horns.render(pose, g, 0xF000F0, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1);
        }
        pose.popPose();
    }
}
