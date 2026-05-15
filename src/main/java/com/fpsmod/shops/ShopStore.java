package com.fpsmod.shops;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Persistence boundary for player shop listings.
 *
 * Implementations must be safe to call from the main server thread. The SQLite
 * implementation routes through the shared {@code SqliteDatabase} so writes serialize
 * cleanly with wallet/guild mutations under WAL.
 *
 * Stock-decrement is exposed as an atomic primitive ({@link #decrementStock}) so the
 * future purchase flow can wrap economy debit + inventory delivery + stock decrement
 * inside one ACID transaction without re-implementing the read-modify-write race window.
 */
public interface ShopStore {
    /** Persist a new listing or overwrite an existing one (id is the key). */
    void upsert(ShopListing listing);

    /** Returns the listing or {@code null} if absent. */
    @Nullable ShopListing find(UUID listingId);

    /** All listings, regardless of state. Use sparingly — for /shop list admin views. */
    List<ShopListing> loadAll();

    /** All listings owned by a player. */
    List<ShopListing> loadForOwner(UUID ownerUuid);

    /**
     * Atomically decrement {@code stock} by {@code units} on an OPEN listing.
     * Returns {@code true} if the row was updated; {@code false} if missing, closed, or
     * insufficient stock. Caller is responsible for the matching wallet debit.
     */
    boolean decrementStock(UUID listingId, int units);

    /** Mark a listing CLOSED (owner action) — idempotent. */
    void close(UUID listingId);

    /** Drop a listing entirely. Used by tests and operator cleanup tools. */
    void delete(UUID listingId);
}
