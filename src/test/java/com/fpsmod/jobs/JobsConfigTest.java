package com.fpsmod.jobs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JobsConfigTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void xpForLevelMonotonicAndZeroAtZero() {
        JobsConfig config = JobsConfig.defaults();
        JobProgression progression = config.jobById("miner").progression;
        Assertions.assertEquals(0L, progression.xpForLevel(0));
        long prev = -1L;
        for (int l = 1; l <= progression.maxLevel; l++) {
            long x = progression.xpForLevel(l);
            Assertions.assertTrue(x > prev, "xpForLevel must increase, broke at level " + l);
            prev = x;
        }
        Assertions.assertEquals(20L, progression.xpForLevel(1));
        Assertions.assertEquals(40, progression.maxLevel);
    }

    @Test
    void levelForXpRoundTrip() {
        JobsConfig config = JobsConfig.defaults();
        JobProgression progression = config.jobById("fighter").progression;
        for (int l = 0; l <= progression.maxLevel; l++) {
            long x = progression.xpForLevel(l);
            Assertions.assertEquals(l, progression.levelForXp(x),
                "round-trip mismatch at level " + l);
        }
    }

    @Test
    void boostTablesAreMonotonicByConfiguredThresholds() {
        JobsConfig config = JobsConfig.defaults();
        Job job = config.jobById("miner");
        Assertions.assertNotNull(job);
        Assertions.assertEquals(1.0D, job.boosts.moneyMultiplierForLevel(0), 1e-9);
        Assertions.assertEquals(0L, job.boosts.moneyFlatBonusForLevel(0));
        Assertions.assertEquals(1.0D, job.boosts.xpMultiplierForLevel(0), 1e-9);
        Assertions.assertTrue(job.boosts.moneyMultiplierForLevel(job.progression.maxLevel) <= 1.20D);
        Assertions.assertTrue(job.boosts.xpMultiplierForLevel(job.progression.maxLevel) <= 1.20D);
    }

    @Test
    void starterPackShipsFiveDistinctJobsWithAlignedDefaults() {
        JobsConfig config = JobsConfig.defaults();
        Assertions.assertEquals(5, config.jobs.size());
        Assertions.assertEquals("single", config.global.activationPolicy);
        Assertions.assertEquals(1, config.global.maxActiveJobs);
        Assertions.assertNotNull(config.jobById("excavator"));
        for (Job job : config.jobs) {
            Assertions.assertFalse(job.triggers.isEmpty(), job.id + " should have a starter trigger");
            Assertions.assertTrue(job.triggers.get(0).requireEconomyReward, job.id + " should align to rewardable events");
        }
    }

    @Test
    void jobStateXpAndLevel() {
        JobsConfig config = JobsConfig.defaults();
        Job miner = config.jobById("miner");
        JobState s = new JobState();
        Assertions.assertNull(s.primaryActiveJobId());
        Assertions.assertEquals(0L, s.getXp("miner"));
        Assertions.assertEquals(0, s.levelOf("miner", miner.progression));

        long after = s.addXp("miner", miner.progression.xpForLevel(1));
        Assertions.assertEquals(miner.progression.xpForLevel(1), after);
        Assertions.assertEquals(1, s.levelOf("miner", miner.progression));

        s.activate("miner", 1);
        Assertions.assertEquals("miner", s.primaryActiveJobId());
    }

    @Test
    void sanitizeKeepsUniqueJobsAndNormalizesGlobalDefaults() {
        JobsConfig config = new JobsConfig();
        config.global.activationPolicy = "MULTI";
        config.global.maxActiveJobs = 0;
        config.global.defaultIconGlyph = " ";
        config.jobs = new java.util.ArrayList<>();

        Job duplicateA = new Job();
        duplicateA.id = " CUSTOM_MINER ";
        duplicateA.displayName = "";
        duplicateA.progression.maxLevel = 0;
        duplicateA.progression.xpPerEvent = -9L;
        duplicateA.triggers.add(new JobTrigger());

        Job duplicateB = new Job();
        duplicateB.id = "custom_miner";
        duplicateB.displayName = "Should be dropped";
        duplicateB.triggers.add(new JobTrigger());

        Job blankId = new Job();
        blankId.id = " ";

        config.jobs.add(duplicateA);
        config.jobs.add(duplicateB);
        config.jobs.add(blankId);

        config.sanitize();

        Assertions.assertEquals("multi", config.global.activationPolicy);
        Assertions.assertEquals(1, config.global.maxActiveJobs);
        Assertions.assertEquals("*", config.global.defaultIconGlyph);
        Assertions.assertEquals(1, config.jobs.size());
        Assertions.assertEquals("custom_miner", config.jobs.get(0).id);
        Assertions.assertEquals("Custom Miner", config.jobs.get(0).displayName);
        Assertions.assertEquals(5L, config.jobs.get(0).progression.xpPerEvent);
        Assertions.assertEquals(40, config.jobs.get(0).progression.maxLevel);
    }
}
