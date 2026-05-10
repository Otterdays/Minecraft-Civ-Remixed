package com.fpsmod.economy;

import java.util.Map;
import java.util.UUID;

/** Result of {@link WalletStore#load()}: balances plus human-readable name hints (not authoritative). */
public record WalletLedger(Map<UUID, Long> balances, Map<UUID, String> displayHints) {
    public WalletLedger {
        balances = Map.copyOf(balances);
        displayHints = Map.copyOf(displayHints);
    }

    public static WalletLedger empty() {
        return new WalletLedger(Map.of(), Map.of());
    }
}
