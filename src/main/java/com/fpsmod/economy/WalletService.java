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
}
