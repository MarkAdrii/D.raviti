package com.markadrii.draviti.client;

/**
 * Teclas del mod (se pueden cambiar en Opciones → Controles → D.raviti).
 * Ejemplo:
 * public static final KeyMapping MI_TECLA = new KeyMapping("key.draviti.mi_tecla",
 *         KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);
 * y regístrala en ClientModEvents.registerKeys.
 */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.draviti";

    private KeyBindings() {}
}
