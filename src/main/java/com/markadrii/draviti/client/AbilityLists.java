package com.markadrii.draviti.client;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.power.Power;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Listas de la barra, con la misma lógica que Palladium (AbilityBarRenderer.getAbilityLists):
 * list_index -> lista = índice / 5, hueco = índice % 5; sin índice van al final;
 * se saltan las listas vacías o con todo bloqueado; con una sola lista y una sola habilidad la barra se encoge.
 */
public final class AbilityLists {
    public static final int SIZE = 5;

    public static final class AbilityList {
        public final Power power;
        private final List<List<Ability>> slots = new ArrayList<>();
        public boolean simple;

        AbilityList(Power power) {
            this.power = power;
            for (int i = 0; i < SIZE; i++) slots.add(new ArrayList<>());
        }

        void add(int index, Ability a) { slots.get(index).add(a); }

        /** Como getDisplayedAbilities: en cada hueco, la primera desbloqueada (o la primera si ninguna lo está). */
        public Ability[] displayed(PowerData d) {
            Ability[] out = new Ability[SIZE];
            for (int i = 0; i < SIZE; i++)
                for (Ability a : slots.get(i)) {
                    if (out[i] == null) out[i] = a;
                    else if (!d.isUnlocked(power, out[i]) && d.isUnlocked(power, a)) out[i] = a;
                }
            return out;
        }

        boolean isEmpty() { return slots.stream().allMatch(List::isEmpty); }

        boolean fullyLocked(PowerData d) {
            for (Ability a : displayed(d)) if (a != null && d.isUnlocked(power, a)) return false;
            return true;
        }

        void simplify(PowerData d) {
            if (!power.allowBarShrinking() || !power.energyBars().isEmpty()) return;
            int count = 0;
            Ability last = null;
            for (Ability a : displayed(d)) if (a != null && d.isUnlocked(power, a)) { count++; last = a; }
            simple = count == 1;
            if (simple) {
                slots.forEach(List::clear);
                slots.get(0).add(last);
            }
        }
    }

    private static List<AbilityList> lists = new ArrayList<>();
    private static int selected;

    private AbilityLists() {}

    public static List<AbilityList> lists() { return lists; }

    /** Se recalcula cada tick de cliente, como en Palladium. */
    public static void update() {
        var player = Minecraft.getInstance().player;
        if (player == null) { lists = new ArrayList<>(); return; }
        PowerData d = PowerCapability.get(player).orElse(null);
        List<AbilityList> out = new ArrayList<>();
        if (d != null) {
            for (Power p : d.getPowers()) {
                List<AbilityList> indexed = new ArrayList<>();
                List<AbilityList> rest = new ArrayList<>();
                List<Ability> remaining = new ArrayList<>();
                for (Ability a : p.abilities()) {
                    if (a.hiddenInBar()) continue;
                    int i = a.listIndex();
                    if (i >= 0) {
                        int li = Math.floorDiv(i, SIZE);
                        while (indexed.size() - 1 < li) indexed.add(new AbilityList(p));
                        indexed.get(li).add(i % SIZE, a);
                    } else remaining.add(a);
                }
                for (int i = 0; i < remaining.size(); i++) {
                    int li = Math.floorDiv(i, SIZE);
                    while (rest.size() - 1 < li) rest.add(new AbilityList(p));
                    rest.get(li).add(i % SIZE, remaining.get(i));
                }
                for (AbilityList l : indexed) if (!l.isEmpty() && !l.fullyLocked(d)) out.add(l);
                for (AbilityList l : rest) if (!l.isEmpty() && !l.fullyLocked(d)) out.add(l);
                // Poder todavía sin habilidades en la barra (p. ej. Aqua): se enseña la barra vacía
                if (indexed.isEmpty() && rest.isEmpty() && p.abilities().stream().noneMatch(a -> !a.hiddenInBar()))
                    out.add(new AbilityList(p));
            }
            if (out.size() <= 1) out.forEach(l -> l.simplify(d));
        }
        lists = out;
        if (selected >= lists.size()) selected = Math.max(0, lists.size() - 1);
    }

    public static AbilityList selected() {
        if (lists.isEmpty()) return null;
        if (selected >= lists.size() || selected < 0) selected = 0;
        return lists.get(selected);
    }

    /** Elige la lista (cuando se comparte la barra con Cursed Crown, la elige ClientEvents). */
    public static void select(int index) {
        selected = Math.max(0, Math.min(index, Math.max(0, lists.size() - 1)));
    }

    public static int selectedIndex() { return selected; }

    public static void scroll(boolean up) {
        selected += up ? 1 : -1;
        if (selected >= lists.size()) selected = 0;
        else if (selected < 0) selected = Math.max(0, lists.size() - 1);
    }
}
