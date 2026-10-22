package com.markadrii.draviti.gravitycore;

import com.markadrii.draviti.gravitycore.api.GravityChangerAPIForge;
import com.markadrii.draviti.gravitycore.api.IEntityGravityData;
import com.markadrii.draviti.gravitycore.api.ILevelGravityData;
import com.markadrii.draviti.gravitycore.network.S2CEntityGravityPacket;
import com.markadrii.draviti.gravitycore.network.S2CLevelGravityPacket;
import com.markadrii.draviti.gravitycore.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.LoadingModList;

/**
 * Arranque del núcleo de gravedad (lo que en Gravity Control hace GravityChangerForge, sin objetos, pociones,
 * bloques ni comandos). Si el mod Gravity Control está instalado no se arranca: se usa el suyo (ver GravityBridge).
 */
public final class GravityCore {
    private static boolean enabled;

    private GravityCore() {}

    /** Gravity Control (mod id gravitychanger) instalado: entonces sus mixins mandan y los nuestros no se aplican. */
    public static boolean externalModPresent() {
        LoadingModList list = LoadingModList.get();
        return list != null && list.getModFileById("gravitychanger") != null;
    }

    public static boolean enabled() { return enabled; }

    public static void init(IEventBus modBus) {
        if (externalModPresent()) {
            GravityChanger.LOG.info("Gravity Control detectado: D.raviti usará su gravedad");
            return;
        }
        enabled = true;
        modBus.addListener(GravityCore::registerCaps);
        modBus.addListener(GravityCore::setup);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, GravityCore::attachEntity);
        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, GravityCore::attachLevel);
        MinecraftForge.EVENT_BUS.addListener(GravityCore::login);
        MinecraftForge.EVENT_BUS.addListener(GravityCore::respawn);
        MinecraftForge.EVENT_BUS.addListener(GravityCore::dimensionChange);
        MinecraftForge.EVENT_BUS.addListener(GravityCore::tracking);
    }

    /** Lo llama EntityMixinForge al final de cada tick de entidad. */
    public static void onTick(Entity entity) {
        entity.getCapability(GravityChangerAPIForge.ENTITY_GRAVITY).ifPresent(IEntityGravityData::commonTick);
    }

    private static void setup(FMLCommonSetupEvent event) {
        Services.PLATFORM.registerClientPacket(S2CEntityGravityPacket.class, S2CEntityGravityPacket::new);
        Services.PLATFORM.registerClientPacket(S2CLevelGravityPacket.class, S2CLevelGravityPacket::new);
    }

    private static void syncLevel(ServerPlayer player, ServerLevel level) {
        level.getCapability(GravityChangerAPIForge.LEVEL_GRAVITY).ifPresent(data -> {
            CompoundTag tag = new CompoundTag();
            data.toNbt(tag);
            Services.PLATFORM.sendToClient(new S2CLevelGravityPacket(tag), player);
        });
    }

    private static void syncSelf(ServerPlayer player) {
        player.getCapability(GravityChangerAPIForge.ENTITY_GRAVITY).ifPresent(data -> {
            CompoundTag tag = new CompoundTag();
            data.toNbt(tag);
            Services.PLATFORM.sendToTracking(new S2CEntityGravityPacket(player, tag), player, true);
        });
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncLevel(player, player.serverLevel());
        syncSelf(player);
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncLevel(player, player.serverLevel());
        syncSelf(player);
    }

    private static void dimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.server.getLevel(event.getTo());
        if (level != null) syncLevel(player, level);
    }

    private static void tracking(PlayerEvent.StartTracking event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (!(player instanceof ServerPlayer sp)) return;
        target.getCapability(GravityChangerAPIForge.ENTITY_GRAVITY).ifPresent(data -> {
            CompoundTag tag = new CompoundTag();
            data.toNbt(tag);
            Services.PLATFORM.sendToClient(new S2CEntityGravityPacket(target, tag), sp);
        });
    }

    private static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(IEntityGravityData.class);
        event.register(ILevelGravityData.class);
    }

    private static void attachEntity(AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(GravityChanger.DATA_COMPONENT_ID, new EntityGravityCapability(event.getObject()));
    }

    private static void attachLevel(AttachCapabilitiesEvent<Level> event) {
        event.addCapability(GravityChanger.DIMENSION_DATA_ID, new LevelGravityCapability(event.getObject()));
    }
}
