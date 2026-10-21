package com.markadrii.draviti.data;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.power.Power;
import com.markadrii.draviti.power.Powers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Poderes que tiene un jugador y el estado de cada habilidad. */
public class PowerData {
    private final Set<ResourceLocation> powers = new LinkedHashSet<>();
    private final Map<String, AbilityState> states = new HashMap<>();
    /** Barras de energía: "poder/barra" -> valor. */
    private final Map<String, Integer> energy = new HashMap<>();
    /** Ha cambiado algo que el cliente tiene que ver (energía): se envía al final del tick. No se guarda. */
    public boolean dirty;
    /** Las técnicas rompen bloques (se cambia pulsando Shift tres veces seguidas). */
    public boolean breakBlocks = true;

    public static String key(Power power, Ability ability) {
        return power.id() + "/" + ability.id();
    }

    public List<Power> getPowers() {
        List<Power> list = new ArrayList<>();
        for (ResourceLocation id : powers) {
            Power p = Powers.get(id);
            if (p != null) list.add(p);
        }
        return list;
    }

    public boolean hasPower(ResourceLocation id) { return powers.contains(id); }
    public boolean addPower(ResourceLocation id) { return Powers.get(id) != null && powers.add(id); }
    public boolean removePower(ResourceLocation id) { return powers.remove(id); }
    public Set<ResourceLocation> powerIds() { return powers; }

    public AbilityState state(Power power, Ability ability) {
        return states.computeIfAbsent(key(power, ability), k -> new AbilityState());
    }

    /** ¿Está desbloqueada? (si necesita otra habilidad activa del mismo poder). */
    public boolean isUnlocked(Power power, Ability ability) {
        if (ability.requires() == null) return true;
        return power.ability(ability.requires()).map(r -> state(power, r).enabled).orElse(true);
    }

    public int energy(Power power, String bar) { return energy.getOrDefault(power.id() + "/" + bar, 0); }
    public boolean hasEnergy(Power power, String bar) { return energy.containsKey(power.id() + "/" + bar); }
    public void setEnergy(Power power, String bar, int value) {
        Integer old = energy.put(power.id() + "/" + bar, value);
        if (old == null || old != value) dirty = true;
    }

    public void removeStatesOf(Power power) {
        String prefix = power.id() + "/";
        states.keySet().removeIf(k -> k.startsWith(prefix));
        energy.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void copyFrom(PowerData other) {
        powers.clear();
        powers.addAll(other.powers);
        states.clear();
        other.states.forEach((k, v) -> states.put(k, v.copy()));
        energy.clear();
        energy.putAll(other.energy);
        breakBlocks = other.breakBlocks;
    }

    // ---- NBT ----
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ResourceLocation id : powers) list.add(StringTag.valueOf(id.toString()));
        tag.put("Powers", list);
        CompoundTag st = new CompoundTag();
        states.forEach((k, s) -> {
            CompoundTag t = new CompoundTag();
            t.putBoolean("Enabled", s.enabled);
            t.putInt("Cooldown", s.cooldown);
            t.putInt("MaxCooldown", s.maxCooldown);
            t.putInt("Activation", s.activationTimer);
            t.putInt("MaxActivation", s.maxActivationTimer);
            st.put(k, t);
        });
        tag.put("Abilities", st);
        CompoundTag en = new CompoundTag();
        energy.forEach(en::putInt);
        tag.put("Energy", en);
        tag.putBoolean("BreakBlocks", breakBlocks);
        return tag;
    }

    public void load(CompoundTag tag) {
        powers.clear();
        states.clear();
        energy.clear();
        ListTag list = tag.getList("Powers", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) powers.add(id);
        }
        CompoundTag st = tag.getCompound("Abilities");
        for (String k : st.getAllKeys()) {
            CompoundTag t = st.getCompound(k);
            AbilityState s = new AbilityState();
            s.enabled = t.getBoolean("Enabled");
            s.cooldown = t.getInt("Cooldown");
            s.maxCooldown = t.getInt("MaxCooldown");
            s.activationTimer = t.getInt("Activation");
            s.maxActivationTimer = t.getInt("MaxActivation");
            states.put(k, s);
        }
        CompoundTag en = tag.getCompound("Energy");
        for (String k : en.getAllKeys()) energy.put(k, en.getInt(k));
        breakBlocks = !tag.contains("BreakBlocks") || tag.getBoolean("BreakBlocks");
    }

    // ---- Red ----
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(powers.size());
        for (ResourceLocation id : powers) buf.writeResourceLocation(id);
        buf.writeVarInt(states.size());
        states.forEach((k, s) -> {
            buf.writeUtf(k);
            buf.writeBoolean(s.enabled);
            buf.writeVarInt(s.cooldown);
            buf.writeVarInt(s.maxCooldown);
            buf.writeVarInt(s.activationTimer);
            buf.writeVarInt(s.maxActivationTimer);
        });
        buf.writeVarInt(energy.size());
        energy.forEach((k, v) -> { buf.writeUtf(k); buf.writeVarInt(v); });
    }

    public static PowerData read(FriendlyByteBuf buf) {
        PowerData d = new PowerData();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) d.powers.add(buf.readResourceLocation());
        int m = buf.readVarInt();
        for (int i = 0; i < m; i++) {
            String k = buf.readUtf();
            AbilityState s = new AbilityState();
            s.enabled = buf.readBoolean();
            s.cooldown = buf.readVarInt();
            s.maxCooldown = buf.readVarInt();
            s.activationTimer = buf.readVarInt();
            s.maxActivationTimer = buf.readVarInt();
            d.states.put(k, s);
        }
        int e = buf.readVarInt();
        for (int i = 0; i < e; i++) d.energy.put(buf.readUtf(), buf.readVarInt());
        return d;
    }
}
