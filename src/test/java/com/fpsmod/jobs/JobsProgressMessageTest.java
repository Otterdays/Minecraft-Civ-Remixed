package com.fpsmod.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobsProgressMessageTest {

    @Test
    void progressMessageIncludesJobXpAndLevelProgress() {
        JobsConfig config = JobsConfig.defaults();
        String text = JobsService.progressMessageText(Job.MINER, 0, 5L, config, 5L);
        assertEquals("[miner] +5 xp · Lvl 0 · 5/100", text);
    }

    @Test
    void progressMessageShowsMaxAtLevelCap() {
        JobsConfig config = JobsConfig.defaults();
        String text = JobsService.progressMessageText(
            Job.FIGHTER,
            config.maxLevel(),
            config.xpForLevel(config.maxLevel()),
            config,
            config.xpPerEvent(Job.FIGHTER)
        );
        assertEquals("[fighter] +5 xp · MAX", text);
    }
}
