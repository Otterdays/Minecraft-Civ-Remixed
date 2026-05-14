package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Level-aware reward and XP modifiers.
 */
public final class JobBoosts {
    public double moneyMultiplier = 1.0D;
    public List<JobLevelDouble> moneyMultiplierByLevel = new ArrayList<>();
    public long moneyFlatBonus = 0L;
    public List<JobLevelLong> moneyFlatBonusByLevel = new ArrayList<>();
    public double xpMultiplier = 1.0D;
    public List<JobLevelDouble> xpMultiplierByLevel = new ArrayList<>();
    public long xpFlatBonus = 0L;
    public List<JobLevelLong> xpFlatBonusByLevel = new ArrayList<>();

    public static JobBoosts defaults() {
        JobBoosts boosts = new JobBoosts();
        boosts.sanitize(null);
        return boosts;
    }

    public void sanitize(JobBoosts defaults) {
        JobBoosts fallback = defaults == null ? null : defaults;
        moneyMultiplier = sanitizeDouble(moneyMultiplier, fallback == null ? 1.0D : fallback.moneyMultiplier, 1.0D);
        moneyMultiplierByLevel = sanitizeDoubleLevels(moneyMultiplierByLevel, fallback == null ? List.of() : fallback.moneyMultiplierByLevel, 1.0D);
        moneyFlatBonus = sanitizeLong(moneyFlatBonus, fallback == null ? 0L : fallback.moneyFlatBonus, 0L);
        moneyFlatBonusByLevel = sanitizeLongLevels(moneyFlatBonusByLevel, fallback == null ? List.of() : fallback.moneyFlatBonusByLevel, 0L);
        xpMultiplier = sanitizeDouble(xpMultiplier, fallback == null ? 1.0D : fallback.xpMultiplier, 1.0D);
        xpMultiplierByLevel = sanitizeDoubleLevels(xpMultiplierByLevel, fallback == null ? List.of() : fallback.xpMultiplierByLevel, 1.0D);
        xpFlatBonus = sanitizeLong(xpFlatBonus, fallback == null ? 0L : fallback.xpFlatBonus, 0L);
        xpFlatBonusByLevel = sanitizeLongLevels(xpFlatBonusByLevel, fallback == null ? List.of() : fallback.xpFlatBonusByLevel, 0L);
    }

    public double moneyMultiplierForLevel(int level) {
        return levelDoubleFor(level, moneyMultiplier, moneyMultiplierByLevel);
    }

    public long moneyFlatBonusForLevel(int level) {
        return levelLongFor(level, moneyFlatBonus, moneyFlatBonusByLevel);
    }

    public double xpMultiplierForLevel(int level) {
        return levelDoubleFor(level, xpMultiplier, xpMultiplierByLevel);
    }

    public long xpFlatBonusForLevel(int level) {
        return levelLongFor(level, xpFlatBonus, xpFlatBonusByLevel);
    }

    private static double levelDoubleFor(int level, double baseValue, List<JobLevelDouble> levels) {
        double value = baseValue;
        for (JobLevelDouble entry : levels) {
            if (entry != null && level >= entry.level) {
                value = entry.value;
            }
        }
        return value;
    }

    private static long levelLongFor(int level, long baseValue, List<JobLevelLong> levels) {
        long value = baseValue;
        for (JobLevelLong entry : levels) {
            if (entry != null && level >= entry.level) {
                value = entry.value;
            }
        }
        return value;
    }

    private static double sanitizeDouble(double raw, double fallback, double defaultValue) {
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw < 0.0D) {
            return fallback >= 0.0D ? fallback : defaultValue;
        }
        return raw;
    }

    private static long sanitizeLong(long raw, long fallback, long defaultValue) {
        if (raw < 0L) {
            return fallback >= 0L ? fallback : defaultValue;
        }
        return raw;
    }

    private static List<JobLevelDouble> sanitizeDoubleLevels(
        List<JobLevelDouble> raw,
        List<JobLevelDouble> fallback,
        double entryFallback
    ) {
        List<JobLevelDouble> out = new ArrayList<>();
        List<JobLevelDouble> source = raw == null || raw.isEmpty() ? fallback : raw;
        for (JobLevelDouble entry : source) {
            if (entry == null) {
                continue;
            }
            JobLevelDouble copy = new JobLevelDouble();
            copy.level = entry.level;
            copy.value = entry.value;
            copy.sanitize(entryFallback);
            out.add(copy);
        }
        out.sort(Comparator.comparingInt(value -> value.level));
        return out;
    }

    private static List<JobLevelLong> sanitizeLongLevels(
        List<JobLevelLong> raw,
        List<JobLevelLong> fallback,
        long entryFallback
    ) {
        List<JobLevelLong> out = new ArrayList<>();
        List<JobLevelLong> source = raw == null || raw.isEmpty() ? fallback : raw;
        for (JobLevelLong entry : source) {
            if (entry == null) {
                continue;
            }
            JobLevelLong copy = new JobLevelLong();
            copy.level = entry.level;
            copy.value = entry.value;
            copy.sanitize(entryFallback);
            out.add(copy);
        }
        out.sort(Comparator.comparingInt(value -> value.level));
        return out;
    }
}
