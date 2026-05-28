package com.prisonplanet.block;

import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.portal.CondemnedPortalShape;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CondemnedPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final int EXISTING_PORTAL_SEARCH_RADIUS = 4;
    private static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public CondemnedPortalBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .strength(-1.0F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 11)
                .pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_AABB : X_AXIS_AABB;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean changedAlongPortalPlane = direction.getAxis() != axis && direction.getAxis().isHorizontal();
        if (!changedAlongPortalPlane
                && !neighbor.is(this)
                && !CondemnedPortalShape.isCompletePortal(level, pos, axis)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (!(entity instanceof Player player)) {
            return 0;
        }
        return Math.max(1, level.getGameRules().getInt(player.getAbilities().invulnerable
                ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY
                : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY));
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel source, Entity entity, BlockPos entryPos) {
        ServerLevel destination;
        BlockPos proposedExit;
        boolean enteringCondemned;
        if (source.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            destination = source.getServer().getLevel(Level.OVERWORLD);
            if (destination == null) {
                return null;
            }
            proposedExit = destination.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(entryPos.getX(), 0, entryPos.getZ())).above();
            enteringCondemned = false;
        } else {
            destination = source.getServer().getLevel(ModDimensions.THE_CONDEMNED_KEY);
            if (destination == null) {
                return null;
            }
            proposedExit = new BlockPos(entryPos.getX(), 65, entryPos.getZ());
            enteringCondemned = true;
        }

        Direction.Axis axis = source.getBlockState(entryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
        BlockPos exitInterior = findExistingPortal(destination, proposedExit);
        if (exitInterior == null) {
            exitInterior = proposedExit;
            if (enteringCondemned) {
                buildArrivalCell(destination, exitInterior);
            }
            CondemnedPortalShape.createMinimumPortal(destination, exitInterior, axis);
        }
        Vec3 arrival = Vec3.atBottomCenterOf(exitInterior);
        Vec3 collisionFree = PortalShape.findCollisionFreePosition(
                arrival, destination, entity, entity.getDimensions(entity.getPose()));
        return new DimensionTransition(
                destination,
                collisionFree,
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS,
                    0.5F,
                    random.nextFloat() * 0.4F + 0.8F,
                    false);
        }

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double velocityX = (random.nextFloat() - 0.5) * 0.08;
            double velocityY = (random.nextFloat() - 0.5) * 0.08;
            double velocityZ = (random.nextFloat() - 0.5) * 0.08;
            level.addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
                    x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> state.setValue(
                    AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Nullable
    private static BlockPos findExistingPortal(ServerLevel level, BlockPos target) {
        for (int dx = -EXISTING_PORTAL_SEARCH_RADIUS; dx <= EXISTING_PORTAL_SEARCH_RADIUS; dx++) {
            for (int dz = -EXISTING_PORTAL_SEARCH_RADIUS; dz <= EXISTING_PORTAL_SEARCH_RADIUS; dz++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos candidate = new BlockPos(target.getX() + dx, y, target.getZ() + dz);
                    BlockState state = level.getBlockState(candidate);
                    if (!state.is(ModBlocks.CONDEMNED_PORTAL.get())) {
                        continue;
                    }
                    Direction.Axis portalAxis = state.getValue(AXIS);
                    if (!CondemnedPortalShape.isCompletePortal(level, candidate, portalAxis)) {
                        continue;
                    }
                    BlockPos bottom = candidate;
                    while (level.getBlockState(bottom.below()).is(ModBlocks.CONDEMNED_PORTAL.get())) {
                        bottom = bottom.below();
                    }
                    return bottom;
                }
            }
        }
        return null;
    }

    private static void buildArrivalCell(ServerLevel level, BlockPos center) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(center.offset(dx, -1, dz), ModBlocks.CONDEMNED_BRICKS.get().defaultBlockState());
                level.setBlockAndUpdate(center.offset(dx, 3, dz), ModBlocks.CONDEMNED_REINFORCED_BLOCK.get().defaultBlockState());
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(center.offset(2, 0, 2), ModBlocks.PHASE_LANTERN.get().defaultBlockState());
    }
}
