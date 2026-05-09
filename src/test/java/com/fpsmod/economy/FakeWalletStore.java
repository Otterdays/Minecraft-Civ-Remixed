package com.fpsmod.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Test double: in-memory balances, tracks save call count. */
public final class FakeWalletStore implements WalletStore {
    private final Map<UUID, Long> backing;
    int saveCount;

    public FakeWalletStore(Map<UUID, Long> initial) {
        this.backing = new HashMap<>(initial);
    }

    @Override
    public Map<UUID, Long> load() {
        return new HashMap<>(backing);
    }

    @Override
    public void save(Map<UUID, Long> balances) {
        saveCount++;
        backing.clear();
        backing.putAll(balances);
    }
}
