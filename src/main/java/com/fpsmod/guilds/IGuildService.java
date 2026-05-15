package com.fpsmod.guilds;

import com.fpsmod.economy.IEconomyService;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface IGuildService {
    GuildConfig config();
    Guild guildById(UUID id);
    Guild guildByPlayer(UUID player);
    Guild guildByName(String name);
    Collection<Guild> allGuilds();
    String createGuild(ServerPlayer owner, String name);
    String disbandGuild(ServerPlayer player);
    String invitePlayer(ServerPlayer sender, ServerPlayer target);
    String joinGuild(ServerPlayer player);
    String leaveGuild(ServerPlayer player);
    String kickPlayer(ServerPlayer sender, ServerPlayer target);
    String transferOwnership(ServerPlayer sender, ServerPlayer target);
    String promote(ServerPlayer sender, ServerPlayer target);
    String demote(ServerPlayer sender, ServerPlayer target);
    String setHome(ServerPlayer player);
    String teleportHome(ServerPlayer player);
    String depositToTreasury(ServerPlayer player, long amount);
    String withdrawFromTreasury(ServerPlayer player, long amount);
    String claimChunk(ServerPlayer player, int chunkX, int chunkZ);
    String unclaimChunk(ServerPlayer player, int chunkX, int chunkZ);
    String unclaimAll(ServerPlayer player);
    Set<ClaimedChunk> claimsForGuild(UUID guildId);
    ClaimedChunk claimAt(int chunkX, int chunkZ, String dimension);
    Set<ClaimedChunk> allClaims();
    void refresh();
}
