package com.markadrii.draviti.entity;

import com.markadrii.draviti.ethergear.EtherGearInterior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * El Ether Gear: la entidad que vive dentro del poder. Pequeño demonio negro con pelo morado, marcas de éter por el
 * cuerpo, ojos morados y una sonrisita. Flota en su interior (no se mueve ni se le puede hacer daño) y sale en el
 * menú de la U.
 */
public class EtherGearEntity extends PathfinderMob {
    public EtherGearEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12F, 1F));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp && EtherGearInterior.isSessionEntity(sp.getUUID(), getUUID())) {
            EtherGearInterior.greet(sp);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public boolean hurt(DamageSource source, float amount) { return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount); }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(net.minecraft.world.entity.Entity e) {}
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean shouldShowName() { return false; }
    /** No se guarda con el mundo: aparece al entrar y se quita al salir. */
    @Override public boolean shouldBeSaved() { return false; }
}
