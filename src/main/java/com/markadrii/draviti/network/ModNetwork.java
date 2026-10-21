package com.markadrii.draviti.network;

import com.markadrii.draviti.Draviti;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red cliente-servidor. Registra aquí los paquetes con CHANNEL.messageBuilder(...). */
public final class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Draviti.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
    }
}
