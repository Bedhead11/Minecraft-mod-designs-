package com.prisonplanet.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;

/**
 * Persistent record of the most recently observed extended Condemned cycle time.
 */
public class PrisonPlanetSavedData extends SavedData {

    private static final String KEY_CYCLE_TICK = "cycleTick";
    private long cycleTick = 0L;

    public PrisonPlanetSavedData() {
        // Fresh instance starts at tick 0
    }

    // -------------------------------------------------------------------------
    // Factory / persistence
    // -------------------------------------------------------------------------

    public static PrisonPlanetSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        PrisonPlanetSavedData::new,
                        PrisonPlanetSavedData::load,
                        null
                ),
                "prisonplanet_cycle"
        );
    }

    public static PrisonPlanetSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PrisonPlanetSavedData data = new PrisonPlanetSavedData();
        data.cycleTick = tag.getLong(KEY_CYCLE_TICK);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong(KEY_CYCLE_TICK, cycleTick);
        return tag;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public long getCycleTick() {
        return cycleTick;
    }

    public CyclePhase getCurrentPhase() {
        return CyclePhase.fromDayTime(getVisibleDayTime());
    }

    public long getTicksIntoPhase() {
        return CyclePhase.ticksIntoPhase(getVisibleDayTime());
    }

    public boolean isHazardGracePeriod() {
        return getTicksIntoPhase() < 200L;
    }

    /**
     * Advances the cycle by one tick, wrapping at 192,000. Marks data dirty.
     */
    public void tick() {
        cycleTick = (cycleTick + 1) % CyclePhase.CYCLE_LENGTH;
        setDirty();
    }

    public long getVisibleDayTime() {
        return Math.floorMod(cycleTick, CyclePhase.CYCLE_LENGTH);
    }

    public void synchronizeToVisibleDayTime(long dayTime) {
        long nextTick = Math.floorMod(dayTime, CyclePhase.CYCLE_LENGTH);
        CyclePhase oldPhase = getCurrentPhase();
        cycleTick = nextTick;
        if (oldPhase != getCurrentPhase()) {
            setDirty();
        }
    }
}
