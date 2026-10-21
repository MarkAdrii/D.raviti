package com.markadrii.draviti.network;

import com.markadrii.draviti.client.ClientVisuals;
import com.markadrii.draviti.client.fx.GravityFx;
import com.markadrii.draviti.data.PowerCapability;
import com.markadrii.draviti.data.PowerData;
import net.minecraft.client.Minecraft;

/** Solo se carga en el cliente. */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handleVisual(int entityId, java.util.List<String> active) {
        ClientVisuals.set(entityId, active);
    }

    public static void handleSync(PowerData data) {
        var player = Minecraft.getInstance().player;
        if (player != null) PowerCapability.get(player).ifPresent(d -> d.copyFrom(data));
    }

    public static void handleFx(FxPacket p) {
        GravityFx.play(p);
    }
}
