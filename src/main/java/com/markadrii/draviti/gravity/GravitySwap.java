package com.markadrii.draviti.gravity;

import com.markadrii.draviti.Draviti;
import com.markadrii.draviti.ability.Ability;
import com.markadrii.draviti.ability.AbilityBehavior;
import com.markadrii.draviti.data.PowerManager;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gravity Swap: el bloque (o la dirección) al que miras se convierte en tu centro de gravedad. Caes hacia él, la
 * cámara gira y puedes andar por paredes y techos. Pulsar otra vez mirando a la misma superficie (o al suelo)
 * devuelve la gravedad normal. El cliente mueve el cuerpo; aquí se decide la dirección y se gasta el éter.
 */
@Mod.EventBusSubscriber(modid = Draviti.MOD_ID)
public final class GravitySwap {
    private static final Map<UUID, Direction> SWAP = new HashMap<>();

    private GravitySwap() {}

    /** Dirección de la gravedad del jugador (null = normal). */
    public static Direction get(ServerPlayer p) { return SWAP.get(p.getUUID()); }

    public static final AbilityBehavior BEHAVIOR = new AbilityBehavior() {
        @Override
        public boolean tryAction(ServerPlayer p, Ability a) {
            Direction now = SWAP.get(p.getUUID());
            Direction dir = target(p, a.stat("range"));
            if (dir == Direction.DOWN || dir == now) {
                if (now == null) return false;
                reset(p, true);
                return true;
            }
            if (!Gravity.spend(p, a.stat("ether_cost"))) return false;
            SWAP.put(p.getUUID(), dir);
            if (!p.isCreative() && !p.isSpectator()) {
                p.getAbilities().mayfly = true;   // si no, el servidor lo echa por "volar"
                p.onUpdateAbilities();
            }
            Gravity.fxOn(p, Fx.WEIGHTLESS, p, p.getBbHeight(), 20);
            Gravity.fx(p, Fx.DEFLECT, p.position().add(0, 1, 0), 2, 0);
            Gravity.sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 1F, 0.6F);
            Gravity.sound(p, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.6F, 0.6F);
            PowerManager.sync(p);
            return true;
        }
    };

    /** Cara del bloque al que miras (caes hacia ella) o, si no hay, el eje más cercano a donde miras. */
    static Direction target(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.BLOCK) return hit.getDirection().getOpposite();
        Vec3 l = p.getLookAngle();
        return Direction.getNearest(l.x, l.y, l.z);
    }

    public static void reset(ServerPlayer p, boolean effects) {
        if (SWAP.remove(p.getUUID()) == null) return;
        if (!p.isCreative() && !p.isSpectator() && !Gravity.active(p, "ingravidez")) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
        GravityAbilities.safeFall(p, 80);
        if (effects) {
            Gravity.fx(p, Fx.DEFLECT, p.position().add(0, 1, 0), 1, 0);
            Gravity.sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 1F, 1.2F);
        }
        PowerManager.sync(p);
    }

    /** Cada tick: gasta éter mientras la gravedad no es la normal. */
    public static void tick(ServerPlayer p) {
        Direction d = SWAP.get(p.getUUID());
        if (d == null) return;
        p.fallDistance = 0;
        if (!Gravity.has(p)) { reset(p, false); return; }
        if (p.tickCount % 20 == 0) {
            Ability a = Gravity.power().ability("gravity_swap").orElse(null);
            int cost = a == null ? 5 : (int) a.stat("ether_per_second");
            if (Gravity.ether(p) < cost) {
                p.displayClientMessage(Component.translatable("message.draviti.no_ether", cost, Gravity.ether(p)), true);
                reset(p, true);
                return;
            }
            Gravity.setEther(p, Gravity.ether(p) - cost);
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && SWAP.containsKey(p.getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) reset(p, false);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SWAP.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) reset(p, false);
    }
}
