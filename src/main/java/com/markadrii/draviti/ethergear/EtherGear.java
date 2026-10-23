package com.markadrii.draviti.ethergear;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.data.PowerManager;
import com.markadrii.draviti.gravity.Fx;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.network.EtherGearPacket;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.power.Power;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El vínculo con el Ether Gear, la entidad que vive dentro del poder (menú de la U, como el de Seals).
 * <ul>
 *   <li>5 estrellas. Cada estrella desbloquea técnicas; la 3.ª da +1500 de éter máximo.</li>
 *   <li>Afinidad: hablar con él (1 vez al día), cada día de Minecraft, estar cerca de otros jugadores, usar técnicas
 *       y charlar o meditar con él en su interior. La barra mide 100, 300, 700, 1000, 1500 y 2000 según las estrellas.</li>
 * </ul>
 */
public final class EtherGear {
    public static final int MAX_STARS = 5;
    /** Afinidad necesaria para la siguiente estrella según las estrellas que tienes (con 5, la barra de 2000 es la final). */
    public static final int[] AFFINITY_MAX = {100, 300, 700, 1000, 1500, 2000};
    public static final int ETHER_BONUS = 1500, ETHER_BONUS_STARS = 3;
    public static final int TALK_LINES = 14;

    /** Estrella que pide cada técnica (las que no salen aquí están desde el principio). */
    private static final Map<String, Integer> STARS = new HashMap<>();
    static {
        // 0: puño, cañón, ingravidez y antigravedad
        for (String id : List.of("gravity_swap", "onda_gravitatoria", "rafaga_punos", "patada_molinete")) STARS.put(id, 1);
        for (String id : List.of("overdrive", "centro_gravedad", "cometa_gravitatorio", "placaje_gravitatorio", "ingravidez_ajena")) STARS.put(id, 2);
        for (String id : List.of("impacto_gravitatorio", "peso_infernal", "campo_gravitatorio")) STARS.put(id, 3);
        for (String id : List.of("meteor_breaker", "cielo_negro")) STARS.put(id, 4);
        STARS.put("overdrive_completo", 5);
    }

    private static final Map<UUID, Long> LAST_ABILITY_GAIN = new HashMap<>();

    private EtherGear() {}

    public static int starsFor(String abilityId) { return STARS.getOrDefault(abilityId, 0); }

    /** ¿Ya la puede usar? Estrella conseguida y, si pide alguna, pagada una vez (1 lingote de oro). */
    public static boolean unlocked(PowerData d, String abilityId) {
        int need = starsFor(abilityId);
        return d.egStars >= need && (need == 0 || d.egBought.contains(abilityId));
    }

    /** Se puede pagar ya (tiene la estrella y aún no la ha comprado). */
    public static boolean canBuy(PowerData d, String abilityId) {
        int need = starsFor(abilityId);
        return need > 0 && d.egStars >= need && !d.egBought.contains(abilityId);
    }

