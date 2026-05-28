package com.prisonplanet.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "prisonplanet");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THE_CONDEMNED =
            TABS.register("the_condemned", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.prisonplanet.the_condemned"))
                    .icon(() -> new ItemStack(ModItems.PHASE_LANTERN.get()))
                    .displayItems((parameters, output) ->
                            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
