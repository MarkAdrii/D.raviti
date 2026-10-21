package com.markadrii.draviti.data;

/** Estado de una habilidad para un jugador. */
public final class AbilityState {
    public boolean enabled;
    public int cooldown;
    public int maxCooldown;
    /** ACTIVATION: ticks que le quedan activa. */
    public int activationTimer;
    public int maxActivationTimer;
    /** Ticks que lleva activa (no se guarda). */
    public int activeTicks;
    /** HELD: se soltó la tecla pero sigue activa hasta cumplir min_duration (no se guarda). */
    public boolean released;

    public AbilityState copy() {
        AbilityState s = new AbilityState();
        s.enabled = enabled; s.cooldown = cooldown; s.maxCooldown = maxCooldown;
        s.activationTimer = activationTimer; s.maxActivationTimer = maxActivationTimer; s.activeTicks = activeTicks; s.released = released;
        return s;
    }
}
