package com.fpsmod.ottersciv.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Otters Civ. Revived gameplay rewards; loaded from Gson with defaults for missing fields. */
@SuppressWarnings("unused")
public final class RewardRules {
    public boolean enabled = true;
    public String blockTag = "otters_civ_revived:currency_blocks";
    public long blockReward = 1L;
    public String entityTag = "minecraft:hostile";
    public long entityReward = 5L;
    public long blockCooldownMs = 50L;
    public long entityCooldownMs = 100L;
    public boolean skipCreative = true;
    public boolean skipSpectator = true;
    /** Resource location strings; empty = no blacklist. Matches {@code level.dimension().identifier()}. */
    public List<String> dimensionBlacklist = new ArrayList<>();
    public boolean announceRewards = true;

    /**
     * Optional payouts keyed by block id (e.g. {@code minecraft:diamond_ore}).
     * If a broken block matches a key here, this amount wins (even when the block is not in {@link #blockTag}).
     * Use {@code 0} for an explicit no-pay entry. Keys are normalized to lower case on load.
     * {@link RewardRulesLoader#finalizeRewardsForRunningServer} runs on logical server boot: merged map combines
     * tag-derived defaults, keys from {@code rewards.json}, and persisted sibling overlays.
     */
    public Map<String, Long> blockRewards = new LinkedHashMap<>();

    /**
     * Optional payouts keyed by entity type id (e.g. {@code minecraft:zombie}).
     * Same precedence rules as blocks. Keys are normalized on load.
     * Hydrated alongside {@link RewardRulesLoader#finalizeRewardsForRunningServer} exactly like blocks.
     */
    public Map<String, Long> entityRewards = new LinkedHashMap<>();

    public static RewardRules defaults() {
        return new RewardRules();
    }
}
