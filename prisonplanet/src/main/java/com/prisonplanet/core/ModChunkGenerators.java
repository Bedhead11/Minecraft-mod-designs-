package com.prisonplanet.core;

import com.mojang.serialization.MapCodec;
import com.prisonplanet.worldgen.PrisonChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModChunkGenerators {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, "prisonplanet");

    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends ChunkGenerator>,
            MapCodec<PrisonChunkGenerator>> PRISON =
            CHUNK_GENERATORS.register("prison", () -> PrisonChunkGenerator.CODEC);

    private ModChunkGenerators() {}
}
