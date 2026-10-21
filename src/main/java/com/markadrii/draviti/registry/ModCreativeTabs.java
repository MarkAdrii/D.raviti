package com.markadrii.draviti.registry;

import com.markadrii.draviti.Draviti;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Draviti.MOD_ID);

    /** Pestaña creativa: mete todos los objetos registrados en ModItems. Cambia el icono cuando haya uno propio. */
    public static final RegistryObject<CreativeModeTab> DRAVITI_TAB = TABS.register("draviti", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.draviti"))
            .icon(() -> new ItemStack(Items.NETHER_STAR))
            .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());
}
