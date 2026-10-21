package com.markadrii.draviti.network;

import com.markadrii.draviti.Draviti;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red cliente-servidor. */
public final class ModNetwork {
    private static final String VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Draviti.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncPowerDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPowerDataPacket::encode).decoder(SyncPowerDataPacket::decode)
                .consumerMainThread(SyncPowerDataPacket::handle).add();
        CHANNEL.messageBuilder(AbilityKeyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AbilityKeyPacket::encode).decoder(AbilityKeyPacket::decode)
                .consumerMainThread(AbilityKeyPacket::handle).add();
        CHANNEL.messageBuilder(VisualStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VisualStatePacket::encode).decoder(VisualStatePacket::decode)
                .consumerMainThread(VisualStatePacket::handle).add();
        CHANNEL.messageBuilder(FxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FxPacket::encode).decoder(FxPacket::decode)
                .consumerMainThread(FxPacket::handle).add();
        CHANNEL.messageBuilder(MovePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MovePacket::encode).decoder(MovePacket::decode)
                .consumerMainThread(MovePacket::handle).add();
    }
}
