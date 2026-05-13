package com.fpsmod.io;

import com.fpsmod.OogaMod;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Crash-safe file writes via temp-file-then-atomic-rename with optional last-known-good backup.
 *
 * <p>Writes go to {@code target.tmp} in the same directory (same filesystem),
 * the data is flushed and fsynced, then an atomic {@link
 * Files#move(Path, Path, java.nio.file.CopyOption...) rename} commits the file.
 * If the JVM / OS crashes mid-write only the {@code .tmp} file is lost; the
 * original target (if any) is untouched.
 *
 * <p>The {@link #writeAtomicallyWithBackup(Path, ThrowingWriter)} variant also
 * preserves a {@code target.bak} as last-known-good insurance. If the atomic
 * rename itself fails (ENOSPC, disk failure), the backup is restored.
 */
public final class AtomicFileWriter {

    private AtomicFileWriter() {
    }

    @FunctionalInterface
    public interface ThrowingWriter {
        void write(BufferedWriter w) throws IOException;
    }

    /**
     * Writes content atomically to {@code target}.
     *
     * @throws IOException if writing or the atomic rename fails and no fallback succeeds
     */
    public static void writeAtomically(Path target, ThrowingWriter writer) throws IOException {
        doWriteAtomically(target, writer, false);
    }

    /**
     * Like {@link #writeAtomically(Path, ThrowingWriter)} but preserves a
     * {@code .bak} of the previous file as last-known-good insurance.
     * If the atomic rename fails the backup is restored.
     *
     * @throws IOException if writing or the atomic rename fails and no fallback succeeds
     */
    public static void writeAtomicallyWithBackup(Path target, ThrowingWriter writer) throws IOException {
        doWriteAtomically(target, writer, true);
    }

    private static void doWriteAtomically(Path target, ThrowingWriter writer, boolean keepBackup) throws IOException {
        Path tmp = siblingTemp(target);
        Path bak = siblingBak(target);
        Files.createDirectories(target.getParent());

        boolean movedToBak = false;
        if (keepBackup && Files.exists(target)) {
            try {
                Files.move(target, bak, REPLACE_EXISTING);
                movedToBak = true;
            } catch (IOException e) {
                OogaMod.LOGGER.warn("[otters_civ_revived/io] Could not create backup {} — continuing without", bak, e);
            }
        }

        try {
            try (BufferedWriter bw = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(bw);
                bw.flush();
                try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.READ)) {
                    ch.force(true);
                }
            }

            try {
                Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (UnsupportedOperationException e) {
                OogaMod.LOGGER.warn(
                        "[otters_civ_revived/io] ATOMIC_MOVE not supported for {} → {}, falling back to non-atomic replace",
                        tmp, target);
                Files.move(tmp, target, REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            if (movedToBak) {
                restoreBackup(target, bak);
            }
            throw e;
        }

        if (keepBackup) {
            try {
                Files.deleteIfExists(bak);
            } catch (IOException e) {
                OogaMod.LOGGER.warn("[otters_civ_revived/io] Could not remove backup {} — {}", bak, e.toString());
            }
        }
    }

    private static void restoreBackup(Path target, Path bak) {
        try {
            if (Files.exists(bak)) {
                Files.move(bak, target, REPLACE_EXISTING);
                OogaMod.LOGGER.info("[otters_civ_revived/io] Restored backup {} → {}", bak, target);
            }
        } catch (IOException e2) {
            OogaMod.LOGGER.error(
                    "[otters_civ_revived/io] Failed to restore backup {} → {} — data may be lost!",
                    bak, target, e2);
        }
    }

    /**
     * Deletes a stale {@code target.tmp} leftover from a previous crash,
     * logging the action. Safe to call even when the file does not exist.
     */
    public static void deleteStaleTemp(Path target) {
        Path tmp = siblingTemp(target);
        try {
            if (Files.deleteIfExists(tmp)) {
                OogaMod.LOGGER.info("[otters_civ_revived/io] Removed stale temp file {}", tmp);
            }
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[otters_civ_revived/io] Could not delete stale temp {} — {}", tmp, e.toString());
        }
    }

    static Path siblingTemp(Path target) {
        return target.resolveSibling(target.getFileName() + ".tmp");
    }

    static Path siblingBak(Path target) {
        return target.resolveSibling(target.getFileName() + ".bak");
    }
}
