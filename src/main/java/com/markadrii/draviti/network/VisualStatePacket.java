package com.markadrii.draviti.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Habilidades activas de otro jugador, para dibujar sus efectos (estelas, temblor, brazos). */
public class VisualStatePacket {
    final int entityId;
    final List<String> active;

    public VisualStatePacket(int entityId, List<String> active) { this.entityId = entityId; this.active = active; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(active.size());
        for (String s : active) buf.writeUtf(s);
    }

    public static VisualStatePacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int n = buf.readVarInt();
        List<String> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(buf.readUtf());
        return new VisualStatePacket(id, l);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleVisual(entityId, active));
        ctx.get().setPacketHandled(true);
    }
}
