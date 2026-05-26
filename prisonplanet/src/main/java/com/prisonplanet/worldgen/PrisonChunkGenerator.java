package com.prisonplanet.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.prisonplanet.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
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
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PrisonChunkGenerator extends ChunkGenerator {
    public static final MapCodec<PrisonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, PrisonChunkGenerator::new)
    );

    private static final int MIN_Y = -64;
    private static final int SURFACE_Y = 127;
    private static final int GEN_DEPTH = 384;

    public PrisonChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        for (int x = chunkPos.getMinBlockX(); x < chunkPos.getMinBlockX() + 16; x++) {
            for (int z = chunkPos.getMinBlockZ(); z < chunkPos.getMinBlockZ() + 16; z++) {
                for (int y = MIN_Y; y <= SURFACE_Y; y++) {
                    BlockState state = isCarvedSpace(x, y, z)
                            ? Blocks.AIR.defaultBlockState()
                            : selectSubstrate(y);
                    chunk.setBlockState(new BlockPos(x, y, z), state, false);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(
            WorldGenRegion level,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving step) {
        // The generator carves prison passages as it fills its substrate.
    }

    @Override
    public void buildSurface(
            WorldGenRegion level,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk) {
        // The upper stratum and exposed reactive roof are placed during fill.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Custom phase-gated populations are a later content milestone.
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, net.minecraft.world.level.levelgen.Heightmap.Types heightmapType,
                             LevelHeightAccessor level, RandomState randomState) {
        return SURFACE_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = new BlockState[SURFACE_Y - MIN_Y + 1];
        for (int i = 0; i < states.length; i++) {
            int y = MIN_Y + i;
            states[i] = isCarvedSpace(x, y, z)
                    ? Blocks.AIR.defaultBlockState()
                    : selectSubstrate(y);
        }
        return new NoiseColumn(MIN_Y, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Prison Planet: carved prison decks");
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome,
            StructureManager structureManager,
            MobCategory category,
            BlockPos pos) {
        return WeightedRandomList.create();
    }

    private static BlockState selectSubstrate(int y) {
        if (y == SURFACE_Y) return ModBlocks.CONDEMNED_STONE_SURFACE.get().defaultBlockState();
        if (y < 0) return ModBlocks.CONDEMNED_DEEPSLATE.get().defaultBlockState();
        if (y < 50) return ModBlocks.CONDEMNED_CEMENT.get().defaultBlockState();
        if (y < 100) return ModBlocks.CONDEMNED_BRICKS.get().defaultBlockState();
        return ModBlocks.CONDEMNED_STONE.get().defaultBlockState();
    }

    private static boolean isCarvedSpace(int x, int y, int z) {
        if (y < 0) return isCatacombPassage(x, y, z);
        if (isSilo(x, z) && y >= 2 && y <= 127) return true;
        if (y >= 100) return isSurfaceBreach(x, y, z);
        return isDeckInterior(x, y, z);
    }

    private static boolean isDeckInterior(int x, int y, int z) {
        int relativeY = Math.floorMod(y, 8);
        if (relativeY < 1 || relativeY > 5) return false;
        int localX = Math.floorMod(x, 32);
        int localZ = Math.floorMod(z, 32);
        boolean corridor = between(localX, 14, 17) || between(localZ, 14, 17);
        boolean cellBlock = between(localX, 3, 11) && between(localZ, 3, 11);
        boolean guardRoom = between(localX, 20, 27) && between(localZ, 20, 27);
        boolean grandTunnel = between(Math.floorMod(x, 96), 43, 52) && y >= 48 && y <= 84;
        return corridor || cellBlock || guardRoom || grandTunnel;
    }

    private static boolean isCatacombPassage(int x, int y, int z) {
        if (y < -55 || y > -3) return false;
        int localX = Math.floorMod(x, 24);
        int localZ = Math.floorMod(z, 24);
        boolean tunnel = (between(localX, 10, 13) || between(localZ, 10, 13))
                && between(Math.floorMod(y, 8), 1, 5);
        boolean cavern = between(localX, 4, 18) && between(localZ, 4, 18)
                && y >= -32 && y <= -25;
        return tunnel || cavern;
    }

    private static boolean isSilo(int x, int z) {
        return between(Math.floorMod(x, 48), 21, 26)
                && between(Math.floorMod(z, 48), 21, 26);
    }

    private static boolean isSurfaceBreach(int x, int y, int z) {
        if (isSilo(x, z)) return true;
        return y >= 120
                && between(Math.floorMod(x, 64), 8, 20)
                && between(Math.floorMod(z, 64), 8, 20);
    }

    private static boolean between(int value, int min, int max) {
        return value >= min && value <= max;
    }
}
