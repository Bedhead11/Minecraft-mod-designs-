package com.prisonplanet.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, "prisonplanet");

    // BlockItem for the condemned portal (used for creative inventory / give command)
    public static final DeferredHolder<Item, BlockItem> CONDEMNED_PORTAL =
            ITEMS.register("condemned_portal", () ->
                    new BlockItem(ModBlocks.CONDEMNED_PORTAL.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_STONE_SURFACE =
            ITEMS.register("condemned_stone_surface", () ->
                    new BlockItem(ModBlocks.CONDEMNED_STONE_SURFACE.get(), new Item.Properties()));

    // --- Terrain (4) ---
    public static final DeferredHolder<Item, BlockItem> CONDEMNED_STONE =
            ITEMS.register("condemned_stone", () ->
                    new BlockItem(ModBlocks.CONDEMNED_STONE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_DEEPSLATE =
            ITEMS.register("condemned_deepslate", () ->
                    new BlockItem(ModBlocks.CONDEMNED_DEEPSLATE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> SCORCHED_ROCK =
            ITEMS.register("scorched_rock", () ->
                    new BlockItem(ModBlocks.SCORCHED_ROCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> ASHEN_SEDIMENT =
            ITEMS.register("ashen_sediment", () ->
                    new BlockItem(ModBlocks.ASHEN_SEDIMENT.get(), new Item.Properties()));

    // --- Brick/wall (6) ---
    public static final DeferredHolder<Item, BlockItem> CONDEMNED_BRICKS =
            ITEMS.register("condemned_bricks", () ->
                    new BlockItem(ModBlocks.CONDEMNED_BRICKS.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CRACKED_CONDEMNED_BRICKS =
            ITEMS.register("cracked_condemned_bricks", () ->
                    new BlockItem(ModBlocks.CRACKED_CONDEMNED_BRICKS.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CHISELED_CONDEMNED_BRICKS =
            ITEMS.register("chiseled_condemned_bricks", () ->
                    new BlockItem(ModBlocks.CHISELED_CONDEMNED_BRICKS.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_MOSSY_BRICKS =
            ITEMS.register("condemned_mossy_bricks", () ->
                    new BlockItem(ModBlocks.CONDEMNED_MOSSY_BRICKS.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> POLISHED_CONDEMNED_STONE =
            ITEMS.register("polished_condemned_stone", () ->
                    new BlockItem(ModBlocks.POLISHED_CONDEMNED_STONE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_TILE =
            ITEMS.register("condemned_tile", () ->
                    new BlockItem(ModBlocks.CONDEMNED_TILE.get(), new Item.Properties()));

    // --- Dense composites (4) ---
    public static final DeferredHolder<Item, BlockItem> CONDEMNED_REINFORCED_BLOCK =
            ITEMS.register("condemned_reinforced_block", () ->
                    new BlockItem(ModBlocks.CONDEMNED_REINFORCED_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_CARVED_BLOCK =
            ITEMS.register("condemned_carved_block", () ->
                    new BlockItem(ModBlocks.CONDEMNED_CARVED_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_CEMENT =
            ITEMS.register("condemned_cement", () ->
                    new BlockItem(ModBlocks.CONDEMNED_CEMENT.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_METAL_PLATE =
            ITEMS.register("condemned_metal_plate", () ->
                    new BlockItem(ModBlocks.CONDEMNED_METAL_PLATE.get(), new Item.Properties()));

    // --- Pillar/beam (2) ---
    public static final DeferredHolder<Item, BlockItem> CONDEMNED_PILLAR =
            ITEMS.register("condemned_pillar", () ->
                    new BlockItem(ModBlocks.CONDEMNED_PILLAR.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_SUPPORT_BEAM =
            ITEMS.register("condemned_support_beam", () ->
                    new BlockItem(ModBlocks.CONDEMNED_SUPPORT_BEAM.get(), new Item.Properties()));

    // --- Lighting (2) ---
    public static final DeferredHolder<Item, BlockItem> CONDEMNED_LAMP =
            ITEMS.register("condemned_lamp", () ->
                    new BlockItem(ModBlocks.CONDEMNED_LAMP.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_FLOODLIGHT =
            ITEMS.register("condemned_floodlight", () ->
                    new BlockItem(ModBlocks.CONDEMNED_FLOODLIGHT.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> PHASE_LANTERN =
            ITEMS.register("phase_lantern", () ->
                    new BlockItem(ModBlocks.PHASE_LANTERN.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> GLACIAL_SHARD =
            ITEMS.register("glacial_shard", () -> new Item(new Item.Properties()));

    // --- Detail/industrial (6) ---
    public static final DeferredHolder<Item, BlockItem> CONDEMNED_GRATE =
            ITEMS.register("condemned_grate", () ->
                    new BlockItem(ModBlocks.CONDEMNED_GRATE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_BARS_BLOCK =
            ITEMS.register("condemned_bars_block", () ->
                    new BlockItem(ModBlocks.CONDEMNED_BARS_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_VENT =
            ITEMS.register("condemned_vent", () ->
                    new BlockItem(ModBlocks.CONDEMNED_VENT.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_HAZARD_BLOCK =
            ITEMS.register("condemned_hazard_block", () ->
                    new BlockItem(ModBlocks.CONDEMNED_HAZARD_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_SLAG =
            ITEMS.register("condemned_slag", () ->
                    new BlockItem(ModBlocks.CONDEMNED_SLAG.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> CONDEMNED_CHAIN_BLOCK =
            ITEMS.register("condemned_chain_block", () ->
                    new BlockItem(ModBlocks.CONDEMNED_CHAIN_BLOCK.get(), new Item.Properties()));

    private ModItems() {}
}
