package com.fpsmod.ottersciv.reward;

import net.minecraft.server.level.ServerPlayer;

/** Extension point for future jobs/professions; default no-op. */
public interface JobsHooks {
    void onEconomyReward(ServerPlayer player, RewardContext context);

    JobsHooks NO_OP = (player, context) -> {
    };
}
