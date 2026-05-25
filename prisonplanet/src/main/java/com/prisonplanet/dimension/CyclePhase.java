package com.prisonplanet.dimension;

/**
 * Represents the four phases of the Prison Planet day cycle.
 * Total cycle length: 192,000 ticks (each phase = 48,000 ticks).
 */
public enum CyclePhase {

    DAY("Day"),
    SUNSET("Sunset"),
    NIGHT("Night"),
    SUNRISE("Sunrise");

    private final String displayName;

    CyclePhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the CyclePhase corresponding to the given cycle tick.
     * Total cycle = 192,000 ticks; each phase = 48,000 ticks.
     */
    public static CyclePhase fromTick(long cycleTick) {
        int phase = (int) ((cycleTick % 192000L) / 48000L);
        return values()[phase];
    }

    /**
     * Returns true if this phase has active environmental hazards (day heat / night freeze).
     */
    public boolean isHazardActive() {
        return this == DAY || this == NIGHT;
    }
}
