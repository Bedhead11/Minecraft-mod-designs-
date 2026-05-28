package com.prisonplanet.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
public final class BlockExposureHelper {
    public static final int MIN_HAZARD_Y = 50;

    private BlockExposureHelper() {}

    public static boolean isExposed(ServerLevel level, BlockPos pos) {
        return pos.getY() >= MIN_HAZARD_Y
                && level.canSeeSky(pos.above());
    }
}
