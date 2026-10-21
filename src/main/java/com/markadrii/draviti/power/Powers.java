package com.markadrii.draviti.power;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.BarColor;
import com.markadrii.draviti.ability.Icon;
import com.markadrii.draviti.ability.KeyType;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.gravity.GravityAbilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.markadrii.draviti.ability.AbilityType.ACTION;
import static com.markadrii.draviti.ability.AbilityType.ACTIVATION;
import static com.markadrii.draviti.ability.AbilityType.HELD;
import static com.markadrii.draviti.ability.AbilityType.TOGGLE;

/** Registro de todos los poderes del mod (mismo formato que Cursed Crown: Powers). */
public final class Powers {
    private static final Map<ResourceLocation, Power> POWERS = new LinkedHashMap<>();

    private Powers() {}

    public static Power get(ResourceLocation id) { return POWERS.get(id); }
    public static Collection<Power> all() { return Collections.unmodifiableCollection(POWERS.values()); }

    public static Builder create(String path) { return new Builder(path); }

    public static void bootstrap() {
        if (!POWERS.isEmpty()) return;

        // ---------------- Dios de la Gravedad (Satan Gravity de Shiki Granbell, Edens Zero) ----------------
        // Barra: 4 listas de 5 (X para cambiar). Éter (morado) para las técnicas y energía cinética (dorada) para
        // Impacto Gravitatorio.
        create("gravity").icon("gui/powers/gravity.png").color(0x7FE6C8).bar("gui/ability_bars/gravity.png")
                .energyBar(Gravity.ETHER, 0x7FE6C8, Gravity.ETHER_MAX)
                .energyBar(Gravity.KINETIC, 0xF2C94C, Gravity.KINETIC_MAX)

                // Lista 1: Satan Gravity y los golpes básicos
                .add(Ability.builder("ingravidez", TOGGLE).index(0).cooldown(20).color(BarColor.LIGHT_BLUE).icon(icon("ingravidez"))
                        .stat("ether_cost", 20).stat("ether_per_second", 8)
                        .stat("sonic_speed", 2.2).stat("sonic_ether_per_second", 40).stat("sonic_damage", 8)
                        .stat("dash_speed", 1.7).stat("dash_ether_cost", 25)
                        .behavior(GravityAbilities.FLOAT))
                .add(Ability.builder("puno_gravitatorio", ACTION).index(1).cooldown(30).color(BarColor.PURPLE).icon(icon("puno_gravitatorio"))
                        .stat("ether_cost", 60).stat("damage", 14).stat("range", 5).stat("knockback", 2.6).stat("crater", 2.5)
                        .behavior(GravityAbilities.GRAVITY_FIST))
                .add(Ability.builder("canon_gravitatorio", ACTION).index(2).cooldown(40).color(BarColor.PURPLE).icon(icon("canon_gravitatorio"))
                        .stat("ether_cost", 70).stat("damage", 10).stat("knockback", 3.2).stat("speed", 1.6)
                        .behavior(GravityAbilities.GRAVITY_CANNON))
                .add(Ability.builder("onda_gravitatoria", ACTION).index(3).cooldown(200).color(BarColor.MAGENTA).icon(icon("onda_gravitatoria"))
                        .stat("ether_cost", 120).stat("radius", 14).stat("pull_ticks", 35)
                        .behavior(GravityAbilities.GRAVITY_WAVE))
                .add(Ability.builder("centro_gravedad", ACTION).index(4).cooldown(120).color(BarColor.MAGENTA).icon(icon("centro_gravedad"))
                        .stat("ether_cost", 100).stat("damage", 14).stat("range", 32).stat("crater", 2)
                        .behavior(GravityAbilities.GRAVITY_CENTER))

                // Lista 2: Magimech (Makai-ryū)
                .add(Ability.builder("rafaga_punos", HELD).index(5).cooldown(160).color(BarColor.PURPLE).icon(icon("rafaga_punos"))
                        .stat("ether_per_tick", 4).stat("damage", 3).stat("final_damage", 12).stat("range", 3.8)
                        .stat("knockback", 2.4).stat("max_ticks", 80).stat("min_duration", 10)
                        .behavior(GravityAbilities.FIST_FRENZY))
                .add(Ability.builder("patada_molinete", ACTION).index(6).cooldown(80).color(BarColor.PURPLE).icon(icon("patada_molinete"))
                        .stat("ether_cost", 80).stat("damage", 12).stat("radius", 4.5).stat("knockback", 2.2)
                        .behavior(GravityAbilities.WINDMILL_KICK))
                .add(Ability.builder("cometa_gravitatorio", ACTION).index(7).cooldown(160).color(BarColor.ORANGE).icon(icon("cometa_gravitatorio"))
                        .stat("ether_cost", 120).stat("damage", 16).stat("radius", 5).stat("jump", 1.7)
                        .behavior(GravityAbilities.GRAVITY_COMET))
                .add(Ability.builder("placaje_gravitatorio", ACTION).index(8).cooldown(140).color(BarColor.ORANGE).icon(icon("placaje_gravitatorio"))
                        .stat("ether_cost", 100).stat("damage", 18).stat("range", 22).stat("crater", 2.2)
                        .behavior(GravityAbilities.GRAVITY_TACKLE))
                .add(Ability.builder("impacto_gravitatorio", ACTION).index(9).cooldown(100).color(BarColor.YELLOW).icon(icon("impacto_gravitatorio"))
                        .stat("ether_cost", 40).stat("min_kinetic", 100).stat("min_damage", 10).stat("max_damage", 40)
                        .stat("min_radius", 2.5).stat("max_radius", 7)
                        .behavior(GravityAbilities.GRAVITY_IMPACT))

                // Lista 3: campos y control de la gravedad
                .add(Ability.builder("campo_gravitatorio", ACTIVATION).index(10).ticks(200).cooldown(500).color(BarColor.BLUE).icon(icon("campo_gravitatorio"))
                        .stat("ether_cost", 250).stat("radius", 10).stat("damage", 3)
                        .behavior(GravityAbilities.GRAVITY_FIELD))
                .add(Ability.builder("antigravedad", TOGGLE).index(11).cooldown(60).color(BarColor.LIGHT_BLUE).icon(icon("antigravedad"))
                        .stat("ether_per_second", 40).stat("radius", 5).stat("touch_radius", 1.8).stat("knockback", 1.8)
                        .behavior(GravityAbilities.ANTI_GRAVITY))
                .add(Ability.builder("peso_infernal", ACTION).index(12).cooldown(300).color(BarColor.BLUE).icon(icon("peso_infernal"))
                        .stat("ether_cost", 150).stat("radius", 16).stat("duration", 120).stat("damage", 4)
                        .behavior(GravityAbilities.HEAVY))
                .add(Ability.builder("ingravidez_ajena", ACTION).index(13).cooldown(200).color(BarColor.LIGHT_BLUE).icon(icon("ingravidez_ajena"))
                        .stat("ether_cost", 80).stat("range", 28).stat("radius", 3).stat("duration", 120)
                        .behavior(GravityAbilities.LIFT))
                .add(Ability.builder("meteor_breaker", HELD).index(14).cooldown(400).color(BarColor.RED).icon(icon("meteor_breaker"))
                        .stat("ether_cost", 300).stat("damage", 34).stat("radius", 4.5).stat("speed", 1.1).stat("charge_ticks", 50)
                        .behavior(GravityAbilities.METEOR_BREAKER))

                // Lista 4: técnica definitiva y Overdrive
                .add(Ability.builder("cielo_negro", ACTIVATION).index(15).ticks(70).cooldown(2400).color(BarColor.BLACK).icon(icon("cielo_negro"))
                        .stat("ether_cost", 1000).stat("damage", 70).stat("radius", 8).stat("max_size", 5).stat("speed", 0.9).stat("range", 48)
                        .behavior(GravityAbilities.BLACK_SKY))
                .add(Ability.builder("overdrive", TOGGLE).index(16).cooldown(300).color(BarColor.WHITE).icon(icon("overdrive"))
                        .stat("min_ether", 150).stat("ether_per_second", 10)
                        .behavior(GravityAbilities.OVERDRIVE))
                .add(Ability.builder("overdrive_completo", TOGGLE).index(17).cooldown(600).color(BarColor.BLACK).icon(icon("overdrive_completo"))
                        .requires("overdrive").stat("min_ether", 300).stat("ether_per_second", 10)
                        .stat("pull_radius", 9).stat("pull_strength", 0.16)
                        .behavior(GravityAbilities.FULL_OVERDRIVE))

                // Pasivas (sin tecla): solo guardan los valores ajustables
                .add(Ability.builder("pasivas", ACTION).hidden().key(KeyType.KEY_BIND)
                        .stat("ether_regen_per_second", 60).stat("damage_reduction", 0.2).stat("punch_knockback", 0.8)
                        .stat("kinetic_per_damage", 25).stat("kinetic_per_block", 20).stat("safe_fall_blocks", 8)
                        .stat("jump_bonus", 0.18)
                        .stat("ether_per_kill", 60).stat("ether_per_boss_kill", 400).stat("ether_per_player_kill", 200))
                .register();
    }

