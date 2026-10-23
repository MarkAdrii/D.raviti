package com.markadrii.draviti.gravity;

import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.data.PowerData;
import com.markadrii.draviti.data.PowerManager;
import com.markadrii.draviti.network.MovePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Movimientos de Ingravidez que pide el cliente (él mueve su cuerpo; aquí se gasta el éter y se ven los efectos):
 * vuelo sónico (W dos veces en el aire), dash (A, S o D dos veces) y cambiar si las técnicas rompen bloques
 * (Shift tres veces).
 */
public final class GravityMovement {
    private GravityMovement() {}

    static Ability floatAbility() {
        return Gravity.power() == null ? null : Gravity.power().ability("ingravidez").orElse(null);
    }

    public static void handle(ServerPlayer p, MovePacket m) {
        if (!Gravity.has(p)) return;
        Ability a = floatAbility();
        switch (m.action()) {
            case MovePacket.TOGGLE_BLOCKS -> {
                PowerData d = Gravity.data(p);
                d.breakBlocks = !d.breakBlocks;
                p.displayClientMessage(Component.translatable(d.breakBlocks ? "message.draviti.blocks_on" : "message.draviti.blocks_off"), true);
                Gravity.sound(p, p.position(), SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, d.breakBlocks ? 1.2F : 0.8F);
            }
            case MovePacket.TOGGLE_MARK -> {
                PowerData d = Gravity.data(p);
                d.showMark = !d.showMark;
                Gravity.fx(p, Fx.GLOW, p.position(), Limb.RIGHT_ARM.ordinal(), 10);
                Gravity.sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1F, d.showMark ? 1.4F : 0.8F);
                PowerManager.sync(p);
            }
            case MovePacket.EG_TALK -> com.markadrii.draviti.ethergear.EtherGear.talk(p);
            case MovePacket.EG_ENTER -> com.markadrii.draviti.ethergear.EtherGearInterior.enter(p);
            case MovePacket.EG_SUMMON -> com.markadrii.draviti.ethergear.EtherGearCompanion.toggle(p);
            case MovePacket.EG_SHOULDER_DOWN -> com.markadrii.draviti.ethergear.EtherGearCompanion.down(p, null);
            case MovePacket.EG_BUY -> com.markadrii.draviti.ethergear.EtherGear.buy(p, (int) m.x());
            case MovePacket.EG_DIALOG -> com.markadrii.draviti.ethergear.EtherGearInterior.choose(p, (int) m.x());
            case MovePacket.TOGGLE_FLOAT -> PowerManager.onKey(p, Gravity.power().id(), "ingravidez", true);
            case MovePacket.SONIC_START -> {
                if (a == null || !Gravity.active(p, "ingravidez") || GravityAbilities.SONIC.contains(p.getUUID())) return;
                if (Gravity.ether(p) < a.stat("sonic_ether_per_second") / 2) return;
                GravityAbilities.SONIC.add(p.getUUID());
                Vec3 look = p.getLookAngle();
                Gravity.anim(p, "sonic_fly");
                Gravity.fx(p, Fx.SONIC_BOOM, p.position().add(0, 0.9, 0).add(look.scale(-0.5)), look, 1, 0);
                Gravity.shake(p, p.position(), 0.8F, 10);
                Gravity.sound(p, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.4F);
                Gravity.sound(p, p.position(), SoundEvents.ELYTRA_FLYING, 0.6F, 1.6F);
                PowerManager.sync(p);
            }
            case MovePacket.SONIC_STOP -> stopSonic(p);
            case MovePacket.DASH -> {
                if (a == null || !Gravity.active(p, "ingravidez")) return;
                if (!Gravity.spend(p, a.stat("dash_ether_cost"))) return;
                Vec3 dir = new Vec3(m.x(), 0, m.z());
                Gravity.fx(p, Fx.DASH, p.position(), dir, 1, 0);
                Gravity.sound(p, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.6F);
                Gravity.sound(p, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.6F, 1.8F);
            }
            default -> {}
        }
    }

    public static void stopSonic(ServerPlayer p) {
        if (!GravityAbilities.SONIC.remove(p.getUUID())) return;
        Gravity.anim(p, "stop");
        PowerManager.sync(p);
    }

    /** Cada tick mientras va en vuelo sónico: gasta éter y arrolla lo que pilla por delante. */
    public static void tick(ServerPlayer p) {
        if (!GravityAbilities.SONIC.contains(p.getUUID())) return;
        Ability a = floatAbility();
        if (a == null || !Gravity.active(p, "ingravidez") || p.onGround()) { stopSonic(p); return; }
        p.fallDistance = 0;
        if (p.tickCount % 5 == 0) {
            Gravity.setEther(p, Gravity.ether(p) - (int) Math.ceil(a.stat("sonic_ether_per_second") / 4));
            if (Gravity.ether(p) <= 0) { stopSonic(p); return; }
        }
        if (p.tickCount % 2 == 0) {
            Vec3 v = p.getDeltaMovement();
            Vec3 dir = v.lengthSqr() > 1.0E-4 ? v.normalize() : p.getLookAngle();
            for (LivingEntity e : Gravity.around(p, p.position().add(0, 0.9, 0).add(dir), 1.8)) {
                Gravity.hurt(p, e, a.statF("sonic_damage"));
                Gravity.setMotion(e, dir.scale(1.6 * Gravity.resist(e)).add(0, 0.4, 0));
                Gravity.fx(p, Fx.BARRAGE_HIT, e.position().add(0, e.getBbHeight() / 2, 0), dir, 1, 0);
            }
        }
    }
}
