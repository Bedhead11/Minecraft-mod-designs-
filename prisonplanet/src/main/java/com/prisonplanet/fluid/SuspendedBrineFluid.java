package com.prisonplanet.fluid;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.dimension.CyclePhase;
import com.prisonplanet.event.BlockExposureHelper;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Finite, viscous coolant whose buoyancy reverses the usual vertical spread direction.
 *
 * <p>Vanilla flowing fluids route toward downward drops. Suspended Brine mirrors
 * that search toward upward openings and builds shallow pools against ceilings.</p>
 */
public abstract class SuspendedBrineFluid extends BaseFlowingFluid {
    protected SuspendedBrineFluid(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canBeReplacedWith(
            FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.UP && !isSame(fluid);
    }

    @Override
    public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
        double motionX = 0.0D;
        double motionZ = 0.0D;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighbor = level.getFluidState(neighborPos);
            if (!neighbor.isEmpty() && !neighbor.getType().isSame(this)) {
                continue;
            }
            float neighborHeight = neighbor.getOwnHeight();
            float gradient = 0.0F;
            if (neighborHeight == 0.0F && !level.getBlockState(neighborPos).blocksMotion()) {
                FluidState risingNeighbor = level.getFluidState(neighborPos.above());
                if (risingNeighbor.getType().isSame(this) && risingNeighbor.getOwnHeight() > 0.0F) {
                    gradient = state.getOwnHeight() - (risingNeighbor.getOwnHeight() - 0.8888889F);
                }
            } else if (neighborHeight > 0.0F) {
                gradient = state.getOwnHeight() - neighborHeight;
            }
            if (gradient != 0.0F) {
                motionX += direction.getStepX() * gradient;
                motionZ += direction.getStepZ() * gradient;
            }
        }

        Vec3 flow = new Vec3(motionX, 0.0D, motionZ);
        return state.getValue(FALLING)
                ? flow.normalize().add(0.0D, 6.0D, 0.0D).normalize()
                : flow.normalize();
    }

    @Override
    protected void spread(Level level, BlockPos pos, FluidState state) {
        if (state.isEmpty()) {
            return;
        }
        BlockState currentBlock = level.getBlockState(pos);
        BlockPos risePos = pos.above();
        BlockState riseBlock = level.getBlockState(risePos);
        FluidState risingState = getNewLiquid(level, risePos, riseBlock);

        if (canSpreadTo(level, pos, currentBlock, Direction.UP, risePos, riseBlock,
                level.getFluidState(risePos), risingState.getType())) {
            spreadTo(level, risePos, riseBlock, Direction.UP, risingState);
            if (sourceNeighborCount(level, pos) >= 3) {
                spreadToSides(level, pos, state, currentBlock);
            }
        } else {
            spreadToSides(level, pos, state, currentBlock);
        }
    }

    private void spreadToSides(Level level, BlockPos pos, FluidState state, BlockState blockState) {
        int amount = state.getValue(FALLING) ? 7 : state.getAmount() - getDropOff(level);
        if (amount <= 0) {
            return;
        }
        getSpread(level, pos, blockState).forEach((direction, newState) -> {
            BlockPos spreadPos = pos.relative(direction);
            BlockState spreadBlock = level.getBlockState(spreadPos);
            if (canSpreadTo(level, pos, blockState, direction, spreadPos, spreadBlock,
                    level.getFluidState(spreadPos), newState.getType())) {
                spreadTo(level, spreadPos, spreadBlock, direction, newState);
            }
        });
    }

    @Override
    protected FluidState getNewLiquid(Level level, BlockPos pos, BlockState blockState) {
        int greatestAmount = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborBlock = level.getBlockState(neighborPos);
            FluidState neighbor = neighborBlock.getFluidState();
            if (!neighbor.getType().isSame(this)) {
                continue;
            }
            greatestAmount = Math.max(greatestAmount, neighbor.getAmount());
        }

        FluidState lowerFluid = level.getFluidState(pos.below());
        if (!lowerFluid.isEmpty() && lowerFluid.getType().isSame(this)) {
            return getFlowing(8, true);
        }
        int reducedAmount = greatestAmount - getDropOff(level);
        return reducedAmount <= 0
                ? Fluids.EMPTY.defaultFluidState()
                : getFlowing(reducedAmount, false);
    }

    @Override
    protected Map<Direction, FluidState> getSpread(Level level, BlockPos pos, BlockState blockState) {
        int shortestRise = Integer.MAX_VALUE;
        Map<Direction, FluidState> spread = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidatePos = pos.relative(direction);
            BlockState candidateBlock = level.getBlockState(candidatePos);
            FluidState candidateState = getNewLiquid(level, candidatePos, candidateBlock);
            if (!canSpreadTo(level, pos, blockState, direction, candidatePos, candidateBlock,
                    level.getFluidState(candidatePos), candidateState.getType())) {
                continue;
            }
            int distance = canRise(level, candidatePos, candidateBlock)
                    ? 0
                    : findNearestRise(level, candidatePos, candidateBlock, 1, direction.getOpposite());
            if (distance < shortestRise) {
                spread.clear();
                shortestRise = distance;
            }
            if (distance == shortestRise) {
                spread.put(direction, candidateState);
            }
        }
        return spread;
    }

    private int findNearestRise(
            Level level, BlockPos pos, BlockState state, int distance, Direction entryDirection) {
        if (distance > getSlopeFindDistance(level)) {
            return Integer.MAX_VALUE;
        }
        int nearest = Integer.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == entryDirection) {
                continue;
            }
            BlockPos candidatePos = pos.relative(direction);
            BlockState candidateBlock = level.getBlockState(candidatePos);
            if (!canSpreadTo(level, pos, state, direction, candidatePos, candidateBlock,
                    level.getFluidState(candidatePos), getFlowing())) {
                continue;
            }
            if (canRise(level, candidatePos, candidateBlock)) {
                return distance;
            }
            nearest = Math.min(nearest, findNearestRise(
                    level, candidatePos, candidateBlock, distance + 1, direction.getOpposite()));
        }
        return nearest;
    }

    private boolean canRise(Level level, BlockPos pos, BlockState blockState) {
        BlockPos risePos = pos.above();
        BlockState riseBlock = level.getBlockState(risePos);
        return canSpreadTo(level, pos, blockState, Direction.UP, risePos, riseBlock,
                level.getFluidState(risePos), getFlowing());
    }

    private int sourceNeighborCount(LevelReader level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isSourceOfThisType(level.getFluidState(pos.relative(direction)))) {
                count++;
            }
        }
        return count;
    }

    private boolean isSourceOfThisType(FluidState state) {
        return state.getType().isSame(this) && state.isSource();
    }

    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return level.getFluidState(pos.below()).getType().isSame(this) ? 1.0F : state.getOwnHeight();
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        double height = getHeight(state, level, pos);
        return height >= 1.0D
                ? Shapes.block()
                : Shapes.box(0.0D, 1.0D - height, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (!(level instanceof ServerLevel serverLevel)
                || !serverLevel.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            return;
        }
        if (CyclePhase.fromDayTime(serverLevel.getDayTime()) == CyclePhase.DAY
                && BlockExposureHelper.isExposed(serverLevel, pos)
                && random.nextInt(3) == 0) {
            serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        serverLevel.scheduleTick(pos, state.getType(), getTickDelay(serverLevel));
    }

    public static final class Source extends SuspendedBrineFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends SuspendedBrineFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
