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
 * /draviti lista [jugador]
 */
public final class DravitiCommand {
    private DravitiCommand() {}

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
                                .then(Commands.argument("valor", IntegerArgumentType.integer(0, Gravity.ETHER_MAX))
                                        .executes(c -> energy(c, true)))))
                .then(Commands.literal("cinetica")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("valor", IntegerArgumentType.integer(0, Gravity.KINETIC_MAX))
                                        .executes(c -> energy(c, false)))))
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
                if (add && id.equals(Gravity.POWER_ID)) { Gravity.setEther(p, Gravity.ETHER_MAX); PowerManager.sync(p); }
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
