package com.fpsmod.guilds;

public final class GuildConfig {
    public boolean enabled = true;
    public int maxMembers = 20;
    public int maxOfficers = 4;
    public int maxClaims = 16;
    public int claimCost = 100;
    public int creationCost = 250;
    public int minNameLength = 3;
    public int maxNameLength = 24;
    public boolean disbandRefundClaims = true;
    public boolean allowOpenGuilds = true;
    public int homeTeleportCooldownSeconds = 60;

    // Protection settings
    public boolean protectBlocks = true;
    public boolean protectContainers = true;
    public boolean protectInteractables = false;
    public boolean allowMemberBuild = true;
    public boolean allowOfficerBuild = true;
    public boolean pvpInClaims = true;
    public boolean showChunkBorders = true;

    // Map / overlay
    public int mapRadius = 4;
    public int overlayDurationSeconds = 30;

    // Treasury
    /** Max coins any single /guild deposit can move. 0 = no cap. */
    public long maxTreasuryDeposit = 0L;
    /** Max coins any single /guild withdraw can move. 0 = no cap. */
    public long maxTreasuryWithdraw = 0L;

    // Debug
    /**
     * When true, every protection decision (canModifyBlock / canAccess) is logged at INFO
     * with the player, chunk, claim owner, and precedence path used. Disable in production.
     */
    public boolean debugProtection = false;
}
