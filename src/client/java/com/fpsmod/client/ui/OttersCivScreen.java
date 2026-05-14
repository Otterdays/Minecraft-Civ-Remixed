package com.fpsmod.client.ui;

import com.fpsmod.OogaMod;
import com.fpsmod.client.jobs.JobsClientCatalog;
import com.fpsmod.client.jobs.JobsHudOverlay;
import com.fpsmod.client.jobs.JobsClientState;
import com.fpsmod.jobs.JobCatalogSnapshot;
import com.fpsmod.jobs.JobStatusSnapshotData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Otters Civ. Revived — stylized in-game hub. Opens from client-side {@code /otter}.
 * Pure-geometry rendering (no asset textures) so it stays asset-pipeline-free for v1.
 *
 * <p>26.1 Screen API: vanilla calls {@link #extractRenderState} every frame to record
 * draw commands into the render graph. We render the panel directly here; mouse input
 * arrives via {@link #mouseClicked(MouseButtonEvent, boolean)}.
 */
public final class OttersCivScreen extends Screen {

    private enum Tab {
        HOME("Home", "Overview & roadmap"),
        WALLET("Wallet", "Money & ledger"),
        JOBS("Jobs", "Profession & HUD bar"),
        GUILDS("Guilds", "Groups & claims"),
        REWARDS("Rewards", "Payouts & tuning"),
        CIV("Civ", "Shops · gov · future"),
        HELP("Help", "Commands & docs");

        final String label;
        final String subtitle;

        Tab(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    /** Color-coded chip for shipped vs planned. */
    private enum Status {
        SHIPPED("LIVE",     0xFF22C55E),
        PARTIAL("PARTIAL",  0xFFE9B949),
        PLANNED("SOON",     0xFF67E8F9),
        FUTURE("FUTURE",    0xFF6B7280);

        final String label;
        final int color;

        Status(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private static final int PANEL_BG       = 0xFF111827;
    private static final int PANEL_BG_ALT   = 0xFF0B1220;
    private static final int PANEL_BORDER   = 0xFF1F2937;
    private static final int ACCENT_GOLD    = 0xFFE9B949;
    private static final int ACCENT_AQUA    = 0xFF67E8F9;
    private static final int TEXT_PRIMARY   = 0xFFE5E7EB;
    private static final int TEXT_MUTED     = 0xFF9CA3AF;
    private static final int TEXT_DIM       = 0xFF6B7280;
    private static final int HOVER_BG       = 0xFF1E293B;
    private static final int BTN_BG         = 0xFF1F2937;
    private static final int BTN_BG_HOVER   = 0xFF334155;

    private static final int PANEL_W = 520;
    private static final int PANEL_H = 300;
    private static final int SIDEBAR_W = 128;
    private static final int TAB_H = 28;
    private static final int COMFORT_MIN_W = PANEL_W + 120;
    private static final int COMFORT_MIN_H = PANEL_H + 80;
    private static final long ANIM_MS = 180L;

    private final long openedAt = System.currentTimeMillis();
    private Tab active = Tab.HOME;
    private final List<Rect> hotspots = new ArrayList<>();
    private int jobsPage = 0;

    public OttersCivScreen() {
        super(Component.literal("Otters Civ. Revived"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        float t = Mth.clamp((System.currentTimeMillis() - openedAt) / (float) ANIM_MS, 0f, 1f);
        float eased = easeOutCubic(t);
        int dimAlpha = (int) (0xCC * eased) & 0xFF;
        g.fill(0, 0, this.width, this.height, (dimAlpha << 24) | 0x0A0E1A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        float t = Mth.clamp((System.currentTimeMillis() - openedAt) / (float) ANIM_MS, 0f, 1f);
        float eased = easeOutCubic(t);
        int yOffset = (int) ((1f - eased) * 14f);

        int px = (this.width - PANEL_W) / 2;
        int py = (this.height - PANEL_H) / 2 + yOffset;

        hotspots.clear();

        if (isCompactFallbackNeeded()) {
            renderCompactFallback(g, yOffset, mouseX, mouseY);
            return;
        }

        // Drop shadow.
        g.fill(px + 3, py + 6, px + PANEL_W + 3, py + PANEL_H + 6, 0x66000000);
        // Panel body with shaved corners.
        roundedFill(g, px, py, px + PANEL_W, py + PANEL_H, PANEL_BG);
        // Top accent stripe.
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + 2, ACCENT_GOLD);
        // Sidebar background.
        g.fill(px + 1, py + 2, px + SIDEBAR_W, py + PANEL_H - 1, PANEL_BG_ALT);
        // Sidebar divider.
        g.fill(px + SIDEBAR_W, py + 2, px + SIDEBAR_W + 1, py + PANEL_H - 1, PANEL_BORDER);
        // Outer border.
        outlineRect(g, px, py, px + PANEL_W, py + PANEL_H, PANEL_BORDER);

        drawTitle(g, px + 8, py + 8);

        int tabsY = py + 38;
        for (Tab tab : Tab.values()) {
            int x0 = px + 4;
            int x1 = px + SIDEBAR_W - 4;
            int y0 = tabsY;
            int y1 = tabsY + TAB_H;
            boolean hover = inside(mouseX, mouseY, x0, y0, x1, y1);
            boolean selected = tab == active;
            renderTab(g, x0, y0, x1, y1, tab, selected, hover);
            hotspots.add(new Rect(x0, y0, x1, y1, "tab:" + tab.name()));
            tabsY += TAB_H + 2;
        }

        int cx = px + SIDEBAR_W + 14;
        int cy = py + 12;
        int cw = PANEL_W - SIDEBAR_W - 22;
        int ch = PANEL_H - 34;
        renderContent(g, cx, cy, cw, ch, mouseX, mouseY);

        // Footer band — separates content from chrome and prevents overlap with tab content.
        int footerY = py + PANEL_H - 14;
        g.fill(px + SIDEBAR_W + 1, footerY, px + PANEL_W - 1, footerY + 1, PANEL_BORDER);
        String left = "Mod id: project_ooga · /otter · /money";
        String hint = "ESC to close";
        g.text(this.font, left, px + SIDEBAR_W + 10, footerY + 4, TEXT_DIM, false);
        g.text(this.font, hint, px + PANEL_W - 6 - this.font.width(hint), footerY + 4, TEXT_DIM, false);
    }

    private void drawTitle(GuiGraphicsExtractor g, int x, int y) {
        Font f = this.font;
        g.text(f, "OTTERS CIV.", x, y, ACCENT_GOLD, false);
        g.text(f, "REVIVED", x, y + 10, ACCENT_AQUA, false);
        g.fill(x + 62, y + 5, x + 66, y + 9, ACCENT_GOLD);
    }

    private boolean isCompactFallbackNeeded() {
        return this.width < COMFORT_MIN_W || this.height < COMFORT_MIN_H;
    }

    private void renderCompactFallback(GuiGraphicsExtractor g, int yOffset, int mouseX, int mouseY) {
        int cardW = Math.min(360, Math.max(120, this.width - 24));
        int cardH = cardW >= 320 ? 190 : 214;
        int px = (this.width - cardW) / 2;
        int py = (this.height - cardH) / 2 + yOffset;

        g.fill(px + 3, py + 6, px + cardW + 3, py + cardH + 6, 0x66000000);
        roundedFill(g, px, py, px + cardW, py + cardH, PANEL_BG);
        g.fill(px + 1, py + 1, px + cardW - 1, py + 2, ACCENT_GOLD);
        outlineRect(g, px, py, px + cardW, py + cardH, PANEL_BORDER);

        sectionHeading(g, px + 12, py + 12, "Window Too Small");
        body(g, px + 12, py + 28, "/otter needs more room to stay usable.", TEXT_PRIMARY);
        body(g, px + 12, py + 42, "Current GUI: " + this.width + " x " + this.height, TEXT_MUTED);
        body(g, px + 12, py + 56, "Recommended: at least " + COMFORT_MIN_W + " x " + COMFORT_MIN_H, TEXT_MUTED);
        body(g, px + 12, py + 76, "Try one of these:", TEXT_PRIMARY);

        int btnY = py + 90;
        int gap = 6;
        if (cardW >= 320) {
            int btnW = (cardW - 24 - gap * 3) / 4;
            renderButton(g, px + 12, btnY, btnW, 20, "/money",     "action:money",     mouseX, mouseY);
            renderButton(g, px + 12 + btnW + gap, btnY, btnW, 20, "/job",       "jobs:cmd_stats",  mouseX, mouseY);
            renderButton(g, px + 12 + (btnW + gap) * 2, btnY, btnW, 20, "/guild info", "guild:info", mouseX, mouseY);
            renderButton(g, px + 12 + (btnW + gap) * 3, btnY, btnW, 20, "/guild map", "guild:map", mouseX, mouseY);
        } else {
            int btnW = (cardW - 24 - gap) / 2;
            renderButton(g, px + 12, btnY, btnW, 20, "/money",     "action:money",   mouseX, mouseY);
            renderButton(g, px + 12 + btnW + gap, btnY, btnW, 20, "/job",       "jobs:cmd_stats", mouseX, mouseY);
            renderButton(g, px + 12, btnY + 24, cardW - 24, 20, "/guild info", "guild:info",     mouseX, mouseY);
            renderButton(g, px + 12, btnY + 48, cardW - 24, 20, "/guild map", "guild:map",       mouseX, mouseY);
        }

        String hint = "ESC to close";
        g.text(this.font, hint, px + cardW - 12 - this.font.width(hint), py + cardH - 14, TEXT_DIM, false);
    }

    private void renderTab(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, Tab tab, boolean selected, boolean hover) {
        int bg = selected ? HOVER_BG : (hover ? 0xFF182334 : 0);
        if (bg != 0) {
            g.fill(x0, y0, x1, y1, bg);
        }
        if (selected) {
            g.fill(x0, y0, x0 + 2, y1, ACCENT_GOLD);
        }
        int color = selected || hover ? TEXT_PRIMARY : TEXT_MUTED;
        int textX = x0 + 8;
        int maxW = (x1 - 2) - textX;
        g.text(this.font, fit(tab.label, maxW),    textX, y0 + 6,  color, false);
        g.text(this.font, fit(tab.subtitle, maxW), textX, y0 + 17, selected ? ACCENT_AQUA : TEXT_DIM, false);
    }

    /** Truncates with ellipsis so labels never bleed past the sidebar. */
    private String fit(String s, int maxWidth) {
        Font f = this.font;
        if (f.width(s) <= maxWidth) {
            return s;
        }
        String ell = "…";
        int eW = f.width(ell);
        int i = s.length();
        while (i > 0 && f.width(s.substring(0, i)) + eW > maxWidth) {
            i--;
        }
        return i <= 0 ? ell : s.substring(0, i) + ell;
    }

    private void renderContent(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        switch (active) {
            case HOME -> renderHome(g, x, y, w, h, mouseX, mouseY);
            case WALLET -> renderWallet(g, x, y, w, h, mouseX, mouseY);
            case REWARDS -> renderRewards(g, x, y, w, h, mouseX, mouseY);
            case JOBS -> renderJobs(g, x, y, w, h, mouseX, mouseY);
            case GUILDS -> renderGuilds(g, x, y, w, h, mouseX, mouseY);
            case CIV -> renderCiv(g, x, y, w, h, mouseX, mouseY);
            case HELP -> renderHelp(g, x, y, w, h);
        }
    }

    private void renderHome(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        // Live status cards row
        var guildInfo = com.fpsmod.client.guilds.GuildClientState.guildInfo();
        var jobStatus = JobsClientState.primaryActiveJob();

        int cardW = (w - 6) / 3;
        int cardH = 36;

        // Wallet card
        g.fill(x, y, x + cardW, y + cardH, PANEL_BG_ALT);
        outlineRect(g, x, y, x + cardW, y + cardH, PANEL_BORDER);
        g.fill(x, y, x + 2, y + cardH, ACCENT_GOLD);
        g.text(this.font, "WALLET", x + 6, y + 4, ACCENT_GOLD, false);
        g.text(this.font, "/money to check balance", x + 6, y + 16, TEXT_MUTED, false);
        hotspots.add(new Rect(x, y, x + cardW, y + cardH, "action:wallet"));

        // Jobs card
        int cx2 = x + cardW + 3;
        g.fill(cx2, y, cx2 + cardW, y + cardH, PANEL_BG_ALT);
        outlineRect(g, cx2, y, cx2 + cardW, y + cardH, PANEL_BORDER);
        g.fill(cx2, y, cx2 + 2, y + cardH, 0xFF22C55E);
        g.text(this.font, "JOBS", cx2 + 6, y + 4, 0xFF22C55E, false);
        if (jobStatus != null) {
            g.text(this.font, jobStatus.shortLabel + " Lv" + jobStatus.level, cx2 + 6, y + 16, TEXT_PRIMARY, false);
        } else {
            g.text(this.font, "No active job", cx2 + 6, y + 16, TEXT_MUTED, false);
        }
        hotspots.add(new Rect(cx2, y, cx2 + cardW, y + cardH, "tab:JOBS"));

        // Guild card
        int cx3 = cx2 + cardW + 3;
        g.fill(cx3, y, cx3 + cardW, y + cardH, PANEL_BG_ALT);
        outlineRect(g, cx3, y, cx3 + cardW, y + cardH, PANEL_BORDER);
        g.fill(cx3, y, cx3 + 2, y + cardH, 0xFF67E8F9);
        g.text(this.font, "GUILD", cx3 + 6, y + 4, 0xFF67E8F9, false);
        if (guildInfo != null) {
            g.text(this.font, guildInfo.name + " · " + guildInfo.memberCount + " members", cx3 + 6, y + 16, TEXT_PRIMARY, false);
        } else {
            g.text(this.font, "Not in a guild", cx3 + 6, y + 16, TEXT_MUTED, false);
        }
        hotspots.add(new Rect(cx3, y, cx3 + cardW, y + cardH, "tab:GUILDS"));

        // Quick action bar
        int btnY = y + cardH + 8;
        int bw = (w - 12) / 3;
        int bh = 20;
        int gap = 6;
        renderButton(g, x,           btnY, bw, bh, "/money",           "action:money",       mouseX, mouseY);
        renderButton(g, x + bw + gap, btnY, bw, bh, "/job list",       "jobs:cmd_list",      mouseX, mouseY);
        renderButton(g, x + (bw + gap)*2, btnY, bw, bh, "/guild info", "guild:info",         mouseX, mouseY);

        // Milestone strip
        int stripY = btnY + 28;
        int segW = (w - 6) / 7;
        Status[] miles = {
            Status.PARTIAL, Status.PARTIAL,
            Status.SHIPPED, Status.SHIPPED,
            Status.PLANNED, Status.FUTURE, Status.FUTURE
        };
        String[] labels = {"M0","M1","M2","M3","M4","M5","M6"};
        for (int i = 0; i < 7; i++) {
            int sx0 = x + i * segW;
            int sx1 = sx0 + segW - 2;
            int sy0 = stripY;
            int sy1 = stripY + 8;
            g.fill(sx0, sy0, sx1, sy1, miles[i].color & 0x99FFFFFF | (miles[i].color & 0xFF000000));
            int tw = this.font.width(labels[i]);
            g.text(this.font, labels[i], sx0 + (segW - 2 - tw) / 2, sy1 + 2, TEXT_MUTED, false);
        }

        // Badge legend
        int legY = stripY + 14;
        drawBadge(g, x,      legY, "LIVE",    Status.SHIPPED.color);
        drawBadge(g, x + 36, legY, "PARTIAL", Status.PARTIAL.color);
        drawBadge(g, x + 84, legY, "SOON",    Status.PLANNED.color);
        drawBadge(g, x +124, legY, "FUTURE",  Status.FUTURE.color);

        body(g, x, legY + 16, "Click any card above to jump to its panel. Full roadmap in CIV tab.", TEXT_DIM);
    }

    /** Small colored chip with label — used for milestone badges. */
    private void drawBadge(GuiGraphicsExtractor g, int x, int y, String label, int color) {
        int tw = this.font.width(label);
        int w = tw + 8;
        int h = 11;
        g.fill(x, y, x + w, y + h, 0x66000000 | (color & 0x00FFFFFF));
        outlineRect(g, x, y, x + w, y + h, color);
        g.text(this.font, label, x + 4, y + 2, color, false);
    }

    private void renderWallet(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Wallet  ·  M1 Economy");
        body(g, x, y + 14, "config/otters_civ_revived/wallet.properties (uuid=balance).", TEXT_MUTED);

        // Command catalog with badges.
        int row = y + 28;
        drawCommandRow(g, x, row,      "/money",                          "show your balance",            Status.SHIPPED);
        drawCommandRow(g, x, row + 12, "/money set <player> <amount>",    "op-only set (GAMEMASTER tier)", Status.SHIPPED);
        drawCommandRow(g, x, row + 24, "/pay <player> <amount>",          "transfer between players",      Status.PLANNED);
        drawCommandRow(g, x, row + 36, "/ooga money add|take <p> <amt>",  "admin grant/burn with reason",  Status.PLANNED);
        drawCommandRow(g, x, row + 48, "Transaction log + audit views",   "immutable ledger, mod queries", Status.PLANNED);
        drawCommandRow(g, x, row + 60, "Transfer caps · cooldowns · fees", "M1 anti-abuse policy",         Status.PLANNED);

        // Actions.
        renderButton(g, x,        y + 110, 130, 22, "Run /money",       "action:money",       mouseX, mouseY);
        renderButton(g, x + 138,  y + 110, 130, 22, "Open Wallet File", "action:open_wallet", mouseX, mouseY);
    }

    private void drawCommandRow(GuiGraphicsExtractor g, int x, int y, String cmd, String note, Status status) {
        drawBadge(g, x, y, status.label, status.color);
        // Badge width up to ~38 px. Leave 44 px lane.
        int textX = x + 44;
        g.text(this.font, cmd, textX, y + 2, TEXT_PRIMARY, false);
        int cw = this.font.width(cmd);
        g.text(this.font, "·  " + note, textX + cw + 6, y + 2, TEXT_DIM, false);
    }

    private void renderRewards(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Rewards  ·  M2 Engine");
        body(g, x, y + 14, "Block tag: otters_civ_revived:currency_blocks", TEXT_PRIMARY);
        body(g, x, y + 26, "Mob tag:   otters_civ_revived:currency_mobs",   TEXT_PRIMARY);

        // Feature catalog with badges.
        int row = y + 44;
        drawCommandRow(g, x, row,      "Tag-driven payouts",              "core mining + combat",         Status.SHIPPED);
        drawCommandRow(g, x, row + 12, "Per-id overrides",                "block_values · entity_values", Status.SHIPPED);
        drawCommandRow(g, x, row + 24, "Cooldowns + dim-blacklist",       "in rewards.json",              Status.SHIPPED);
        drawCommandRow(g, x, row + 36, "Diminishing returns + anti-farm", "M2 acceptance gate",           Status.PLANNED);
        drawCommandRow(g, x, row + 48, "Farming + crafting reward loops", "5 balanced tracks target",     Status.PLANNED);

        renderButton(g, x,         y + 112, 132, 22, "Open rewards.json",  "action:open_rewards",       mouseX, mouseY);
        renderButton(g, x + 140,   y + 112, 132, 22, "Open block_values",  "action:open_block_values",  mouseX, mouseY);
        renderButton(g, x,         y + 138, 132, 22, "Open entity_values", "action:open_entity_values", mouseX, mouseY);
        renderButton(g, x + 140,   y + 138, 132, 22, "Configs Folder",     "action:open_config",        mouseX, mouseY);
    }


    private void renderJobs(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Jobs");
        JobCatalogSnapshot catalog = JobsClientCatalog.latest();
        JobStatusSnapshotData status = JobsClientState.latest();
        JobStatusSnapshotData.JobProgressEntry primary = JobsClientState.primaryActiveJob();
        var hud = JobsHudOverlay.config();
        List<JobCatalogSnapshot.JobDescriptor> visibleJobs = JobsClientCatalog.visibleJobs();
        int pageSize = 3;
        int maxPage = visibleJobs.isEmpty() ? 0 : Math.max(0, (visibleJobs.size() - 1) / pageSize);
        jobsPage = Mth.clamp(jobsPage, 0, maxPage);
        JobStatusSnapshotData.JobProgressEntry preview = primary != null ? primary : previewEntry(visibleJobs);

        String activeLine;
        String levelLine;
        if (status == null || status.activeJobIds == null || status.activeJobIds.isEmpty()) {
            activeLine = "Active: (none)";
            levelLine = "Use /job join <id>. Catalog sync comes from server.";
        } else {
            activeLine = "Active: " + joinIds(status.activeJobIds);
            if (primary != null) {
                long inLevel = Math.max(0L, primary.xp - primary.xpForLevel);
                long range = Math.max(1L, primary.xpForNextLevel - primary.xpForLevel);
                levelLine = "HUD: " + primary.shortLabel + "  ·  Lvl " + primary.level
                    + "  ·  " + inLevel + "/" + range + (primary.level >= primary.maxLevel ? "  (MAX)" : "");
            } else {
                levelLine = "Primary HUD job unavailable.";
            }
        }
        body(g, x, y + 14, activeLine, TEXT_PRIMARY);
        body(g, x, y + 26, levelLine,  TEXT_MUTED);

        // HUD preview + config row.
        sectionHeading(g, x, y + 44, "HUD Bar");
        body(g, x, y + 58,
            primary == null
                ? "Preview below. Live bar appears above vanilla XP after /job join <id>."
                : "Preview mirrors the live bar drawn above vanilla XP when this menu is closed.",
            TEXT_MUTED);
        if (preview != null) {
            JobsHudOverlay.renderPreview(g, this.font, x, y + 72, Math.min(250, w - 4), hud.scale(), preview);
        }
        body(g, x, y + 102,
            "Visible: " + (hud.visible() ? "ON" : "OFF")
                + "   X: " + hud.offsetX()
                + "   Y: " + hud.offsetY()
                + "   Scale: " + String.format(java.util.Locale.ROOT, "%.2f", hud.scale()),
            TEXT_PRIMARY);

        int row1Y = y + 118;
        int row2Y = y + 144;
        int row3Y = y + 170;
        int bw = 54;
        int bh = 22;
        int gap = 6;

        // Row 1: toggle + reset + config opener + slash command shortcuts.
        renderButton(g, x,                        row1Y, bw, bh, hud.visible() ? "Hide" : "Show", "jobs:toggle", mouseX, mouseY);
        renderButton(g, x + (bw + gap),           row1Y, bw, bh, "Reset",                          "jobs:reset",  mouseX, mouseY);
        renderButton(g, x + (bw + gap) * 2,       row1Y, bw, bh, "Jobs cfg",                       "jobs:open_cfg", mouseX, mouseY);
        renderButton(g, x + (bw + gap) * 3,       row1Y, bw, bh, "/job",                           "jobs:cmd_stats", mouseX, mouseY);
        renderButton(g, x + (bw + gap) * 4,       row1Y, bw, bh, "/job list",                      "jobs:cmd_list",  mouseX, mouseY);

        // Row 2: nudge X/Y/scale.
        int nb = 28;
        int nbgap = 4;
        int rx = x;
        g.text(this.font, "X", rx, row2Y + 7, TEXT_MUTED, false);
        renderButton(g, rx + 12,                 row2Y, nb, bh, "−",  "jobs:x-", mouseX, mouseY);
        renderButton(g, rx + 12 + nb + nbgap,    row2Y, nb, bh, "+",  "jobs:x+", mouseX, mouseY);

        rx = x + 80;
        g.text(this.font, "Y", rx, row2Y + 7, TEXT_MUTED, false);
        renderButton(g, rx + 12,                 row2Y, nb, bh, "−",  "jobs:y-", mouseX, mouseY);
        renderButton(g, rx + 12 + nb + nbgap,    row2Y, nb, bh, "+",  "jobs:y+", mouseX, mouseY);

        rx = x + 160;
        g.text(this.font, "Scale", rx, row2Y + 7, TEXT_MUTED, false);
        renderButton(g, rx + 32,                 row2Y, nb, bh, "−",  "jobs:s-", mouseX, mouseY);
        renderButton(g, rx + 32 + nb + nbgap,    row2Y, nb, bh, "+",  "jobs:s+", mouseX, mouseY);

        sectionHeading(g, x, row3Y, "Catalog");
        body(g, x, row3Y + 14,
            canOpenServerJobsConfig()
                ? "Jobs cfg opens host-side jobs.json on this machine."
                : "Remote server: jobs.json lives on server host. Local opener disabled.",
            TEXT_MUTED);
        int navY = row3Y + 28;
        renderButton(g, x, navY, 46, 18, "< Prev", "jobs:page_prev", mouseX, mouseY);
        renderButton(g, x + 52, navY, 46, 18, "Next >", "jobs:page_next", mouseX, mouseY);
        body(g, x + 106, navY + 5, "Page " + (jobsPage + 1) + "/" + (maxPage + 1)
            + "  ·  slots " + activeCount(status) + "/" + maxActiveSlots(catalog), TEXT_PRIMARY);

        int listY = row3Y + 50;
        if (visibleJobs.isEmpty()) {
            body(g, x, listY, "No jobs synced yet. Try /job list after server join.", TEXT_MUTED);
            return;
        }
        int start = jobsPage * pageSize;
        int end = Math.min(visibleJobs.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            JobCatalogSnapshot.JobDescriptor job = visibleJobs.get(i);
            JobStatusSnapshotData.JobProgressEntry progress = findProgress(status, job.id);
            boolean activeJob = isActive(status, job.id);
            String title = (activeJob ? "[A] " : "") + job.shortLabel;
            String note = progress == null
                ? job.description
                : "Lvl " + progress.level + "/" + progress.maxLevel + " · xp " + progress.xp;
            int rowY = listY + (i - start) * 18;
            g.text(this.font, fit(title, 170), x, rowY, activeJob ? ACCENT_GOLD : TEXT_PRIMARY, false);
            g.text(this.font, fit(note, 170), x + 76, rowY, TEXT_MUTED, false);
            if (job.joinable && job.enabled) {
                renderButton(
                    g,
                    x + w - 94,
                    rowY - 4,
                    42,
                    16,
                    activeJob ? "Leave" : "Join",
                    activeJob ? "jobs:leave:" + job.id : "jobs:join:" + job.id,
                    mouseX,
                    mouseY
                );
            }
            renderButton(g, x + w - 46, rowY - 4, 42, 16, "Info", "jobs:info:" + job.id, mouseX, mouseY);
        }
    }

    private void renderGuilds(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        var info = com.fpsmod.client.guilds.GuildClientState.guildInfo();
        if (info == null) {
            sectionHeading(g, x, y, "Guilds");
            body(g, x, y + 14, "You are not in a guild.", TEXT_MUTED);
            body(g, x, y + 28, "Create one with /guild create <name> ($250).", TEXT_PRIMARY);
            body(g, x, y + 40, "Or accept an invite with /guild join.", TEXT_PRIMARY);
            renderButton(g, x, y + 58, w - 2, 20, "Create guild (costs $250)", "guild:quick_create", mouseX, mouseY);
            return;
        }

        int yy = y;
        sectionHeading(g, x, yy, info.name);
        yy += 14;

        // Status chips
        drawBadge(g, x, yy, info.open ? "OPEN" : "INVITE", info.open ? 0xFF22C55E : 0xFFE9B949);
        drawBadge(g, x + 56, yy, info.role.toUpperCase(), 0xFF67E8F9);
        yy += 12;

        // Stat line
        body(g, x, yy, info.memberCount + "/" + info.maxMembers + " members  ·  "
            + info.claimCount + "/" + info.maxClaims + " claims  ·  $" + info.balance
            + "  ·  Home " + (info.hasHome ? "set" : "not set"), TEXT_PRIMARY);
        yy += 14;

        // Action buttons — 2-column grouped layout
        int colW = (w - 4) / 2;
        int bh = 18;
        int gap = 3;

        // Membership column
        sectionHeading(g, x, yy, "Membership");
        yy += 14;
        renderButton(g, x,      yy, colW, bh, "Invite player",       "guild:invite",    mouseX, mouseY);
        yy += bh + gap;
        renderButton(g, x,      yy, colW, bh, "Kick player",         "guild:kick",      mouseX, mouseY);
        yy += bh + gap;
        renderButton(g, x,      yy, colW, bh, "Promote",             "guild:promote",   mouseX, mouseY);
        yy += bh + gap;
        renderButton(g, x,      yy, colW, bh, "Demote",              "guild:demote",    mouseX, mouseY);
        yy += bh + gap;
        renderButton(g, x,      yy, colW, bh, "Leave guild",         "guild:leave",     mouseX, mouseY);

        // Territory column
        int ty = y + 14 + 14; // align with first col
        sectionHeading(g, x + colW + 4, ty, "Territory");
        ty += 14;
        renderButton(g, x + colW + 4, ty, colW, bh, "Claim chunk",     "guild:claim",     mouseX, mouseY);
        ty += bh + gap;
        renderButton(g, x + colW + 4, ty, colW, bh, "Unclaim chunk",   "guild:unclaim",   mouseX, mouseY);
        ty += bh + gap;
        renderButton(g, x + colW + 4, ty, colW, bh, "Chunk map",       "guild:map",       mouseX, mouseY);
        ty += bh + gap;
        renderButton(g, x + colW + 4, ty, colW, bh, "Set home",        "guild:sethome",   mouseX, mouseY);
        ty += bh + gap;
        renderButton(g, x + colW + 4, ty, colW, bh, "Go home",         "guild:home",      mouseX, mouseY);

        // Bottom bar — info + config
        int bottomY = y + h - 14;
        g.fill(x, bottomY, x + w, bottomY + 1, PANEL_BORDER);
        body(g, x, bottomY + 4, "Chunks: green on overlay. Rep: /guild map.", TEXT_DIM);
        renderButton(g, x + w - 76, bottomY + 2, 76, 12, "Open config", "guild:config", mouseX, mouseY);
    }

    private void renderCiv(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Roadmap");
        int yy = y + 14;

        String[][] milestones = {
            {"M3  Guilds & Claims", "SHIPPED", "Create, invite, claim chunks, home TP, protections, map"},
            {"M4  Player Shops",    "PLANNED", "Market UI, escrow, listing caps, tax"},
            {"M5  Governance",      "FUTURE",  "Diplomacy, territory projects, regional bonuses"},
            {"M6  Scale",           "FUTURE",  "SQLite → PostgreSQL, soak testing, RC hardening"}
        };
        for (String[] m : milestones) {
            Status s = switch (m[1]) {
                case "SHIPPED" -> Status.SHIPPED;
                case "PLANNED" -> Status.PLANNED;
                default -> Status.FUTURE;
            };
            g.fill(x, yy, x + w, yy + 24, PANEL_BG_ALT);
            outlineRect(g, x, yy, x + w, yy + 24, PANEL_BORDER);
            g.fill(x, yy, x + 2, yy + 24, s.color);
            g.text(this.font, m[0], x + 6, yy + 3, TEXT_PRIMARY, false);
            drawBadge(g, x + 6 + this.font.width(m[0]) + 4, yy + 3, s.label, s.color);
            g.text(this.font, fit(m[2], w - 16), x + 6, yy + 14, TEXT_MUTED, false);
            yy += 27;
        }

        renderButton(g, x, yy + 4, (w - 3) / 2, 18, "Guilds panel", "tab:GUILDS", mouseX, mouseY);
        renderButton(g, x + (w + 3) / 2, yy + 4, (w - 3) / 2, 18, "Open guilds.json", "guild:config", mouseX, mouseY);
    }

    private void drawMilestone(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, String detail, Status status) {
        g.fill(x, y, x + w, y + h, PANEL_BG_ALT);
        outlineRect(g, x, y, x + w, y + h, PANEL_BORDER);
        // Left accent stripe in status color.
        g.fill(x, y, x + 2, y + h, status.color);
        g.text(this.font, title, x + 8, y + 5, TEXT_PRIMARY, false);
        int titleW = this.font.width(title);
        drawBadge(g, x + 8 + titleW + 8, y + 5, status.label, status.color);
        g.text(this.font, fit(detail, w - 16), x + 8, y + 20, TEXT_MUTED, false);
    }

    private void renderHelp(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        sectionHeading(g, x, y, "All Commands");

        int row = y + 16;
        // Shipped commands.
        drawCommandRow(g, x, row,      "/otter",                          "opens this menu",                 Status.SHIPPED);
        drawCommandRow(g, x, row + 12, "/money",                          "balance",                         Status.SHIPPED);
        drawCommandRow(g, x, row + 24, "/money set <p> <amt>",            "op-tier set",                     Status.SHIPPED);
        drawCommandRow(g, x, row + 36, "/job",                            "active job + progression",        Status.SHIPPED);
        drawCommandRow(g, x, row + 48, "/job list",                       "jobs catalog",                    Status.SHIPPED);
        drawCommandRow(g, x, row + 60, "/job join <slug> · /job leave",   "pick or clear a job",            Status.SHIPPED);

        // Shipped M3 guilds.
        drawCommandRow(g, x, row + 76, "/guild create <name>",            "M3 create ($250)",                Status.SHIPPED);
        drawCommandRow(g, x, row + 88, "/guild invite|join|leave|kick",   "M3 membership",                   Status.SHIPPED);
        drawCommandRow(g, x, row +100, "/guild claim|unclaim|map",        "M3 chunk claims ($100)",          Status.SHIPPED);
        drawCommandRow(g, x, row +112, "/guild promote|demote",           "M3 officer ranks",                Status.SHIPPED);
        drawCommandRow(g, x, row +124, "/guild sethome|home",             "M3 guild teleport",               Status.SHIPPED);
        drawCommandRow(g, x, row +136, "/pay <player> <amount>",          "M1 transfer",                     Status.PLANNED);
        drawCommandRow(g, x, row +148, "Market UI · /shop",               "M4 player shops",                 Status.PLANNED);

        body(g, x, row + 142, "Docs: README.md · index.html · DOCS/ROADMAP.md", TEXT_DIM);
    }

    private void renderButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, String actionKey, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, x + w, y + h);
        int bg = hover ? BTN_BG_HOVER : BTN_BG;
        g.fill(x, y, x + w, y + h, bg);
        outlineRect(g, x, y, x + w, y + h, hover ? ACCENT_AQUA : PANEL_BORDER);
        int tw = this.font.width(label);
        g.text(this.font, label, x + (w - tw) / 2, y + (h - 8) / 2, hover ? ACCENT_GOLD : TEXT_PRIMARY, false);
        hotspots.add(new Rect(x, y, x + w, y + h, actionKey));
    }

    private void sectionHeading(GuiGraphicsExtractor g, int x, int y, String title) {
        g.text(this.font, title, x, y, ACCENT_GOLD, false);
        g.fill(x, y + 10, x + 18, y + 11, ACCENT_AQUA);
    }

    private void body(GuiGraphicsExtractor g, int x, int y, String s, int color) {
        g.text(this.font, s, x, y, color, false);
    }

    private static void outlineRect(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    private static void roundedFill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0 + 1, y0, x1 - 1, y1, color);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    private static boolean inside(int mx, int my, int x0, int y0, int x1, int y1) {
        return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
        if (event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();
            for (Rect r : hotspots) {
                if (r.contains(mx, my)) {
                    handleAction(r.key);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClicked);
    }

    private void handleAction(String key) {
        if (key.startsWith("tab:")) {
            active = Tab.valueOf(key.substring(4));
            return;
        }
        if (key.startsWith("jobs:")) {
            handleJobsAction(key.substring(5));
            return;
        }
        if (key.startsWith("guild:")) {
            handleGuildAction(key.substring(6));
            return;
        }
        switch (key) {
            case "action:wallet"             -> active = Tab.WALLET;
            case "action:jobs"               -> active = Tab.JOBS;
            case "action:rewards"            -> active = Tab.REWARDS;
            case "action:money"              -> runCommand("money");
            case "action:open_config"        -> openClientConfigDir("otters_civ_revived");
            case "action:open_wallet"        -> openClientConfigFile("otters_civ_revived", "wallet.properties");
            case "action:open_rewards"       -> openClientConfigFile("otters_civ_revived", "rewards.json");
            case "action:open_block_values"  -> openClientConfigFile("otters_civ_revived", "block_values.json");
            case "action:open_entity_values" -> openClientConfigFile("otters_civ_revived", "entity_values.json");
            default -> { /* no-op */ }
        }
    }

    private void handleGuildAction(String op) {
        switch (op) {
            case "info"          -> runCommand("guild info");
            case "config"        -> openClientConfigFile("otters_civ_revived", "guilds.json");
            case "quick_create"  -> runCommand("guild create");
            case "invite"        -> runCommand("guild invite");
            case "claim"         -> runCommand("guild claim");
            case "unclaim"       -> runCommand("guild unclaim");
            case "map"           -> runCommand("guild map");
            case "sethome"       -> runCommand("guild sethome");
            case "home"          -> runCommand("guild home");
            case "promote"       -> runCommand("guild promote");
            case "demote"        -> runCommand("guild demote");
            case "kick"          -> runCommand("guild kick");
            case "leave"         -> runCommand("guild leave");
            default -> { /* no-op */ }
        }
    }

    private void handleJobsAction(String op) {
        var hud = JobsHudOverlay.config();
        if (op.startsWith("join:")) {
            runCommand("job join " + op.substring(5));
            return;
        }
        if (op.startsWith("leave:")) {
            runCommand("job leave " + op.substring(6));
            return;
        }
        if (op.startsWith("info:")) {
            runCommand("job info " + op.substring(5));
            return;
        }
        switch (op) {
            case "toggle" -> hud.setVisible(!hud.visible());
            case "reset"  -> hud.reset();
            case "x-" -> hud.nudgeOffsetX(-4);
            case "x+" -> hud.nudgeOffsetX(4);
            case "y-" -> hud.nudgeOffsetY(-4);
            case "y+" -> hud.nudgeOffsetY(4);
            case "s-" -> hud.nudgeScale(-0.1f);
            case "s+" -> hud.nudgeScale(0.1f);
            case "open_cfg" -> {
                if (canOpenServerJobsConfig()) {
                    openClientConfigFile("otters_civ_revived", "jobs.json");
                } else {
                    showClientNotice("jobs.json lives on server host. Use /job list or ask server owner.");
                }
            }
            case "cmd_stats" -> runCommand("job");
            case "cmd_list"  -> runCommand("job list");
            case "page_prev" -> jobsPage = Math.max(0, jobsPage - 1);
            case "page_next" -> jobsPage++;
            default -> { /* no-op */ }
        }
    }

    private void runCommand(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player != null && player.connection != null) {
            player.connection.sendCommand(cmd);
        }
    }

    private void runCommandAndClose(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player != null && player.connection != null) {
            player.connection.sendCommand(cmd);
        }
        this.onClose();
    }

    /** Opens the client's own config dir. On a host machine this is the server-side config too. */
    private static void openClientConfigDir(String sub) {
        try {
            Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(sub);
            dir.toFile().mkdirs();
            Util.getPlatform().openFile(dir.toFile());
        } catch (Exception e) {
            OogaMod.LOGGER.warn("[otters_civ_revived] open config dir failed", e);
        }
    }

    private static void openClientConfigFile(String sub, String fileName) {
        try {
            Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(sub);
            dir.toFile().mkdirs();
            File f = dir.resolve(fileName).toFile();
            Util.getPlatform().openFile(f.exists() ? f : dir.toFile());
        } catch (Exception e) {
            OogaMod.LOGGER.warn("[otters_civ_revived] open config file failed", e);
        }
    }

    private static boolean canOpenServerJobsConfig() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.hasSingleplayerServer();
    }

    private static void showClientNotice(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(message));
        }
    }

    private static String joinIds(List<String> ids) {
        return ids == null || ids.isEmpty() ? "(none)" : String.join(", ", ids);
    }

    private static int activeCount(JobStatusSnapshotData status) {
        return status == null || status.activeJobIds == null ? 0 : status.activeJobIds.size();
    }

    private static int maxActiveSlots(JobCatalogSnapshot catalog) {
        return catalog == null ? 1 : Math.max(1, catalog.maxActiveJobs);
    }

    private static boolean isActive(JobStatusSnapshotData status, String jobId) {
        return status != null && status.activeJobIds != null && status.activeJobIds.contains(jobId);
    }

    private static JobStatusSnapshotData.JobProgressEntry findProgress(JobStatusSnapshotData status, String jobId) {
        if (status == null || status.progress == null) {
            return null;
        }
        for (JobStatusSnapshotData.JobProgressEntry entry : status.progress) {
            if (entry != null && jobId.equals(entry.jobId)) {
                return entry;
            }
        }
        return null;
    }

    private static JobStatusSnapshotData.JobProgressEntry previewEntry(List<JobCatalogSnapshot.JobDescriptor> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return null;
        }
        JobCatalogSnapshot.JobDescriptor first = jobs.get(0);
        JobStatusSnapshotData.JobProgressEntry entry = new JobStatusSnapshotData.JobProgressEntry();
        entry.jobId = first.id;
        entry.displayName = first.displayName;
        entry.shortLabel = first.shortLabel;
        entry.iconGlyph = first.iconGlyph;
        entry.iconKey = first.iconKey;
        entry.level = 0;
        entry.xp = 0L;
        entry.xpForLevel = 0L;
        entry.xpForNextLevel = Math.max(1L, first.firstLevelXp);
        entry.maxLevel = Math.max(1, first.maxLevel);
        return entry;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Rect(int x0, int y0, int x1, int y1, String key) {
        boolean contains(int mx, int my) {
            return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
        }
    }
}
