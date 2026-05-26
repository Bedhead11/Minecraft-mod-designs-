package com.prisonplanet.dimension;

import com.prisonplanet.core.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Ticks the Prison Planet cycle every server tick and syncs the dimension's
 * daytime so the sky reflects the current phase.
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
        data.tick();
        CyclePhase currentPhase = data.getCurrentPhase();

        // Map 192,000-tick cycle to 24,000 vanilla sky ticks for skybox rendering
        level.setDayTime(data.getCycleTick() / 8L);

        if (previousPhase != currentPhase) {
            NeoForge.EVENT_BUS.post(new PhaseTransitionEvent(level, previousPhase, currentPhase));
        }
    }
}
