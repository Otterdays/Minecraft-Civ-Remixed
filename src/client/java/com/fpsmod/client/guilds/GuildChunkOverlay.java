package com.fpsmod.client.guilds;

import com.fpsmod.OogaMod;
import com.fpsmod.guilds.ClaimedChunk;
import com.fpsmod.guilds.net.GuildStatusPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.UUID;

@SuppressWarnings("null")
public final class GuildChunkOverlay {
    private static boolean visible = false;
    private static int viewRadius = 4;
    private static long hideAt = 0L;
    private static final Identifier OVERLAY_ID =
        Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "guild_chunk_overlay");

    private GuildChunkOverlay() {}

    public static void show(int seconds) {
        visible = true;
        hideAt = System.currentTimeMillis() + seconds * 1000L;
    }

    public static void hide() {
        visible = false;
        hideAt = 0L;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.INFO_BAR,
            OVERLAY_ID,
            GuildChunkOverlay::render
        );
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!visible) return;
        if (hideAt > 0L && System.currentTimeMillis() > hideAt) {
            visible = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int cx = mc.player.blockPosition().getX() >> 4;
        int cz = mc.player.blockPosition().getZ() >> 4;

        int chunkPx = 8;
        int mapW = (viewRadius * 2 + 1) * chunkPx;
        int mapH = mapW;
        int mapX = sw - mapW - 10;
        int mapY = (sh - mapH) / 2;

        g.fill(mapX - 2, mapY - 2, mapX + mapW + 2, mapY + mapH + 2, 0xAA111827);
        g.fill(mapX - 1, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, 0xCC1F2937);

        String dim = mc.level.dimension().identifier().toString();
        ClientLevel level = mc.level;
        UUID myGuild = myGuildId();

        for (int dz = -viewRadius; dz <= viewRadius; dz++) {
            for (int dx = -viewRadius; dx <= viewRadius; dx++) {
                int rx = cx + dx;
                int rz = cz + dz;
                int px = mapX + (dx + viewRadius) * chunkPx;
                int py = mapY + (dz + viewRadius) * chunkPx;

                ClaimedChunk claim = GuildClientState.claimAt(dim, rx, rz);
                boolean isPlayerCell = dx == 0 && dz == 0;

                int base = terrainArgb(level, dim, rx, rz);
                g.fill(px, py, px + chunkPx, py + chunkPx, base);

                if (claim != null) {
                    boolean mine = myGuild != null && claim.guildId().equals(myGuild);
                    int tint = mine ? 0x5522C55E : 0x55F97316;
                    g.fill(px, py, px + chunkPx, py + chunkPx, tint);
                }

                if (isPlayerCell) {
                    g.fill(px, py, px + chunkPx, py + chunkPx, 0x4467E8F9);
                    drawFacingWedge(g, px, py, chunkPx, mc.player.getYRot());
                }

                g.fill(px, py + chunkPx - 1, px + chunkPx, py + chunkPx, 0xFF111827);
                g.fill(px + chunkPx - 1, py, px + chunkPx, py + chunkPx, 0xFF111827);
            }
        }

        int legX = mapX;
        int legY = mapY + mapH + 4;
        g.fill(legX, legY, legX + 6, legY + 6, 0xFF6B7280);
        g.text(mc.font, "terrain", legX + 8, legY, 0xFF9CA3AF, false);
        g.fill(legX + 52, legY, legX + 58, legY + 6, 0xFF22C55E);
        g.text(mc.font, "yours", legX + 60, legY, 0xFF9CA3AF, false);
        g.fill(legX + 100, legY, legX + 106, legY + 6, 0xFFF97316);
        g.text(mc.font, "other", legX + 108, legY, 0xFF9CA3AF, false);
        g.fill(legX + 150, legY, legX + 156, legY + 6, 0xFF67E8F9);
        g.text(mc.font, "you + facing", legX + 158, legY, 0xFF9CA3AF, false);
    }

    private static UUID myGuildId() {
        GuildStatusPayload.ClientGuildInfo info = GuildClientState.guildInfo();
        if (info == null || info.guildId == null || info.guildId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(info.guildId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int terrainArgb(ClientLevel level, String dim, int chunkX, int chunkZ) {
        if (!dim.equals(level.dimension().identifier().toString())) {
            return 0xFF374151;
        }
        int wx = chunkX * 16 + 8;
        int wz = chunkZ * 16 + 8;
        if (!level.getWorldBorder().isWithinBounds(wx, wz)) {
            return 0xFF1F2937;
        }
        if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            return 0xFF4B5563;
        }
        try {
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
            BlockPos pos = new BlockPos(wx, y, wz);
            BlockState state = level.getBlockState(pos);
            MapColor mapColor = state.getMapColor(level, pos);
            if (mapColor == MapColor.NONE) {
                return 0xFF374151;
            }
            return 0xFF000000 | mapColor.calculateARGBColor(MapColor.Brightness.NORMAL);
        } catch (RuntimeException e) {
            return 0xFF374151;
        }
    }

    /**
     * Draws a small wedge from the cell center in the horizontal facing direction (Minecraft yaw).
     */
    private static void drawFacingWedge(GuiGraphicsExtractor g, int cellX, int cellY, int cellSize, float yawDeg) {
        float yawRad = (float) Math.toRadians(yawDeg);
        float fx = -Mth.sin(yawRad);
        float fz = Mth.cos(yawRad);
        int cx = cellX + cellSize / 2;
        int cy = cellY + cellSize / 2;
        int tipX = cx + Mth.floor(fx * (cellSize / 2 + 2));
        int tipY = cy + Mth.floor(fz * (cellSize / 2 + 2));
        fillThickLine(g, cx, cy, tipX, tipY, 0xFFFFFFFF, 2);
    }

    private static void fillThickLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        int half = Math.max(1, thickness / 2);
        while (true) {
            g.fill(x - half, y - half, x + half + 1, y + half + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
