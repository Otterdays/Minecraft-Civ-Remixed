package com.fpsmod.client.guilds;

import com.fpsmod.OogaMod;
import com.fpsmod.guilds.ClaimedChunk;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

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
                    g.fill(px, py, px + chunkPx, py + chunkPx, 0xFF67E8F9);
                } else if (claim != null) {
                    g.fill(px, py, px + chunkPx, py + chunkPx, 0xFF22C55E);
                } else {
                    g.fill(px, py, px + chunkPx, py + chunkPx, 0xFF374151);
                }

                g.fill(px, py + chunkPx - 1, px + chunkPx, py + chunkPx, 0xFF111827);
                g.fill(px + chunkPx - 1, py, px + chunkPx, py + chunkPx, 0xFF111827);
            }
        }

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
