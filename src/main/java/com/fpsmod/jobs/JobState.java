package com.fpsmod.jobs;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Per-player jobs state. Active jobs are string ids; XP is retained per job across switches. */
public final class JobState {
    private final List<String> activeJobIds = new ArrayList<>();
    private final LinkedHashMap<String, Long> xpByJobId = new LinkedHashMap<>();

    public List<String> activeJobIds() {
        return List.copyOf(activeJobIds);
    }

    @Nullable
    public String primaryActiveJobId() {
        return activeJobIds.isEmpty() ? null : activeJobIds.get(activeJobIds.size() - 1);
    }

    public boolean isActive(String jobId) {
        return activeJobIds.contains(Job.normalizeId(jobId));
    }

    public void setActiveJobs(List<String> jobIds, int maxActiveJobs) {
        activeJobIds.clear();
        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String jobId : jobIds) {
            String normalized = Job.normalizeId(jobId);
            if (!normalized.isEmpty()) {
                ordered.add(normalized);
            }
        }
        int keep = Math.max(1, maxActiveJobs);
        List<String> deduped = new ArrayList<>(ordered);
        if (deduped.size() > keep) {
            deduped = deduped.subList(deduped.size() - keep, deduped.size());
        }
        activeJobIds.addAll(deduped);
    }

    public boolean activate(String rawJobId, int maxActiveJobs) {
        String jobId = Job.normalizeId(rawJobId);
        if (jobId.isEmpty()) {
            return false;
        }
        boolean wasActive = activeJobIds.remove(jobId);
        activeJobIds.add(jobId);
        boolean changed = !wasActive;
        int keep = Math.max(1, maxActiveJobs);
        while (activeJobIds.size() > keep) {
            activeJobIds.remove(0);
            changed = true;
        }
        return changed;
    }

    public boolean deactivate(@Nullable String rawJobId) {
        if (rawJobId == null || rawJobId.isBlank()) {
            if (activeJobIds.isEmpty()) {
                return false;
            }
            activeJobIds.clear();
            return true;
        }
        return activeJobIds.remove(Job.normalizeId(rawJobId));
    }

    public long getXp(String rawJobId) {
        return xpByJobId.getOrDefault(Job.normalizeId(rawJobId), 0L);
    }

    public long addXp(String rawJobId, long delta) {
        String jobId = Job.normalizeId(rawJobId);
        if (jobId.isEmpty()) {
            return 0L;
        }
        long prev = getXp(jobId);
        long next = Math.max(0L, prev + Math.max(0L, delta));
        xpByJobId.put(jobId, next);
        return next;
    }

    public int levelOf(String rawJobId, JobProgression progression) {
        if (progression == null) {
            return 0;
        }
        return progression.levelForXp(getXp(rawJobId));
    }

    public void setXp(String rawJobId, long amount) {
        String jobId = Job.normalizeId(rawJobId);
        if (jobId.isEmpty()) {
            return;
        }
        xpByJobId.put(jobId, Math.max(0L, amount));
    }

    public Map<String, Long> snapshotXp() {
        return new LinkedHashMap<>(xpByJobId);
    }
}
