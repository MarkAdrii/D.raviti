package com.markadrii.draviti.ethergear;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.data.PowerManager;
import com.markadrii.draviti.entity.EtherGearEntity;
import com.markadrii.draviti.gravity.Fx;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El Ether Gear invocado fuera (botón Invocar del menú de la U, como el mini golem de Seals): flota a tu lado y te
 * sigue. Shift + clic derecho con la mano vacía sobre él: se sube a tu hombro; shift + clic derecho con la mano vacía
 * (en un bloque o en el aire): se baja. Desaparece al salir del juego y vuelve contigo al entrar.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID)
public final class EtherGearCompanion {
    private static final Map<UUID, UUID> ENTITY = new HashMap<>();

    private EtherGearCompanion() {}

    public static boolean isActive(ServerPlayer p, EtherGearEntity e) {
        PowerData d = Gravity.data(p);
        return d != null && d.egSummoned && !d.egShoulder && Gravity.has(p) && e.getUUID().equals(ENTITY.get(p.getUUID()))
                && e.level() == p.level() && !EtherGearInterior.isInside(p);
    }

    private static Entity find(ServerPlayer p) {
        UUID id = ENTITY.get(p.getUUID());
        return id == null ? null : p.serverLevel().getEntity(id);
    }

    /** Botón Invocar / Desinvocar. */
    public static void toggle(ServerPlayer p) {
        PowerData d = Gravity.data(p);
        if (d == null || !Gravity.has(p)) return;
        if (d.egSummoned) {
            despawn(p, true);
            d.egSummoned = false;
            d.egShoulder = false;
            p.displayClientMessage(Component.translatable("message.draviti.eg.unsummoned").withStyle(ChatFormatting.GRAY), true);
            PowerManager.sync(p);
            return;
        }
        if (EtherGearInterior.isInside(p)) return;
        d.egSummoned = true;
        d.egShoulder = false;
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        spawn(p, p.position().add(look.scale(1.5)).add(0, 1, 0));
        Gravity.sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1F, 1.4F);
        p.displayClientMessage(Component.translatable("message.draviti.eg.summoned").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        PowerManager.sync(p);
    }

    static void spawn(ServerPlayer p, Vec3 at) {
        despawn(p, false);
        EtherGearEntity e = ModEntities.ETHER_GEAR.get().create(p.serverLevel());
        if (e == null) return;
        e.setOwner(p);
        e.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        ENTITY.put(p.getUUID(), e.getUUID());
        p.serverLevel().addFreshEntity(e);
        Gravity.fxOn(p, Fx.WEIGHTLESS, e, e.getBbHeight(), 20);
    }

    static void despawn(ServerPlayer p, boolean effects) {
        Entity e = find(p);
        if (e != null) {
            if (effects) Gravity.fx(p, Fx.DEFLECT, e.position().add(0, 0.6, 0), 1, 0);
            e.discard();
        }
        ENTITY.remove(p.getUUID());
    }

    /** Al hombro. */
    public static void up(ServerPlayer p, EtherGearEntity e) {
        PowerData d = Gravity.data(p);
        if (d == null || !d.egSummoned || d.egShoulder) return;
        despawn(p, false);
        d.egShoulder = true;
        Gravity.sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1F, 1.6F);
        PowerManager.sync(p);
    }

    /** Del hombro al suelo (o delante de ti). */
    public static void down(ServerPlayer p, Vec3 at) {
        PowerData d = Gravity.data(p);
        if (d == null || !d.egSummoned || !d.egShoulder || !p.getMainHandItem().isEmpty()) return;
        d.egShoulder = false;
        if (at == null) at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5)).add(0, 1, 0);
        spawn(p, at);
        Gravity.sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1F, 1.2F);
        PowerManager.sync(p);
    }

    /** Vuelve a aparecer con su dueño (al entrar, al cambiar de dimensión...). */
    public static void respawnIfNeeded(ServerPlayer p) {
        PowerData d = Gravity.data(p);
        if (d == null || !d.egSummoned || d.egShoulder || !Gravity.has(p) || EtherGearInterior.isInside(p)) return;
        if (find(p) instanceof EtherGearEntity e && e.isAlive()) return;
        spawn(p, p.position().add(0, 1.5, 0));
    }

    // ------------------------------------------------------------------ eventos

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND
                || !p.getMainHandItem().isEmpty()) return;
        PowerData d = Gravity.data(p);
        if (d == null || !d.egSummoned || !d.egShoulder) return;
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        down(p, Vec3.atBottomCenterOf(event.getPos().relative(face)).add(0, 0.5, 0));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) respawnIfNeeded(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) despawn(p, false);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ENTITY.remove(p.getUUID()); // el de la otra dimensión desaparece solo
        respawnIfNeeded(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ENTITY.remove(p.getUUID());
            respawnIfNeeded(p);
        }
    }
}
