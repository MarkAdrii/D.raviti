package com.markadrii.draviti.entity;

import com.markadrii.draviti.gravity.Fx;
import com.markadrii.draviti.gravity.Gravity;
import com.markadrii.draviti.network.FxPacket;
import com.markadrii.draviti.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Esfera de gravedad: Cañón Gravitatorio (Jūdan), Meteor Breaker y Cielo Negro (Kokuten).
 * Mientras se carga va pegada al jugador (en el puño o sobre la cabeza) y crece; al lanzarla vuela en línea recta
 * y estalla al tocar algo.
 */
public class GravitySphereEntity extends Projectile {
    public static final int CANNON = 0, METEOR = 1, BLACK_SKY = 2;

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(GravitySphereEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(GravitySphereEntity.class, EntityDataSerializers.FLOAT);
    /** Forma de quien la lanza: 0-1 turquesa, 2 morada (Overdrive completo). */
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(GravitySphereEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HELD = SynchedEntityData.defineId(GravitySphereEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 8;
    private float radius = 2;
    private float knockback = 2;
    private int life;

    public GravitySphereEntity(EntityType<? extends GravitySphereEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static GravitySphereEntity create(ServerPlayer owner, int kind, float size) {
        GravitySphereEntity e = new GravitySphereEntity(ModEntities.GRAVITY_SPHERE.get(), owner.level());
        e.setOwner(owner);
        e.entityData.set(KIND, kind);
        e.entityData.set(SIZE, size);
        e.entityData.set(HELD, true);
        e.entityData.set(TIER, Gravity.tier(owner));
        e.setPos(e.anchor(owner));
        owner.level().addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, CANNON);
        entityData.define(SIZE, 0.5F);
        entityData.define(HELD, false);
        entityData.define(TIER, 0);
    }

    public int kind() { return entityData.get(KIND); }
    public int tier() { return entityData.get(TIER); }
    public float size() { return entityData.get(SIZE); }
    public void setSize(float s) { entityData.set(SIZE, s); }
    public boolean held() { return entityData.get(HELD); }
    public void setStats(float damage, float radius, float knockback) { this.damage = damage; this.radius = radius; this.knockback = knockback; }

    /** Dónde va mientras se carga: en el puño (cañón, meteoro) o sobre la cabeza y algo delante (Cielo Negro). */
    public Vec3 anchor(Entity owner) {
        Vec3 look = owner.getLookAngle();
        if (kind() == BLACK_SKY)
            return owner.position().add(look.x * 1.5, owner.getBbHeight() + 1.2 + size() * 0.9, look.z * 1.5);
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        return owner.getEyePosition().add(look.scale(0.9 + size() * 0.5)).add(right.scale(0.35)).subtract(0, 0.3, 0);
    }

    public void launch(Vec3 dir, double speed) {
        entityData.set(HELD, false);
        setDeltaMovement(dir.normalize().scale(speed));
        life = 0;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwner();
        if (held()) {
            if (owner == null || !owner.isAlive()) { if (!level().isClientSide) discard(); return; }
            Vec3 a = anchor(owner);
            setPos(a.x, a.y, a.z);
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && kind() == BLACK_SKY) pullAround(owner, 4 + size() * 2, 0.06);
            return;
        }
        if (level().isClientSide) {
            Vec3 v = getDeltaMovement();
            setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
            return;
        }
        if (++life > 160) { burst(position()); return; }

        Vec3 from = position();
        Vec3 v = getDeltaMovement();
        // Choque con bloques y con criaturas (con el tamaño de la esfera)
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        float s = size() * 0.5F;
        AABB box = getBoundingBox().inflate(s).expandTowards(v);
        boolean touch = !level().getEntities(this, box, this::canHitEntity).isEmpty();
        if (hit.getType() != HitResult.Type.MISS || touch) {
            Vec3 at = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : from;
            if (kind() == CANNON) cannonHit(at, v);
            burst(at);
            return;
        }
        setPos(from.x + v.x, from.y + v.y, from.z + v.z);
        if (kind() == BLACK_SKY) pullAround(owner, 3 + size() * 2, 0.08);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e)) return false;
        if (getOwner() instanceof ServerPlayer p) return Gravity.canHit(p, e);
        return e instanceof LivingEntity;
    }

