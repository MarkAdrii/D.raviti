package com.markadrii.draviti.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Icono de un poder o habilidad: un objeto o una textura PNG (se dibuja a 16x16). */
public final class Icon {
    private final java.util.function.Supplier<? extends ItemLike> item;
    private final ResourceLocation texture;

    /** Trozo de la textura (u, v, ancho, alto, ancho y alto de la textura); null = la textura entera. */
    private int[] region;

    private Icon(java.util.function.Supplier<? extends ItemLike> item, ResourceLocation texture) { this.item = item; this.texture = texture; }

    /** Un trozo de una textura (p. ej. la cara de un bicho de Minecraft). */
    public static Icon region(ResourceLocation texture, int u, int v, int w, int h, int texW, int texH) {
        Icon i = new Icon(null, texture);
        i.region = new int[]{u, v, w, h, texW, texH};
        return i;
    }

    public int[] region() { return region; }

    public static Icon item(ItemLike item) { return new Icon(() -> item, null); }
    /** Objeto de nuestro mod (aún sin registrar al crear los poderes). */
    public static Icon item(java.util.function.Supplier<? extends ItemLike> item) { return new Icon(item, null); }
    public static Icon texture(ResourceLocation texture) { return new Icon(null, texture); }

    public boolean isTexture() { return texture != null; }
    public ResourceLocation texture() { return texture; }
    public ItemStack stack() { return item == null ? ItemStack.EMPTY : new ItemStack(item.get()); }
}
