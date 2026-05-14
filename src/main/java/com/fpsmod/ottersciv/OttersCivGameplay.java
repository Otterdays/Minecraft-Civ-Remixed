package com.fpsmod.ottersciv;

import com.fpsmod.economy.WalletService;
import com.fpsmod.jobs.JobEventContext;
import com.fpsmod.jobs.JobsService;
import com.fpsmod.ottersciv.config.RewardRules;
import com.fpsmod.ottersciv.reward.RewardOrchestrator;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class OttersCivGameplay {
    private OttersCivGameplay() {
    }

    /** Otters Civ. Revived gameplay rewards (wired from main mod initializer). */
    public static RewardOrchestrator register(WalletService wallets, RewardRules rules, JobsService jobsService) {
        RewardOrchestrator orchestrator = new RewardOrchestrator(wallets, rules, jobsService);

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) {
                return;
            }
            long payout = orchestrator.onBlockBroken(sp, level, pos, state);
            jobsService.onGameplayEvent(sp, JobEventContext.forBlockBreak(sp, level, state, payout > 0L));
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) ->
            rewardKill(orchestrator, jobsService, entity, damageSource)
        );

        JoinWelcome.register(wallets);

        return orchestrator;
    }

    private static void rewardKill(
        RewardOrchestrator orchestrator,
        JobsService jobsService,
        LivingEntity entity,
        DamageSource damageSource
    ) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Player killerPlayer = resolveAttackingPlayer(damageSource);
        if (!(killerPlayer instanceof ServerPlayer killer)) {
            return;
        }
        long payout = orchestrator.onMobKilled(killer, entity, level);
        jobsService.onGameplayEvent(
            killer,
            JobEventContext.forMobKill(killer, level, entity.getType(), payout > 0L, true)
        );
    }

    /** Direct player entity on the damage source (not indirect / environmental). */
    private static Player resolveAttackingPlayer(DamageSource damageSource) {
        return damageSource.getEntity() instanceof Player player ? player : null;
    }
}
