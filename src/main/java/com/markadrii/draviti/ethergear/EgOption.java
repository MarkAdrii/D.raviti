package com.markadrii.draviti.ethergear;

import net.minecraft.network.chat.Component;

/** Respuestas en el interior del Ether Gear. bit != 0: solo una vez por visita. */
public enum EgOption {
    HOW(1),        // ¿Cómo estás?
    WHAT(2),       // ¿Qué eres?
    MEDITATE(4),   // Meditar juntos (rellena el éter)
    NEXT(0),       // ¿Qué me falta para la siguiente estrella?
    EXIT(0),       // Salir del interior
    BYE(0);        // Cerrar la conversación

    public final int bit;

    EgOption(int bit) { this.bit = bit; }

    public Component label() { return Component.translatable("eg.draviti.option." + name().toLowerCase()); }

    public static EgOption byId(int id) {
        EgOption[] v = values();
        return id >= 0 && id < v.length ? v[id] : null;
    }
}
