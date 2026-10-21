package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.KeyType;
import com.markadrii.draviti.data.AbilityState;
import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.network.AbilityKeyPacket;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.power.Power;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Teclas de la barra (misma lógica que Cursed Crown: Powers).
 * <p>
 * Con Cursed Crown instalado las teclas son las mismas para los dos mods. Este evento va con prioridad HIGHEST para
 * ejecutarse antes que el suyo en el mismo tick: X lo gestiona D.raviti (pasa por sus listas y luego por las nuestras)
 * y, cuando la lista que se ve es nuestra, se "tapan" las teclas para que su mod no las vea.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Habilidad que se pulsó con cada tecla, para soltar la misma aunque se cambie de lista. */
    private static final Power[] heldPower = new Power[AbilityLists.SIZE];
    private static final Ability[] heldAbility = new Ability[AbilityLists.SIZE];
    private static final List<Object[]> heldClicks = new ArrayList<>();
    private static boolean useWasDown, attackWasDown;

    /** Lista elegida contando las de los dos mods: primero las de Cursed Crown y luego las nuestras. */
    private static int combined;
    private static int combinedTotal;
    /** ¿Se ve una lista de D.raviti? */
    private static boolean ours;

    private ClientEvents() {}

    public static boolean oursSelected() { return ours; }
    /** Listas en total (las suyas y las nuestras): con más de una se enseña la tecla de cambiar de lista. */
    public static int totalLists() { return combinedTotal; }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ModKeys.ready()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PowerData data = PowerCapability.get(mc.player).orElse(null);
        if (data == null) return;

        // Enfriamientos y activaciones avanzan también en el cliente para que la barra se vea fluida
        if (!mc.isPaused())
            for (Power p : data.getPowers())
                for (Ability a : p.abilities()) {
                    AbilityState s = data.state(p, a);
                    if (s.cooldown > 0) s.cooldown--;
                    if (s.enabled && s.activationTimer > 1) s.activationTimer--;
                }

        AbilityLists.update();
        boolean shared = ModKeys.shared();
        int mine = AbilityLists.lists().size();
        int theirs = shared ? CursedCompat.theirListCount() : 0;
        combinedTotal = mine + theirs;

        // X: cambiar de lista. Con las teclas compartidas nos quedamos con la pulsación para que su mod no cambie solo.
        KeyMapping sw = ModKeys.switchList();
        while (sw.consumeClick())
            if (mc.screen == null && combinedTotal > 0)
                combined = Math.floorMod(combined + (mc.player.isCrouching() ? -1 : 1), combinedTotal);
        if (combinedTotal == 0) combined = 0;
        else if (combined >= combinedTotal) combined = combinedTotal - 1;

        boolean wasOurs = ours;
        ours = mine > 0 && combined >= theirs;
        if (shared && !ours && theirs > 0) CursedCompat.setTheirSelected(combined);
        if (ours) AbilityLists.select(combined - theirs);
        if (wasOurs && !ours) releaseAllKeys();

        // Teclas de la barra
        AbilityLists.AbilityList list = ours ? AbilityLists.selected() : null;
        Ability[] shown = list == null ? new Ability[AbilityLists.SIZE] : list.displayed(data);
        for (int i = 0; i < AbilityLists.SIZE; i++) {
            KeyMapping key = ModKeys.ability(i);
            boolean clicked = false, down = false;
            if (ours) {
                while (key.consumeClick()) clicked = true;
                down = (shared ? physicallyDown(mc, key) : key.isDown()) && mc.screen == null;
                // Su mod lee la misma tecla justo después: que no la vea pulsada mientras la lista es nuestra
                if (shared) key.setDown(false);
            }

            if (heldAbility[i] == null && (clicked || down) && mc.screen == null && list != null) {
                Ability a = shown[i];
                if (a != null && a.keyType() == KeyType.KEY_BIND && data.isUnlocked(list.power, a)) {
                    heldPower[i] = list.power;
                    heldAbility[i] = a;
                    send(heldPower[i], a, true);
                }
            }
            if (heldAbility[i] != null && !down) {
                send(heldPower[i], heldAbility[i], false);
                heldPower[i] = null;
                heldAbility[i] = null;
            }
        }

        // Habilidades de clic derecho / izquierdo (en cualquier lista, como en Cursed Crown)
        boolean use = mc.screen == null && mc.options.keyUse.isDown();
        boolean attack = mc.screen == null && mc.options.keyAttack.isDown();
        if (use && !useWasDown) pressClick(mc, data, KeyType.RIGHT_CLICK);
        if (attack && !attackWasDown) pressClick(mc, data, KeyType.LEFT_CLICK);
        heldClicks.removeIf(h -> {
            boolean still = h[2] == KeyType.RIGHT_CLICK ? use : attack;
            if (!still) send((Power) h[0], (Ability) h[1], false);
            return !still;
        });
        useWasDown = use;
        attackWasDown = attack;
    }

    private static void releaseAllKeys() {
        for (int i = 0; i < AbilityLists.SIZE; i++) {
            if (heldAbility[i] != null) send(heldPower[i], heldAbility[i], false);
            heldPower[i] = null;
            heldAbility[i] = null;
        }
    }

    /** Estado real de la tecla en el teclado/ratón (no el de la KeyMapping, que tapamos para Cursed Crown). */
    private static boolean physicallyDown(Minecraft mc, KeyMapping key) {
        InputConstants.Key k = key.getKey();
        if (k == InputConstants.UNKNOWN) return false;
        long window = mc.getWindow().getWindow();
        if (k.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
        if (k.getType() == InputConstants.Type.KEYSYM)
            return InputConstants.isKeyDown(window, k.getValue());
        return key.isDown();
    }

    private static void pressClick(Minecraft mc, PowerData data, KeyType type) {
        for (Power p : data.getPowers())
            for (Ability a : p.abilities())
                if (a.keyType() == type && data.isUnlocked(p, a)
                        && (!a.needsEmptyHand() || mc.player.getMainHandItem().isEmpty())) {
                    send(p, a, true);
                    heldClicks.add(new Object[]{p, a, type});
                }
    }

    private static void send(Power p, Ability a, boolean pressed) {
        ModNetwork.CHANNEL.sendToServer(new AbilityKeyPacket(p.id(), a.id(), pressed));
    }

    /** Con la barra compartida, su barra no se dibuja mientras se ve una lista nuestra. */
    @SubscribeEvent
    public static void hideTheirBar(RenderGuiOverlayEvent.Pre event) {
        if (ours && ModKeys.shared() && CursedCompat.THEIR_BAR.equals(event.getOverlay().id())) event.setCanceled(true);
    }
}
