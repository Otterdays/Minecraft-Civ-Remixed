package com.fpsmod.economy;

import com.fpsmod.OogaMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Appends one CSV line per entry to {@code config/otters_civ_revived/transactions.log}.
 * Format: timestamp,playerId,delta,balanceAfter,reason,note
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

    private static String sanitize(String note) {
        if (note == null) return "";
        return note.replace(",", ";").replace("\n", " ").replace("\r", "");
    }
}