    private static String icon(String id) { return "gui/icons/gravity/" + id + ".png"; }

    public static final class Builder {
        private final String path;
        private Icon icon = Icon.item(Items.NETHER_STAR);
        private int color = 0xFFFFFF;
        private ResourceLocation bar;
        private final List<Ability> abilities = new ArrayList<>();
        private final List<Power.EnergyBar> energyBars = new ArrayList<>();
        private boolean shrink = true;

        private Builder(String path) { this.path = path; }

        public Builder icon(ItemLike item) { this.icon = Icon.item(item); return this; }
        public Builder icon(String texturePath) { this.icon = Icon.texture(Draviti.id("textures/" + texturePath)); return this; }
        public Builder color(int rgb) { this.color = rgb; return this; }
        public Builder bar(String texturePath) { this.bar = Draviti.id("textures/" + texturePath); return this; }
        public Builder energyBar(String name, int rgb, int max) { energyBars.add(new Power.EnergyBar(name, rgb, max)); return this; }
        public Builder noBarShrinking() { this.shrink = false; return this; }
        public Builder add(Ability.Builder ability) { abilities.add(ability.build()); return this; }

        public Power register() {
            Power p = new Power(Draviti.id(path), icon, color, bar, energyBars, shrink, abilities);
            POWERS.put(p.id(), p);
            return p;
        }
    }
}
