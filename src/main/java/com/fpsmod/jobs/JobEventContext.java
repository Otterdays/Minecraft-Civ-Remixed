package com.fpsmod.jobs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable gameplay event snapshot used by the jobs engine.
 */
public final class JobEventContext {
    private final JobEventType eventType;
    private final String targetId;
    private final String dimensionId;
    private final String mainHandItemId;
    private final boolean economyRewarded;
    private final boolean directPlayerKill;
    @Nullable
    private final BlockState blockState;
    @Nullable
    private final EntityType<?> entityType;

    private JobEventContext(
        JobEventType eventType,
        String targetId,
        String dimensionId,
        String mainHandItemId,
        boolean economyRewarded,
        boolean directPlayerKill,
        @Nullable BlockState blockState,
        @Nullable EntityType<?> entityType
    ) {
        this.eventType = eventType;
        this.targetId = targetId;
        this.dimensionId = dimensionId;
        this.mainHandItemId = mainHandItemId;
        this.economyRewarded = economyRewarded;
        this.directPlayerKill = directPlayerKill;
        this.blockState = blockState;
        this.entityType = entityType;
    }

    public static JobEventContext forBlockBreak(
        ServerPlayer player,
        ServerLevel level,
        BlockState state,
        boolean economyRewarded
    ) {
        return new JobEventContext(
            JobEventType.BLOCK_BREAK,
            BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
            level.dimension().identifier().toString(),
            BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString(),
            economyRewarded,
            true,
            state,
            null
        );
    }

    public static JobEventContext forMobKill(
        ServerPlayer player,
        ServerLevel level,
        EntityType<?> type,
        boolean economyRewarded,
        boolean directPlayerKill
    ) {
        return new JobEventContext(
            JobEventType.MOB_KILL,
            BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
            level.dimension().identifier().toString(),
            BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString(),
            economyRewarded,
            directPlayerKill,
            null,
            type
        );
    }

    public JobEventType eventType() {
        return eventType;
    }

    public String targetId() {
        return targetId;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public String mainHandItemId() {
        return mainHandItemId;
    }

    public boolean economyRewarded() {
        return economyRewarded;
    }

    public boolean directPlayerKill() {
        return directPlayerKill;
    }

    @Nullable
    public BlockState blockState() {
        return blockState;
    }

    @Nullable
    public EntityType<?> entityType() {
        return entityType;
    }
}
