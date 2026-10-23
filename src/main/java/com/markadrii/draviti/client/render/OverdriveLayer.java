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
 *   <li>Completo: brazos y piernas oscurecidos con marcas moradas, detalles en pecho y espalda (sin cubrirlo entero)
 *       y cuernos negros con las mismas marcas.</li>
 * </ul>
 */
public class OverdriveLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation MARKINGS = Draviti.id("textures/entity/od_markings.png");
    private static final ResourceLocation TORSO = Draviti.id("textures/entity/od_torso.png");
    private static final ResourceLocation TORSO_GLOW = Draviti.id("textures/entity/od_torso_glow.png");
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
        draw(new PlayerBody.Vanilla(getParentModel()), pose, buffer, light, LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                player, partialTick, age);
    }

    /** Aspecto del Overdrive (vale para el modelo normal y el de Epic Fight). */
    public static void draw(PlayerBody body, PoseStack pose, MultiBufferSource buffer, int light, int overlay,
                            AbstractClientPlayer player, float partialTick, float age) {
        if (player.isInvisible()) return;
        boolean full = Gravity.fullOverdrive(player);
        if (!full && !Gravity.overdrive(player)) return;
        // Al transformarse aparece poco a poco
        String key = Gravity.POWER_ID + "/" + (full ? "overdrive_completo" : "overdrive");
        // El aspecto aparece de golpe al final de la transformación (cuando estalla)
        int start = com.markadrii.draviti.gravity.GravityAbilities.transformTicks(full) - 3;
        float in = Mth.clamp((ClientVisuals.activeTicks(player, key) - start + partialTick) / 5F, 0, 1);
        if (in <= 0 && !full) return;
        if (full && in <= 0) { full = false; in = 1; }
        float pulse = 0.8F + 0.2F * Mth.sin(age * 0.15F);
        PlayerBody.Part[] limbs = {PlayerBody.Part.RIGHT_ARM, PlayerBody.Part.LEFT_ARM, PlayerBody.Part.RIGHT_LEG, PlayerBody.Part.LEFT_LEG};

        if (!full) {
            // Piel y pelo más claros: la propia skin otra vez, sumada (solo las partes de dentro)
            float k = 0.16F * in;
            body.draw(pose, buffer, RenderType.eyes(player.getSkinTextureLocation()), 0xF000F0, overlay, k, k, k * 1.05F, 1,
                    PlayerBody.ALL_INNER);
            float mk = in * pulse * 0.55F;
            body.draw(pose, buffer, RenderType.eyes(MARKINGS), 0xF000F0, overlay, mk, mk, mk, 1, PlayerBody.ALL);
        } else {
            // Brazos y piernas oscurecidos con marcas moradas
            body.draw(pose, buffer, RenderType.entityTranslucentCull(ARMS), light, overlay, 1, 1, 1, in,
                    PlayerBody.Part.RIGHT_ARM, PlayerBody.Part.LEFT_ARM, PlayerBody.Part.RIGHT_SLEEVE, PlayerBody.Part.LEFT_SLEEVE,
                    PlayerBody.Part.RIGHT_LEG, PlayerBody.Part.LEFT_LEG, PlayerBody.Part.RIGHT_PANTS, PlayerBody.Part.LEFT_PANTS);
            float gk = in * pulse * 0.7F;
            body.draw(pose, buffer, RenderType.eyes(ARMS_GLOW), 0xF000F0, overlay, gk, gk, gk, 1, limbs);
            // Detalles en pecho y espalda (el centro del pecho queda al aire)
            body.draw(pose, buffer, RenderType.entityTranslucentCull(TORSO), light, overlay, 1, 1, 1, in,
                    PlayerBody.Part.BODY, PlayerBody.Part.JACKET);
            body.draw(pose, buffer, RenderType.eyes(TORSO_GLOW), 0xF000F0, overlay, gk, gk, gk, 1, PlayerBody.Part.BODY);
        }

        // Cuernos (siguen a la cabeza)
        pose.pushPose();
        body.toHead(pose);
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
