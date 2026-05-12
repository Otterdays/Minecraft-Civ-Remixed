package com.fpsmod.client.ui;

import com.fpsmod.FpsMod;
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
        HOME("Home", "Quick actions"),
        WALLET("Wallet", "Balance & commands"),
        REWARDS("Rewards", "Mining & combat"),
        CIV("Civ", "Coming soon"),
        HELP("Help", "Commands & configs");

        final String label;
        final String subtitle;

        Tab(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
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

    private static final int PANEL_W = 460;
    private static final int PANEL_H = 248;
    private static final int SIDEBAR_W = 128;
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
        String left = "Mod id: fpsmod · /otter · /money";
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
            case CIV -> renderCiv(g, x, y, w, h);
            case HELP -> renderHelp(g, x, y, w, h);
        }
    }

    private void renderHome(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Welcome back");
        body(g, x, y + 14, "Project OOGA — an all-in-one civ suite.", TEXT_PRIMARY);
        body(g, x, y + 26, "Economy live. Factions & jobs incoming.", TEXT_PRIMARY);

        int bw = (w - 8) / 2;
        renderButton(g, x,           y + 50, bw, 22, "Open Wallet",    "action:wallet",      mouseX, mouseY);
        renderButton(g, x + bw + 8,  y + 50, bw, 22, "Rewards Info",   "action:rewards",     mouseX, mouseY);
        renderButton(g, x,           y + 78, bw, 22, "Configs Folder", "action:open_config", mouseX, mouseY);
        renderButton(g, x + bw + 8,  y + 78, bw, 22, "Run /money",     "action:money",       mouseX, mouseY);
    }

    private void renderWallet(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Wallet");
        body(g, x, y + 14, "Balance shown via /money in chat.", TEXT_PRIMARY);
        body(g, x, y + 26, "Server stores wallets at:", TEXT_PRIMARY);
        g.text(this.font, "config/otters_civ_revived/wallet.properties", x, y + 38, ACCENT_AQUA, false);

        renderButton(g, x,        y + 60, 130, 22, "Run /money",       "action:money",       mouseX, mouseY);
        renderButton(g, x + 138,  y + 60, 130, 22, "Open Wallet File", "action:open_wallet", mouseX, mouseY);

        body(g, x, y + 92,  "Operator: /money set <player> <amount> (gamemaster).", TEXT_MUTED);
        body(g, x, y + 104, "Hint lines (# Name:) refresh on join & rewards.",       TEXT_MUTED);
    }

    private void renderRewards(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Rewards");
        body(g, x, y + 14, "Mining: tag otters_civ_revived:currency_blocks", TEXT_PRIMARY);
        body(g, x, y + 26, "Combat: tag otters_civ_revived:currency_mobs",   TEXT_PRIMARY);
        body(g, x, y + 40, "Per-id overrides in block_values.json / entity_values.json.", TEXT_MUTED);
        body(g, x, y + 52, "Inline overrides in rewards.json (blockRewards/entityRewards).", TEXT_MUTED);

        renderButton(g, x,         y + 76,  140, 22, "Open rewards.json",  "action:open_rewards",       mouseX, mouseY);
        renderButton(g, x + 148,   y + 76,  140, 22, "Open block_values",  "action:open_block_values",  mouseX, mouseY);
        renderButton(g, x,         y + 102, 140, 22, "Open entity_values", "action:open_entity_values", mouseX, mouseY);
        renderButton(g, x + 148,   y + 102, 140, 22, "Configs Folder",     "action:open_config",        mouseX, mouseY);
    }

    private void renderCiv(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        sectionHeading(g, x, y, "Civilization");
        body(g, x, y + 14, "Factions, jobs, professions, player shops.", TEXT_PRIMARY);
        body(g, x, y + 28, "Roadmap-tracked. See DOCS/ROADMAP.md.",       TEXT_MUTED);

        int bx = x + (w - 110) / 2;
        int by = y + h / 2 - 8;
        g.fill(bx, by, bx + 110, by + 18, PANEL_BG_ALT);
        outlineRect(g, bx, by, bx + 110, by + 18, ACCENT_GOLD);
        String s = "COMING SOON";
        g.text(this.font, s, bx + (110 - this.font.width(s)) / 2, by + 5, ACCENT_GOLD, false);
    }

    private void renderHelp(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        sectionHeading(g, x, y, "Help & Commands");
        body(g, x, y + 14,  "/otter — opens this menu",                           TEXT_PRIMARY);
        body(g, x, y + 26,  "/money — show your balance",                         TEXT_PRIMARY);
        body(g, x, y + 38,  "/money set <player> <amount> — op only",             TEXT_PRIMARY);
        body(g, x, y + 56,  "Datapacks: data/otters_civ_revived/tags/block/",     TEXT_MUTED);
        body(g, x, y + 68,  "             data/otters_civ_revived/tags/entity_type/", TEXT_MUTED);
        body(g, x, y + 84,  "Precedence: sibling file > inline > tag flat reward.",  TEXT_MUTED);
        body(g, x, y + 96,  "Docs: README.md, index.html, DOCS/",                    TEXT_MUTED);
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
        switch (key) {
            case "action:wallet"             -> active = Tab.WALLET;
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
            FpsMod.LOGGER.warn("[otters_civ_revived] open config dir failed", e);
        }
    }

    private static void openClientConfigFile(String sub, String fileName) {
        try {
            Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(sub);
            dir.toFile().mkdirs();
            File f = dir.resolve(fileName).toFile();
            Util.getPlatform().openFile(f.exists() ? f : dir.toFile());
        } catch (Exception e) {
            FpsMod.LOGGER.warn("[otters_civ_revived] open config file failed", e);
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
