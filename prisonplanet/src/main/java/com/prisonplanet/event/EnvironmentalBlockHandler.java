package com.prisonplanet.event;

import com.prisonplanet.block.CondemnedStoneSurfaceBlock;
import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.dimension.CyclePhase;
import com.prisonplanet.dimension.PrisonPlanetSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class EnvironmentalBlockHandler {
    private static final int UPDATE_RADIUS = 8;

    private EnvironmentalBlockHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().getLevel(ModDimensions.THE_CONDEMNED_KEY);
        if (level == null || level.getGameTime() % 20L != 0L) return;

        CyclePhase phase = PrisonPlanetSavedData.getOrCreate(level).getCurrentPhase();
        level.players().forEach(player -> processArea(level, player.blockPosition(), phase));
    }

    private static void processArea(ServerLevel level, BlockPos center, CyclePhase phase) {
        for (int dx = -UPDATE_RADIUS; dx <= UPDATE_RADIUS; dx++) {
            for (int dz = -UPDATE_RADIUS; dz <= UPDATE_RADIUS; dz++) {
                if ((Math.abs(dx) + Math.abs(dz)) % 3 != level.getGameTime() / 20L % 3) continue;
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                BlockPos airPos = new BlockPos(x, topY, z);
                BlockPos groundPos = airPos.below();
                BlockState ground = level.getBlockState(groundPos);

                if (phase == CyclePhase.DAY) {
                    meltSurface(level, groundPos, ground);
                    if (ground.is(Blocks.WATER) && BlockExposureHelper.isExposed(level, groundPos)) {
                        level.setBlockAndUpdate(groundPos, Blocks.AIR.defaultBlockState());
                    }
                } else if (phase == CyclePhase.SUNSET) {
                    solidifySurface(level, groundPos, ground);
                } else if (phase == CyclePhase.NIGHT) {
                    freezeAndSnow(level, airPos, groundPos, ground);
                } else {
                    meltSnow(level, groundPos, ground);
                }
            }
        }
    }

    private static void meltSurface(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.CONDEMNED_STONE_SURFACE.get())
                && !state.getValue(CondemnedStoneSurfaceBlock.MOLTEN)
                && BlockExposureHelper.isExposed(level, pos)
                && level.random.nextInt(3) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(CondemnedStoneSurfaceBlock.MOLTEN, true));
        }
    }

    private static void solidifySurface(ServerLevel level, BlockPos pos, BlockState state) {
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
        if (ground.is(Blocks.SNOW)) {
            int layers = ground.getValue(SnowLayerBlock.LAYERS);
            if (layers == 8) {
                level.setBlockAndUpdate(groundPos, Blocks.SNOW_BLOCK.defaultBlockState());
                level.setBlockAndUpdate(airPos, Blocks.SNOW.defaultBlockState());
            } else {
                level.setBlockAndUpdate(groundPos, ground.setValue(SnowLayerBlock.LAYERS, layers + 1));
            }
        } else if (level.getBlockState(airPos).isAir()) {
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
