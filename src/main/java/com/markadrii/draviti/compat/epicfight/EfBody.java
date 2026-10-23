package com.markadrii.draviti.compat.epicfight;

import com.markadrii.draviti.client.render.PlayerBody;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;

import java.lang.reflect.Field;

/** El cuerpo de Epic Fight: las capas se dibujan sobre su malla con sus huesos, así siguen sus animaciones. */
final class EfBody implements PlayerBody {
    private static Field hiddenField;

    private final HumanoidMesh mesh;
    private final Armature armature;
    private final OpenMatrix4f[] poses;

    EfBody(HumanoidMesh mesh, Armature armature, OpenMatrix4f[] poses) {
        this.mesh = mesh;
        this.armature = armature;
        this.poses = poses;
    }

    private MeshPart part(Part p) {
        return switch (p) {
            case HEAD -> mesh.head; case BODY -> mesh.torso; case RIGHT_ARM -> mesh.rightArm; case LEFT_ARM -> mesh.leftArm;
            case RIGHT_LEG -> mesh.rightLeg; case LEFT_LEG -> mesh.leftLeg; case HAT -> mesh.hat; case JACKET -> mesh.jacket;
            case RIGHT_SLEEVE -> mesh.rightSleeve; case LEFT_SLEEVE -> mesh.leftSleeve; case RIGHT_PANTS -> mesh.rightPants;
            case LEFT_PANTS -> mesh.leftPants;
        };
    }

    private static boolean hidden(MeshPart p) {
        try {
            if (hiddenField == null) {
                hiddenField = MeshPart.class.getDeclaredField("isHidden");
                hiddenField.setAccessible(true);
            }
            return hiddenField.getBoolean(p);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void draw(PoseStack pose, MultiBufferSource buffer, RenderType type, int light, int overlay,
                     float r, float g, float b, float a, Part... parts) {
        Part[] all = Part.values();
        boolean[] before = new boolean[all.length];
        boolean[] want = new boolean[all.length];
        for (Part p : parts) want[p.ordinal()] = true;
        for (Part p : all) {
            MeshPart mp = part(p);
            before[p.ordinal()] = hidden(mp);
            // Solo las que pide la capa (y las que Epic Fight u otro mod ya ocultaban siguen ocultas)
            mp.setHidden(before[p.ordinal()] || !want[p.ordinal()]);
        }
        try {
            mesh.draw(pose, buffer, type, light, r, g, b, a, overlay, armature, poses);
        } finally {
            for (Part p : all) part(p).setHidden(before[p.ordinal()]);
        }
    }

    private void toJoint(PoseStack pose, String joint) {
        var j = armature.searchJointByName(joint);
        if (j != null) MathUtils.mulStack(pose, poses[j.getId()]);
    }

    @Override
    public void toHead(PoseStack pose) {
        toJoint(pose, "Head");
        pose.scale(-1F, -1F, 1F);
    }

    @Override
    public void toBody(PoseStack pose) {
        // El pecho está 0,4 bloques por debajo del cuello
        toJoint(pose, "Chest");
        pose.translate(0F, 0.4F, 0F);
        pose.scale(-1F, -1F, 1F);
    }
}
