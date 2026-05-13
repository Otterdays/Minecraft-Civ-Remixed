package com.fpsmod.client.jobs;

import com.fpsmod.OogaMod;
import com.fpsmod.jobs.net.JobStatusPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Compact job bar drawn above the vanilla XP bar. Shows:
 * <pre>
 * ┌ ⛏ MINER · Lvl 12 ─────────── 240/520 xp ┐
 * ▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░
 * </pre>
 * Layout is centered horizontally with operator-tunable X/Y offset and scale via {@link JobsHudConfig}.
 */
public final class JobsHudOverlay {
    private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "jobs_overlay");

    /** Plain BMP code points that Minecraft's unifont resolves reliably across resource packs. */
    private static final Map<String, String> ICON_BY_SLUG = Map.of(
        "miner",      "⛏",  // ⛏
        "lumberjack", "▲",  // ▲ (tree silhouette)
        "farmer",     "✿",  // ✿
        "fighter",    "⚔"   // ⚔
    );

    private static final int BAR_BASE_W = 160;
    private static final int BAR_BASE_H = 22;
    private static final int BG          = 0xC0111827;
    private static final int BORDER      = 0xFF1F2937;
    private static final int FILL_FROM   = 0xFFE9B949;
    private static final int FILL_TO     = 0xFFFCD34D;
    private static final int FILL_EMPTY  = 0xFF1E293B;
    private static final int TEXT_LIGHT  = 0xFFE5E7EB;
    private static final int TEXT_GOLD   = 0xFFE9B949;
    private static final int TEXT_AQUA   = 0xFF67E8F9;

    private static final JobsHudConfig CONFIG = new JobsHudConfig();

    private JobsHudOverlay() {}

    public static JobsHudConfig config() {
        return CONFIG;
    }

    public static void register() {
        // Render after the experience level (which itself draws above the XP bar) so our overlay
        // paints on top. Position is computed manually relative to vanilla XP bar Y.
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.EXPERIENCE_LEVEL,
            OVERLAY_ID,
            JobsHudOverlay::render
        );
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        if (!CONFIG.visible()) return;

        JobStatusPayload p = JobsClientState.latest();
        if (p == null || p.slug().isEmpty()) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        float scale = CONFIG.scale();
        int barW = Math.max(80, (int) (BAR_BASE_W * scale));
        int barH = Math.max(14, (int) (BAR_BASE_H * scale));

        // Anchor: above vanilla XP bar (~y = screenH - 32). Offset Y is "pixels higher than that".
        int anchorY = screenH - 32 - barH - CONFIG.offsetY();
        int anchorX = (screenW - barW) / 2 + CONFIG.offsetX();

        int x0 = anchorX;
        int y0 = anchorY;
        int x1 = anchorX + barW;
        int y1 = anchorY + barH;

        // Background + border.
        g.fill(x0, y0, x1, y1, BG);
        outline(g, x0, y0, x1, y1, BORDER);

        // XP fill (cap to current/next thresholds, guard div-by-zero at max level).
        long range = Math.max(1L, p.xpForNextLevel() - p.xpForLevel());
        long inLevel = Math.max(0L, p.xp() - p.xpForLevel());
        float pct = Math.min(1f, Math.max(0f, (float) inLevel / (float) range));

        int innerPad = 3;
        int innerX0 = x0 + innerPad;
        int innerX1 = x1 - innerPad;
        int innerY1 = y1 - innerPad;
        int innerY0 = innerY1 - 3;
        g.fill(innerX0, innerY0, innerX1, innerY1, FILL_EMPTY);
        int fillW = (int) ((innerX1 - innerX0) * pct);
        if (fillW > 0) {
            // Two-tone gradient: simple horizontal split for a "shine" cue.
            int mid = innerX0 + Math.max(1, fillW / 2);
            g.fill(innerX0, innerY0, mid, innerY1, FILL_FROM);
            g.fill(mid, innerY0, innerX0 + fillW, innerY1, FILL_TO);
        }

        // Icon + label line.
        String icon = ICON_BY_SLUG.getOrDefault(p.slug(), "*");
        String name = p.slug().toUpperCase(java.util.Locale.ROOT);
        String left = icon + "  " + name + "  Lvl " + p.level();
        int textY = y0 + (barH - 8) / 2 - 2;
        g.text(font, left, x0 + 6, textY, TEXT_LIGHT, false);

        String right;
        if (p.level() >= 50) {
            right = "MAX";
        } else {
            right = inLevel + " / " + range + " xp";
        }
        int rightW = font.width(right);
        g.text(font, right, x1 - 6 - rightW, textY, TEXT_AQUA, false);

        // Icon recoloring: redraw just the icon in gold over the white version.
        g.text(font, icon, x0 + 6, textY, TEXT_GOLD, false);
    }

    private static void outline(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }
}
