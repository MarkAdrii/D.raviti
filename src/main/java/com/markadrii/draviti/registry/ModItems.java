package com.markadrii.draviti.registry;

import com.markadrii.draviti.Draviti;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Objetos del mod. Añade aquí cada uno con ITEMS.register("nombre", () -> new Item(...)). */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Draviti.MOD_ID);
}
