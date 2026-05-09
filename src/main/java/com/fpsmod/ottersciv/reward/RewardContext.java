package com.fpsmod.ottersciv.reward;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;

import org.jetbrains.annotations.Nullable;

/** Immutable snapshot for downstream jobs/professions hooks. */
public record RewardContext(
    RewardReason reason,
    long amountGranted,
    @Nullable BlockState blockState,
    @Nullable EntityType<?> entityType
) {
}
