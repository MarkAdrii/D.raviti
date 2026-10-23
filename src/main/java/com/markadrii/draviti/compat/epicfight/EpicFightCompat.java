package com.markadrii.draviti.compat.epicfight;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.anim.GravityAnimations;
import com.markadrii.draviti.client.ethergear.EtherGearShoulderLayer;
import com.markadrii.draviti.client.render.EtherGearLayer;
import com.markadrii.draviti.client.render.GlowLimbLayer;
import com.markadrii.draviti.client.render.OverdriveLayer;
import com.markadrii.draviti.client.render.PlayerBody;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.forgeevent.BattleModeSustainableEvent;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Compatibilidad con Epic Fight (solo se carga si está instalado):
 * <ul>
 *   <li>Las capas del poder (marca del Ether Gear, aspecto del Overdrive, brillo de los golpes y el Ether Gear en el
 *       hombro) se dibujan sobre la malla de Epic Fight y siguen sus animaciones. Sin esto Epic Fight las dibuja
 *       quietas en la postura normal.</li>
 *   <li>Mientras suena una animación de una técnica (Player Animator), Epic Fight pasa un momento a modo normal y
 *       luego vuelve solo a modo combate (como hace Cursed Crown: Powers), y se respeta la cámara que tenías.</li>
 * </ul>
 */
public final class EpicFightCompat {
    private static final int HOLD_TICKS = 4;
    private static int hold;
    private static PlayerPatch<?> pendingPatch;
    private static boolean modeBefore;
    private static CameraType cameraBefore;

    private EpicFightCompat() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(EpicFightCompat::modifyRenderers);
        MinecraftForge.EVENT_BUS.addListener(EpicFightCompat::sustain);
        MinecraftForge.EVENT_BUS.addListener(EpicFightCompat::clientTick);
        Draviti.LOGGER.info("Epic Fight detectado: compatibilidad de D.raviti activada.");
    }

    /** Lo que dibuja cada capa sobre un cuerpo cualquiera. */
    @FunctionalInterface
    private interface Drawer {
        void draw(PlayerBody body, RenderLayer<?, ?> layer, PoseStack pose, MultiBufferSource buffer, int light, int overlay,
                  AbstractClientPlayer player, float partialTick, float age);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class EfLayer extends PatchedLayer {
        private final Drawer drawer;

        EfLayer(Drawer drawer) { this.drawer = drawer; }

        @Override
        protected void renderLayer(LivingEntityPatch patch, LivingEntity entity, RenderLayer layer, PoseStack pose,
                                   MultiBufferSource buffer, int light, OpenMatrix4f[] poses, float bob, float yRot,
                                   float xRot, float partialTick) {
            if (!(entity instanceof AbstractClientPlayer player)) return;
            HumanoidMesh mesh = ("slim".equals(player.getModelName()) ? Meshes.ALEX : Meshes.BIPED).get();
            if (mesh == null) return;
            PlayerBody body = new EfBody(mesh, patch.getArmature(), poses);
            drawer.draw(body, layer, pose, buffer, light, LivingEntityRenderer.getOverlayCoords(player, 0F), player,
                    partialTick, player.tickCount + partialTick);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void modifyRenderers(PatchedRenderersEvent.Modify event) {
        if (!(event.get(EntityType.PLAYER) instanceof PatchedLivingEntityRenderer r)) {
            Draviti.LOGGER.warn("Epic Fight: no se encontró el renderer del jugador; las capas de D.raviti se verán quietas.");
            return;
        }
        r.addPatchedLayer(EtherGearLayer.class, new EfLayer((b, l, pose, buf, light, ov, p, pt, age) ->
                EtherGearLayer.draw(b, pose, buf, light, ov, p)));
        r.addPatchedLayer(OverdriveLayer.class, new EfLayer((b, l, pose, buf, light, ov, p, pt, age) ->
                OverdriveLayer.draw(b, pose, buf, light, ov, p, pt, age)));
        r.addPatchedLayer(GlowLimbLayer.class, new EfLayer((b, l, pose, buf, light, ov, p, pt, age) ->
                GlowLimbLayer.draw(b, pose, buf, p, age)));
        r.addPatchedLayer(EtherGearShoulderLayer.class, new EfLayer((b, l, pose, buf, light, ov, p, pt, age) -> {
            if (l instanceof EtherGearShoulderLayer s) EtherGearShoulderLayer.draw(b, s.model(), pose, buf, light, p, age);
        }));
        Draviti.LOGGER.info("Epic Fight: capas de D.raviti añadidas al jugador.");
    }

    // ------------------------------------------------------------------ animaciones y modo combate

    private static void sustain(BattleModeSustainableEvent event) {
        PlayerPatch<?> patch = event.getPlayerPatch();
        Minecraft mc = Minecraft.getInstance();
        if (patch == null || mc.player == null || patch.getOriginal() != mc.player) return;
        pendingPatch = patch;
        modeBefore = patch.isEpicFightMode();
        cameraBefore = mc.options.getCameraType();
        if (hold > 0) event.setCanceled(true);
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { hold = 0; pendingPatch = null; return; }
        if (GravityAnimations.animating(mc.player)) hold = HOLD_TICKS;
        else if (hold > 0) hold--;
        PlayerPatch<?> patch = pendingPatch;
        if (patch == null) return;
        CameraType saved = cameraBefore;
        boolean before = modeBefore;
        pendingPatch = null;
        cameraBefore = null;
        if (saved == null || patch.isEpicFightMode() == before) return;
        if (mc.options.getCameraType() != saved) mc.options.setCameraType(saved);
    }

    /** Para la prueba automática: pone al jugador local en modo combate de Epic Fight. */
    public static void testBattleMode(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
        if (patch == null) return;
        if (on) patch.toEpicFightMode(true); else patch.toVanillaMode(true);
        Draviti.LOGGER.info("DRAVITI_PLAYTEST epicfight modo combate={}", patch.isEpicFightMode());
    }
}
