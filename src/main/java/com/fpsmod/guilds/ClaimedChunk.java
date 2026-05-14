package com.fpsmod.guilds;

import java.util.Objects;
import java.util.UUID;

public record ClaimedChunk(String dimension, int chunkX, int chunkZ, UUID guildId, long claimedAt) {
    public ClaimedChunk {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(guildId, "guildId");
    }

    public String key() {
        return dimension + ":" + chunkX + "," + chunkZ;
    }

    public static String keyOf(String dimension, int chunkX, int chunkZ) {
        return dimension + ":" + chunkX + "," + chunkZ;
    }
}
