package com.fpsmod.ottersciv.reward;

import net.minecraft.server.level.ServerPlayer;

/**
 * Extension point for jobs/professions.
 *
 * <p>Two stages on every passive payout:</p>
 * <ol>
 *   <li>{@link #multiplyPayout} — called <em>before</em> {@code wallets.addBalance(...)} so the
 *       returned amount is what the player actually gets. Default returns {@code basePayout}.</li>
 *   <li>{@link #onEconomyReward} — called <em>after</em> the credit lands. Use to award job XP,
 *       fire achievement triggers, etc. Default no-op.</li>
 * </ol>
 */
public interface JobsHooks {
    default long multiplyPayout(ServerPlayer player, RewardContext context, long basePayout) {
        return basePayout;
    }

    void onEconomyReward(ServerPlayer player, RewardContext context);

    JobsHooks NO_OP = (player, context) -> {
    };
}
