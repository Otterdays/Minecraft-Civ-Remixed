package com.fpsmod.jobs;

import java.util.Map;
import java.util.UUID;

/** Serialization snapshot: per-player state plus operator-visible name hints. */
public record JobsLedger(Map<UUID, JobState> states, Map<UUID, String> displayHints) {
}
