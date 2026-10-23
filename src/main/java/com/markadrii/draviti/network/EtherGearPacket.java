package com.markadrii.draviti.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Del servidor al cliente: lo que dice el Ether Gear (menú de la U) y el diálogo de su interior. */
public record EtherGearPacket(int kind, Component line, List<Integer> options) {
    public static final int LINE = 0, DIALOG = 1, CLOSE = 2;

    public static EtherGearPacket line(Component line) { return new EtherGearPacket(LINE, line, List.of()); }
    public static EtherGearPacket dialog(Component line, List<Integer> options) { return new EtherGearPacket(DIALOG, line, options); }
    public static EtherGearPacket close(Component line) { return new EtherGearPacket(CLOSE, line, List.of()); }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeComponent(line);
        buf.writeVarInt(options.size());
        for (int o : options) buf.writeVarInt(o);
    }

    public static EtherGearPacket decode(FriendlyByteBuf buf) {
        int kind = buf.readVarInt();
        Component line = buf.readComponent();
        int n = buf.readVarInt();
        List<Integer> options = new ArrayList<>();
        for (int i = 0; i < n; i++) options.add(buf.readVarInt());
        return new EtherGearPacket(kind, line, options);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.markadrii.draviti.client.ethergear.EtherGearClient.receive(this));
        ctx.get().setPacketHandled(true);
    }
}
