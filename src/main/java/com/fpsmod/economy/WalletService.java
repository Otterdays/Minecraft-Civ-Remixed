package com.fpsmod.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService implements IEconomyService {

    public enum TransferResult { OK, INSUFFICIENT_FUNDS, SAME_PLAYER, RECEIVER_MAX_BALANCE }

    private final WalletStore walletStore;
    private final TransactionLog transactionLog;
    private final Map<UUID, Long> balances;
    /** Plain-text hints for operators; persisted as {@code # Name:} lines; UUID=balance stays authoritative. */
    private final Map<UUID, String> displayHints;
    private volatile EconomyConfig economyConfig;

    public WalletService(WalletStore walletStore) {
        this(walletStore, TransactionLog.NO_OP, EconomyConfig.defaults());
    }

    public WalletService(WalletStore walletStore, TransactionLog transactionLog) {
        this(walletStore, transactionLog, EconomyConfig.defaults());
    }

    public WalletService(WalletStore walletStore, TransactionLog transactionLog, EconomyConfig economyConfig) {
        this.walletStore = walletStore;
        this.transactionLog = transactionLog;
        this.economyConfig = economyConfig;
        WalletLedger loaded = walletStore.load();
        this.balances = new ConcurrentHashMap<>(loaded.balances());
        this.displayHints = new ConcurrentHashMap<>(loaded.displayHints());
    }

    public static WalletService createDefault() {
        return new WalletService(new FileWalletStore(), new FileTransactionLog(), EconomyConfig.defaults());
    }

    public EconomyConfig economyConfig() {
        return economyConfig;
    }

    public void setEconomyConfig(EconomyConfig config) {
        this.economyConfig = config;
    }

    /**
     * Stages an operator-visible name without writing disk (included on the next balance save).
     */
    public void rememberPlayerName(UUID playerId, String plainName) {
        String sanitized = FileWalletStore.sanitizeHintForStorage(plainName);
        if (!sanitized.isEmpty()) {
            displayHints.put(playerId, sanitized);
        }
    }

    /**
     * Updates the stored hint and persists immediately only if it changed.
     */
    public void touchPlayerLabelForOps(UUID playerId, String plainName) {
        String sanitized = FileWalletStore.sanitizeHintForStorage(plainName);
        if (sanitized.isEmpty()) {
            return;
        }
        String previous = displayHints.put(playerId, sanitized);
        if (!sanitized.equals(previous)) {
            persist();
        }
    }

    private synchronized void persist() {
        walletStore.save(Map.copyOf(balances), Map.copyOf(displayHints));
    }

    public long getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    /** Read-only access to the audit log for moderator views and reporting. */
    public TransactionLog transactionLog() {
        return transactionLog;
    }

    /** Admin: replace balance entirely. Logs {@link TransactionReason#ADMIN_SET}. */
    public long setBalance(UUID playerId, long amount) {
        long current = balances.getOrDefault(playerId, 0L);
        long sanitized = clampToBalanceCap(Math.max(0L, amount));
        balances.put(playerId, sanitized);
        persist();
        transactionLog.record(new LedgerEntry(playerId, sanitized - current, sanitized, TransactionReason.ADMIN_SET, null));
        return sanitized;
    }

    /** Admin: add coins to a player's balance. Logs {@link TransactionReason#ADMIN_ADD}. */
    public long adminAdd(UUID playerId, long amount) {
        BalanceChange change = applyDelta(playerId, Math.max(0L, amount));
        transactionLog.record(new LedgerEntry(playerId, change.delta(), change.next(), TransactionReason.ADMIN_ADD, null));
        return change.next();
    }

    /** Admin: deduct coins from a player's balance (clamped to 0). Logs {@link TransactionReason#ADMIN_TAKE}. */
    public long adminTake(UUID playerId, long amount) {
        long current = balances.getOrDefault(playerId, 0L);
        long taken = Math.min(amount, current);
        long next = current - taken;
        balances.put(playerId, next);
        persist();
        transactionLog.record(new LedgerEntry(playerId, -taken, next, TransactionReason.ADMIN_TAKE, null));
        return next;
    }

    /**
     * Player-to-player transfer. Atomic under this service's lock.
     * Logs {@link TransactionReason#PLAYER_PAY_SENT} and {@link TransactionReason#PLAYER_PAY_RECEIVED}.
     */
    public synchronized TransferResult transfer(UUID fromId, String fromName, UUID toId, String toName, long amount) {
        return transfer(fromId, fromName, toId, toName, amount, 0L);
    }

    /**
     * Player-to-player transfer with an optional sink fee.
     * The recipient always receives {@code amount}; the sender pays {@code amount + fee}.
     */
    public synchronized TransferResult transfer(
        UUID fromId,
        String fromName,
        UUID toId,
        String toName,
        long amount,
        long fee
    ) {
        if (fromId.equals(toId)) return TransferResult.SAME_PLAYER;
        long safeAmount = Math.max(0L, amount);
        long safeFee = Math.max(0L, fee);
        long totalCost;
        try {
            totalCost = Math.addExact(safeAmount, safeFee);
        } catch (ArithmeticException e) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }
        long fromBalance = balances.getOrDefault(fromId, 0L);
        if (fromBalance < totalCost) return TransferResult.INSUFFICIENT_FUNDS;

        long currentTo = balances.getOrDefault(toId, 0L);
        if (wouldExceedBalanceCap(currentTo, safeAmount)) {
            return TransferResult.RECEIVER_MAX_BALANCE;
        }
        long balanceAfterAmount = fromBalance - safeAmount;
        long newFrom = balanceAfterAmount - safeFee;
        long newTo = saturatingAdd(currentTo, safeAmount);

        balances.put(fromId, newFrom);
        balances.put(toId, newTo);
        if (fromName != null) rememberPlayerName(fromId, fromName);
        if (toName != null) rememberPlayerName(toId, toName);
        persist();
        transactionLog.record(new LedgerEntry(fromId, -safeAmount, balanceAfterAmount, TransactionReason.PLAYER_PAY_SENT, "to:" + toId));
        transactionLog.record(new LedgerEntry(toId, safeAmount, newTo, TransactionReason.PLAYER_PAY_RECEIVED, "from:" + fromId));
        if (safeFee > 0L) {
            transactionLog.record(new LedgerEntry(fromId, -safeFee, newFrom, TransactionReason.PLAYER_PAY_FEE, "to:" + toId));
        }
        return TransferResult.OK;
    }

    /**
     * Adds delta to the player's balance (negative subtracts). Result is clamped to >= 0.
     * Overflow toward positive infinity caps at {@link Long#MAX_VALUE}.
     * Logs with the provided reason.
     */
    public long addBalance(UUID playerId, long delta, String displayHintOrNull, TransactionReason reason) {
        if (displayHintOrNull != null) {
            String s = FileWalletStore.sanitizeHintForStorage(displayHintOrNull);
            if (!s.isEmpty()) {
                displayHints.put(playerId, s);
            }
        }
        BalanceChange change = applyDelta(playerId, delta);
        transactionLog.record(new LedgerEntry(playerId, change.delta(), change.next(), reason, null));
        return change.next();
    }

    /**
     * Adds delta to the player's balance. Logs as {@link TransactionReason#REWARD_BLOCK}.
     */
    public long addBalance(UUID playerId, long delta) {
        return addBalance(playerId, delta, null);
    }

    /**
     * Like {@link #addBalance(UUID, long)} but merges a display hint in the same save.
     * Logs as {@link TransactionReason#REWARD_BLOCK}.
     */
    public long addBalance(UUID playerId, long delta, String displayHintOrNull) {
        return addBalance(playerId, delta, displayHintOrNull, TransactionReason.REWARD_BLOCK);
    }

    private BalanceChange applyDelta(UUID playerId, long delta) {
        long current = balances.getOrDefault(playerId, 0L);
        long next = clampToBalanceCap(Math.max(0L, saturatingAdd(current, delta)));
        balances.put(playerId, next);
        persist();
        return new BalanceChange(current, next);
    }

    private long clampToBalanceCap(long amount) {
        long cap = economyConfig.maxBalance;
        if (cap > 0L && amount > cap) {
            return cap;
        }
        return amount;
    }

    private boolean wouldExceedBalanceCap(long currentBalance, long delta) {
        long cap = economyConfig.maxBalance;
        if (cap <= 0L) {
            return false;
        }
        if (currentBalance >= cap) {
            return delta > 0L;
        }
        return delta > cap - currentBalance;
    }

    private long saturatingAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            return right > 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    private record BalanceChange(long previous, long next) {
        private long delta() {
            return next - previous;
        }
    }
}
