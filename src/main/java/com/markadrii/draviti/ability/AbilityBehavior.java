package com.markadrii.draviti.ability;

import net.minecraft.server.level.ServerPlayer;

/** Lógica de servidor de una habilidad. Se pueden sobrescribir las versiones con o sin la Ability. */
public interface AbilityBehavior {
    /** Al activarse (TOGGLE/HELD/ACTIVATION) o al usarse (ACTION). */
    default void onActivate(ServerPlayer player) {}
    /** Cada tick mientras está activa (TOGGLE/HELD/ACTIVATION). */
    default void tick(ServerPlayer player, int activeTicks) {}
    /** Al desactivarse (también al perder el poder o morir). */
    default void onDeactivate(ServerPlayer player) {}

    default void onActivate(ServerPlayer player, Ability ability) { onActivate(player); }
    default void tick(ServerPlayer player, Ability ability, int activeTicks) { tick(player, activeTicks); }
    default void onDeactivate(ServerPlayer player, Ability ability) { onDeactivate(player); }

    /** ACTIVATION: cuando termina su tiempo de forma natural (no al morir ni al perder el poder). */
    default void onActivationEnd(ServerPlayer player, Ability ability) {}

    /** ACTION: true si esta pulsación no debe gastar ni respetar el enfriamiento (p. ej. cambiar de variante). */
    default boolean bypassCooldown(ServerPlayer player, Ability ability) { return false; }

    /** ACTION: si devuelve false no se activa el enfriamiento (no se pudo usar). */
    default boolean tryAction(ServerPlayer player, Ability ability) { onActivate(player, ability); return true; }

    /** TOGGLE / HELD / ACTIVATION: si devuelve false no se activa (p. ej. falta energía). */
    default boolean canActivate(ServerPlayer player, Ability ability) { return true; }

    /** Cada tick mientras está activa: true para apagarla (y que empiece el enfriamiento). */
    default boolean shouldStop(ServerPlayer player, Ability ability) { return false; }

    /** Enfriamiento que se aplica al terminar (por defecto el de la habilidad / config). */
    default int cooldownFor(ServerPlayer player, Ability ability) { return ability.cooldown(); }

    AbilityBehavior NONE = new AbilityBehavior() {};
}
