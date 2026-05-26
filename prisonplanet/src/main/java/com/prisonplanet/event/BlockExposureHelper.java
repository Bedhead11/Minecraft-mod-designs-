package com.prisonplanet.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;

public final class BlockExposureHelper {
    public static final int MIN_HAZARD_Y = 50;
    public static final int MIN_SKY_LIGHT = 7;

    private BlockExposureHelper() {}

    public static boolean isExposed(ServerLevel level, BlockPos pos) {
        return pos.getY() >= MIN_HAZARD_Y
                && level.getBrightness(LightLayer.SKY, pos.above()) >= MIN_SKY_LIGHT;
    }
}
