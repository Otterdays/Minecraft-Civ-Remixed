package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-job progression model. Supports either explicit level thresholds or the legacy power curve.
 */
public final class JobProgression {
    private static final List<Long> STARTER_LEVEL_THRESHOLDS = List.of(
        20L, 50L, 90L, 140L, 200L, 270L, 350L, 440L, 540L, 650L,
        775L, 915L, 1070L, 1240L, 1430L, 1640L, 1870L, 2120L, 2395L, 2695L,
        3020L, 3370L, 3745L, 4145L, 4575L, 5035L, 5525L, 6045L, 6595L, 7175L,
        7785L, 8425L, 9095L, 9795L, 10525L, 11285L, 12075L, 12895L, 13745L, 14625L
    );

    public int maxLevel = 40;
    public long xpPerEvent = 5L;
    public double xpBase = 80.0D;
    public double xpExponent = 1.45D;
    public List<Long> levelThresholds = defaultThresholds();

    public static JobProgression defaults() {
        JobProgression progression = new JobProgression();
        progression.sanitize(null);
        return progression;
    }

    public static List<Long> defaultThresholds() {
        return new ArrayList<>(STARTER_LEVEL_THRESHOLDS);
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
