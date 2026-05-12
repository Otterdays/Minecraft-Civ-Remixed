package com.fpsmod.ottersciv.config;

import com.fpsmod.FpsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.function.Function;

/**
 * Expands block / entity payout tags into per-id payout maps.
 *
 * <p>Uses {@link Registry#getTagOrEmpty(TagKey)} on the static {@link BuiltInRegistries} — that's where
 * {@code TagLoader} binds datapack tag membership during the server resource reload. The
 * {@code RegistryAccess.Frozen} view exposed by {@code ServerLevel#registryAccess()} does <em>not</em>
 * carry tag bindings in this Minecraft version (even vanilla tags like {@code minecraft:hostile} come
 * back empty through that path), so the lookup must go through the static registry.</p>
 */
public final class RewardTagExpansion {
    private RewardTagExpansion() {
    }

    public static LinkedHashMap<String, Long> payoutsForTaggedBlocks(
        ServerLevel level,
        TagKey<Block> tagKey,
        long perBlock
    ) {
        return resolveTagMembers(BuiltInRegistries.BLOCK, tagKey, perBlock, "block", BuiltInRegistries.BLOCK::getKey);
    }

    public static LinkedHashMap<String, Long> payoutsForTaggedEntities(
        ServerLevel overworldSample,
        TagKey<EntityType<?>> tagKey,
        long perMob
    ) {
        return resolveTagMembers(
            BuiltInRegistries.ENTITY_TYPE,
            tagKey,
            perMob,
            "entity",
            BuiltInRegistries.ENTITY_TYPE::getKey
        );
    }

    private static <T> LinkedHashMap<String, Long> resolveTagMembers(
        Registry<T> registry,
        TagKey<T> tagKey,
        long perEntry,
        String label,
        Function<T, Identifier> idLookup
    ) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        if (tagKey == null) {
            FpsMod.LOGGER.warn("[otters_civ_revived] {} tag expansion skipped: tag id failed to parse", label);
            return out;
        }
        long amount = Math.max(0L, perEntry);

        Iterable<Holder<T>> members;
        try {
            members = registry.getTagOrEmpty(tagKey);
        } catch (RuntimeException e) {
            FpsMod.LOGGER.error(
                "[otters_civ_revived] Could not query {} registry for tag {}; expansion aborted",
                label,
                tagKey.location(),
                e
            );
            return out;
        }

        for (Holder<T> holder : members) {
            T value;
            try {
                value = holder.value();
            } catch (RuntimeException e) {
                continue;
            }
            Identifier id = idLookup.apply(value);
            if (id != null) {
                out.put(id.toString(), amount);
            }
        }

        if (out.isEmpty()) {
            long boundTagCount = registry.getTags().count();
            FpsMod.LOGGER.warn(
                "[otters_civ_revived] {} tag {} resolved 0 entries (registry holds {} bound tags total). "
                    + "Tag id may be misspelled, datapack not loaded, or tags not yet bound on this registry.",
                label,
                tagKey.location(),
                boundTagCount
            );
        } else {
            FpsMod.LOGGER.info(
                "[otters_civ_revived] {} tag {} resolved {} entries (each defaulting to {})",
                label,
                tagKey.location(),
                out.size(),
                amount
            );
        }
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
