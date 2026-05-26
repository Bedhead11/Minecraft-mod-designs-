package com.prisonplanet.dimension;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public class PhaseTransitionEvent extends Event {
    private final ServerLevel level;
    private final CyclePhase previousPhase;
    private final CyclePhase phase;

    public PhaseTransitionEvent(ServerLevel level, CyclePhase previousPhase, CyclePhase phase) {
        this.level = level;
        this.previousPhase = previousPhase;
        this.phase = phase;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public CyclePhase getPreviousPhase() {
        return previousPhase;
    }

    public CyclePhase getPhase() {
        return phase;
    }
}
