package com.markadrii.draviti.client.ethergear;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.AbilityType;
import com.markadrii.draviti.client.AbilityBarOverlay;
import com.markadrii.draviti.client.ModKeys;
import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.entity.EtherGearEntity;
import com.markadrii.draviti.ethergear.EtherGear;
import com.markadrii.draviti.ethergear.EtherGearInterior;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.network.MovePacket;
import com.markadrii.draviti.power.Power;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Menú del Ether Gear (tecla U), con la misma estructura que el del mod Seals.
 * <ul>
 *   <li>Izquierda: el Ether Gear, sus estrellas, la barra de afinidad y abajo lo que explica lo que tengas bajo el
 *       ratón (o lo último que te ha dicho).</li>
 *   <li>Centro: el camino de estrellas (qué da cada una). Con el botón Habilidades: la lista de técnicas a la izquierda
 *       y el detalle de la elegida en el centro (estrella que pide, daño, éter...).</li>
 *   <li>Derecha: el modelo del Ether Gear en 3D (arrastra para girarlo).</li>
 * </ul>
 * Abajo: Habilidades, Hablar (1 vez al día) y Entrar en su interior (con 1 estrella, cada pocos días).
 */
public class EtherGearScreen extends Screen {
    // Colores: negro y morado
    static final int BG = 0xF0120A1C, PANEL = 0xFF1B1127, PANEL_L = 0xFF281A3A, BORDER = 0xFF4A2E72,
            HEADER = 0xFF2E1A4A, ACCENT = 0xFFB56CFF, TEXT = 0xFFEDE3FF, MUTED = 0xFF9C8CB8,
            STAR_ON = 0xFFD9A8FF, STAR_OFF = 0xFF4A3A60, BAR = 0xFFB56CFF, OK = 0xFF7EE08A, NO = 0xFFE07E8E;
    private static final ResourceLocation EMBLEM = Draviti.id("textures/gui/ether_gear_emblem.png");

    private static final int HEADER_H = 16, FOOTER = 24, GAP = 6, ROW = 17;

    private int x0, y0, w, h, lx, lw, mx, mw, rx, rw, top, bottom;
    private boolean abilitiesPage;
    private int selected, scroll;
    private float rotation = 20F;
    private boolean dragging;
    private List<Ability> abilities = new ArrayList<>();

    private enum Info { BOND, STARS, AFFINITY, ETHER }
    private Info info = Info.BOND;
    private int infoTop;

    public EtherGearScreen() {
        super(Component.translatable("screen.draviti.eg.title"));
    }

    @Override
    protected void init() {
        w = Math.min(width - 12, 500);
        h = Math.min(height - 12, 280);
        x0 = (width - w) / 2;
        y0 = (height - h) / 2;
        int inner = w - 12;
        lw = (int) (inner * 0.34F);
        mw = (int) (inner * 0.36F);
        rw = inner - lw - mw - GAP * 2;
        lx = x0 + 6;
        mx = lx + lw + GAP;
        rx = mx + mw + GAP;
        top = y0 + HEADER_H + 6;
        bottom = y0 + h - FOOTER;
        Power pw = Gravity.power();
        abilities = new ArrayList<>();
        if (pw != null)
            for (Ability a : pw.abilities()) if (!a.id().equals("pasivas")) abilities.add(a);
        abilities.sort(Comparator.comparingInt(a -> EtherGear.starsFor(a.id())));
    }

    @Override public boolean isPauseScreen() { return false; }

    private PowerData data() {
        var p = Minecraft.getInstance().player;
        return p == null ? null : PowerCapability.get(p).orElse(null);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PowerData d = data();
        if (d == null || Minecraft.getInstance().player == null || !Gravity.has(Minecraft.getInstance().player)) {
            onClose();
            return;
        }
        renderBackground(g);
        g.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, BORDER);
        g.fill(x0, y0, x0 + w, y0 + h, BG);
        drawHeader(g, d);
        info = Info.BOND;
        if (abilitiesPage) {
            drawAbilityList(g, d, mouseX, mouseY);
            drawAbilityDetail(g, d);
        } else {
            drawLeft(g, d, mouseX, mouseY);
            drawInfoBox(g, d);
            drawStarPath(g, d);
        }
        drawRight(g, partialTick);
        drawFooter(g, d, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphics g, PowerData d) {
        g.fill(x0, y0, x0 + w, y0 + HEADER_H, HEADER);
        g.fill(x0, y0 + HEADER_H, x0 + w, y0 + HEADER_H + 1, ACCENT);
        g.drawString(font, title, x0 + 6, y0 + 4, TEXT, false);
        int sx = x0 + w - 8 - EtherGear.MAX_STARS * 9;
        for (int i = 0; i < EtherGear.MAX_STARS; i++) star(g, sx + i * 9, y0 + 4, i < d.egStars ? STAR_ON : STAR_OFF);
    }

