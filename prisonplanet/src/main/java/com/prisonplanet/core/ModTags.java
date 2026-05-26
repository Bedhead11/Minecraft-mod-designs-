package com.prisonplanet.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Blocks {
        public static final TagKey<Block> HEAT_SOURCES = TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("prisonplanet", "heat_sources")
        );

        private Blocks() {}
    }

    private ModTags() {}
}
