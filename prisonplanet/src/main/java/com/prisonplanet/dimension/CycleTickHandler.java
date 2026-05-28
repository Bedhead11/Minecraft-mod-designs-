package com.prisonplanet.dimension;

import com.prisonplanet.core.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Observes world time as an eight-day Condemned cycle with four equal phases.
 */
@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class CycleTickHandler {

    private CycleTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().getLevel(ModDimensions.THE_CONDEMNED_KEY);
        if (level == null) return;

        PrisonPlanetSavedData data = PrisonPlanetSavedData.getOrCreate(level);
        CyclePhase previousPhase = data.getCurrentPhase();
        data.synchronizeToVisibleDayTime(level.getDayTime());
        CyclePhase currentPhase = data.getCurrentPhase();

        if (previousPhase != currentPhase) {
            NeoForge.EVENT_BUS.post(new PhaseTransitionEvent(level, previousPhase, currentPhase));
        }
    }
}
