package com.fpsmod.ottersciv.reward;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/** Helpers for hostile / entity-tag membership used by payouts and prefilled configs. */
public final class KillRewardTagChecks {
    private KillRewardTagChecks() {
    }

    /** True when {@code type} is in {@code tag} for this server's loaded tag datapacks. */
    public static boolean isEntityTypeTagged(
        ServerLevel level,
        EntityType<?> type,
        TagKey<EntityType<?>> tag
    ) {
        Optional<ResourceKey<EntityType<?>>> key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getResourceKey(type);
        if (key.isEmpty()) {
            return false;
        }
        return level.registryAccess()
            .lookupOrThrow(Registries.ENTITY_TYPE)
            .get(key.get())
            .map(holder -> holder.is(tag))
            .orElse(false);
    }
}
