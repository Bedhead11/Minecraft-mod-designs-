package com.prisonplanet.block;

import com.prisonplanet.core.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.WeakHashMap;

public class CondemnedPortalBlock extends Block {

    public static final EnumProperty<net.minecraft.core.Direction.Axis> AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;

    // Tracks how many ticks a player has been inside the portal
    private static final WeakHashMap<ServerPlayer, Integer> PORTAL_TICKS = new WeakHashMap<>();

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
        int ticks = PORTAL_TICKS.getOrDefault(player, 0) + 1;

        if (ticks >= PORTAL_DELAY) {
            PORTAL_TICKS.remove(player);
            teleportPlayer(player, level, pos);
        } else {
            PORTAL_TICKS.put(player, ticks);
        }
    }

    private static void teleportPlayer(ServerPlayer player, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ServerLevel destLevel;
        double destX = pos.getX();
        double destZ = pos.getZ();

        if (serverLevel.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            // Send to overworld
            destLevel = serverLevel.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        } else {
            // Send to the condemned
            destLevel = serverLevel.getServer().getLevel(ModDimensions.THE_CONDEMNED_KEY);
        }

        if (destLevel == null) return;

        // Teleport to a safe surface position
        player.changeDimension(new DimensionTransition(
                destLevel,
                new Vec3(destX + 0.5, 128.5, destZ + 0.5),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        ));

        player.setPortalCooldown(100);
    }
}
