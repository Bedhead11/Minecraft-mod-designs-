package com.prisonplanet.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.prisonplanet.core.ModBlocks;
import com.prisonplanet.core.ModExpansionBlocks;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Generates a natural, ash-covered surface over a world-spanning prison network.
 *
 * <p>The structure layout is derived from seeded districts rather than chunk-local
 * randomness. Every district hub is linked to its east and south neighbours on
 * each deck, so generated chunks meet cleanly and all local rooms reach a
 * continuous network.</p>
 */
public class PrisonChunkGenerator extends ChunkGenerator {
    public static final MapCodec<PrisonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, PrisonChunkGenerator::new)
    );

    private static final ResourceLocation TERRAIN_NOISE =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "terrain_shape");
    private static final ResourceLocation DAMAGE_NOISE =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "surface_damage");
    private static final ResourceLocation STONE_NOISE =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "surface_stone");
    private static final ResourceLocation DISTRICT_RANDOM =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "structure_district");
    private static final ResourceLocation CATACOMB_NOISE =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "catacomb_cavern");

    private static final int MIN_Y = -64;
    private static final int GEN_DEPTH = 384;
    private static final int DISTRICT_SIZE = 96;
    private static final int STRUCTURE_BOTTOM = 8;
    private static final int STRUCTURE_TOP = 97;
    private static final int UPPER_LANDING = 76;
    private static final int[] DECK_BASES = {10, 21, 32, 43, 54, 65, 76, 87};

    public PrisonChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        Map<Long, District> districtCache = new HashMap<>();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                ColumnPlan plan = planColumn(randomState, x, z, districtCache);
                for (int y = MIN_Y; y <= plan.surfaceY(); y++) {
                    chunk.setBlockState(
                            new BlockPos(x, y, z),
                            selectGeneratedState(x, y, z, plan),
                            false);
                }
                BlockState feature = surfaceFeature(x, z, plan);
                if (!feature.isAir() && plan.surfaceY() + 1 < chunk.getMaxBuildHeight()) {
                    chunk.setBlockState(new BlockPos(x, plan.surfaceY() + 1, z), feature, false);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(
            WorldGenRegion level,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving step) {
        // Structural voids and catacomb channels are evaluated in fillFromNoise.
    }

    @Override
    public void buildSurface(
            WorldGenRegion level,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk) {
        // Surface composition is integrated with the broken prison roof.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Future landmark content may supply phase-gated population rules.
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            net.minecraft.world.level.levelgen.Heightmap.Types heightmapType,
            LevelHeightAccessor level,
            RandomState randomState) {
        ColumnPlan plan = planColumn(randomState, x, z);
        for (int y = plan.surfaceY(); y >= MIN_Y; y--) {
            if (!selectGeneratedState(x, y, z, plan).isAir()) {
                return y + 1;
            }
        }
        return MIN_Y;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        ColumnPlan plan = planColumn(randomState, x, z);
        BlockState[] states = new BlockState[plan.surfaceY() - MIN_Y + 1];
        for (int i = 0; i < states.length; i++) {
            int y = MIN_Y + i;
            states[i] = selectGeneratedState(x, y, z, plan);
        }
        return new NoiseColumn(MIN_Y, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Prison Planet: seeded prison districts beneath ashen uplands");
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome,
            StructureManager structureManager,
            MobCategory category,
            BlockPos pos) {
        return biome.value().getMobSettings().getMobs(category);
    }

    private static ColumnPlan planColumn(RandomState randomState, int x, int z) {
        return planColumn(randomState, x, z, new HashMap<>());
    }

    private static ColumnPlan planColumn(RandomState randomState, int x, int z, Map<Long, District> districtCache) {
        int surfaceY = surfaceHeight(randomState, x, z);
        DistrictGrid districts = districtGrid(randomState, x, z, districtCache);
        int[] ceilings = new int[DECK_BASES.length];
        for (int deck = 0; deck < DECK_BASES.length; deck++) {
            ceilings[deck] = interiorCeiling(districts, x, z, deck);
        }
        int openingBottom = skyOpeningBottom(districts, x, z);
        boolean shaft = isShaftColumn(districts, x, z);
        boolean roofPanel = valueNoise(randomState, DAMAGE_NOISE, x, z, 24) > -0.12
                && openingBottom == Integer.MAX_VALUE;
        boolean stonePocket = valueNoise(randomState, STONE_NOISE, x, z, 31) > 0.48
                && !roofPanel;
        int paletteSalt = districts.get(0, 0).paletteSalt();
        CatacombPlan catacombs = catacombPlan(randomState, districts, x, z);
        return new ColumnPlan(surfaceY, openingBottom, shaft, roofPanel, stonePocket,
                paletteSalt, ceilings, districts, catacombs);
    }

    private static BlockState selectGeneratedState(int x, int y, int z, ColumnPlan plan) {
        if (isRailing(x, y, z, plan)) {
            return ModBlocks.CONDEMNED_BARS_BLOCK.get().defaultBlockState();
        }
        if (plan.isCarved(y)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (isStructureMass(y, plan)) {
            return structureBlock(x, y, z, plan.paletteSalt());
        }
        return terrainBlock(x, y, z, plan);
    }

    private static boolean isStructureMass(int y, ColumnPlan plan) {
        return y >= STRUCTURE_BOTTOM && y <= Math.min(STRUCTURE_TOP, plan.surfaceY() - 3);
    }

    private static BlockState terrainBlock(int x, int y, int z, ColumnPlan plan) {
        if (y == plan.surfaceY()) {
            if (plan.roofPanel()) {
                return structureBlock(x, y, z, plan.paletteSalt());
            }
            if (plan.stonePocket()) {
                return vanillaStonePocket(x, z, plan.paletteSalt());
            }
            return surfaceDeposit(x, z, plan.paletteSalt());
        }
        if (y >= plan.surfaceY() - 2) {
            return scatter(x, y, z, plan.paletteSalt(), 8) == 0
                    ? ModExpansionBlocks.state("compacted_cinder")
                    : ModBlocks.ASHEN_SEDIMENT.get().defaultBlockState();
        }
        if (y < 0) {
            return deepGeology(x, y, z, plan.paletteSalt());
        }
        return middleGeology(x, y, z, plan.paletteSalt());
    }

    private static BlockState structureBlock(int x, int y, int z, int salt) {
        int texture = Math.floorMod(salt + x * 31 + y * 17 + z * 13, 32);
        if (texture < 3) {
            return ModBlocks.CONDEMNED_REINFORCED_BLOCK.get().defaultBlockState();
        }
        if (texture < 6) {
            return ModExpansionBlocks.state("cracked_wall_panel");
        }
        if (texture < 8) {
            return ModExpansionBlocks.state("soot_stained_bricks");
        }
        if (texture == 8) {
            return ModBlocks.CONDEMNED_METAL_PLATE.get().defaultBlockState();
        }
        if (texture == 10) {
            return ModExpansionBlocks.state("riveted_plate");
        }
        if (texture == 11) {
            return ModExpansionBlocks.state("reinforced_bulkhead");
        }
        return ModBlocks.CONDEMNED_BRICKS.get().defaultBlockState();
    }

    private static BlockState surfaceDeposit(int x, int z, int salt) {
        return switch (scatter(x, 0, z, salt, 21)) {
            case 0, 1 -> ModExpansionBlocks.state("frost_ash");
            case 2 -> ModExpansionBlocks.state("sulfur_sand");
            case 3, 4, 5 -> ModExpansionBlocks.state("cinder_sand");
            case 6 -> ModExpansionBlocks.state("red_ash");
            default -> ModExpansionBlocks.state("fine_ash");
        };
    }

    private static BlockState middleGeology(int x, int y, int z, int salt) {
        int sample = scatter(x, y, z, salt, 547);
        if (sample == 0) {
            return ModExpansionBlocks.state("sulfur_ore");
        }
        if (sample == 1) {
            return ModExpansionBlocks.state("ferric_scrap_ore");
        }
        if (sample == 2) {
            return ModExpansionBlocks.state("glacial_shard_ore");
        }
        if (sample == 3) {
            return ModExpansionBlocks.state("ember_crystal_ore");
        }
        return switch (Math.floorMod(salt + x / 7 + z / 9, 7)) {
            case 0 -> ModExpansionBlocks.state("ashen_shale");
            case 1 -> ModExpansionBlocks.state("charred_basalt");
            case 2 -> ModExpansionBlocks.state("vitrified_rock");
            case 3 -> ModExpansionBlocks.state("brimstone");
            default -> ModBlocks.SCORCHED_ROCK.get().defaultBlockState();
        };
    }

    private static BlockState deepGeology(int x, int y, int z, int salt) {
        int sample = scatter(x, y, z, salt, 463);
        if (sample == 0) {
            return ModExpansionBlocks.state("deepslate_glacial_shard_ore");
        }
        if (sample == 1) {
            return ModExpansionBlocks.state("deepslate_ember_crystal_ore");
        }
        if (sample == 2) {
            return ModExpansionBlocks.state("deepslate_ferric_scrap_ore");
        }
        if (sample == 3) {
            return ModExpansionBlocks.state("deepslate_sulfur_ore");
        }
        return Math.floorMod(salt + x / 11 + z / 13, 5) == 0
                ? ModExpansionBlocks.state("obsidian_slag")
                : ModBlocks.CONDEMNED_DEEPSLATE.get().defaultBlockState();
    }

    private static BlockState surfaceFeature(int x, int z, ColumnPlan plan) {
        if (plan.roofPanel() || plan.stonePocket()) {
            return Blocks.AIR.defaultBlockState();
        }
        int feature = scatter(x, plan.surfaceY(), z, plan.paletteSalt(), 113);
        return switch (feature) {
            case 0 -> ModExpansionBlocks.state("ash_thorn");
            case 1 -> ModExpansionBlocks.state("cinder_bloom");
            case 2 -> ModExpansionBlocks.state("frost_reed");
            case 3 -> ModExpansionBlocks.state("prison_moss");
            case 4 -> ModExpansionBlocks.state("ember_fungus");
            case 5 -> ModExpansionBlocks.state("pale_root");
            case 6 -> ModExpansionBlocks.state("glassweed");
            case 7 -> ModExpansionBlocks.state("bloodfern");
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    private static int scatter(int x, int y, int z, int salt, int bound) {
        long value = ((long) x * 341873128712L)
                ^ ((long) y * 42317861L)
                ^ ((long) z * 132897987541L)
                ^ salt;
        value ^= value >>> 13;
        value *= 1274126177L;
        return Math.floorMod(value, bound);
    }

    private static BlockState vanillaStonePocket(int x, int z, int salt) {
        return switch (Math.floorMod(salt + x * 19 + z * 37, 5)) {
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            case 2 -> Blocks.GRANITE.defaultBlockState();
            case 3 -> Blocks.DIORITE.defaultBlockState();
            case 4 -> Blocks.ANDESITE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    private static int surfaceHeight(RandomState randomState, int x, int z) {
        double uplands = valueNoise(randomState, TERRAIN_NOISE, x, z, 192) * 21.0;
        double valleys = valueNoise(randomState, TERRAIN_NOISE, x + 8000, z - 8000, 72) * 12.0;
        double detail = valueNoise(randomState, TERRAIN_NOISE, x - 15000, z + 6000, 28) * 4.0;
        return 106 + (int) Math.round(uplands + valleys + detail);
    }

    private static int interiorCeiling(DistrictGrid districts, int x, int z, int deck) {
        int height = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                District current = districts.get(dx, dz);
                height = Math.max(height, hubAndRoomHeight(current, x, z, deck));
                height = Math.max(height, linkHeight(current,
                        districts.get(dx + 1, dz), x, z, deck, 0));
                height = Math.max(height, linkHeight(current,
                        districts.get(dx, dz + 1), x, z, deck, 1));
            }
        }
        return height == 0 ? Integer.MIN_VALUE : DECK_BASES[deck] + height;
    }

    private static int hubAndRoomHeight(District district, int x, int z, int deck) {
        if (inRectangle(x, z, district.centerX() - 6, district.centerZ() - 6,
                district.centerX() + 6, district.centerZ() + 6)) {
            return deck >= 4 ? 6 : 5;
        }
        RandomSource random = district.deckRandom(deck);
        int roomWidth = 6 + random.nextInt(7);
        int roomDepth = 5 + random.nextInt(8);
        int direction = random.nextInt(4);
        int minX = district.centerX() - roomWidth / 2;
        int maxX = minX + roomWidth;
        int minZ = district.centerZ() - roomDepth / 2;
        int maxZ = minZ + roomDepth;
        if (direction == 0) {
            minX = district.centerX() + 4;
            maxX = minX + roomWidth;
        } else if (direction == 1) {
            maxX = district.centerX() - 4;
            minX = maxX - roomWidth;
        } else if (direction == 2) {
            minZ = district.centerZ() + 4;
            maxZ = minZ + roomDepth;
        } else {
            maxZ = district.centerZ() - 4;
            minZ = maxZ - roomDepth;
        }
        if (inRectangle(x, z, minX, minZ, maxX, maxZ)) {
            return 4 + random.nextInt(4);
        }
        return 0;
    }

    private static int linkHeight(District from, District to, int x, int z, int deck, int directionSalt) {
        RandomSource random = from.deckRandom(deck * 3 + directionSalt + 40);
        boolean horizontalFirst = random.nextBoolean();
        int halfWidth = 1 + random.nextInt(2);
        int height = 4 + (deck >= 4 && random.nextBoolean() ? 1 : 0);
        if (containsManhattanRoute(x, z, from.centerX(), from.centerZ(), to.centerX(), to.centerZ(),
                halfWidth, horizontalFirst)) {
            height = Math.max(height, 4);
        } else {
            height = 0;
        }
        boolean grandTunnel = deck >= 3 && deck <= 5 && from.grandDeck() == deck
                && from.grandDirection() == directionSalt;
        if (grandTunnel && containsManhattanRoute(x, z, from.centerX(), from.centerZ(),
                to.centerX(), to.centerZ(), 5, horizontalFirst)) {
            return 18;
        }
        return height;
    }

    private static boolean isShaftColumn(DistrictGrid districts, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                District district = districts.get(dx, dz);
                if (squaredDistance(x, z, district.centerX(), district.centerZ())
                        <= district.shaftRadius() * district.shaftRadius()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int skyOpeningBottom(DistrictGrid districts, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                District district = districts.get(dx, dz);
                if (!district.openToSky()) {
                    continue;
                }
                int radius = district.openingRadius();
                if (squaredDistance(x, z, district.centerX(), district.centerZ()) <= radius * radius) {
                    return district.deepDrop() ? 22 : UPPER_LANDING + 1;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private static boolean isRailing(int x, int y, int z, ColumnPlan plan) {
        if (y != UPPER_LANDING + 1 || plan.openingBottom() != Integer.MAX_VALUE) {
            return false;
        }
        return skyOpeningBottom(plan.districts(), x + 1, z) != Integer.MAX_VALUE
                || skyOpeningBottom(plan.districts(), x - 1, z) != Integer.MAX_VALUE
                || skyOpeningBottom(plan.districts(), x, z + 1) != Integer.MAX_VALUE
                || skyOpeningBottom(plan.districts(), x, z - 1) != Integer.MAX_VALUE;
    }

    private static CatacombPlan catacombPlan(
            RandomState randomState, DistrictGrid districts, int x, int z) {
        double ceiling = -27.0 + valueNoise(randomState, CATACOMB_NOISE, x, z, 54) * 10.0;
        double radius = 2.0 + Math.max(0.0, valueNoise(randomState, CATACOMB_NOISE, x + 5000, z, 30)) * 5.0;
        boolean cavern = valueNoise(randomState, CATACOMB_NOISE, x, z, 24) > -0.34;
        boolean route = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                District current = districts.get(dx, dz);
                if (containsManhattanRoute(x, z, current.centerX(), current.centerZ(),
                        districts.get(dx + 1, dz).centerX(),
                        districts.get(dx + 1, dz).centerZ(), 2, true)
                        || containsManhattanRoute(x, z, current.centerX(), current.centerZ(),
                        districts.get(dx, dz + 1).centerX(),
                        districts.get(dx, dz + 1).centerZ(), 2, false)) {
                    route = true;
                    break;
                }
            }
            if (route) {
                break;
            }
        }
        return new CatacombPlan(ceiling, radius, cavern, route);
    }

    private static District district(RandomState randomState, int gridX, int gridZ) {
        RandomSource random = randomState.getOrCreateRandomFactory(DISTRICT_RANDOM).at(gridX, 0, gridZ);
        int centerX = gridX * DISTRICT_SIZE + 25 + random.nextInt(46);
        int centerZ = gridZ * DISTRICT_SIZE + 25 + random.nextInt(46);
        int shaftRadius = 2 + random.nextInt(4);
        boolean openToSky = random.nextInt(5) <= 1;
        int openingRadius = 6 + random.nextInt(8);
        boolean deepDrop = openToSky && random.nextInt(5) == 0;
        int grandDeck = 3 + random.nextInt(3);
        int grandDirection = random.nextInt(2);
        return new District(gridX, gridZ, centerX, centerZ, shaftRadius, openToSky,
                openingRadius, deepDrop, grandDeck, grandDirection, random.nextInt());
    }

    private static DistrictGrid districtGrid(
            RandomState randomState, int x, int z, Map<Long, District> districtCache) {
        int gridX = Math.floorDiv(x, DISTRICT_SIZE);
        int gridZ = Math.floorDiv(z, DISTRICT_SIZE);
        District[][] cells = new District[4][4];
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                int districtX = gridX + dx;
                int districtZ = gridZ + dz;
                cells[dx + 1][dz + 1] = districtCache.computeIfAbsent(
                        ChunkPos.asLong(districtX, districtZ),
                        ignored -> district(randomState, districtX, districtZ)
                );
            }
        }
        return new DistrictGrid(cells);
    }

    private static double valueNoise(
            RandomState randomState, ResourceLocation stream, int x, int z, int scale) {
        double scaledX = (double) x / scale;
        double scaledZ = (double) z / scale;
        int x0 = (int) Math.floor(scaledX);
        int z0 = (int) Math.floor(scaledZ);
        double fractionX = fade(scaledX - x0);
        double fractionZ = fade(scaledZ - z0);
        double lower = lerp(fractionX,
                randomValue(randomState, stream, x0, z0),
                randomValue(randomState, stream, x0 + 1, z0));
        double upper = lerp(fractionX,
                randomValue(randomState, stream, x0, z0 + 1),
                randomValue(randomState, stream, x0 + 1, z0 + 1));
        return lerp(fractionZ, lower, upper);
    }

    private static double randomValue(RandomState randomState, ResourceLocation stream, int x, int z) {
        return randomState.getOrCreateRandomFactory(stream).at(x, 0, z).nextDouble() * 2.0 - 1.0;
    }

    private static double fade(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double lerp(double amount, double start, double end) {
        return start + amount * (end - start);
    }

    private static boolean containsManhattanRoute(
            int x, int z, int startX, int startZ, int endX, int endZ, int radius, boolean horizontalFirst) {
        if (horizontalFirst) {
            return inRectangle(x, z, Math.min(startX, endX), startZ - radius,
                    Math.max(startX, endX), startZ + radius)
                    || inRectangle(x, z, endX - radius, Math.min(startZ, endZ),
                    endX + radius, Math.max(startZ, endZ));
        }
        return inRectangle(x, z, startX - radius, Math.min(startZ, endZ),
                startX + radius, Math.max(startZ, endZ))
                || inRectangle(x, z, Math.min(startX, endX), endZ - radius,
                Math.max(startX, endX), endZ + radius);
    }

    private static boolean inRectangle(int x, int z, int minX, int minZ, int maxX, int maxZ) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private static int squaredDistance(int x, int z, int targetX, int targetZ) {
        int dx = x - targetX;
        int dz = z - targetZ;
        return dx * dx + dz * dz;
    }

    private record ColumnPlan(
            int surfaceY,
            int openingBottom,
            boolean shaft,
            boolean roofPanel,
            boolean stonePocket,
            int paletteSalt,
            int[] deckCeilings,
            DistrictGrid districts,
            CatacombPlan catacombs) {
        private boolean isCarved(int y) {
            if (openingBottom != Integer.MAX_VALUE && y >= openingBottom && y <= surfaceY) {
                return true;
            }
            if (shaft && y >= DECK_BASES[0] + 1 && y <= STRUCTURE_TOP) {
                return true;
            }
            for (int deck = 0; deck < DECK_BASES.length; deck++) {
                if (y >= DECK_BASES[deck] + 1 && y <= deckCeilings[deck]) {
                    return true;
                }
            }
            return catacombs.isCarved(y);
        }
    }

    private record CatacombPlan(double ceiling, double radius, boolean cavern, boolean route) {
        private boolean isCarved(int y) {
            if (y >= 0 || y < -58) {
                return false;
            }
            if (cavern && Math.abs(y - ceiling) <= radius) {
                return true;
            }
            return route && y >= -43 && y <= -38;
        }
    }

    private record DistrictGrid(District[][] cells) {
        private District get(int relativeX, int relativeZ) {
            return cells[relativeX + 1][relativeZ + 1];
        }
    }

    private record District(
            int gridX,
            int gridZ,
            int centerX,
            int centerZ,
            int shaftRadius,
            boolean openToSky,
            int openingRadius,
            boolean deepDrop,
            int grandDeck,
            int grandDirection,
            int paletteSalt) {
        private RandomSource deckRandom(int deck) {
            return RandomSource.create((long) paletteSalt * 341873128712L + deck * 132897987541L);
        }
    }
}
