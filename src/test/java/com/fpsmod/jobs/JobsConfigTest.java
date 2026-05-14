package com.fpsmod.jobs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JobsConfigTest {

    @Test
    void xpForLevelMonotonicAndZeroAtZero() {
        JobsConfig config = JobsConfig.defaults();
        Assertions.assertEquals(0L, config.xpForLevel(0));
        long prev = -1L;
        for (int l = 1; l <= config.maxLevel(); l++) {
            long x = config.xpForLevel(l);
            Assertions.assertTrue(x > prev, "xpForLevel must increase, broke at level " + l);
            prev = x;
        }
    }

    @Test
    void levelForXpRoundTrip() {
        JobsConfig config = JobsConfig.defaults();
        for (int l = 0; l <= config.maxLevel(); l++) {
            long x = config.xpForLevel(l);
            Assertions.assertEquals(l, config.levelForXp(x),
                "round-trip mismatch at level " + l);
        }
    }

    @Test
    void multiplierClampedAndMonotonic() {
        JobsConfig config = JobsConfig.defaults();
        Assertions.assertEquals(1.0, config.multiplierForLevel(0), 1e-9);
        Assertions.assertEquals(1.0, config.multiplierForLevel(-5), 1e-9);
        Assertions.assertEquals(2.0, config.multiplierForLevel(config.maxLevel()), 1e-9);
        Assertions.assertEquals(2.0, config.multiplierForLevel(config.maxLevel() + 99), 1e-9);
        double prev = -1.0;
        for (int l = 0; l <= config.maxLevel(); l++) {
            double m = config.multiplierForLevel(l);
            Assertions.assertTrue(m >= prev, "multiplier must be non-decreasing at level " + l);
            prev = m;
        }
    }

    @Test
    void jobStateXpAndLevel() {
        JobsConfig config = JobsConfig.defaults();
        JobState s = new JobState();
        Assertions.assertNull(s.active());
        Assertions.assertEquals(0L, s.getXp(Job.MINER));
        Assertions.assertEquals(0, s.levelOf(Job.MINER, config));

        long after = s.addXp(Job.MINER, config.xpForLevel(1));
        Assertions.assertEquals(config.xpForLevel(1), after);
        Assertions.assertEquals(1, s.levelOf(Job.MINER, config));

        s.setActive(Job.MINER);
        Assertions.assertEquals(Job.MINER, s.active());
    }

    @Test
    void sanitizeKeepsShippedJobsAndAllowsPerJobOverrides() {
        JobsConfig config = new JobsConfig();
        config.maxLevel = 0;
        config.xpBase = -1.0D;
        config.xpExponent = 0.0D;
        config.multiplierTopBonus = -2.0D;
        config.xpPerEvent = -5L;
        JobsConfig.JobSettings miner = new JobsConfig.JobSettings();
        miner.tagId = " custom:miner ";
        miner.xpPerEvent = 9L;
        JobsConfig.JobSettings fighter = new JobsConfig.JobSettings();
        fighter.tagId = " ";
        fighter.xpPerEvent = -3L;
        config.jobs.put("miner", miner);
        config.jobs.put("fighter", fighter);
        config.jobs.put("unknown_job", new JobsConfig.JobSettings());

        config.sanitize();

        Assertions.assertEquals(JobsConfig.DEFAULT_MAX_LEVEL, config.maxLevel());
        Assertions.assertEquals(JobsConfig.DEFAULT_XP_BASE, config.xpBase, 1e-9);
        Assertions.assertEquals(JobsConfig.DEFAULT_XP_EXPONENT, config.xpExponent, 1e-9);
        Assertions.assertEquals(JobsConfig.DEFAULT_MULTIPLIER_TOP_BONUS, config.multiplierTopBonus, 1e-9);
        Assertions.assertEquals(JobsConfig.DEFAULT_XP_PER_EVENT, config.xpPerEvent);
        Assertions.assertEquals("custom:miner", config.tagIdFor(Job.MINER));
        Assertions.assertEquals(9L, config.xpPerEvent(Job.MINER));
        Assertions.assertEquals(Job.FIGHTER.defaultTagId(), config.tagIdFor(Job.FIGHTER));
        Assertions.assertEquals(JobsConfig.DEFAULT_XP_PER_EVENT, config.xpPerEvent(Job.FIGHTER));
        Assertions.assertEquals(Job.values().length, config.jobs.size());
    }
}
