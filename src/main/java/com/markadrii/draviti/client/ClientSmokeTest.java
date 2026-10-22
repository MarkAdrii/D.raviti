package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.SmokeTest;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Prueba automática (-Ddraviti.smoketest=true): al abrirse la primera pantalla tras la carga (menú principal o, la
 * primera vez, la de accesibilidad) carga las clases de los mixins y sale.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static boolean done;

    private ClientSmokeTest() {}

    @SubscribeEvent
    public static void title(ScreenEvent.Opening event) {
        if (done || !SmokeTest.enabled() || event.getNewScreen() == null) return;
        done = true;
        SmokeTest.run(true);
        Minecraft.getInstance().stop();
    }
}
