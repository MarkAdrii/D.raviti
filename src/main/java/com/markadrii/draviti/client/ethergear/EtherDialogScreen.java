package com.markadrii.draviti.client.ethergear;

import com.markadrii.draviti.ethergear.EgOption;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.network.MovePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Conversación con el Ether Gear en su interior: abajo, como un cuadro de diálogo, con el texto letra a letra. */
public class EtherDialogScreen extends Screen {
    private static final int OPTION_H = 16;
    private Component line;
    private List<EgOption> options = new ArrayList<>();
    private int ticks;

    public EtherDialogScreen(Component line, List<Integer> options) {
        super(Component.translatable("entity.draviti.ether_gear"));
        update(line, options);
    }

    void update(Component line, List<Integer> ids) {
        this.line = line;
        this.options = new ArrayList<>();
        for (int id : ids) {
            EgOption o = EgOption.byId(id);
            if (o != null) options.add(o);
        }
        ticks = 0;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() { ticks++; }

    private int boxW() { return Math.min(width - 20, 380); }
    private int boxX() { return (width - boxW()) / 2; }
    private int boxH() { return 64 + options.size() * (OPTION_H + 2); }
    private int boxY() { return height - boxH() - 10; }
    private int optionY(int i) { return boxY() + 54 + i * (OPTION_H + 2); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = boxX(), y = boxY(), w = boxW(), h = boxH();
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, EtherGearScreen.BORDER);
        g.fill(x, y, x + w, y + h, 0xE8120A1C);
        g.fill(x, y, x + w, y + 14, EtherGearScreen.HEADER);
        g.fill(x, y + 14, x + w, y + 15, EtherGearScreen.ACCENT);
        g.drawString(font, title, x + 6, y + 3, EtherGearScreen.STAR_ON, false);

        String full = line.getString();
        int shown = Math.min(full.length(), (ticks + 1) * 2 + (int) (partialTick * 2));
        int ty = y + 20;
        for (FormattedCharSequence l : font.split(Component.literal(full.substring(0, shown)), w - 12)) {
            if (ty > y + 48) break;
            g.drawString(font, l, x + 6, ty, EtherGearScreen.TEXT, false);
            ty += 10;
        }
        for (int i = 0; i < options.size(); i++) {
            int oy = optionY(i);
            boolean hov = mouseX >= x + 6 && mouseX < x + w - 6 && mouseY >= oy && mouseY < oy + OPTION_H;
            g.fill(x + 6, oy, x + w - 6, oy + OPTION_H, hov ? EtherGearScreen.ACCENT : 0x40B56CFF);
            EtherGearScreen.outline(g, x + 6, oy, w - 12, OPTION_H, EtherGearScreen.BORDER);
            Component label = Component.literal((i + 1) + ". ").append(options.get(i).label());
            g.drawString(font, label, x + 12, oy + 4, hov ? 0xFFFFFFFF : EtherGearScreen.TEXT, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = boxX(), w = boxW();
            for (int i = 0; i < options.size(); i++) {
                int oy = optionY(i);
                if (mouseX >= x + 6 && mouseX < x + w - 6 && mouseY >= oy && mouseY < oy + OPTION_H) {
                    choose(options.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int n = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
        if (n >= 0 && n < options.size() && n < 9) {
            choose(options.get(n));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void choose(EgOption o) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (o == EgOption.BYE) onClose();
        ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.EG_DIALOG, o.ordinal(), 0));
    }
}
