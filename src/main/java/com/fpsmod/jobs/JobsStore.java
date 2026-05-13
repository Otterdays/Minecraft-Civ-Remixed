package com.fpsmod.jobs;

import java.util.Map;
import java.util.UUID;

public interface JobsStore {
    JobsLedger load();
    void save(Map<UUID, JobState> states, Map<UUID, String> displayHints);
}
