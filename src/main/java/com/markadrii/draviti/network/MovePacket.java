package com.markadrii.draviti.network;

import com.markadrii.draviti.gravity.GravityMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Del cliente al servidor: vuelo sónico (empezar/parar), dash y cambiar si se rompen bloques. */
public record MovePacket(int action, float x, float z) {
    public static final int SONIC_START = 0, SONIC_STOP = 1, DASH = 2, TOGGLE_BLOCKS = 3, TOGGLE_MARK = 4;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeFloat(x);
        buf.writeFloat(z);
    }

    public static MovePacket decode(FriendlyByteBuf buf) {
        return new MovePacket(buf.readVarInt(), buf.readFloat(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p != null) GravityMovement.handle(p, this);
        ctx.get().setPacketHandled(true);
    }
}
