package com.fpsmod.jobs;

/**
 * MVP curve constants. Future: load from {@code config/otters_civ_revived/jobs.json}.
 *
 * <p>XP curve: cumulative XP to reach level L is {@code xpBase * L^xpExponent}. Default
 * {@code 100 * L^1.5} → level 50 ≈ 35,355 XP.</p>
 *
 * <p>Multiplier curve: payout at level L is {@code 1.0 + (multiplierTopBonus * L / maxLevel)}.
 * Default: 1.0 at level 0, 2.0 at level 50.</p>
 */
public final class JobsConfig {
    public static final int MAX_LEVEL = 50;
    public static final double XP_BASE = 100.0;
    public static final double XP_EXPONENT = 1.5;
    public static final double MULTIPLIER_TOP_BONUS = 1.0;
    /** XP awarded per matching reward event (block break / mob kill). */
    public static final long XP_PER_EVENT = 5L;

    private JobsConfig() {}

    public static long xpForLevel(int level) {
        if (level <= 0) return 0L;
        return (long) Math.ceil(XP_BASE * Math.pow(level, XP_EXPONENT));
    }

    public static int levelForXp(long xp) {
        if (xp <= 0L) return 0;
        for (int lvl = MAX_LEVEL; lvl >= 1; lvl--) {
            if (xp >= xpForLevel(lvl)) return lvl;
        }
        return 0;
    }

    public static double multiplierForLevel(int level) {
        int clamped = Math.max(0, Math.min(MAX_LEVEL, level));
        return 1.0 + (MULTIPLIER_TOP_BONUS * (double) clamped / (double) MAX_LEVEL);
    }
}
