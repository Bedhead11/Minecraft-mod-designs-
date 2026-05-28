package com.prisonplanet.client;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.dimension.CyclePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Drives maximum visible snow during the Condemned night without changing overworld weather.
 */
@EventBusSubscriber(modid = "prisonplanet", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CondemnedWeatherClientEvents {
    private CondemnedWeatherClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            return;
        }
        level.setRainLevel(CyclePhase.fromDayTime(level.getDayTime()) == CyclePhase.NIGHT ? 1.0F : 0.0F);
    }
}
