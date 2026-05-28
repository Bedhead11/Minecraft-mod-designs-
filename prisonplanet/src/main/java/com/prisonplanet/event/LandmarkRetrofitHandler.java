package com.prisonplanet.event;

import com.prisonplanet.core.ModDimensions;
import com.prisonplanet.dimension.LandmarkRetrofitSavedData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Retrofits landmarks into chunks generated before landmark worldgen was added.
 * New chunks continue to use the data-driven structure sets.
 */
@EventBusSubscriber(modid = "prisonplanet", bus = EventBusSubscriber.Bus.GAME)
public final class LandmarkRetrofitHandler {
    private static final ResourceLocation SAFE_HOUSE =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "safe_house");
    private static final ResourceLocation WAYSTONE_1 =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "waystone1");
    private static final ResourceLocation WAYSTONE_2 =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "waystone2");
    private static final ResourceLocation RUSTWARDEN_KEEP =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "rustwarden_keep");

    private static final long SAFE_HOUSE_SALT = 54179320L;
    private static final long WAYSTONE_SALT = 54179321L;
    private static final long RUSTWARDEN_KEEP_SALT = 54179322L;
    private static final RandomSpreadStructurePlacement SAFE_HOUSE_PLACEMENT =
            new RandomSpreadStructurePlacement(7, 2, RandomSpreadType.LINEAR, (int) SAFE_HOUSE_SALT);
    private static final RandomSpreadStructurePlacement WAYSTONE_PLACEMENT =
            new RandomSpreadStructurePlacement(32, 12, RandomSpreadType.LINEAR, (int) WAYSTONE_SALT);
    private static final RandomSpreadStructurePlacement RUSTWARDEN_KEEP_PLACEMENT =
            new RandomSpreadStructurePlacement(64, 24, RandomSpreadType.LINEAR, (int) RUSTWARDEN_KEEP_SALT);

    private LandmarkRetrofitHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk()
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.THE_CONDEMNED_KEY)) {
            return;
        }

        ChunkPos chunkPos = event.getChunk().getPos();
        level.getServer().execute(() -> retrofitLoadedChunk(level, chunkPos));
    }

    private static void retrofitLoadedChunk(ServerLevel level, ChunkPos chunkPos) {
        if (!level.hasChunk(chunkPos.x, chunkPos.z)
                || !LandmarkRetrofitSavedData.getOrCreate(level).markProcessed(chunkPos)) {
            return;
        }

        if (isSelectedAnchor(level, chunkPos, SAFE_HOUSE_PLACEMENT)) {
            RandomSource selection = selectionRandom(level, chunkPos, SAFE_HOUSE_SALT);
            boolean underground = selection.nextInt(5) < 2;
            placeLandmark(level, chunkPos, SAFE_HOUSE, SAFE_HOUSE_SALT,
                    underground ? 4 : 0, underground ? 20 : 0);
        }
        if (isSelectedAnchor(level, chunkPos, WAYSTONE_PLACEMENT)) {
            ResourceLocation waystone = selectionRandom(level, chunkPos, WAYSTONE_SALT).nextBoolean()
                    ? WAYSTONE_1
                    : WAYSTONE_2;
            placeLandmark(level, chunkPos, waystone, WAYSTONE_SALT, 0, 0);
        }
        if (isSelectedAnchor(level, chunkPos, RUSTWARDEN_KEEP_PLACEMENT)) {
            placeLandmark(level, chunkPos, RUSTWARDEN_KEEP, RUSTWARDEN_KEEP_SALT, 1, 20);
        }
    }

    private static boolean isSelectedAnchor(
            ServerLevel level,
            ChunkPos chunkPos,
            RandomSpreadStructurePlacement placement
    ) {
        return placement.getPotentialStructureChunk(level.getSeed(), chunkPos.x, chunkPos.z).equals(chunkPos);
    }

    private static void placeLandmark(
            ServerLevel level,
            ChunkPos chunkPos,
            ResourceLocation templateId,
            long salt,
            int minimumDepth,
            int maximumDepth
    ) {
        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(templateId);
        if (optionalTemplate.isEmpty()) {
            return;
        }

        RandomSource random = selectionRandom(level, chunkPos, salt);
        Rotation rotation = Rotation.getRandom(random);
        StructureTemplate template = optionalTemplate.get();
        Vec3i size = template.getSize(rotation);
        int centerX = chunkPos.getMinBlockX() + 8;
        int centerZ = chunkPos.getMinBlockZ() + 8;
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, centerX, centerZ) - 1;
        int depth = maximumDepth == 0 ? 0 : minimumDepth + random.nextInt(maximumDepth - minimumDepth + 1);
        BlockPos origin = new BlockPos(
                centerX - size.getX() / 2,
                surfaceY - depth,
                centerZ - size.getZ() / 2
        );
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        template.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_ALL);
    }

    private static RandomSource selectionRandom(ServerLevel level, ChunkPos chunkPos, long salt) {
        return RandomSource.create(level.getSeed() ^ chunkPos.toLong() ^ salt);
    }
}
