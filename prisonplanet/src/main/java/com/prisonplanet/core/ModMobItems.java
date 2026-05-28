package com.prisonplanet.core;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModMobItems {
    public static final DeferredHolder<Item, DeferredSpawnEggItem> ASH_STALKER_SPAWN_EGG =
            egg("ash_stalker", ModEntities.ASH_STALKER::get, 0x302929, 0xB34D31);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> CHAIN_WRETCH_SPAWN_EGG =
            egg("chain_wretch", ModEntities.CHAIN_WRETCH::get, 0x25282A, 0x88766B);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> FROST_WRAITH_SPAWN_EGG =
            egg("frost_wraith", ModEntities.FROST_WRAITH::get, 0xCBD8DC, 0x75C5D6);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> VENT_CRAWLER_SPAWN_EGG =
            egg("vent_crawler", ModEntities.VENT_CRAWLER::get, 0x1F2024, 0xA74A24);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> CAGE_PHANTOM_SPAWN_EGG =
            egg("cage_phantom", ModEntities.CAGE_PHANTOM::get, 0x272B30, 0xA1A6A5);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> CINDER_HOUND_SPAWN_EGG =
            egg("cinder_hound", ModEntities.CINDER_HOUND::get, 0x2C211D, 0xEB642C);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> RUST_SENTINEL_SPAWN_EGG =
            egg("rust_sentinel", ModEntities.RUST_SENTINEL::get, 0x343537, 0x91352B);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SLAG_BRUTE_SPAWN_EGG =
            egg("slag_brute", ModEntities.SLAG_BRUTE::get, 0x292527, 0xC46630);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> FURNACE_WARDEN_SPAWN_EGG =
            egg("furnace_warden", ModEntities.FURNACE_WARDEN::get, 0x17181B, 0xFD8730);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> ASH_GRAZER_SPAWN_EGG =
            egg("ash_grazer", ModEntities.ASH_GRAZER::get, 0x5B5755, 0xB2A89B);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> GLACIAL_DRIFTER_SPAWN_EGG =
            egg("glacial_drifter", ModEntities.GLACIAL_DRIFTER::get, 0xB8D5D9, 0x56B5CF);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SALVAGE_PORTER_SPAWN_EGG =
            egg("salvage_porter", ModEntities.SALVAGE_PORTER::get, 0x4E3930, 0xB36A38);

    private static DeferredHolder<Item, DeferredSpawnEggItem> egg(
            String id,
            java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
            int baseColor,
            int spotColor) {
        return ModItems.ITEMS.register(id + "_spawn_egg",
                () -> new DeferredSpawnEggItem(type, baseColor, spotColor, new Item.Properties()));
    }

    public static void initialize() {
        // Class initialization performs the deferred registrations.
    }

    private ModMobItems() {
    }
}
