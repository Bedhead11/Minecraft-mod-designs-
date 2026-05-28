package com.prisonplanet.dimension;

/**
 * Represents equal-length environmental phases in the extended Condemned day.
 */
public enum CyclePhase {
    DAY("Day"),
    SUNSET("Sunset"),
    NIGHT("Night"),
    SUNRISE("Sunrise");

    public static final long PHASE_LENGTH = 48000L;
    public static final long CYCLE_LENGTH = PHASE_LENGTH * 4L;

    private final String displayName;

    CyclePhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CyclePhase fromDayTime(long dayTime) {
        long cycleTime = Math.floorMod(dayTime, CYCLE_LENGTH);
        if (cycleTime < PHASE_LENGTH) {
            return DAY;
        }
        if (cycleTime < PHASE_LENGTH * 2L) {
            return SUNSET;
        }
        if (cycleTime < PHASE_LENGTH * 3L) {
            return NIGHT;
        }
        return SUNRISE;
    }

    public static long ticksIntoPhase(long dayTime) {
        long cycleTime = Math.floorMod(dayTime, CYCLE_LENGTH);
        long phaseStart = switch (fromDayTime(cycleTime)) {
            case DAY -> 0L;
            case SUNSET -> PHASE_LENGTH;
            case NIGHT -> PHASE_LENGTH * 2L;
            case SUNRISE -> PHASE_LENGTH * 3L;
        };
        return cycleTime - phaseStart;
    }

    /**
     * Supplies the celestial angle used by vanilla skylight calculations.
     *
     * <p>Day and night intentionally hold their peak light states while sunset
     * and sunrise transition across their full 48,000-tick windows.</p>
     */
    public static float skylightTimeOfDay(long dayTime) {
        float phaseProgress = (float) ticksIntoPhase(dayTime) / (float) PHASE_LENGTH;
        return switch (fromDayTime(dayTime)) {
            case DAY -> 0.0F;
            case SUNSET -> phaseProgress * 0.5F;
            case NIGHT -> 0.5F;
            case SUNRISE -> 0.5F + phaseProgress * 0.5F;
        };
    }

    /**
     * Returns true if this phase has active environmental hazards (day heat / night freeze).
     */
    public boolean isHazardActive() {
        return this == DAY || this == NIGHT;
    }
}
