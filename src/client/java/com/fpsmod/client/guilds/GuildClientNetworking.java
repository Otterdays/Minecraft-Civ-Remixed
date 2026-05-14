package com.fpsmod.client.guilds;

import com.fpsmod.guilds.net.ClaimsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class GuildClientNetworking {
    private GuildClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ClaimsPayload.TYPE, (payload, context) -> {
            GuildClientState.update(payload.claims());
        });
    }
}
