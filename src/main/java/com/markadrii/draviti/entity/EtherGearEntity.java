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
    /** Dueño cuando está invocado fuera (null = es el de su interior). Solo servidor. */
    private java.util.UUID owner;

    public void setOwner(Player p) { owner = p.getUUID(); noPhysics = true; }
    public java.util.UUID owner() { return owner; }
    public boolean isCompanion() { return owner != null; }

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
        if (player instanceof ServerPlayer sp) {
            if (EtherGearInterior.isSessionEntity(sp.getUUID(), getUUID())) {
                EtherGearInterior.greet(sp);
            } else if (sp.getUUID().equals(owner)) {
                // Shift + clic con la mano vacía: al hombro; clic normal: hablar con él
                if (sp.isShiftKeyDown() && sp.getMainHandItem().isEmpty())
                    com.markadrii.draviti.ethergear.EtherGearCompanion.up(sp, this);
                else com.markadrii.draviti.ethergear.EtherGear.talk(sp);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Invocado: vuela al lado del hombro de su dueño; si se aleja mucho, aparece a su lado. */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || owner == null) return;
        Player p = level().getPlayerByUUID(owner);
        if (!(p instanceof ServerPlayer sp) || !com.markadrii.draviti.ethergear.EtherGearCompanion.isActive(sp, this)) {
            discard();
            return;
        }
        noPhysics = true;
        double yaw = Math.toRadians(p.getYRot());
        // Detrás y a la derecha del jugador, a la altura de la cabeza
        net.minecraft.world.phys.Vec3 target = p.position().add(-Math.cos(yaw) * 0.9 - (-Math.sin(yaw)) * 0.6,
                1.3 + Math.sin(tickCount * 0.08) * 0.15, -Math.sin(yaw) * 0.9 - Math.cos(yaw) * 0.6);
        net.minecraft.world.phys.Vec3 to = target.subtract(position());
        if (to.lengthSqr() > 16 * 16) {
            moveTo(target.x, target.y, target.z, p.getYRot(), 0);
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        } else {
            net.minecraft.world.phys.Vec3 v = to.scale(0.18);
            if (v.length() > 0.9) v = v.normalize().scale(0.9);
            setDeltaMovement(v);
        }
        // Mira a donde mira su dueño
        setYRot(p.getYRot());
        yBodyRot = p.getYRot();
        yHeadRot = p.getYHeadRot();
        setXRot(p.getXRot() * 0.5F);
    }

    @Override public boolean hurt(DamageSource source, float amount) { return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return true; }
    @Override protected void doPush(net.minecraft.world.entity.Entity e) {}
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean shouldShowName() { return false; }
    /** No se guarda con el mundo: aparece al entrar y se quita al salir. */
    @Override public boolean shouldBeSaved() { return false; }
}
