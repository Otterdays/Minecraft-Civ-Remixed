package com.fpsmod.guilds;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record GuildLedger(Map<UUID, Guild> guilds, Map<String, UUID> byName, Set<ClaimedChunk> claims) {
}
