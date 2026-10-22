package com.markadrii.draviti.gravitycore.config;

/** Valores del núcleo de gravedad (los mismos por defecto que Gravity Control). */
public class GravityChangerConfig {
    /** Duración del giro de cámara al cambiar la gravedad (ms). */
    public int rotationTime = 500;
    public boolean worldVelocity = false;
    public double gravityStrengthMultiplier = 1.0;
    public boolean resetGravityOnRespawn = true;
    public boolean voidDamageAboveWorld = true;
    public boolean voidDamageOnHorizontalFallTooFar = true;
    public boolean autoJumpOnGravityPlateInnerCorner = true;
    public boolean adjustPositionAfterChangingGravity = true;
}
