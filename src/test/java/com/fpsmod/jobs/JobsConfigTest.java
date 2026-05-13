package com.fpsmod.jobs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JobsConfigTest {

    @Test
    void xpForLevelMonotonicAndZeroAtZero() {
        Assertions.assertEquals(0L, JobsConfig.xpForLevel(0));
        long prev = -1L;
        for (int l = 1; l <= JobsConfig.MAX_LEVEL; l++) {
            long x = JobsConfig.xpForLevel(l);
            Assertions.assertTrue(x > prev, "xpForLevel must increase, broke at level " + l);
            prev = x;
        }
    }

    @Test
    void levelForXpRoundTrip() {
        for (int l = 0; l <= JobsConfig.MAX_LEVEL; l++) {
            long x = JobsConfig.xpForLevel(l);
            Assertions.assertEquals(l, JobsConfig.levelForXp(x),
                "round-trip mismatch at level " + l);
        }
    }

    @Test
    void multiplierClampedAndMonotonic() {
        Assertions.assertEquals(1.0, JobsConfig.multiplierForLevel(0), 1e-9);
        Assertions.assertEquals(1.0, JobsConfig.multiplierForLevel(-5), 1e-9);
        Assertions.assertEquals(2.0, JobsConfig.multiplierForLevel(JobsConfig.MAX_LEVEL), 1e-9);
        Assertions.assertEquals(2.0, JobsConfig.multiplierForLevel(JobsConfig.MAX_LEVEL + 99), 1e-9);
        double prev = -1.0;
        for (int l = 0; l <= JobsConfig.MAX_LEVEL; l++) {
            double m = JobsConfig.multiplierForLevel(l);
            Assertions.assertTrue(m >= prev, "multiplier must be non-decreasing at level " + l);
            prev = m;
        }
    }

    @Test
    void jobStateXpAndLevel() {
        JobState s = new JobState();
        Assertions.assertNull(s.active());
        Assertions.assertEquals(0L, s.getXp(Job.MINER));
        Assertions.assertEquals(0, s.levelOf(Job.MINER));

        long after = s.addXp(Job.MINER, JobsConfig.xpForLevel(1));
        Assertions.assertEquals(JobsConfig.xpForLevel(1), after);
        Assertions.assertEquals(1, s.levelOf(Job.MINER));

        s.setActive(Job.MINER);
        Assertions.assertEquals(Job.MINER, s.active());
    }
}
