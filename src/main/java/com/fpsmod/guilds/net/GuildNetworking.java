package com.fpsmod.guilds.net;

import com.fpsmod.guilds.GuildService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class GuildNetworking {
    private GuildNetworking() {}

    public static void registerServer(GuildService guilds) {
        PayloadTypeRegistry.clientboundPlay().register(ClaimsPayload.TYPE, ClaimsPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendClaimsTo(guilds, handler.getPlayer());
        });
    }

    public static void sendClaimsTo(GuildService guilds, ServerPlayer player) {
        if (player == null) return;
        ServerPlayNetworking.send(player, ClaimsPayload.fromClaims(guilds.allClaims()));
    }

    public static void broadcastClaims(GuildService guilds, MinecraftServer server) {
        if (server == null) return;
        var payload = ClaimsPayload.fromClaims(guilds.allClaims());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
