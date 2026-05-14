package com.fpsmod.jobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JobsService} using an in-memory {@link FakeJobsStore}.
 * Covers join/leave, XP accumulation, hint storage, and persist-on-mutation.
 */
class JobsServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static JobsConfig config;
    private static final String MINER = "miner";
    private static final String FIGHTER = "fighter";
    private static final String LUMBERJACK = "lumberjack";

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
        config = JobsConfig.defaults();
    }

    @Test
    void joinJobSetsActiveAndPersists() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store, config);
        svc.joinJob(ID, MINER, null);
        // null player is fine for these tests since we don't trigger status listener side-effects

        assertEquals(MINER, svc.stateOf(ID).primaryActiveJobId());
        assertEquals(1, store.saveCount);

        JobsLedger reloaded = store.load();
        assertEquals(MINER, reloaded.states().get(ID).primaryActiveJobId());
    }

    @Test
    void joinJobIdempotentReturnsFalse() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store, config);
        assertTrue(svc.joinJob(ID, FIGHTER, null));
        assertFalse(svc.joinJob(ID, FIGHTER, null));
        assertEquals(1, store.saveCount);  // second join didn't persist
    }

    @Test
    void leaveJobClearsActiveAndPersists() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store, config);
        svc.joinJob(ID, LUMBERJACK, null);
        assertEquals(1, store.saveCount);

        svc.leaveJob(ID, null, null);
        assertEquals(2, store.saveCount);
        assertNull(svc.stateOf(ID).primaryActiveJobId());
    }

    @Test
    void leaveJobWhenInactiveReturnsFalse() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store, config);
        assertFalse(svc.leaveJob(ID, null, null));
        assertEquals(0, store.saveCount);
    }

    @Test
    void statusSnapshotReflectsXp() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store, config);
        svc.joinJob(ID, MINER, null);
        assertEquals(0L, svc.stateOf(ID).getXp(MINER));

        Job miner = svc.jobById(MINER);
        assertNotNull(miner);
        long xp = svc.stateOf(ID).addXp(MINER, miner.progression.xpPerEvent);
        assertEquals(miner.progression.xpPerEvent, xp);

        JobStatusSnapshotData snapshot = svc.statusSnapshot(ID);
        assertEquals(List.of(MINER), snapshot.activeJobIds);
        assertTrue(snapshot.progress.stream().anyMatch(entry -> MINER.equals(entry.jobId) && entry.xp == xp));
    }

    /**
     * {@link JobsService#rememberPlayerName} stores hints in memory; they are persisted
     * during the next {@link JobsService#joinJob} / mutate call that triggers a persist.
     */
    @Test
    void hintSurvivesPersistRoundTrip() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store, config);

        svc.rememberPlayerName(ID, "TestPlayer");
        svc.joinJob(ID, MINER, null);

        JobsLedger reloaded = store.load();
        assertEquals("TestPlayer", reloaded.displayHints().get(ID));
    }

    @Test
    void stateOfReturnsNewStateForUnknownId() {
        FakeJobsStore store = new FakeJobsStore(new HashMap<>());
        JobsService svc = new JobsService(store, config);

        JobState s = svc.stateOf(ID);
        assertNotNull(s);
        assertNull(s.primaryActiveJobId());
        assertEquals(0L, s.getXp(MINER));
    }
}
