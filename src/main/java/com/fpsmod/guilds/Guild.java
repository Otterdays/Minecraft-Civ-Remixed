package com.fpsmod.guilds;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Guild {
    public final UUID id;
    public String name;
    public String description;
    public UUID owner;
    public final Set<UUID> officers = new HashSet<>();
    public final Set<UUID> members = new HashSet<>();
    public long balance;
    public long createdAt;
    public BlockPos homePos;
    public String homeDimension;
    public boolean open;

    public Guild(UUID id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.description = "";
        this.balance = 0L;
        this.createdAt = System.currentTimeMillis();
        this.members.add(owner);
        this.open = false;
    }

    public boolean isOwner(UUID player) {
        return owner.equals(player);
    }

    public boolean isOfficer(UUID player) {
        return officers.contains(player) || isOwner(player);
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public int memberCount() {
        return members.size();
    }

    public String roleOf(UUID player) {
        if (isOwner(player)) return "owner";
        if (officers.contains(player)) return "officer";
        if (members.contains(player)) return "member";
        return "none";
    }
}
