package com.markadrii.draviti.config;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.power.Power;
import com.markadrii.draviti.power.Powers;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * config/draviti-common.toml: romper bloques y los ajustes de cada habilidad (enfriamiento y valores),
 * igual que la sección [abilities] de Cursed Crown: Powers.
 */
public final class CommonConfig {
    private static final Map<String, ForgeConfigSpec.IntValue> COOLDOWNS = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.DoubleValue> STATS = new HashMap<>();

    public static ForgeConfigSpec SPEC;
    public static ForgeConfigSpec.BooleanValue BREAK_BLOCKS;
    public static ForgeConfigSpec.DoubleValue MAX_BLOCK_HARDNESS;
    public static ForgeConfigSpec.BooleanValue HURT_PLAYERS;
    /** Lo que cuesta desbloquear por primera vez las técnicas de cada estrella (1..5). */
    private static final ForgeConfigSpec.ConfigValue<?>[] UNLOCK_ITEM = new ForgeConfigSpec.ConfigValue<?>[6];
    private static final ForgeConfigSpec.IntValue[] UNLOCK_COUNT = new ForgeConfigSpec.IntValue[6];

    private CommonConfig() {}

    public static int cooldown(Ability a, int def) {
        var v = COOLDOWNS.get(a.powerPath() + "/" + a.id());
        return v == null || SPEC == null || !SPEC.isLoaded() ? def : v.get();
    }

    public static double stat(Ability a, String name, double def) {
        var v = STATS.get(a.powerPath() + "/" + a.id() + "/" + name);
        return v == null || SPEC == null || !SPEC.isLoaded() ? def : v.get();
    }

    public static boolean breakBlocks() { return SPEC == null || !SPEC.isLoaded() || BREAK_BLOCKS.get(); }
    public static float maxHardness() { return SPEC == null || !SPEC.isLoaded() ? 50F : MAX_BLOCK_HARDNESS.get().floatValue(); }
    public static boolean hurtPlayers() { return SPEC == null || !SPEC.isLoaded() || HURT_PLAYERS.get(); }

    /** Objeto (id) que piden las técnicas de esa estrella la primera vez. */
    public static String unlockItem(int star) {
        if (star < 1 || star > 5 || SPEC == null || !SPEC.isLoaded()) return "minecraft:gold_ingot";
        return String.valueOf(UNLOCK_ITEM[star].get());
    }

    /** Cuántos (0 = gratis). */
    public static int unlockCount(int star) {
        if (star < 1 || star > 5 || SPEC == null || !SPEC.isLoaded()) return 1;
        return UNLOCK_COUNT[star].get();
    }

    /** Se llama después de Powers.bootstrap(). */
    public static void build() {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        BREAK_BLOCKS = b.comment("Las técnicas de gravedad rompen bloques (cráteres). Respeta las protecciones (FTB Chunks...). Nunca rompe cofres ni bloques con inventario.")
                .define("break_blocks", true);
        MAX_BLOCK_HARDNESS = b.comment("Dureza máxima de los bloques que se pueden romper (obsidiana = 50; la roca madre nunca).")
                .defineInRange("max_block_hardness", 50.0, 0.0, 1000.0);
        HURT_PLAYERS = b.comment("Las técnicas dañan a otros jugadores.")
                .define("hurt_players", true);
        b.pop();

        b.comment("Ether Gear: lo que hay que pagar la primera vez para desbloquear cada técnica, según la estrella que pide.",
                "item = id del objeto (p. ej. minecraft:gold_ingot, minecraft:diamond); count = cuántos (0 = gratis).").push("ether_gear");
        for (int s = 1; s <= 5; s++) {
            b.push("star_" + s);
            UNLOCK_ITEM[s] = b.define("item", "minecraft:gold_ingot");
            UNLOCK_COUNT[s] = b.defineInRange("count", 1, 0, 64 * 36);
            b.pop();
        }
        b.pop();

        b.comment("Ajustes de cada habilidad. cooldown en ticks (20 ticks = 1 segundo); damage en medios corazones;",
                "ether_cost = éter que gasta (la barra llega a 1000).").push("abilities");
        for (Power p : Powers.all()) {
            b.push(p.id().getPath());
            for (Ability a : p.abilities()) {
                if (a.defaultCooldown() <= 0 && a.defaultStats().isEmpty()) continue;
                String key = p.id().getPath() + "/" + a.id();
                b.push(a.id());
                COOLDOWNS.put(key, b.defineInRange("cooldown", a.defaultCooldown(), 0, 1_000_000));
                for (var st : a.defaultStats().entrySet())
                    STATS.put(key + "/" + st.getKey(), b.defineInRange(st.getKey(), st.getValue(), -1_000_000.0, 1_000_000.0));
                b.pop();
            }
            b.pop();
        }
        b.pop();
        SPEC = b.build();
    }
}
