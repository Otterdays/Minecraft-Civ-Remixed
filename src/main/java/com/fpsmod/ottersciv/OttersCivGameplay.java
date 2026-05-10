package com.fpsmod.ottersciv;

import com.fpsmod.economy.WalletService;
import com.fpsmod.ottersciv.config.RewardRules;
import com.fpsmod.ottersciv.reward.JobsHooks;
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
    public static RewardOrchestrator register(WalletService wallets, RewardRules rules) {
        RewardOrchestrator orchestrator = new RewardOrchestrator(wallets, rules, JobsHooks.NO_OP);

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) {
                return;
            }
            orchestrator.onBlockBroken(sp, level, pos, state);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) ->
            rewardKill(orchestrator, entity, damageSource)
        );

        JoinWelcome.register();

        return orchestrator;
    }

    private static void rewardKill(RewardOrchestrator orchestrator, LivingEntity entity, DamageSource damageSource) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Player killerPlayer = resolveAttackingPlayer(damageSource);
        if (!(killerPlayer instanceof ServerPlayer killer)) {
            return;
        }
        orchestrator.onMobKilled(killer, entity, level);
    }

    /** Direct player entity on the damage source (not indirect / environmental). */
    private static Player resolveAttackingPlayer(DamageSource damageSource) {
        return damageSource.getEntity() instanceof Player player ? player : null;
    }
}
