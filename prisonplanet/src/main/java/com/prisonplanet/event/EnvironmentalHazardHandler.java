package com.prisonplanet.event;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.dimension.CyclePhase;
import com.prisonplanet.dimension.PrisonPlanetSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class EnvironmentalHazardHandler {
    private static final int MAX_FREEZE_TICKS = 140;

    private EnvironmentalHazardHandler() {}

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            return;
        }

        PrisonPlanetSavedData data = PrisonPlanetSavedData.getOrCreate(level);
        if (data.isHazardGracePeriod()
                && (data.getCurrentPhase() == CyclePhase.DAY || data.getCurrentPhase() == CyclePhase.NIGHT)) {
            thaw(player, 4);
            return;
        }

        switch (data.getCurrentPhase()) {
            case DAY -> applyDayHeat(level, player);
            case NIGHT -> applyNightCold(level, player);
            case SUNSET, SUNRISE -> thaw(player, 4);
        }
    }

    private static void applyDayHeat(ServerLevel level, ServerPlayer player) {
        if (!BlockExposureHelper.isExposed(level, player.blockPosition())
                || player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return;
        }
        if (player.tickCount % 40 == 0) {
            int skyLight = level.getBrightness(LightLayer.SKY, player.blockPosition().above());
            player.igniteForSeconds(skyLight >= 12 ? 3 : 2);
        }
    }

    private static void applyNightCold(ServerLevel level, ServerPlayer player) {
        if (!BlockExposureHelper.isExposed(level, player.blockPosition())) {
            thaw(player, 4);
            return;
        }
        if (player.tickCount % 10 == 0 && HeatSourceChecker.hasNearbyHeat(level, player.blockPosition())) {
            thaw(player, 40);
            return;
        }
        player.setTicksFrozen(Math.min(MAX_FREEZE_TICKS, player.getTicksFrozen() + 8));
        if (player.getTicksFrozen() >= MAX_FREEZE_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        } else if (player.getTicksFrozen() >= MAX_FREEZE_TICKS / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        }
    }

    private static void thaw(ServerPlayer player, int amount) {
        if (player.getTicksFrozen() > 0) {
            player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - amount));
        }
    }
}
