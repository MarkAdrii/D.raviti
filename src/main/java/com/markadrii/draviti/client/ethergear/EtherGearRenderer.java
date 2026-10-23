package com.markadrii.draviti.client.ethergear;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.entity.EtherGearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Ether Gear: modelo con textura negra y morada; ojos, sonrisa y marcas de éter brillan en la oscuridad. */
public class EtherGearRenderer extends MobRenderer<EtherGearEntity, EtherGearModel> {
    public static final ResourceLocation TEXTURE = Draviti.id("textures/entity/ether_gear.png");
    public static final ResourceLocation GLOW = Draviti.id("textures/entity/ether_gear_glow.png");

    public EtherGearRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EtherGearModel(ctx.bakeLayer(EtherGearModel.LAYER)), 0.35F);
        addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() { return RenderType.eyes(GLOW); }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(EtherGearEntity e) { return TEXTURE; }

    @Override
    protected void scale(EtherGearEntity e, PoseStack pose, float partialTick) {
        pose.scale(0.9F, 0.9F, 0.9F);
    }
}
