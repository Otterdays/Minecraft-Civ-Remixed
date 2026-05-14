package com.fpsmod.jobs.net;

import com.fpsmod.jobs.JobsConfig;
import com.fpsmod.jobs.JobsService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Registers the s2c jobs payloads + sends snapshots on player connection. */
public final class JobsNetworking {
    private JobsNetworking() {}

    public static void registerServer(JobsService jobs) {
        PayloadTypeRegistry.clientboundPlay().register(JobCatalogPayload.TYPE, JobCatalogPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(JobStatusPayload.TYPE, JobStatusPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendAllFor(jobs, handler.getPlayer()));
    }

    public static void sendAllFor(JobsService jobs, ServerPlayer player) {
        sendCatalogFor(jobs, player);
        sendStatusFor(jobs, player);
    }

    public static void sendCatalogFor(JobsService jobs, ServerPlayer player) {
        if (player == null) return;
        ServerPlayNetworking.send(player, JobCatalogPayload.fromSnapshot(jobs.catalogSnapshot()));
    }

    public static void sendStatusFor(JobsService jobs, ServerPlayer player) {
        if (player == null) return;
        ServerPlayNetworking.send(player, JobStatusPayload.fromSnapshot(jobs.statusSnapshot(player.getUUID())));
    }

    public static void broadcastCatalogAndStatuses(JobsService jobs, MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendAllFor(jobs, player);
        }
    }
}
