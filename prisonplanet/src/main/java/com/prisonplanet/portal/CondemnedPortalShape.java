package com.prisonplanet.portal;

import com.prisonplanet.block.CondemnedPortalBlock;
import com.prisonplanet.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Validates and creates Nether-style portal rectangles using Netherite frames.
 * Corners are optional, so a 2 x 3 portal needs ten frame blocks.
 */
public final class CondemnedPortalShape {
    private static final int MIN_WIDTH = 2;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_WIDTH = 21;
    private static final int MAX_HEIGHT = 21;

    private CondemnedPortalShape() {}

    public static boolean trySpawnPortal(LevelAccessor level, BlockPos pos) {
        return tryAxis(level, pos, Direction.Axis.X, false)
                || tryAxis(level, pos, Direction.Axis.Z, false);
    }

    public static boolean isCompletePortal(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        return tryAxis(level, pos, axis, true);
    }

    public static void createMinimumPortal(LevelAccessor level, BlockPos innerBottom, Direction.Axis axis) {
        Direction horizontal = horizontal(axis);
        for (int width = 0; width < MIN_WIDTH; width++) {
            level.setBlock(innerBottom.below().relative(horizontal, width), Blocks.NETHERITE_BLOCK.defaultBlockState(), 3);
            level.setBlock(innerBottom.above(MIN_HEIGHT).relative(horizontal, width), Blocks.NETHERITE_BLOCK.defaultBlockState(), 3);
        }
        for (int height = 0; height < MIN_HEIGHT; height++) {
            level.setBlock(innerBottom.above(height).relative(horizontal, -1), Blocks.NETHERITE_BLOCK.defaultBlockState(), 3);
            level.setBlock(innerBottom.above(height).relative(horizontal, MIN_WIDTH), Blocks.NETHERITE_BLOCK.defaultBlockState(), 3);
        }
        fillInterior(level, innerBottom, MIN_WIDTH, MIN_HEIGHT, axis);
    }

    private static boolean tryAxis(LevelAccessor level, BlockPos startPos, Direction.Axis axis, boolean completeOnly) {
        Direction forward = horizontal(axis);
        Direction backward = forward.getOpposite();
        BlockPos innerLeft = findInnerEdge(level, startPos, backward, completeOnly);
        BlockPos innerRight = findInnerEdge(level, startPos, forward, completeOnly);
        if (innerLeft == null || innerRight == null) {
            return false;
        }

        int width = getAxisCoord(innerRight, axis) - getAxisCoord(innerLeft, axis) + 1;
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            return false;
        }

        BlockPos innerBottom = findInnerEdge(level, innerLeft, Direction.DOWN, completeOnly);
        BlockPos innerTop = findInnerEdge(level, innerLeft, Direction.UP, completeOnly);
        if (innerBottom == null || innerTop == null) {
            return false;
        }

        int height = innerTop.getY() - innerBottom.getY() + 1;
        if (height < MIN_HEIGHT || height > MAX_HEIGHT
                || !isValidFrame(level, innerBottom, width, height, axis)
                || !isInteriorValid(level, innerBottom, width, height, axis, completeOnly)) {
            return false;
        }

        if (!completeOnly) {
            fillInterior(level, innerBottom, width, height, axis);
        }
        return true;
    }

    private static BlockPos findInnerEdge(
            LevelAccessor level, BlockPos start, Direction direction, boolean allowPortal) {
        BlockPos current = start;
        int maxSteps = direction.getAxis() == Direction.Axis.Y ? MAX_HEIGHT + 2 : MAX_WIDTH + 2;
        for (int i = 0; i <= maxSteps; i++) {
            BlockState state = level.getBlockState(current);
            if (state.is(Blocks.NETHERITE_BLOCK)) {
                return i == 0 ? null : current.relative(direction.getOpposite());
            }
            if (!isInteriorBlock(state, allowPortal)) {
                return null;
            }
            current = current.relative(direction);
        }
        return null;
    }

    private static boolean isValidFrame(
            LevelAccessor level, BlockPos innerBottom, int width, int height, Direction.Axis axis) {
        Direction horizontal = horizontal(axis);
        for (int offset = 0; offset < width; offset++) {
            if (!level.getBlockState(innerBottom.below().relative(horizontal, offset)).is(Blocks.NETHERITE_BLOCK)
                    || !level.getBlockState(innerBottom.above(height).relative(horizontal, offset)).is(Blocks.NETHERITE_BLOCK)) {
                return false;
            }
        }
        for (int offset = 0; offset < height; offset++) {
            BlockPos row = innerBottom.above(offset);
            if (!level.getBlockState(row.relative(horizontal, -1)).is(Blocks.NETHERITE_BLOCK)
                    || !level.getBlockState(row.relative(horizontal, width)).is(Blocks.NETHERITE_BLOCK)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInteriorValid(
            LevelAccessor level, BlockPos innerBottom, int width, int height, Direction.Axis axis, boolean completeOnly) {
        Direction horizontal = horizontal(axis);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                BlockState state = level.getBlockState(innerBottom.above(y).relative(horizontal, x));
                if (completeOnly) {
                    if (!state.is(ModBlocks.CONDEMNED_PORTAL.get())
                            || state.getValue(CondemnedPortalBlock.AXIS) != axis) {
                        return false;
                    }
                } else if (!isInteriorBlock(state, false)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isInteriorBlock(BlockState state, boolean allowPortal) {
        return state.isAir()
                || state.canBeReplaced()
                || allowPortal && state.is(ModBlocks.CONDEMNED_PORTAL.get());
    }

    private static void fillInterior(
            LevelAccessor level, BlockPos innerBottom, int width, int height, Direction.Axis axis) {
        Direction horizontal = horizontal(axis);
        BlockState portal = ModBlocks.CONDEMNED_PORTAL.get().defaultBlockState()
                .setValue(CondemnedPortalBlock.AXIS, axis);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                level.setBlock(innerBottom.above(y).relative(horizontal, x), portal, 3);
            }
        }
    }

    private static Direction horizontal(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    private static int getAxisCoord(BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.X ? pos.getX() : pos.getZ();
    }
}
