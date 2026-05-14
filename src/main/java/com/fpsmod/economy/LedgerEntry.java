package com.fpsmod.economy;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
    Instant timestamp,
    UUID playerId,
    long delta,
    long balanceAfter,
    TransactionReason reason,
    String note
) {
    public LedgerEntry(UUID playerId, long delta, long balanceAfter, TransactionReason reason, String note) {
        this(Instant.now(), playerId, delta, balanceAfter, reason, note);
    }
}
