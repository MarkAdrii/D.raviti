package com.markadrii.draviti.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * El cuerpo del jugador sobre el que se dibujan las capas del poder (marca, Overdrive, brillos, Ether Gear al
 * hombro). Hay dos: el modelo normal de Minecraft y el de Epic Fight (compat/epicfight), así las capas se ven igual
 * con o sin Epic Fight y siguen sus animaciones.
 */
public interface PlayerBody {
    enum Part { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, HAT, JACKET, RIGHT_SLEEVE, LEFT_SLEEVE, RIGHT_PANTS, LEFT_PANTS }

    /** Dibuja esas partes con la textura de la capa (mismo mapa de UV que una skin). */
    void draw(PoseStack pose, MultiBufferSource buffer, RenderType type, int light, int overlay,
              float r, float g, float b, float a, Part... parts);

    /** Mueve la pose a la cabeza (como ModelPart.translateAndRotate de la cabeza del modelo normal). */
    void toHead(PoseStack pose);

    /** Mueve la pose a la raíz del cuerpo (el cuello), en coordenadas del modelo normal. */
    void toBody(PoseStack pose);

    /** Modelo normal de Minecraft. */
    record Vanilla(PlayerModel<AbstractClientPlayer> m) implements PlayerBody {
        public ModelPart part(Part p) {
            return switch (p) {
                case HEAD -> m.head; case BODY -> m.body; case RIGHT_ARM -> m.rightArm; case LEFT_ARM -> m.leftArm;
                case RIGHT_LEG -> m.rightLeg; case LEFT_LEG -> m.leftLeg; case HAT -> m.hat; case JACKET -> m.jacket;
                case RIGHT_SLEEVE -> m.rightSleeve; case LEFT_SLEEVE -> m.leftSleeve; case RIGHT_PANTS -> m.rightPants;
                case LEFT_PANTS -> m.leftPants;
            };
        }

        @Override
        public void draw(PoseStack pose, MultiBufferSource buffer, RenderType type, int light, int overlay,
                         float r, float g, float b, float a, Part... parts) {
            VertexConsumer vc = buffer.getBuffer(type);
            for (Part p : parts) part(p).render(pose, vc, light, overlay, r, g, b, a);
        }

        @Override public void toHead(PoseStack pose) { m.head.translateAndRotate(pose); }
        @Override public void toBody(PoseStack pose) { }
    }

    Part[] ALL_INNER = {Part.HEAD, Part.BODY, Part.RIGHT_ARM, Part.LEFT_ARM, Part.RIGHT_LEG, Part.LEFT_LEG};
    Part[] ALL = Part.values();
}
