package com.prisonplanet.event;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.dimension.CyclePhase;
import com.prisonplanet.entity.CondemnedHazardAware;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class EnvironmentalHazardHandler {
    private static final int MAX_FREEZE_TICKS = 140;
    private static final int COLD_CHECK_INTERVAL = 10;

    private EnvironmentalHazardHandler() {}

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)
                || (!(living instanceof ServerPlayer) && !(living instanceof CondemnedHazardAware))
                || !(living.level() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            return;
        }
        if (living instanceof CondemnedHazardAware nativeMob && nativeMob.isEnvironmentalHazardImmune()) {
            living.setTicksFrozen(0);
            return;
        }

        CyclePhase phase = CyclePhase.fromDayTime(level.getDayTime());
        if (CyclePhase.ticksIntoPhase(level.getDayTime()) < 200L
                && (phase == CyclePhase.DAY || phase == CyclePhase.NIGHT)) {
            thaw(living, 4);
            return;
        }

        switch (phase) {
            case DAY -> applyDayHeat(level, living);
            case NIGHT -> applyNightCold(level, living);
            case SUNSET, SUNRISE -> thaw(living, 4);
        }
    }

    private static void applyDayHeat(ServerLevel level, LivingEntity living) {
        if (!BlockExposureHelper.isExposed(level, living.blockPosition())
                || living.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return;
        }
        if (living.tickCount % 40 == 0) {
            living.igniteForSeconds(3);
        }
    }

    private static void applyNightCold(ServerLevel level, LivingEntity living) {
        if (!BlockExposureHelper.isExposed(level, living.blockPosition())) {
            thaw(living, 4);
            return;
        }
        if (living.tickCount % COLD_CHECK_INTERVAL != 0) {
            return;
        }
        if (HeatSourceChecker.hasNearbyHeat(level, living.blockPosition())) {
            thaw(living, 40);
            return;
        }
        living.setTicksFrozen(Math.min(MAX_FREEZE_TICKS, living.getTicksFrozen() + 24));
        if (living.getTicksFrozen() >= MAX_FREEZE_TICKS) {
            refreshColdEffect(living, 1);
        } else if (living.getTicksFrozen() >= MAX_FREEZE_TICKS / 2) {
            refreshColdEffect(living, 0);
        }
    }

    private static void refreshColdEffect(LivingEntity living, int amplifier) {
        MobEffectInstance active = living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (active == null || active.getAmplifier() < amplifier || active.getDuration() <= 20) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, amplifier, false, false));
        }
    }

    private static void thaw(LivingEntity living, int amount) {
        if (living.getTicksFrozen() > 0) {
            living.setTicksFrozen(Math.max(0, living.getTicksFrozen() - amount));
        }
    }
}
