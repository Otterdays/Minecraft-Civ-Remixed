package com.fpsmod.ottersciv.reward;

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

import java.util.Optional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Otters Civ. Revived — single entry for paying players on block breaks and kills. */
public final class RewardOrchestrator {
    private final WalletService wallets;
    private final RewardRules rules;
    private final JobsHooks jobsHooks;
    private final ConcurrentHashMap<UUID, Long> lastBlockRewardMs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastEntityRewardMs = new ConcurrentHashMap<>();

    private volatile TagKey<net.minecraft.world.level.block.Block> blockTagKey;
    private volatile TagKey<EntityType<?>> entityTagKey;
    private volatile boolean loggedInvalidBlockTag;
    private volatile boolean loggedInvalidEntityTag;

    public RewardOrchestrator(WalletService wallets, RewardRules rules, JobsHooks jobsHooks) {
        this.wallets = wallets;
        this.rules = rules;
        this.jobsHooks = jobsHooks;
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

    public void onBlockBroken(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!rules.enabled || rules.blockReward <= 0L) {
            return;
        }
        if (!eligiblePlayer(player)) {
            return;
        }
        if (dimensionBlocked(level)) {
            return;
        }
        TagKey<net.minecraft.world.level.block.Block> tag = blockTagKey;
        if (tag == null) {
            if (!loggedInvalidBlockTag) {
                loggedInvalidBlockTag = true;
                com.fpsmod.FpsMod.LOGGER.warn("[otters_civ_revived] Invalid block tag {}; block rewards disabled", rules.blockTag);
            }
            return;
        }
        if (!state.is(tag)) {
            return;
        }
        if (!pastCooldown(lastBlockRewardMs, player.getUUID(), rules.blockCooldownMs)) {
            return;
        }

        wallets.addBalance(player.getUUID(), rules.blockReward);
        jobsHooks.onEconomyReward(player, new RewardContext(RewardReason.BLOCK_BREAK, rules.blockReward, state, null));

        if (rules.announceRewards) {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("+" + rules.blockReward + " (mining)")
            );
        }
    }

    public void onMobKilled(ServerPlayer killer, LivingEntity victim, ServerLevel level) {
        if (!rules.enabled || rules.entityReward <= 0L) {
            return;
        }
        if (!eligiblePlayer(killer)) {
            return;
        }
        if (dimensionBlocked(level)) {
            return;
        }
        TagKey<EntityType<?>> tag = entityTagKey;
        if (tag == null) {
            if (!loggedInvalidEntityTag) {
                loggedInvalidEntityTag = true;
                com.fpsmod.FpsMod.LOGGER.warn("[otters_civ_revived] Invalid entity tag {}; kill rewards disabled", rules.entityTag);
            }
            return;
        }

        EntityType<?> type = victim.getType();
        if (!entityMatchesKillTag(level, type, tag)) {
            return;
        }

        if (!pastCooldown(lastEntityRewardMs, killer.getUUID(), rules.entityCooldownMs)) {
            return;
        }

        wallets.addBalance(killer.getUUID(), rules.entityReward);
        jobsHooks.onEconomyReward(
            killer,
            new RewardContext(RewardReason.MOB_KILL, rules.entityReward, null, type)
        );

        if (rules.announceRewards) {
            killer.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("+" + rules.entityReward + " (combat)")
            );
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
        Optional<ResourceKey<EntityType<?>>> key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(type);
        if (key.isEmpty()) {
            return false;
        }
        return level.registryAccess()
            .lookupOrThrow(Registries.ENTITY_TYPE)
            .get(key.get())
            .map(holder -> holder.is(tag))
            .orElse(false);
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
