package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ClientVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Campo Gravitatorio: una esfera semitransparente que cubre la zona, con un brillo hexagonal que gira y late.
 * Turquesa pálido (normal y Overdrive) o morada (Overdrive completo). Los demás jugadores también la ven.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class FieldDomes {
    private static final ResourceLocation FILL = Draviti.id("textures/entity/field_fill.png");
    private static final ResourceLocation HEX = Draviti.id("textures/entity/field_hex.png");
    private static final int SEG_U = 32, SEG_V = 18;

    private static final class Dome {
        Vec3 pos; float radius; boolean violet; int born, until, closing = -1;
    }

    /** Uno por jugador que lo ha creado. */
    private static final Map<Integer, Dome> DOMES = new HashMap<>();

    private FieldDomes() {}

    /** state: 0 nuevo, 1 sigue (llega cada 10 ticks), 2 terminado. */
    public static void update(int owner, Vec3 pos, float radius, int state, boolean violet) {
        int now = ClientVisuals.ticks();
        if (state == 2) {
            Dome d = DOMES.get(owner);
            if (d != null && d.closing < 0) d.closing = now;
            return;
        }
        Dome d = DOMES.get(owner);
        if (d == null || state == 0 || d.closing >= 0) {
            d = new Dome();
            d.born = state == 0 ? now : now - 10;
            DOMES.put(owner, d);
        }
        d.pos = pos; d.radius = radius; d.violet = violet;
        d.until = now + 25;
    }

    public static void clear() { DOMES.clear(); }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || DOMES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { DOMES.clear(); return; }
        int now = ClientVisuals.ticks();
        float pt = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        Iterator<Dome> it = DOMES.values().iterator();
        while (it.hasNext()) {
            Dome d = it.next();
            float age = now - d.born + pt;
            float close = d.closing >= 0 ? (now - d.closing + pt) / 10F : (now > d.until ? (now - d.until + pt) / 10F : 0);
            if (close >= 1) { it.remove(); continue; }
            // Se abre desde el suelo con un pequeño rebote y se apaga al final
            float grow = Mth.clamp(age / 8F, 0, 1);
            float scale = grow < 1 ? (float) Math.sin(grow * Math.PI * 0.5) * 1.06F : 1F + 0.015F * Mth.sin(age * 0.2F);
            float alpha = (1 - close) * Mth.clamp(age / 4F, 0, 1);
            float r = d.radius * scale * (1 - close * 0.15F);
            float[] c = d.violet ? new float[]{0.62F, 0.34F, 1.0F} : new float[]{0.48F, 0.92F, 0.80F};

            pose.pushPose();
            pose.translate(d.pos.x - cam.x, d.pos.y - cam.y, d.pos.z - cam.z);
            // Relleno translúcido
            sphere(pose, buf.getBuffer(RenderType.entityTranslucent(FILL)), r, 0, 1, c[0], c[1], c[2], 0.16F * alpha);
            // Rejilla brillante que gira despacio
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 0.6F));
            float k = (0.55F + 0.15F * Mth.sin(age * 0.25F)) * alpha;
            sphere(pose, buf.getBuffer(RenderType.eyes(HEX)), r * 1.003F, 0, 4, c[0] * k, c[1] * k, c[2] * k, 1F);
            pose.popPose();
        }
        buf.endBatch(RenderType.entityTranslucent(FILL));
        buf.endBatch(RenderType.eyes(HEX));
    }

    /** Esfera UV (solo la parte de arriba del suelo hacia arriba se ve, la de abajo queda enterrada). */
    private static void sphere(PoseStack pose, VertexConsumer vc, float r, float u0, float uRepeat,
                               float red, float green, float blue, float a) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int i = 0; i < SEG_V; i++) {
            double v0 = Math.PI * i / SEG_V, v1 = Math.PI * (i + 1) / SEG_V;
            for (int j = 0; j < SEG_U; j++) {
                double h0 = 2 * Math.PI * j / SEG_U, h1 = 2 * Math.PI * (j + 1) / SEG_U;
                vert(vc, m, n, r, v0, h0, u0 + uRepeat * j / (float) SEG_U, i / (float) SEG_V, red, green, blue, a);
                vert(vc, m, n, r, v1, h0, u0 + uRepeat * j / (float) SEG_U, (i + 1) / (float) SEG_V, red, green, blue, a);
                vert(vc, m, n, r, v1, h1, u0 + uRepeat * (j + 1) / (float) SEG_U, (i + 1) / (float) SEG_V, red, green, blue, a);
                vert(vc, m, n, r, v0, h1, u0 + uRepeat * (j + 1) / (float) SEG_U, i / (float) SEG_V, red, green, blue, a);
            }
        }
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float r, double v, double h, float u, float tv,
                             float red, float green, float blue, float a) {
        float x = (float) (Math.sin(v) * Math.cos(h)), y = (float) Math.cos(v), z = (float) (Math.sin(v) * Math.sin(h));
        vc.vertex(m, x * r, y * r, z * r).color(red, green, blue, a).uv(u, tv * 2F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(n, x, y, z).endVertex();
    }
}
