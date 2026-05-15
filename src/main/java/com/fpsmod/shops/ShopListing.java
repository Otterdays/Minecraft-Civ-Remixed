package com.fpsmod.shops;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One player-owned shop listing.
 *
 * Persistence shape (M4 foundation): a listing is a durable offer to sell {@code unitCount}
 * of {@code itemId} (with optional canonical {@code itemNbt}) at {@code unitPrice} coins per
 * unit, with {@code stock} units remaining. Optional physical anchor at
 * {@code dimension/posX/posY/posZ} ties the listing to a chest or sign in-world.
 *
 * State machine: {@code OPEN} → {@code SOLD_OUT} (auto when stock hits 0) or {@code CLOSED}
 * (owner action). Closed/sold-out rows are kept for audit; pruning is a separate operation.
 */
public final class ShopListing {
    public enum State { OPEN, CLOSED, SOLD_OUT }

    public final UUID id;
    public final UUID ownerUuid;
    public String itemId;
    /** Canonical SNBT for matching; {@code null} = vanilla item with no NBT. */
    @Nullable public String itemNbt;
    public int unitCount;
    public long unitPrice;
    public int stock;
    @Nullable public String dimension;
    @Nullable public Integer posX;
    @Nullable public Integer posY;
    @Nullable public Integer posZ;
    public State state;
    public long createdAt;
    public long updatedAt;

    public ShopListing(UUID id, UUID ownerUuid, String itemId, int unitCount, long unitPrice, int stock) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.itemId = itemId;
        this.unitCount = unitCount;
        this.unitPrice = unitPrice;
        this.stock = stock;
        this.state = State.OPEN;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ShopListing fresh(UUID owner, String itemId, int unitCount, long unitPrice, int stock) {
        return new ShopListing(UUID.randomUUID(), owner, itemId, unitCount, unitPrice, stock);
    }

    public boolean isOpen() {
        return state == State.OPEN && stock > 0;
    }
}
