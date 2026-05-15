package com.fpsmod.guilds;

import java.util.Objects;
import java.util.UUID;

/**
 * @param guildDisplayName optional human-readable guild name; used only for client sync payloads
 *                         (not persisted in SQLite / guilds_data.json).
 */
public record ClaimedChunk(
    String dimension,
    int chunkX,
    int chunkZ,
    UUID guildId,
    long claimedAt,
    String guildDisplayName
) {
    public ClaimedChunk(String dimension, int chunkX, int chunkZ, UUID guildId, long claimedAt) {
        this(dimension, chunkX, chunkZ, guildId, claimedAt, null);
    }

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
