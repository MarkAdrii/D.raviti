package com.markadrii.draviti.gravity;

import com.markadrii.draviti.gravitycore.GravityCore;
import com.markadrii.draviti.gravitycore.api.GravityChangerAPI;
import com.markadrii.draviti.gravitycore.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Acceso a la gravedad de las entidades: con el núcleo copiado de Gravity Control o, si ese mod está instalado,
 * con el suyo (por reflexión, sin depender de él).
 */
public final class GravityBridge {
    private static Method extGet, extSet;
    private static boolean extTried;

    private GravityBridge() {}

    private static boolean external() {
        if (GravityCore.enabled()) return false;
        if (!extTried) {
            extTried = true;
            try {
                Class<?> api = Class.forName("gravitychanger.api.GravityChangerAPI");
                extGet = api.getMethod("getGravityDirection", Entity.class);
                extSet = api.getMethod("setBaseGravityDirection", Entity.class, Direction.class);
            } catch (Throwable t) {
                extGet = extSet = null;
            }
        }
        return extGet != null;
    }

    /** Dirección de la gravedad aplicada (DOWN = normal). */
    public static Direction get(Entity e) {
        try {
            if (GravityCore.enabled()) return GravityChangerAPI.getGravityDirection(e);
            if (external()) return (Direction) extGet.invoke(null, e);
        } catch (Throwable ignored) {}
        return Direction.DOWN;
    }

    /** Cambia la gravedad base (DOWN = normal). Solo en el servidor; se sincroniza sola. */
    public static void set(Entity e, Direction d) {
        try {
            if (GravityCore.enabled()) GravityChangerAPI.setBaseGravityDirection(e, d);
            else if (external()) extSet.invoke(null, e, d);
        } catch (Throwable ignored) {}
    }

    /** Velocidad en coordenadas del mundo (con la gravedad girada, la de Minecraft va en las del jugador). */
    public static Vec3 worldVelocity(Entity e) {
        Direction d = get(e);
        return d == Direction.DOWN ? e.getDeltaMovement() : RotationUtil.vecPlayerToWorld(e.getDeltaMovement(), d);
    }

    public static void setWorldVelocity(Entity e, Vec3 v) {
        Direction d = get(e);
        e.setDeltaMovement(d == Direction.DOWN ? v : RotationUtil.vecWorldToPlayer(v, d));
    }
}