    private void panel(GuiGraphics g, int x, int y, int pw, int ph) {
        g.fill(x - 1, y - 1, x + pw + 1, y + ph + 1, BORDER);
        g.fill(x, y, x + pw, y + ph, PANEL);
    }

    private void drawLeft(GuiGraphics g, PowerData d, int mouseX, int mouseY) {
        panel(g, lx, top, lw, bottom - top);
        int x = lx + 6, bw = lw - 12, y = top + 6;

        g.fill(x - 1, y - 1, x + 25, y + 25, BORDER);
        RenderSystem.enableBlend();
        g.blit(EMBLEM, x, y, 24, 24, 0, 0, 64, 64, 64, 64);
        g.drawString(font, Component.translatable("screen.draviti.eg.entity"), x + 30, y + 2, MUTED, false);
        g.drawString(font, Component.translatable("entity.draviti.ether_gear"), x + 30, y + 13, TEXT, false);
        y += 30;

        // Estrellas
        g.drawString(font, Component.translatable("screen.draviti.eg.stars"), x, y, TEXT, false);
        for (int i = 0; i < EtherGear.MAX_STARS; i++)
            star(g, x + bw - EtherGear.MAX_STARS * 9 + i * 9, y, i < d.egStars ? STAR_ON : STAR_OFF);
        if (hover(mouseX, mouseY, x, y - 1, bw, 10)) info = Info.STARS;
        y += 12;

        // Afinidad
        int max = EtherGear.affinityMax(d.egStars);
        String aff = EtherGear.maxed(d) ? Component.translatable("screen.draviti.eg.max").getString() : d.egAffinity + " / " + max;
        g.drawString(font, Component.translatable("screen.draviti.eg.affinity"), x, y, TEXT, false);
        g.drawString(font, aff, x + bw - font.width(aff), y, TEXT, false);
        bar(g, x, y + 10, bw, 6, d.egAffinity / (float) max, BAR);
        if (hover(mouseX, mouseY, x, y, bw, 18)) info = Info.AFFINITY;
        y += 20;

        // Éter máximo
        var p = Minecraft.getInstance().player;
        String ether = Gravity.etherMax(p) + (d.egStars >= EtherGear.ETHER_BONUS_STARS ? "  (+" + EtherGear.ETHER_BONUS + ")" : "");
        g.drawString(font, Component.translatable("screen.draviti.eg.ether"), x, y, TEXT, false);
        g.drawString(font, ether, x + bw - font.width(ether), y, ACCENT, false);
        if (hover(mouseX, mouseY, x, y - 1, bw, 10)) info = Info.ETHER;
        y += 12;

        // Forma
        Component form = Component.translatable(Gravity.fullOverdrive(p) ? "screen.draviti.eg.form.full"
                : Gravity.overdrive(p) ? "screen.draviti.eg.form.overdrive" : "screen.draviti.eg.form.normal");
        g.drawString(font, Component.translatable("screen.draviti.eg.form", form), x, y, MUTED, false);
        y += 12;
        infoTop = y;
    }

