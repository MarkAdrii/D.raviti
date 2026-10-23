package com.markadrii.draviti.client.ethergear;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.entity.EtherGearEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * Modelo del Ether Gear: diablillo chibi negro con pelo blanco, aureola morada, cuernos, alas de murciélago y cola con
 * punta de flecha. Generado desde el mismo esquema que su textura (la textura es x2: 128x128 sobre 64x64).
 */
public class EtherGearModel extends HierarchicalModel<EtherGearEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Draviti.id("ether_gear"), "main");

    private final ModelPart root, head, halo, armR, armL, legR, legL, wingR, wingL, wingR2, wingL2, tail, tail2, tail3;

    public EtherGearModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = root.getChild("head");
        this.halo = head.getChild("halo");
        ModelPart body = root.getChild("body");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.wingR = body.getChild("wing_r");
        this.wingL = body.getChild("wing_l");
        this.wingR2 = wingR.getChild("wing_r2");
        this.wingL2 = wingL.getChild("wing_l2");
        this.tail = body.getChild("tail");
        this.tail2 = tail.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
    }

    @SuppressWarnings("unused")
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 14).addBox(-3F, 0F, -2F, 6F, 6F, 4F),
                PartPose.offset(0F, 14F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -7F, -3.5F, 8F, 7F, 7F),
                PartPose.offset(0F, 14F, 0F));
        PartDefinition hair = head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(30, 0).addBox(-4.5F, -8F, -4F, 9F, 3F, 8F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bangs = head.addOrReplaceChild("bangs", CubeListBuilder.create().texOffs(20, 26).addBox(-4F, -7F, -4.6F, 8F, 3F, 1F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition halo = head.addOrReplaceChild("halo", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0F, -10.5F, 0.5F, -0.209F, 0F, 0F));
        PartDefinition halo0 = halo.addOrReplaceChild("halo0", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(3.6F, 0F, 0F, 0F, 1.571F, 0F));
        PartDefinition halo1 = halo.addOrReplaceChild("halo1", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(3.118F, 0F, 1.8F, 0F, 1.047F, 0F));
        PartDefinition halo2 = halo.addOrReplaceChild("halo2", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(1.8F, 0F, 3.118F, 0F, 0.524F, 0F));
        PartDefinition halo3 = halo.addOrReplaceChild("halo3", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offset(0F, 0F, 3.6F));
        PartDefinition halo4 = halo.addOrReplaceChild("halo4", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(-1.8F, 0F, 3.118F, 0F, -0.524F, 0F));
        PartDefinition halo5 = halo.addOrReplaceChild("halo5", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(-3.118F, 0F, 1.8F, 0F, -1.047F, 0F));
        PartDefinition halo6 = halo.addOrReplaceChild("halo6", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(-3.6F, 0F, 0F, 0F, -1.571F, 0F));
        PartDefinition halo7 = halo.addOrReplaceChild("halo7", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(-3.118F, 0F, -1.8F, 0F, -2.094F, 0F));
        PartDefinition halo8 = halo.addOrReplaceChild("halo8", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(-1.8F, 0F, -3.118F, 0F, -2.618F, 0F));
        PartDefinition halo9 = halo.addOrReplaceChild("halo9", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(0F, 0F, -3.6F, 0F, -3.142F, 0F));
        PartDefinition halo10 = halo.addOrReplaceChild("halo10", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(1.8F, 0F, -3.118F, 0F, -3.665F, 0F));
        PartDefinition halo11 = halo.addOrReplaceChild("halo11", CubeListBuilder.create().texOffs(26, 37).addBox(-1F, -0.5F, -0.5F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(3.118F, 0F, -1.8F, 0F, -4.189F, 0F));
        PartDefinition hornl = head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(52, 14).addBox(-2F, -2F, -1F, 2F, 3F, 2F),
                PartPose.offsetAndRotation(-4F, -2.5F, -0.5F, 0F, 0F, 0.349F));
        PartDefinition hornr = head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(52, 14).addBox(0F, -2F, -1F, 2F, 3F, 2F),
                PartPose.offsetAndRotation(4F, -2.5F, -0.5F, 0F, 0F, -0.349F));
        PartDefinition armr = root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(20, 14).addBox(-2F, -1F, -1F, 2F, 6F, 2F),
                PartPose.offsetAndRotation(-3F, 15F, 0F, 0F, 0F, 0.384F));
        PartDefinition arml = root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(28, 14).addBox(0F, -1F, -1F, 2F, 6F, 2F),
                PartPose.offsetAndRotation(3F, 15F, 0F, 0F, 0F, -0.384F));
        PartDefinition legr = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(36, 14).addBox(-1F, 0F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(-1.5F, 20F, 0F, 0F, 0F, 0.14F));
        PartDefinition legl = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(44, 14).addBox(-1F, 0F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(1.5F, 20F, 0F, 0F, 0F, -0.14F));
        PartDefinition wingl = body.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 37).addBox(0F, -5F, 0F, 5F, 7F, 0F),
                PartPose.offsetAndRotation(1.5F, 1.5F, 2.1F, 0F, -0.489F, -0.314F));
        PartDefinition wingl2 = wingl.addOrReplaceChild("wing_l2", CubeListBuilder.create().texOffs(12, 37).addBox(0F, -1F, 0F, 7F, 8F, 0F),
                PartPose.offsetAndRotation(5F, -4.6F, 0F, 0F, -0.384F, -0.14F));
        PartDefinition wingr = body.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 37).mirror().addBox(-5F, -5F, 0F, 5F, 7F, 0F),
                PartPose.offsetAndRotation(-1.5F, 1.5F, 2.1F, 0F, 0.489F, 0.314F));
        PartDefinition wingr2 = wingr.addOrReplaceChild("wing_r2", CubeListBuilder.create().texOffs(12, 37).mirror().addBox(-7F, -1F, 0F, 7F, 8F, 0F),
                PartPose.offsetAndRotation(-5F, -4.6F, 0F, 0F, 0.384F, 0.14F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(50, 26).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F),
                PartPose.offsetAndRotation(0F, 5.5F, 2F, 0.698F, 0F, 0F));
        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(50, 26).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F),
                PartPose.offsetAndRotation(0F, 0F, 4F, -0.785F, 0.262F, 0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(50, 26).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 4F),
                PartPose.offsetAndRotation(0F, 0F, 4F, -0.611F, 0.262F, 0F));
        PartDefinition tailtip = tail3.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(50, 31).addBox(-0.5F, -1.5F, 0F, 1F, 3F, 3F),
                PartPose.offset(0F, 0F, 4F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() { return root; }

    /** Flota subiendo y bajando, balancea brazos, alas y cola, y mira al jugador. */
    @Override
    public void setupAnim(EtherGearEntity e, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        root.y += Mth.sin(age * 0.08F) * 1.2F - 1.5F;
        head.yRot = headYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F;
        head.zRot = Mth.sin(age * 0.05F) * 0.05F;
        float arm = Mth.sin(age * 0.09F);
        armR.zRot += arm * 0.1F;
        armL.zRot -= arm * 0.1F;
        armR.xRot = Mth.sin(age * 0.07F) * 0.18F;
        armL.xRot = -Mth.sin(age * 0.07F) * 0.18F;
        legR.xRot = Mth.sin(age * 0.06F) * 0.12F;
        legL.xRot = -Mth.sin(age * 0.06F) * 0.12F;
        float flap = Mth.sin(age * 0.25F) * 0.3F;
        wingR.yRot -= flap;
        wingL.yRot += flap;
        wingR2.yRot -= flap * 0.6F;
        wingL2.yRot += flap * 0.6F;
        // La aureola gira despacio y sube y baja
        halo.yRot = age * 0.03F;
        halo.y += Mth.sin(age * 0.1F) * 0.4F;
        tail.yRot = Mth.sin(age * 0.1F) * 0.35F;
        tail2.yRot += Mth.sin(age * 0.1F - 0.7F) * 0.25F;
        tail3.yRot += Mth.sin(age * 0.1F - 1.4F) * 0.25F;
    }
}
