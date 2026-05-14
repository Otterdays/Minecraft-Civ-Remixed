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
        Path file = tempDir.resolve("jobs_state.json");
        FileJobsStore store = new FileJobsStore(file);

        JobState state = new JobState();
        state.activate("miner", 1);
        state.addXp("miner", 100L);
        state.addXp("fighter", 50L);

        store.save(Map.of(ID, state), Map.of(ID, "PlayerOne"));
        JobsLedger loaded = store.load();

        JobState loadedState = loaded.states().get(ID);
        assertNotNull(loadedState);
        assertEquals("miner", loadedState.primaryActiveJobId());
        assertEquals(100L, loadedState.getXp("miner"));
        assertEquals(50L, loadedState.getXp("fighter"));
        assertEquals("PlayerOne", loaded.displayHints().get(ID));
        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));
    }

    @Test
    void loadMigratesLegacyPropertiesFile() throws Exception {
        Path file = tempDir.resolve("jobs_state.json");
        Path legacy = tempDir.resolve("jobs.properties");
        AtomicFileWriter.writeAtomically(legacy, w -> {
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
        assertEquals("miner", s.primaryActiveJobId());
        assertEquals(100L, s.getXp("miner"));
        assertEquals(50L, s.getXp("fighter"));
        assertEquals("PlayerOne", loaded.displayHints().get(ID));
        assertFalse(Files.exists(legacy));
        assertTrue(Files.exists(file));
    }

    @Test
    void loadCleansUpStaleTempFile() throws Exception {
        Path file = tempDir.resolve("jobs_state.json");
        AtomicFileWriter.writeAtomically(file, w -> {
            w.write("{\"players\":{\"" + ID + "\":{\"activeJobs\":[\"fighter\"],\"xpByJobId\":{}}}}");
        });

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, "garbage", StandardCharsets.UTF_8);
        assertTrue(Files.exists(tmp));

        FileJobsStore store = new FileJobsStore(file);
        JobsLedger loaded = store.load();
        assertEquals("fighter", loaded.states().get(ID).primaryActiveJobId());
        assertFalse(Files.exists(tmp), "stale .tmp should be cleaned");
    }

    @Test
    void loadReturnsEmptyWhenNoFile() {
        Path file = tempDir.resolve("nonexistent.json");
        FileJobsStore store = new FileJobsStore(file);
        JobsLedger loaded = store.load();
        assertTrue(loaded.states().isEmpty());
    }

    @Test
    void overwritePreservesNewData() {
        Path file = tempDir.resolve("jobs_state.json");
        FileJobsStore store = new FileJobsStore(file);

        JobState first = new JobState();
        first.activate("miner", 1);
        store.save(Map.of(ID, first), Map.of());

        JobState second = new JobState();
        second.activate("fighter", 1);
        second.addXp("fighter", 200L);
        store.save(Map.of(ID, second), Map.of());

        JobsLedger loaded = store.load();
        assertEquals("fighter", loaded.states().get(ID).primaryActiveJobId());
        assertEquals(200L, loaded.states().get(ID).getXp("fighter"));
    }

    @Test
    void loadFromEmptyFileReturnsEmpty() throws Exception {
        Path file = tempDir.resolve("jobs_state.json");
        AtomicFileWriter.writeAtomically(file, w -> {});
        FileJobsStore store = new FileJobsStore(file);
        JobsLedger loaded = store.load();
        assertTrue(loaded.states().isEmpty());
    }
}
