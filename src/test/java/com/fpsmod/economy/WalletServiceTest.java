package com.fpsmod.economy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletServiceTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
}
