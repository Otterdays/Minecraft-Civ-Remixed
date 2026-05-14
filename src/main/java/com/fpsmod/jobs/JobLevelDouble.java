package com.fpsmod.jobs;

public final class JobLevelDouble {
    public int level = 0;
    public double value = 1.0D;

    public void sanitize(double fallback) {
        if (level < 0) {
            level = 0;
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            value = fallback;
        }
    }
}
