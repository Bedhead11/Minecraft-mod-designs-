package com.prisonplanet.core;

import com.prisonplanet.fluid.SuspendedBrineFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Fluid registrations for environmental substances native to the Condemned.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "prisonplanet");
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, "prisonplanet");

    public static final DeferredHolder<FluidType, FluidType> SUSPENDED_BRINE_TYPE =
            FLUID_TYPES.register("suspended_brine", () -> new FluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid_type.prisonplanet.suspended_brine")
                            .motionScale(0.007D)
                            .canPushEntity(true)
                            .canSwim(false)
                            .canDrown(false)
                            .fallDistanceModifier(0.35F)
                            .canExtinguish(false)
                            .canConvertToSource(false)
                            .supportsBoating(false)
                            .canHydrate(false)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                            .density(-900)
                            .temperature(274)
                            .viscosity(6000)));

    public static final DeferredHolder<Fluid, SuspendedBrineFluid.Source> SUSPENDED_BRINE =
            FLUIDS.register("suspended_brine", () -> new SuspendedBrineFluid.Source(properties()));
    public static final DeferredHolder<Fluid, SuspendedBrineFluid.Flowing> FLOWING_SUSPENDED_BRINE =
            FLUIDS.register("flowing_suspended_brine", () -> new SuspendedBrineFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(
                SUSPENDED_BRINE_TYPE::get,
                SUSPENDED_BRINE::get,
                FLOWING_SUSPENDED_BRINE::get)
                .block(() -> ModBlocks.SUSPENDED_BRINE.get())
                .bucket(() -> ModItems.SUSPENDED_BRINE_BUCKET.get())
                .slopeFindDistance(4)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

    public static void initialize() {
        // Class initialization performs the deferred registrations.
    }

    private ModFluids() {
    }
}
