package com.fpsmod.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Test double: in-memory balances + hints; tracks save call count. */
public final class FakeWalletStore implements WalletStore {
    private final Map<UUID, Long> backingBalances;
    private final Map<UUID, String> backingHints;
    int saveCount;

    public FakeWalletStore(Map<UUID, Long> initial) {
        this.backingBalances = new HashMap<>(initial);
        this.backingHints = new HashMap<>();
    }

    @Override
    public WalletLedger load() {
        return new WalletLedger(backingBalances, backingHints);
    }

    @Override
    public void save(Map<UUID, Long> balances, Map<UUID, String> displayHints) {
        saveCount++;
        backingBalances.clear();
        backingBalances.putAll(balances);
        backingHints.clear();
        backingHints.putAll(displayHints);
    }
}
