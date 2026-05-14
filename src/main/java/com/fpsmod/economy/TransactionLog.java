package com.fpsmod.economy;

public interface TransactionLog {
    void record(LedgerEntry entry);

    TransactionLog NO_OP = entry -> {};
}
