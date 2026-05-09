package com.fpsmod.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService {
    private final WalletStore walletStore;
    private final Map<UUID, Long> balances;

    public WalletService(WalletStore walletStore) {
        this.walletStore = walletStore;
        this.balances = new ConcurrentHashMap<>(walletStore.load());
    }

    public static WalletService createDefault() {
        return new WalletService(new FileWalletStore());
    }

    public long getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    public long setBalance(UUID playerId, long amount) {
        long sanitized = Math.max(0L, amount);
        balances.put(playerId, sanitized);
        walletStore.save(balances);
        return sanitized;
    }

    /**
     * Adds delta to the player's balance (negative subtracts). Result is clamped to >= 0.
     * Overflow toward positive infinity caps at {@link Long#MAX_VALUE}.
     */
    public long addBalance(UUID playerId, long delta) {
        long current = balances.getOrDefault(playerId, 0L);
        long next;
        try {
            next = Math.addExact(current, delta);
        } catch (ArithmeticException e) {
            next = delta > 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        next = Math.max(0L, next);
        balances.put(playerId, next);
        walletStore.save(balances);
        return next;
    }
}
