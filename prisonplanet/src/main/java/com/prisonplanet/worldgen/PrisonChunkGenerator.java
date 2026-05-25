package com.prisonplanet.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PrisonChunkGenerator extends ChunkGenerator {

    public static final MapCodec<PrisonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, PrisonChunkGenerator::new)
    );

    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState STONE     = Blocks.STONE.defaultBlockState();
    private static final BlockState AIR        = Blocks.AIR.defaultBlockState();

    /** Y range: -64 (min) to 127 filled with deepslate; Y=127 surface = stone; Y>=128 = air. */
    private static final int MIN_Y        = -64;
    private static final int FILL_TOP_Y   = 127; // last deepslate y (inclusive)
    private static final int SURFACE_Y    = 127; // top solid layer (stone overlay)
    private static final int GEN_DEPTH    = 384;

    public PrisonChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // -----------------------------------------------------------------------
    // Core generation
    // -----------------------------------------------------------------------

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk) {

        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                // Fill deepslate from minY up to (FILL_TOP_Y - 1)
                for (int y = MIN_Y; y < FILL_TOP_Y; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), DEEPSLATE, false);
                }
                // Surface layer: stone at Y=127
                chunk.setBlockState(new BlockPos(x, SURFACE_Y, z), STONE, false);
                // Everything above Y=128 is air (default, no action needed)
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    // -----------------------------------------------------------------------
    // No-op overrides
    // -----------------------------------------------------------------------

    @Override
    public void applyCarvers(
            WorldGenRegion level,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving step) {
        // No carvers in the condemned dimension
    }

    @Override
    public void buildSurface(
            WorldGenRegion level,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk) {
        // Surface handled in fillFromNoise
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // No mob spawning during world generation
    }

    // -----------------------------------------------------------------------
    // Dimension metrics
    // -----------------------------------------------------------------------

    @Override
    public int getGenDepth() {
        return GEN_DEPTH; // 384
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return MIN_Y; // -64
    }

    @Override
    public int getBaseHeight(int x, int z, net.minecraft.world.level.levelgen.Heightmap.Types heightmapType,
                              LevelHeightAccessor level, RandomState randomState) {
        return SURFACE_Y + 1; // 128 — one block above the surface
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        // Column: deepslate from minY to 127, stone at 127 (overwritten), air above
        int columnHeight = SURFACE_Y - MIN_Y + 1; // 192 blocks (y=-64 to y=127 inclusive)
        BlockState[] states = new BlockState[columnHeight];
        for (int i = 0; i < columnHeight - 1; i++) {
            states[i] = DEEPSLATE;
        }
        // Top of column (Y=127) — stone surface
        states[columnHeight - 1] = STONE;
        return new NoiseColumn(MIN_Y, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Prison Planet Generator");
    }

    // -----------------------------------------------------------------------
    // Mob spawning (none)
    // -----------------------------------------------------------------------

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome,
            StructureManager structureManager,
            MobCategory category,
            BlockPos pos) {
        return WeightedRandomList.create();
    }
}
