package com.markadrii.draviti.power;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.Icon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Un poder: un conjunto de habilidades con nombre, icono, color y textura de barra (como en Cursed Crown). */
public final class Power {
    /** La textura de barra por defecto (formato de Palladium). */
    public static final ResourceLocation DEFAULT_BAR = Draviti.id("textures/gui/ability_bars/default.png");

    private final ResourceLocation id;
    private final Icon icon;
    private final int color;
    private final ResourceLocation barTexture;
    private final List<Ability> abilities;
    private final List<EnergyBar> energyBars;
    private final boolean allowBarShrinking;

    Power(ResourceLocation id, Icon icon, int color, ResourceLocation barTexture,
          List<EnergyBar> energyBars, boolean allowBarShrinking, List<Ability> abilities) {
        this.id = id;
        this.icon = icon;
        this.color = color;
        this.barTexture = barTexture == null ? DEFAULT_BAR : barTexture;
        this.energyBars = List.copyOf(energyBars);
        this.allowBarShrinking = allowBarShrinking;
        this.abilities = Collections.unmodifiableList(new ArrayList<>(abilities));
        for (Ability a : abilities) a.bindToPower(id.getPath());
    }

    public ResourceLocation id() { return id; }
    public Icon icon() { return icon; }
    public int color() { return color; }
    public ResourceLocation barTexture() { return barTexture; }
    public List<EnergyBar> energyBars() { return energyBars; }
    public boolean allowBarShrinking() { return allowBarShrinking; }
    public List<Ability> abilities() { return abilities; }

    public Optional<Ability> ability(String abilityId) {
        for (Ability a : abilities) if (a.id().equals(abilityId)) return Optional.of(a);
        return Optional.empty();
    }

    public Component name() {
        return Component.translatable("power." + id.getNamespace() + "." + id.getPath());
    }

    /** Barra de energía como en Palladium (color RGB). */
    public record EnergyBar(String name, int color, int max) {}
}
