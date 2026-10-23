package com.markadrii.draviti.data;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.command.DravitiCommand;
import com.markadrii.draviti.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = Draviti.MOD_ID)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PowerCapability.Provider provider = new PowerCapability.Provider();
            event.addCapability(PowerCapability.KEY, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        PowerCapability.get(event.getOriginal()).ifPresent(old ->
                PowerCapability.get(event.getEntity()).ifPresent(now -> now.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) PowerManager.disableAll(sp);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) { PowerManager.sync(sp); resyncLater(sp); }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) { PowerManager.sync(sp); resyncLater(sp); }
    }

    /**
     * Al cambiar de dimensión el cliente crea un jugador nuevo (sin datos). Se manda todo otra vez al momento y unos
     * ticks después, por si el primer envío llega antes de que el cliente haya cambiado de jugador.
     */
    private static final java.util.Map<java.util.UUID, Integer> RESYNC = new java.util.concurrent.ConcurrentHashMap<>();

    public static void resyncLater(ServerPlayer sp) { RESYNC.put(sp.getUUID(), 10); }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) { PowerManager.sync(sp); resyncLater(sp); }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher)
            PowerCapability.get(target).ifPresent(d -> ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> watcher), PowerManager.visualState(target, d)));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer sp) {
            PowerManager.tick(sp);
            Integer left = RESYNC.get(sp.getUUID());
            if (left != null) {
                if (left <= 1) { RESYNC.remove(sp.getUUID()); PowerManager.sync(sp); }
                else RESYNC.put(sp.getUUID(), left - 1);
            }
            com.markadrii.draviti.ethergear.EtherGear.tick(sp);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        DravitiCommand.register(event.getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = Draviti.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerCaps(RegisterCapabilitiesEvent event) {
            event.register(PowerData.class);
        }
    }
}
