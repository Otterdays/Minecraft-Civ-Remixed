package com.fpsmod.client.ui;

import com.fpsmod.OogaMod;
import com.fpsmod.client.guilds.GuildClientState;
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
 * Four client-only color themes (Otter, Midnight, Sunset, Mint); click the footer
 * <q>Theme</q> chip to cycle — persisted in {@code config/project_ooga/otter_ui.properties}.
 * The Brief tab mirrors DOCS/whitepaper.md Phases 0–3 and DOCS/ROADMAP.md M0–M3;
 * the CIV tab lists milestone cards M0–M6.
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
        BRIEF("Brief", "WP Ph 0–3 · M0–M3"),
        CIV("Civ", "Roadmap M0–M6"),
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

    private static final int PANEL_W = 520;
    private static final int PANEL_H = 352;
    private static final int SIDEBAR_W = 128;
    private static final int TAB_H = 28;
    private static final int COMFORT_MIN_W = PANEL_W + 120;
    private static final int COMFORT_MIN_H = PANEL_H + 80;
    private static final long ANIM_MS = 180L;

    private final long openedAt = System.currentTimeMillis();
    private Tab active = Tab.HOME;
    private final List<Rect> hotspots = new ArrayList<>();
    private int jobsPage = 0;
    /** Brief tab: 0 = whitepaper-style spec, 1 = roadmap M0–M3 checklist. */
    private int briefPage;

    private OtterUiPalette pal() {
        return OtterUiThemeConfig.instance().palette();
    }

    public OttersCivScreen() {
        super(Component.literal("Otters Civ. Revived"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        float t = Mth.clamp((System.currentTimeMillis() - openedAt) / (float) ANIM_MS, 0f, 1f);
        float eased = easeOutCubic(t);
        int dimAlpha = (int) (0xCC * eased) & 0xFF;
        g.fill(0, 0, this.width, this.height, (dimAlpha << 24) | (pal().scrimRgb() & 0x00FFFFFF));
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
        roundedFill(g, px, py, px + PANEL_W, py + PANEL_H, pal().panelBg());
        // Top accent stripe.
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + 2, pal().accentPrimary());
        // Sidebar background.
        g.fill(px + 1, py + 2, px + SIDEBAR_W, py + PANEL_H - 1, pal().panelBgAlt());
        // Sidebar divider.
        g.fill(px + SIDEBAR_W, py + 2, px + SIDEBAR_W + 1, py + PANEL_H - 1, pal().panelBorder());
        // Outer border.
        outlineRect(g, px, py, px + PANEL_W, py + PANEL_H, pal().panelBorder());

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
        g.fill(px + SIDEBAR_W + 1, footerY, px + PANEL_W - 1, footerY + 1, pal().panelBorder());
        String left = "Mod id: project_ooga · " + buildCompactFooterLeft();
        String hint = "ESC to close";
        String themeBtn = "Theme: " + OtterUiThemeConfig.instance().theme().displayName() + " \u25B6";
        int themeW = Math.max(72, this.font.width(themeBtn) + 10);
        int hintW = this.font.width(hint);
        int rightPad = 6;
        int themeX = px + PANEL_W - rightPad - hintW - 8 - themeW;
        g.text(this.font, left, px + SIDEBAR_W + 10, footerY + 4, pal().textDim(), false);
        renderButton(g, themeX, footerY + 2, themeW, 11, themeBtn, "ui:theme_cycle", mouseX, mouseY);
        g.text(this.font, hint, px + PANEL_W - rightPad - hintW, footerY + 4, pal().textDim(), false);
    }

    private void drawTitle(GuiGraphicsExtractor g, int x, int y) {
        Font f = this.font;
        g.text(f, "OTTERS CIV.", x, y, pal().accentPrimary(), false);
        g.text(f, "REVIVED", x, y + 10, pal().accentSecondary(), false);
        g.fill(x + 62, y + 5, x + 66, y + 9, pal().accentPrimary());
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
        roundedFill(g, px, py, px + cardW, py + cardH, pal().panelBg());
        g.fill(px + 1, py + 1, px + cardW - 1, py + 2, pal().accentPrimary());
        outlineRect(g, px, py, px + cardW, py + cardH, pal().panelBorder());

        sectionHeading(g, px + 12, py + 12, "Window Too Small");
        body(g, px + 12, py + 28, "/otter needs more room to stay usable.", pal().textPrimary());
        body(g, px + 12, py + 42, "Current GUI: " + this.width + " x " + this.height, pal().textMuted());
        body(g, px + 12, py + 56, "Recommended: at least " + COMFORT_MIN_W + " x " + COMFORT_MIN_H, pal().textMuted());
        body(g, px + 12, py + 76, "Try one of these:", pal().textPrimary());

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
        String themeBtn = "Theme: " + OtterUiThemeConfig.instance().theme().displayName() + " \u25B6";
        int tw = Math.min(140, Math.max(88, this.font.width(themeBtn) + 8));
        renderButton(g, px + 12, py + cardH - 26, tw, 12, themeBtn, "ui:theme_cycle", mouseX, mouseY);
        g.text(this.font, hint, px + cardW - 12 - this.font.width(hint), py + cardH - 14, pal().textDim(), false);
    }

    private void renderTab(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, Tab tab, boolean selected, boolean hover) {
        int bg = selected ? pal().hoverBg() : (hover ? pal().tabHoverBg() : 0);
        if (bg != 0) {
            g.fill(x0, y0, x1, y1, bg);
        }
        if (selected) {
            g.fill(x0, y0, x0 + 2, y1, pal().accentPrimary());
        }
        int color = selected || hover ? pal().textPrimary() : pal().textMuted();
        int textX = x0 + 8;
        int maxW = (x1 - 2) - textX;
        g.text(this.font, fit(tab.label, maxW),    textX, y0 + 6,  color, false);
        g.text(this.font, fit(tab.subtitle, maxW), textX, y0 + 17, selected ? pal().accentSecondary() : pal().textDim(), false);
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
            case BRIEF -> renderBrief(g, x, y, w, h, mouseX, mouseY);
            case JOBS -> renderJobs(g, x, y, w, h, mouseX, mouseY);
            case GUILDS -> renderGuilds(g, x, y, w, h, mouseX, mouseY);
            case CIV -> renderCiv(g, x, y, w, h, mouseX, mouseY);
            case HELP -> renderHelp(g, x, y, w, h);
        }
    }

    private void renderHome(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        // Live status cards row
        var guildInfo = GuildClientState.guildInfo();
        var jobStatus = JobsClientState.primaryActiveJob();

        int cardW = (w - 6) / 3;
        int cardH = 44;

        // Wallet card
        g.fill(x, y, x + cardW, y + cardH, pal().panelBgAlt());
        outlineRect(g, x, y, x + cardW, y + cardH, pal().panelBorder());
        g.fill(x, y, x + 2, y + cardH, pal().accentPrimary());
        g.text(this.font, "WALLET", x + 6, y + 3, pal().accentPrimary(), false);
        g.text(this.font, fit("/money · /pay", cardW - 10), x + 6, y + 14, pal().textPrimary(), false);
        g.text(this.font, fit("Ledger on host (SQLite)", cardW - 10), x + 6, y + 26, pal().textMuted(), false);
        hotspots.add(new Rect(x, y, x + cardW, y + cardH, "action:wallet"));

        // Jobs card
        int cx2 = x + cardW + 3;
        g.fill(cx2, y, cx2 + cardW, y + cardH, pal().panelBgAlt());
        outlineRect(g, cx2, y, cx2 + cardW, y + cardH, pal().panelBorder());
        g.fill(cx2, y, cx2 + 2, y + cardH, pal().stripeJobs());
        g.text(this.font, "JOBS", cx2 + 6, y + 3, pal().stripeJobs(), false);
        if (jobStatus != null) {
            g.text(this.font, fit(jobStatus.shortLabel + " Lv" + jobStatus.level, cardW - 10), cx2 + 6, y + 14, pal().textPrimary(), false);
            g.text(this.font, fit("HUD + catalog in Jobs tab", cardW - 10), cx2 + 6, y + 26, pal().textMuted(), false);
        } else {
            g.text(this.font, fit("No active job", cardW - 10), cx2 + 6, y + 14, pal().textMuted(), false);
            g.text(this.font, fit("/job list · /job join", cardW - 10), cx2 + 6, y + 26, pal().textMuted(), false);
        }
        hotspots.add(new Rect(cx2, y, cx2 + cardW, y + cardH, "tab:JOBS"));

        // Guild card
        int cx3 = cx2 + cardW + 3;
        g.fill(cx3, y, cx3 + cardW, y + cardH, pal().panelBgAlt());
        outlineRect(g, cx3, y, cx3 + cardW, y + cardH, pal().panelBorder());
        g.fill(cx3, y, cx3 + 2, y + cardH, pal().stripeGuild());
        g.text(this.font, "GUILD", cx3 + 6, y + 3, pal().stripeGuild(), false);
        if (guildInfo != null) {
            g.text(this.font, fit(guildInfo.name + " · " + guildInfo.memberCount + " mbrs", cardW - 10), cx3 + 6, y + 14, pal().textPrimary(), false);
            g.text(this.font, fit(guildInfo.role + " · $" + guildInfo.balance + " treasury", cardW - 10), cx3 + 6, y + 26, pal().textMuted(), false);
        } else {
            g.text(this.font, fit("Not in a guild", cardW - 10), cx3 + 6, y + 14, pal().textMuted(), false);
            g.text(this.font, fit("/guild create · invites", cardW - 10), cx3 + 6, y + 26, pal().textMuted(), false);
        }
        hotspots.add(new Rect(cx3, y, cx3 + cardW, y + cardH, "tab:GUILDS"));

        // Quick action bar
        int btnY = y + cardH + 8;
        int gap = 6;
        int nBtns = 4;
        int totalGaps = gap * (nBtns - 1);
        int bw = (w - totalGaps) / nBtns;
        int bh = 20;
        renderButton(g, x,                        btnY, bw, bh, "/money",      "action:money",  mouseX, mouseY);
        renderButton(g, x + bw + gap,             btnY, bw, bh, "/guide",      "action:guide",  mouseX, mouseY);
        renderButton(g, x + (bw + gap) * 2,      btnY, bw, bh, "/job list",   "jobs:cmd_list", mouseX, mouseY);
        renderButton(g, x + (bw + gap) * 3,      btnY, bw, bh, "/guild info", "guild:info",    mouseX, mouseY);

        int envY = btnY + bh + 6;
        body(g, x, envY, fit(buildClientVersionLine(), w), pal().textDim());
        body(g, x, envY + 11, fit(buildSessionKindLine(), w), pal().textDim());
        body(g, x, envY + 22, fit(buildStandingChunkLine(), w), pal().textDim());

        // Milestone strip
        int stripY = envY + 36;
        int segW = (w - 6) / 7;
        Status[] miles = {
            Status.SHIPPED, Status.SHIPPED, Status.SHIPPED, Status.SHIPPED,
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
            g.text(this.font, labels[i], sx0 + (segW - 2 - tw) / 2, sy1 + 2, pal().textMuted(), false);
        }

        // Badge legend
        int legY = stripY + 14;
        drawBadge(g, x,      legY, "LIVE",    Status.SHIPPED.color);
        drawBadge(g, x + 36, legY, "PARTIAL", Status.PARTIAL.color);
        drawBadge(g, x + 84, legY, "SOON",    Status.PLANNED.color);
        drawBadge(g, x +124, legY, "FUTURE",  Status.FUTURE.color);

        body(g, x, legY + 16, "Cards jump to tabs. Brief = whitepaper + M0–M3 · CIV = full M0–M6 · HELP = commands.", pal().textDim());
        int nClaims = GuildClientState.claims().size();
        int nJobs = JobsClientCatalog.visibleJobs().size();
        body(g, x, legY + 28, fit("Client sync snapshot: " + nClaims + " claim chunks · " + nJobs + " visible jobs in catalog", w), pal().textDim());
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
        body(g, x, y + 14, "Host data: config/otters_civ_revived/project_ooga.db (SQLite).", pal().textMuted());

        // Command catalog with badges.
        int row = y + 28;
        drawCommandRow(g, x, row,      "/money",                          "show your balance",            Status.SHIPPED);
        drawCommandRow(g, x, row + 12, "/money set <player> <amount>",    "op-only set (GAMEMASTER tier)", Status.SHIPPED);
        drawCommandRow(g, x, row + 24, "/pay <player> <amount>",          "player transfer",               Status.SHIPPED);
        drawCommandRow(g, x, row + 36, "/economy reload",                 "re-read economy.json",          Status.SHIPPED);
        drawCommandRow(g, x, row + 48, "/economy log [count]",            "wallet audit view",             Status.SHIPPED);
        drawCommandRow(g, x, row + 60, "/economy log player <p> [n]",      "audit one player (gamemaster)", Status.SHIPPED);
        drawCommandRow(g, x, row + 72, "Caps · cooldowns · fees",         "live from economy.json",        Status.SHIPPED);

        // Actions.
        renderButton(g, x,        y + 122, 130, 22, "Run /money",       "action:money",       mouseX, mouseY);
        renderButton(g, x + 138,  y + 122, 130, 22,
            canOpenServerJobsConfig() ? "Open Host DB" : "Ask Host",
            "action:open_database",
            mouseX,
            mouseY
        );
    }

    private void drawCommandRow(GuiGraphicsExtractor g, int x, int y, String cmd, String note, Status status) {
        drawBadge(g, x, y, status.label, status.color);
        // Badge width up to ~38 px. Leave 44 px lane.
        int textX = x + 44;
        g.text(this.font, cmd, textX, y + 2, pal().textPrimary(), false);
        int cw = this.font.width(cmd);
        g.text(this.font, "·  " + note, textX + cw + 6, y + 2, pal().textDim(), false);
    }

    private void renderRewards(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Rewards  ·  M2 Engine");
        body(g, x, y + 14, "Block tag: otters_civ_revived:currency_blocks", pal().textPrimary());
        body(g, x, y + 26, "Mob tag:   otters_civ_revived:currency_mobs",   pal().textPrimary());
        body(g, x, y + 38, "After edits: /reload (or restart host). Precedence: sibling values > maps > tag.", pal().textDim());

        // Feature catalog with badges.
        int row = y + 52;
        drawCommandRow(g, x, row,      "Tag-driven payouts",              "core mining + combat",         Status.SHIPPED);
        drawCommandRow(g, x, row + 12, "Per-id overrides",                "block_values · entity_values", Status.SHIPPED);
        drawCommandRow(g, x, row + 24, "Cooldowns + dim-blacklist",       "in rewards.json",              Status.SHIPPED);
        drawCommandRow(g, x, row + 36, "Diminishing returns + anti-farm", "M2 acceptance gate",           Status.PLANNED);
        drawCommandRow(g, x, row + 48, "Farming + crafting reward loops", "5 balanced tracks target",     Status.PLANNED);

        renderButton(g, x,         y + 120, 132, 22, "Open rewards.json",  "action:open_rewards",       mouseX, mouseY);
        renderButton(g, x + 140,   y + 120, 132, 22, "Open block_values",  "action:open_block_values",  mouseX, mouseY);
        renderButton(g, x,         y + 146, 132, 22, "Open entity_values", "action:open_entity_values", mouseX, mouseY);
        renderButton(g, x + 140,   y + 146, 132, 22, "Configs Folder",     "action:open_config",        mouseX, mouseY);
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
        body(g, x, y + 14, activeLine, pal().textPrimary());
        body(g, x, y + 26, levelLine,  pal().textMuted());

        // HUD preview + config row.
        sectionHeading(g, x, y + 44, "HUD Bar");
        body(g, x, y + 58,
            primary == null
                ? "Preview below. Live bar appears above vanilla XP after /job join <id>."
                : "Preview mirrors the live bar drawn above vanilla XP when this menu is closed.",
            pal().textMuted());
        if (preview != null) {
            JobsHudOverlay.renderPreview(g, this.font, x, y + 72, Math.min(250, w - 4), hud.scale(), preview);
        }
        body(g, x, y + 102,
            "Visible: " + (hud.visible() ? "ON" : "OFF")
                + "   X: " + hud.offsetX()
                + "   Y: " + hud.offsetY()
                + "   Scale: " + String.format(java.util.Locale.ROOT, "%.2f", hud.scale()),
            pal().textPrimary());

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
        g.text(this.font, "X", rx, row2Y + 7, pal().textMuted(), false);
        renderButton(g, rx + 12,                 row2Y, nb, bh, "−",  "jobs:x-", mouseX, mouseY);
        renderButton(g, rx + 12 + nb + nbgap,    row2Y, nb, bh, "+",  "jobs:x+", mouseX, mouseY);

        rx = x + 80;
        g.text(this.font, "Y", rx, row2Y + 7, pal().textMuted(), false);
        renderButton(g, rx + 12,                 row2Y, nb, bh, "−",  "jobs:y-", mouseX, mouseY);
        renderButton(g, rx + 12 + nb + nbgap,    row2Y, nb, bh, "+",  "jobs:y+", mouseX, mouseY);

        rx = x + 160;
        g.text(this.font, "Scale", rx, row2Y + 7, pal().textMuted(), false);
        renderButton(g, rx + 32,                 row2Y, nb, bh, "−",  "jobs:s-", mouseX, mouseY);
        renderButton(g, rx + 32 + nb + nbgap,    row2Y, nb, bh, "+",  "jobs:s+", mouseX, mouseY);

        sectionHeading(g, x, row3Y, "Catalog");
        body(g, x, row3Y + 14,
            canOpenServerJobsConfig()
                ? "Jobs cfg opens host-side jobs.json on this machine."
                : "Remote server: jobs.json lives on server host. Local opener disabled.",
            pal().textMuted());
        int navY = row3Y + 28;
        renderButton(g, x, navY, 46, 18, "< Prev", "jobs:page_prev", mouseX, mouseY);
        renderButton(g, x + 52, navY, 46, 18, "Next >", "jobs:page_next", mouseX, mouseY);
        body(g, x + 106, navY + 5, "Page " + (jobsPage + 1) + "/" + (maxPage + 1)
            + "  ·  slots " + activeCount(status) + "/" + maxActiveSlots(catalog), pal().textPrimary());

        int listY = row3Y + 50;
        if (visibleJobs.isEmpty()) {
            body(g, x, listY, "No jobs synced yet. Try /job list after server join.", pal().textMuted());
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
            g.text(this.font, fit(title, 170), x, rowY, activeJob ? pal().accentPrimary() : pal().textPrimary(), false);
            g.text(this.font, fit(note, 170), x + 76, rowY, pal().textMuted(), false);
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
            body(g, x, y + 14, "You are not in a guild.", pal().textMuted());
            body(g, x, y + 28, "Create one with /guild create <name> ($250).", pal().textPrimary());
            body(g, x, y + 40, "Or accept an invite with /guild join.", pal().textPrimary());
            renderButton(g, x, y + 58, w - 2, 20, "Create guild (costs $250)", "guild:quick_create", mouseX, mouseY);
            return;
        }

        int yy = y;
        sectionHeading(g, x, yy, info.name);
        yy += 14;

        // Status chips
        drawBadge(g, x, yy, info.open ? "OPEN" : "INVITE", info.open ? pal().stripeJobs() : pal().accentPrimary());
        drawBadge(g, x + 56, yy, info.role.toUpperCase(), pal().stripeGuild());
        yy += 12;

        // Stat line
        body(g, x, yy, info.memberCount + "/" + info.maxMembers + " members  ·  "
            + info.claimCount + "/" + info.maxClaims + " claims  ·  $" + info.balance
            + "  ·  Home " + (info.hasHome ? "set" : "not set"), pal().textPrimary());
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
        g.fill(x, bottomY, x + w, bottomY + 1, pal().panelBorder());
        body(g, x, bottomY + 4, "Map: surface tint, own vs other claim tint, facing wedge; walking into a claim shows a hotbar hint.", pal().textDim());
        renderButton(g, x + w - 76, bottomY + 2, 76, 12, "Open config", "guild:config", mouseX, mouseY);
    }

    private void renderCiv(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Roadmap  ·  DOCS/ROADMAP.md");
        body(g, x, y + 14, fit("Milestone order: M0–M3 shipped in-repo; M4–M6 + M4.5 ahead (see Brief tab for whitepaper Ph 0–3).", w), pal().textDim());
        int yy = y + 28;

        String[][] milestones = {
            {"M0  Foundation", "SHIPPED", "SQLite WAL; schema_version + migrations; wallet_ledger; guilds/claims/jobs_state; AtomicFileWriter; /ooga db"},
            {"M1  Economy MVP", "SHIPPED", "/money /pay; economy.json caps·fee·cooldown; /economy log + per-player filter; join starting balance"},
            {"M2  Jobs MVP", "SHIPPED", "jobs.json triggers; /job validate; 5-role starter; HUD + /otter Jobs; payouts via wallet service"},
            {"M3  Guilds & Claims", "SHIPPED", "Invites/ranks; chunk claim $; protections; /guild map ASCII + overlay + particles + walk-in notifier"},
            {"M4  Player Shops", "PLANNED", "Market UI, escrow, listing caps, tax"},
            {"M4.5 Social", "PLANNED", "Friends + PMs (roadmap)"},
            {"M5  Governance", "FUTURE", "Diplomacy, territory projects, regional bonuses"},
            {"M6  Scale", "FUTURE", "PostgreSQL dialect; soak; RC — whitepaper scale path"}
        };
        for (String[] m : milestones) {
            Status s = switch (m[1]) {
                case "SHIPPED" -> Status.SHIPPED;
                case "PLANNED" -> Status.PLANNED;
                default -> Status.FUTURE;
            };
            g.fill(x, yy, x + w, yy + 22, pal().panelBgAlt());
            outlineRect(g, x, yy, x + w, yy + 22, pal().panelBorder());
            g.fill(x, yy, x + 2, yy + 22, s.color);
            g.text(this.font, m[0], x + 6, yy + 2, pal().textPrimary(), false);
            drawBadge(g, x + 6 + this.font.width(m[0]) + 4, yy + 2, s.label, s.color);
            g.text(this.font, fit(m[2], w - 16), x + 6, yy + 12, pal().textMuted(), false);
            yy += 24;
        }

        int btnY = Math.min(yy + 4, y + h - 40);
        renderButton(g, x, btnY, (w - 3) / 2, 18, "Brief (spec)", "tab:BRIEF", mouseX, mouseY);
        renderButton(g, x + (w + 3) / 2, btnY, (w - 3) / 2, 18, "Guilds panel", "tab:GUILDS", mouseX, mouseY);
        renderButton(g, x, btnY + 22, w - 2, 14, "Open guilds.json", "guild:config", mouseX, mouseY);
    }

    private void renderBrief(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Project OOGA  ·  Phases 0–3");
        body(g, x, y + 14, fit("Condensed from DOCS/whitepaper.md + DOCS/ROADMAP.md — not a legal contract; shipped behavior wins.", w), pal().textDim());

        int tw = (w - 10) / 2;
        boolean p0 = briefPage == 0;
        renderButton(g, x, y + 26, tw, 14, p0 ? "· Spec ·" : "1  Whitepaper", "brief:p0", mouseX, mouseY);
        renderButton(g, x + tw + 10, y + 26, tw, 14, !p0 ? "· M0–M3 ·" : "2  Roadmap", "brief:p1", mouseX, mouseY);

        int ty = y + 44;
        int lh = 10;
        int mid = x + tw + 10;
        if (p0) {
            sectionHeading(g, x, ty, "Vision & pillars");
            int u = ty + 14;
            body(g, x, u, fit("One civ stack: economy, jobs, land, future shops — native Fabric, less plugin friction.", tw), pal().textPrimary()); u += lh;
            body(g, x, u, fit("Pillars: wallet + audit; professions; claims; commerce later; fail-fast integrity.", tw), pal().textMuted()); u += lh;
            body(g, x, u, fit("Architecture: typed services → persistence; events validated before state writes.", tw), pal().textMuted()); u += lh;
            body(g, x, u, fit("Anti-exploit: server-authoritative money; no trusting client balance.", tw), pal().textMuted()); u += lh + 4;

            sectionHeading(g, mid, ty, "Data strategy (WP)");
            int v = ty + 14;
            body(g, mid, v, fit("Default: SQLite embedded, WAL, busy_timeout, forward-only migrations.", tw), pal().textPrimary()); v += lh;
            body(g, mid, v, fit("Authoritative wallets/ledger/claims/jobs live in project_ooga.db — not flat JSON.", tw), pal().textMuted()); v += lh;
            body(g, mid, v, fit("Scale path (M6): same schema → Postgres / JDBC dialect — whitepaper § Phase 6.", tw), pal().textMuted()); v += lh;
            body(g, mid, v, fit("Avoid: NoSQL/doc primary; custom binary; JSON wallets (corruption + no ACID).", tw), pal().textMuted()); v += lh + 4;

            sectionHeading(g, x, u, "Delivery Phases 0–3 (WP)");
            u += 14;
            body(g, x, u, fit("P0 ✓ Relational schema + schema_version + SQLite + wallet_ledger + /ooga db status|migrate.", w), pal().textPrimary()); u += lh;
            body(g, x, u, fit("P1 ✓ Wallet cmds, transfers, reasons, admin ops, transaction visibility.", w), pal().textMuted()); u += lh;
            body(g, x, u, fit("P2 ✓ Jobs/professions, reward hooks, progression + HUD (diminishing returns = later gate).", w), pal().textMuted()); u += lh;
            body(g, x, u, fit("P3 ✓ Factions model = guilds; chunk claims; role checks; treasury field; map UX.", w), pal().textMuted()); u += lh;
            body(g, x, u, fit("Progression loop: Nomad → Specialist → Settler → Founder → Governor → Civilizer (WP).", w), pal().textMuted()); u += lh;
            body(g, x, u, fit("Non-goals (current): cross-server economy sync; MMO quest trees — see whitepaper.", w), pal().textDim());
        } else {
            sectionHeading(g, x, ty, "M0 Foundation");
            int u = ty + 14;
            body(g, x, u, fit("Interfaces IEconomy/IJobs/IGuild; PersistenceService; SchemaMigrator; SqlitePersistenceIntegrationTest.", tw), pal().textPrimary()); u += lh;
            body(g, x, u, fit("Risks ✓ WAL + busy_timeout; migration drift mitigated by tests + rollback notes.", tw), pal().textMuted()); u += lh + 4;

            sectionHeading(g, mid, ty, "M1 Economy");
            int v = ty + 14;
            body(g, mid, v, fit("Shipped: /pay atomic; economy.json policy; /economy log player; wallet_ledger reasons.", tw), pal().textPrimary()); v += lh;
            body(g, mid, v, fit("Open: inflation/sink tuning; race hardening under command spam (roadmap risks).", tw), Status.PARTIAL.color); v += lh + 4;

            sectionHeading(g, x, u, "M2 Jobs");
            u += 14;
            body(g, x, u, fit("Shipped: /job join|leave|info|list|stats|reload|validate; event rewards; starter 5-pack; HUD.", tw), pal().textPrimary()); u += lh;
            body(g, x, u, fit("Open: diminishing returns; TPS throttling for reward storms (roadmap).", tw), Status.PARTIAL.color); u += lh + 4;

            sectionHeading(g, mid, v, "M3 Guilds");
            v += 14;
            body(g, mid, v, fit("Shipped: claims; protections break/place/container; home TP; map+particles+client notifier.", tw), pal().textPrimary()); v += lh;
            body(g, mid, v, fit("Open: treasury audit depth; false-positive denial tooling (roadmap).", tw), Status.PARTIAL.color); v += lh;

            int foot = Math.max(u, v) + 6;
            body(g, x, foot, fit("Next milestones (panel): M4 shops · M4.5 social · M5 gov · M6 Postgres scale — CIV tab.", w), pal().textDim());
            renderButton(g, x, foot + 12, w - 2, 16, "Open full roadmap tab", "tab:CIV", mouseX, mouseY);
        }
    }

    private void drawMilestone(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, String detail, Status status) {
        g.fill(x, y, x + w, y + h, pal().panelBgAlt());
        outlineRect(g, x, y, x + w, y + h, pal().panelBorder());
        // Left accent stripe in status color.
        g.fill(x, y, x + 2, y + h, status.color);
        g.text(this.font, title, x + 8, y + 5, pal().textPrimary(), false);
        int titleW = this.font.width(title);
        drawBadge(g, x + 8 + titleW + 8, y + 5, status.label, status.color);
        g.text(this.font, fit(detail, w - 16), x + 8, y + 20, pal().textMuted(), false);
    }

    private void renderHelp(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        sectionHeading(g, x, y, "All Commands");

        int row = y + 16;
        int s = 10;
        int r = row;
        drawCommandRow(g, x, r, "/otter", "chat list · mod client = hub + Theme", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/guide · /guide give", "handbook · admin give", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/money · /pay", "balance + player transfers", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/money set|add|take …", "op-tier admin edits", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/economy reload|log …", "op ledger (SQLite)", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/economy log player …", "filter audit to one player", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/job …", "list · join · leave · info · validate", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/job reload", "op refresh jobs.json", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/guild invite|join|leave|kick", "membership · join <name> if public", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/guild transfer · unclaimall", "owner lead move · wipe claims", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/guild claim|unclaim|map", "claims; map = tint + facing + walk hint", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/guild promote|demote|home|sethome", "ranks · TP anchors", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/guild open|close · info|list", "public gate · lookups", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/guild reload", "op re-read guilds.json", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "/ooga db …", "SQLite status / migrate", Status.SHIPPED);
        r += s;
        drawCommandRow(g, x, r, "Market UI · /shop", "M4 player shops", Status.PLANNED);

        body(g, x, r + s + 4, "Docs: README.md · index.html · DOCS/ROADMAP.md · DOCS/whitepaper.md", pal().textDim());
        body(g, x, r + s + 16, fit("Join chat: showJoinWelcome + starting balance in economy.json", w), pal().textDim());
        body(g, x, r + s + 28, fit("Brief tab (sidebar): condensed whitepaper Ph 0–3 + roadmap M0–M3 checklist.", w), pal().textDim());
    }

    private void renderButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, String actionKey, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, x + w, y + h);
        int bg = hover ? pal().btnBgHover() : pal().btnBg();
        g.fill(x, y, x + w, y + h, bg);
        outlineRect(g, x, y, x + w, y + h, hover ? pal().accentSecondary() : pal().panelBorder());
        int tw = this.font.width(label);
        g.text(this.font, label, x + (w - tw) / 2, y + (h - 8) / 2, hover ? pal().accentPrimary() : pal().textPrimary(), false);
        hotspots.add(new Rect(x, y, x + w, y + h, actionKey));
    }

    private void sectionHeading(GuiGraphicsExtractor g, int x, int y, String title) {
        g.text(this.font, title, x, y, pal().accentPrimary(), false);
        g.fill(x, y + 10, x + 18, y + 11, pal().accentSecondary());
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
        if (key.startsWith("brief:")) {
            switch (key.substring(6)) {
                case "p0" -> briefPage = 0;
                case "p1" -> briefPage = 1;
                default -> { /* no-op */ }
            }
            return;
        }
        switch (key) {
            case "action:wallet"             -> active = Tab.WALLET;
            case "action:jobs"               -> active = Tab.JOBS;
            case "action:rewards"            -> active = Tab.REWARDS;
            case "action:money"              -> runCommand("money");
            case "action:guide"              -> runCommand("guide");
            case "action:open_config"        -> openClientConfigDir("otters_civ_revived");
            case "action:open_database"      -> {
                if (canOpenServerJobsConfig()) {
                    openClientConfigFile("otters_civ_revived", "project_ooga.db");
                } else {
                    showClientNotice("project_ooga.db lives on the server host. Ask the host if you need it.");
                }
            }
            case "action:open_rewards"       -> openClientConfigFile("otters_civ_revived", "rewards.json");
            case "action:open_block_values"  -> openClientConfigFile("otters_civ_revived", "block_values.json");
            case "action:open_entity_values" -> openClientConfigFile("otters_civ_revived", "entity_values.json");
            case "ui:theme_cycle"             -> OtterUiThemeConfig.instance().cycleTheme();
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

    private static String buildCompactFooterLeft() {
        var L = net.fabricmc.loader.api.FabricLoader.getInstance();
        String mc = L.getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        String ver = L.getModContainer(OogaMod.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        return "MC " + mc + " · v" + ver + " · /guide";
    }

    private static String buildClientVersionLine() {
        var L = net.fabricmc.loader.api.FabricLoader.getInstance();
        String mc = L.getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        String ver = L.getModContainer(OogaMod.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        return "Minecraft " + mc + " · Otters Civ. " + ver + " (" + OogaMod.MOD_ID + ")";
    }

    private static String buildSessionKindLine() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            return "Session: local world — JSON + SQLite under your .minecraft/config";
        }
        if (mc.player != null && mc.player.connection != null) {
            return "Session: multiplayer — civ data runs on the server host";
        }
        return "Session: title / loading";
    }

    private static String buildStandingChunkLine() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return "Chunk: join a world to see claim status here.";
        }
        var p = mc.player;
        String dim = mc.level.dimension().identifier().toString();
        int cx = p.blockPosition().getX() >> 4;
        int cz = p.blockPosition().getZ() >> 4;
        var claim = GuildClientState.claimAt(dim, cx, cz);
        String base = "Standing: [" + cx + ", " + cz + "] in " + abbrev(dim, 44);
        if (claim == null) {
            return base + " — unclaimed";
        }
        String gName = claim.guildDisplayName();
        if (gName == null || gName.isEmpty()) {
            gName = "guild";
        }
        gName = abbrev(gName, 26);
        var gi = GuildClientState.guildInfo();
        boolean own = gi != null && gi.guildId != null && gi.guildId.equals(claim.guildId().toString());
        return base + " — " + (own ? "your guild (" : "claimed: ") + gName + (own ? ")" : "");
    }

    private static String abbrev(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max - 1) + "\u2026";
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
