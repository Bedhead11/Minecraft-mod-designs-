package com.prisonplanet.dimension;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Records legacy Condemned chunks examined for landmark retrofit placement.
 */
public class LandmarkRetrofitSavedData extends SavedData {
    private static final String DATA_ID = "prisonplanet_landmark_retrofit_0_5_7";
    private static final String KEY_PROCESSED_CHUNKS = "processedChunks";

    private final Set<Long> processedChunks = new HashSet<>();

    public static LandmarkRetrofitSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        LandmarkRetrofitSavedData::new,
                        LandmarkRetrofitSavedData::load,
                        null
                ),
                DATA_ID
        );
    }

    public static LandmarkRetrofitSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandmarkRetrofitSavedData data = new LandmarkRetrofitSavedData();
        for (long chunk : tag.getLongArray(KEY_PROCESSED_CHUNKS)) {
            data.processedChunks.add(chunk);
        }
        return data;
    }

    public boolean markProcessed(ChunkPos chunkPos) {
        if (!processedChunks.add(chunkPos.toLong())) {
            return false;
        }
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray(KEY_PROCESSED_CHUNKS, processedChunks.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
