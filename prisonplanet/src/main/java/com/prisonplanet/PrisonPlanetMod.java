package com.prisonplanet;

import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModChunkGenerators;
import com.prisonplanet.core.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Main mod class. Event handlers (PortalActivationHandler, CycleTickHandler,
 * EnvironmentalHazardHandler) are registered automatically via @EventBusSubscriber.
 */
@Mod("prisonplanet")
public class PrisonPlanetMod {

    public PrisonPlanetMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modBus);
    }
}
