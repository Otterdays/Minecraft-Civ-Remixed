package com.fpsmod.persistence;

import com.fpsmod.OogaMod;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SchemaMigrator {
    private static final int CURRENT_VERSION = 1;

    private final SqliteDatabase db;

    public SchemaMigrator(SqliteDatabase db) {
        this.db = db;
    }

    public void migrate() {
        try (Connection conn = db.connection()) {
            int version = readVersion(conn);
            if (version > CURRENT_VERSION) {
                throw new RuntimeException(
                    "Database schema version " + version + " is newer than code version " + CURRENT_VERSION
                        + ". Downgrade not supported.");
            }
            if (version == CURRENT_VERSION) {
                OogaMod.LOGGER.info("[persistence] Schema up to date (v{})", version);
                return;
            }
            OogaMod.LOGGER.info("[persistence] Migrating schema from v{} to v{}", version, CURRENT_VERSION);
            for (int v = version + 1; v <= CURRENT_VERSION; v++) {
                applyMigration(conn, v);
            }
            OogaMod.LOGGER.info("[persistence] Schema migration complete (v{})", CURRENT_VERSION);
        } catch (Exception e) {
            throw new RuntimeException("Schema migration failed", e);
        }
    }

    public int readVersion() {
        try (Connection conn = db.connection()) {
            return readVersion(conn);
        } catch (Exception e) {
            return 0;
        }
    }

    private int readVersion(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT max(version) FROM schema_version");
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void applyMigration(Connection conn, int version) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            for (String sql : migrationScript(version)) {
                stmt.execute(sql);
            }
            stmt.execute("INSERT INTO schema_version (version, description) VALUES ("
                + version + ", '" + migrationName(version) + "')");
        }
    }

    private String migrationName(int version) {
        return switch (version) {
            case 1 -> "Initial schema: wallets, ledger, guilds, claims, jobs";
            default -> "v" + version;
        };
    }

    private List<String> migrationScript(int version) {
        List<String> sql = new ArrayList<>();
        switch (version) {
            case 1 -> {
                sql.add("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version INTEGER PRIMARY KEY,
                        applied_at TEXT NOT NULL DEFAULT (datetime('now')),
                        description TEXT
                    )""");
                sql.add("""
                    CREATE TABLE IF NOT EXISTS wallets (
                        player_uuid TEXT PRIMARY KEY,
                        balance INTEGER NOT NULL DEFAULT 0,
                        name_hint TEXT,
                        updated_at TEXT NOT NULL DEFAULT (datetime('now'))
                    )""");
                sql.add("""
                    CREATE TABLE IF NOT EXISTS wallet_ledger (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        delta INTEGER NOT NULL,
                        balance_after INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        note TEXT,
                        created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    )""");
                sql.add("CREATE INDEX IF NOT EXISTS idx_ledger_player ON wallet_ledger(player_uuid)");
                sql.add("""
                    CREATE TABLE IF NOT EXISTS guilds (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        description TEXT DEFAULT '',
                        owner_uuid TEXT NOT NULL,
                        balance INTEGER NOT NULL DEFAULT 0,
                        home_x INTEGER, home_y INTEGER, home_z INTEGER,
                        home_dimension TEXT,
                        open INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )""");
                sql.add("""
                    CREATE TABLE IF NOT EXISTS guild_members (
                        guild_id TEXT NOT NULL REFERENCES guilds(id),
                        player_uuid TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'member',
                        PRIMARY KEY (guild_id, player_uuid)
                    )""");
                sql.add("""
                    CREATE TABLE IF NOT EXISTS claims (
                        dimension TEXT NOT NULL,
                        chunk_x INTEGER NOT NULL,
                        chunk_z INTEGER NOT NULL,
                        guild_id TEXT NOT NULL REFERENCES guilds(id),
                        claimed_at INTEGER NOT NULL,
                        PRIMARY KEY (dimension, chunk_x, chunk_z)
                    )""");
                sql.add("""
                    CREATE TABLE IF NOT EXISTS jobs_state (
                        player_uuid TEXT NOT NULL,
                        job_id TEXT NOT NULL,
                        xp INTEGER NOT NULL DEFAULT 0,
                        active INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, job_id)
                    )""");
                sql.add("CREATE INDEX IF NOT EXISTS idx_jobs_active ON jobs_state(player_uuid, active)");
            }
        }
        return sql;
    }
}
