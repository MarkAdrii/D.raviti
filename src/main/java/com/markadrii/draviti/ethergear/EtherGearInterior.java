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
 * Se entra desde el menú de la U con 1 estrella, cuando quieras. Charlar ahí no da afinidad; lo único que hace algo es
 * pedirle que te preste la transformación (cada 5 días).
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
        /** Criaturas (y jugadores) que entraron acompañándote y a dónde devolverlas. */
        final java.util.List<Guest> guests = new java.util.ArrayList<>();

        Session(BlockPos origin) { this.origin = origin; }
    }

    private record Guest(UUID id, boolean player, ResourceKey<Level> dim, double x, double y, double z, float yaw) {}

    /** Estrellas para "Entrar acompañado" y distancia (en bloques, desde tu cuerpo) de lo que entra contigo. */
    public static final int COMPANY_STARS = 5;
    public static final double COMPANY_RANGE = 1.0;

    private EtherGearInterior() {}

    public static boolean isInside(Entity e) { return e.level().dimension() == KEY; }

    public static boolean isSessionEntity(UUID owner, UUID entity) {
        Session s = SESSIONS.get(owner);
        return s != null && entity.equals(s.entityId);
    }

    /** Con 1 estrella se puede entrar cuando quieras (0 = ya). */
    public static long daysLeft(PowerData d, long today) { return 0; }

    // ------------------------------------------------------------------ entrar / salir

    public static void enter(ServerPlayer p) { enter(p, false); }

    /** withCompany: con 5 estrellas, también entran las criaturas que estén a menos de 1 bloque de ti. */
    public static void enter(ServerPlayer p, boolean withCompany) {
        PowerData d = Gravity.data(p);
        if (d == null || !Gravity.has(p) || d.egInside || isInside(p)) return;
        if (d.egStars < ENTER_STARS) {
            p.displayClientMessage(Component.translatable("message.draviti.eg.enter_stars", ENTER_STARS).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long today = EtherGear.today(p.server);
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
        Session session = new Session(origin);
        SESSIONS.put(p.getUUID(), session);

        // Lo que entra contigo se elige antes de moverte (a menos de 1 bloque de tu cuerpo)
        java.util.List<net.minecraft.world.entity.LivingEntity> company = new java.util.ArrayList<>();
        if (withCompany && d.egStars >= COMPANY_STARS) {
            company.addAll(p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    p.getBoundingBox().inflate(COMPANY_RANGE), e -> e != p && e.isAlive() && !e.isSpectator()
                            && !(e instanceof EtherGearEntity) && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                            && !isInside(e) && !(e instanceof ServerPlayer sp && Gravity.data(sp) != null && Gravity.data(sp).egInside)));
        }

        p.teleportTo(inner, origin.getX() + 0.5, origin.getY(), origin.getZ() - 8 + 0.5, 0F, 0F);
        int i = 0;
        for (var e : company) {
            double ang = i++ * 2.4, r = 1.6 + (i % 3) * 0.6;
            double gx = origin.getX() + 0.5 + Math.cos(ang) * r, gz = origin.getZ() - 8 + 0.5 + Math.sin(ang) * r;
            Guest g = new Guest(e.getUUID(), e instanceof ServerPlayer, e.level().dimension(), e.getX(), e.getY(), e.getZ(), e.getYRot());
            if (e instanceof ServerPlayer gp) {
                PowerData gd = Gravity.data(gp);
                if (gd != null) {
                    gd.egReturnDim = gp.level().dimension().location().toString();
                    gd.egRx = gp.getX(); gd.egRy = gp.getY(); gd.egRz = gp.getZ(); gd.egRyaw = gp.getYRot();
                    gd.egInside = true;
                }
                gp.teleportTo(inner, gx, origin.getY(), gz, 0F, 0F);
                gp.sendSystemMessage(Component.translatable("message.draviti.eg.guest", p.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                PowerManager.sync(gp);
                com.markadrii.draviti.data.CommonEvents.resyncLater(gp);
            } else {
                e.teleportTo(inner, gx, origin.getY(), gz, java.util.Set.of(), e.getYRot(), 0F);
            }
            session.guests.add(g);
        }
        if (!company.isEmpty())
            p.sendSystemMessage(Component.translatable("message.draviti.eg.enter_company", company.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        inner.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.5F);
        inner.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1F, 0.6F);
        p.sendSystemMessage(Component.translatable("message.draviti.eg.enter").withStyle(ChatFormatting.LIGHT_PURPLE));
        PowerManager.sync(p);
        com.markadrii.draviti.data.CommonEvents.resyncLater(p);
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
        com.markadrii.draviti.data.CommonEvents.resyncLater(p);
    }

    private static void cleanup(MinecraftServer server, UUID owner) {
        Session s = SESSIONS.remove(owner);
        if (s == null) return;
        returnGuests(server, s);
        if (s.entityId == null) return;
        ServerLevel inner = server.getLevel(KEY);
        if (inner != null) {
            Entity e = inner.getEntity(s.entityId);
            if (e != null) e.discard();
        }
    }

    /** Al salir (o desconectarte, morir...) lo que entró contigo vuelve a donde estaba. */
    private static void returnGuests(MinecraftServer server, Session s) {
        ServerLevel inner = server.getLevel(KEY);
        for (Guest g : s.guests) {
            if (g.player()) {
                ServerPlayer gp = server.getPlayerList().getPlayer(g.id());
                if (gp != null && isInside(gp)) exit(gp);
                continue;
            }
            Entity e = inner == null ? null : inner.getEntity(g.id());
            ServerLevel dest = server.getLevel(g.dim());
            if (e == null || !e.isAlive() || dest == null) continue;
            e.teleportTo(dest, g.x(), g.y(), g.z(), java.util.Set.of(), g.yaw(), 0F);
        }
        s.guests.clear();
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
            // Solo si te falta alguna de las dos transformaciones
            if (o == EgOption.BORROW && EtherGear.missingForm(d) == 0) continue;
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
            }
            case WHAT -> {
                d.egVisit |= o.bit;
                dialog(p, Component.translatable(k + "what." + r.nextInt(3)), options(d));
            }
            case ASK_OVERDRIVE, ASK_WHY, ASK_ETHER -> {
                d.egVisit |= o.bit;
                String q = o.name().substring(4).toLowerCase();
                dialog(p, Component.translatable(k + "ask." + q + "." + r.nextInt(2)), options(d));
            }
            case BORROW -> {
                int form = EtherGear.missingForm(d);
                if (form == 0) return;
                long left = EtherGear.borrowDaysLeft(d, EtherGear.today(p.server));
                if (left > 0) {
                    dialog(p, Component.translatable(k + "borrow.wait", left), options(d));
                    return;
                }
                // Te saca de su interior y te presta su éter
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                        EtherGearPacket.close(Component.translatable(k + "borrow." + form)));
                exit(p);
                EtherGear.startBorrow(p, form);
                return;
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
