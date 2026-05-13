package com.fpsmod.jobs.net;

import com.fpsmod.jobs.Job;
import com.fpsmod.jobs.JobState;
import com.fpsmod.jobs.JobsConfig;
import com.fpsmod.jobs.JobsService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Registers the s2c job status payload + sends a snapshot on player connection. */
public final class JobsNetworking {
    private JobsNetworking() {}

    public static void registerServer(JobsService jobs) {
        PayloadTypeRegistry.clientboundPlay().register(JobStatusPayload.TYPE, JobStatusPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            sendStatusFor(jobs, handler.getPlayer())
        );
    }

    /** Send the current snapshot for {@code player}. Cheap; call on any state mutation. */
    public static void sendStatusFor(JobsService jobs, ServerPlayer player) {
        if (player == null) return;
        JobState state = jobs.stateOf(player.getUUID());
        Job active = state.active();
        if (active == null) {
            ServerPlayNetworking.send(
                player,
                new JobStatusPayload(JobStatusPayload.NO_ACTIVE, 0, 0L, 0L, 0L)
            );
            return;
        }
        long xp = state.getXp(active);
        int level = state.levelOf(active);
        long floor = JobsConfig.xpForLevel(level);
        long ceil = level >= JobsConfig.MAX_LEVEL
            ? JobsConfig.xpForLevel(JobsConfig.MAX_LEVEL)
            : JobsConfig.xpForLevel(level + 1);
        ServerPlayNetworking.send(
            player,
            new JobStatusPayload(active.slug(), level, xp, floor, ceil)
        );
    }
}
