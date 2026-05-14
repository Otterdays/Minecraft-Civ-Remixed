package com.fpsmod.client.jobs;

import com.fpsmod.OogaMod;
import com.fpsmod.jobs.JobStatusSnapshotData;
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
    private static final Map<String, String> ICON_BY_KEY = Map.of(
        "miner",      "⛏",  // ⛏
        "lumberjack", "▲",  // ▲ (tree silhouette)
        "farmer",     "✿",  // ✿
        "excavator",  "▤",  // ▤
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
    private static final int H_PADDING   = 6;
    private static final int TEXT_GAP    = 8;
    private static final int SCREEN_PAD  = 4;

    private static final JobsHudConfig CONFIG = new JobsHudConfig();

    private JobsHudOverlay() {}

    public static JobsHudConfig config() {
        return CONFIG;
    }

    public static void register() {
        // Render after the info bar so we sit above the vanilla XP/jump/locator strip without
        // depending on the experience-level text layer specifically.
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.INFO_BAR,
            OVERLAY_ID,
            JobsHudOverlay::render
        );
    }

    public static void renderPreview(
        GuiGraphicsExtractor g,
        Font font,
        int x,
        int y,
        int maxWidth,
        float scale,
        JobStatusSnapshotData.JobProgressEntry payload
    ) {
        if (payload == null || payload.jobId == null || payload.jobId.isEmpty()) return;
        int barW = resolveBarWidth(font, payload, scale, Math.max(80, maxWidth));
        int barH = Math.max(14, (int) (BAR_BASE_H * scale));
        drawBar(g, font, x, y, barW, barH, payload);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        if (!CONFIG.visible()) return;

        JobStatusSnapshotData.JobProgressEntry p = JobsClientState.primaryActiveJob();
        if (p == null || p.jobId == null || p.jobId.isEmpty()) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        float scale = CONFIG.scale();
        int maxWidth = Math.max(80, screenW - (SCREEN_PAD * 2));
        int barW = resolveBarWidth(font, p, scale, maxWidth);
        int barH = Math.max(14, (int) (BAR_BASE_H * scale));

        // Anchor: above vanilla XP bar (~y = screenH - 32). Offset Y is "pixels higher than that".
        int anchorY = clamp(screenH - 32 - barH - CONFIG.offsetY(), SCREEN_PAD, screenH - SCREEN_PAD - barH);
        int anchorX = clamp((screenW - barW) / 2 + CONFIG.offsetX(), SCREEN_PAD, screenW - SCREEN_PAD - barW);

        drawBar(g, font, anchorX, anchorY, barW, barH, p);
    }

    private static void drawBar(
        GuiGraphicsExtractor g,
        Font font,
        int x0,
        int y0,
        int barW,
        int barH,
        JobStatusSnapshotData.JobProgressEntry p
    ) {
        int x1 = x0 + barW;
        int y1 = y0 + barH;
        // Background + border.
        g.fill(x0, y0, x1, y1, BG);
        outline(g, x0, y0, x1, y1, BORDER);

        // XP fill (cap to current/next thresholds, guard div-by-zero at max level).
        long range = Math.max(1L, p.xpForNextLevel - p.xpForLevel);
        long inLevel = Math.max(0L, p.xp - p.xpForLevel);
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
        String icon = resolveIcon(p);
        String right = rightLabel(p, inLevel, range);
        int rightW = font.width(right);
        int leftX = x0 + H_PADDING;
        int rightX = x1 - H_PADDING - rightW;
        int leftMaxWidth = Math.max(0, rightX - leftX - TEXT_GAP);
        String left = fitLeftLabel(font, p, icon, leftMaxWidth);
        int textY = y0 + (barH - 8) / 2 - 2;
        g.text(font, left, leftX, textY, TEXT_LIGHT, false);
        g.text(font, right, rightX, textY, TEXT_AQUA, false);

        // Icon recoloring: redraw just the icon in gold over the white version.
        g.text(font, icon, leftX, textY, TEXT_GOLD, false);
    }

    private static int resolveBarWidth(Font font, JobStatusSnapshotData.JobProgressEntry payload, float scale, int maxWidth) {
        int scaledBaseWidth = Math.max(80, (int) (BAR_BASE_W * scale));
        long range = Math.max(1L, payload.xpForNextLevel - payload.xpForLevel);
        long inLevel = Math.max(0L, payload.xp - payload.xpForLevel);
        String left = fullLeftLabel(payload);
        String right = rightLabel(payload, inLevel, range);
        int desiredWidth = (H_PADDING * 2) + font.width(left) + TEXT_GAP + font.width(right);
        return Math.min(maxWidth, Math.max(scaledBaseWidth, desiredWidth));
    }

    private static String fullLeftLabel(JobStatusSnapshotData.JobProgressEntry p) {
        String icon = resolveIcon(p);
        String name = displayLabel(p).toUpperCase(java.util.Locale.ROOT);
        return icon + "  " + name + "  Lvl " + p.level;
    }

    private static String rightLabel(JobStatusSnapshotData.JobProgressEntry p, long inLevel, long range) {
        if (p.level >= p.maxLevel) {
            return "MAX";
        }
        return inLevel + " / " + range + " xp";
    }

    private static String fitLeftLabel(Font font, JobStatusSnapshotData.JobProgressEntry p, String icon, int maxWidth) {
        String suffix = "  Lvl " + p.level;
        String prefix = icon + "  ";
        int suffixWidth = font.width(suffix);
        int prefixWidth = font.width(prefix);
        if (maxWidth <= prefixWidth) {
            return icon;
        }
        if (maxWidth <= prefixWidth + suffixWidth) {
            return trimToWidth(font, prefix + suffix, maxWidth);
        }

        String name = displayLabel(p).toUpperCase(java.util.Locale.ROOT);
        int nameWidth = Math.max(0, maxWidth - prefixWidth - suffixWidth);
        return prefix + trimToWidth(font, name, nameWidth) + suffix;
    }

    private static String resolveIcon(JobStatusSnapshotData.JobProgressEntry p) {
        if (p.iconGlyph != null && !p.iconGlyph.isBlank()) {
            return p.iconGlyph;
        }
        if (p.iconKey != null && ICON_BY_KEY.containsKey(p.iconKey)) {
            return ICON_BY_KEY.get(p.iconKey);
        }
        if (p.jobId != null && ICON_BY_KEY.containsKey(p.jobId)) {
            return ICON_BY_KEY.get(p.jobId);
        }
        return "*";
    }

    private static String displayLabel(JobStatusSnapshotData.JobProgressEntry p) {
        if (p.shortLabel != null && !p.shortLabel.isBlank()) {
            return p.shortLabel;
        }
        if (p.displayName != null && !p.displayName.isBlank()) {
            return p.displayName;
        }
        return p.jobId == null ? "JOB" : p.jobId;
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (maxWidth <= 0 || text.isEmpty()) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth > maxWidth) {
            return "";
        }
        int len = text.length();
        while (len > 0 && font.width(text.substring(0, len)) + ellipsisWidth > maxWidth) {
            len--;
        }
        return len <= 0 ? ellipsis : text.substring(0, len) + ellipsis;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void outline(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }
}
