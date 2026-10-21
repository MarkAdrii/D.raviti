package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.entity.GravitySphereEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Esfera de gravedad: núcleo negro con borde morado, un halo que brilla y un anillo en espiral que gira
 * (siempre de cara a la cámara). Las partículas que la rodean las pone GravityFx.
 */
public class GravitySphereRenderer extends EntityRenderer<GravitySphereEntity> {
    private static final ResourceLocation CORE = Draviti.id("textures/entity/sphere_core.png");
    private static final ResourceLocation HALO = Draviti.id("textures/entity/sphere_halo.png");
    private static final ResourceLocation RING = Draviti.id("textures/entity/sphere_ring.png");

    public GravitySphereRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(GravitySphereEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        float size = e.size();
        float t = e.tickCount + pt;
        pose.pushPose();
        pose.translate(0, 0.3, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));

        float pulse = 1 + 0.06F * Mth.sin(t * 0.5F);
        // Halo exterior
        quad(pose, buf.getBuffer(RenderType.eyes(HALO)), size * 2.6F * pulse, t * 2F, 0.9F, 0.8F, 1F);
        // Anillo en espiral girando (dos capas en sentidos contrarios)
        quad(pose, buf.getBuffer(RenderType.eyes(RING)), size * 2.0F, t * 9F, 1F, 1F, 1F);
        quad(pose, buf.getBuffer(RenderType.eyes(RING)), size * 1.7F, -t * 6F, 0.7F, 0.6F, 1F);
        // Núcleo negro
        quad(pose, buf.getBuffer(RenderType.entityTranslucent(CORE)), size * 1.25F, 0, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, pt, pose, buf, light);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float size, float rotDeg, float r, float g, float b) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(rotDeg));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        float h = size * 0.5F;
        v(vc, m, n, -h, -h, 0, 1, r, g, b);
        v(vc, m, n, h, -h, 1, 1, r, g, b);
        v(vc, m, n, h, h, 1, 0, r, g, b);
        v(vc, m, n, -h, h, 0, 0, r, g, b);
        pose.popPose();
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, float r, float g, float b) {
        vc.vertex(m, x, y, 0).color(r, g, b, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(GravitySphereEntity e) { return CORE; }
}
