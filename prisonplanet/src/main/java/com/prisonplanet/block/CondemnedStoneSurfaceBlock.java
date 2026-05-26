package com.prisonplanet.block;

import com.prisonplanet.dimension.CyclePhase;
import com.prisonplanet.dimension.PrisonPlanetSavedData;
import com.prisonplanet.event.BlockExposureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
                .randomTicks()
                .lightLevel(state -> state.getValue(MOLTEN) ? 15 : 0)
                .sound(SoundType.STONE));
        registerDefaultState(stateDefinition.any().setValue(MOLTEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOLTEN);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!BlockExposureHelper.isExposed(level, pos)) return;

        CyclePhase phase = PrisonPlanetSavedData.getOrCreate(level).getCurrentPhase();
        if (phase == CyclePhase.DAY && !state.getValue(MOLTEN) && random.nextInt(3) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(MOLTEN, true));
        } else if (phase == CyclePhase.SUNSET && state.getValue(MOLTEN) && random.nextInt(2) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(MOLTEN, false));
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(MOLTEN)) {
            entity.igniteForSeconds(3);
        }
        super.entityInside(state, level, pos, entity);
    }
}
