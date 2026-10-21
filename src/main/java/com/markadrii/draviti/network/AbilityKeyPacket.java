package com.markadrii.draviti.network;

import com.markadrii.draviti.data.PowerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilityKeyPacket {
    private final ResourceLocation power;
    private final String ability;
    private final boolean pressed;

    public AbilityKeyPacket(ResourceLocation power, String ability, boolean pressed) {
        this.power = power; this.ability = ability; this.pressed = pressed;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
        buf.writeUtf(ability, 64);
        buf.writeBoolean(pressed);
    }

    public static AbilityKeyPacket decode(FriendlyByteBuf buf) {
        return new AbilityKeyPacket(buf.readResourceLocation(), buf.readUtf(64), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) PowerManager.onKey(player, power, ability, pressed);
        ctx.get().setPacketHandled(true);
    }
}
