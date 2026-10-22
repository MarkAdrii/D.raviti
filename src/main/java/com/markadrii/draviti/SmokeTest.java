package com.markadrii.draviti;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Solo para la prueba automática de GitHub (-Ddraviti.smoketest=true): carga todas las clases que tocan los mixins
 * del núcleo de gravedad para que, si alguno no encaja, falle ahí y no en la partida. Escribe DRAVITI_SMOKE_OK o
 * DRAVITI_SMOKE_FAIL en el log y cierra el juego.
 */
public final class SmokeTest {
    private static final Logger LOG = LoggerFactory.getLogger("D.raviti smoke test");

    private SmokeTest() {}

    public static boolean enabled() { return Boolean.getBoolean("draviti.smoketest"); }

    public static void init() {
        if (!enabled()) return;
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                run(false);
                e.getServer().halt(false);
            }
        });
    }

    /** Devuelve true si todo ha cargado bien. */
    public static boolean run(boolean client) {
        List<String> failed = new ArrayList<>();
        int loaded = 0;
        try (InputStream in = SmokeTest.class.getResourceAsStream("/draviti.gravity.mixins.json")) {
            JsonObject cfg = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            String pkg = cfg.get("package").getAsString();
            List<String> mixins = new ArrayList<>();
            add(mixins, cfg.getAsJsonArray("mixins"));
            if (client) add(mixins, cfg.getAsJsonArray("client"));
            for (String m : mixins) {
                for (String target : targets(pkg + "." + m)) {
                    try {
                        Class.forName(target, true, Thread.currentThread().getContextClassLoader());
                        loaded++;
                    } catch (Throwable t) {
                        failed.add(m + " -> " + target + ": " + t);
                        LOG.error("Mixin {} en {} ha fallado", m, target, t);
                    }
                }
            }
        } catch (Throwable t) {
            failed.add("config: " + t);
            LOG.error("No se pudo leer la config de mixins", t);
        }
        if (failed.isEmpty()) LOG.info("DRAVITI_SMOKE_OK ({}) {} clases", client ? "cliente" : "servidor", loaded);
        else LOG.error("DRAVITI_SMOKE_FAIL ({}) {}", client ? "cliente" : "servidor", failed);
        return failed.isEmpty();
    }

    private static void add(List<String> out, JsonArray arr) {
        if (arr != null) arr.forEach(e -> out.add(e.getAsString()));
    }

    /** Clases objetivo de un mixin, leídas de su anotación @Mixin sin cargarlo. */
    private static List<String> targets(String mixinClass) throws Exception {
        List<String> out = new ArrayList<>();
        try (InputStream in = SmokeTest.class.getResourceAsStream("/" + mixinClass.replace('.', '/') + ".class")) {
            if (in == null) throw new IllegalStateException("no existe " + mixinClass);
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
            List<AnnotationNode> anns = node.invisibleAnnotations;
            if (anns == null) return out;
            for (AnnotationNode a : anns) {
                if (!a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;") || a.values == null) continue;
                for (int i = 0; i < a.values.size(); i += 2) {
                    String key = (String) a.values.get(i);
                    Object val = a.values.get(i + 1);
                    if (key.equals("value") && val instanceof List<?> l)
                        for (Object o : l) out.add(((Type) o).getClassName());
                    if (key.equals("targets") && val instanceof List<?> l)
                        for (Object o : l) out.add(((String) o).replace('/', '.'));
                }
            }
        }
        return out;
    }
}
