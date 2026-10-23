package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.ethergear.EtherDialogScreen;
import com.markadrii.draviti.client.ethergear.EtherGearScreen;
import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.entity.EtherGearEntity;
import com.markadrii.draviti.ethergear.EgOption;
import com.markadrii.draviti.ethergear.EtherGearCompanion;
import com.markadrii.draviti.ethergear.EtherGearInterior;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.network.MovePacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Prueba de juego automática (-Ddraviti.playtest=true, solo para GitHub): crea un mundo plano, se da el poder y va
 * probando el menú de la U, invocar al Ether Gear, el hombro, su dimensión, hablar con él y el préstamo de
 * transformación, haciendo capturas (run/screenshots) y dejando en el log lo que ve (DRAVITI_PLAYTEST ...).
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class PlayTest {
    private static final Logger LOG = LoggerFactory.getLogger("D.raviti playtest");
    private static boolean started;
    private static int t = -1;

    private PlayTest() {}

    static boolean enabled() { return Boolean.getBoolean("draviti.playtest"); }

    @SubscribeEvent
    public static void title(ScreenEvent.Opening event) {
        if (!enabled() || started || !(event.getNewScreen() instanceof TitleScreen)) return;
        started = true;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            LOG.info("DRAVITI_PLAYTEST creando mundo");
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            LevelSettings settings = new LevelSettings("playtest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    rules, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("playtest" + System.currentTimeMillis(), settings,
                    new WorldOptions(1234L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        });
    }

    private static ServerPlayer sp(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        return server == null || mc.player == null ? null : server.getPlayerList().getPlayer(mc.player.getUUID());
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            ServerPlayer p = sp(mc);
            if (p != null) action.accept(p);
        });
    }

    private static void cmd(Minecraft mc, String command) {
        server(mc, p -> p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4), command));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> LOG.info("DRAVITI_PLAYTEST captura {}", name));
    }

    private static void state(Minecraft mc, String step) {
        var d = PowerCapability.get(mc.player).orElse(null);
        AbilityLists.update();
        LOG.info("DRAVITI_PLAYTEST {} dim={} has={} datos={} estrellas={} dentro={} invocado={} hombro={} listas={} pantalla={} prestamo={} overdrive={} completo={}",
                step, mc.level.dimension().location(), Gravity.has(mc.player), d != null,
                d == null ? -1 : d.egStars, d != null && d.egInside, d != null && d.egSummoned, d != null && d.egShoulder,
                AbilityLists.lists().size(), mc.screen == null ? "-" : mc.screen.getClass().getSimpleName(),
                d == null ? -1 : d.egBorrowTicks, Gravity.overdrive(mc.player), Gravity.fullOverdrive(mc.player));
        ServerPlayer p = sp(mc);
        var sd = p == null ? null : PowerCapability.get(p).orElse(null);
        LOG.info("DRAVITI_PLAYTEST {} servidor: datos={} has={} dentro={}", step, sd != null, p != null && Gravity.has(p), sd != null && sd.egInside);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        t++;
        try {
            step(mc);
        } catch (Throwable e) {
            LOG.error("DRAVITI_PLAYTEST error en t={}", t, e);
        }
    }

    private static void step(Minecraft mc) {
        switch (t) {
            case 60 -> {
                cmd(mc, "time set 6000");
                cmd(mc, "draviti dar @s");
                cmd(mc, "draviti estrellas @s 5");
                cmd(mc, "draviti desbloquear @s");
            }
            case 100 -> { state(mc, "con_poder"); shot(mc, "01_hud"); }
            case 110 -> mc.setScreen(new EtherGearScreen());
            case 130 -> shot(mc, "02_menu");
            case 140 -> { if (mc.screen instanceof EtherGearScreen s) s.testShowAbility("overdrive_completo"); }
            case 160 -> shot(mc, "03_habilidades");
            case 170 -> { mc.setScreen(null); ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.EG_SUMMON, 0, 0)); }
            case 175 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 230 -> { state(mc, "invocado"); shot(mc, "04_invocado"); }
            case 240 -> server(mc, p -> {
                for (var e : p.serverLevel().getEntities(com.markadrii.draviti.registry.ModEntities.ETHER_GEAR.get(), x -> true))
                    if (e.isCompanion()) EtherGearCompanion.up(p, e);
            });
            case 280 -> { state(mc, "hombro"); shot(mc, "05_hombro"); }
            case 290 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); cmd(mc, "draviti estrellas @s 1"); }
            case 300 -> ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.EG_ENTER, 0, 0));
            case 380 -> { state(mc, "dentro"); shot(mc, "06_dentro"); }
            case 390 -> server(mc, EtherGearInterior::greet);
            case 420 -> { state(mc, "dialogo"); shot(mc, "07_dialogo"); }
            case 430 -> {
                if (mc.screen instanceof EtherDialogScreen)
                    ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.EG_DIALOG, EgOption.BORROW.ordinal(), 0));
                else LOG.warn("DRAVITI_PLAYTEST no se abrió el diálogo");
            }
            case 520 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); state(mc, "prestamo"); }
            case 540 -> shot(mc, "08_prestamo");
            // ---- Epic Fight: modo combate con las capas del poder encima
            case 570 -> {
                if (net.minecraftforge.fml.ModList.get().isLoaded("epicfight"))
                    com.markadrii.draviti.compat.epicfight.EpicFightCompat.testBattleMode(true);
                else LOG.info("DRAVITI_PLAYTEST epicfight no instalado");
            }
            case 610 -> { state(mc, "ef_overdrive"); shot(mc, "09_ef_overdrive"); }
            case 620 -> {
                cmd(mc, "draviti estrellas @s 5");
                server(mc, p -> com.markadrii.draviti.data.PowerManager.onKey(p, Gravity.POWER_ID, "overdrive_completo", true));
            }
            case 700 -> { state(mc, "ef_completo"); shot(mc, "10_ef_completo"); }
            case 705 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 712 -> shot(mc, "11_ef_completo_espalda");
            case 720 -> server(mc, p -> com.markadrii.draviti.data.PowerManager.onKey(p, Gravity.POWER_ID, "puno_gravitatorio", true));
            case 728 -> shot(mc, "12_ef_puno");
            // ---- Cursed Crown: Powers (si está): teclas y barra compartidas
            case 760 -> {
                LOG.info("DRAVITI_PLAYTEST cursed instalado={} teclas_compartidas={}", CursedCompat.loaded(), CursedCompat.keysShared());
                if (CursedCompat.loaded()) cmd(mc, "cursedpowers add @s cursedpowers:aqua");
            }
            case 800 -> {
                LOG.info("DRAVITI_PLAYTEST cursed listas_suyas={} total={}", CursedCompat.theirListCount(), ClientEvents.totalLists());
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            case 810 -> shot(mc, "13_barra");
            // ---- Entrar acompañado (5 estrellas): un cerdo pegado al jugador entra y sale con él
            case 830 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); cmd(mc, "summon minecraft:pig ~0.6 ~ ~0.6 {NoAI:1b}"); }
            case 840 -> ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.EG_ENTER, 1, 0));
            case 920 -> server(mc, p -> LOG.info("DRAVITI_PLAYTEST acompanado dentro={} cerdos_dentro={}", EtherGearInterior.isInside(p),
                    p.serverLevel().getEntities(net.minecraft.world.entity.EntityType.PIG, x -> true).size()));
            case 925 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
            case 935 -> shot(mc, "14_acompanado");
            case 940 -> server(mc, EtherGearInterior::exit);
            case 1000 -> server(mc, p -> LOG.info("DRAVITI_PLAYTEST acompanado vuelta dim={} cerdos_aqui={}", p.level().dimension().location(),
                    p.serverLevel().getEntities(net.minecraft.world.entity.EntityType.PIG, x -> true).size()));
            case 1010 -> { state(mc, "fin"); LOG.info("DRAVITI_PLAYTEST_DONE"); mc.stop(); }
            default -> {}
        }
    }
}
