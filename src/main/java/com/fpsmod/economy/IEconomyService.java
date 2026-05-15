package com.fpsmod.economy;

import java.util.UUID;

public interface IEconomyService {
    long getBalance(UUID playerId);
    long setBalance(UUID playerId, long amount);
    long addBalance(UUID playerId, long delta);
    long addBalance(UUID playerId, long delta, String displayHintOrNull);
    long addBalance(UUID playerId, long delta, String displayHintOrNull, TransactionReason reason);
    void rememberPlayerName(UUID playerId, String plainName);
    void touchPlayerLabelForOps(UUID playerId, String plainName);
}
