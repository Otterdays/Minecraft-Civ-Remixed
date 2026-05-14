package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-job progression model. Supports either explicit level thresholds or the legacy power curve.
 */
public final class JobProgression {
    public int maxLevel = 50;
    public long xpPerEvent = 5L;
    public double xpBase = 100.0D;
    public double xpExponent = 1.5D;
    public List<Long> levelThresholds = new ArrayList<>();

    public static JobProgression defaults() {
        JobProgression progression = new JobProgression();
        progression.sanitize(null);
        return progression;
    }

    public void sanitize(JobProgression defaults) {
        JobProgression fallback = defaults == null ? null : defaults;
        if (xpPerEvent < 0L) {
            xpPerEvent = fallback == null ? 5L : fallback.xpPerEvent;
        }
        if (!(xpBase > 0.0D)) {
            xpBase = fallback == null ? 100.0D : fallback.xpBase;
        }
        if (!(xpExponent > 0.0D)) {
            xpExponent = fallback == null ? 1.5D : fallback.xpExponent;
        }
        levelThresholds = sanitizeThresholds(levelThresholds);
        if (!levelThresholds.isEmpty()) {
            maxLevel = levelThresholds.size();
        } else if (maxLevel < 1) {
            maxLevel = fallback == null ? 50 : fallback.maxLevel;
        }
    }

    public long xpForLevel(int level) {
        if (level <= 0) {
            return 0L;
        }
        if (!levelThresholds.isEmpty()) {
            int idx = Math.min(levelThresholds.size(), level) - 1;
            return levelThresholds.get(idx);
        }
        return (long) Math.ceil(xpBase * Math.pow(level, xpExponent));
    }

    public int levelForXp(long xp) {
        if (xp <= 0L) {
            return 0;
        }
        for (int lvl = maxLevel; lvl >= 1; lvl--) {
            if (xp >= xpForLevel(lvl)) {
                return lvl;
            }
        }
        return 0;
    }

    public boolean isMaxLevel(int level) {
        return level >= maxLevel;
    }

    private static List<Long> sanitizeThresholds(List<Long> raw) {
        List<Long> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        long prev = 0L;
        for (Long value : raw) {
            if (value == null) {
                continue;
            }
            long next = Math.max(prev + 1L, value);
            out.add(next);
            prev = next;
        }
        return out;
    }
}
