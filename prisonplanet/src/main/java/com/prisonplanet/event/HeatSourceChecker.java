package com.prisonplanet.event;

import com.prisonplanet.block.CondemnedStoneSurfaceBlock;
import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class HeatSourceChecker {
    private static final int RADIUS = 5;

    private HeatSourceChecker() {}

    public static boolean hasNearbyHeat(ServerLevel level, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) continue;
                    BlockState state = level.getBlockState(center.offset(dx, dy, dz));
                    if (state.is(ModBlocks.CONDEMNED_STONE_SURFACE.get())
                            && state.getValue(CondemnedStoneSurfaceBlock.MOLTEN)) {
                        return true;
                    }
                    if (!state.is(ModTags.Blocks.HEAT_SOURCES)) continue;
                    if (state.hasProperty(BlockStateProperties.LIT)
                            && !state.getValue(BlockStateProperties.LIT)) continue;
                    return true;
                }
            }
        }
        return false;
    }
}
