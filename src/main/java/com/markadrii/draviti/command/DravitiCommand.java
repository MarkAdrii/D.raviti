package com.markadrii.draviti.command;

import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.data.PowerManager;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.power.Power;
import com.markadrii.draviti.power.Powers;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * /draviti dar|quitar &lt;jugadores&gt; [poder] — da o quita un poder (por defecto el Dios de la Gravedad).
 * /draviti eter &lt;jugadores&gt; &lt;valor&gt; — pone la barra de éter (y /draviti cinetica igual).
 * /draviti eter_max|cinetica_max &lt;jugadores&gt; &lt;valor&gt; — cambia el máximo de cada barra (por defecto 1000).
 * /draviti lista [jugador]
 * /draviti estrellas &lt;jugadores&gt; &lt;0-5&gt; [afinidad] — estrellas del Ether Gear.
 * /draviti afinidad &lt;jugadores&gt; &lt;valor&gt; — suma afinidad con el Ether Gear.
 * /draviti desbloquear &lt;jugadores&gt; — da por pagadas todas las técnicas (siguen pidiendo su estrella).
 * /draviti esperas &lt;jugadores&gt; — se puede volver a hablar con él y entrar en su interior ya.
 */
public final class DravitiCommand {
    private DravitiCommand() {}

    private static final int MAX_CAP = 1_000_000;

    private static final SuggestionProvider<CommandSourceStack> POWERS = (ctx, b) ->
            SharedSuggestionProvider.suggestResource(Powers.all().stream().map(Power::id), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("draviti").requires(s -> s.hasPermission(2))
                .then(Commands.literal("dar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(c -> give(c, Gravity.POWER_ID, true))
                                .then(Commands.argument("poder", ResourceLocationArgument.id()).suggests(POWERS)
                                        .executes(c -> give(c, ResourceLocationArgument.getId(c, "poder"), true)))))
                .then(Commands.literal("quitar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(c -> give(c, Gravity.POWER_ID, false))
                                .then(Commands.argument("poder", ResourceLocationArgument.id()).suggests(POWERS)
                                        .executes(c -> give(c, ResourceLocationArgument.getId(c, "poder"), false)))))
                .then(Commands.literal("eter")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("valor", IntegerArgumentType.integer(0, MAX_CAP))
                                        .executes(c -> energy(c, true)))))
                .then(Commands.literal("cinetica")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("valor", IntegerArgumentType.integer(0, MAX_CAP))
                                        .executes(c -> energy(c, false)))))
                .then(Commands.literal("eter_max")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("valor", IntegerArgumentType.integer(1, MAX_CAP))
                                        .executes(c -> cap(c, Gravity.ETHER)))))
                .then(Commands.literal("cinetica_max")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("valor", IntegerArgumentType.integer(1, MAX_CAP))
                                        .executes(c -> cap(c, Gravity.KINETIC)))))
                .then(Commands.literal("estrellas")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("estrellas", IntegerArgumentType.integer(0, 5))
                                        .executes(c -> stars(c, 0))
                                        .then(Commands.argument("afinidad", IntegerArgumentType.integer(0, 2000))
                                                .executes(c -> stars(c, IntegerArgumentType.getInteger(c, "afinidad")))))))
                .then(Commands.literal("afinidad")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("valor", IntegerArgumentType.integer(1, 100000))
                                        .executes(DravitiCommand::affinity))))
                .then(Commands.literal("desbloquear")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(DravitiCommand::unlockAll)))
                .then(Commands.literal("esperas")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(DravitiCommand::resetWaits)))
                .then(Commands.literal("lista")
                        .executes(c -> list(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(c -> list(c, EntityArgument.getPlayer(c, "jugador"))))));
    }

    private static int give(CommandContext<CommandSourceStack> c, ResourceLocation id, boolean add) throws CommandSyntaxException {
        Power power = Powers.get(id);
        if (power == null) {
            c.getSource().sendFailure(Component.translatable("command.draviti.unknown", id.toString()));
            return 0;
        }
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "jugadores");
        int n = 0;
        for (ServerPlayer p : players) {
            boolean ok = add ? PowerManager.grant(p, id) : PowerManager.revoke(p, id);
            if (ok) {
                n++;
                if (add && id.equals(Gravity.POWER_ID)) { Gravity.setEther(p, Gravity.etherMax(p)); PowerManager.sync(p); }
            }
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.translatable(add ? "command.draviti.given" : "command.draviti.removed",
                power.name(), count), true);
        return n;
    }

    private static int energy(CommandContext<CommandSourceStack> c, boolean ether) throws CommandSyntaxException {
        int v = IntegerArgumentType.getInteger(c, "valor");
        for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores")) {
            if (ether) Gravity.setEther(p, v); else Gravity.setKinetic(p, v);
            PowerManager.sync(p);
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.draviti.energy", v), true);
        return 1;
    }

    /** Cambia el máximo de una barra; si la energía actual se pasa, se baja al nuevo máximo. */
    private static int cap(CommandContext<CommandSourceStack> c, String bar) throws CommandSyntaxException {
        int v = IntegerArgumentType.getInteger(c, "valor");
        Power power = Gravity.power();
        for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores")) {
            var d = PowerCapability.get(p).orElse(null);
            if (d == null || power == null) continue;
            d.setMaxEnergy(power, bar, v);
            if (Gravity.ETHER.equals(bar)) Gravity.setEther(p, Math.min(Gravity.ether(p), Gravity.etherMax(p)));
            else Gravity.setKinetic(p, Math.min(Gravity.kinetic(p), v));
            PowerManager.sync(p);
        }
        c.getSource().sendSuccess(() -> Component.translatable(Gravity.ETHER.equals(bar)
                ? "command.draviti.ether_max" : "command.draviti.kinetic_max", v), true);
        return 1;
    }

    private static int stars(CommandContext<CommandSourceStack> c, int affinity) throws CommandSyntaxException {
        int st = IntegerArgumentType.getInteger(c, "estrellas");
        for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores"))
            com.markadrii.draviti.ethergear.EtherGear.set(p, st, affinity);
        c.getSource().sendSuccess(() -> Component.translatable("command.draviti.stars", st), true);
        return 1;
    }

    private static int affinity(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        int v = IntegerArgumentType.getInteger(c, "valor");
        for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores"))
            com.markadrii.draviti.ethergear.EtherGear.addAffinity(p, v, "message.draviti.eg.reason.command");
        c.getSource().sendSuccess(() -> Component.translatable("command.draviti.affinity", v), true);
        return 1;
    }

    /** Marca como pagadas todas las técnicas (siguen pidiendo su estrella). */
    private static int unlockAll(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Power pw = Gravity.power();
        for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores")) {
            var d = PowerCapability.get(p).orElse(null);
            if (d == null || pw == null) continue;
            for (var a : pw.abilities()) d.egBought.add(a.id());
            PowerManager.sync(p);
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.draviti.unlocked"), true);
        return 1;
    }

    private static int resetWaits(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores")) {
            var d = PowerCapability.get(p).orElse(null);
            if (d == null) continue;
            d.egTalkDay = -100;
            d.egEnterDay = -100;
            d.egBorrowDay = -100; // también el préstamo de transformación
            PowerManager.sync(p);
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.draviti.waits"), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        var d = PowerCapability.get(p).orElse(null);
        if (d == null || d.getPowers().isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("command.draviti.none", p.getDisplayName()), false);
            return 0;
        }
        for (Power pw : d.getPowers())
            c.getSource().sendSuccess(() -> Component.literal("- ").append(pw.name()).append(" (" + pw.id() + ")"), false);
        return d.getPowers().size();
    }
}
