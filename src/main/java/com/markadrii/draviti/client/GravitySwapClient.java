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
import org.joml.Vector3f;

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
    /** Orientación del "suelo" actual (suavizada) y la mirada relativa a ese suelo (grados, como yaw/pitch). */
    private static final Quaternionf frame = new Quaternionf();
    private static Direction frameDir;
    private static boolean steering;
    private static float relYaw, relPitch;
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

    // ------------------------------------------------------------------ cámara y ratón

    /** Rotación que lleva el "abajo" normal (0,-1,0) a la gravedad de d (identidad si es normal). */
    private static Quaternionf target(Direction d) {
        if (d == null) return new Quaternionf();
        Vec3 g = Vec3.atLowerCornerOf(d.getNormal());
        return new Quaternionf().rotationTo(0, -1, 0, (float) g.x, (float) g.y, (float) g.z);
    }

    /** Dirección de mirada estilo Minecraft (yaw/pitch en grados). */
    private static Vector3f look(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f((float) (-Math.sin(y) * Math.cos(p)), (float) -Math.sin(p), (float) (Math.cos(y) * Math.cos(p)));
    }

    private static float yawOf(Vector3f f) { return (float) Math.toDegrees(Math.atan2(-f.x, f.z)); }
    private static float pitchOf(Vector3f f) { return (float) Math.toDegrees(Math.asin(Mth.clamp(-f.y, -1F, 1F))); }

    /**
     * Cada frame: el marco (orientación del suelo) gira suave hacia el de la gravedad actual. La mirada en el mundo
     * no salta: se recalcula la mirada relativa al marco nuevo. Al volver a la gravedad normal se suelta el control.
     */
    private static void updateFrame(LocalPlayer p) {
        Direction d = dir(p);
        if (!steering) {
            if (d == null) return;
            frame.identity();
            steering = true;
            relYaw = p.getYRot();
            relPitch = p.getXRot();
        }
        frameDir = d;
        Quaternionf t = target(d);
        Vector3f world = new Quaternionf(frame).transform(look(relYaw, relPitch));
        frame.slerp(t, 0.12F);
        Vector3f local = new Quaternionf(frame).conjugate().transform(world);
        relYaw = yawOf(local);
        relPitch = Mth.clamp(pitchOf(local), -90F, 90F);
        applyLook(p);
        if (d == null && frame.equals(t, 0.002F)) { frame.identity(); steering = false; frameDir = null; }
    }

    /** La mirada del jugador en el mundo (para apuntar y moverse) sale de la mirada relativa. */
    private static void applyLook(LocalPlayer p) {
        Vector3f f = new Quaternionf(frame).transform(look(relYaw, relPitch));
        float yaw = yawOf(f), pitch = pitchOf(f);
        // yaw continuo (sin saltos de 360) para que el cuerpo no gire de golpe
        float prev = p.getYRot();
        yaw = prev + Mth.wrapDegrees(yaw - prev);
        p.setYRot(yaw); p.setXRot(pitch);
        p.yRotO = yaw; p.xRotO = pitch;
    }

    /**
     * Lo llama el mixin de Entity.turn (movimiento del ratón). Con la gravedad girada el ratón mueve la mirada
     * relativa al suelo nuevo: izquierda/derecha giran alrededor de tu "arriba" y arriba/abajo inclinan la cabeza.
     */
    public static boolean handleTurn(Entity e, double dx, double dy) {
        Minecraft mc = Minecraft.getInstance();
        if (e != mc.player || !steering) return false;
        relYaw += (float) dx * 0.15F;
        relPitch = Mth.clamp(relPitch + (float) dy * 0.15F, -90F, 90F);
        applyLook(mc.player);
        return true;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        updateFrame(mc.player);
        if (!steering || event.getCamera().getEntity() != mc.player) return;
        Vector3f f = new Quaternionf(frame).transform(look(relYaw, relPitch));
        Vector3f u = new Quaternionf(frame).transform(look(relYaw, relPitch - 90F));
        float yaw = yawOf(f), pitch = pitchOf(f);
        // Ángulo de giro para que el "arriba" de la cámara sea el del suelo nuevo
        double yr = Math.toRadians(yaw);
        Vector3f r = new Vector3f((float) -Math.cos(yr), 0, (float) -Math.sin(yr));
        Vector3f u0 = new Vector3f(r).cross(look(yaw, pitch));
        float roll = (float) Math.toDegrees(Math.atan2(u.dot(r), u.dot(u0)));
        float shake = event.getRoll();
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(roll + shake);
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
