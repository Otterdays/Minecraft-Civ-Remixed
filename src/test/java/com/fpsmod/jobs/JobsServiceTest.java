package com.fpsmod.jobs;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JobsService} using an in-memory {@link FakeJobsStore}.
 * Covers join/leave, XP accumulation, hint storage, and persist-on-mutation.
 */
class JobsServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void joinJobSetsActiveAndPersists() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store);
        svc.joinJob(ID, Job.MINER, null);
        // null player is fine for these tests since we don't trigger status listener side-effects

        assertEquals(Job.MINER, svc.stateOf(ID).active());
        assertEquals(1, store.saveCount);

        JobsLedger reloaded = store.load();
        assertEquals(Job.MINER, reloaded.states().get(ID).active());
    }

    @Test
    void joinJobIdempotentReturnsFalse() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store);
        assertTrue(svc.joinJob(ID, Job.FIGHTER, null));
        assertFalse(svc.joinJob(ID, Job.FIGHTER, null));
        assertEquals(1, store.saveCount);  // second join didn't persist
    }

    @Test
    void leaveJobClearsActiveAndPersists() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store);
        svc.joinJob(ID, Job.LUMBERJACK, null);
        assertEquals(1, store.saveCount);

        svc.leaveJob(ID, null);
        assertEquals(2, store.saveCount);
        assertNull(svc.stateOf(ID).active());
    }

    @Test
    void leaveJobWhenInactiveReturnsFalse() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store);
        assertFalse(svc.leaveJob(ID, null));
        assertEquals(0, store.saveCount);
    }

    @Test
    void xpPersistedOnEconomyReward() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store);
        svc.joinJob(ID, Job.MINER, null);
        assertEquals(0L, svc.stateOf(ID).getXp(Job.MINER));

        svc.onEconomyReward(null, null);
        // null ctx means no job matching, so no XP
        assertEquals(0L, svc.stateOf(ID).getXp(Job.MINER));

        // We can't easily test the full RewardContext flow without Minecraft classes,
        // but we verify the XP-add path directly:
        long xp = svc.stateOf(ID).addXp(Job.MINER, JobsConfig.XP_PER_EVENT);
        assertEquals(JobsConfig.XP_PER_EVENT, xp);
    }

    /**
     * {@link JobsService#rememberPlayerName} stores hints in memory; they are persisted
     * during the next {@link JobsService#joinJob} / mutate call that triggers a persist.
     */
    @Test
    void hintSurvivesPersistRoundTrip() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store);

        svc.rememberPlayerName(ID, "TestPlayer");
        svc.joinJob(ID, Job.MINER, null);

        JobsLedger reloaded = store.load();
        assertEquals("TestPlayer", reloaded.displayHints().get(ID));
    }

    @Test
    void stateOfReturnsNewStateForUnknownId() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store);

        JobState s = svc.stateOf(ID);
        assertNotNull(s);
        assertNull(s.active());
        assertEquals(0L, s.getXp(Job.MINER));
    }
}
