package com.markadrii.draviti.client;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.Icon;
import com.markadrii.draviti.ability.KeyType;
import com.markadrii.draviti.config.ClientConfig;
import com.markadrii.draviti.config.ClientConfig.Position;
import com.markadrii.draviti.data.AbilityState;
import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.power.Power;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * Barra de habilidades de Cursed Crown: Powers (copia de la de Palladium 4.5): mismas medidas, misma textura
 * (formato ability_bar.png) y mismo comportamiento con el chat abierto.
 * Con Cursed Crown instalado se dibuja en el mismo sitio que la suya y solo cuando la lista elegida es nuestra.
 */
public class AbilityBarOverlay implements IGuiOverlay {
    private static final int INDICATOR_W = 52, INDICATOR_H = 28;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        List<AbilityLists.AbilityList> lists = AbilityLists.lists();
        if (lists.isEmpty() || !ClientEvents.oursSelected()) return;
        PowerData data = PowerCapability.get(mc.player).orElse(null);
        if (data == null) return;

        Position position = position();
        AbilityLists.AbilityList list = AbilityLists.selected();
        if (position == Position.HIDDEN || list == null) return;
        if (position.top && mc.options.renderDebug) return;
        if (!position.top && mc.screen instanceof ChatScreen)
            position = position.left ? Position.TOP_LEFT : Position.TOP_RIGHT;

        boolean simple = list.simple && ClientEvents.totalLists() <= 1;
        ResourceLocation texture = list.power.barTexture();
        PoseStack pose = g.pose();

        if (!simple) {
            pose.pushPose();
            if (!position.top) pose.translate(0, height - INDICATOR_H, 0);
            if (!position.left) pose.translate(width - INDICATOR_W, 0, 0);
            renderIndicator(mc, g, list, position, texture, ClientEvents.totalLists() > 1);
            pose.popPose();
        }

        if (!list.power.energyBars().isEmpty()) {
            pose.pushPose();
            int n = list.power.energyBars().size();
            int w = n * 9 + 1, h = 104;
            if (position.top) pose.translate(!position.left ? width - 24 - w + 1 : 24 - 1, INDICATOR_H - 1, 0);
            else pose.translate(!position.left ? width - 24 - w + 1 : 24 - 1, height - INDICATOR_H - h + 1, 0);
            renderEnergyBars(g, list.power, data, texture);
            pose.popPose();
        }

        pose.pushPose();
        if (!simple) {
            if (position.top) pose.translate(!position.left ? width - 24 : 0, INDICATOR_H - 1, 0);
            else pose.translate(!position.left ? width - 24 : 0, height - INDICATOR_H - 112 + 1, 0);
        } else {
            if (position.top) pose.translate(!position.left ? width - 24 : 0, 0, 0);
            else pose.translate(!position.left ? width - 24 : 0, height - 24, 0);
        }
        Ability[] shown = list.displayed(data);
        renderAbilitiesBackground(mc, g, position, list, shown, data, texture, simple);
        renderAbilitiesOverlay(mc, g, position, list, shown, data, texture, simple);
        pose.popPose();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /** La de Cursed Crown si está instalado (las dos barras ocupan el mismo sitio); si no, la nuestra. */
    static Position position() {
        if (ModKeys.shared()) {
            Position theirs = CursedCompat.theirPosition();
            if (theirs != null) return theirs;
        }
        return ClientConfig.POSITION.get();
    }

