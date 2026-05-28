package com.prisonplanet.block;

import com.mojang.serialization.MapCodec;
import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModExpansionBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sparse surface growth adapted to the ash, cinder, and vitrified soils of the Condemned.
 */
public class CondemnedPlantBlock extends BushBlock {
    public static final MapCodec<CondemnedPlantBlock> CODEC = simpleCodec(CondemnedPlantBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public CondemnedPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CondemnedPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.ASHEN_SEDIMENT.get())
                || state.is(ModBlocks.SCORCHED_ROCK.get())
                || ModExpansionBlocks.isPlantSoil(state)
                || super.mayPlaceOn(state, level, pos);
    }
}
