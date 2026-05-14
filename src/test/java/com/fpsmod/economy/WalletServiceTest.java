package com.fpsmod.economy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletServiceTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void addBalanceIncrementsAndPersists() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>());
        WalletService svc = new WalletService(store);

        assertEquals(10L, svc.addBalance(ID, 10L));
        assertEquals(10L, svc.getBalance(ID));
        assertEquals(1, store.saveCount);

        assertEquals(15L, svc.addBalance(ID, 5L));
        assertEquals(2, store.saveCount);
    }

    @Test
    void addBalanceSubtractsAndClampsAtZero() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>(Map.of(ID, 3L)));
        WalletService svc = new WalletService(store);

        assertEquals(0L, svc.addBalance(ID, -10L));
        assertEquals(0L, svc.getBalance(ID));
    }

    @Test
    void addBalanceCapsOnOverflow() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>(Map.of(ID, Long.MAX_VALUE)));
        WalletService svc = new WalletService(store);

        assertEquals(Long.MAX_VALUE, svc.addBalance(ID, 1L));
    }

    @Test
    void setBalanceStillReplacesAndSaves() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>());
        WalletService svc = new WalletService(store);
        svc.setBalance(ID, 99L);

        assertEquals(99L, svc.getBalance(ID));
        assertEquals(1, store.saveCount);
    }

    @Test
    void addBalanceWithHintStoresDisplayLabel() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>());
        WalletService svc = new WalletService(store);

        svc.addBalance(ID, 10L, "PlayerOne");
        WalletLedger after = store.load();
        assertEquals(10L, after.balances().get(ID).longValue());
        assertEquals("PlayerOne", after.displayHints().get(ID));
    }

    @Test
    void setBalanceHonorsMaxBalance() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>());
        FakeTransactionLog log = new FakeTransactionLog();
        EconomyConfig cfg = EconomyConfig.defaults();
        cfg.maxBalance = 50L;
        WalletService svc = new WalletService(store, log, cfg);

        svc.setBalance(ID, 99L);

        assertEquals(50L, svc.getBalance(ID));
        assertEquals(50L, log.entries().getFirst().delta());
    }

    @Test
    void addBalanceLogsActualDeltaWhenCapClamps() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>(Map.of(ID, 45L)));
        FakeTransactionLog log = new FakeTransactionLog();
        EconomyConfig cfg = EconomyConfig.defaults();
        cfg.maxBalance = 50L;
        WalletService svc = new WalletService(store, log, cfg);

        assertEquals(50L, svc.addBalance(ID, 10L));
        assertEquals(5L, log.entries().getFirst().delta());
        assertEquals(50L, log.entries().getFirst().balanceAfter());
    }

    @Test
    void transferWithFeeLogsFeeAndMovesMoneyAtomically() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>(Map.of(ID, 100L, OTHER_ID, 5L)));
        FakeTransactionLog log = new FakeTransactionLog();
        WalletService svc = new WalletService(store, log, EconomyConfig.defaults());

        assertEquals(
            WalletService.TransferResult.OK,
            svc.transfer(ID, "Alice", OTHER_ID, "Bob", 10L, 2L)
        );
        assertEquals(88L, svc.getBalance(ID));
        assertEquals(15L, svc.getBalance(OTHER_ID));
        assertEquals(TransactionReason.PLAYER_PAY_SENT, log.entries().get(0).reason());
        assertEquals(TransactionReason.PLAYER_PAY_RECEIVED, log.entries().get(1).reason());
        assertEquals(TransactionReason.PLAYER_PAY_FEE, log.entries().get(2).reason());
        assertEquals(-2L, log.entries().get(2).delta());
    }

    @Test
    void transferRejectsReceiverCapWithoutChangingBalances() {
        FakeWalletStore store = new FakeWalletStore(new HashMap<>(Map.of(ID, 100L, OTHER_ID, 45L)));
        FakeTransactionLog log = new FakeTransactionLog();
        EconomyConfig cfg = EconomyConfig.defaults();
        cfg.maxBalance = 50L;
        WalletService svc = new WalletService(store, log, cfg);

        assertEquals(
            WalletService.TransferResult.RECEIVER_MAX_BALANCE,
            svc.transfer(ID, "Alice", OTHER_ID, "Bob", 10L, 0L)
        );
        assertEquals(100L, svc.getBalance(ID));
        assertEquals(45L, svc.getBalance(OTHER_ID));
        assertEquals(0, log.entries().size());
    }

    private static final class FakeTransactionLog implements TransactionLog {
        private final List<LedgerEntry> entries = new ArrayList<>();

        @Override
        public void record(LedgerEntry entry) {
            entries.add(entry);
        }

        private List<LedgerEntry> entries() {
            return entries;
        }
    }
}
