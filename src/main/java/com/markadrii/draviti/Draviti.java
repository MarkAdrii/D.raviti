package com.markadrii.draviti;

import com.markadrii.draviti.network.ModNetwork;
import com.markadrii.draviti.registry.ModBlocks;
import com.markadrii.draviti.registry.ModCreativeTabs;
import com.markadrii.draviti.registry.ModEntities;
import com.markadrii.draviti.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Draviti.MOD_ID)
public class Draviti {
    public static final String MOD_ID = "draviti";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Draviti() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        LOGGER.info("D.raviti cargado.");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
