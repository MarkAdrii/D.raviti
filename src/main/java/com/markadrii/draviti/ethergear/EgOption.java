package com.markadrii.draviti.ethergear;

import net.minecraft.network.chat.Component;

/** Respuestas en el interior del Ether Gear. bit != 0: solo una vez por visita. */
public enum EgOption {
    HOW(1),        // ¿Cómo estás?
    WHAT(2),       // ¿Qué eres?
    ASK_OVERDRIVE(8),  // ¿Qué es el Overdrive?
    ASK_WHY(16),       // ¿Por qué me elegiste?
    ASK_ETHER(32),     // Háblame del éter
    NEXT(0),       // ¿Qué me falta para la siguiente estrella?
    BORROW(0),     // Préstame tu poder (si te falta el Overdrive o el Completo, cada 5 días)
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
