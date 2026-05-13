package com.fpsmod.jobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Optional;

/**
 * MVP fixed set: miner / lumberjack / farmer / fighter.
 * Each job is tied to a tag id under the {@code otters_civ_revived:job/...} namespace.
 * Block-jobs use the BLOCK tag key; FIGHTER uses the ENTITY_TYPE tag key.
 */
public enum Job {
    MINER("miner", "otters_civ_revived:job/miner_blocks", Kind.BLOCK),
    LUMBERJACK("lumberjack", "otters_civ_revived:job/lumberjack_blocks", Kind.BLOCK),
    FARMER("farmer", "otters_civ_revived:job/farmer_blocks", Kind.BLOCK),
    FIGHTER("fighter", "otters_civ_revived:job/fighter_mobs", Kind.ENTITY);

    public enum Kind { BLOCK, ENTITY }

    private final String slug;
    private final String tagId;
    private final Kind kind;

    Job(String slug, String tagId, Kind kind) {
        this.slug = slug;
        this.tagId = tagId;
        this.kind = kind;
    }

    public String slug() { return slug; }
    public Kind kind() { return kind; }
    public String tagId() { return tagId; }

    public TagKey<Block> blockTagKey() {
        if (kind != Kind.BLOCK) {
            return null;
        }
        return TagKey.create(Registries.BLOCK, Identifier.tryParse(tagId));
    }

    public TagKey<EntityType<?>> entityTagKey() {
        if (kind != Kind.ENTITY) {
            return null;
        }
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.tryParse(tagId));
    }

    public static Optional<Job> bySlug(String raw) {
        if (raw == null) return Optional.empty();
        String norm = raw.trim().toLowerCase(Locale.ROOT);
        for (Job j : values()) {
            if (j.slug.equals(norm)) return Optional.of(j);
        }
        return Optional.empty();
    }
}
