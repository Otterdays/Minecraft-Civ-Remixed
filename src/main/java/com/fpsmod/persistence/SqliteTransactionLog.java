package com.fpsmod.persistence;

import com.fpsmod.economy.LedgerEntry;
import com.fpsmod.economy.TransactionLog;
import com.fpsmod.economy.TransactionReason;

import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqliteTransactionLog implements TransactionLog {
    private final SqliteDatabase db;

    public SqliteTransactionLog(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void record(LedgerEntry entry) {
        try (Connection conn = db.connection();
             var stmt = conn.prepareStatement(
                 "INSERT INTO wallet_ledger (player_uuid, delta, balance_after, reason, note) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, entry.playerId().toString());
            stmt.setLong(2, entry.delta());
            stmt.setLong(3, entry.balanceAfter());
            stmt.setString(4, entry.reason().name());
            stmt.setString(5, entry.note());
            stmt.executeUpdate();
        } catch (Exception e) {
            // Log but never throw — ledger must never crash the game
            System.err.println("[persistence] Failed to record ledger entry: " + e.getMessage());
        }
    }

    @Override
    public List<LedgerEntry> readRecent(int count) {
        List<LedgerEntry> entries = new ArrayList<>();
        try (Connection conn = db.connection();
             var stmt = conn.prepareStatement(
                 "SELECT player_uuid, delta, balance_after, reason, note, created_at FROM wallet_ledger ORDER BY id DESC LIMIT ?")) {
            stmt.setInt(1, count);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            System.err.println("[persistence] Failed to read ledger: " + e.getMessage());
        }
        return entries;
    }

    @Override
    public List<LedgerEntry> readForPlayer(UUID playerId, int count) {
        List<LedgerEntry> entries = new ArrayList<>();
        try (Connection conn = db.connection();
             var stmt = conn.prepareStatement(
                 "SELECT player_uuid, delta, balance_after, reason, note, created_at FROM wallet_ledger"
                     + " WHERE player_uuid = ? ORDER BY id DESC LIMIT ?")) {
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, count);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            System.err.println("[persistence] Failed to read player ledger: " + e.getMessage());
        }
        return entries;
    }

    private LedgerEntry mapRow(java.sql.ResultSet rs) throws Exception {
        return new LedgerEntry(
            Instant.parse(rs.getString("created_at").replace(" ", "T") + "Z"),
            UUID.fromString(rs.getString("player_uuid")),
            rs.getLong("delta"),
            rs.getLong("balance_after"),
            TransactionReason.valueOf(rs.getString("reason")),
            rs.getString("note")
        );
    }
}
