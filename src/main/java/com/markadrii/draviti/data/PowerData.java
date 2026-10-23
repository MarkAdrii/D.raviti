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
    /** Máximo de cada barra para este jugador ("poder/barra" -> máximo); si no está, el del poder. */
    private final Map<String, Integer> energyMax = new HashMap<>();
    /** Ha cambiado algo que el cliente tiene que ver (energía): se envía al final del tick. No se guarda. */
    public boolean dirty;
    /** Las técnicas rompen bloques (se cambia pulsando Shift tres veces seguidas). */
    public boolean breakBlocks = true;
    /** Se ve la marca del Ether Gear en el brazo (F dos veces para ocultarla). */
    public boolean showMark = true;

    // ---- Vínculo con el Ether Gear (menú de la U) ----
    /** Estrellas (0-5) y afinidad dentro de la estrella actual. */
    public int egStars, egAffinity;
    /** Último día de Minecraft en que se habló, se entró en su interior y se contó para la afinidad diaria. */
    public long egTalkDay = -100, egEnterDay = -100, egDay = -1;
    /** Está dentro del interior del Ether Gear y a dónde volver (solo servidor). */
    public boolean egInside;
    public String egReturnDim = "";
    public double egRx, egRy, egRz;
    public float egRyaw;
    /** Técnicas ya pagadas (1 lingote de oro la primera vez). */
    public final Set<String> egBought = new java.util.HashSet<>();
    /** Respuestas ya usadas en esta visita (bits). */
    public int egVisit;

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
        // Estrellas del Ether Gear
        if (power.id().equals(com.markadrii.draviti.gravity.Gravity.POWER_ID)
                && !com.markadrii.draviti.ethergear.EtherGear.unlocked(this, ability.id())) return false;
        if (ability.requires() == null) return true;
        return power.ability(ability.requires()).map(r -> state(power, r).enabled).orElse(true);
    }

    public int energy(Power power, String bar) { return energy.getOrDefault(power.id() + "/" + bar, 0); }
    public int maxEnergy(Power power, String bar, int def) {
        return energyMax.getOrDefault(power.id() + "/" + bar, def) + com.markadrii.draviti.ethergear.EtherGear.bonusMax(this, power, bar);
    }
    public void setMaxEnergy(Power power, String bar, int max) { energyMax.put(power.id() + "/" + bar, max); dirty = true; }
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
        energyMax.clear();
        energyMax.putAll(other.energyMax);
        breakBlocks = other.breakBlocks;
        showMark = other.showMark;
        egStars = other.egStars; egAffinity = other.egAffinity;
        egTalkDay = other.egTalkDay; egEnterDay = other.egEnterDay; egDay = other.egDay;
        egInside = other.egInside; egReturnDim = other.egReturnDim;
        egRx = other.egRx; egRy = other.egRy; egRz = other.egRz; egRyaw = other.egRyaw; egVisit = other.egVisit;
        egBought.clear(); egBought.addAll(other.egBought);
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
        CompoundTag mx = new CompoundTag();
        energyMax.forEach(mx::putInt);
        tag.put("EnergyMax", mx);
        tag.putBoolean("BreakBlocks", breakBlocks);
        tag.putBoolean("ShowMark", showMark);
        CompoundTag eg = new CompoundTag();
        eg.putInt("Stars", egStars);
        eg.putInt("Affinity", egAffinity);
        eg.putLong("TalkDay", egTalkDay);
        eg.putLong("EnterDay", egEnterDay);
        eg.putLong("Day", egDay);
        eg.putBoolean("Inside", egInside);
        eg.putString("ReturnDim", egReturnDim);
        eg.putDouble("Rx", egRx); eg.putDouble("Ry", egRy); eg.putDouble("Rz", egRz); eg.putFloat("Ryaw", egRyaw);
        ListTag bought = new ListTag();
        for (String id : egBought) bought.add(StringTag.valueOf(id));
        eg.put("Bought", bought);
        tag.put("EtherGear", eg);
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
        energyMax.clear();
        CompoundTag mx = tag.getCompound("EnergyMax");
        for (String k : mx.getAllKeys()) energyMax.put(k, mx.getInt(k));
        breakBlocks = !tag.contains("BreakBlocks") || tag.getBoolean("BreakBlocks");
        showMark = !tag.contains("ShowMark") || tag.getBoolean("ShowMark");
        CompoundTag eg = tag.getCompound("EtherGear");
        egStars = eg.getInt("Stars");
        egAffinity = eg.getInt("Affinity");
        egTalkDay = eg.contains("TalkDay") ? eg.getLong("TalkDay") : -100;
        egEnterDay = eg.contains("EnterDay") ? eg.getLong("EnterDay") : -100;
        egDay = eg.contains("Day") ? eg.getLong("Day") : -1;
        egInside = eg.getBoolean("Inside");
        egReturnDim = eg.getString("ReturnDim");
        egRx = eg.getDouble("Rx"); egRy = eg.getDouble("Ry"); egRz = eg.getDouble("Rz"); egRyaw = eg.getFloat("Ryaw");
        egBought.clear();
        ListTag bought = eg.getList("Bought", Tag.TAG_STRING);
        for (int i = 0; i < bought.size(); i++) egBought.add(bought.getString(i));
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
        buf.writeVarInt(energyMax.size());
        energyMax.forEach((k, v) -> { buf.writeUtf(k); buf.writeVarInt(v); });
        buf.writeVarInt(egStars);
        buf.writeVarInt(egAffinity);
        buf.writeLong(egTalkDay);
        buf.writeLong(egEnterDay);
        buf.writeBoolean(egInside);
        // Lo que cuesta desbloquear en cada estrella (config del servidor)
        for (int s = 1; s <= 5; s++) {
            buf.writeUtf(com.markadrii.draviti.config.CommonConfig.unlockItem(s));
            buf.writeVarInt(com.markadrii.draviti.config.CommonConfig.unlockCount(s));
        }
        buf.writeVarInt(egBought.size());
        for (String id : egBought) buf.writeUtf(id);
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
        int mx = buf.readVarInt();
        for (int i = 0; i < mx; i++) d.energyMax.put(buf.readUtf(), buf.readVarInt());
        d.egStars = buf.readVarInt();
        d.egAffinity = buf.readVarInt();
        d.egTalkDay = buf.readLong();
        d.egEnterDay = buf.readLong();
        d.egInside = buf.readBoolean();
        String[] items = new String[6];
        int[] counts = new int[6];
        for (int s = 1; s <= 5; s++) { items[s] = buf.readUtf(); counts[s] = buf.readVarInt(); }
        com.markadrii.draviti.ethergear.EtherGear.syncedItems = items;
        com.markadrii.draviti.ethergear.EtherGear.syncedCounts = counts;
        int nb = buf.readVarInt();
        for (int i = 0; i < nb; i++) d.egBought.add(buf.readUtf());
        return d;
    }
}
