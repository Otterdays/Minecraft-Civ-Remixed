package com.fpsmod.persistence;

import com.fpsmod.OogaMod;
import com.fpsmod.shops.ShopListing;
import com.fpsmod.shops.ShopStore;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite-backed {@link ShopStore}. Schema lives in {@link SchemaMigrator} v2.
 *
 * Atomic stock decrement uses a guarded UPDATE — the {@code WHERE state='open' AND stock>=?}
 * predicate ensures concurrent buyers cannot race past the row's available units.
 */
public final class SqliteShopStore implements ShopStore {
    private final SqliteDatabase db;

    public SqliteShopStore(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public void upsert(ShopListing listing) {
        long now = System.currentTimeMillis();
        listing.updatedAt = now;
        if (listing.stock <= 0 && listing.state == ShopListing.State.OPEN) {
            listing.state = ShopListing.State.SOLD_OUT;
        }
        try (Connection conn = db.connection();
             PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO shop_listings
                  (id, owner_uuid, item_id, item_nbt, unit_count, unit_price, stock,
                   dimension, pos_x, pos_y, pos_z, state, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                  item_id=excluded.item_id,
                  item_nbt=excluded.item_nbt,
                  unit_count=excluded.unit_count,
                  unit_price=excluded.unit_price,
                  stock=excluded.stock,
                  dimension=excluded.dimension,
                  pos_x=excluded.pos_x,
                  pos_y=excluded.pos_y,
                  pos_z=excluded.pos_z,
                  state=excluded.state,
                  updated_at=excluded.updated_at
                """)) {
            ps.setString(1, listing.id.toString());
            ps.setString(2, listing.ownerUuid.toString());
            ps.setString(3, listing.itemId);
            ps.setString(4, listing.itemNbt);
            ps.setInt(5, listing.unitCount);
            ps.setLong(6, listing.unitPrice);
            ps.setInt(7, listing.stock);
            ps.setString(8, listing.dimension);
            setIntOrNull(ps, 9, listing.posX);
            setIntOrNull(ps, 10, listing.posY);
            setIntOrNull(ps, 11, listing.posZ);
            ps.setString(12, listing.state.name());
            ps.setLong(13, listing.createdAt);
            ps.setLong(14, listing.updatedAt);
            ps.executeUpdate();
        } catch (Exception e) {
            OogaMod.LOGGER.error("[shops] upsert failed for listing {}", listing.id, e);
        }
    }

    @Override
    public @Nullable ShopListing find(UUID listingId) {
        try (Connection conn = db.connection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM shop_listings WHERE id=?")) {
            ps.setString(1, listingId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readRow(rs) : null;
            }
        } catch (Exception e) {
            OogaMod.LOGGER.error("[shops] find failed for {}", listingId, e);
            return null;
        }
    }

    @Override
    public List<ShopListing> loadAll() {
        return queryList("SELECT * FROM shop_listings ORDER BY created_at DESC", null);
    }

    @Override
    public List<ShopListing> loadForOwner(UUID ownerUuid) {
        return queryList("SELECT * FROM shop_listings WHERE owner_uuid=? ORDER BY created_at DESC",
            ownerUuid.toString());
    }

    @Override
    public boolean decrementStock(UUID listingId, int units) {
        if (units <= 0) return false;
        try (Connection conn = db.connection();
             PreparedStatement ps = conn.prepareStatement("""
                UPDATE shop_listings
                   SET stock = stock - ?,
                       state = CASE WHEN stock - ? <= 0 THEN 'SOLD_OUT' ELSE state END,
                       updated_at = ?
                 WHERE id = ?
                   AND state = 'OPEN'
                   AND stock >= ?
                """)) {
            ps.setInt(1, units);
            ps.setInt(2, units);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, listingId.toString());
            ps.setInt(5, units);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            OogaMod.LOGGER.error("[shops] decrementStock failed for {}", listingId, e);
            return false;
        }
    }

    @Override
    public void close(UUID listingId) {
        try (Connection conn = db.connection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE shop_listings SET state='CLOSED', updated_at=? WHERE id=?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, listingId.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            OogaMod.LOGGER.error("[shops] close failed for {}", listingId, e);
        }
    }

    @Override
    public void delete(UUID listingId) {
        try (Connection conn = db.connection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM shop_listings WHERE id=?")) {
            ps.setString(1, listingId.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            OogaMod.LOGGER.error("[shops] delete failed for {}", listingId, e);
        }
    }

    private List<ShopListing> queryList(String sql, @Nullable String singleParam) {
        List<ShopListing> out = new ArrayList<>();
        try (Connection conn = db.connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (singleParam != null) ps.setString(1, singleParam);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(readRow(rs));
            }
        } catch (Exception e) {
            OogaMod.LOGGER.error("[shops] queryList failed", e);
        }
        return out;
    }

    private static ShopListing readRow(ResultSet rs) throws Exception {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID owner = UUID.fromString(rs.getString("owner_uuid"));
        ShopListing l = new ShopListing(
            id, owner,
            rs.getString("item_id"),
            rs.getInt("unit_count"),
            rs.getLong("unit_price"),
            rs.getInt("stock")
        );
        l.itemNbt = rs.getString("item_nbt");
        l.dimension = rs.getString("dimension");
        l.posX = rs.getObject("pos_x") != null ? rs.getInt("pos_x") : null;
        l.posY = rs.getObject("pos_y") != null ? rs.getInt("pos_y") : null;
        l.posZ = rs.getObject("pos_z") != null ? rs.getInt("pos_z") : null;
        String stateStr = rs.getString("state");
        l.state = stateStr != null ? ShopListing.State.valueOf(stateStr) : ShopListing.State.OPEN;
        l.createdAt = rs.getLong("created_at");
        l.updatedAt = rs.getLong("updated_at");
        return l;
    }

    private static void setIntOrNull(PreparedStatement ps, int idx, @Nullable Integer v) throws Exception {
        if (v == null) ps.setNull(idx, java.sql.Types.INTEGER);
        else ps.setInt(idx, v);
    }
}
