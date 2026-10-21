package com.markadrii.draviti.client.fx;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Temblor de cámara para los golpes fuertes (se puede apagar en draviti-client.toml). */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class CameraShake {
    private static float strength;
    private static int ticks, total;
    private static final RandomSource RANDOM = RandomSource.create();

    private CameraShake() {}

    public static void add(float s, int duration) {
        if (!ClientConfig.SCREEN_SHAKE.get() || s <= 0) return;
        if (s >= strength * (ticks / (float) Math.max(1, total))) {
            strength = s;
            ticks = total = Math.max(1, duration);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticks > 0 && !Minecraft.getInstance().isPaused()) ticks--;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (ticks <= 0) return;
        float k = strength * ((ticks - (float) event.getPartialTick()) / total);
        k = Mth.clamp(k, 0, 6);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * k);
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * k);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * k * 0.6F);
    }
}
