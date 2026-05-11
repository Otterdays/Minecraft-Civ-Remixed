package com.fpsmod.ottersciv.config;

import com.fpsmod.FpsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Optional;

/**
 * Expands block / entity payout tags into per-id payout maps. Queries the server's live registry for the
 * tag's members directly (HolderSet.Named) instead of iterating every registry entry and asking "is this
 * tagged?" — the direct query reliably reflects datapack tag bindings at SERVER_STARTED time.
 */
public final class RewardTagExpansion {
    private RewardTagExpansion() {
    }

    public static LinkedHashMap<String, Long> payoutsForTaggedBlocks(
        ServerLevel level,
        TagKey<Block> tagKey,
        long perBlock
    ) {
        return resolveTagMembers(level, Registries.BLOCK, tagKey, perBlock, "block");
    }

    public static LinkedHashMap<String, Long> payoutsForTaggedEntities(
        ServerLevel overworldSample,
        TagKey<EntityType<?>> tagKey,
        long perMob
    ) {
        return resolveTagMembers(overworldSample, Registries.ENTITY_TYPE, tagKey, perMob, "entity");
    }

    private static <T> LinkedHashMap<String, Long> resolveTagMembers(
        ServerLevel level,
        ResourceKey<net.minecraft.core.Registry<T>> registryKey,
        TagKey<T> tagKey,
        long perEntry,
        String label
    ) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        if (level == null) {
            FpsMod.LOGGER.warn("[otters_civ_revived] {} tag expansion skipped: server level is null", label);
            return out;
        }
        if (tagKey == null) {
            FpsMod.LOGGER.warn("[otters_civ_revived] {} tag expansion skipped: tag id failed to parse", label);
            return out;
        }
        long amount = Math.max(0L, perEntry);

        Optional<HolderSet.Named<T>> members;
        try {
            members = level.registryAccess().lookupOrThrow(registryKey).get(tagKey);
        } catch (RuntimeException e) {
            FpsMod.LOGGER.error(
                "[otters_civ_revived] Could not query {} registry for tag {}; expansion aborted",
                label,
                tagKey.location(),
                e
            );
            return out;
        }

        if (members.isEmpty()) {
            FpsMod.LOGGER.warn(
                "[otters_civ_revived] {} tag {} is not present in the server registry — datapack missing, tag id misspelled, or tag empty",
                label,
                tagKey.location()
            );
            return out;
        }

        for (Holder<T> holder : members.get()) {
            Optional<ResourceKey<T>> key = holder.unwrapKey();
            if (key.isEmpty()) {
                continue;
            }
            out.put(key.get().location().toString(), amount);
        }

        FpsMod.LOGGER.info(
            "[otters_civ_revived] {} tag {} resolved {} entries (each defaulting to {})",
            label,
            tagKey.location(),
            out.size(),
            amount
        );
        return out;
    }

    static TagKey<Block> parseBlockTagKey(String raw) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            return null;
        }
        return TagKey.create(Registries.BLOCK, id);
    }

    static TagKey<EntityType<?>> parseEntityTagKey(String raw) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            return null;
        }
        return TagKey.create(Registries.ENTITY_TYPE, id);
    }
}
