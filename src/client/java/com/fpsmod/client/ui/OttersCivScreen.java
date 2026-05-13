package com.fpsmod.client.ui;

import com.fpsmod.OogaMod;
import com.fpsmod.client.jobs.JobsHudOverlay;
import com.fpsmod.jobs.net.JobStatusPayload;
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
        REWARDS("Rewards", "Payouts & tuning"),
        CIV("Civ", "Factions · shops · gov"),
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

    private static final int PANEL_W = 480;
    private static final int PANEL_H = 268;
    private static final int SIDEBAR_W = 132;
    private static final int TAB_H = 28;
    private static final long ANIM_MS = 180L;

    private final long openedAt = System.currentTimeMillis();
    private Tab active = Tab.HOME;
    private final List<Rect> hotspots = new ArrayList<>();
    private int lastMouseX;
    private int lastMouseY;

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

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        float t = Mth.clamp((System.currentTimeMillis() - openedAt) / (float) ANIM_MS, 0f, 1f);
        float eased = easeOutCubic(t);
        int yOffset = (int) ((1f - eased) * 14f);

        int px = (this.width - PANEL_W) / 2;
        int py = (this.height - PANEL_H) / 2 + yOffset;

        hotspots.clear();

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
            case CIV -> renderCiv(g, x, y, w, h);
            case HELP -> renderHelp(g, x, y, w, h);
        }
    }

    private void renderHome(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Welcome back");
        body(g, x, y + 14, "Project OOGA — all-in-one civ suite for Fabric.", TEXT_PRIMARY);
        body(g, x, y + 26, "Economy & jobs live. Factions, shops, governance incoming.", TEXT_MUTED);

        // Milestone progress strip — 7 segments M0–M6.
        int stripY = y + 44;
        int segW = (w - 6) / 7;
        Status[] miles = {
            Status.PARTIAL, // M0 Foundation — partial (wallet/rewards persist; no audit log/migrations yet)
            Status.PARTIAL, // M1 Economy MVP — /money + /money set live; /pay, /ooga still planned
            Status.SHIPPED, // M2 Jobs MVP — 4 jobs, XP, HUD bar shipped
            Status.PLANNED, // M3 Factions & Claims
            Status.PLANNED, // M4 Player Shops
            Status.FUTURE,  // M5 Governance
            Status.FUTURE   // M6 Scale & PostgreSQL
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
        // Legend.
        int legY = stripY + 22;
        drawBadge(g, x,      legY, "LIVE",    Status.SHIPPED.color);
        drawBadge(g, x + 36, legY, "PARTIAL", Status.PARTIAL.color);
        drawBadge(g, x + 84, legY, "SOON",    Status.PLANNED.color);
        drawBadge(g, x +124, legY, "FUTURE",  Status.FUTURE.color);

        // Quick actions.
        int qaY = y + 86;
        int bw = (w - 8) / 2;
        renderButton(g, x,           qaY,      bw, 22, "Open Wallet",     "action:wallet",      mouseX, mouseY);
        renderButton(g, x + bw + 8,  qaY,      bw, 22, "Jobs & HUD",      "action:jobs",        mouseX, mouseY);
        renderButton(g, x,           qaY + 28, bw, 22, "Reward Configs",  "action:rewards",     mouseX, mouseY);
        renderButton(g, x + bw + 8,  qaY + 28, bw, 22, "Run /money",      "action:money",       mouseX, mouseY);
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
        var p = com.fpsmod.client.jobs.JobsClientState.latest();
        var hud = JobsHudOverlay.config();
        JobStatusPayload preview = (p != null && !p.slug().isEmpty())
            ? p
            : new JobStatusPayload("lumberjack", 0, 0L, 0L, 100L);

        String activeLine;
        String levelLine;
        if (p == null || p.slug().isEmpty()) {
            activeLine = "Active: (none)";
            levelLine = "Pick one with /job join <miner|lumberjack|farmer|fighter>";
        } else {
            activeLine = "Active: " + p.slug().toUpperCase(java.util.Locale.ROOT) + "  ·  Lvl " + p.level();
            long inLevel = Math.max(0L, p.xp() - p.xpForLevel());
            long range = Math.max(1L, p.xpForNextLevel() - p.xpForLevel());
            levelLine = "XP: " + inLevel + " / " + range + (p.level() >= 50 ? "  (MAX)" : "");
        }
        body(g, x, y + 14, activeLine, TEXT_PRIMARY);
        body(g, x, y + 26, levelLine,  TEXT_MUTED);

        // HUD preview + config row.
        sectionHeading(g, x, y + 44, "HUD Bar");
        body(g, x, y + 58,
            p == null || p.slug().isEmpty()
                ? "Preview below. Live bar appears above vanilla XP after /job join <slug>."
                : "Preview mirrors the live bar drawn above vanilla XP when this menu is closed.",
            TEXT_MUTED);
        JobsHudOverlay.renderPreview(g, this.font, x, y + 72, Math.min(250, w - 4), hud.scale(), preview);
        body(g, x, y + 102,
            "Visible: " + (hud.visible() ? "ON" : "OFF")
                + "   X: " + hud.offsetX()
                + "   Y: " + hud.offsetY()
                + "   Scale: " + String.format(java.util.Locale.ROOT, "%.2f", hud.scale()),
            TEXT_PRIMARY);

        int row1Y = y + 118;
        int row2Y = y + 144;
        int row3Y = y + 170;
        int bw = 60;
        int bh = 22;
        int gap = 6;

        // Row 1: toggle + reset + slash command shortcuts.
        renderButton(g, x,                        row1Y, bw, bh, hud.visible() ? "Hide" : "Show", "jobs:toggle", mouseX, mouseY);
        renderButton(g, x + (bw + gap),           row1Y, bw, bh, "Reset",                          "jobs:reset",  mouseX, mouseY);
        renderButton(g, x + (bw + gap) * 2,       row1Y, bw, bh, "/job",                           "jobs:cmd_stats", mouseX, mouseY);
        renderButton(g, x + (bw + gap) * 3,       row1Y, bw, bh, "/job list",                      "jobs:cmd_list",  mouseX, mouseY);

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

        // Direct join shortcuts reduce the "controls but no HUD" confusion for first-time users.
        int joinW = 74;
        int joinGap = 4;
        renderButton(g, x,                         row3Y, joinW, bh, "Miner",      "jobs:join_miner",      mouseX, mouseY);
        renderButton(g, x + (joinW + joinGap),    row3Y, joinW, bh, "Lumberjack", "jobs:join_lumberjack", mouseX, mouseY);
        renderButton(g, x + (joinW + joinGap) * 2, row3Y, joinW, bh, "Farmer",     "jobs:join_farmer",     mouseX, mouseY);
        renderButton(g, x + (joinW + joinGap) * 3, row3Y, joinW, bh, "Fighter",    "jobs:join_fighter",    mouseX, mouseY);
    }

    private void renderCiv(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        sectionHeading(g, x, y, "Civilization Roadmap");
        body(g, x, y + 14, "M3 → M6 — full execution order in DOCS/ROADMAP.md.", TEXT_MUTED);

        int cardY = y + 28;
        int cardH = 38;
        int cardGap = 4;

        drawMilestone(g, x, cardY, w, cardH,
            "M3  Factions & Claims",
            "/f create|invite|join|leave|promote|demote · chunk claims · faction treasury",
            Status.PLANNED);
        cardY += cardH + cardGap;

        drawMilestone(g, x, cardY, w, cardH,
            "M4  Player Shops",
            "Screen Handler market UI · escrow purchase · listing caps · tax sinks",
            Status.PLANNED);
        cardY += cardH + cardGap;

        drawMilestone(g, x, cardY, w, cardH,
            "M5  Governance",
            "Diplomacy (ally/neutral/rival/war) · territory projects · regional bonuses",
            Status.FUTURE);
        cardY += cardH + cardGap;

        drawMilestone(g, x, cardY, w, cardH,
            "M6  Stabilization & Scale",
            "Telemetry · adversarial soak · SQLite → PostgreSQL migration · RC",
            Status.FUTURE);
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

        // Planned commands.
        drawCommandRow(g, x, row + 76, "/pay <player> <amount>",          "M1 transfer",                     Status.PLANNED);
        drawCommandRow(g, x, row + 88, "/ooga money add|take",            "M1 admin grant/burn",             Status.PLANNED);
        drawCommandRow(g, x, row +100, "/profession info",                "M2 progression view",             Status.PLANNED);
        drawCommandRow(g, x, row +112, "/f create|invite|join|...",       "M3 factions",                     Status.PLANNED);
        drawCommandRow(g, x, row +124, "Market UI · /shop",               "M4 player shops",                 Status.PLANNED);

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

    private void handleJobsAction(String op) {
        var hud = JobsHudOverlay.config();
        switch (op) {
            case "toggle" -> hud.setVisible(!hud.visible());
            case "reset"  -> hud.reset();
            case "x-" -> hud.nudgeOffsetX(-4);
            case "x+" -> hud.nudgeOffsetX(4);
            case "y-" -> hud.nudgeOffsetY(-4);
            case "y+" -> hud.nudgeOffsetY(4);
            case "s-" -> hud.nudgeScale(-0.1f);
            case "s+" -> hud.nudgeScale(0.1f);
            case "cmd_stats" -> runCommand("job");
            case "cmd_list"  -> runCommand("job list");
            case "join_miner"      -> runCommand("job join miner");
            case "join_lumberjack" -> runCommand("job join lumberjack");
            case "join_farmer"     -> runCommand("job join farmer");
            case "join_fighter"    -> runCommand("job join fighter");
            default -> { /* no-op */ }
        }
    }

    private void runCommand(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand(cmd);
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
