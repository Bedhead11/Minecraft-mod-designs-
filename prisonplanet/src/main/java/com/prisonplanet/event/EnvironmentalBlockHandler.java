package com.prisonplanet.event;

import com.prisonplanet.block.CondemnedStoneSurfaceBlock;
import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.core.ModFluids;
import com.prisonplanet.dimension.CyclePhase;
import com.prisonplanet.dimension.MoltenStoneSavedData;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class EnvironmentalBlockHandler {
    private static final int HAZARD_CHUNK_SWEEP_INTERVAL = 20;
    private static final int NIGHT_SNOW_CHUNK_SWEEP_INTERVAL = 4;
    private static final int HAZARD_COLUMN_SAMPLES_PER_CHUNK = 4;
    private static final int NIGHT_SNOW_SAMPLES_PER_CHUNK = 1;
    private static final int MAX_MOLTEN_RESTORES_PER_TICK = 32;

    private EnvironmentalBlockHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().getLevel(ModDimensions.THE_CONDEMNED_KEY);
        if (level == null) return;

        CyclePhase phase = CyclePhase.fromDayTime(level.getDayTime());
        if (phase != CyclePhase.DAY) {
            MoltenStoneSavedData.getOrCreate(level).restoreLoaded(level, MAX_MOLTEN_RESTORES_PER_TICK);
        }
        processTickingChunks(level, phase);
    }

    private static void processTickingChunks(ServerLevel level, CyclePhase phase) {
        int simulationDistance = level.getServer().getPlayerList().getSimulationDistance();
        int sweepInterval = phase == CyclePhase.NIGHT
                ? NIGHT_SNOW_CHUNK_SWEEP_INTERVAL
                : HAZARD_CHUNK_SWEEP_INTERVAL;
        int columnSamples = phase == CyclePhase.NIGHT
                ? NIGHT_SNOW_SAMPLES_PER_CHUNK
                : HAZARD_COLUMN_SAMPLES_PER_CHUNK;
        long sweepSlot = Math.floorMod(level.getGameTime(), sweepInterval);
        Set<Long> processedChunks = new HashSet<>();
        level.players().forEach(player -> {
            ChunkPos center = player.chunkPosition();
            for (int chunkX = center.x - simulationDistance; chunkX <= center.x + simulationDistance; chunkX++) {
                for (int chunkZ = center.z - simulationDistance; chunkZ <= center.z + simulationDistance; chunkZ++) {
                    long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
                    if (!processedChunks.add(chunkKey)
                            || Math.floorMod(chunkKey, sweepInterval) != sweepSlot
                            || !level.shouldTickBlocksAt(chunkKey)
                            || level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                        continue;
                    }
                    processChunk(level, chunkX, chunkZ, phase, columnSamples);
                }
            }
        });
    }

    private static void processChunk(
            ServerLevel level, int chunkX, int chunkZ, CyclePhase phase, int columnSamples) {
        for (int sample = 0; sample < columnSamples; sample++) {
            int x = chunkX * 16 + level.random.nextInt(16);
            int z = chunkZ * 16 + level.random.nextInt(16);
            processSurfaceColumn(level, x, z, phase);
        }
    }

    private static void processSurfaceColumn(ServerLevel level, int x, int z, CyclePhase phase) {
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos airPos = new BlockPos(x, topY, z);
        BlockPos groundPos = airPos.below();
        BlockState ground = level.getBlockState(groundPos);

        if (phase == CyclePhase.DAY) {
            meltSurface(level, groundPos, ground);
            evaporateSurfaceFluid(level, groundPos, ground);
            igniteSurface(level, airPos, groundPos, ground);
        } else if (phase == CyclePhase.NIGHT) {
            solidifyLegacySurface(level, groundPos, ground);
            freezeAndSnow(level, airPos, groundPos, ground);
        } else if (phase == CyclePhase.SUNRISE) {
            solidifyLegacySurface(level, groundPos, ground);
            meltSnow(level, groundPos, ground);
        }
    }

    private static void meltSurface(ServerLevel level, BlockPos pos, BlockState state) {
        if (BlockExposureHelper.isExposed(level, pos) && level.random.nextInt(3) == 0) {
            MoltenStoneSavedData.getOrCreate(level).melt(level, pos, state);
        }
    }

    private static void evaporateSurfaceFluid(ServerLevel level, BlockPos pos, BlockState state) {
        if ((state.is(Blocks.WATER)
                || state.getFluidState().getType().isSame(ModFluids.SUSPENDED_BRINE.get()))
                && BlockExposureHelper.isExposed(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void igniteSurface(ServerLevel level, BlockPos airPos, BlockPos groundPos, BlockState ground) {
        if (!level.getBlockState(airPos).isAir()
                || !BlockExposureHelper.isExposed(level, groundPos)
                || !ground.isFlammable(level, groundPos, Direction.UP)
                || level.random.nextInt(4) != 0) {
            return;
        }
        BlockState fire = BaseFireBlock.getState(level, airPos);
        if (fire.canSurvive(level, airPos)) {
            level.setBlockAndUpdate(airPos, fire);
        }
    }

    private static void solidifyLegacySurface(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.CONDEMNED_STONE_SURFACE.get())
                && state.getValue(CondemnedStoneSurfaceBlock.MOLTEN)) {
            level.setBlockAndUpdate(pos, state.setValue(CondemnedStoneSurfaceBlock.MOLTEN, false));
        }
    }

    private static void freezeAndSnow(ServerLevel level, BlockPos airPos, BlockPos groundPos, BlockState ground) {
        if (!BlockExposureHelper.isExposed(level, groundPos)) return;
        if (ground.is(Blocks.WATER)) {
            level.setBlockAndUpdate(groundPos, Blocks.PACKED_ICE.defaultBlockState());
            return;
        }
        if (level.getBrightness(LightLayer.BLOCK, airPos) >= 10
                || !level.getBiome(airPos).value().shouldSnow(level, airPos)) {
            return;
        }
        BlockState landing = level.getBlockState(airPos);
        if (landing.is(Blocks.SNOW)) {
            int layers = landing.getValue(SnowLayerBlock.LAYERS);
            if (layers < 8) {
                BlockState accumulated = landing.setValue(SnowLayerBlock.LAYERS, layers + 1);
                Block.pushEntitiesUp(landing, accumulated, level, airPos);
                level.setBlockAndUpdate(airPos, accumulated);
            } else {
                level.setBlockAndUpdate(airPos, Blocks.SNOW_BLOCK.defaultBlockState());
            }
        } else if (landing.isAir()) {
            level.setBlockAndUpdate(airPos, Blocks.SNOW.defaultBlockState());
        }
    }

    private static void meltSnow(ServerLevel level, BlockPos topPos, BlockState top) {
        if (top.is(Blocks.SNOW)) {
            int layers = top.getValue(SnowLayerBlock.LAYERS);
            level.setBlockAndUpdate(topPos, layers == 1
                    ? Blocks.AIR.defaultBlockState()
                    : top.setValue(SnowLayerBlock.LAYERS, layers - 1));
        } else if (top.is(Blocks.SNOW_BLOCK)) {
            level.setBlockAndUpdate(topPos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7));
        } else if (top.is(Blocks.PACKED_ICE) && BlockExposureHelper.isExposed(level, topPos)) {
            level.setBlockAndUpdate(topPos, Blocks.WATER.defaultBlockState());
        }
    }
}
