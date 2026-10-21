package com.markadrii.draviti.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Mismas teclas que Cursed Crown: Powers (y Palladium): V, B, N, M y coma; X cambia de lista (con Shift hacia atrás).
 * Si Cursed Crown: Powers está instalado se usan sus teclas y estas no se crean (no salen en Controles).
 */
public final class ModKeys {
    public static final String CATEGORY = "key.categories.draviti";
    private static final int[] DEFAULTS = {GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA};

    private static final KeyMapping[] ABILITIES = new KeyMapping[5];
    private static KeyMapping switchList;

    private ModKeys() {}

    /** Se llama en RegisterKeyMappingsEvent. */
    public static void register(RegisterKeyMappingsEvent event) {
        if (CursedCompat.linkKeys()) {
            for (int i = 0; i < ABILITIES.length; i++) ABILITIES[i] = CursedCompat.theirAbility(i);
            switchList = CursedCompat.theirSwitch();
            return;
        }
        // Se crean aquí y no antes: una KeyMapping creada ya responde a su tecla aunque no esté registrada
        for (int i = 0; i < ABILITIES.length; i++) {
            ABILITIES[i] = new KeyMapping("key.draviti.ability_" + (i + 1),
                    KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, DEFAULTS[i], CATEGORY);
            event.register(ABILITIES[i]);
        }
        switchList = new KeyMapping("key.draviti.switch_list",
                KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
        event.register(switchList);
    }

    public static boolean ready() { return switchList != null; }
    public static KeyMapping ability(int i) { return ABILITIES[i]; }
    public static KeyMapping switchList() { return switchList; }
    /** true = son las teclas de Cursed Crown. */
    public static boolean shared() { return CursedCompat.keysShared(); }
}
