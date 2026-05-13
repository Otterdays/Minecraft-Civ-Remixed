package com.fpsmod.jobs;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** Per-player jobs state. One active job slot; XP retained per job across switches. */
public final class JobState {
    @Nullable
    private Job active;
    private final EnumMap<Job, Long> xp = new EnumMap<>(Job.class);

    @Nullable
    public Job active() { return active; }

    public void setActive(@Nullable Job job) { this.active = job; }

    public long getXp(Job job) {
        return xp.getOrDefault(job, 0L);
    }

    public long addXp(Job job, long delta) {
        long prev = getXp(job);
        long next = Math.max(0L, prev + Math.max(0L, delta));
        xp.put(job, next);
        return next;
    }

    public int levelOf(Job job) {
        return JobsConfig.levelForXp(getXp(job));
    }

    public Map<Job, Long> snapshotXp() {
        return new EnumMap<>(xp);
    }
}
