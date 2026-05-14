package com.fpsmod.ottersciv.reward;

import com.fpsmod.jobs.JobEventContext;
import com.fpsmod.jobs.JobsService;
import com.fpsmod.ottersciv.config.RewardRules;
import com.fpsmod.economy.WalletService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.Objects;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Otters Civ. Revived — single entry for paying players on block breaks and kills. */
public final class RewardOrchestrator {
    private final WalletService wallets;
    private volatile RewardRules rules;
    private final JobsService jobsService;
    private final ConcurrentHashMap<UUID, Long> lastBlockRewardMs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastEntityRewardMs = new ConcurrentHashMap<>();

    private volatile TagKey<net.minecraft.world.level.block.Block> blockTagKey;
    private volatile TagKey<EntityType<?>> entityTagKey;
    private volatile boolean loggedInvalidBlockTag;
    private volatile boolean loggedInvalidEntityTag;

    public RewardOrchestrator(WalletService wallets, RewardRules rules, JobsService jobsService) {
        this.wallets = wallets;
        this.rules = Objects.requireNonNull(rules, "reward rules");
        this.jobsService = jobsService;
        refreshTagKeys();
    }

    /** Applies rules produced after registry/tag load (expanded per-id payouts on disk when needed). */
    public void replaceRules(RewardRules next) {
        this.rules = Objects.requireNonNull(next, "reward rules");
        this.loggedInvalidBlockTag = false;
        this.loggedInvalidEntityTag = false;
        refreshTagKeys();
    }

    /** Re-resolve tag keys after config reload (future); call when rules reference changes. */
    public void refreshTagKeys() {
        this.blockTagKey = parseTag(Registries.BLOCK, rules.blockTag);
        this.entityTagKey = parseTag(Registries.ENTITY_TYPE, rules.entityTag);
    }

    private static <T> TagKey<T> parseTag(
        ResourceKey<net.minecraft.core.Registry<T>> registryKey,
        String id
    ) {
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null) {
            return null;
        }
        return TagKey.create(registryKey, parsed);
    }

    public long onBlockBroken(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!rules.enabled) {
            return 0L;
        }
        if (!eligiblePlayer(player)) {
            return 0L;
        }
        if (dimensionBlocked(level)) {
            return 0L;
        }

        long basePayout = resolvedBlockReward(state);
        if (basePayout <= 0L) {
            return 0L;
        }
        if (!pastCooldown(lastBlockRewardMs, player.getUUID(), rules.blockCooldownMs)) {
            return 0L;
        }

        JobEventContext eventContext = JobEventContext.forBlockBreak(player, level, state, true);
        long payout = Math.max(0L, jobsService.modifyPayout(player, eventContext, basePayout));

        wallets.addBalance(player.getUUID(), payout, player.getName().getString());
        if (rules.announceRewards) {
            sendCoinMessage(player, payout);
        }
        return payout;
    }

    private long resolvedBlockReward(BlockState state) {
        Identifier bid = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String bidStr = bid != null ? bid.toString() : null;
        java.util.Map<String, Long> byId = rules.blockRewards;
        if (bidStr != null && byId != null && byId.containsKey(bidStr)) {
            return Math.max(0L, byId.get(bidStr));
        }

        TagKey<net.minecraft.world.level.block.Block> tag = blockTagKey;
        if (tag == null) {
            warnInvalidBlockTagOnce();
            return 0L;
        }
        if (!state.is(tag)) {
            return 0L;
        }
        return Math.max(0L, rules.blockReward);
    }

    private void warnInvalidBlockTagOnce() {
        if (!loggedInvalidBlockTag) {
            loggedInvalidBlockTag = true;
            if (rules.blockReward > 0L) {
                com.fpsmod.OogaMod.LOGGER.warn(
                    "[otters_civ_revived] Invalid block tag {}; mining fallback via blockTag/blockReward disabled",
                    rules.blockTag
                );
            }
        }
    }

    public long onMobKilled(ServerPlayer killer, LivingEntity victim, ServerLevel level) {
        if (!rules.enabled) {
            return 0L;
        }
        if (!eligiblePlayer(killer)) {
            return 0L;
        }
        if (dimensionBlocked(level)) {
            return 0L;
        }

        EntityType<?> type = victim.getType();
        long basePayout = resolvedEntityReward(level, type);
        if (basePayout <= 0L) {
            return 0L;
        }

        if (!pastCooldown(lastEntityRewardMs, killer.getUUID(), rules.entityCooldownMs)) {
            return 0L;
        }

        JobEventContext eventContext = JobEventContext.forMobKill(killer, level, type, true, true);
        long payout = Math.max(0L, jobsService.modifyPayout(killer, eventContext, basePayout));

        wallets.addBalance(killer.getUUID(), payout, killer.getName().getString());
        if (rules.announceRewards) {
            sendCoinMessage(killer, payout);
        }
        return payout;
    }

    static String coinMessageText(long payout) {
        return "+" + payout + " coins";
    }

    private static void sendCoinMessage(ServerPlayer player, long payout) {
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal(coinMessageText(payout))
        );
    }

    private long resolvedEntityReward(ServerLevel level, EntityType<?> type) {
        Identifier tid = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String tidStr = tid != null ? tid.toString() : null;
        java.util.Map<String, Long> byId = rules.entityRewards;
        if (tidStr != null && byId != null && byId.containsKey(tidStr)) {
            return Math.max(0L, byId.get(tidStr));
        }

        TagKey<EntityType<?>> tag = entityTagKey;
        if (tag == null) {
            warnInvalidEntityTagOnce();
            return 0L;
        }
        if (!entityMatchesKillTag(level, type, tag)) {
            return 0L;
        }
        return Math.max(0L, rules.entityReward);
    }

    private void warnInvalidEntityTagOnce() {
        if (!loggedInvalidEntityTag) {
            loggedInvalidEntityTag = true;
            if (rules.entityReward > 0L) {
                com.fpsmod.OogaMod.LOGGER.warn(
                    "[otters_civ_revived] Invalid entity tag {}; combat fallback via entityTag/entityReward disabled",
                    rules.entityTag
                );
            }
        }
    }

    private boolean dimensionBlocked(Level level) {
        if (rules.dimensionBlacklist == null || rules.dimensionBlacklist.isEmpty()) {
            return false;
        }
        Identifier dimId = level.dimension().identifier();
        return rules.dimensionBlacklist.contains(dimId.toString());
    }

    private boolean eligiblePlayer(ServerPlayer player) {
        if (rules.skipCreative && player.isCreative()) {
            return false;
        }
        return !rules.skipSpectator || !player.isSpectator();
    }

    private static boolean entityMatchesKillTag(ServerLevel level, EntityType<?> type, TagKey<EntityType<?>> tag) {
        return KillRewardTagChecks.isEntityTypeTagged(level, type, tag);
    }

    /**
     * Cooldown gate: returns false if called again within {@code cooldownMs} for the same player.
     */
    private boolean pastCooldown(ConcurrentHashMap<UUID, Long> map, UUID id, long cooldownMs) {
        if (cooldownMs <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long prev = map.get(id);
        if (prev != null && now - prev < cooldownMs) {
            return false;
        }
        map.put(id, now);
        return true;
    }
}
