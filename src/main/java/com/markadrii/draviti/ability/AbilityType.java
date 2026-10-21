package com.markadrii.draviti.ability;

/** Cómo se activa una habilidad (igual que las condiciones de Palladium). */
public enum AbilityType {
    /** Pulsar activa / desactiva. */
    TOGGLE,
    /** Pulsar la usa una vez y entra en enfriamiento. */
    ACTION,
    /** Activa mientras se mantiene la tecla. */
    HELD,
    /** Pulsar la activa durante unos ticks y luego entra en enfriamiento. */
    ACTIVATION
}
