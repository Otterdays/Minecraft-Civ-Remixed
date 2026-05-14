package com.fpsmod.economy;

/**
 * Server-tunable economy policy. Loaded from {@code config/otters_civ_revived/economy.json}.
 * Operator-friendly: missing fields fall back to defaults.
 */
public final class EconomyConfig {
    // --- Display ---

    /** Symbol prepended to coin amounts in chat messages. */
    public String currencySymbol = "$";

    /** Singular name for the currency (used in messages). */
    public String currencyName = "coin";

    /** Plural name for the currency (used in messages). */
    public String currencyNamePlural = "coins";

    // --- Starting balance ---

    /** Coins awarded to a player the first time they join this server. {@code 0} disables. */
    public long newPlayerStartingBalance = 0L;

    // --- Balance cap ---

    /** Hard ceiling on any player's balance. {@code 0} means no cap. */
    public long maxBalance = 0L;

    // --- /pay transfer rules ---

    /** Maximum coins a single /pay command can move. {@code 0} disables the cap. */
    public long maxTransferPerCommand = 100_000L;

    /** Minimum coins a /pay command must move. Prevents spam with tiny amounts. */
    public long minTransferAmount = 1L;

    /** Per-sender cooldown between successful /pay commands, in seconds. {@code 0} disables. */
    public int transferCooldownSeconds = 3;

    /** Flat fee deducted from the sender on a successful /pay (in addition to the amount). {@code 0} disables. */
    public long transferFlatFee = 0L;

    /** Whether a player can /pay themselves. Almost always {@code false}. */
    public boolean allowSelfPay = false;

    // --- Join welcome ---

    /** Show the welcome message when a player joins. */
    public boolean showJoinWelcome = true;

    public static EconomyConfig defaults() {
        return new EconomyConfig();
    }

    /** Formatted coin amount using this config's symbol. */
    public String format(long amount) {
        return currencySymbol + amount;
    }

    /** Copy field values from {@code other} into this instance (used by reload to update in-place). */
    public void copyFrom(EconomyConfig other) {
        this.currencySymbol = other.currencySymbol;
        this.currencyName = other.currencyName;
        this.currencyNamePlural = other.currencyNamePlural;
        this.newPlayerStartingBalance = other.newPlayerStartingBalance;
        this.maxBalance = other.maxBalance;
        this.maxTransferPerCommand = other.maxTransferPerCommand;
        this.minTransferAmount = other.minTransferAmount;
        this.transferCooldownSeconds = other.transferCooldownSeconds;
        this.transferFlatFee = other.transferFlatFee;
        this.allowSelfPay = other.allowSelfPay;
        this.showJoinWelcome = other.showJoinWelcome;
    }
}
