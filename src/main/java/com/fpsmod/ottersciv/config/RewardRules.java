package com.fpsmod.ottersciv.config;

import java.util.ArrayList;
import java.util.List;

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

    public static RewardRules defaults() {
        return new RewardRules();
    }
}
