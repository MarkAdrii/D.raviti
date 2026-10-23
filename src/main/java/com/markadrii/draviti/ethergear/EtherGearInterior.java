package com.markadrii.draviti.ethergear;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.data.PowerManager;
import com.markadrii.draviti.entity.EtherGearEntity;
import com.markadrii.draviti.gravity.Fx;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.gravity.GravitySwap;
import com.markadrii.draviti.network.EtherGearPacket;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El interior del Ether Gear: un vacío oscuro con una plataforma negra recorrida por líneas de éter moradas. En el
 * centro flota el Ether Gear; clic derecho sobre él (o acercarse) para hablar. Cada jugador tiene su zona.
 * Se entra desde el menú de la U con 1 estrella, cada pocos días.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID)
public final class EtherGearInterior {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, Draviti.id("ether_gear"));
    public static final int ENTER_STARS = 1;
    public static final int FLOOR_Y = 64, RADIUS = 14, HEIGHT = 18, SPACING = 2048;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private static final class Session {
        final BlockPos origin;
        UUID entityId;
        int spawnDelay = 2, respawns;
        boolean greeted;

        Session(BlockPos origin) { this.origin = origin; }
    }

    private EtherGearInterior() {}

    public static boolean isInside(Entity e) { return e.level().dimension() == KEY; }

    public static boolean isSessionEntity(UUID owner, UUID entity) {
        Session s = SESSIONS.get(owner);
        return s != null && entity.equals(s.entityId);
    }

    public static int daysBetweenVisits() { return (int) EtherGear.stat("dias_entre_visitas", 3); }

    /** Días que faltan para poder entrar (0 = ya). Vale en el cliente con el día del mundo. */
    public static long daysLeft(PowerData d, long today) {
        return Math.max(0, d.egEnterDay + daysBetweenVisits() - today);
    }

    // ------------------------------------------------------------------ entrar / salir

    public static void enter(ServerPlayer p) {
        PowerData d = Gravity.data(p);
        if (d == null || !Gravity.has(p) || d.egInside || isInside(p)) return;
        if (d.egStars < ENTER_STARS) {
            p.displayClientMessage(Component.translatable("message.draviti.eg.enter_stars", ENTER_STARS).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long today = EtherGear.today(p.server);
        long left = daysLeft(d, today);
        if (left > 0) {
            p.displayClientMessage(Component.translatable("message.draviti.eg.enter_wait", left).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerLevel inner = p.server.getLevel(KEY);
        if (inner == null) {
            p.displayClientMessage(Component.translatable("message.draviti.eg.unavailable").withStyle(ChatFormatting.RED), true);
            return;
        }
        // Gravedad normal antes de entrar
        GravitySwap.reset(p, false);
        int index = EtherGearRegions.get(p.server).regionOf(p.getUUID());
        BlockPos origin = new BlockPos((index + 1) * SPACING, FLOOR_Y, 0);
        cleanup(p.server, p.getUUID());
        build(inner, origin);

        d.egReturnDim = p.level().dimension().location().toString();
        d.egRx = p.getX(); d.egRy = p.getY(); d.egRz = p.getZ(); d.egRyaw = p.getYRot();
        d.egInside = true;
        d.egVisit = 0;
        d.egEnterDay = today;
        SESSIONS.put(p.getUUID(), new Session(origin));

        p.teleportTo(inner, origin.getX() + 0.5, origin.getY(), origin.getZ() - 8 + 0.5, 0F, 0F);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        inner.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.5F);
        inner.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1F, 0.6F);
        p.sendSystemMessage(Component.translatable("message.draviti.eg.enter").withStyle(ChatFormatting.LIGHT_PURPLE));
        PowerManager.sync(p);
    }

    public static void exit(ServerPlayer p) {
        PowerData d = Gravity.data(p);
        cleanup(p.server, p.getUUID());
        if (d == null) return;
        boolean wasInside = d.egInside;
        d.egInside = false;
        if (isInside(p)) {
            ServerLevel dest = null;
            ResourceLocation dim = ResourceLocation.tryParse(d.egReturnDim);
            if (dim != null) dest = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
            double x = d.egRx, y = d.egRy, z = d.egRz;
            if (!wasInside || dest == null || dest.dimension() == KEY) {
                dest = p.server.overworld();
                BlockPos spawn = dest.getSharedSpawnPos();
                x = spawn.getX() + 0.5; y = spawn.getY(); z = spawn.getZ() + 0.5;
            }
            p.teleportTo(dest, x, y, z, d.egRyaw, 0F);
            dest.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1.2F);
        }
        PowerManager.sync(p);
    }

    private static void cleanup(MinecraftServer server, UUID owner) {
        Session s = SESSIONS.remove(owner);
        if (s == null || s.entityId == null) return;
        ServerLevel inner = server.getLevel(KEY);
        if (inner != null) {
            Entity e = inner.getEntity(s.entityId);
            if (e != null) e.discard();
        }
    }

    private static void spawnEntity(ServerLevel inner, Session s) {
        EtherGearEntity e = ModEntities.ETHER_GEAR.get().create(inner);
        if (e == null) return;
        BlockPos o = s.origin;
        e.moveTo(o.getX() + 0.5, o.getY() + 0.6, o.getZ() + 4.5, 180F, 0F);
        e.setYHeadRot(180F);
        e.setYBodyRot(180F);
        s.entityId = e.getUUID();
        inner.addFreshEntity(e);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        MinecraftServer server = event.getServer();
        ServerLevel inner = server.getLevel(KEY);
        if (inner == null) return;
        for (Map.Entry<UUID, Session> en : SESSIONS.entrySet()) {
            Session s = en.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null || p.level() != inner) continue;
            if (s.spawnDelay > 0) {
                if (--s.spawnDelay == 0) spawnEntity(inner, s);
                continue;
            }
            Entity e = s.entityId == null ? null : inner.getEntity(s.entityId);
            if ((e == null || !e.isAlive()) && p.tickCount % 20 == 0 && s.respawns < 5) {
                s.respawns++;
                spawnEntity(inner, s);
                continue;
            }
            // Se cae de la plataforma: vuelve al centro
            if (p.getY() < FLOOR_Y - 20) p.teleportTo(s.origin.getX() + 0.5, s.origin.getY(), s.origin.getZ() - 8 + 0.5);
            // Al acercarse por primera vez, te saluda
            if (!s.greeted && e != null && e.distanceToSqr(p) < 6 * 6) {
                s.greeted = true;
                greet(p);
            }
        }
    }

    // ------------------------------------------------------------------ diálogo

    /** Clic derecho sobre el Ether Gear o acercarse. */
    public static void greet(ServerPlayer p) {
        PowerData d = Gravity.data(p);
        if (d == null || !d.egInside) return;
        dialog(p, Component.translatable("eg.draviti.inner.greet." + Math.min(3, p.getRandom().nextInt(4))), options(d));
    }

    private static List<Integer> options(PowerData d) {
        List<Integer> list = new ArrayList<>();
        for (EgOption o : EgOption.values()) {
            if (o.bit != 0 && (d.egVisit & o.bit) != 0) continue;
            list.add(o.ordinal());
        }
        return list;
    }

    private static void dialog(ServerPlayer p, Component line, List<Integer> options) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), EtherGearPacket.dialog(line, options));
    }

    public static void choose(ServerPlayer p, int id) {
        PowerData d = Gravity.data(p);
        EgOption o = EgOption.byId(id);
        if (d == null || o == null || !d.egInside || !isInside(p)) return;
        if (o.bit != 0 && (d.egVisit & o.bit) != 0) return;
        String k = "eg.draviti.inner.";
        var r = p.getRandom();
        switch (o) {
            case HOW -> {
                d.egVisit |= o.bit;
                dialog(p, Component.translatable(k + "how." + d.egStars), options(d));
                EtherGear.addAffinity(p, (int) EtherGear.stat("afinidad_charla", 5), "message.draviti.eg.reason.chat");
            }
            case WHAT -> {
                d.egVisit |= o.bit;
                dialog(p, Component.translatable(k + "what." + r.nextInt(3)), options(d));
                EtherGear.addAffinity(p, (int) EtherGear.stat("afinidad_charla", 5), "message.draviti.eg.reason.chat");
            }
            case MEDITATE -> {
                d.egVisit |= o.bit;
                Gravity.setEther(p, Gravity.etherMax(p));
                p.heal(p.getMaxHealth());
                Gravity.fxOn(p, Fx.WEIGHTLESS, p, p.getBbHeight(), 60);
                Gravity.sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 1F, 0.7F);
                dialog(p, Component.translatable(k + "meditate"), options(d));
                EtherGear.addAffinity(p, (int) EtherGear.stat("afinidad_meditar", 20), "message.draviti.eg.reason.meditate");
            }
            case NEXT -> {
                Component line = EtherGear.maxed(d) || d.egStars >= EtherGear.MAX_STARS
                        ? Component.translatable(k + "next.max")
                        : Component.translatable(k + "next", EtherGear.affinityMax(d.egStars) - d.egAffinity, d.egStars + 1);
                dialog(p, line, options(d));
            }
            case EXIT -> {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                        EtherGearPacket.close(Component.translatable(k + "bye")));
                exit(p);
                return;
            }
            case BYE -> { }
        }
        PowerManager.sync(p);
    }

    // ------------------------------------------------------------------ eventos

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        // Se salió del juego dentro: vuelve a donde estaba
        PowerData d = Gravity.data(p);
        if ((d != null && d.egInside) || isInside(p)) exit(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) cleanup(p.server, p.getUUID());
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PowerData d = Gravity.data(p);
            cleanup(p.server, p.getUUID());
            if (d != null) d.egInside = false;
        }
    }

    @SubscribeEvent
    public static void changedDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getFrom() == KEY && event.getTo() != KEY) {
            PowerData d = Gravity.data(p);
            cleanup(p.server, p.getUUID());
            if (d != null && d.egInside) { d.egInside = false; PowerManager.sync(p); }
        }
    }

    // ------------------------------------------------------------------ construcción

    /**
     * Plataforma circular negra con el dibujo del Ether Gear en morado: dos anillos, seis radios y nodos de obsidiana
     * llorosa. Luces invisibles para que se vea, y barreras alrededor.
     */
    private static void build(ServerLevel level, BlockPos o) {
        BlockState black = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState line = Blocks.PURPLE_CONCRETE.defaultBlockState();
        BlockState glow = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState rim = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 11);
        int outer = RADIUS + 2;
        for (int x = -outer; x <= outer; x++) {
            for (int z = -outer; z <= outer; z++) {
                double dist = Math.sqrt(x * x + z * z);
                BlockState floor;
                if (dist > RADIUS + 0.5) floor = dist <= RADIUS + 1.5 ? barrier : air;
                else if (dist > RADIUS - 0.5) floor = rim;
                else floor = pattern(x, z, dist) == 2 ? glow : pattern(x, z, dist) == 1 ? line : black;
                level.setBlock(o.offset(x, -1, z), floor, 2);
                for (int y = 0; y <= HEIGHT; y++) {
                    BlockState want = dist > RADIUS + 0.5 && dist <= RADIUS + 1.5 ? barrier
                            : y == HEIGHT && dist <= RADIUS + 0.5 ? barrier : air;
                    BlockPos pos = o.offset(x, y, z);
                    if (level.getBlockState(pos) != want) level.setBlock(pos, want, 2);
                }
            }
        }
        for (int x = -12; x <= 12; x += 6)
            for (int z = -12; z <= 12; z += 6)
                if (x * x + z * z <= RADIUS * RADIUS) level.setBlock(o.offset(x, 4, z), light, 2);
    }

    /** 0 = negro, 1 = línea de éter, 2 = nodo brillante. */
    private static int pattern(int x, int z, double dist) {
        if (dist < 1.5) return 2;
        boolean ring = Math.abs(dist - 5) < 0.5 || Math.abs(dist - 10) < 0.5;
        double ang = Math.toDegrees(Math.atan2(z, x));
        boolean spoke = false;
        for (int i = 0; i < 6; i++) {
            double a = Math.abs(((ang - (i * 60 + 30)) % 360 + 540) % 360 - 180);
            if (dist > 1.5 && dist < 10.5 && Math.toRadians(a) * dist < 0.55) spoke = true;
        }
        if ((ring && spoke) || (ring && Math.abs(dist - 10) < 0.5 && ((int) Math.round(ang + 360) % 60) < 4)) return 2;
        return ring || spoke ? 1 : 0;
    }
}
