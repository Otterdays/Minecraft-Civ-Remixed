package com.fpsmod.client.guilds;

import com.fpsmod.guilds.net.ClaimsPayload;
import com.fpsmod.guilds.net.GuildStatusPayload;
import com.fpsmod.guilds.net.MapTogglePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class GuildClientNetworking {
    private GuildClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ClaimsPayload.TYPE, (payload, context) -> {
            GuildClientState.updateClaims(payload.claims());
        });
        ClientPlayNetworking.registerGlobalReceiver(GuildStatusPayload.TYPE, (payload, context) -> {
            GuildClientState.updateGuildInfo(payload.info());
        });
        ClientPlayNetworking.registerGlobalReceiver(MapTogglePayload.TYPE, (payload, context) -> {
            if (payload.durationSeconds() > 0) {
                GuildChunkOverlay.show(payload.durationSeconds());
            } else {
                GuildChunkOverlay.hide();
            }
        });
    }
}
