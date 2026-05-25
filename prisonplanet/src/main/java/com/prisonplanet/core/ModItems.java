package com.prisonplanet.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, "prisonplanet");

    // BlockItem for the condemned portal (used for creative inventory / give command)
    public static final net.neoforged.neoforge.registries.DeferredHolder<Item, BlockItem> CONDEMNED_PORTAL =
            ITEMS.register("condemned_portal", () ->
                    new BlockItem(ModBlocks.CONDEMNED_PORTAL.get(), new Item.Properties()));

    private ModItems() {}
}