    /** Botón Desbloquear del menú: cobra 1 lingote de oro la primera vez. index = posición en el poder. */
    public static void buy(ServerPlayer p, int index) {
        PowerData d = Gravity.data(p);
        Power pw = Gravity.power();
        if (d == null || pw == null || !Gravity.has(p) || index < 0 || index >= pw.abilities().size()) return;
        Ability a = pw.abilities().get(index);
        if (!canBuy(d, a.id())) return;
        if (!p.getAbilities().instabuild && !takeGold(p)) {
            p.displayClientMessage(Component.translatable("message.draviti.eg.no_gold").withStyle(ChatFormatting.RED), true);
            return;
        }
        d.egBought.add(a.id());
        p.sendSystemMessage(Component.translatable("message.draviti.eg.bought", a.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
        Gravity.sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1F, 1.2F);
        Gravity.sound(p, p.position(), SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
        Gravity.fxOn(p, Fx.WEIGHTLESS, p, p.getBbHeight(), 20);
        PowerManager.sync(p);
    }

    private static boolean takeGold(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.is(net.minecraft.world.item.Items.GOLD_INGOT)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    public static int affinityMax(int stars) { return AFFINITY_MAX[Math.max(0, Math.min(MAX_STARS, stars))]; }

    /** Éter extra por estrellas (se suma al máximo de la barra). */
    public static int bonusMax(PowerData d, Power power, String bar) {
        return d.egStars >= ETHER_BONUS_STARS && Gravity.ETHER.equals(bar) && power.id().equals(Gravity.POWER_ID) ? ETHER_BONUS : 0;
    }

    public static boolean maxed(PowerData d) { return d.egStars >= MAX_STARS && d.egAffinity >= affinityMax(MAX_STARS); }

    /** Día de Minecraft (sirve en el cliente y en el servidor). */
    public static long day(Player p) { return p.level().getDayTime() / 24000L; }

    public static long today(MinecraftServer server) { return server.overworld().getDayTime() / 24000L; }

    static double stat(String name, double def) {
        Power pw = Gravity.power();
        Ability a = pw == null ? null : pw.ability("pasivas").orElse(null);
        return a == null ? def : a.stat(name);
    }

    // ------------------------------------------------------------------ afinidad

    /** Suma afinidad (y estrellas si se llena la barra). reason = clave del motivo, null = sin aviso. */
    public static void addAffinity(ServerPlayer p, int amount, String reason) {
        PowerData d = Gravity.data(p);
        if (d == null || !Gravity.has(p) || amount <= 0 || maxed(d)) return;
        d.egAffinity += amount;
        while (d.egStars < MAX_STARS && d.egAffinity >= affinityMax(d.egStars)) {
            d.egAffinity -= affinityMax(d.egStars);
            d.egStars++;
            starUp(p, d.egStars);
        }
        if (d.egStars >= MAX_STARS) d.egAffinity = Math.min(d.egAffinity, affinityMax(MAX_STARS));
        if (reason != null)
            p.displayClientMessage(Component.translatable("message.draviti.eg.affinity", amount,
                    Component.translatable(reason)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        PowerManager.sync(p);
    }

    /** Pone estrellas y afinidad a mano (comando). */
    public static void set(ServerPlayer p, int stars, int affinity) {
        PowerData d = Gravity.data(p);
        if (d == null) return;
        d.egStars = Math.max(0, Math.min(MAX_STARS, stars));
        d.egAffinity = Math.max(0, Math.min(affinityMax(d.egStars) - (d.egStars < MAX_STARS ? 1 : 0), affinity));
        relock(p, d);
        PowerManager.sync(p);
    }

    private static void starUp(ServerPlayer p, int stars) {
        p.sendSystemMessage(Component.translatable("message.draviti.eg.star", "★".repeat(stars))
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.translatable("message.draviti.eg.star." + stars).withStyle(ChatFormatting.WHITE));
        Gravity.sound(p, p.position(), SoundEvents.PLAYER_LEVELUP, 1F, 0.8F);
        Gravity.sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.6F);
        Gravity.fxOn(p, Fx.WEIGHTLESS, p, p.getBbHeight(), 40);
        Gravity.fx(p, Fx.SHOCKWAVE, p.position(), 4, 0);
        if (stars == ETHER_BONUS_STARS) Gravity.setEther(p, Gravity.ether(p) + ETHER_BONUS);
    }

    /** Si se quitan estrellas, apaga lo que ya no se puede usar. */
    private static void relock(ServerPlayer p, PowerData d) {
        Power pw = Gravity.power();
        if (pw == null) return;
        for (Ability a : pw.abilities())
            if (d.state(pw, a).enabled && !d.isUnlocked(pw, a)) PowerManager.forceStop(p, pw, a, false);
        Gravity.setEther(p, Gravity.ether(p));
    }

    /** Cada vez que se usa una técnica: un poco de afinidad (como mucho cada 15 s). */
    public static void onAbilityUsed(ServerPlayer p) {
        if (!Gravity.has(p)) return;
        long now = p.level().getGameTime();
        Long last = LAST_ABILITY_GAIN.get(p.getUUID());
        if (last != null && now - last < 300) return;
        LAST_ABILITY_GAIN.put(p.getUUID(), now);
        addAffinity(p, (int) stat("afinidad_habilidades", 2), null);
    }

    /** Cada tick del jugador (servidor). */
    public static void tick(ServerPlayer p) {
        if (p.tickCount % 20 != 0 || !Gravity.has(p)) return;
        PowerData d = Gravity.data(p);
        long today = today(p.server);
        if (d.egDay < 0 || d.egDay > today) {
            d.egDay = today;
        } else if (today > d.egDay) {
            long days = Math.min(7, today - d.egDay);
            d.egDay = today;
            addAffinity(p, (int) (days * stat("afinidad_por_dia", 25)), "message.draviti.eg.reason.day");
        }
        // Cerca de otros jugadores (cada minuto)
        if (p.tickCount % 1200 == 0 && !p.isSpectator()) {
            boolean near = p.serverLevel().players().stream()
                    .anyMatch(o -> o != p && !o.isSpectator() && o.distanceToSqr(p) < 24 * 24);
            if (near) addAffinity(p, (int) stat("afinidad_cerca_de_jugadores", 3), "message.draviti.eg.reason.near");
        }
    }

    // ------------------------------------------------------------------ hablar (botón de la U)

    public static void talk(ServerPlayer p) {
        PowerData d = Gravity.data(p);
        if (d == null || !Gravity.has(p)) return;
        long today = today(p.server);
        if (today <= d.egTalkDay) {
            p.displayClientMessage(Component.translatable("message.draviti.eg.talk_wait").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        d.egTalkDay = today;
        // Las frases van cambiando con las estrellas (cuanto más vínculo, más se abre)
        int span = Math.min(TALK_LINES, 6 + d.egStars * 2);
        Component line = Component.translatable("eg.draviti.talk." + p.getRandom().nextInt(span));
        sendLine(p, line);
        p.sendSystemMessage(Component.literal("[").append(Component.translatable("entity.draviti.ether_gear")).append("] ")
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(line.copy().withStyle(ChatFormatting.WHITE)));
        Gravity.sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1F, 0.6F);
        addAffinity(p, (int) stat("afinidad_hablar", 15), "message.draviti.eg.reason.talk");
        PowerManager.sync(p);
    }

    static void sendLine(ServerPlayer p, Component line) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), EtherGearPacket.line(line));
    }
}