    private void drawInfoBox(GuiGraphics g, PowerData d) {
        int x = lx + 4, y = infoTop, bw = lw - 8, bh = bottom - 4 - y;
        if (bh < 24) return;
        g.fill(x, y, x + bw, y + bh, PANEL_L);
        outline(g, x, y, bw, bh, BORDER);
        Component title;
        List<Component> paragraphs = new ArrayList<>();
        switch (info) {
            case STARS -> {
                title = Component.translatable("info.draviti.eg.stars.title");
                paragraphs.add(Component.translatable("info.draviti.eg.stars"));
            }
            case AFFINITY -> {
                title = Component.translatable("info.draviti.eg.affinity.title");
                paragraphs.add(Component.translatable("info.draviti.eg.affinity"));
            }
            case ETHER -> {
                title = Component.translatable("info.draviti.eg.ether.title");
                paragraphs.add(Component.translatable("info.draviti.eg.ether", EtherGear.ETHER_BONUS, EtherGear.ETHER_BONUS_STARS));
            }
            default -> {
                Component said = EtherGearClient.lastLine();
                if (said != null) {
                    title = Component.translatable("info.draviti.eg.said");
                    paragraphs.add(Component.literal("«").append(said).append("»"));
                } else {
                    title = Component.translatable("info.draviti.eg.bond.title");
                    paragraphs.add(Component.translatable("info.draviti.eg.bond"));
                }
            }
        }
        int ty = y + 4;
        g.drawString(font, title, x + 4, ty, ACCENT, false);
        ty += 11;
        for (Component p : paragraphs) {
            for (FormattedCharSequence line : font.split(p, bw - 8)) {
                if (ty > y + bh - 9) return;
                g.drawString(font, line, x + 4, ty, TEXT, false);
                ty += 9;
            }
            ty += 3;
        }
    }

    /** Centro: qué da cada estrella. */
    private void drawStarPath(GuiGraphics g, PowerData d) {
        panel(g, mx, top, mw, bottom - top);
        int x = mx + 6, bw = mw - 12, y = top + 6;
        g.drawString(font, Component.translatable("screen.draviti.eg.path"), x, y, ACCENT, false);
        y += 13;
        for (int s = 0; s <= EtherGear.MAX_STARS; s++) {
            boolean got = d.egStars >= s;
            List<Component> rewards = new ArrayList<>();
            for (Ability a : abilities) if (EtherGear.starsFor(a.id()) == s) rewards.add(a.name());
            if (s == EtherGear.ETHER_BONUS_STARS) rewards.add(0, Component.translatable("screen.draviti.eg.reward.ether", EtherGear.ETHER_BONUS));
            if (s == 1) rewards.add(Component.translatable("screen.draviti.eg.reward.enter"));
            // Estrellas de la fila
            if (s == 0) g.drawString(font, Component.translatable("screen.draviti.eg.start"), x, y, got ? STAR_ON : MUTED, false);
            else for (int i = 0; i < s; i++) star(g, x + i * 8, y, got ? STAR_ON : STAR_OFF);
            y += 10;
            Component joined = Component.empty();
            for (int i = 0; i < rewards.size(); i++) {
                if (i > 0) joined = joined.copy().append(", ");
                joined = joined.copy().append(rewards.get(i));
            }
            for (FormattedCharSequence line : font.split(joined, bw - 6)) {
                if (y > bottom - 12) return;
                g.drawString(font, line, x + 6, y, got ? TEXT : MUTED, false);
                y += 9;
            }
            y += 4;
        }
    }

    // ------------------------------------------------------------------ página de habilidades

    private int listTop() { return top + 20; }
    private int visibleRows() { return Math.max(1, (bottom - 4 - listTop()) / ROW); }

