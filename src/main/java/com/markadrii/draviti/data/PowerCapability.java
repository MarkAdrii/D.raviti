package com.markadrii.draviti.data;

import com.markadrii.draviti.Draviti;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;

public final class PowerCapability {
    public static final Capability<PowerData> POWER_DATA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation KEY = Draviti.id("power_data");

    private PowerCapability() {}

    public static Optional<PowerData> get(Player player) {
        return player.getCapability(POWER_DATA).resolve();
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PowerData data = new PowerData();
        private final LazyOptional<PowerData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == POWER_DATA ? optional.cast() : LazyOptional.empty();
        }

        @Override public CompoundTag serializeNBT() { return data.save(); }
        @Override public void deserializeNBT(CompoundTag tag) { data.load(tag); }

        public void invalidate() { optional.invalidate(); }
    }
}
