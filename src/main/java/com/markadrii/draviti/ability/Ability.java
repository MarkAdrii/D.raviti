package com.markadrii.draviti.ability;

import com.markadrii.draviti.Draviti;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

/** Definición de una habilidad dentro de un poder. */
public final class Ability {
    private final String id;
    private final AbilityType type;
    private final KeyType keyType;
    private final Icon icon;
    private final int cooldown;
    private final int activationTicks;
    private final int listIndex;
    private final BarColor color;
    private final String requires;
    private final AbilityBehavior behavior;
    private final boolean hiddenInBar;
    private final boolean needsEmptyHand;
    private final java.util.LinkedHashMap<String, Double> stats;
    private String powerPath = "";

    private Ability(Builder b) {
        this.id = b.id;
        this.type = b.type;
        this.keyType = b.keyType;
        this.icon = b.icon;
        this.cooldown = b.cooldown;
        this.activationTicks = b.activationTicks;
        this.listIndex = b.listIndex;
        this.color = b.color;
        this.requires = b.requires;
        this.behavior = b.behavior;
        this.hiddenInBar = b.hiddenInBar;
        this.needsEmptyHand = b.needsEmptyHand;
        this.stats = b.stats;
    }

    public void bindToPower(String powerPath) { this.powerPath = powerPath; }

    public String id() { return id; }
    public AbilityType type() { return type; }
    public KeyType keyType() { return keyType; }
    /** Enfriamiento en ticks (se puede cambiar en la config). */
    public int cooldown() { return com.markadrii.draviti.config.CommonConfig.cooldown(this, cooldown); }
    public int defaultCooldown() { return cooldown; }
    public String powerPath() { return powerPath; }
    public boolean hiddenInBar() { return hiddenInBar; }
    public boolean needsEmptyHand() { return needsEmptyHand; }
    /** Valores ajustables (daño, alcance...) con sus valores por defecto. */
    public java.util.Map<String, Double> defaultStats() { return stats; }
    /** Valor ajustable leído de la config (o el de por defecto). */
    public double stat(String name) { return com.markadrii.draviti.config.CommonConfig.stat(this, name, stats.getOrDefault(name, 0.0)); }
    public float statF(String name) { return (float) stat(name); }
    public int statI(String name) { return (int) Math.round(stat(name)); }
    public int activationTicks() { return activationTicks; }
    /** Posición en la barra como en Palladium: lista = índice / 5, hueco = índice % 5. -1 = la siguiente libre. */
    public int listIndex() { return listIndex; }
    public BarColor color() { return color; }
    /** Habilidad del mismo poder que tiene que estar activa para desbloquear esta (null = siempre desbloqueada). */
    public String requires() { return requires; }
    public AbilityBehavior behavior() { return behavior; }
    public Icon icon() { return icon; }

    public String translationKey() { return "ability.draviti." + powerPath + "." + id; }
    public Component name() { return Component.translatable(translationKey()); }

    public static Builder builder(String id, AbilityType type) { return new Builder(id, type); }

    public static final class Builder {
        private final String id;
        private final AbilityType type;
        private KeyType keyType = KeyType.KEY_BIND;
        private Icon icon = Icon.item(net.minecraft.world.item.Items.BARRIER);
        private int cooldown;
        private int activationTicks = 20;
        private int listIndex = -1;
        private BarColor color = BarColor.LIGHT_GRAY;
        private String requires;
        private AbilityBehavior behavior = AbilityBehavior.NONE;
        private boolean hiddenInBar;
        private boolean needsEmptyHand;
        private final java.util.LinkedHashMap<String, Double> stats = new java.util.LinkedHashMap<>();

        private Builder(String id, AbilityType type) { this.id = id; this.type = type; }

        public Builder icon(Icon icon) { this.icon = icon; return this; }
        public Builder icon(ItemLike item) { this.icon = Icon.item(item); return this; }
        /** Textura dentro de assets/draviti/textures/, p. ej. "gui/icons/error/aim.png". */
        public Builder icon(String texturePath) { this.icon = Icon.texture(Draviti.id("textures/" + texturePath)); return this; }
        public Builder cooldown(int ticks) { this.cooldown = ticks; return this; }
        /** Solo ACTIVATION: cuántos ticks está activa. */
        public Builder ticks(int ticks) { this.activationTicks = ticks; return this; }
        public Builder index(int listIndex) { this.listIndex = listIndex; return this; }
        public Builder key(KeyType keyType) { this.keyType = keyType; return this; }
        public Builder color(BarColor color) { this.color = color; return this; }
        public Builder requires(String abilityId) { this.requires = abilityId; return this; }
        public Builder behavior(AbilityBehavior behavior) { this.behavior = behavior; return this; }
        /** No sale en la barra (se usa con clic, como las sub-habilidades de Palladium). */
        public Builder hidden() { this.hiddenInBar = true; return this; }
        public Builder emptyHand() { this.needsEmptyHand = true; return this; }
        /** Valor ajustable en config/draviti-common.toml. */
        public Builder stat(String name, double value) { stats.put(name, value); return this; }
        public Ability build() { return new Ability(this); }
    }
}
