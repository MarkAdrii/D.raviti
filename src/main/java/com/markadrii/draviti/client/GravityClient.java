package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.fx.GravityFx;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.gravity.Limb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cliente, cada tick: el vuelo de Ingravidez (el jugador mueve su propio cuerpo, así que se hace aquí),
 * los efectos que duran y el aura de cada jugador.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class GravityClient {
    private GravityClient() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { GravityFx.clear(); return; }
        if (mc.isPaused()) return;

        floatPhysics(mc, mc.player);
        GravityFx.tick(mc.level);

        for (Player p : mc.level.players()) {
            if (p.distanceToSqr(mc.player) > 64 * 64 || p.isInvisible()) continue;
            boolean od = Gravity.overdrive(p), full = Gravity.fullOverdrive(p);
            boolean anti = Gravity.active(p, "antigravedad"), floating = Gravity.active(p, "ingravidez");
            float gr = ClientVisuals.glow(p, Limb.RIGHT_ARM), gl = ClientVisuals.glow(p, Limb.LEFT_ARM), lg = ClientVisuals.glow(p, Limb.RIGHT_LEG);
            if (od || full || anti || floating || gr > 0 || gl > 0 || lg > 0)
                GravityFx.ambient(mc.level, p, od, full, anti, floating, gr, gl, lg);
        }
    }

    /**
     * Ingravidez: sin peso. Salto sube, agacharse baja; si no, se queda flotando.
     * Contra una pared se puede andar por ella hacia arriba o abajo (la gravedad apunta a la pared).
     */
    private static void floatPhysics(Minecraft mc, LocalPlayer p) {
        if (!Gravity.active(p, "ingravidez") || p.isSpectator()) return;
        if (p.getAbilities().flying && !p.isCreative()) p.getAbilities().flying = false;
        if (p.getAbilities().flying) return;
        boolean up = mc.options.keyJump.isDown(), down = mc.options.keyShift.isDown();
        Vec3 v = p.getDeltaMovement();

        // Control en el aire como si se nadara en el vacío
        float fwd = p.input.forwardImpulse, side = p.input.leftImpulse;
        double yaw = Math.toRadians(p.getYRot());
        double ax = (-Math.sin(yaw) * fwd + Math.cos(yaw) * side) * 0.055;
        double az = (Math.cos(yaw) * fwd + Math.sin(yaw) * side) * 0.055;
        double vx = (v.x + ax) * 0.93, vz = (v.z + az) * 0.93;
        double max = 0.55;
        double h = Math.sqrt(vx * vx + vz * vz);
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
