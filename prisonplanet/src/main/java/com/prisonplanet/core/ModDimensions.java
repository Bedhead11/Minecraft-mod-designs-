package com.prisonplanet.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public final class ModDimensions {

    public static final ResourceKey<Level> THE_CONDEMNED_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "the_condemned")
    );

    public static final ResourceKey<DimensionType> THE_CONDEMNED_TYPE_KEY = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "the_condemned_type")
    );

    private ModDimensions() {}
}
