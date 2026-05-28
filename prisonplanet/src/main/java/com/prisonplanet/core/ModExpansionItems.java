package com.prisonplanet.core;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Block items and mined resources introduced with the extended Condemned material set.
 */
public final class ModExpansionItems {
    static {
        ModExpansionBlocks.entries().forEach((id, block) ->
                ModItems.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
    }

    public static final DeferredHolder<Item, Item> SULFUR_CLUSTER =
            ModItems.ITEMS.register("sulfur_cluster", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FERRIC_SCRAP =
            ModItems.ITEMS.register("ferric_scrap", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> EMBER_CRYSTAL =
            ModItems.ITEMS.register("ember_crystal", () -> new Item(new Item.Properties()));

    public static void initialize() {
        // Class initialization performs the deferred registrations.
    }

    private ModExpansionItems() {
    }
}
