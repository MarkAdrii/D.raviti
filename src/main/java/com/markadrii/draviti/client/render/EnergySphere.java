package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Esfera de energía semitransparente para las técnicas de área: borde brillante (más luz donde se ve de lado, como
 * una burbuja), franjas de energía que giran por dentro y luz que se suma (lo negro no se ve). Se ve también desde
 * dentro. Turquesa pálido (normal y Overdrive) o morada (Overdrive completo).
 */
public final class EnergySphere {
    public static final ResourceLocation SWIRL = Draviti.id("textures/entity/energy_swirl.png");
    public static final ResourceLocation WHITE = Draviti.id("textures/entity/energy_white.png");
    private static final int SEG_U = 40, SEG_V = 22;

    public static final float[] TEAL = {0.42F, 0.95F, 0.85F};
    public static final float[] VIOLET = {0.66F, 0.36F, 1.0F};

    private EnergySphere() {}

    /** Luz que se suma, sin tapar nada y por las dos caras. */
    private static final class Types extends RenderType {
        private Types() { super("draviti_dummy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, () -> {}, () -> {}); }

        static RenderType energy(ResourceLocation tex) {
            return create("draviti_energy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_EYES_SHADER)
                            .setTextureState(new TextureStateShard(tex, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE)
                            .setLightmapState(NO_LIGHTMAP)
                            .setOverlayState(NO_OVERLAY)
                            .createCompositeState(false));
        }
    }

    private static RenderType swirlType, whiteType;

    public static RenderType swirl() { if (swirlType == null) swirlType = Types.energy(SWIRL); return swirlType; }
    public static RenderType white() { if (whiteType == null) whiteType = Types.energy(WHITE); return whiteType; }

    /**
     * Dibuja la esfera centrada en el origen de la pose. cam = posición de la cámara relativa al centro (para el
     * borde brillante). alpha 0..1 (brillo general), age en ticks (giro de las franjas).
     */
    public static void draw(PoseStack pose, MultiBufferSource buffer, float radius, Vec3 cam, float[] color, float alpha, float age) {
        if (radius <= 0.01F || alpha <= 0.002F) return;
        float r = color[0], g = color[1], b = color[2];
        // 1) Borde: casi nada de frente, mucho de lado
        shell(pose, buffer.getBuffer(white()), radius, cam, r, g, b, alpha, 0, 1, 0.05F, 1.15F, 2.6F);
        // 2) Dos capas de franjas que giran en sentidos distintos (las de dentro un poco más pequeñas)
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(age * 2.2F));
        pose.mulPose(Axis.ZP.rotationDegrees(18F));
        shell(pose, buffer.getBuffer(swirl()), radius * 0.985F, cam, r, g, b, alpha, age * 0.004F, 2, 0.55F, 0.7F, 1.4F);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-age * 1.5F + 60F));
        pose.mulPose(Axis.XP.rotationDegrees(-24F));
        shell(pose, buffer.getBuffer(swirl()), radius * 0.94F, cam, r, g, b, alpha, -age * 0.003F, 1, 0.35F, 0.5F, 1.2F);
        pose.popPose();
    }

    /**
     * Esfera UV. Intensidad de cada vértice = alpha * (base + rim * borde^pow), donde borde = 1 - |normal·vista|.
     */
    private static void shell(PoseStack pose, VertexConsumer vc, float radius, Vec3 cam, float r, float g, float b, float alpha,
                              float uScroll, float uRepeat, float base, float rim, float pow) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        // La cámara en el espacio de esta pose (para el borde aunque la esfera esté girada)
        Matrix4f inv = new Matrix4f(m).invert();
        Vector4f c4 = inv.transform(new Vector4f(0, 0, 0, 1));
        Vector3f camLocal = new Vector3f(c4.x, c4.y, c4.z);
        for (int i = 0; i < SEG_V; i++) {
            double v0 = Math.PI * i / SEG_V, v1 = Math.PI * (i + 1) / SEG_V;
            for (int j = 0; j < SEG_U; j++) {
                double h0 = 2 * Math.PI * j / SEG_U, h1 = 2 * Math.PI * (j + 1) / SEG_U;
                float u0 = uScroll + uRepeat * j / (float) SEG_U, u1 = uScroll + uRepeat * (j + 1) / (float) SEG_U;
                vert(vc, m, n, radius, v0, h0, u0, i / (float) SEG_V, camLocal, r, g, b, alpha, base, rim, pow);
                vert(vc, m, n, radius, v1, h0, u0, (i + 1) / (float) SEG_V, camLocal, r, g, b, alpha, base, rim, pow);
                vert(vc, m, n, radius, v1, h1, u1, (i + 1) / (float) SEG_V, camLocal, r, g, b, alpha, base, rim, pow);
                vert(vc, m, n, radius, v0, h1, u1, i / (float) SEG_V, camLocal, r, g, b, alpha, base, rim, pow);
            }
        }
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float radius, double v, double h, float u, float tv,
                             Vector3f cam, float r, float g, float b, float alpha, float base, float rim, float pow) {
        float x = (float) (Math.sin(v) * Math.cos(h)), y = (float) Math.cos(v), z = (float) (Math.sin(v) * Math.sin(h));
        float px = x * radius, py = y * radius, pz = z * radius;
        float vx = cam.x - px, vy = cam.y - py, vz = cam.z - pz;
        float len = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        float dot = len < 1e-4F ? 0 : Math.abs((x * vx + y * vy + z * vz) / len);
        float edge = (float) Math.pow(1 - dot, pow);
        float k = alpha * (base + rim * edge);
        vc.vertex(m, px, py, pz).color(Math.min(1, r * k), Math.min(1, g * k), Math.min(1, b * k), 1F).uv(u, tv)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, x, y, z).endVertex();
    }
}
