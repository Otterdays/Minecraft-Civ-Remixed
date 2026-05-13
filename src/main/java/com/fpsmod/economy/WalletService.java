package com.fpsmod.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService {
    private final WalletStore walletStore;
    private final Map<UUID, Long> balances;
    /** Plain-text hints for operators; persisted as {@code # Name:} lines; UUID=balance stays authoritative. */
    private final Map<UUID, String> displayHints;

    public WalletService(WalletStore walletStore) {
        this.walletStore = walletStore;
        WalletLedger loaded = walletStore.load();
        this.balances = new ConcurrentHashMap<>(loaded.balances());
        this.displayHints = new ConcurrentHashMap<>(loaded.displayHints());
    }

    public static WalletService createDefault() {
        return new WalletService(new FileWalletStore());
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
     * Updates the stored hint and persists immediately only if it changed — for join or read-only chatter
     * that still should refresh labels on disk.
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

    public long setBalance(UUID playerId, long amount) {
        long sanitized = Math.max(0L, amount);
        balances.put(playerId, sanitized);
        persist();
        return sanitized;
    }

    /**
     * Adds delta to the player's balance (negative subtracts). Result is clamped to >= 0.
     * Overflow toward positive infinity caps at {@link Long#MAX_VALUE}.
     */
    public long addBalance(UUID playerId, long delta) {
        return addBalance(playerId, delta, null);
    }

    /**
     * Like {@link #addBalance(UUID, long)} but merges a display hint (e.g. in-game display name)
     * in the same save.
     */
    public long addBalance(UUID playerId, long delta, String displayHintOrNull) {
        if (displayHintOrNull != null) {
            String s = FileWalletStore.sanitizeHintForStorage(displayHintOrNull);
            if (!s.isEmpty()) {
                displayHints.put(playerId, s);
            }
        }

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
