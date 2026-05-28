package com.prisonplanet.event;

import com.prisonplanet.block.CondemnedStoneSurfaceBlock;
import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class HeatSourceChecker {
    private static final int MAX_RADIUS = 12;

    private HeatSourceChecker() {}

    public static boolean hasNearbyHeat(ServerLevel level, BlockPos center) {
        for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx++) {
            for (int dy = -MAX_RADIUS; dy <= MAX_RADIUS; dy++) {
                for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz++) {
                    BlockState state = level.getBlockState(center.offset(dx, dy, dz));
                    int radius = getRadius(state);
                    if (radius == 0 || dx * dx + dy * dy + dz * dz > radius * radius) continue;
                    if (state.hasProperty(BlockStateProperties.LIT)
                            && !state.getValue(BlockStateProperties.LIT)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static int getRadius(BlockState state) {
        if (state.is(ModBlocks.CONDEMNED_STONE_SURFACE.get())
                && state.getValue(CondemnedStoneSurfaceBlock.MOLTEN)) {
            return 12;
        }
        if (!state.is(ModTags.Blocks.HEAT_SOURCES)) {
            return 0;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.LAVA)
                || state.is(ModBlocks.PHASE_LANTERN.get())) {
            return 12;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) {
            return 10;
        }
        return 8;
    }
}
