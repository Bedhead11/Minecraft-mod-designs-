package com.prisonplanet.core;

import com.prisonplanet.block.CondemnedPlantBlock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Full construction and environmental palette for the Condemned dimension.
 *
 * <p>Registrations are ordered by creative inventory grouping. Building variants deliberately
 * use the vanilla shape blocks so wall/fence/bar connections and stair/slab geometry obey
 * Minecraft's standard neighbour-update behavior.</p>
 */
public final class ModExpansionBlocks {
    private static final Map<String, DeferredHolder<Block, ? extends Block>> REGISTERED = new LinkedHashMap<>();
    private static final List<String> PLANT_SOILS = List.of(
            "ashen_shale", "compacted_cinder", "fine_ash", "red_ash", "cinder_sand",
            "sulfur_sand", "black_glass_sand", "frost_ash", "powdered_char");

    static {
        // Natural stone and weathered geological strata.
        registerStone("ashen_shale");
        registerStone("charred_basalt");
        registerStone("brimstone");
        registerStone("vitrified_rock");
        registerStone("sulfur_crust");
        registerStone("frostbitten_stone");
        registerStone("obsidian_slag");
        registerStone("compacted_cinder");

        // Mineable deposits with dedicated drops and host-rock appearances.
        registerOre("glacial_shard_ore");
        registerOre("deepslate_glacial_shard_ore");
        registerOre("sulfur_ore");
        registerOre("deepslate_sulfur_ore");
        registerOre("ferric_scrap_ore");
        registerOre("deepslate_ferric_scrap_ore");
        registerOre("ember_crystal_ore");
        registerOre("deepslate_ember_crystal_ore");

        // Architectural surfaces and prison machinery cladding.
        for (String id : List.of(
                "etched_condemned_bricks", "riveted_condemned_bricks", "soot_stained_bricks",
                "frozen_condemned_bricks", "condemned_mosaic", "condemned_runed_tile",
                "prison_wall_panel", "cracked_wall_panel", "reinforced_bulkhead",
                "rusted_bulkhead", "blackened_concrete", "ash_concrete",
                "blood_rust_plate", "warning_stripe_plate", "drainage_tile",
                "riveted_plate", "vented_plate", "cage_floor", "warden_insignia_block",
                "ritual_carved_stone")) {
            registerMasonry(id);
        }
        registerPillar("scorched_pillar", SoundType.STONE);
        registerPillar("cracked_support_column", SoundType.STONE);
        registerPillar("pipe_bundle", SoundType.METAL);
        registerPillar("coolant_pipe", SoundType.METAL);

        // Dedicated textures on all shaped blocks keep each building set visually distinct.
        registerShapes("scorched_rock", () -> ModBlocks.SCORCHED_ROCK.get().defaultBlockState());
        registerShapes("condemned_brick", () -> ModBlocks.CONDEMNED_BRICKS.get().defaultBlockState());
        registerShapes("reinforced", () -> ModBlocks.CONDEMNED_REINFORCED_BLOCK.get().defaultBlockState());
        registerShapes("ashen_shale", () -> state("ashen_shale"));
        registerShapes("blackened_concrete", () -> state("blackened_concrete"));
        registerShapes("condemned_tile", () -> ModBlocks.CONDEMNED_TILE.get().defaultBlockState());

        // Connected industrial barriers: bars/panes, then fences.
        for (String id : List.of(
                "rusted_bars", "reinforced_bars", "frost_caked_bars", "hazard_bars",
                "grated_pane", "coolant_pane")) {
            register(id, () -> new IronBarsBlock(metalProperties().noOcclusion()));
        }
        for (String id : List.of("prison_fence", "rusted_fence", "reinforced_fence", "cable_fence")) {
            register(id, () -> new FenceBlock(metalProperties()));
        }

        // Light-emitting safety fixtures, braziers, and failing utility luminaires.
        registerLight("ember_lamp", 13);
        registerLight("red_warning_lamp", 10);
        registerLight("furnace_lamp", 15);
        registerLight("frozen_lantern", 12);
        registerLight("soul_beacon_lamp", 14);
        registerLight("emergency_strip_light", 11);
        registerLight("industrial_ceiling_light", 15);
        registerLight("ritual_brazier", 14);

        // Low vegetation able to survive on dimension-specific deposits.
        registerPlant("ash_thorn", 0);
        registerPlant("cinder_bloom", 4);
        registerPlant("frost_reed", 2);
        registerPlant("prison_moss", 0);
        registerPlant("ember_fungus", 7);
        registerPlant("pale_root", 0);
        registerPlant("glassweed", 3);
        registerPlant("bloodfern", 0);

        // Gravity-affected deposits that reshape caves and exposed walkways.
        registerFalling("fine_ash", 0x4A4545FF);
        registerFalling("red_ash", 0x714844FF);
        registerFalling("cinder_sand", 0x392F2BFF);
        registerFalling("sulfur_sand", 0x8B7745FF);
        registerFalling("black_glass_sand", 0x201F24FF);
        registerFalling("frost_ash", 0xA7ADB3FF);
        registerFalling("slag_gravel", 0x403A38FF);
        registerFalling("powdered_char", 0x292528FF);
    }

