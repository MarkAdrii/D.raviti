package com.markadrii.draviti.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** config/draviti-client.toml (igual que en Cursed Crown: Powers). */
public final class ClientConfig {
    /** Igual que en Palladium. */
    public enum Position {
        TOP_LEFT(true, true), TOP_RIGHT(false, true), BOTTOM_LEFT(true, false), BOTTOM_RIGHT(false, false), HIDDEN(false, false);

        public final boolean left, top;

        Position(boolean left, boolean top) { this.left = left; this.top = top; }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<Position> POSITION;
    public static final ForgeConfigSpec.BooleanValue SCREEN_SHAKE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("ability_bar");
        POSITION = b.comment("Dónde se dibuja la barra de habilidades: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT o HIDDEN.",
                        "Si tienes Cursed Crown: Powers instalado se usa la posición de su barra (las dos barras son una).")
                .defineEnum("bar_position", Position.TOP_RIGHT);
        b.pop();
        b.push("effects");
        SCREEN_SHAKE = b.comment("La pantalla tiembla con los golpes fuertes.").define("screen_shake", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
