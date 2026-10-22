package com.markadrii.draviti.gravitycore;

import com.markadrii.draviti.gravitycore.api.GravityChangerAPIForge;
import com.markadrii.draviti.gravitycore.network.S2CEntityGravityPacket;
import com.markadrii.draviti.gravitycore.network.S2CLevelGravityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

public class ClientPacketHandler {
    public static void handle(S2CLevelGravityPacket s2CLevelGravityPacket) {
        ClientLevel level = Minecraft.getInstance().level;
        level.getCapability(GravityChangerAPIForge.LEVEL_GRAVITY).ifPresent(iLevelGravityData -> iLevelGravityData.fromNbt(s2CLevelGravityPacket.data));
    }

    public static void handle(S2CEntityGravityPacket s2CEntityGravityPacket) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity entity = level.getEntity(s2CEntityGravityPacket.entityID);
        if (entity != null) {
            entity.getCapability(GravityChangerAPIForge.ENTITY_GRAVITY).ifPresent(iEntityGravityData -> {
                iEntityGravityData.fromNbt(s2CEntityGravityPacket.data);
                iEntityGravityData.applyGravityChange();
            });
        }
    }
}
