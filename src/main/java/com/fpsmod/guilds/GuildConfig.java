package com.fpsmod.guilds;

public final class GuildConfig {
    public boolean enabled = true;
    public int maxMembers = 20;
    public int maxClaims = 16;
    public int claimCost = 100;
    public int creationCost = 250;
    public int minNameLength = 3;
    public int maxNameLength = 24;
    public boolean disbandRefundClaims = true;

    // Protection settings
    public boolean protectBlocks = true;
    public boolean protectContainers = true;
    public boolean protectInteractables = false;
    public boolean allowMemberBuild = true;
    public boolean allowOfficerBuild = true;
    public boolean pvpInClaims = true;
    public boolean showChunkBorders = true;
}
