package com.markadrii.draviti.gravity;

/** Ids de los efectos visuales que el servidor manda a los clientes (FxPacket). */
public final class Fx {
    private Fx() {}

    /** Animación del jugador: a = índice en {@link #ANIMATIONS}. */
    public static final int ANIM = 0;
    /** Temblor de pantalla: a = fuerza, b = ticks. Solo cerca de pos. */
    public static final int SHAKE = 1;
    /** Brillo de una parte del cuerpo: a = parte (ClientVisuals.Limb), b = ticks. */
    public static final int GLOW = 2;

    /** El puño se llena de gravedad (partículas en la mano). */
    public static final int FIST_CHARGE = 10;
    /** Puño Gravitatorio: onda en cono hacia dir, a = alcance. */
    public static final int FIST_WAVE = 11;
    /** Onda expansiva en el suelo: a = radio. */
    public static final int SHOCKWAVE = 12;
    /** Onda Gravitatoria: espiral que atrae hacia pos, a = radio, b = ticks. */
    public static final int SPIRAL = 13;
    /** Columna que aplasta hacia abajo sobre pos (Centro de Gravedad): a = altura. */
    public static final int CRUSH_COLUMN = 14;
    /** Golpe de la ráfaga: chispas en pos. */
    public static final int BARRAGE_HIT = 15;
    /** Patada Molinete: anillo giratorio a la altura de la cintura, a = radio. */
    public static final int SPIN_RING = 16;
    /** Despegue del Cometa/Placaje: estela hacia arriba. */
    public static final int LAUNCH = 17;
    /** Gran impacto con cráter: a = radio, b = 1 si es de Impacto Gravitatorio (más energía). */
    public static final int IMPACT = 18;
    /** Campo Gravitatorio: cúpula y líneas que caen, a = radio. */
    public static final int FIELD = 19;
    /** Antigravedad desviando algo en pos. */
    public static final int DEFLECT = 20;
    /** Objetivo más pesado: líneas que lo aplastan, a = altura del objetivo. */
    public static final int HEAVY = 21;
    /** Objetivo ingrávido: anillos que lo rodean, a = altura del objetivo. */
    public static final int WEIGHTLESS = 22;
    /** Explosión de esfera de gravedad (cañón/meteoro): a = radio. */
    public static final int SPHERE_BURST = 23;
    /** Cielo Negro: implosión final, a = radio. */
    public static final int BLACK_SKY_BURST = 24;
    /** Transformación Overdrive: a = 1 forma completa. */
    public static final int OVERDRIVE = 25;
    /** Energía cinética absorbida (Impacto Gravitatorio). */
    public static final int ABSORB = 26;
    /** Fin del Overdrive. */
    public static final int OVERDRIVE_OFF = 27;

    /** Animaciones (assets/draviti/player_animation/NOMBRE.json). "stop" para la que esté sonando. */
    public static final String[] ANIMATIONS = {
            "stop", "gravity_punch", "palm_ground", "throw", "spin_kick", "barrage",
            "slam", "charge", "overdrive", "point_down", "dive_kick", "raise_sky", "float"
    };

    public static int anim(String name) {
        for (int i = 0; i < ANIMATIONS.length; i++) if (ANIMATIONS[i].equals(name)) return i;
        return 0;
    }
}