    private static void registerStone(String id) {
        register(id, () -> new Block(stoneProperties()));
    }

    private static void registerOre(String id) {
        register(id, () -> new Block(stoneProperties().strength(3.4F, 6.0F)));
    }

    private static void registerMasonry(String id) {
        register(id, () -> new Block(stoneProperties().strength(2.7F, 7.0F)));
    }

    private static void registerPillar(String id, SoundType sound) {
        register(id, () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops().strength(3.0F, 8.0F).sound(sound)));
    }

    private static void registerShapes(String prefix, Supplier<BlockState> base) {
        register(prefix + "_slab", () -> new SlabBlock(stoneProperties()));
        register(prefix + "_stairs", () -> new StairBlock(base.get(), stoneProperties()));
        register(prefix + "_wall", () -> new WallBlock(stoneProperties()));
    }

    private static void registerLight(String id, int level) {
        register(id, () -> new Block(metalProperties().strength(1.6F, 6.0F).lightLevel(state -> level)));
    }

    private static void registerPlant(String id, int light) {
        register(id, () -> new CondemnedPlantBlock(BlockBehaviour.Properties.of()
                .noCollission().instabreak().sound(SoundType.GRASS)
                .lightLevel(state -> light).offsetType(BlockBehaviour.OffsetType.XZ)));
    }

    private static void registerFalling(String id, int dustColor) {
        register(id, () -> new ColoredFallingBlock(new ColorRGBA(dustColor),
                BlockBehaviour.Properties.of().strength(0.55F).sound(SoundType.SAND)));
    }

    private static BlockBehaviour.Properties stoneProperties() {
        return BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metalProperties() {
        return BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops().strength(3.0F, 8.0F).sound(SoundType.METAL);
    }

    private static <T extends Block> DeferredHolder<Block, T> register(String id, Supplier<T> supplier) {
        DeferredHolder<Block, T> block = ModBlocks.BLOCKS.register(id, supplier);
        REGISTERED.put(id, block);
        return block;
    }

    public static BlockState state(String id) {
        return REGISTERED.get(id).get().defaultBlockState();
    }

    public static boolean isPlantSoil(BlockState state) {
        return PLANT_SOILS.stream().anyMatch(id -> state.is(REGISTERED.get(id).get()));
    }

    public static Map<String, DeferredHolder<Block, ? extends Block>> entries() {
        return Collections.unmodifiableMap(REGISTERED);
    }

    public static void initialize() {
        // Class initialization performs the deferred registrations.
    }

    private ModExpansionBlocks() {
    }
}
