package com.markadrii.draviti.gravity;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Acciones que duran varios ticks (cargas, caídas, atracciones...). Se ejecutan al final de cada tick del servidor. */
public final class Tasks {
    /** Devuelve true cuando ha terminado. t empieza en 0. */
    @FunctionalInterface
    public interface Step { boolean run(int t); }

    private record Task(ServerPlayer player, Step step, int[] t) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> ADDED = new ArrayList<>();

    private Tasks() {}

    public static void start(ServerPlayer player, Step step) {
        ADDED.add(new Task(player, step, new int[]{0}));
    }

    /** Hace algo dentro de delay ticks. */
    public static void later(ServerPlayer player, int delay, Runnable r) {
        start(player, t -> {
            if (t < delay) return false;
            r.run();
            return true;
        });
    }

    public static void tick() {
        TASKS.addAll(ADDED);
        ADDED.clear();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.player.isRemoved() || !task.player.isAlive()) { it.remove(); continue; }
            boolean done;
            try {
                done = task.step.run(task.t[0]++);
            } catch (Exception e) {
                com.markadrii.draviti.Draviti.LOGGER.error("Error en una técnica de gravedad", e);
                done = true;
            }
            if (done) it.remove();
        }
    }

    public static void clear() {
        TASKS.clear();
        ADDED.clear();
    }
}
