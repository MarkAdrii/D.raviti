package com.markadrii.draviti.client;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.client.anim.GravityAnimations;
import com.markadrii.draviti.client.render.GlowLimbLayer;
import com.markadrii.draviti.client.render.GravitySphereRenderer;
import com.markadrii.draviti.client.render.OverdriveLayer;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.registry.ModEntities;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registro de cosas del cliente (teclas, barra, renderers, capas del jugador, animaciones). */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Habilidades activas de cualquier jugador (las manda el servidor) para los efectos y las pasivas en el cliente
        Gravity.clientActive = (player, key) -> ClientVisuals.active(player, key);
        if (ClientVisuals.playerAnimator()) event.enqueueWork(GravityAnimations::init);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ModKeys.register(event);
        event.register(ModKeys.ETHER_GEAR);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("ability_bar", new AbilityBarOverlay());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GRAVITY_SPHERE.get(), GravitySphereRenderer::new);
        event.registerEntityRenderer(ModEntities.ETHER_GEAR.get(), com.markadrii.draviti.client.ethergear.EtherGearRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.markadrii.draviti.client.ethergear.EtherGearModel.LAYER,
                com.markadrii.draviti.client.ethergear.EtherGearModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer pr) {
                pr.addLayer(new com.markadrii.draviti.client.render.EtherGearLayer(pr));
                pr.addLayer(new OverdriveLayer(pr));
                pr.addLayer(new GlowLimbLayer(pr));
                pr.addLayer(new com.markadrii.draviti.client.ethergear.EtherGearShoulderLayer(pr, event.getEntityModels()));
            }
        }
    }
}
