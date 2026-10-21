package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.gravity.Limb;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Habilidades activas de cada jugador (las suyas y las de los demás) y brillos temporales de brazos/piernas,
 * para que los efectos se vean en todos los clientes.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class ClientVisuals {
    private static final Map<Integer, Set<String>> ACTIVE = new HashMap<>();
    private static final Map<Integer, Map<String, Integer>> STARTED = new HashMap<>();
    /** Brillo de una parte del cuerpo hasta cierto tick: entidad -> parte -> tick final. */
    private static final Map<Integer, Map<Limb, Integer>> GLOW = new HashMap<>();
    private static int clientTicks;

    private ClientVisuals() {}

    public static void set(int entityId, List<String> active) {
        Set<String> now = new HashSet<>(active);
        Map<String, Integer> st = STARTED.computeIfAbsent(entityId, k -> new HashMap<>());
        st.keySet().retainAll(now);
        for (String s : now) st.putIfAbsent(s, clientTicks);
        ACTIVE.put(entityId, now);
    }

    /** "draviti:gravity/overdrive" etc. */
    public static boolean active(Entity e, String key) {
        Set<String> s = ACTIVE.get(e.getId());
        return s != null && s.contains(key);
    }

    /** Ticks que lleva activa (0 si no lo está). */
    public static int activeTicks(Entity e, String key) {
        Map<String, Integer> st = STARTED.get(e.getId());
        Integer t = st == null ? null : st.get(key);
        return t == null ? 0 : clientTicks - t;
    }

    public static void glow(int entityId, Limb limb, int ticks) {
        GLOW.computeIfAbsent(entityId, k -> new HashMap<>()).put(limb, clientTicks + ticks);
    }

    /** 0..1: cuánto brilla esa parte (se apaga al final). */
    public static float glow(Entity e, Limb limb) {
        Map<Limb, Integer> m = GLOW.get(e.getId());
        if (m == null) return 0;
        float best = 0;
        for (var en : m.entrySet()) {
            Limb l = en.getKey();
            boolean match = l == limb || (l == Limb.BOTH_ARMS && (limb == Limb.RIGHT_ARM || limb == Limb.LEFT_ARM));
            if (!match) continue;
            int left = en.getValue() - clientTicks;
            if (left > 0) best = Math.max(best, Math.min(1F, left / 6F));
        }
        return best;
    }

    public static int ticks() { return clientTicks; }

    /** Player Animator está instalado (es obligatorio, pero por si acaso no se llama a su API sin él). */
    public static boolean playerAnimator() {
        return net.minecraftforge.fml.ModList.get().isLoaded("playeranimator");
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) {
            ACTIVE.clear(); STARTED.clear(); GLOW.clear();
            return;
        }
        if (Minecraft.getInstance().isPaused()) return;
        clientTicks++;
        if (clientTicks % 40 == 0)
            GLOW.values().forEach(m -> m.values().removeIf(t -> t < clientTicks));
    }
}
