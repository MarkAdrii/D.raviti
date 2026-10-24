package com.markadrii.draviti.client.render;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ClientVisuals;
import com.markadrii.draviti.gravity.Gravity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Esferas de energía de las técnicas de área (las manda el servidor con los efectos de siempre):
 * <ul>
 *   <li>BURST: crece rápido hasta su radio y se apaga (Peso Infernal, Patada Molinete, impactos, Meteor Breaker...).</li>
 *   <li>IMPLODE: empieza grande y se encoge hacia el centro (Onda Gravitatoria, que lo atrae todo).</li>
 *   <li>Burbuja de Antigravedad: alrededor del jugador mientras está activa.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class AreaSpheres {
    public enum Kind { BURST, IMPLODE }

    private record Sphere(Vec3 pos, float radius, Kind kind, int life, boolean violet, int born, EnergySphere.Style style) {}

    private static final List<Sphere> LIST = new ArrayList<>();

    private AreaSpheres() {}

    public static void add(Vec3 pos, float radius, Kind kind, int life, boolean violet) {
        add(pos, radius, kind, life, violet, EnergySphere.Style.SWIRL);
    }

    public static void add(Vec3 pos, float radius, Kind kind, int life, boolean violet, EnergySphere.Style style) {
        if (radius <= 0.2F) return;
        if (LIST.size() > 40) LIST.remove(0);
        LIST.add(new Sphere(pos, radius, kind, Math.max(6, life), violet, ClientVisuals.ticks(), style));
    }

    public static void clear() { LIST.clear(); }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { LIST.clear(); return; }
        int now = ClientVisuals.ticks();
        float pt = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        boolean any = false;

        Iterator<Sphere> it = LIST.iterator();
        while (it.hasNext()) {
            Sphere s = it.next();
            float age = now - s.born() + pt;
            float t = age / s.life();
            if (t >= 1) { it.remove(); continue; }
            float radius, alpha;
            if (s.kind() == Kind.IMPLODE) {
                // Grande y se cierra hacia el centro, cada vez más brillante
                float e = t * t;
                radius = s.radius() * (1 - 0.85F * e);
                alpha = Mth.clamp(age / 4F, 0, 1) * (0.55F + 0.45F * t) * (t > 0.85F ? (1 - t) / 0.15F : 1);
            } else {
                // Crece con un frenazo al final y se desvanece
                float grow = Mth.clamp(t / 0.3F, 0, 1);
                radius = s.radius() * (0.3F + 0.7F * (1 - (1 - grow) * (1 - grow)));
                alpha = t < 0.5F ? 1.15F : 1.15F * (1 - (t - 0.5F) / 0.5F);
            }
            draw(pose, buf, s.pos(), cam, radius, s.violet(), alpha, age, s.style());
            any = true;
        }

        // Burbuja de Antigravedad alrededor de cada jugador que la tenga activa
        for (Player p : mc.level.players()) {
            if (p.isInvisible() || !Gravity.active(p, "antigravedad")) continue;
            Vec3 c = p.getPosition(pt).add(0, p.getBbHeight() / 2, 0);
            float age = ClientVisuals.activeTicks(p, Gravity.POWER_ID + "/antigravedad") + pt;
            float in = Mth.clamp(age / 6F, 0, 1);
            float radius = (1.35F + 0.04F * Mth.sin(age * 0.3F)) * (0.6F + 0.4F * in);
            draw(pose, buf, c, cam, radius, Gravity.fullOverdrive(p), 0.55F * in, age, EnergySphere.Style.CELLS);
            any = true;
        }
        if (any) EnergySphere.endBatch(buf);
    }

    static void draw(PoseStack pose, MultiBufferSource buf, Vec3 pos, Vec3 cam, float radius, boolean violet, float alpha, float age,
                     EnergySphere.Style style) {
        pose.pushPose();
        pose.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        EnergySphere.draw(pose, buf, radius, cam.subtract(pos), violet ? EnergySphere.VIOLET : EnergySphere.TEAL, alpha, age, style);
        pose.popPose();
    }
}
