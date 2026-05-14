package com.fpsmod.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService {

    public enum TransferResult { OK, INSUFFICIENT_FUNDS, SAME_PLAYER }

    private final WalletStore walletStore;
    private final TransactionLog transactionLog;
    private final Map<UUID, Long> balances;
    /** Plain-text hints for operators; persisted as {@code # Name:} lines; UUID=balance stays authoritative. */
    private final Map<UUID, String> displayHints;

    public WalletService(WalletStore walletStore) {
        this(walletStore, TransactionLog.NO_OP);
    }

    public WalletService(WalletStore walletStore, TransactionLog transactionLog) {
        this.walletStore = walletStore;
        this.transactionLog = transactionLog;
        WalletLedger loaded = walletStore.load();
        this.balances = new ConcurrentHashMap<>(loaded.balances());
        this.displayHints = new ConcurrentHashMap<>(loaded.displayHints());
    }

    public static WalletService createDefault() {
        return new WalletService(new FileWalletStore(), new FileTransactionLog());
    }

    /**
     * Stages an operator-visible name without writing disk (included on the next balance save).
     */
    public void rememberPlayerName(UUID playerId, String plainName) {
        String sanitized = FileWalletStore.sanitizeHintForStorage(plainName);
        if (!sanitized.isEmpty()) {
            displayHints.put(playerId, sanitized);
        }
    }

    /**
     * Updates the stored hint and persists immediately only if it changed.
     */
    public void touchPlayerLabelForOps(UUID playerId, String plainName) {
        String sanitized = FileWalletStore.sanitizeHintForStorage(plainName);
        if (sanitized.isEmpty()) {
            return;
        }
        String previous = displayHints.put(playerId, sanitized);
        if (!sanitized.equals(previous)) {
            persist();
        }
    }

    private synchronized void persist() {
        walletStore.save(Map.copyOf(balances), Map.copyOf(displayHints));
    }

    public long getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    /** Admin: replace balance entirely. Logs {@link TransactionReason#ADMIN_SET}. */
    public long setBalance(UUID playerId, long amount) {
        long sanitized = Math.max(0L, amount);
        balances.put(playerId, sanitized);
        persist();
        transactionLog.record(new LedgerEntry(playerId, sanitized, sanitized, TransactionReason.ADMIN_SET, null));
        return sanitized;
    }

    /** Admin: add coins to a player's balance. Logs {@link TransactionReason#ADMIN_ADD}. */
    public long adminAdd(UUID playerId, long amount) {
        long next = applyDelta(playerId, Math.max(0L, amount));
        transactionLog.record(new LedgerEntry(playerId, amount, next, TransactionReason.ADMIN_ADD, null));
        return next;
    }

    /** Admin: deduct coins from a player's balance (clamped to 0). Logs {@link TransactionReason#ADMIN_TAKE}. */
    public long adminTake(UUID playerId, long amount) {
        long current = balances.getOrDefault(playerId, 0L);
        long taken = Math.min(amount, current);
        long next = current - taken;
        balances.put(playerId, next);
        persist();
        transactionLog.record(new LedgerEntry(playerId, -taken, next, TransactionReason.ADMIN_TAKE, null));
        return next;
    }

    /**
     * Player-to-player transfer. Atomic under this service's lock.
     * Logs {@link TransactionReason#PLAYER_PAY_SENT} and {@link TransactionReason#PLAYER_PAY_RECEIVED}.
     */
    public synchronized TransferResult transfer(UUID fromId, String fromName, UUID toId, String toName, long amount) {
        if (fromId.equals(toId)) return TransferResult.SAME_PLAYER;
        long fromBalance = balances.getOrDefault(fromId, 0L);
        if (fromBalance < amount) return TransferResult.INSUFFICIENT_FUNDS;

        long newFrom = fromBalance - amount;
        long currentTo = balances.getOrDefault(toId, 0L);
        long newTo;
        try {
            newTo = Math.addExact(currentTo, amount);
        } catch (ArithmeticException e) {
            newTo = Long.MAX_VALUE;
        }

        balances.put(fromId, newFrom);
        balances.put(toId, newTo);
        if (fromName != null) rememberPlayerName(fromId, fromName);
        if (toName != null) rememberPlayerName(toId, toName);
        persist();
        transactionLog.record(new LedgerEntry(fromId, -amount, newFrom, TransactionReason.PLAYER_PAY_SENT, "to:" + toId));
        transactionLog.record(new LedgerEntry(toId, amount, newTo, TransactionReason.PLAYER_PAY_RECEIVED, "from:" + fromId));
        return TransferResult.OK;
    }

    /**
     * Adds delta to the player's balance (negative subtracts). Result is clamped to >= 0.
     * Overflow toward positive infinity caps at {@link Long#MAX_VALUE}.
     * Logs with the provided reason.
     */
    public long addBalance(UUID playerId, long delta, String displayHintOrNull, TransactionReason reason) {
        if (displayHintOrNull != null) {
            String s = FileWalletStore.sanitizeHintForStorage(displayHintOrNull);
            if (!s.isEmpty()) {
                displayHints.put(playerId, s);
            }
        }
        long next = applyDelta(playerId, delta);
        transactionLog.record(new LedgerEntry(playerId, delta, next, reason, null));
        return next;
    }

    /**
     * Adds delta to the player's balance. Logs as {@link TransactionReason#REWARD_BLOCK}.
     */
    public long addBalance(UUID playerId, long delta) {
        return addBalance(playerId, delta, null);
    }

    /**
     * Like {@link #addBalance(UUID, long)} but merges a display hint in the same save.
     * Logs as {@link TransactionReason#REWARD_BLOCK}.
     */
    public long addBalance(UUID playerId, long delta, String displayHintOrNull) {
        return addBalance(playerId, delta, displayHintOrNull, TransactionReason.REWARD_BLOCK);
    }

    private long applyDelta(UUID playerId, long delta) {
        long current = balances.getOrDefault(playerId, 0L);
        long next;
        try {
            next = Math.addExact(current, delta);
        } catch (ArithmeticException e) {
            next = delta > 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        next = Math.max(0L, next);
        balances.put(playerId, next);
        persist();
        return next;
    }
}
