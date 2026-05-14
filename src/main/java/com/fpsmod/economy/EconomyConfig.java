package com.fpsmod.economy;

/**
 * Server-tunable economy policy. Loaded from {@code config/otters_civ_revived/economy.json}.
 * Operator-friendly: missing fields fall back to defaults.
 */
public final class EconomyConfig {
    /** Maximum coins a single /pay command can move. {@code 0} disables the cap. */
    public long maxTransferPerCommand = 100_000L;

    /** Per-sender cooldown between successful /pay commands, in seconds. {@code 0} disables. */
    public int transferCooldownSeconds = 3;

    /** Flat fee deducted from the sender on a successful /pay (in addition to the amount). {@code 0} disables. */
    public long transferFlatFee = 0L;

    public static EconomyConfig defaults() {
        return new EconomyConfig();
    }

    /** Copy field values from {@code other} into this instance (used by reload to update in-place). */
    public void copyFrom(EconomyConfig other) {
        this.maxTransferPerCommand = other.maxTransferPerCommand;
        this.transferCooldownSeconds = other.transferCooldownSeconds;
        this.transferFlatFee = other.transferFlatFee;
    }
}
