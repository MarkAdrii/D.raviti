package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.gravity.Gravity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

/**
 * Gravity Swap en el cliente:
 * <ul>
 *   <li>Física del jugador: cae hacia la superficie elegida y anda por ella con WASD (salto = separarse).</li>
 *   <li>Cámara: gira para que esa superficie quede "abajo" en la pantalla.</li>
 *   <li>Modelo: todos ven al jugador de pie sobre la pared o el techo.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, value = Dist.CLIENT)
public final class GravitySwapClient {
    private static final String KEY = Gravity.POWER_ID + "/swap_";
    private static boolean wasSwapped;
    private static float roll, lastTarget;
    /** Desplazamiento de la cámara (suavizado entre frames). */
    private static Vec3 eye = Vec3.ZERO;

    private GravitySwapClient() {}

    /** Dirección de la gravedad de un jugador (null = normal). */
    public static Direction dir(Entity e) {
        String s = ClientVisuals.suffix(e, KEY);
        if (s == null) return null;
        Direction d = Direction.byName(s);
        return d == Direction.DOWN ? null : d;
    }

    // ------------------------------------------------------------------ física

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused()) return;
        Direction d = dir(p);
        if (d == null || p.isSpectator() || Gravity.active(p, "ingravidez")) {
            if (wasSwapped) { p.setNoGravity(false); wasSwapped = false; }
            return;
        }
        wasSwapped = true;
        p.setNoGravity(true);
        if (p.getAbilities().flying && !p.isCreative()) p.getAbilities().flying = false;

        Vec3 g = Vec3.atLowerCornerOf(d.getNormal());
        Vec3 v = p.getDeltaMovement();
        double vg = v.dot(g);
        Vec3 vp = v.subtract(g.scale(vg));
        boolean grounded = !p.level().noCollision(p, p.getBoundingBox().move(g.scale(0.06)));

        // Adelante = donde miras, aplanado sobre la superficie; izquierda = adelante x gravedad
        Vec3 look = p.getLookAngle();
        Vec3 fwd = look.subtract(g.scale(look.dot(g)));
        if (fwd.lengthSqr() < 1.0E-3) {
            Vec3 up = Vec3.directionFromRotation(p.getXRot() - 90, p.getYRot());
            fwd = up.subtract(g.scale(up.dot(g)));
        }
        fwd = fwd.normalize();
        Vec3 left = fwd.cross(g).normalize();

        float fi = p.input.forwardImpulse, li = p.input.leftImpulse;
        double accel = grounded ? (p.isSprinting() ? 0.15 : 0.11) : 0.03;
        vp = vp.scale(grounded ? 0.5 : 0.96).add(fwd.scale(fi * accel)).add(left.scale(li * accel));
        if (grounded) vg = p.input.jumping ? -0.45 : 0.02;
        else vg = Math.min(vg + 0.08, 3.0);
        p.setDeltaMovement(vp.add(g.scale(vg)));
        p.fallDistance = 0;
    }

    // ------------------------------------------------------------------ cámara

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Direction d = mc.player == null ? null : dir(mc.player);
        float target = 0;
        if (d != null && event.getCamera().getEntity() == mc.player) {
            Vec3 g = Vec3.atLowerCornerOf(d.getNormal());
            double yaw = Math.toRadians(event.getYaw()), pitch = Math.toRadians(event.getPitch());
            Vec3 f = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            Vec3 r = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            Vec3 u = r.cross(f);
            double gx = g.dot(r), gy = g.dot(u);
            // La superficie a la que caes queda abajo en la pantalla
            target = Math.abs(gx) + Math.abs(gy) < 0.15 ? lastTarget : (float) -Math.toDegrees(Math.atan2(gx, -gy));
            lastTarget = target;
        }
        roll += Mth.wrapDegrees(target - roll) * 0.15F;
        if (Math.abs(roll) > 0.01F) event.setRoll(event.getRoll() + roll);
    }

    /**
     * Cuánto hay que mover la cámara para que los ojos queden donde toca con la gravedad girada (lo usa CameraMixin).
     * Ojos normales: pies + 1.62 hacia arriba. Girados: centro del cuerpo dibujado + 0.72 en contra de la gravedad.
     */
    public static Vec3 eyeOffset(Entity entity, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player) return null;
        Direction d = dir(entity);
        Vec3 target = Vec3.ZERO;
        if (d != null) {
            Vec3 g = Vec3.atLowerCornerOf(d.getNormal());
            float half = entity.getBbHeight() / 2F, eyeH = entity.getEyeHeight();
            double along = d.getAxis() == Direction.Axis.Y ? half : entity.getBbWidth() / 2F;
            Vec3 swapped = new Vec3(0, half, 0).subtract(g.scale(eyeH - along));
            target = swapped.subtract(0, eyeH, 0);
        }
        eye = eye.add(target.subtract(eye).scale(0.18));
        if (eye.lengthSqr() < 1.0E-5 && target == Vec3.ZERO) { eye = Vec3.ZERO; return null; }
        return eye;
    }

    // ------------------------------------------------------------------ modelo

    @SubscribeEvent
    public static void renderPre(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        Direction d = dir(p);
        if (d == null) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        Vec3 g = Vec3.atLowerCornerOf(d.getNormal());
        float half = p.getBbHeight() / 2F;
        // Los pies tocan la superficie: se gira el cuerpo sobre su centro y se acerca a la pared
        double along = d.getAxis() == Direction.Axis.Y ? half : p.getBbWidth() / 2F;
        Vec3 off = g.scale(-(half - along));
        pose.translate(off.x, off.y + half, off.z);
        pose.mulPose(new Quaternionf().rotationTo(0, -1, 0, (float) g.x, (float) g.y, (float) g.z));
        pose.translate(0, -half, 0);
    }

    @SubscribeEvent
    public static void renderPost(RenderPlayerEvent.Post event) {
        if (dir(event.getEntity()) != null) event.getPoseStack().popPose();
    }
}
