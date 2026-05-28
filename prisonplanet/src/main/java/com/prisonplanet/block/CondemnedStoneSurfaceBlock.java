package com.prisonplanet.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class CondemnedStoneSurfaceBlock extends Block {
    public static final BooleanProperty MOLTEN = BooleanProperty.create("molten");

    public CondemnedStoneSurfaceBlock() {
        super(BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(3.0f, 6.0f)
                .sound(SoundType.STONE));
        registerDefaultState(stateDefinition.any().setValue(MOLTEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOLTEN);
    }
}