    private void drawAbilityList(GuiGraphics g, PowerData d, int mouseX, int mouseY) {
        panel(g, lx, top, lw, bottom - top);
        int x = lx + 6, bw = lw - 12, y = top + 6;
        Power pw = Gravity.power();
        long unlocked = abilities.stream().filter(a -> EtherGear.starsFor(a.id()) <= d.egStars).count();
        g.drawString(font, Component.translatable("screen.draviti.eg.abilities", unlocked, abilities.size()), x, y, ACCENT, false);
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, abilities.size() - rows));
        y = listTop();
        for (int k = 0; k < rows && k + scroll < abilities.size(); k++) {
            int i = k + scroll;
            Ability a = abilities.get(i);
            int need = EtherGear.starsFor(a.id());
            boolean open = need <= d.egStars;
            boolean sel = i == selected, hov = hover(mouseX, mouseY, x, y, bw, ROW - 2);
            g.fill(x, y, x + bw, y + ROW - 2, sel ? HEADER : hov ? 0xFF3A2752 : PANEL_L);
            outline(g, x, y, bw, ROW - 2, BORDER);
            RenderSystem.setShaderColor(1, 1, 1, open ? 1F : 0.35F);
            AbilityBarOverlay.drawIcon(g, a.icon(), x, y - 1);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Component name = a.name();
            int maxName = bw - 22 - need * 8 - 4;
            String txt = font.plainSubstrByWidth(name.getString(), maxName);
            g.drawString(font, txt, x + 19, y + 4, sel ? 0xFFFFFFFF : open ? TEXT : MUTED, false);
            for (int s = 0; s < need; s++) star(g, x + bw - 3 - (need - s) * 8, y + 4, open ? STAR_ON : STAR_OFF);
            y += ROW;
        }
        // Barra de desplazamiento
        if (abilities.size() > rows) {
            int trackH = rows * ROW - 2, th = Math.max(10, trackH * rows / abilities.size());
            int ty = listTop() + (trackH - th) * scroll / Math.max(1, abilities.size() - rows);
            g.fill(lx + lw - 4, listTop(), lx + lw - 2, listTop() + trackH, 0x40FFFFFF);
            g.fill(lx + lw - 4, ty, lx + lw - 2, ty + th, ACCENT);
        }
        if (pw == null) return;
    }

    private void drawAbilityDetail(GuiGraphics g, PowerData d) {
        panel(g, mx, top, mw, bottom - top);
        if (selected >= abilities.size()) return;
        Ability a = abilities.get(selected);
        int x = mx + 6, bw = mw - 12, y = top + 6, maxY = bottom - 12;
        g.drawString(font, a.name(), x, y, ACCENT, false);
        y += 11;
        Component kind = Component.translatable("screen.draviti.eg.kind." + a.type().name().toLowerCase());
        g.drawString(font, kind, x, y, MUTED, false);
        y += 12;

        // Lo que pide: la estrella y si ya la tienes
        int need = EtherGear.starsFor(a.id());
        boolean open = need <= d.egStars;
        Component req = need == 0 ? Component.translatable("screen.draviti.eg.req.none")
                : Component.translatable("screen.draviti.eg.req", "★".repeat(need));
        g.drawString(font, req, x, y, TEXT, false);
        y += 10;
        Component status = open ? Component.translatable("screen.draviti.eg.req.ok")
                : Component.translatable("screen.draviti.eg.req.missing", need - d.egStars);
        for (FormattedCharSequence line : font.split(status, bw)) {
            g.drawString(font, line, x, y, open ? OK : NO, false);
            y += 9;
        }
        if (a.requires() != null) {
            Power pw = Gravity.power();
            Component r = pw == null ? Component.literal(a.requires())
                    : pw.ability(a.requires()).map(Ability::name).orElse(Component.literal(a.requires()));
            for (FormattedCharSequence line : font.split(Component.translatable("screen.draviti.eg.req.active", r), bw)) {
                g.drawString(font, line, x, y, MUTED, false);
                y += 9;
            }
        }
        y += 5;
        g.fill(x, y - 3, x + bw, y - 2, BORDER);

        for (FormattedCharSequence line : font.split(Component.translatable("eg.draviti.desc." + a.id()), bw)) {
            if (y > maxY) return;
            g.drawString(font, line, x, y, TEXT, false);
            y += 9;
        }
        y += 4;
        for (Component stat : stats(a)) {
            for (FormattedCharSequence line : font.split(Component.literal("• ").append(stat), bw)) {
                if (y > maxY) return;
                g.drawString(font, line, x, y, MUTED, false);
                y += 9;
            }
        }
    }

    /** Datos útiles de la técnica (de la config): daño, éter y enfriamiento. */
    private static List<Component> stats(Ability a) {
        List<Component> out = new ArrayList<>();
        var st = a.defaultStats();
        if (st.containsKey("damage")) out.add(Component.translatable("screen.draviti.eg.stat.damage", hearts(a.stat("damage"))));
        if (st.containsKey("final_damage")) out.add(Component.translatable("screen.draviti.eg.stat.final", hearts(a.stat("final_damage"))));
        if (st.containsKey("min_damage")) out.add(Component.translatable("screen.draviti.eg.stat.range_damage",
                hearts(a.stat("min_damage")), hearts(a.stat("min_damage") + a.stat("max_damage"))));
        if (st.containsKey("sonic_damage")) out.add(Component.translatable("screen.draviti.eg.stat.sonic", hearts(a.stat("sonic_damage"))));
        if (st.containsKey("ether_cost")) out.add(Component.translatable("screen.draviti.eg.stat.ether", (int) a.stat("ether_cost")));
        if (st.containsKey("ether_per_second")) out.add(Component.translatable("screen.draviti.eg.stat.ether_s", (int) a.stat("ether_per_second")));
        if (st.containsKey("ether_per_tick")) out.add(Component.translatable("screen.draviti.eg.stat.ether_s", (int) (a.stat("ether_per_tick") * 20)));
        if (a.cooldown() > 0) out.add(Component.translatable("screen.draviti.eg.stat.cooldown", String.format("%.1f", a.cooldown() / 20F)));
        if (a.id().equals("ingravidez")) out.add(Component.translatable("screen.draviti.eg.stat.ctrl_jump"));
        return out;
    }

    private static String hearts(double dmg) {
        double h = dmg / 2;
        return h == Math.floor(h) ? String.valueOf((int) h) : String.format("%.1f", h);
    }

    // ------------------------------------------------------------------ modelo 3D

    private void drawRight(GuiGraphics g, float partialTick) {
        panel(g, rx, top, rw, bottom - top);
        int x = rx + 6, bw = rw - 12;
        g.drawString(font, Component.translatable("screen.draviti.eg.view"), x, top + 6, MUTED, false);
        int boxY = top + 18, boxH = bottom - 6 - boxY - 22;
        g.fill(x, boxY, x + bw, boxY + boxH, 0x40000000);
        outline(g, x, boxY, bw, boxH, BORDER);
        EtherGearEntity e = EtherGearClient.preview();
        if (e != null && boxH > 30) {
            int scale = (int) Math.min(bw / 1.6F, boxH / 2.3F);
            g.enableScissor(x + 1, boxY + 1, x + bw - 1, boxY + boxH - 1);
            renderEntity(g, x + bw / 2, boxY + boxH - 14, scale, rotation, e);
            g.disableScissor();
        }
        int ty = boxY + boxH + 4;
        for (FormattedCharSequence line : font.split(Component.translatable("screen.draviti.eg.hint"), bw)) {
            if (ty > bottom - 10) break;
            g.drawString(font, line, x, ty, MUTED, false);
            ty += 9;
        }
    }

    private static void renderEntity(GuiGraphics g, int cx, int cy, int scale, float rot, EtherGearEntity e) {
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX((float) Math.toRadians(-8));
        pose.mul(camera);
        e.yBodyRot = 180F + rot;
        e.yBodyRotO = e.yBodyRot;
        e.setYRot(180F + rot);
        e.setXRot(0);
        e.yHeadRot = e.getYRot();
        e.yHeadRotO = e.getYRot();
        InventoryScreen.renderEntityInInventory(g, cx, cy, scale, pose, camera, e);
    }

    // ------------------------------------------------------------------ pie

    private record Btn(String id, Component label, boolean enabled, Component tip, int x, int y, int w, int h) {}

    private int[] closeBtn() {
        Component label = Component.translatable("screen.draviti.eg.close");
        int bw = Math.max(60, font.width(label) + 16);
        return new int[]{x0 + w - 6 - bw, y0 + h - FOOTER + 4, bw, 16};
    }

    private List<Btn> footerButtons(PowerData d) {
        List<Btn> list = new ArrayList<>();
        var mc = Minecraft.getInstance();
        if (mc.level == null) return list;
        long today = EtherGear.day(mc.player);
        int bx = x0 + 6, by = y0 + h - FOOTER + 4;

        Component ab = Component.translatable(abilitiesPage ? "screen.draviti.eg.back" : "screen.draviti.eg.abilities_btn");
        int aw = font.width(ab) + 16;
        list.add(new Btn("abilities", ab, true, Component.empty(), bx, by, aw, 16));
        bx += aw + 6;

        Component talk = Component.translatable("screen.draviti.eg.talk");
        int tw = font.width(talk) + 16;
        list.add(new Btn("talk", talk, today > d.egTalkDay, Component.translatable("screen.draviti.eg.tip.talk"), bx, by, tw, 16));
        bx += tw + 6;

        Component enter = Component.translatable("screen.draviti.eg.enter");
        int ew = font.width(enter) + 16;
        long left = EtherGearInterior.daysLeft(d, today);
        Component tip = d.egStars < EtherGearInterior.ENTER_STARS
                ? Component.translatable("screen.draviti.eg.tip.enter_stars", EtherGearInterior.ENTER_STARS)
                : d.egInside ? Component.translatable("screen.draviti.eg.tip.inside")
                : Component.translatable("screen.draviti.eg.tip.days", left);
        boolean ok = d.egStars >= EtherGearInterior.ENTER_STARS && left == 0 && !d.egInside;
        list.add(new Btn("enter", enter, ok, tip, bx, by, ew, 16));
        return list;
    }

    private void drawFooter(GuiGraphics g, PowerData d, int mouseX, int mouseY) {
        Btn hovered = null;
        for (Btn b : footerButtons(d)) {
            if (b.enabled()) {
                button(g, new int[]{b.x(), b.y(), b.w(), b.h()}, b.label(), mouseX, mouseY);
            } else {
                g.fill(b.x() - 1, b.y() - 1, b.x() + b.w() + 1, b.y() + b.h() + 1, BORDER);
                g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), 0xFF3A3346);
                g.drawString(font, b.label(), b.x() + b.w() / 2 - font.width(b.label()) / 2, b.y() + 4, 0xFF8A809A, false);
                if (hover(mouseX, mouseY, b.x(), b.y(), b.w(), b.h())) hovered = b;
            }
        }
        button(g, closeBtn(), Component.translatable("screen.draviti.eg.close"), mouseX, mouseY);
        if (hovered != null) g.renderTooltip(font, hovered.tip(), mouseX, mouseY);
    }

    private void button(GuiGraphics g, int[] r, Component label, int mouseX, int mouseY) {
        boolean hov = hover(mouseX, mouseY, r[0], r[1], r[2], r[3]);
        g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, BORDER);
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hov ? ACCENT : PANEL_L);
        g.drawString(font, label, r[0] + r[2] / 2 - font.width(label) / 2, r[1] + 4, hov ? 0xFFFFFFFF : TEXT, false);
    }

    // ------------------------------------------------------------------ entrada

    private static boolean hover(double mouseX, double mouseY, int x, int y, int bw, int bh) {
        return mouseX >= x && mouseX < x + bw && mouseY >= y && mouseY < y + bh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PowerData d = data();
        if (d != null && button == 0) {
            if (abilitiesPage) {
                int x = lx + 6, y = listTop();
                for (int k = 0; k < visibleRows() && k + scroll < abilities.size(); k++) {
                    if (hover(mouseX, mouseY, x, y + k * ROW, lw - 12, ROW - 2)) {
                        selected = k + scroll;
                        click();
                        return true;
                    }
                }
            }
            int[] c = closeBtn();
            if (hover(mouseX, mouseY, c[0], c[1], c[2], c[3])) {
                click();
                onClose();
                return true;
            }
            for (Btn b : footerButtons(d)) {
                if (!b.enabled() || !hover(mouseX, mouseY, b.x(), b.y(), b.w(), b.h())) continue;
                click();
                switch (b.id()) {
                    case "abilities" -> abilitiesPage = !abilitiesPage;
                    case "talk" -> ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.EG_TALK, 0, 0));
                    case "enter" -> {
                        ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.EG_ENTER, 0, 0));
                        onClose();
                    }
                    default -> {}
                }
                return true;
            }
            if (hover(mouseX, mouseY, rx, top, rw, bottom - top)) {
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (abilitiesPage && hover(mouseX, mouseY, lx, top, lw, bottom - top)) {
            scroll = Math.max(0, Math.min(abilities.size() - visibleRows(), scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            rotation += (float) dragX * 2F;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.ETHER_GEAR.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    // ------------------------------------------------------------------ utilidades de dibujo

    static void bar(GuiGraphics g, int x, int y, int w, int h, float progress, int color) {
        g.fill(x, y, x + w, y + h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF1A1224);
        int fill = (int) ((w - 2) * Math.max(0F, Math.min(1F, progress)));
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, color);
            g.fill(x + 1, y + 1, x + 1 + fill, y + 2, 0x40FFFFFF);
        }
    }

    /** Estrella de 7x7 píxeles (la de Seals). */
    static void star(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 3, y, x + 4, y + 2, color);
        g.fill(x, y + 2, x + 7, y + 3, color);
        g.fill(x + 1, y + 3, x + 6, y + 4, color);
        g.fill(x + 2, y + 4, x + 5, y + 5, color);
        g.fill(x + 1, y + 5, x + 3, y + 6, color);
        g.fill(x + 4, y + 5, x + 6, y + 6, color);
        g.fill(x + 1, y + 6, x + 2, y + 7, color);
        g.fill(x + 5, y + 6, x + 6, y + 7, color);
    }

    static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @SuppressWarnings("unused")
    private static boolean isHeld(AbilityType t) { return t == AbilityType.HELD; }
}
