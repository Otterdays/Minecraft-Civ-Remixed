package com.fpsmod.guilds;

import com.fpsmod.economy.IEconomyService;
import com.fpsmod.economy.TransactionReason;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuildService implements IGuildService {
    private final GuildStore store;
    private final IEconomyService wallets;
    private volatile GuildConfig config;
    private final Map<UUID, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();
    private final Map<String, ClaimedChunk> claimsByKey = new ConcurrentHashMap<>();
    private final Set<ClaimedChunk> claims = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastHomeTeleports = new ConcurrentHashMap<>();

    public GuildService(GuildStore store, IEconomyService wallets, GuildConfig config) {
        this.store = store;
        this.wallets = wallets;
        this.config = config;
        load();
    }

    public static GuildService createDefault(IEconomyService wallets) {
        return new GuildService(new FileGuildStore(), wallets, GuildConfigLoader.loadOrCreate());
    }

    public GuildConfig config() {
        return config;
    }

    public void refresh() {
        this.config = GuildConfigLoader.loadOrCreate();
        boolean mutated = false;
        if (!config.enabled || !config.allowOpenGuilds) {
            for (Guild guild : guilds.values()) {
                if (guild.open) {
                    guild.open = false;
                    mutated = true;
                }
            }
        }
        if (mutated) {
            persist();
        }
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
        if (!config.enabled) return "Guilds are disabled.";
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
        if (!config.enabled) return "Guilds are disabled.";
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
        if (!config.enabled) return "Guilds are disabled.";
        if (guildByPlayer(player.getUUID()) != null) {
            pendingInvites.remove(player.getUUID());
            return "You are already in a guild.";
        }
        UUID guildId = pendingInvites.remove(player.getUUID());
        if (guildId == null) {
            if (!config.allowOpenGuilds) {
                return "You have no pending invite.";
            }
            Guild openGuild = null;
            for (Guild g : guilds.values()) {
                if (!g.open || g.memberCount() >= config.maxMembers) {
                    continue;
                }
                if (openGuild != null) {
                    return "Multiple guilds are open. Use /guild join <name>.";
                }
                openGuild = g;
            }
            if (openGuild == null) {
                return "You have no pending invite.";
            }
            openGuild.members.add(player.getUUID());
            persist();
            return "You joined " + openGuild.name + "!";
        }
        Guild g = guilds.get(guildId);
        if (g == null) return "That guild no longer exists.";
        if (g.memberCount() >= config.maxMembers) return "Guild is full.";
        g.members.add(player.getUUID());
        persist();
        return "You joined " + g.name + "!";
    }

    public String joinGuild(ServerPlayer player, String name) {
        if (!config.enabled) return "Guilds are disabled.";
        if (guildByPlayer(player.getUUID()) != null) {
            return "You are already in a guild.";
        }
        UUID invitedGuildId = pendingInvites.get(player.getUUID());
        if (invitedGuildId != null) {
            Guild invitedGuild = guilds.get(invitedGuildId);
            if (invitedGuild != null
                && FileGuildStore.normalizeName(invitedGuild.name).equals(FileGuildStore.normalizeName(name))) {
                if (invitedGuild.memberCount() >= config.maxMembers) {
                    return "Guild is full.";
                }
                pendingInvites.remove(player.getUUID());
                invitedGuild.members.add(player.getUUID());
                persist();
                return "You joined " + invitedGuild.name + "!";
            }
        }
        Guild g = guildByName(name);
        if (g == null) {
            return "No guild named '" + name + "' exists.";
        }
        if (!config.allowOpenGuilds) {
            return "Open guild joining is disabled.";
        }
        if (!g.open) {
            return "That guild is invite-only.";
        }
        if (g.memberCount() >= config.maxMembers) {
            return "Guild is full.";
        }
        pendingInvites.remove(player.getUUID());
        g.members.add(player.getUUID());
        persist();
        return "You joined " + g.name + "!";
    }

    public String leaveGuild(ServerPlayer player) {
        if (!config.enabled) return "Guilds are disabled.";
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
        if (!config.enabled) return "Guilds are disabled.";
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
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(sender.getUUID())) return "Only the owner can transfer.";
        if (!g.isMember(target.getUUID())) return "That player is not in your guild.";
        g.owner = target.getUUID();
        persist();
        return "Ownership transferred to " + target.getName().getString() + ".";
    }

    public String promote(ServerPlayer sender, ServerPlayer target) {
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(sender.getUUID())) return "Only the owner can promote.";
        if (!g.isMember(target.getUUID())) return "That player is not in your guild.";
        if (g.isOwner(target.getUUID())) return "The owner is already the owner.";
        if (g.officers.contains(target.getUUID())) return "They are already an officer.";
        if (config.maxOfficers > 0 && g.officers.size() >= config.maxOfficers) {
            return "Officer cap reached (max " + config.maxOfficers + ").";
        }
        g.officers.add(target.getUUID());
        persist();
        return "Promoted " + target.getName().getString() + " to officer.";
    }

    public String demote(ServerPlayer sender, ServerPlayer target) {
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(sender.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(sender.getUUID())) return "Only the owner can demote.";
        if (!g.officers.contains(target.getUUID())) return "They are not an officer.";
        g.officers.remove(target.getUUID());
        persist();
        return "Demoted " + target.getName().getString() + " from officer.";
    }

    public String setHome(ServerPlayer player) {
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOfficer(player.getUUID())) return "Only officers can set the guild home.";
        g.homePos = player.blockPosition();
        g.homeDimension = dimensionId(player);
        persist();
        return "Guild home set at your location.";
    }

    public String teleportHome(ServerPlayer player) {
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (g.homePos == null) return "No guild home has been set.";
        long cooldownSeconds = Math.max(0, config.homeTeleportCooldownSeconds);
        long now = System.currentTimeMillis();
        if (cooldownSeconds > 0L) {
            Long lastTeleportAt = lastHomeTeleports.get(player.getUUID());
            if (lastTeleportAt != null) {
                long cooldownMs = cooldownSeconds * 1000L;
                long elapsedMs = now - lastTeleportAt;
                if (elapsedMs < cooldownMs) {
                    long remainingSeconds = (cooldownMs - elapsedMs + 999L) / 1000L;
                    return "Guild home is on cooldown for " + remainingSeconds + "s.";
                }
            }
        }

        String dimStr = g.homeDimension != null ? g.homeDimension : "minecraft:overworld";
        Identifier dimId = Identifier.parse(dimStr);
        ResourceKey<Level> dimKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, dimId);
        ServerLevel targetLevel = player.level().getServer().getLevel(dimKey);
        if (targetLevel == null) return "Home dimension not found.";

        BlockPos pos = g.homePos;
        player.teleportTo(targetLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            Set.of(), player.getYRot(), player.getXRot(), false);
        if (cooldownSeconds > 0L) {
            lastHomeTeleports.put(player.getUUID(), now);
        }
        return "Teleported to guild home.";
    }

    public String setOpen(ServerPlayer player, boolean open) {
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOwner(player.getUUID())) return "Only the owner can change guild join mode.";
        if (open && !config.allowOpenGuilds) return "Open guild joining is disabled in guilds.json.";
        if (g.open == open) {
            return open ? "Your guild is already open to the public." : "Your guild is already invite-only.";
        }
        g.open = open;
        persist();
        return open ? "Your guild is now open to the public." : "Your guild is now invite-only.";
    }

    // --- Guild treasury ---

    /**
     * Any member can deposit coins from their personal wallet into the guild treasury.
     * The transaction is atomic: the player's wallet is debited and the guild balance credited
     * in the same call. Both sides are written to the wallet ledger for full auditability.
     */
    public String depositToTreasury(ServerPlayer player, long amount) {
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (amount <= 0) return "Amount must be positive.";
        if (config.maxTreasuryDeposit > 0 && amount > config.maxTreasuryDeposit)
            return "Single deposit cap is $" + config.maxTreasuryDeposit + ".";
        long playerBal = wallets.getBalance(player.getUUID());
        if (playerBal < amount)
            return "Insufficient funds (you have $" + playerBal + ").";
        String note = "guild:" + g.id + ":" + g.name;
        wallets.addBalance(player.getUUID(), -amount, player.getName().getString(), TransactionReason.GUILD_DEPOSIT);
        synchronized (g) {
            g.balance = Math.max(0L, g.balance + amount);
        }
        persist();
        return "Deposited $" + amount + " into " + g.name + " treasury. Treasury: $" + g.balance + ".";
    }

    /**
     * Officers and the owner can withdraw coins from the guild treasury into their personal wallet.
     * Role-validated: regular members are rejected. Ledger entries record both sides.
     */
    public String withdrawFromTreasury(ServerPlayer player, long amount) {
        if (!config.enabled) return "Guilds are disabled.";
        Guild g = guildByPlayer(player.getUUID());
        if (g == null) return "You are not in a guild.";
        if (!g.isOfficer(player.getUUID())) return "Only officers can withdraw from the treasury.";
        if (amount <= 0) return "Amount must be positive.";
        if (config.maxTreasuryWithdraw > 0 && amount > config.maxTreasuryWithdraw)
            return "Single withdrawal cap is $" + config.maxTreasuryWithdraw + ".";
        synchronized (g) {
            if (g.balance < amount)
                return "Treasury has insufficient funds ($" + g.balance + ").";
            g.balance -= amount;
        }
        wallets.addBalance(player.getUUID(), amount, player.getName().getString(), TransactionReason.GUILD_WITHDRAW);
        persist();
        return "Withdrew $" + amount + " from " + g.name + " treasury. Treasury: $" + g.balance + ".";
    }

    // --- Chunk claims ---

    public String claimChunk(ServerPlayer player, int chunkX, int chunkZ) {
        if (!config.enabled) return "Guilds are disabled.";
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
        if (!config.enabled) return "Guilds are disabled.";
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
        if (!config.enabled) return "Guilds are disabled.";
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

    /**
     * Rebuilds the O(1) chunk-key → claim lookup index from the authoritative {@code claims} set.
     * Called only on bulk mutations (unclaimAll, load). Single-claim mutations go through
     * {@link #claimsByKey} directly, so normal play never triggers a full rebuild.
     * Total claims are bounded by {@code maxClaims * guild count}, keeping worst-case
     * rebuild linear and fast even on servers with many guilds.
     */
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
