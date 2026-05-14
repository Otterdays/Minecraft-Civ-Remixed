package com.fpsmod.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobsProgressMessageTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void progressMessageIncludesJobXpAndLevelProgress() {
        Job job = new Job();
        job.id = "miner";
        job.displayName = "Miner";
        job.shortLabel = "miner";
        job.sanitize(JobsConfig.defaults().global);
        String text = JobsService.progressMessageText(job, 0, 5L, 5L);
        assertEquals("[miner] +5 xp · Lvl 0 · 5/20", text);
    }

    @Test
    void progressMessageShowsMaxAtLevelCap() {
        Job job = new Job();
        job.id = "fighter";
        job.displayName = "Fighter";
        job.shortLabel = "fighter";
        job.sanitize(JobsConfig.defaults().global);
        String text = JobsService.progressMessageText(
            job,
            job.progression.maxLevel,
            job.progression.xpForLevel(job.progression.maxLevel),
            job.progression.xpPerEvent
        );
        assertEquals("[fighter] +5 xp · MAX", text);
    }
}
