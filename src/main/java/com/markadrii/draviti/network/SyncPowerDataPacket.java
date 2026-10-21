package com.markadrii.draviti.network;

import com.markadrii.draviti.data.PowerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPowerDataPacket {
    private final PowerData data;

    public SyncPowerDataPacket(PowerData data) { this.data = data; }

    public void encode(FriendlyByteBuf buf) { data.write(buf); }
    public static SyncPowerDataPacket decode(FriendlyByteBuf buf) { return new SyncPowerDataPacket(PowerData.read(buf)); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(data));
        ctx.get().setPacketHandled(true);
    }
}
