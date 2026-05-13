package com.fpsmod.jobs;

import com.fpsmod.io.AtomicFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link FileJobsStore} saves and loads atomically,
 * cleans up stale temp files, and round-trips job state correctly.
 */
class FileJobsStoreAtomicTest {

    @TempDir
    Path tempDir;

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void saveAndLoadRoundTrip() {
        Path file = tempDir.resolve("jobs.properties");
        FileJobsStore store = new FileJobsStore(file);

        JobState state = new JobState();
        state.setActive(Job.MINER);
        state.addXp(Job.MINER, 100L);
        state.addXp(Job.FIGHTER, 50L);

        store.save(Map.of(ID, state), Map.of(ID, "PlayerOne"));
        JobsLedger loaded = store.load();

        JobState loadedState = loaded.states().get(ID);
        assertNotNull(loadedState);
        assertEquals(Job.MINER, loadedState.active());
        assertEquals(100L, loadedState.getXp(Job.MINER));
        assertEquals(50L, loadedState.getXp(Job.FIGHTER));
        assertEquals("PlayerOne", loaded.displayHints().get(ID));
        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));
    }

    @Test
    void loadFromExistingFile() throws Exception {
        Path file = tempDir.resolve("jobs.properties");
        AtomicFileWriter.writeAtomically(file, w -> {
            w.write("# Name: PlayerOne");
            w.newLine();
            w.write(ID + ".active=miner");
            w.newLine();
            w.write(ID + ".xp.miner=100");
            w.newLine();
            w.write(ID + ".xp.fighter=50");
            w.newLine();
        });

        FileJobsStore store = new FileJobsStore(file);
        JobsLedger loaded = store.load();

        JobState s = loaded.states().get(ID);
        assertNotNull(s);
        assertEquals(Job.MINER, s.active());
        assertEquals(100L, s.getXp(Job.MINER));
        assertEquals(50L, s.getXp(Job.FIGHTER));
        assertEquals("PlayerOne", loaded.displayHints().get(ID));
    }

    @Test
    void loadCleansUpStaleTempFile() throws Exception {
        Path file = tempDir.resolve("jobs.properties");
        AtomicFileWriter.writeAtomically(file, w -> {
            w.write(ID + ".active=fighter");
            w.newLine();
        });

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, "garbage", StandardCharsets.UTF_8);
        assertTrue(Files.exists(tmp));

        FileJobsStore store = new FileJobsStore(file);
        JobsLedger loaded = store.load();
        assertEquals(Job.FIGHTER, loaded.states().get(ID).active());
        assertFalse(Files.exists(tmp), "stale .tmp should be cleaned");
    }

    @Test
    void loadReturnsEmptyWhenNoFile() {
        Path file = tempDir.resolve("nonexistent.properties");
        FileJobsStore store = new FileJobsStore(file);
        JobsLedger loaded = store.load();
        assertTrue(loaded.states().isEmpty());
    }

    @Test
    void overwritePreservesNewData() {
        Path file = tempDir.resolve("jobs.properties");
        FileJobsStore store = new FileJobsStore(file);

        JobState first = new JobState();
        first.setActive(Job.MINER);
        store.save(Map.of(ID, first), Map.of());

        JobState second = new JobState();
        second.setActive(Job.FIGHTER);
        second.addXp(Job.FIGHTER, 200L);
        store.save(Map.of(ID, second), Map.of());

        JobsLedger loaded = store.load();
        assertEquals(Job.FIGHTER, loaded.states().get(ID).active());
        assertEquals(200L, loaded.states().get(ID).getXp(Job.FIGHTER));
    }

    @Test
    void loadFromEmptyFileReturnsEmpty() throws Exception {
        Path file = tempDir.resolve("jobs.properties");
        AtomicFileWriter.writeAtomically(file, w -> {});
        FileJobsStore store = new FileJobsStore(file);
        JobsLedger loaded = store.load();
        assertTrue(loaded.states().isEmpty());
    }
}
