package com.fpsmod.economy;

import java.util.Map;
import java.util.UUID;

public interface WalletStore {
    Map<UUID, Long> load();

    void save(Map<UUID, Long> balances);
}
