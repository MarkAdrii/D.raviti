package com.markadrii.draviti.client.fx;

import com.markadrii.draviti.Draviti;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Efectos de Photon (assets/draviti/fx/*.fx) sin depender de Photon: todo por reflexión y solo si está instalado.
 * Si no está (o algo de su API cambia) {@link #available()} es false y GravityFx usa los efectos de siempre.
 */
public final class PhotonBridge {
    private static Boolean ok;
    private static Method getFX, start, setOffset, setRotation, setScale, setAllowMulti, setForcedDeath, getRuntime;
    private static Method destroy, isAlive;
    private static Constructor<?> entityCtor, blockCtor;
    private static Object autoNone, autoForward;

    /** Efectos que hay que quitar a mano: auras (hasta que se paran) y efectos con duración. */
    private record Running(Object effect, Entity entity, String name, int until) {}
    private static final List<Running> RUNNING = new ArrayList<>();
    /** Auras activas: entidad -> nombre -> efecto. */
    private static final Map<Integer, Map<String, Running>> AURAS = new HashMap<>();
    private static int ticks;

    private PhotonBridge() {}

    public static boolean available() {
        if (ok == null) ok = init();
        return ok;
    }

    private static boolean init() {
        if (!ModList.get().isLoaded("photon")) return false;
        try {
            ClassLoader cl = PhotonBridge.class.getClassLoader();
            Class<?> helper = Class.forName("com.lowdragmc.photon.client.fx.FXHelper", true, cl);
            Class<?> fx = Class.forName("com.lowdragmc.photon.client.fx.FX", true, cl);
            Class<?> fxEffect = Class.forName("com.lowdragmc.photon.client.fx.FXEffect", true, cl);
            Class<?> entityEffect = Class.forName("com.lowdragmc.photon.client.fx.EntityEffect", true, cl);
            Class<?> blockEffect = Class.forName("com.lowdragmc.photon.client.fx.BlockEffect", true, cl);
            Class<?> runtime = Class.forName("com.lowdragmc.photon.client.fx.FXRuntime", true, cl);
            getFX = helper.getMethod("getFX", ResourceLocation.class);
            try {
                Class<?> auto = Class.forName("com.lowdragmc.photon.client.fx.EntityEffect$AutoRotate", true, cl);
                entityCtor = entityEffect.getConstructor(fx, Level.class, Entity.class, auto);
                autoNone = auto.getField("NONE").get(null);
                autoForward = auto.getField("FORWARD").get(null);
            } catch (ReflectiveOperationException old) {
                entityCtor = entityEffect.getConstructor(fx, Level.class, Entity.class); // versiones antiguas
            }
            blockCtor = blockEffect.getConstructor(fx, Level.class, BlockPos.class);
            start = fxEffect.getMethod("start");
            setOffset = fxEffect.getMethod("setOffset", Vector3f.class);
            setRotation = fxEffect.getMethod("setRotation", Quaternionf.class);
            setScale = fxEffect.getMethod("setScale", Vector3f.class);
            setAllowMulti = fxEffect.getMethod("setAllowMulti", boolean.class);
            setForcedDeath = fxEffect.getMethod("setForcedDeath", boolean.class);
            getRuntime = fxEffect.getMethod("getRuntime");
            destroy = runtime.getMethod("destroy", boolean.class);
            isAlive = runtime.getMethod("isAlive");
            Draviti.LOGGER.info("Photon detectado: D.raviti usa sus efectos.");
            return true;
        } catch (Throwable t) {
            Draviti.LOGGER.warn("Photon está instalado pero su API no es la esperada; D.raviti usa sus efectos normales.", t);
            return false;
        }
    }

    private static Object fx(String name) throws ReflectiveOperationException {
        return getFX.invoke(null, Draviti.id(name));
    }

    private static void fail(Throwable t) {
        Draviti.LOGGER.warn("Fallo al lanzar un efecto de Photon; se desactivan.", t);
        ok = false;
    }

    /** Efecto en un punto del mundo (escala uniforme). */
    public static void at(String name, Vec3 pos, float scale) { at(name, pos, null, scale); }

    /** Efecto en un punto, girado para que su +Y apunte en dir (ondas, conos). */
    public static void at(String name, Vec3 pos, Vec3 dir, float scale) {
        if (!available()) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        try {
            Object f = fx(name);
            if (f == null) return;
            // BlockEffect se borra si cambia su bloque: se ancla a un bloque de aire si lo hay
            BlockPos bp = BlockPos.containing(pos);
            if (!level.getBlockState(bp).isAir() && level.getBlockState(bp.above()).isAir()) bp = bp.above();
            Object e = blockCtor.newInstance(f, level, bp);
            setOffset.invoke(e, new Vector3f((float) (pos.x - bp.getX() - 0.5), (float) (pos.y - bp.getY() - 0.5), (float) (pos.z - bp.getZ() - 0.5)));
            if (dir != null && dir.lengthSqr() > 1.0E-6) {
                Vec3 d = dir.normalize();
                setRotation.invoke(e, new Quaternionf().rotationTo(0, 1, 0, (float) d.x, (float) d.y, (float) d.z));
            }
            setScale.invoke(e, new Vector3f(scale, scale, scale));
            setAllowMulti.invoke(e, true);
            start.invoke(e);
        } catch (Throwable t) {
            fail(t);
        }
    }

    /** Efecto pegado a una entidad. ticks > 0: se quita pasado ese tiempo (para los que van en bucle). */
    public static void on(Entity entity, String name, float scale, boolean followBody, int ticks) {
        Object e = create(entity, name, scale, followBody, true);
        if (e != null && ticks > 0) RUNNING.add(new Running(e, entity, name, PhotonBridge.ticks + ticks));
    }

    private static Object create(Entity entity, String name, float scale, boolean followBody, boolean multi) {
        if (!available() || entity == null) return null;
        try {
            Object f = fx(name);
            if (f == null) return null;
            Object e = entityCtor.getParameterCount() == 4
                    ? entityCtor.newInstance(f, entity.level(), entity, followBody ? autoForward : autoNone)
                    : entityCtor.newInstance(f, entity.level(), entity);
            setScale.invoke(e, new Vector3f(scale, scale, scale));
            setAllowMulti.invoke(e, multi);
            start.invoke(e);
            return e;
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    /** Aura en bucle: se enciende/apaga según on (no se repite si ya está). */
    public static void aura(Entity entity, String name, boolean on, float scale, boolean followBody) {
        if (!available()) return;
        Map<String, Running> m = AURAS.computeIfAbsent(entity.getId(), k -> new HashMap<>());
        Running r = m.get(name);
        if (on && (r == null || !alive(r.effect))) {
            Object e = create(entity, name, scale, followBody, true);
            if (e != null) m.put(name, new Running(e, entity, name, Integer.MAX_VALUE));
        } else if (!on && r != null) {
            kill(r.effect, false);
            m.remove(name);
        }
    }

    private static boolean alive(Object effect) {
        try {
            Object rt = getRuntime.invoke(effect);
            return rt != null && (boolean) isAlive.invoke(rt);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void kill(Object effect, boolean force) {
        try {
            Object rt = getRuntime.invoke(effect);
            if (rt != null) destroy.invoke(rt, force);
        } catch (Throwable ignored) {
        }
    }

    /** Cada tick de cliente: quita los efectos con duración que han terminado. */
    public static void tick() {
        ticks++;
        Iterator<Running> it = RUNNING.iterator();
        while (it.hasNext()) {
            Running r = it.next();
            if (r.until <= ticks || !r.entity.isAlive()) { kill(r.effect, false); it.remove(); }
        }
        if (ticks % 100 == 0) {
            AURAS.values().forEach(m -> m.values().removeIf(r -> !r.entity.isAlive()));
            AURAS.values().removeIf(Map::isEmpty);
        }
    }

    public static void clear() {
        RUNNING.clear();
        AURAS.clear();
    }
}
