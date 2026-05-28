package com.prisonplanet.mixin;

import com.prisonplanet.dimension.CyclePhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes vanilla ambient skylight through the extended Condemned day cycle.
 */
@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {
    private static final ResourceLocation CONDEMNED_EFFECTS =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "the_condemned");

    @Shadow
    public abstract ResourceLocation effectsLocation();

    @Inject(method = "timeOfDay", at = @At("HEAD"), cancellable = true)
    private void prisonplanet$useExtendedSkyLightCycle(long dayTime, CallbackInfoReturnable<Float> callback) {
        if (CONDEMNED_EFFECTS.equals(effectsLocation())) {
            callback.setReturnValue(CyclePhase.skylightTimeOfDay(dayTime));
        }
    }
}
