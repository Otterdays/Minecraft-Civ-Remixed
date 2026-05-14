package com.fpsmod.economy;

import com.fpsmod.OogaMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Appends one CSV line per entry to {@code config/otters_civ_revived/transactions.log}.
 * Format: {@code timestamp,playerId,delta,balanceAfter,reason,note}
 * Append-only; never truncated by this class.
 */
public class FileTransactionLog implements TransactionLog {
    private static final String CONFIG_FOLDER = "otters_civ_revived";
    private static final String FILE_NAME = "transactions.log";

    private final Path logPath;

    public FileTransactionLog() {
        this.logPath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(CONFIG_FOLDER)
            .resolve(FILE_NAME);
    }

    @Override
    public synchronized void record(LedgerEntry entry) {
        try {
            Files.createDirectories(logPath.getParent());
            String line = String.join(",",
                entry.timestamp().toString(),
                entry.playerId().toString(),
                Long.toString(entry.delta()),
                Long.toString(entry.balanceAfter()),
                entry.reason().name(),
                sanitize(entry.note())
            ) + System.lineSeparator();
            Files.writeString(logPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[economy] Failed to write transaction log entry: {}", e.getMessage());
        }
    }

    @Override
    public synchronized List<LedgerEntry> readRecent(int count) {
        return readMatching(count, entry -> true);
    }

    @Override
    public synchronized List<LedgerEntry> readForPlayer(UUID playerId, int count) {
        return readMatching(count, entry -> entry.playerId().equals(playerId));
    }

    /**
     * Streams the log from end-of-file backward into a bounded ring buffer.
     * For our current file scale (one line per balance change) a forward scan is fine;
     * when this grows we can swap in a reverse reader without breaking the interface contract.
     */
    private List<LedgerEntry> readMatching(int count, Predicate<LedgerEntry> filter) {
        if (count <= 0 || !Files.exists(logPath)) return List.of();
        Deque<LedgerEntry> ring = new ArrayDeque<>(count);
        try {
            for (String line : Files.readAllLines(logPath)) {
                LedgerEntry parsed = parse(line);
                if (parsed == null) continue;
                if (!filter.test(parsed)) continue;
                if (ring.size() == count) ring.removeFirst();
                ring.addLast(parsed);
            }
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[economy] Failed to read transaction log: {}", e.getMessage());
            return List.of();
        }
        // Newest first: reverse the FIFO ring (which holds oldest→newest).
        List<LedgerEntry> result = new ArrayList<>(ring);
        java.util.Collections.reverse(result);
        return List.copyOf(result);
    }

    private static LedgerEntry parse(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split(",", 6);
        if (parts.length < 5) return null;
        try {
            Instant ts = Instant.parse(parts[0]);
            UUID player = UUID.fromString(parts[1]);
            long delta = Long.parseLong(parts[2]);
            long balanceAfter = Long.parseLong(parts[3]);
            TransactionReason reason = TransactionReason.valueOf(parts[4]);
            String note = parts.length >= 6 ? parts[5] : "";
            return new LedgerEntry(ts, player, delta, balanceAfter, reason, note);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return null;
        }
    }

    private static String sanitize(String note) {
        if (note == null) return "";
        return note.replace(",", ";").replace("\n", " ").replace("\r", "");
    }
}
