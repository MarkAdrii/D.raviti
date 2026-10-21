package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ClientVisuals;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.gravity.Limb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** En primera persona también se ve el brazo brillando y los brazos del Overdrive completo. */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonArms {
    private static final ResourceLocation ARMS = Draviti.id("textures/entity/od_arms.png");
    private static final ResourceLocation ARMS_GLOW = Draviti.id("textures/entity/od_arms_glow.png");
    private static final ResourceLocation MARKINGS = Draviti.id("textures/entity/od_markings.png");

    private FirstPersonArms() {}

    @SubscribeEvent
    public static void arm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        float glow = ClientVisuals.glow(player, right ? Limb.RIGHT_ARM : Limb.LEFT_ARM);
        boolean od = Gravity.overdrive(player), full = Gravity.fullOverdrive(player);
        if (glow <= 0 && !od && !full) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;

        PlayerModel<AbstractClientPlayer> m = renderer.getModel();
        // Misma postura que PlayerRenderer.renderHand
        m.attackTime = 0.0F;
        m.crouching = false;
        m.swimAmount = 0.0F;
        m.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ModelPart arm = right ? m.rightArm : m.leftArm;
        ModelPart sleeve = right ? m.rightSleeve : m.leftSleeve;
        arm.xRot = 0.0F;
        sleeve.xRot = 0.0F;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float pulse = 0.8F + 0.2F * Mth.sin(player.tickCount * 0.15F);

        if (full) {
            VertexConsumer dark = buf.getBuffer(RenderType.entityTranslucentCull(ARMS));
            arm.render(pose, dark, light, OverlayTexture.NO_OVERLAY);
            sleeve.render(pose, dark, light, OverlayTexture.NO_OVERLAY);
            VertexConsumer g = buf.getBuffer(RenderType.eyes(ARMS_GLOW));
            arm.render(pose, g, 0xF000F0, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1);
        } else if (od) {
            VertexConsumer g = buf.getBuffer(RenderType.eyes(MARKINGS));
            arm.render(pose, g, 0xF000F0, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1);
        }
        if (glow > 0) {
            VertexConsumer g = buf.getBuffer(RenderType.eyes(GlowLimbLayer.TEXTURE));
            float k = glow * pulse;
            arm.render(pose, g, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
            sleeve.render(pose, g, 0xF000F0, OverlayTexture.NO_OVERLAY, k, k, k, 1);
        }
    }
}
