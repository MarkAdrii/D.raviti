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

    /**
     * Personalidad de cada esfera: textura de las dos capas de dentro, cuántas veces se repite, cómo se mueve
     * (giro y desplazamiento) y cuánto brilla el borde.
     */
    public enum Style {
        /** Franjas de energía (por defecto). */
        SWIRL("energy_swirl", "energy_swirl", 2, 1, 1, 1, 2.2F, -1.5F, 0.004F, 0, 0.55F, 1.15F),
        /** Grietas de luz que laten (impactos, Meteor Breaker). */
        CRACKS("energy_cracks", "energy_swirl", 2, 1, 1, 1, 0.6F, -0.4F, 0, 0, 0.9F, 1.3F),
        /** Bandas de presión que bajan sin parar (Peso Infernal). */
        BANDS("energy_bands", "energy_bands", 3, 2, 2, 3, 0.3F, -0.3F, 0, 0.03F, 0.7F, 0.9F),
        /** Espiral que gira hacia el centro (Onda Gravitatoria). */
        SPIRAL("energy_spiral", "energy_spiral", 1, 1, 2, 1, 5F, 3.5F, 0.012F, 0.004F, 0.8F, 1.0F),
        /** Celdas como una pompa de jabón (Antigravedad, Cañón). */
        CELLS("energy_cells", "energy_swirl", 2, 1, 1, 1, 0.9F, -1.2F, 0.002F, 0, 0.6F, 1.5F),
        /** Escudo hexagonal (Campo Gravitatorio). */
        HEX("energy_hex", "energy_swirl", 3, 1, 3, 1, 0.5F, -1.0F, 0, 0, 0.75F, 1.2F),
        /** Vacío con estrellas (Cielo Negro). */
        VOID("energy_stars", "energy_cracks", 2, 1, 1, 1, 0.8F, -2F, 0.001F, 0, 0.9F, 1.6F);

        final ResourceLocation texA, texB;
        final int uRepA, uRepB, vRepA, vRepB;
        final float spinA, spinB, uScroll, vScroll, inner, rim;

        Style(String a, String b, int uRepA, int uRepB, int vRepA, int vRepB, float spinA, float spinB,
              float uScroll, float vScroll, float inner, float rim) {
            this.texA = Draviti.id("textures/entity/" + a + ".png");
            this.texB = Draviti.id("textures/entity/" + b + ".png");
            this.uRepA = uRepA; this.uRepB = uRepB; this.vRepA = vRepA; this.vRepB = vRepB;
            this.spinA = spinA; this.spinB = spinB; this.uScroll = uScroll; this.vScroll = vScroll;
            this.inner = inner; this.rim = rim;
        }
    }

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

    private static final java.util.Map<ResourceLocation, RenderType> TYPES = new java.util.LinkedHashMap<>();

    public static RenderType type(ResourceLocation tex) { return TYPES.computeIfAbsent(tex, Types::energy); }
    public static RenderType swirl() { return type(SWIRL); }
    public static RenderType white() { return type(WHITE); }

    /** Termina de dibujar todas las capas (al final del frame). */
    public static void endBatch(MultiBufferSource.BufferSource buf) {
        for (RenderType t : TYPES.values()) buf.endBatch(t);
    }

    /**
     * Dibuja la esfera centrada en el origen de la pose. cam = posición de la cámara relativa al centro (para el
     * borde brillante). alpha 0..1 (brillo general), age en ticks (giro de las franjas).
     */
    public static void draw(PoseStack pose, MultiBufferSource buffer, float radius, Vec3 cam, float[] color, float alpha, float age) {
        draw(pose, buffer, radius, cam, color, alpha, age, Style.SWIRL);
    }

    public static void draw(PoseStack pose, MultiBufferSource buffer, float radius, Vec3 cam, float[] color, float alpha,
                            float age, Style st) {
        if (radius <= 0.01F || alpha <= 0.002F) return;
        float r = color[0], g = color[1], b = color[2];
        // Las grietas laten
        float beat = st == Style.CRACKS ? 0.75F + 0.35F * (float) Math.sin(age * 0.9F) : 1F;
        // 1) Borde: casi nada de frente, mucho de lado
        shell(pose, buffer.getBuffer(white()), radius, cam, r, g, b, alpha, 0, 1, 0, 1, 0.05F, st.rim, 2.6F);
        // 2) Dos capas que giran en sentidos distintos (la de dentro un poco más pequeña)
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(age * st.spinA));
        if (st != Style.BANDS) pose.mulPose(Axis.ZP.rotationDegrees(18F));
        shell(pose, buffer.getBuffer(type(st.texA)), radius * 0.985F, cam, r, g, b, alpha * beat, age * st.uScroll, st.uRepA,
                age * st.vScroll, st.vRepA, st.inner, 0.7F, 1.4F);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(age * st.spinB + 60F));
        if (st != Style.BANDS) pose.mulPose(Axis.XP.rotationDegrees(-24F));
        shell(pose, buffer.getBuffer(type(st.texB)), radius * 0.94F, cam, r, g, b, alpha * 0.7F, -age * st.uScroll * 0.7F, st.uRepB,
                age * st.vScroll * 1.3F, st.vRepB, st.inner * 0.6F, 0.5F, 1.2F);
        pose.popPose();
    }

    /**
     * Esfera UV. Intensidad de cada vértice = alpha * (base + rim * borde^pow), donde borde = 1 - |normal·vista|.
     */
    private static void shell(PoseStack pose, VertexConsumer vc, float radius, Vec3 cam, float r, float g, float b, float alpha,
                              float uScroll, float uRepeat, float vScroll, float vRepeat, float base, float rim, float pow) {
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
                float t0 = vScroll + vRepeat * i / (float) SEG_V, t1 = vScroll + vRepeat * (i + 1) / (float) SEG_V;
                vert(vc, m, n, radius, v0, h0, u0, t0, camLocal, r, g, b, alpha, base, rim, pow);
                vert(vc, m, n, radius, v1, h0, u0, t1, camLocal, r, g, b, alpha, base, rim, pow);
                vert(vc, m, n, radius, v1, h1, u1, t1, camLocal, r, g, b, alpha, base, rim, pow);
                vert(vc, m, n, radius, v0, h1, u1, t0, camLocal, r, g, b, alpha, base, rim, pow);
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
