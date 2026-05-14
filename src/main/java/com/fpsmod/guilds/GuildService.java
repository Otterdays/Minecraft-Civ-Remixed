package com.fpsmod.guilds;

import com.fpsmod.economy.WalletService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuildService {
    private final GuildStore store;
    private final WalletService wallets;
    private volatile GuildConfig config;
    private final Map<UUID, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();
    private final Map<String, ClaimedChunk> claimsByKey = new ConcurrentHashMap<>();
    private final Set<ClaimedChunk> claims = ConcurrentHashMap.newKeySet();

    public GuildService(GuildStore store, WalletService wallets, GuildConfig config) {
        this.store = store;
        this.wallets = wallets;
        this.config = config;
        load();
    }

    public static GuildService createDefault(WalletService wallets) {
        return new GuildService(new FileGuildStore(), wallets, GuildConfigLoader.loadOrCreate());
    }

    public GuildConfig config() {
        return config;
    }

    public void refresh() {
        this.config = GuildConfigLoader.loadOrCreate();
    }

    private static int chunkCoord(int blockCoord) {
        return blockCoord >> 4;
    }

    private static String dimensionId(ServerPlayer player) {
        return player.level().dimension().identifier().toString();
    }

    public Guild guildById(UUID id) {
        return guilds.get(id);
    }

    public Guild guildByPlayer(UUID player) {
        for (Guild g : guilds.values()) {
            if (g.isMember(player)) return g;
        }
        return null;
    }

    public Guild guildByName(String name) {
        UUID id = byName.get(FileGuildStore.normalizeName(name));
        return id == null ? null : guilds.get(id);
    }

    public Collection<Guild> allGuilds() {
        return Collections.unmodifiableCollection(guilds.values());
    }

    public String createGuild(ServerPlayer owner, String name) {
        if (!config.enabled) return "Guilds are disabled.";
        if (guildByPlayer(owner.getUUID()) != null) return "You are already in a guild.";
        String normalized = FileGuildStore.normalizeName(name);
        if (normalized.length() < config.minNameLength)
            return "Name too short (min " + config.minNameLength + ").";
        if (normalized.length() > config.maxNameLength)
            return "Name too long (max " + config.maxNameLength + ").";
        if (!normalized.matches("[a-z0-9_]+"))
            return "Name must be alphanumeric (a-z, 0-9, underscores).";
        if (byName.containsKey(normalized))
            return "A guild named '" + name + "' already exists.";
        if (config.creationCost > 0) {
            long balance = wallets.getBalance(owner.getUUID());
            if (balance < config.creationCost)
                return "You need $" + config.creationCost + " to create a guild (you have $" + balance + ").";
            wallets.addBalance(owner.getUUID(), -config.creationCost);
        }
        Guild g = new Guild(UUID.randomUUID(), name.trim(), owner.getUUID());
        guilds.put(g.id, g);
        byName.put(normalized, g.id);
        persist();
        return "Guild '" + g.name + "' created!";
    }

    public String disbandGuild(ServerPlayer player) {
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(player.getUUID())) return "Only the owner can disband the guild.";
        if (config.disbandRefundClaims) {
            wallets.addBalance(player.getUUID(), (long) config.claimCost * claimsForGuild(g.id).size());
        }
        byName.values().removeIf(v -> v.equals(g.id));
        guilds.remove(g.id);
        claims.removeIf(c -> c.guildId().equals(g.id));
        rebuildClaimsIndex();
        persist();
        return "Guild '" + g.name + "' disbanded.";
    }

    public String invitePlayer(ServerPlayer sender, ServerPlayer target) {
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOfficer(sender.getUUID())) return "Only officers can invite.";
        if (g.memberCount() >= config.maxMembers) return "Guild is full (max " + config.maxMembers + ").";
        if (g.isMember(target.getUUID())) return "They are already in your guild.";
        if (guildByPlayer(target.getUUID()) != null) return "They are already in another guild.";
        pendingInvites.put(target.getUUID(), g.id);
        return "Invited " + target.getName().getString() + " to " + g.name + ".";
    }

    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();

    public String joinGuild(ServerPlayer player) {
        UUID guildId = pendingInvites.remove(player.getUUID());
        if (guildId == null) {
            // Check if guild is open
            for (Guild g : guilds.values()) {
                if (g.open && !g.isMember(player.getUUID()) && g.memberCount() < config.maxMembers) {
                    g.members.add(player.getUUID());
                    persist();
                    return "You joined " + g.name + "!";
                }
            }
            return "You have no pending invite.";
        }
        Guild g = guilds.get(guildId);
        if (g == null) return "That guild no longer exists.";
        if (g.memberCount() >= config.maxMembers) return "Guild is full.";
        if (guildByPlayer(player.getUUID()) != null) {
            pendingInvites.remove(player.getUUID());
            return "You are already in a guild.";
        }
        g.members.add(player.getUUID());
        persist();
        return "You joined " + g.name + "!";
    }

    public String leaveGuild(ServerPlayer player) {
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (g.isOwner(player.getUUID())) return "Use /guild disband to dissolve the guild.";
        g.members.remove(player.getUUID());
        g.officers.remove(player.getUUID());
        if (g.members.isEmpty()) {
            guilds.remove(g.id);
            byName.values().removeIf(v -> v.equals(g.id));
        }
        persist();
        return "You left " + g.name + ".";
    }

    public String kickPlayer(ServerPlayer sender, ServerPlayer target) {
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOfficer(sender.getUUID())) return "Only officers can kick.";
        if (g.isOwner(target.getUUID())) return "You cannot kick the owner.";
        if (!g.isMember(target.getUUID())) return "That player is not in your guild.";
        g.members.remove(target.getUUID());
        g.officers.remove(target.getUUID());
        persist();
        return "Kicked " + target.getName().getString() + " from " + g.name + ".";
    }

    public String transferOwnership(ServerPlayer sender, ServerPlayer target) {
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(sender.getUUID())) return "Only the owner can transfer.";
        if (!g.isMember(target.getUUID())) return "That player is not in your guild.";
        g.owner = target.getUUID();
        persist();
        return "Ownership transferred to " + target.getName().getString() + ".";
    }

    public String promote(ServerPlayer sender, ServerPlayer target) {
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(sender.getUUID())) return "Only the owner can promote.";
        if (!g.isMember(target.getUUID())) return "That player is not in your guild.";
        if (g.officers.contains(target.getUUID())) return "They are already an officer.";
        g.officers.add(target.getUUID());
        persist();
        return "Promoted " + target.getName().getString() + " to officer.";
    }

    public String demote(ServerPlayer sender, ServerPlayer target) {
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(sender.getUUID())) return "Only the owner can demote.";
        if (!g.officers.contains(target.getUUID())) return "They are not an officer.";
        g.officers.remove(target.getUUID());
        persist();
        return "Demoted " + target.getName().getString() + " from officer.";
    }

    public String setHome(ServerPlayer player) {
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOfficer(player.getUUID())) return "Only officers can set the guild home.";
        g.homePos = player.blockPosition();
        g.homeDimension = dimensionId(player);
        persist();
        return "Guild home set at your location.";
    }

    public String teleportHome(ServerPlayer player) {
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (g.homePos == null) return "No guild home has been set.";

        String dimStr = g.homeDimension != null ? g.homeDimension : "minecraft:overworld";
        Identifier dimId = Identifier.parse(dimStr);
        ResourceKey<Level> dimKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, dimId);
        ServerLevel targetLevel = player.level().getServer().getLevel(dimKey);
        if (targetLevel == null) return "Home dimension not found.";

        BlockPos pos = g.homePos;
        player.teleportTo(targetLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            Set.of(), player.getYRot(), player.getXRot(), false);
        return "Teleported to guild home.";
    }

    // --- Chunk claims ---

    public String claimChunk(ServerPlayer player, int chunkX, int chunkZ) {
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOfficer(player.getUUID())) return "Only officers can claim land.";
        String dim = dimensionId(player);
        String key = ClaimedChunk.keyOf(dim, chunkX, chunkZ);

        if (claimsByKey.containsKey(key)) {
            ClaimedChunk existing = claimsByKey.get(key);
            if (existing.guildId().equals(g.id)) return "Your guild already owns this chunk.";
            return "This chunk is already claimed.";
        }
        if (claimsForGuild(g.id).size() >= config.maxClaims) return "Max claims reached (" + config.maxClaims + ").";

        if (config.claimCost > 0) {
            long balance = wallets.getBalance(player.getUUID());
            if (balance < config.claimCost)
                return "You need $" + config.claimCost + " to claim (you have $" + balance + ").";
            wallets.addBalance(player.getUUID(), -config.claimCost);
        }

        ClaimedChunk claim = new ClaimedChunk(dim, chunkX, chunkZ, g.id, System.currentTimeMillis());
        claims.add(claim);
        claimsByKey.put(key, claim);
        persist();
        return "Chunk claimed for " + g.name + "!";
    }

    public String unclaimChunk(ServerPlayer player, int chunkX, int chunkZ) {
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOfficer(player.getUUID())) return "Only officers can unclaim land.";
        String dim = dimensionId(player);
        String key = ClaimedChunk.keyOf(dim, chunkX, chunkZ);
        ClaimedChunk claim = claimsByKey.get(key);
        if (claim == null) return "This chunk is not claimed.";
        if (!claim.guildId().equals(g.id)) return "This chunk belongs to another guild.";
        claims.remove(claim);
        claimsByKey.remove(key);
        persist();
        return "Chunk unclaimed.";
    }

    public String unclaimAll(ServerPlayer player) {
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(player.getUUID())) return "Only the owner can unclaim all.";
        int count = claimsForGuild(g.id).size();
        claims.removeIf(c -> c.guildId().equals(g.id));
        rebuildClaimsIndex();
        persist();
        return "Unclaimed " + count + " chunk(s).";
    }

    public Set<ClaimedChunk> claimsForGuild(UUID guildId) {
        Set<ClaimedChunk> result = new HashSet<>();
        for (ClaimedChunk c : claims) {
            if (c.guildId().equals(guildId)) result.add(c);
        }
        return result;
    }

    public ClaimedChunk claimAt(int chunkX, int chunkZ, String dimension) {
        return claimsByKey.get(ClaimedChunk.keyOf(dimension, chunkX, chunkZ));
    }

    public Set<ClaimedChunk> allClaims() {
        return Collections.unmodifiableSet(claims);
    }

    private void rebuildClaimsIndex() {
        claimsByKey.clear();
        for (ClaimedChunk c : claims) {
            claimsByKey.put(c.key(), c);
        }
    }

    private synchronized void persist() {
        Map<String, UUID> nameIndex = new HashMap<>();
        for (Map.Entry<String, UUID> e : byName.entrySet()) {
            nameIndex.put(e.getKey(), e.getValue());
        }
        store.save(Map.copyOf(guilds), nameIndex, Set.copyOf(claims));
    }

    private void load() {
        GuildLedger ledger = store.load();
        guilds.clear();
        byName.clear();
        claims.clear();
        claimsByKey.clear();
        guilds.putAll(ledger.guilds());
        byName.putAll(ledger.byName());
        claims.addAll(ledger.claims());
        rebuildClaimsIndex();
    }
}
