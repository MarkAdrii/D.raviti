package com.markadrii.draviti.ability;

/** Color del marco de la habilidad en la barra (bar_color de Palladium). u/v dentro de la textura. */
public enum BarColor {
    WHITE(104, 0), ORANGE(104, 24), MAGENTA(104, 48), LIGHT_BLUE(104, 72),
    YELLOW(104, 96), LIME(104, 120), PINK(104, 144), GRAY(104, 168),
    LIGHT_GRAY(128, 0), CYAN(128, 24), PURPLE(128, 48), BLUE(128, 72),
    BROWN(128, 96), GREEN(128, 120), RED(128, 144), BLACK(128, 168);

    public final int u, v;

    BarColor(int u, int v) { this.u = u; this.v = v; }
}