    /** Atrae lo que haya cerca (agujero negro). */
    private void pullAround(Entity owner, double r, double strength) {
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(r), x -> x instanceof ItemEntity
                || (owner instanceof ServerPlayer p ? Gravity.canAffect(p, x) : x instanceof LivingEntity))) {
            if (e == owner) continue;
            Vec3 to = position().subtract(e.position());
            if (to.lengthSqr() < 0.5) continue;
            Gravity.push(e, to.normalize().scale(strength * Gravity.resist(e)));
        }
    }

    /** Cañón: el peso de la esfera empuja al objetivo hacia atrás. */
    private void cannonHit(Vec3 at, Vec3 dir) {
        if (!(getOwner() instanceof ServerPlayer p)) return;
        for (LivingEntity e : Gravity.around(p, at, size() + 1.2)) {
            Gravity.hurtRaw(p, e, damage);
            Vec3 d = dir.normalize().scale(knockback * Gravity.resist(e));
            Gravity.setMotion(e, new Vec3(d.x, Math.max(0.35, d.y + 0.35), d.z));
        }
    }

    private void burst(Vec3 at) {
        ServerLevel level = (ServerLevel) level();
        if (getOwner() instanceof ServerPlayer p) {
            switch (kind()) {
                case CANNON -> {
                    FxPacket.send(level, Fx.SPHERE_BURST, p, at, getDeltaMovement(), 1.5F, 0);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6F, 1.6F);
                }
                case METEOR -> {
                    for (LivingEntity e : Gravity.around(p, at, radius + 1)) {
                        Gravity.hurtRaw(p, e, damage);
                        Gravity.launchFrom(e, at, knockback, 0.8);
                    }
                    Gravity.crater(p, at, radius * 0.85, 0.8);
                    FxPacket.send(level, Fx.SPHERE_BURST, p, at, getDeltaMovement(), radius, 1);
                    Gravity.shake(p, at, 1.2F + radius * 0.2F, 18);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3F, 0.6F);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2F, 0.5F);
                }
                default -> blackSky(p, level, at);
            }
        }
        discard();
    }

    /** Cielo Negro: implosión (todo hacia el centro) y luego el golpe que lo aplasta todo. */
    private void blackSky(ServerPlayer p, ServerLevel level, Vec3 at) {
        float r = radius;
        FxPacket.send(level, Fx.BLACK_SKY_BURST, p, at, r, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 3F, 0.5F);
        Gravity.shake(p, at, 3F, 40);
        float dmg = damage, kb = knockback;
        com.markadrii.draviti.gravity.Tasks.start(p, t -> {
            // 1 segundo atrayendo todo hacia el punto
            for (LivingEntity e : Gravity.around(p, at, r * 2)) {
                Vec3 to = at.subtract(e.position());
                Gravity.setMotion(e, to.normalize().scale(Math.min(1.2, to.length() * 0.25) * Gravity.resist(e)));
            }
            if (t < 20) return false;
            for (LivingEntity e : Gravity.around(p, at, r + 1)) {
                Gravity.hurtRaw(p, e, dmg);
                Gravity.launchFrom(e, at, kb, 1.2);
            }
            // Esfera entera: aplasta lo que haya alrededor del punto, también por encima
            Gravity.crater(p, at, r, 1.0, true);
            FxPacket.send(level, Fx.IMPACT, p, at, r, 2);
            Gravity.shake(p, at, 4F, 45);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 6F, 0.4F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4F, 0.3F);
            return true;
        });
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 160 * 160; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind());
        tag.putFloat("Size", size());
        tag.putFloat("Damage", damage);
        tag.putFloat("Radius", radius);
        tag.putFloat("Knockback", knockback);
        tag.putInt("Tier", tier());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("Kind"));
        entityData.set(SIZE, tag.getFloat("Size"));
        damage = tag.getFloat("Damage");
        radius = tag.getFloat("Radius");
        knockback = tag.getFloat("Knockback");
        entityData.set(TIER, tag.getInt("Tier"));
        // Al cargar el mundo ya no hay nadie que la sujete
        entityData.set(HELD, false);
    }
}
