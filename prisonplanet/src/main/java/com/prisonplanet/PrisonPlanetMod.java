package com.prisonplanet;

import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModChunkGenerators;
import com.prisonplanet.core.ModCreativeTabs;
import com.prisonplanet.core.ModExpansionBlocks;
import com.prisonplanet.core.ModExpansionItems;
import com.prisonplanet.core.ModFluids;
import com.prisonplanet.core.ModEntities;
import com.prisonplanet.core.ModItems;
import com.prisonplanet.core.ModMobItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Main mod class. Event handlers (PortalActivationHandler, CycleTickHandler,
 * EnvironmentalHazardHandler) are registered automatically via @EventBusSubscriber.
 */
@Mod("prisonplanet")
public class PrisonPlanetMod {

    public PrisonPlanetMod(IEventBus modBus) {
        ModFluids.initialize();
        ModExpansionBlocks.initialize();
        ModExpansionItems.initialize();
        ModMobItems.initialize();
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modBus);
    }
}
