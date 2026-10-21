package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del cliente durante la partida (pulsar teclas, HUD, etc.). */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Aquí se comprueban las teclas: while (KeyBindings.MI_TECLA.consumeClick()) { ... }
    }
}
