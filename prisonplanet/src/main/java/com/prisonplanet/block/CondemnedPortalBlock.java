package com.prisonplanet.block;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.WeakHashMap;

public class CondemnedPortalBlock extends Block {

    public static final EnumProperty<net.minecraft.core.Direction.Axis> AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;

    // Tracks how many ticks a player has been inside the portal
    private static final WeakHashMap<ServerPlayer, PortalContact> PORTAL_TICKS = new WeakHashMap<>();

    // Ticks a player must stand in the portal before teleporting
    private static final int PORTAL_DELAY = 80;

    public CondemnedPortalBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .lightLevel(state -> 11)
                .sound(SoundType.STONE)
                .instabreak()
                .noOcclusion()
        );
        // Register default blockstate with axis=x
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, net.minecraft.core.Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Only act on server side
        if (level.isClientSide()) return;

        // Only act on ServerPlayer that is not a passenger
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isPassenger()) return;

        // Check portal cooldown
        if (player.getPortalCooldown() > 0) return;

        // Increment tick counter
        PortalContact previous = PORTAL_TICKS.get(player);
        int ticks = previous == null || previous.lastTick() < player.tickCount - 1
                ? 1
                : previous.lastTick() == player.tickCount ? previous.ticks() : previous.ticks() + 1;

        if (ticks >= PORTAL_DELAY) {
            PORTAL_TICKS.remove(player);
            teleportPlayer(player, level, pos);
        } else {
            PORTAL_TICKS.put(player, new PortalContact(ticks, player.tickCount));
        }
    }

    private static void teleportPlayer(ServerPlayer player, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ServerLevel destLevel;
        BlockPos destination;

        if (serverLevel.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            // Send to overworld
            destLevel = serverLevel.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (destLevel == null) return;
            destination = destLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(pos.getX(), 0, pos.getZ())).above();
        } else {
            // Send to the condemned
            destLevel = serverLevel.getServer().getLevel(ModDimensions.THE_CONDEMNED_KEY);
            if (destLevel == null) return;
            destination = new BlockPos(pos.getX(), 65, pos.getZ());
            buildArrivalCell(destLevel, destination);
        }

        player.changeDimension(new DimensionTransition(
                destLevel,
                Vec3.atCenterOf(destination),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        ));

        player.setPortalCooldown(100);
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

    private record PortalContact(int ticks, int lastTick) {}
}
