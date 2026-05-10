package com.fpsmod.economy;

import java.util.Map;
import java.util.UUID;

public interface WalletStore {

    /** Loads balances and optional display hints persisted as {@code # Name: ...} comment lines. */
    WalletLedger load();

    void save(Map<UUID, Long> balances, Map<UUID, String> displayHints);
}
