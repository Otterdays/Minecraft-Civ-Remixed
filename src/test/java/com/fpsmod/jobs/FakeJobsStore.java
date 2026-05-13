package com.fpsmod.jobs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Test double: in-memory job states + hints; tracks save call count. */
public final class FakeJobsStore implements JobsStore {
    final Map<UUID, JobState> backingStates;
    final Map<UUID, String> backingHints;
    int saveCount;

    public FakeJobsStore(Map<UUID, JobState> initial) {
        this.backingStates = new HashMap<>(initial);
        this.backingHints = new HashMap<>();
    }

    @Override
    public JobsLedger load() {
        return new JobsLedger(backingStates, backingHints);
    }

    @Override
    public void save(Map<UUID, JobState> states, Map<UUID, String> displayHints) {
        saveCount++;
        backingStates.clear();
        backingStates.putAll(states);
        backingHints.clear();
        backingHints.putAll(displayHints);
    }
}
