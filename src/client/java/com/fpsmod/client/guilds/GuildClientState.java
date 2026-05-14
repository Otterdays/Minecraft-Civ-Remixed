package com.fpsmod.client.guilds;

import com.fpsmod.guilds.ClaimedChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GuildClientState {
    private static volatile List<ClaimedChunk> claims = List.of();

    private GuildClientState() {}

    public static void update(List<ClaimedChunk> newClaims) {
        claims = Collections.unmodifiableList(new ArrayList<>(newClaims));
    }

    public static List<ClaimedChunk> claims() {
        return claims;
    }
}
