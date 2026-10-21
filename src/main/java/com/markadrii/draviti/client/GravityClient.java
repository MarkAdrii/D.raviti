package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.client.fx.GravityFx;
import com.markadrii.draviti.client.fx.PhotonBridge;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.gravity.Limb;
import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.network.MovePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cliente, cada tick:
 * <ul>
 *   <li>Ingravidez: flotar y andar por las paredes; W dos veces en el aire = vuelo sónico (mientras mantienes W);
 *       A, S o D dos veces = dash hacia ese lado.</li>
 *   <li>Shift tres veces seguidas: las técnicas rompen o no rompen bloques.</li>
 *   <li>Efectos que duran y auras de cada jugador (con Photon si está instalado).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class GravityClient {
    private static final int DOUBLE_TAP = 7, TRIPLE_SHIFT = 18;

    private static int ticks;
    private static final int[] lastTap = {-100, -100, -100, -100}; // W, A, S, D
    private static final boolean[] wasDown = new boolean[4];
    private static final int[] shiftTaps = {-100, -100, -100};
    private static boolean shiftWasDown;
    private static int lastSwap = -100;
    private static boolean swapWasDown;
    private static int lastCtrl = -100, lastJump = -100;
    private static boolean ctrlWasDown, jumpWasDown;
    private static boolean sonic;
    private static int sonicTicks, dashCooldown;

    private GravityClient() {}

    public static boolean sonicLocal() { return sonic; }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            GravityFx.clear();
            com.markadrii.draviti.client.render.FieldDomes.clear();
            sonic = false;
            return;
        }
        if (mc.isPaused()) return;
        ticks++;

        LocalPlayer me = mc.player;
        if (Gravity.has(me) && mc.screen == null) {
            tripleShift(mc);
            doubleSwap(mc);
            ctrlJump(mc);
            movementTaps(mc, me);
        } else if (sonic) {
            stopSonic();
        }
        floatPhysics(mc, me);
        GravityFx.tick(mc.level);
        if (PhotonBridge.available()) PhotonBridge.tick();

        for (Player p : mc.level.players()) {
            if (p.distanceToSqr(me) > 64 * 64 || p.isInvisible()) continue;
            boolean od = Gravity.overdrive(p), full = Gravity.fullOverdrive(p);
            boolean anti = Gravity.active(p, "antigravedad"), floating = Gravity.active(p, "ingravidez");
            boolean sonicFly = p == me ? sonic : Gravity.active(p, "vuelo_sonico");
            if (PhotonBridge.available()) {
                String v = full ? "_p" : "";
                PhotonBridge.aura(p, "aura_overdrive", od && !full, 1, false);
                PhotonBridge.aura(p, "aura_full_p", full, 1, false);
                PhotonBridge.aura(p, "aura_anti" + v, anti, 1, false);
                PhotonBridge.aura(p, "aura_anti" + (full ? "" : "_p"), false, 1, false);
                PhotonBridge.aura(p, "aura_float", floating && !sonicFly && !full, 1, false);
                PhotonBridge.aura(p, "aura_float_p", floating && !sonicFly && full, 1, false);
                PhotonBridge.aura(p, "aura_sonic", sonicFly && !full, 1, false);
                PhotonBridge.aura(p, "aura_sonic_p", sonicFly && full, 1, false);
                continue;
            }
            float gr = ClientVisuals.glow(p, Limb.RIGHT_ARM), gl = ClientVisuals.glow(p, Limb.LEFT_ARM), lg = ClientVisuals.glow(p, Limb.RIGHT_LEG);
            if (od || full || anti || floating || sonicFly || gr > 0 || gl > 0 || lg > 0)
                GravityFx.ambient(mc.level, p, od, full, anti, floating, sonicFly, gr, gl, lg);
        }
    }

    // ------------------------------------------------------------------ teclas

    /** Shift tres veces seguidas: romper bloques sí/no (lo guarda el servidor). */
    private static void tripleShift(Minecraft mc) {
        boolean down = mc.options.keyShift.isDown();
        if (down && !shiftWasDown) {
            shiftTaps[0] = shiftTaps[1];
            shiftTaps[1] = shiftTaps[2];
            shiftTaps[2] = ticks;
            if (shiftTaps[2] - shiftTaps[0] <= TRIPLE_SHIFT) {
                ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.TOGGLE_BLOCKS, 0, 0));
                shiftTaps[0] = shiftTaps[1] = shiftTaps[2] = -100;
            }
        }
        shiftWasDown = down;
    }

    /** F dos veces (la tecla de cambiar de mano): oculta o enseña la marca del Ether Gear. */
    private static void doubleSwap(Minecraft mc) {
        boolean down = mc.options.keySwapOffhand.isDown();
        if (down && !swapWasDown) {
            if (ticks - lastSwap <= DOUBLE_TAP + 2) {
                ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.TOGGLE_MARK, 0, 0));
                lastSwap = -100;
            } else {
                lastSwap = ticks;
            }
        }
        swapWasDown = down;
    }

    /**
     * Ctrl + salto a la vez: Ingravidez sí/no. Las dos teclas tienen que pulsarse casi juntas (3 ticks), así correr
     * con Ctrl mantenido y saltar no la activa.
     */
    private static void ctrlJump(Minecraft mc) {
        boolean ctrl = mc.options.keySprint.isDown(), jump = mc.options.keyJump.isDown();
        if (ctrl && !ctrlWasDown) lastCtrl = ticks;
        if (jump && !jumpWasDown) lastJump = ticks;
        boolean fresh = (ctrl && !ctrlWasDown) || (jump && !jumpWasDown);
        if (fresh && ctrl && jump && Math.abs(lastCtrl - lastJump) <= 3) {
            ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.TOGGLE_FLOAT, 0, 0));
            lastCtrl = lastJump = -100;
        }
        ctrlWasDown = ctrl;
        jumpWasDown = jump;
    }

    private static void movementTaps(Minecraft mc, LocalPlayer p) {
        KeyMapping[] keys = {mc.options.keyUp, mc.options.keyLeft, mc.options.keyDown, mc.options.keyRight};
        boolean floating = Gravity.active(p, "ingravidez");
        if (dashCooldown > 0) dashCooldown--;
        for (int i = 0; i < 4; i++) {
            boolean down = keys[i].isDown();
            if (down && !wasDown[i]) {
                boolean doubleTap = ticks - lastTap[i] <= DOUBLE_TAP;
                lastTap[i] = doubleTap ? -100 : ticks;
                if (doubleTap && floating) {
                    if (i == 0) {
                        if (!p.onGround() && !sonic && Gravity.ether(p) > 0) startSonic(p);
                    } else if (dashCooldown <= 0) {
                        dash(p, i);
                    }
                }
            }
            wasDown[i] = down;
        }
    }

    private static Ability floatAbility() {
        return Gravity.power() == null ? null : Gravity.power().ability("ingravidez").orElse(null);
    }

    private static void startSonic(LocalPlayer p) {
        sonic = true;
        sonicTicks = 0;
        ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.SONIC_START, 0, 0));
    }

    private static void stopSonic() {
        if (!sonic) return;
        sonic = false;
        ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.SONIC_STOP, 0, 0));
    }

    /** Dash: A izquierda, S atrás, D derecha (respecto a donde miras). */
    private static void dash(LocalPlayer p, int key) {
        Ability a = floatAbility();
        double speed = a == null ? 1.7 : a.stat("dash_speed");
        double yaw = Math.toRadians(p.getYRot());
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 dir = key == 1 ? left : key == 2 ? fwd.scale(-1) : left.scale(-1);
        Vec3 v = dir.scale(speed);
        p.setDeltaMovement(v.x, Math.max(p.getDeltaMovement().y, 0.05), v.z);
        dashCooldown = 10;
        ModNetwork.CHANNEL.sendToServer(new MovePacket(MovePacket.DASH, (float) dir.x, (float) dir.z));
    }

    // ------------------------------------------------------------------ física

    /**
     * Ingravidez: sin peso. Salto sube, agacharse baja; si no, se queda flotando.
     * Contra una pared se puede andar por ella hacia arriba o abajo (la gravedad apunta a la pared).
     * Vuelo sónico: vuela muy rápido hacia donde mira mientras se mantiene W.
     */
    private static void floatPhysics(Minecraft mc, LocalPlayer p) {
        if (!Gravity.active(p, "ingravidez") || p.isSpectator()) {
            if (sonic) stopSonic();
            return;
        }
        if (p.getAbilities().flying && !p.isCreative()) p.getAbilities().flying = false;
        if (p.getAbilities().flying) return;

        if (sonic) {
            if (!mc.options.keyUp.isDown() || p.onGround() || Gravity.ether(p) <= 0 || mc.screen != null) {
                stopSonic();
            } else {
                Ability a = floatAbility();
                double max = a == null ? 2.2 : a.stat("sonic_speed");
                double speed = Math.min(max, 0.9 + sonicTicks * 0.15);
                sonicTicks++;
                Vec3 look = p.getLookAngle();
                Vec3 v = p.getDeltaMovement().scale(0.3).add(look.scale(speed * 0.7));
                if (v.length() > speed) v = v.normalize().scale(speed);
                p.setDeltaMovement(v);
                p.fallDistance = 0;
                return;
            }
        }

        boolean up = mc.options.keyJump.isDown(), down = mc.options.keyShift.isDown();
        Vec3 v = p.getDeltaMovement();

        // Control en el aire como si se nadara en el vacío
        float fwd = p.input.forwardImpulse, side = p.input.leftImpulse;
        double yaw = Math.toRadians(p.getYRot());
        double ax = (-Math.sin(yaw) * fwd + Math.cos(yaw) * side) * 0.055;
        double az = (Math.cos(yaw) * fwd + Math.sin(yaw) * side) * 0.055;
        double vx = (v.x + ax) * 0.93, vz = (v.z + az) * 0.93;
        double h = Math.sqrt(vx * vx + vz * vz);
        // Tras un dash se deja la velocidad alta un momento
        double max = dashCooldown > 4 ? 2.0 : 0.55;
        if (h > max) { vx *= max / h; vz *= max / h; }

        double vy;
        if (p.horizontalCollision && !p.onGround()) {
            // Andando por la pared: hacia delante sube, agacharse baja
            vy = up || fwd > 0 ? 0.28 : down ? -0.28 : 0.0;
        } else {
            vy = up ? 0.3 : down ? -0.3 : v.y * 0.6;
            if (!up && !down && Math.abs(vy) < 0.01) vy = Math.sin(p.tickCount * 0.12) * 0.012;
        }
        p.setDeltaMovement(vx, vy, vz);
        p.fallDistance = 0;
    }
}
