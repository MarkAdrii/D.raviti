package com.markadrii.draviti.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Cuernos del Overdrive: dos cuernos curvos a los lados de la cabeza (cada uno, 4 trozos que se van afinando).
 * Coordenadas de la cabeza del jugador: la caja va de -4 a 4 en x/z y de -8 a 0 en y.
 */
public final class HornModel {
    private static ModelPart root;

    private HornModel() {}

    public static ModelPart get() {
        if (root == null) root = create().bakeRoot();
        return root;
    }

    private static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        horn(r, "left", 1);
        horn(r, "right", -1);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void horn(PartDefinition root, String name, int side) {
        // Sale de la sien, hacia fuera y arriba, y se curva hacia atrás
        PartDefinition base = root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.25F, -3.5F, -1.25F, 2.5F, 3.5F, 2.5F),
                PartPose.offsetAndRotation(side * 4.2F, -6.2F, -0.5F, -0.15F, 0, side * 0.75F));
        PartDefinition mid = base.addOrReplaceChild("mid", CubeListBuilder.create()
                        .texOffs(10, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0, -3.3F, 0, -0.3F, 0, -side * 0.45F));
        PartDefinition top = mid.addOrReplaceChild("top", CubeListBuilder.create()
                        .texOffs(0, 8).addBox(-0.75F, -2.5F, -0.75F, 1.5F, 2.5F, 1.5F),
                PartPose.offsetAndRotation(0, -2.8F, 0, -0.45F, 0, -side * 0.3F));
        top.addOrReplaceChild("tip", CubeListBuilder.create()
                        .texOffs(8, 8).addBox(-0.4F, -2.0F, -0.4F, 0.8F, 2.0F, 0.8F),
                PartPose.offsetAndRotation(0, -2.3F, 0, -0.5F, 0, -side * 0.2F));
    }
}
