package com.fpsmod.jobs;

public final class JobLevelLong {
    public int level = 0;
    public long value = 0L;

    public void sanitize(long fallback) {
        if (level < 0) {
            level = 0;
        }
        if (value < 0L) {
            value = fallback;
        }
    }
}
