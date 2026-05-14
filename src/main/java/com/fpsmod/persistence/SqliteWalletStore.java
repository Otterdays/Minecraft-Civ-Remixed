package com.fpsmod.persistence;

import com.fpsmod.economy.WalletLedger;
import com.fpsmod.economy.WalletStore;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SqliteWalletStore implements WalletStore {
    private final SqliteDatabase db;

    public SqliteWalletStore(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public WalletLedger load() {
        Map<UUID, Long> balances = new HashMap<>();
        Map<UUID, String> hints = new HashMap<>();
        try (Connection conn = db.connection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT player_uuid, balance, name_hint FROM wallets")) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_uuid"));
                balances.put(id, rs.getLong("balance"));
                String hint = rs.getString("name_hint");
                if (hint != null && !hint.isEmpty()) {
                    hints.put(id, hint);
                }
            }
        } catch (Exception e) {
            // Table may not exist yet — return empty
        }
        return new WalletLedger(balances, hints);
    }

    @Override
    public void save(Map<UUID, Long> balances, Map<UUID, String> displayHints) {
        try (Connection conn = db.connection()) {
            conn.setAutoCommit(false);
            try (var upsert = conn.prepareStatement(
                "INSERT INTO wallets (player_uuid, balance, name_hint, updated_at) VALUES (?, ?, ?, datetime('now'))"
                + " ON CONFLICT(player_uuid) DO UPDATE SET balance=excluded.balance, name_hint=excluded.name_hint, updated_at=excluded.updated_at")) {
                for (Map.Entry<UUID, Long> e : balances.entrySet()) {
                    upsert.setString(1, e.getKey().toString());
                    upsert.setLong(2, e.getValue());
                    String hint = displayHints.get(e.getKey());
                    upsert.setString(3, hint != null ? hint : null);
                    upsert.addBatch();
                }
                upsert.executeBatch();
            }
            conn.commit();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save wallets", ex);
        }
    }
}
