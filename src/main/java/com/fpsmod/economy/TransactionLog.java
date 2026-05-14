package com.fpsmod.economy;

import java.util.List;
import java.util.UUID;

/**
 * Audit log for every balance mutation. Append-only by contract.
 * Backed today by {@link FileTransactionLog} (CSV); future impls may use SQLite.
 */
public interface TransactionLog {
    /** Persist a single entry. Must be idempotent on best effort — never throw. */
    void record(LedgerEntry entry);

    /** Most recent {@code count} entries across all players, newest first. */
    default List<LedgerEntry> readRecent(int count) {
        return List.of();
    }

    /** Most recent {@code count} entries for a single player, newest first. */
    default List<LedgerEntry> readForPlayer(UUID playerId, int count) {
        return List.of();
    }

    TransactionLog NO_OP = entry -> {};
}
