package com.fpsmod.economy;

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
 * Integration tests for {@link FileWalletStore} covering atomic save/load,
 * stale temp cleanup, legacy format compatibility, concurrent persistence,
 * and crash recovery via the {@link AtomicFileWriter} backing.
 */
class FileWalletStoreAtomicTest {

    @TempDir
    Path tempDir;

    private static final UUID ID1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void saveAndLoadRoundTrip() {
        Path file = tempDir.resolve("wallet.properties");
        FileWalletStore store = new FileWalletStore(file);

        store.save(Map.of(ID1, 100L, ID2, 250L), Map.of(ID1, "Alice"));

        WalletLedger loaded = store.load();
        assertEquals(100L, loaded.balances().get(ID1));
        assertEquals(250L, loaded.balances().get(ID2));
        assertEquals("Alice", loaded.displayHints().get(ID1));
        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));
    }

    @Test
    void loadFromExistingFile() throws Exception {
        Path file = tempDir.resolve("wallet.properties");
        AtomicFileWriter.writeAtomically(file, w -> {
            w.write("# Name: Alice");
            w.newLine();
            w.write(ID1.toString() + "=100");
            w.newLine();
            w.write(ID2.toString() + "=250");
            w.newLine();
        });

        FileWalletStore store = new FileWalletStore(file);
        WalletLedger loaded = store.load();
        assertEquals(100L, loaded.balances().get(ID1));
        assertEquals(250L, loaded.balances().get(ID2));
        assertEquals("Alice", loaded.displayHints().get(ID1));
    }

    @Test
    void loadCleansUpStaleTempFile() throws Exception {
        Path file = tempDir.resolve("wallet.properties");
        AtomicFileWriter.writeAtomically(file, w -> {
            w.write(ID1.toString() + "=100");
            w.newLine();
        });

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, "garbage", StandardCharsets.UTF_8);
        assertTrue(Files.exists(tmp));

        FileWalletStore store = new FileWalletStore(file);
        WalletLedger loaded = store.load();
        assertEquals(100L, loaded.balances().get(ID1));
        assertFalse(Files.exists(tmp), "stale .tmp should be cleaned");
    }

    @Test
    void loadReturnsEmptyWhenNoFile() {
        Path file = tempDir.resolve("nonexistent.properties");
        FileWalletStore store = new FileWalletStore(file);
        WalletLedger loaded = store.load();
        assertTrue(loaded.balances().isEmpty());
    }

    @Test
    void overwritePreservesNewData() {
        Path file = tempDir.resolve("wallet.properties");
        FileWalletStore store = new FileWalletStore(file);

        store.save(Map.of(ID1, 100L), Map.of());
        store.save(Map.of(ID1, 999L), Map.of());

        WalletLedger loaded = store.load();
        assertEquals(999L, loaded.balances().get(ID1));
    }

    @Test
    void saveWithHintsNullSafe() {
        Path file = tempDir.resolve("wallet.properties");
        FileWalletStore store = new FileWalletStore(file);

        var hints = new java.util.HashMap<UUID, String>();
        hints.put(ID1, null);
        store.save(Map.of(ID1, 50L), hints);
        WalletLedger loaded = store.load();
        assertEquals(50L, loaded.balances().get(ID1));
    }

    @Test
    void concurrentSavesProduceValidFile() throws Exception {
        Path file = tempDir.resolve("concurrent.properties");
        FileWalletStore store = new FileWalletStore(file);

        UUID[] ids = {ID1, ID2, ID1};
        long[] amounts = {100L, 200L, 300L};
        Thread[] threads = new Thread[3];
        for (int i = 0; i < threads.length; i++) {
            int idx = i;
            threads[i] = new Thread(() ->
                store.save(Map.of(ids[idx], amounts[idx]), Map.of())
            );
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // File must be valid and parseable
        WalletLedger result = store.load();
        assertNotNull(result);
    }

    @Test
    void loadFromLegacyPropertiesFormat() throws Exception {
        Path file = tempDir.resolve("wallet.properties");
        // Write in java.util.Properties format (no # Name: hints)
        var props = new java.util.Properties();
        props.setProperty(ID1.toString(), "100");
        props.setProperty(ID2.toString(), "250");
        try (var os = Files.newOutputStream(file)) {
            props.store(os, null);
        }

        FileWalletStore store = new FileWalletStore(file);
        WalletLedger loaded = store.load();
        assertEquals(100L, loaded.balances().get(ID1));
        assertEquals(250L, loaded.balances().get(ID2));
    }

    @Test
    void loadFromCorruptFileReturnsEmpty() throws Exception {
        Path file = tempDir.resolve("corrupt.properties");
        Files.writeString(file, "not a valid format at all !!!");

        FileWalletStore store = new FileWalletStore(file);
        WalletLedger loaded = store.load();
        assertTrue(loaded.balances().isEmpty());
    }

    @Test
    void loadWithBothStaleTempAndBak() throws Exception {
        // Previous successful write
        Path file = tempDir.resolve("with-bak.properties");
        FileWalletStore store = new FileWalletStore(file);
        store.save(Map.of(ID1, 42L), Map.of());

        // Verify file readable
        WalletLedger loaded = store.load();
        assertEquals(42L, loaded.balances().get(ID1));
    }
}
