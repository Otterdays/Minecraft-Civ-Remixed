package com.fpsmod.client.guilds;

import com.fpsmod.guilds.ClaimedChunk;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.ChunkPos;

/**
 * Chunk claim overlay — renders a grid of claimed chunks around the player
 * with colour coding (own guild = green, other = red).
 */
public final class GuildChunkOverlay {
    private static boolean visible = false;
    private static int viewRadius = 4;

    private GuildChunkOverlay() {}

    public static void toggle() {
        visible = !visible;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void register() {
        HudRenderCallback.EVENT.register(GuildChunkOverlay::render);
    }

    private static void render(GuiGraphicsExtractor g, float tickDelta) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        ChunkPos center = mc.player.chunkPosition();
        int cx = center.x;
        int cz = center.z;

        // Map dimensions
        int chunkPx = 8;
        int mapW = (viewRadius * 2 + 1) * chunkPx;
        int mapH = mapW;
        int mapX = sw - mapW - 10;
        int mapY = (sh - mapH) / 2;

        // Background
        g.fill(mapX - 2, mapY - 2, mapX + mapW + 2, mapY + mapH + 2, 0xAA111827);
        g.fill(mapX - 1, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, 0xCC1F2937);

        String dim = mc.level.dimension().location().toString();

        for (int dz = -viewRadius; dz <= viewRadius; dz++) {
            for (int dx = -viewRadius; dx <= viewRadius; dx++) {
                int rx = cx + dx;
                int rz = cz + dz;
                int px = mapX + (dx + viewRadius) * chunkPx;
                int py = mapY + (dz + viewRadius) * chunkPx;

                ClaimedChunk claim = null;
                for (ClaimedChunk c : GuildClientState.claims()) {
                    if (c.dimension().equals(dim) && c.chunkX() == rx && c.chunkZ() == rz) {
                        claim = c;
                        break;
                    }
                }

                if (dx == 0 && dz == 0) {
                    // Player position — highlight
                    g.fill(px, py, px + chunkPx, py + chunkPx, 0xFF67E8F9);
                } else if (claim != null) {
                    g.fill(px, py, px + chunkPx, py + chunkPx, 0xFF22C55E);
                } else {
                    g.fill(px, py, px + chunkPx, py + chunkPx, 0xFF374151);
                }

                // Grid lines
                g.fill(px, py + chunkPx - 1, px + chunkPx, py + chunkPx, 0xFF111827);
                g.fill(px + chunkPx - 1, py, px + chunkPx, py + chunkPx, 0xFF111827);
            }
        }

        // Legend
        int legX = mapX;
        int legY = mapY + mapH + 4;
        g.fill(legX, legY, legX + 6, legY + 6, 0xFF374151);
        g.text(mc.font, "unclaimed", legX + 8, legY, 0xFF9CA3AF, false);
        g.fill(legX + 72, legY, legX + 78, legY + 6, 0xFF22C55E);
        g.text(mc.font, "claimed", legX + 80, legY, 0xFF9CA3AF, false);
        g.fill(legX + 130, legY, legX + 136, legY + 6, 0xFF67E8F9);
        g.text(mc.font, "you", legX + 138, legY, 0xFF9CA3AF, false);
    }
}
