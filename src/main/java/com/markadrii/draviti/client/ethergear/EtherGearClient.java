package com.markadrii.draviti.client.ethergear;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ModKeys;
import com.markadrii.draviti.entity.EtherGearEntity;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.network.EtherGearPacket;
import com.markadrii.draviti.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cliente del Ether Gear: tecla U, lo último que ha dicho y la entidad que se dibuja en el menú. */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class EtherGearClient {
    private static Component lastLine;
    private static EtherGearEntity preview;

    private EtherGearClient() {}

    public static Component lastLine() { return lastLine; }
    public static void clearLine() { lastLine = null; }

    /** Entidad solo para dibujarla en el menú (no está en el mundo). */
    public static EtherGearEntity preview() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        if (preview == null || preview.level() != mc.level) preview = ModEntities.ETHER_GEAR.get().create(mc.level);
        return preview;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { preview = null; return; }
        if (preview != null) preview.tickCount++;
        while (ModKeys.ETHER_GEAR.consumeClick()) {
            if (mc.screen == null && Gravity.has(mc.player)) mc.setScreen(new EtherGearScreen());
        }
    }

    public static void receive(EtherGearPacket p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.kind()) {
            case EtherGearPacket.LINE -> lastLine = p.line();
            case EtherGearPacket.DIALOG -> {
                if (mc.screen instanceof EtherDialogScreen s) s.update(p.line(), p.options());
                else mc.setScreen(new EtherDialogScreen(p.line(), p.options()));
            }
            case EtherGearPacket.CLOSE -> {
                if (mc.screen instanceof EtherDialogScreen) mc.setScreen(null);
                if (mc.player != null)
                    mc.player.displayClientMessage(Component.translatable("entity.draviti.ether_gear").append(": ").append(p.line()), true);
            }
            default -> {}
        }
    }
}
