package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.config.ClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Puente con Cursed Crown: Powers (mod de RedneTwo) sin depender de él: todo por reflexión y solo si está instalado.
 * <ul>
 *   <li>Teclas: se usan las suyas (V, B, N, M, coma y X) y las de D.raviti no se registran, así en Controles solo hay un juego.</li>
 *   <li>Barra: las dos barras son una. X pasa por sus listas y luego por las nuestras; solo se dibuja la que toca.</li>
 * </ul>
 * Si algo cambia en su mod y la reflexión falla, D.raviti vuelve a usar sus propias teclas y su barra sola.
 */
public final class CursedCompat {
    public static final String MOD_ID = "cursedpowers";
    /** Id de su barra en la lista de overlays (registerAboveAll("ability_bar") en su ClientEvents). */
    public static final ResourceLocation THEIR_BAR = new ResourceLocation(MOD_ID, "ability_bar");

    private static boolean keysShared;
    private static KeyMapping[] theirAbilities;
    private static KeyMapping theirSwitch;

    private static boolean listsBroken;
    private static Method listsMethod;
    private static Field selectedField;

    private static boolean positionBroken;
    private static ForgeConfigSpec.EnumValue<?> theirPosition;

    private CursedCompat() {}

    public static boolean loaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    private static Class<?> find(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        }
    }

    /** Intenta usar sus teclas. true = compartidas (no hay que registrar las nuestras). */
    public static boolean linkKeys() {
        if (!loaded()) return false;
        try {
            Class<?> keys = find("com.cursedcrown.powers.client.ModKeys");
            KeyMapping[] abilities = (KeyMapping[]) keys.getField("ABILITIES").get(null);
            KeyMapping sw = (KeyMapping) keys.getField("SWITCH_LIST").get(null);
            if (abilities == null || abilities.length < 5 || sw == null) return false;
            for (int i = 0; i < 5; i++) if (abilities[i] == null) return false;
            theirAbilities = abilities;
            theirSwitch = sw;
            keysShared = true;
            Draviti.LOGGER.info("Cursed Crown: Powers detectado: D.raviti usa sus teclas y comparte su barra.");
            return true;
        } catch (Throwable t) {
            Draviti.LOGGER.warn("Cursed Crown: Powers está instalado pero no se pudieron compartir las teclas; D.raviti usa las suyas.", t);
            return false;
        }
    }

    public static boolean keysShared() { return keysShared; }
    public static KeyMapping theirAbility(int i) { return theirAbilities[i]; }
    public static KeyMapping theirSwitch() { return theirSwitch; }

    private static boolean prepareLists() {
        if (listsBroken || !keysShared) return false;
        if (listsMethod != null) return true;
        try {
            Class<?> lists = find("com.cursedcrown.powers.client.AbilityLists");
            listsMethod = lists.getMethod("lists");
            selectedField = lists.getDeclaredField("selected");
            selectedField.setAccessible(true);
            return true;
        } catch (Throwable t) {
            listsBroken = true;
            Draviti.LOGGER.warn("No se pudo unir la barra con la de Cursed Crown: Powers; cada una va por su lado.", t);
            return false;
        }
    }

    /** Cuántas listas tiene su barra ahora mismo (0 si el jugador no tiene poderes suyos). */
    public static int theirListCount() {
        if (!prepareLists()) return 0;
        try {
            Object l = listsMethod.invoke(null);
            return l instanceof List<?> list ? list.size() : 0;
        } catch (Throwable t) {
            listsBroken = true;
            return 0;
        }
    }

    /** Elige qué lista suya se ve. */
    public static void setTheirSelected(int index) {
        if (!prepareLists()) return;
        try {
            selectedField.setInt(null, index);
        } catch (Throwable t) {
            listsBroken = true;
        }
    }

    /** Posición de su barra (para que las dos estén en el mismo sitio); null si no se puede leer. */
    public static ClientConfig.Position theirPosition() {
        if (positionBroken || !loaded()) return null;
        try {
            if (theirPosition == null) {
                Class<?> cfg = find("com.cursedcrown.powers.config.ClientConfig");
                theirPosition = (ForgeConfigSpec.EnumValue<?>) cfg.getField("POSITION").get(null);
            }
            Object v = theirPosition.get();
            return v instanceof Enum<?> e ? ClientConfig.Position.valueOf(e.name()) : null;
        } catch (Throwable t) {
            positionBroken = true;
            return null;
        }
    }
}
