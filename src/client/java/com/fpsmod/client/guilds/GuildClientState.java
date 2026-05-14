package com.fpsmod.client.guilds;

import com.fpsmod.guilds.ClaimedChunk;
import com.fpsmod.guilds.net.GuildStatusPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GuildClientState {
    private static volatile List<ClaimedChunk> claims = List.of();
    private static volatile GuildStatusPayload.ClientGuildInfo guildInfo;

    private GuildClientState() {}

    public static void updateClaims(List<ClaimedChunk> newClaims) {
        claims = Collections.unmodifiableList(new ArrayList<>(newClaims));
    }

    public static void updateGuildInfo(GuildStatusPayload.ClientGuildInfo info) {
        guildInfo = info;
    }

    public static List<ClaimedChunk> claims() {
        return claims;
    }

    public static GuildStatusPayload.ClientGuildInfo guildInfo() {
        return guildInfo;
    }
}
