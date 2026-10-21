package com.markadrii.draviti.network;

import com.markadrii.draviti.gravity.Fx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Efecto visual en el cliente: tipo (ver {@link Fx}), entidad que lo hace, posición, dirección y dos valores
 * (tamaño, fuerza...). También sirve para animaciones del jugador y temblor de pantalla.
 */
public record FxPacket(int type, int entityId, double x, double y, double z,
                       float dx, float dy, float dz, float a, float b, int tier) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type);
        buf.writeVarInt(entityId);
        buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
        buf.writeFloat(dx); buf.writeFloat(dy); buf.writeFloat(dz);
        buf.writeFloat(a); buf.writeFloat(b);
        buf.writeByte(tier);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleFx(this));
        ctx.get().setPacketHandled(true);
    }

    public Vec3 pos() { return new Vec3(x, y, z); }
    public Vec3 dir() { return new Vec3(dx, dy, dz); }

    // ---- Envío desde el servidor ----

    /** A todos los que ven la zona. La forma (tier) se saca del jugador que lo hace: 0 normal, 1 Overdrive, 2 completo. */
    public static void send(ServerLevel level, int type, Entity source, Vec3 pos, Vec3 dir, float a, float b) {
        int tier = source instanceof net.minecraft.world.entity.player.Player pl ? com.markadrii.draviti.gravity.Gravity.tier(pl) : 0;
        send(level, type, source, pos, dir, a, b, tier);
    }

    public static void send(ServerLevel level, int type, Entity source, Vec3 pos, Vec3 dir, float a, float b, int tier) {
        FxPacket p = new FxPacket(type, source == null ? -1 : source.getId(), pos.x, pos.y, pos.z,
                (float) dir.x, (float) dir.y, (float) dir.z, a, b, tier);
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 96, level.dimension())), p);
    }

    public static void send(ServerLevel level, int type, Entity source, Vec3 pos, float a, float b) {
        send(level, type, source, pos, Vec3.ZERO, a, b);
    }
}
