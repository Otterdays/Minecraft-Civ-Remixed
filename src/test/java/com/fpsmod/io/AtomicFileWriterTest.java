package com.fpsmod.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AtomicFileWriter}: crash-safe write, crash simulation,
 * stale temp cleanup, backup rotation, concurrent safety, edge cases.
 */
class AtomicFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void normalWriteRoundTrip() throws IOException {
        Path target = tempDir.resolve("test.txt");
        AtomicFileWriter.writeAtomically(target, w -> {
            w.write("hello world");
        });
        assertEquals("hello world", Files.readString(target));
        assertFalse(Files.exists(AtomicFileWriter.siblingTemp(target)));
    }

    @Test
    void midWriteCrashLeavesTargetUntouched() throws IOException {
        Path target = tempDir.resolve("test.txt");
        AtomicFileWriter.writeAtomically(target, w -> w.write("original content"));

        try {
            AtomicFileWriter.writeAtomically(target, w -> {
                w.write("new content ");
                throw new RuntimeException("simulated crash");
            });
            fail("expected exception");
        } catch (RuntimeException ignored) {
        }

        assertEquals("original content", Files.readString(target));
    }

    @Test
    void midWriteCrashOnFirstWriteLeavesNoTarget() throws IOException {
        Path target = tempDir.resolve("test.txt");
        try {
            AtomicFileWriter.writeAtomically(target, w -> {
                w.write("partial ");
                throw new RuntimeException("simulated crash");
            });
            fail("expected exception");
        } catch (RuntimeException ignored) {
        }

        assertFalse(Files.exists(target), "target should not exist after failed first write");
        // .tmp is intentionally left behind as evidence of the interrupted write;
        // deleteStaleTemp will clean it on the next load().
        assertTrue(Files.exists(AtomicFileWriter.siblingTemp(target)), "stale .tmp should remain after crash");
    }

    @Test
    void staleTempCleanedOnDelete() throws IOException {
        Path target = tempDir.resolve("test.txt");
        Path tmp = AtomicFileWriter.siblingTemp(target);
        Files.writeString(tmp, "stale data");

        AtomicFileWriter.deleteStaleTemp(target);
        assertFalse(Files.exists(tmp));
    }

    @Test
    void deleteStaleTempNoOpWhenNoFile() {
        Path target = tempDir.resolve("nope.txt");
        AtomicFileWriter.deleteStaleTemp(target);
        assertFalse(Files.exists(AtomicFileWriter.siblingTemp(target)));
    }

    @Test
    void overwriteExistingFile() throws IOException {
        Path target = tempDir.resolve("test.txt");
        AtomicFileWriter.writeAtomically(target, w -> w.write("first"));
        AtomicFileWriter.writeAtomically(target, w -> w.write("second"));
        assertEquals("second", Files.readString(target));
    }

    @Test
    void largeContent() throws IOException {
        Path target = tempDir.resolve("large.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("line ").append(i).append("\n");
        }
        String content = sb.toString();
        AtomicFileWriter.writeAtomically(target, w -> w.write(content));
        assertEquals(content, Files.readString(target));
    }

    @Test
    void writeWithBackupCreatesBakThenCleans() throws IOException {
        Path target = tempDir.resolve("backup-test.txt");
        AtomicFileWriter.writeAtomicallyWithBackup(target, w -> w.write("first"));
        assertFalse(Files.exists(AtomicFileWriter.siblingBak(target)));

        AtomicFileWriter.writeAtomicallyWithBackup(target, w -> w.write("second"));
        assertFalse(Files.exists(AtomicFileWriter.siblingBak(target)));
        assertEquals("second", Files.readString(target));
    }

    @Test
    void writeWithBackupFirstWriteNoBak() throws IOException {
        Path target = tempDir.resolve("fresh.txt");
        AtomicFileWriter.writeAtomicallyWithBackup(target, w -> w.write("new"));
        assertEquals("new", Files.readString(target));
        assertFalse(Files.exists(AtomicFileWriter.siblingBak(target)));
    }

    @Test
    void emptyContent() throws IOException {
        Path target = tempDir.resolve("empty.txt");
        AtomicFileWriter.writeAtomically(target, w -> {});
        assertEquals("", Files.readString(target));
    }

    @Test
    void specialCharacters() throws IOException {
        Path target = tempDir.resolve("special.txt");
        String content = "line1\nline2\r\nline3\t tab\nunicode π≈3.14\nemoji_symbols ©®™";
        AtomicFileWriter.writeAtomically(target, w -> w.write(content));
        assertEquals(content, Files.readString(target));
    }

    @Test
    void directoryAsTargetFails() {
        Path target = tempDir.resolve("subdir");
        try {
            Files.createDirectory(target);
            AtomicFileWriter.writeAtomically(target, w -> w.write("content"));
            fail("expected IOException for directory target");
        } catch (IOException ignored) {
        }
    }

    @Test
    void writeWithBackupRestoresOnCrash() throws IOException {
        Path target = tempDir.resolve("backup-crash.txt");
        AtomicFileWriter.writeAtomicallyWithBackup(target, w -> w.write("good data"));

        try {
            AtomicFileWriter.writeAtomicallyWithBackup(target, w -> {
                w.write("new data ");
                throw new RuntimeException("simulated crash");
            });
            fail("expected exception");
        } catch (RuntimeException ignored) {
        }

        // Writer restored target from .bak; .bak is cleaned after restore
        assertEquals("good data", Files.readString(target));
        assertFalse(Files.exists(AtomicFileWriter.siblingBak(target)),
            ".bak should be consumed by restore");
        assertTrue(Files.exists(AtomicFileWriter.siblingTemp(target)),
            "stale .tmp should remain as evidence");
    }

    @Test
    void writeWithBackupTwiceThenCrashRestoresV2() throws IOException {
        Path target = tempDir.resolve("backup-twice.txt");
        AtomicFileWriter.writeAtomicallyWithBackup(target, w -> w.write("v1"));
        AtomicFileWriter.writeAtomicallyWithBackup(target, w -> w.write("v2"));

        try {
            AtomicFileWriter.writeAtomicallyWithBackup(target, w -> {
                w.write("v3 ");
                throw new RuntimeException("simulated crash");
            });
            fail("expected exception");
        } catch (RuntimeException ignored) {
        }

        // After restore, target should be v2 (last successful write)
        assertEquals("v2", Files.readString(target));
    }

    @Test
    void concurrentWritesWithBackup() throws Exception {
        Path target = tempDir.resolve("concurrent-bak.txt");
        String[] contents = {"aaa\n", "bbb\n", "ccc\n"};
        Thread[] threads = new Thread[contents.length];
        for (int i = 0; i < contents.length; i++) {
            int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    AtomicFileWriter.writeAtomicallyWithBackup(target, w -> w.write(contents[idx]));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertTrue(Files.exists(target));
        String result = Files.readString(target);
        assertTrue(result.equals("aaa\n") || result.equals("bbb\n") || result.equals("ccc\n"),
            "unexpected content: " + result);
    }

    @Test
    void siblingTempAndBakPaths() {
        Path target = Path.of("data.properties");
        assertEquals("data.properties.tmp", AtomicFileWriter.siblingTemp(target).getFileName().toString());
        assertEquals("data.properties.bak", AtomicFileWriter.siblingBak(target).getFileName().toString());
    }

    @Test
    void concurrentWrites() throws Exception {
        Path target = tempDir.resolve("concurrent.txt");
        String[] contents = {"alpha\n", "beta\n", "gamma\n"};
        Thread[] threads = new Thread[contents.length];
        for (int i = 0; i < contents.length; i++) {
            int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    AtomicFileWriter.writeAtomically(target, w -> w.write(contents[idx]));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        String result = Files.readString(target);
        assertTrue(result.equals("alpha\n") || result.equals("beta\n") || result.equals("gamma\n"),
            "unexpected content: " + result);
    }
}