    private static void renderIndicator(Minecraft mc, GuiGraphics g, AbilityLists.AbilityList list, Position position,
                                        ResourceLocation texture, boolean showKey) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(texture, 0, 0, position.left ? 52 : 0, position.top ? 28 : 0, 52, 28);
        if (!showKey) {
            drawIcon(g, list.power.icon(), position.left ? 17 : 19, position.top ? 5 : 7);
            return;
        }
        // Poderes con más de 5 habilidades: la tecla de cambiar de lista (p. ej. X) y el icono juntos, centrados
        FormattedText props = mc.font.substrByWidth(ModKeys.switchList().getTranslatedKeyMessage(), 10);
        int textW = mc.font.width(props), gap = 2;
        int start = (INDICATOR_W - (textW + gap + 16)) / 2 + (position.left ? -1 : 1); // igual que sin tecla (17 / 19)
        int textX = position.left ? start : start + 16 + gap;
        int iconX = position.left ? start + textW + gap : start;
        drawIcon(g, list.power.icon(), iconX, position.top ? 5 : 7);
        g.drawString(mc.font, Component.literal(props.getString()), textX, position.top ? 10 : 12, -1, false);
    }

    private static void renderAbilitiesBackground(Minecraft mc, GuiGraphics g, Position position, AbilityLists.AbilityList list,
                                                  Ability[] shown, PowerData data, ResourceLocation texture, boolean simple) {
        boolean showName = mc.screen instanceof ChatScreen;
        Power power = list.power;
        for (int i = 0; i < 5 && (!simple || i <= 0); i++) {
            Lighting.setupFor3DItems();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            int y = i * 22 + 3;
            g.blit(texture, 3, y, 60, 56, 18, 18);
            Ability a = shown[i];
            if (a == null) {
                g.blit(texture, 3, y, 60, 56, 18, 18);
                continue;
            }
            AbilityState st = data.state(power, a);
            boolean unlocked = data.isUnlocked(power, a);
            if (st.enabled && st.activationTimer != 0 && st.maxActivationTimer != 0) {
                int h = (int) (st.activationTimer / (float) st.maxActivationTimer * 18.0F);
                g.blit(texture, 3, y, 24, 56, 18, 18);
                g.blit(texture, 3, y + (18 - h), 42, 74 - h, 18, h);
            } else {
                g.blit(texture, 3, y, st.enabled ? 42 : 24, unlocked ? 56 : 74, 18, 18);
            }
            AbilityState cd = cooldownState(power, a, data);
            if (cd != null) {
                int w = (int) (cd.cooldown / (float) cd.maxCooldown * 18.0F);
                g.blit(texture, 3, y, 60, 74, w, 18);
            }
            if (!unlocked) g.blit(texture, 3, y, 42, 74, 18, 18);
            else drawIcon(g, a.icon(), 4, 4 + i * 22);

            if (showName) {
                Component name = a.name();
                int w = mc.font.width(name);
                int bx = position.left ? 24 : -w - 10;
                g.fill(bx, i * 22 + 5, bx + 10 + w, i * 22 + 5 + 14, 0x80000000);
                g.drawString(mc.font, name, position.left ? 29 : -w - 5, i * 22 + 8, -1, false);
            }
        }
    }

    private static void renderAbilitiesOverlay(Minecraft mc, GuiGraphics g, Position position, AbilityLists.AbilityList list,
                                               Ability[] shown, PowerData data, ResourceLocation texture, boolean simple) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        if (!simple) g.blit(texture, 0, 0, 0, 56, 24, 112);
        else g.blit(texture, 0, 0, 0, 168, 24, 24);

        for (int i = 0; i < 5; i++) {
            Ability a = shown[i];
            if (a == null) continue;
            boolean unlocked = data.isUnlocked(list.power, a);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Lighting.setupFor3DItems();
            RenderSystem.enableBlend();
            if (!unlocked) g.blit(texture, 3, i * 22 + 3, 42, 74, 18, 18);
            g.blit(texture, 0, i * 22, a.color().u, a.color().v, 24, 24);
            // Barra de progreso del enfriamiento (también el de los ataques ocultos que dependen de ella)
            AbilityState cd = cooldownState(list.power, a, data);
            if (cd != null) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 250);
                float done = 1f - cd.cooldown / (float) cd.maxCooldown;
                int by = i * 22 + 18;
                g.fill(4, by, 20, by + 2, 0xC0000000);
                g.fill(4, by, 4 + Math.round(16 * done), by + 2, 0xFF000000 | list.power.color());
                g.pose().popPose();
            }
            if (unlocked) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                KeyType kt = a.keyType();
                if (kt == KeyType.KEY_BIND) {
                    var key = ModKeys.ability(i);
                    if (key.getKey().getType() == InputConstants.Type.MOUSE) {
                        g.blit(texture, 16, 5 + i * 22 + 8, 24, 112, 5, 7);
                    } else {
                        Component keyName = key.getTranslatedKeyMessage();
                        g.drawString(mc.font, keyName, 22 - mc.font.width(keyName), 5 + i * 22 + 7, 0xFFFFFF, false);
                    }
                } else if (kt == KeyType.LEFT_CLICK) {
                    g.blit(texture, 16, 5 + i * 22 + 8, 24, 92, 5, 7);
                } else if (kt == KeyType.RIGHT_CLICK) {
                    g.blit(texture, 16, 5 + i * 22 + 8, 29, 92, 5, 7);
                }
                g.pose().popPose();
            }
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void renderEnergyBars(GuiGraphics g, Power power, PowerData data, ResourceLocation texture) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(texture, 0, 0, 152, 0, 1, 104);
        int x = 1;
        for (Power.EnergyBar bar : power.energyBars()) {
            g.blit(texture, x, 0, 153, 0, 9, 104);
            int h = (int) (data.energy(power, bar.name()) / (float) Math.max(1, bar.max()) * 98.0F);
            // Relleno dibujado sin textura: se ve aunque la textura de la barra no tenga la zona de energía
            g.fill(x + 2, 3, x + 6, 101, 0xA0000000);
            g.fill(x + 2, 101 - h, x + 6, 101, 0xFF000000 | bar.color());
            x += 9;
        }
    }

    /** Estado con más enfriamiento entre la habilidad y las ocultas que dependen de ella (p. ej. disparo y rayo del Blaster). */
    static AbilityState cooldownState(Power power, Ability a, PowerData data) {
        AbilityState best = null;
        for (Ability o : power.abilities()) {
            if (o != a && !(o.hiddenInBar() && a.id().equals(o.requires()))) continue;
            AbilityState st = data.state(power, o);
            if (st.cooldown <= 0 || st.maxCooldown <= 0) continue;
            if (best == null || st.cooldown > best.cooldown) best = st;
        }
        return best;
    }

    static void drawIcon(GuiGraphics g, Icon icon, int x, int y) {
        if (icon.isTexture() && icon.region() != null) {
            int[] r = icon.region();
            RenderSystem.enableBlend();
            g.blit(icon.texture(), x, y, 16, 16, r[0], r[1], r[2], r[3], r[4], r[5]);
        } else if (icon.isTexture()) {
            RenderSystem.enableBlend();
            g.blit(icon.texture(), x, y, 0f, 0f, 16, 16, 16, 16);
        } else {
            g.renderItem(icon.stack(), x, y);
        }
    }
}
