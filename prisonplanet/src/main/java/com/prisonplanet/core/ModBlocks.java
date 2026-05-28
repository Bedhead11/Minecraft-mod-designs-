package com.prisonplanet.core;

import com.prisonplanet.block.CondemnedPortalBlock;
import com.prisonplanet.block.CondemnedStoneSurfaceBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, "prisonplanet");

    // --- Existing ---
    public static final DeferredHolder<Block, CondemnedPortalBlock> CONDEMNED_PORTAL =
            BLOCKS.register("condemned_portal", CondemnedPortalBlock::new);

    public static final DeferredHolder<Block, CondemnedStoneSurfaceBlock> CONDEMNED_STONE_SURFACE =
            BLOCKS.register("condemned_stone_surface", CondemnedStoneSurfaceBlock::new);

    // --- Terrain (4) ---
    public static final DeferredHolder<Block, Block> CONDEMNED_STONE =
            BLOCKS.register("condemned_stone",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_DEEPSLATE =
            BLOCKS.register("condemned_deepslate",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> SCORCHED_ROCK =
            BLOCKS.register("scorched_rock",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> ASHEN_SEDIMENT =
            BLOCKS.register("ashen_sediment",
                () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.6f).sound(SoundType.SAND)));

    public static final DeferredHolder<Block, LiquidBlock> SUSPENDED_BRINE =
            BLOCKS.register("suspended_brine",
                () -> new LiquidBlock(ModFluids.SUSPENDED_BRINE.get(), BlockBehaviour.Properties.of()
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                    .randomTicks().sound(SoundType.EMPTY)));

    // --- Brick/wall (6) ---
    public static final DeferredHolder<Block, Block> CONDEMNED_BRICKS =
            BLOCKS.register("condemned_bricks",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CRACKED_CONDEMNED_BRICKS =
            BLOCKS.register("cracked_condemned_bricks",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CHISELED_CONDEMNED_BRICKS =
            BLOCKS.register("chiseled_condemned_bricks",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_MOSSY_BRICKS =
            BLOCKS.register("condemned_mossy_bricks",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> POLISHED_CONDEMNED_STONE =
            BLOCKS.register("polished_condemned_stone",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_TILE =
            BLOCKS.register("condemned_tile",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.STONE)));

    // --- Dense composites (4) ---
    public static final DeferredHolder<Block, Block> CONDEMNED_REINFORCED_BLOCK =
            BLOCKS.register("condemned_reinforced_block",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(5.0f, 1200.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_CARVED_BLOCK =
            BLOCKS.register("condemned_carved_block",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_CEMENT =
            BLOCKS.register("condemned_cement",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_METAL_PLATE =
            BLOCKS.register("condemned_metal_plate",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 8.0f).sound(SoundType.METAL)));

    // --- Pillar/beam (2) ---
    public static final DeferredHolder<Block, RotatedPillarBlock> CONDEMNED_PILLAR =
            BLOCKS.register("condemned_pillar",
                () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, RotatedPillarBlock> CONDEMNED_SUPPORT_BEAM =
            BLOCKS.register("condemned_support_beam",
                () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 8.0f).sound(SoundType.METAL)));

    // --- Lighting (2) ---
    public static final DeferredHolder<Block, Block> CONDEMNED_LAMP =
            BLOCKS.register("condemned_lamp",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(1.5f, 6.0f).lightLevel(state -> 12).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_FLOODLIGHT =
            BLOCKS.register("condemned_floodlight",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(1.5f, 6.0f).lightLevel(state -> 15).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> PHASE_LANTERN =
            BLOCKS.register("phase_lantern",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.0f, 6.0f).lightLevel(state -> 12).sound(SoundType.LANTERN)));

    // --- Detail/industrial (6) ---
    public static final DeferredHolder<Block, Block> CONDEMNED_GRATE =
            BLOCKS.register("condemned_grate",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 8.0f).sound(SoundType.METAL)));

    public static final DeferredHolder<Block, IronBarsBlock> CONDEMNED_BARS_BLOCK =
            BLOCKS.register("condemned_bars_block",
                () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 8.0f)
                    .sound(SoundType.METAL).noOcclusion()));

    public static final DeferredHolder<Block, Block> CONDEMNED_VENT =
            BLOCKS.register("condemned_vent",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(3.0f, 8.0f).sound(SoundType.METAL)));

    public static final DeferredHolder<Block, Block> CONDEMNED_HAZARD_BLOCK =
            BLOCKS.register("condemned_hazard_block",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_SLAG =
            BLOCKS.register("condemned_slag",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));

    public static final DeferredHolder<Block, Block> CONDEMNED_CHAIN_BLOCK =
            BLOCKS.register("condemned_chain_block",
                () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.STONE)));

    private ModBlocks() {}
}
