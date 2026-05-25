package com.prisonplanet.portal;

import com.prisonplanet.block.CondemnedPortalBlock;
import com.prisonplanet.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility class that checks for a valid Netherite Block frame and fills the interior
 * with Condemned Portal blocks.
 *
 * Frame block: Blocks.NETHERITE_BLOCK
 * Interior min size: 2 wide x 3 tall
 * Interior max size: 21 wide x 21 tall
 * Checked on both X and Z axes.
 */
public final class CondemnedPortalShape {

    private static final int MIN_WIDTH  = 2;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_WIDTH  = 21;
    private static final int MAX_HEIGHT = 21;

    private CondemnedPortalShape() {}

    /**
     * Attempts to find a valid Netherite Block frame around {@code pos} and fill
     * the interior with Condemned Portal blocks.
     *
     * @return true if a portal was spawned
     */
    public static boolean trySpawnPortal(LevelAccessor level, BlockPos pos) {
        // Try X axis (portal faces east/west, frame extends along X and Y)
        if (tryAxis(level, pos, Direction.Axis.X)) return true;
        // Try Z axis (portal faces north/south, frame extends along Z and Y)
        return tryAxis(level, pos, Direction.Axis.Z);
    }

    private static boolean tryAxis(LevelAccessor level, BlockPos startPos, Direction.Axis axis) {
        // Directions along the horizontal axis and always up
        Direction forward = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction backward = forward.getOpposite();

        // Walk backward to find the left inner edge of the frame
        BlockPos innerLeft = findInnerEdge(level, startPos, backward);
        if (innerLeft == null) return false;

        // Walk forward to find the right inner edge
        BlockPos innerRight = findInnerEdge(level, startPos, forward);
        if (innerRight == null) return false;

        // Interior width computed along the horizontal axis (inclusive)
        int width2 = getAxisCoord(innerRight, axis) - getAxisCoord(innerLeft, axis) + 1;
        if (width2 < MIN_WIDTH || width2 > MAX_WIDTH) return false;

        // Walk downward from startPos to find the bottom inner edge
        BlockPos innerBottom = findInnerEdge(level, innerLeft, Direction.DOWN);
        if (innerBottom == null) return false;

        // Walk upward from startPos to find the top inner edge
        BlockPos innerTop = findInnerEdge(level, innerLeft, Direction.UP);
        if (innerTop == null) return false;

        int height = innerTop.getY() - innerBottom.getY() + 1;
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) return false;

        // Verify the frame is complete
        if (!isValidFrame(level, innerBottom, innerLeft, width2, height, axis)) return false;

        // Verify the interior is all air/replaceable
        if (!isInteriorClear(level, innerBottom, innerLeft, width2, height, axis)) return false;

        // Fill interior with portal blocks
        fillInterior(level, innerBottom, innerLeft, width2, height, axis);
        return true;
    }

    /** Walk in the given direction until we hit a Netherite Block (frame). Return the last non-frame pos. */
    private static BlockPos findInnerEdge(LevelAccessor level, BlockPos start, Direction dir) {
        BlockPos current = start;
        int maxSteps = MAX_WIDTH + 2;
        for (int i = 0; i <= maxSteps; i++) {
            BlockState state = level.getBlockState(current);
            if (state.is(Blocks.NETHERITE_BLOCK)) {
                // We hit the frame — the inner edge is one step back
                return i == 0 ? null : current.relative(dir.getOpposite());
            }
            if (!state.canBeReplaced() && !state.isAir()) {
                return null; // Blocked by something that isn't air or frame
            }
            current = current.relative(dir);
        }
        return null; // No frame found within range
    }

    private static boolean isValidFrame(LevelAccessor level, BlockPos innerBottom, BlockPos innerLeft,
                                         int width, int height, Direction.Axis axis) {
        Direction horizontal = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        // Bottom row (y = innerBottom.y - 1)
        int frameY = innerBottom.getY() - 1;
        BlockPos frameCornerBL = new BlockPos(innerLeft.getX(), frameY, innerLeft.getZ());
        for (int w = -1; w <= width; w++) {
            BlockPos p = frameCornerBL.relative(horizontal, w);
            if (!level.getBlockState(p).is(Blocks.NETHERITE_BLOCK)) return false;
        }

        // Top row (y = innerBottom.y + height)
        int topFrameY = innerBottom.getY() + height;
        BlockPos frameCornerTL = new BlockPos(innerLeft.getX(), topFrameY, innerLeft.getZ());
        for (int w = -1; w <= width; w++) {
            BlockPos p = frameCornerTL.relative(horizontal, w);
            if (!level.getBlockState(p).is(Blocks.NETHERITE_BLOCK)) return false;
        }

        // Left column and right column
        for (int h = 0; h < height; h++) {
            // Left side (one block outside innerLeft)
            BlockPos leftFrame = new BlockPos(innerLeft.getX(), innerBottom.getY() + h, innerLeft.getZ())
                    .relative(horizontal, -1);
            if (!level.getBlockState(leftFrame).is(Blocks.NETHERITE_BLOCK)) return false;

            // Right side (one block outside innerRight)
            BlockPos rightFrame = new BlockPos(innerLeft.getX(), innerBottom.getY() + h, innerLeft.getZ())
                    .relative(horizontal, width);
            if (!level.getBlockState(rightFrame).is(Blocks.NETHERITE_BLOCK)) return false;
        }

        return true;
    }

    private static boolean isInteriorClear(LevelAccessor level, BlockPos innerBottom, BlockPos innerLeft,
                                            int width, int height, Direction.Axis axis) {
        Direction horizontal = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                BlockPos p = new BlockPos(innerLeft.getX(), innerBottom.getY() + h, innerLeft.getZ())
                        .relative(horizontal, w);
                BlockState state = level.getBlockState(p);
                if (!state.isAir() && !state.canBeReplaced()) return false;
            }
        }
        return true;
    }

    private static void fillInterior(LevelAccessor level, BlockPos innerBottom, BlockPos innerLeft,
                                      int width, int height, Direction.Axis axis) {
        Direction horizontal = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockState portalState = ModBlocks.CONDEMNED_PORTAL.get().defaultBlockState()
                .setValue(CondemnedPortalBlock.AXIS, axis);

        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                BlockPos p = new BlockPos(innerLeft.getX(), innerBottom.getY() + h, innerLeft.getZ())
                        .relative(horizontal, w);
                level.setBlock(p, portalState, 3);
            }
        }
    }

    private static int getAxisCoord(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }
}
