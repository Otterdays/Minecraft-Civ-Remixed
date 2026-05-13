package com.fpsmod.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobsProgressMessageTest {

    @Test
    void progressMessageIncludesJobXpAndLevelProgress() {
        String text = JobsService.progressMessageText(Job.MINER, 0, 5L);
        assertEquals("[miner] +5 xp · Lvl 0 · 5/100", text);
    }

    @Test
    void progressMessageShowsMaxAtLevelCap() {
        String text = JobsService.progressMessageText(Job.FIGHTER, JobsConfig.MAX_LEVEL, JobsConfig.xpForLevel(JobsConfig.MAX_LEVEL));
        assertEquals("[fighter] +5 xp · MAX", text);
    }
}
