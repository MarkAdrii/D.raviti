package com.markadrii.draviti.data;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.AbilityType;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.network.SyncPowerDataPacket;
import com.markadrii.draviti.network.VisualStatePacket;
import com.markadrii.draviti.power.Power;
import com.markadrii.draviti.power.Powers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Lógica de servidor: dar/quitar poderes, teclas y tick (misma lógica que Cursed Crown: Powers). */
public final class PowerManager {
    private PowerManager() {}

    public static void sync(ServerPlayer player) {
        PowerCapability.get(player).ifPresent(d -> {
            d.dirty = false;
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPowerDataPacket(d));
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), visualState(player, d));
        });
    }

    /** Lista de habilidades activas ("poder/habilidad") para que los demás vean los efectos. */
    public static VisualStatePacket visualState(ServerPlayer player, PowerData d) {
        List<String> active = new ArrayList<>();
        for (Power p : d.getPowers())
            for (Ability a : p.abilities())
                if (d.state(p, a).enabled) active.add(PowerData.key(p, a));
        // Que los demás sepan que tiene el poder (marca del Ether Gear en el brazo) y si va en vuelo sónico
        for (Power p : d.getPowers()) active.add(p.id() + "/_has");
        if (d.showMark && d.hasPower(com.markadrii.draviti.gravity.Gravity.POWER_ID))
            active.add(com.markadrii.draviti.gravity.Gravity.POWER_ID + "/_mark");
        var swap = com.markadrii.draviti.gravity.GravitySwap.get(player);
        if (swap != null) active.add(com.markadrii.draviti.gravity.Gravity.POWER_ID + "/swap_" + swap.getName());
        if (com.markadrii.draviti.gravity.GravityAbilities.SONIC.contains(player.getUUID()))
            active.add(com.markadrii.draviti.gravity.Gravity.POWER_ID + "/vuelo_sonico");
        return new VisualStatePacket(player.getId(), active);
    }

    public static boolean has(ServerPlayer player, ResourceLocation id) {
        return PowerCapability.get(player).map(d -> d.hasPower(id)).orElse(false);
    }

    public static boolean grant(ServerPlayer player, ResourceLocation id) {
        boolean ok = PowerCapability.get(player).map(d -> d.addPower(id)).orElse(false);
        if (ok) sync(player);
        return ok;
    }

    public static boolean revoke(ServerPlayer player, ResourceLocation id) {
        boolean ok = PowerCapability.get(player).map(d -> {
            if (!d.hasPower(id)) return false;
            shutdown(player, d, id);
            d.removePower(id);
            return true;
        }).orElse(false);
        if (ok) sync(player);
        return ok;
    }

    /** Desactiva las habilidades de un poder que el jugador acaba de perder. */
    private static void shutdown(ServerPlayer player, PowerData d, ResourceLocation id) {
        Power power = Powers.get(id);
        if (power == null) return;
        for (Ability a : power.abilities()) {
            AbilityState s = d.state(power, a);
            if (s.enabled) { s.enabled = false; a.behavior().onDeactivate(player, a); }
        }
        d.removeStatesOf(power);
    }

    /** Desactiva todas las habilidades activas (al morir, por ejemplo). */
    public static void disableAll(ServerPlayer player) {
        PowerCapability.get(player).ifPresent(d -> {
            for (Power p : d.getPowers())
                for (Ability a : p.abilities()) {
                    AbilityState s = d.state(p, a);
                    if (s.enabled) { s.enabled = false; s.activeTicks = 0; a.behavior().onDeactivate(player, a); }
                }
        });
    }

    /** Apaga una habilidad desde código (p. ej. Overdrive sin éter). */
    public static void forceStop(ServerPlayer player, Power power, Ability ability, boolean cooldown) {
        PowerCapability.get(player).ifPresent(d -> {
            AbilityState s = d.state(power, ability);
            if (!s.enabled) return;
            s.enabled = false; s.activeTicks = 0; s.activationTimer = 0; s.released = false;
            ability.behavior().onDeactivate(player, ability);
            if (cooldown) startCooldown(player, s, ability);
            sync(player);
        });
    }

    public static void onKey(ServerPlayer player, ResourceLocation powerId, String abilityId, boolean pressed) {
        Power power = Powers.get(powerId);
        if (power == null || player.isSpectator()) return;
        Ability ability = power.ability(abilityId).orElse(null);
        if (ability == null) return;
        PowerCapability.get(player).ifPresent(d -> {
            if (!d.hasPower(powerId)) return;
            AbilityState s = d.state(power, ability);
            if (pressed && !d.isUnlocked(power, ability)) return;
            boolean changed = false;
            switch (ability.type()) {
                case ACTION -> {
                    if (pressed && ability.behavior().bypassCooldown(player, ability)) {
                        ability.behavior().onActivate(player, ability);
                    } else if (pressed && s.cooldown <= 0) {
                        if (ability.behavior().tryAction(player, ability)) {
                            startCooldown(player, s, ability);
                            changed = true;
                        }
                    }
                }
                case TOGGLE -> {
                    if (!pressed) break;
                    if (s.enabled) {
                        s.enabled = false; s.activeTicks = 0;
                        ability.behavior().onDeactivate(player, ability);
                        startCooldown(player, s, ability);
                        changed = true;
                    } else if (s.cooldown <= 0 && ability.behavior().canActivate(player, ability)) {
                        s.enabled = true; s.activeTicks = 0;
                        ability.behavior().onActivate(player, ability);
                        changed = true;
                    }
                }
                case ACTIVATION -> {
                    if (pressed && !s.enabled && s.cooldown <= 0 && ability.behavior().canActivate(player, ability)) {
                        s.enabled = true; s.activeTicks = 0;
                        s.activationTimer = Math.max(1, ability.activationTicks());
                        s.maxActivationTimer = s.activationTimer;
                        ability.behavior().onActivate(player, ability);
                        changed = true;
                    }
                }
                case HELD -> {
                    if (pressed && s.enabled) {
                        s.released = false; // se vuelve a mantener: sigue activa
                    } else if (pressed && !s.enabled && s.cooldown <= 0 && ability.behavior().canActivate(player, ability)) {
                        s.enabled = true; s.activeTicks = 0; s.released = false;
                        ability.behavior().onActivate(player, ability);
                        changed = true;
                    } else if (!pressed && s.enabled && s.activeTicks < ability.statI("min_duration")) {
                        s.released = true; // dura al menos min_duration ticks
                    } else if (!pressed && s.enabled) {
                        s.enabled = false; s.activeTicks = 0; s.released = false;
                        ability.behavior().onDeactivate(player, ability);
                        startCooldown(player, s, ability);
                        changed = true;
                    }
                }
            }
            if (changed) sync(player);
        });
    }

    private static void startCooldown(ServerPlayer player, AbilityState s, Ability a) {
        int cd = Math.max(0, a.behavior().cooldownFor(player, a));
        s.cooldown = cd;
        s.maxCooldown = Math.max(cd, 1);
    }

    public static void tick(ServerPlayer player) {
        PowerCapability.get(player).ifPresent(d -> {
            boolean changed = false;
            for (Power p : d.getPowers()) {
                for (Ability a : p.abilities()) {
                    AbilityState s = d.state(p, a);
                    if (s.cooldown > 0) s.cooldown--;
                    if (!s.enabled || a.type() == AbilityType.ACTION) continue;
                    // Si se bloquea (p. ej. se apaga la habilidad que la desbloquea), se apaga
                    if (!d.isUnlocked(p, a)) {
                        s.enabled = false; s.activeTicks = 0; s.activationTimer = 0; s.released = false;
                        a.behavior().onDeactivate(player, a);
                        changed = true;
                        continue;
                    }
                    if (a.behavior().shouldStop(player, a)) {
                        s.enabled = false; s.activeTicks = 0; s.activationTimer = 0; s.released = false;
                        a.behavior().onDeactivate(player, a);
                        startCooldown(player, s, a);
                        changed = true;
                        continue;
                    }
                    a.behavior().tick(player, a, s.activeTicks);
                    s.activeTicks++;
                    if (a.type() == AbilityType.HELD && s.released && s.activeTicks >= a.statI("min_duration")) {
                        s.enabled = false; s.activeTicks = 0; s.released = false;
                        a.behavior().onDeactivate(player, a);
                        startCooldown(player, s, a);
                        changed = true;
                        continue;
                    }
                    if (a.type() == AbilityType.ACTIVATION && --s.activationTimer <= 0) {
                        s.enabled = false; s.activeTicks = 0; s.activationTimer = 0;
                        a.behavior().onDeactivate(player, a);
                        a.behavior().onActivationEnd(player, a);
                        startCooldown(player, s, a);
                        changed = true;
                    }
                }
            }
            // La energía se envía como mucho cada 4 ticks
            if (changed || (d.dirty && player.tickCount % 4 == 0)) sync(player);
        });
    }
}
