package com.markadrii.draviti.gravitycore;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Si Gravity Control está instalado no se aplica ningún mixin del núcleo (si no, girarían todo dos veces). */
public class GravityMixinPlugin implements IMixinConfigPlugin {
    private boolean external;

    @Override
    public void onLoad(String mixinPackage) {
        // Sin tocar clases de Minecraft aquí (aún no se pueden cargar)
        var list = net.minecraftforge.fml.loading.LoadingModList.get();
        external = list != null && list.getModFileById("gravitychanger") != null;
        try {
            com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
        } catch (Throwable ignored) {
            // MixinExtras ya iniciado por Forge u otro mod
        }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return !external; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
