package com.markadrii.draviti.gravitycore;

import com.markadrii.draviti.gravitycore.config.GravityChangerConfig;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Núcleo de gravedad copiado de Gravity Control (Gravity Changer) de Tfarcenim, Fusion Flux y Gaider10 (licencia MIT,
 * ver GRAVITY_CHANGER_LICENSE.txt). Gira hitbox, físicas, cámara, controles y modelo de cualquier entidad según su
 * dirección de gravedad. D.raviti solo lo usa para Gravity Swap.
 */
public class GravityChanger {
    public static final String MOD_NAME = "D.raviti gravity core";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    public static final String MOD_ID = "draviti";
    public static final ResourceLocation DATA_COMPONENT_ID = id("gravity_data");
    public static final ResourceLocation DIMENSION_DATA_ID = id("gravity_dimension_data");
    public static final GravityChangerConfig config = new GravityChangerConfig();

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
