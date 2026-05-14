package com.fpsmod.persistence;

import com.fpsmod.guilds.ClaimedChunk;
import com.fpsmod.guilds.Guild;
import com.fpsmod.guilds.GuildLedger;
import com.fpsmod.guilds.GuildStore;

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SqliteGuildStore implements GuildStore {
    private final SqliteDatabase db;

    public SqliteGuildStore(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public GuildLedger load() {
        Map<UUID, Guild> guilds = new HashMap<>();
        Map<String, UUID> byName = new HashMap<>();
        Set<ClaimedChunk> claims = new HashSet<>();

        try (Connection conn = db.connection()) {
            // Load guilds
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT * FROM guilds")) {
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    Guild g = new Guild(id, rs.getString("name"), owner);
                    g.description = rs.getString("description") != null ? rs.getString("description") : "";
                    g.balance = rs.getLong("balance");
                    g.createdAt = rs.getLong("created_at");
                    g.open = rs.getInt("open") != 0;
                    if (rs.getObject("home_x") != null) {
                        g.homePos = new net.minecraft.core.BlockPos(
                            rs.getInt("home_x"), rs.getInt("home_y"), rs.getInt("home_z"));
                    }
                    g.homeDimension = rs.getString("home_dimension");
                    guilds.put(g.id, g);
                    byName.put(normalize(g.name), g.id);
                }
            }
            // Load members
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT guild_id, player_uuid, role FROM guild_members")) {
                while (rs.next()) {
                    UUID gid = UUID.fromString(rs.getString("guild_id"));
                    Guild g = guilds.get(gid);
                    if (g == null) continue;
                    UUID pid = UUID.fromString(rs.getString("player_uuid"));
                    String role = rs.getString("role");
                    g.members.add(pid);
                    if ("officer".equals(role) || "owner".equals(role)) {
                        g.officers.add(pid);
                    }
                }
            }
            // Load claims
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT dimension, chunk_x, chunk_z, guild_id, claimed_at FROM claims")) {
                while (rs.next()) {
                    claims.add(new ClaimedChunk(
                        rs.getString("dimension"),
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_z"),
                        UUID.fromString(rs.getString("guild_id")),
                        rs.getLong("claimed_at")
                    ));
                }
            }
        } catch (Exception e) {
            // Tables may not exist yet
        }
        return new GuildLedger(guilds, byName, claims);
    }

    @Override
    public void save(Map<UUID, Guild> guilds, Map<String, UUID> byName, Set<ClaimedChunk> claims) {
        try (Connection conn = db.connection()) {
            conn.setAutoCommit(false);
            try (var delG = conn.prepareStatement("DELETE FROM guild_members WHERE guild_id = ?");
                 var delC = conn.prepareStatement("DELETE FROM claims WHERE guild_id = ?");
                 var upsertG = conn.prepareStatement(
                     "INSERT INTO guilds (id, name, description, owner_uuid, balance, home_x, home_y, home_z, home_dimension, open, created_at)"
                         + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                         + " ON CONFLICT(id) DO UPDATE SET name=excluded.name, description=excluded.description,"
                         + " owner_uuid=excluded.owner_uuid, balance=excluded.balance,"
                         + " home_x=excluded.home_x, home_y=excluded.home_y, home_z=excluded.home_z,"
                         + " home_dimension=excluded.home_dimension, open=excluded.open");
                 var insM = conn.prepareStatement(
                     "INSERT OR REPLACE INTO guild_members (guild_id, player_uuid, role) VALUES (?, ?, ?)");
                 var insC = conn.prepareStatement(
                     "INSERT OR REPLACE INTO claims (dimension, chunk_x, chunk_z, guild_id, claimed_at) VALUES (?, ?, ?, ?, ?)");

                 // Clear removed guilds
                 var delAllM = conn.prepareStatement("DELETE FROM guild_members");
                 var delAllC = conn.prepareStatement("DELETE FROM claims");
                 var delAllG = conn.prepareStatement("DELETE FROM guilds")) {

                delAllC.executeUpdate();
                delAllM.executeUpdate();
                delAllG.executeUpdate();

                for (Guild g : guilds.values()) {
                    upsertG.setString(1, g.id.toString());
                    upsertG.setString(2, g.name);
                    upsertG.setString(3, g.description);
                    upsertG.setString(4, g.owner.toString());
                    upsertG.setLong(5, g.balance);
                    if (g.homePos != null) {
                        upsertG.setInt(6, g.homePos.getX());
                        upsertG.setInt(7, g.homePos.getY());
                        upsertG.setInt(8, g.homePos.getZ());
                    } else {
                        upsertG.setNull(6, java.sql.Types.INTEGER);
                        upsertG.setNull(7, java.sql.Types.INTEGER);
                        upsertG.setNull(8, java.sql.Types.INTEGER);
                    }
                    upsertG.setString(9, g.homeDimension);
                    upsertG.setInt(10, g.open ? 1 : 0);
                    upsertG.setLong(11, g.createdAt);
                    upsertG.addBatch();

                    for (UUID pid : g.members) {
                        insM.setString(1, g.id.toString());
                        insM.setString(2, pid.toString());
                        insM.setString(3, g.officers.contains(pid) ? "officer" : "member");
                        insM.addBatch();
                    }
                }
                upsertG.executeBatch();
                insM.executeBatch();

                for (ClaimedChunk c : claims) {
                    insC.setString(1, c.dimension());
                    insC.setInt(2, c.chunkX());
                    insC.setInt(3, c.chunkZ());
                    insC.setString(4, c.guildId().toString());
                    insC.setLong(5, c.claimedAt());
                    insC.addBatch();
                }
                insC.executeBatch();
            }
            conn.commit();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save guilds", ex);
        }
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
