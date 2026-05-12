package com.fpsmod.client.ui;

import com.fpsmod.FpsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Otters Civ. Revived — stylized in-game hub. Opens from client-side {@code /otter}.
 * Pure-geometry rendering (no asset textures) so it stays asset-pipeline-free for v1.
 */
public final class OttersCivScreen extends Screen {

    private enum Tab {
        HOME("Home", "Mod overview & quick actions"),
        WALLET("Wallet", "Your balance & quick commands"),
        REWARDS("Rewards", "Mining & combat payouts"),
        CIV("Civ", "Factions & professions — coming soon"),
        HELP("Help", "Commands, configs, datapacks");

        final String label;
        final String subtitle;

        Tab(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    // Palette — modern dark-navy w/ gold + aqua accents.
    private static final int BG_DIM         = 0xCC0A0E1A;
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

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 240;
    private static final int SIDEBAR_W = 110;
    private static final int TAB_H = 28;
    private static final long ANIM_MS = 180L;

    private final long openedAt = System.currentTimeMillis();
    private Tab active = Tab.HOME;
    private final List<Rect> hotspots = new ArrayList<>();

    public OttersCivScreen() {
        super(Component.literal("Otters Civ. Revived"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Animated dim backdrop.
        float t = Mth.clamp((System.currentTimeMillis() - openedAt) / (float) ANIM_MS, 0f, 1f);
        float eased = easeOutCubic(t);
        int dimAlpha = (int) (0xCC * eased) & 0xFF;
        g.fill(0, 0, this.width, this.height, (dimAlpha << 24) | 0x0A0E1A);

        super.render(g, mouseX, mouseY, partialTick);

        int px = (this.width - PANEL_W) / 2;
        int py = (this.height - PANEL_H) / 2;
        int yOffset = (int) ((1f - eased) * 14f);
        py += yOffset;

        hotspots.clear();

        // Outer drop shadow approximation.
        g.fill(px + 3, py + 6, px + PANEL_W + 3, py + PANEL_H + 6, 0x66000000);
        // Panel body.
        roundedFill(g, px, py, px + PANEL_W, py + PANEL_H, PANEL_BG);
        // Top accent stripe.
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + 2, ACCENT_GOLD);
        // Sidebar background.
        g.fill(px + 1, py + 2, px + SIDEBAR_W, py + PANEL_H - 1, PANEL_BG_ALT);
        // Sidebar / body divider.
        g.fill(px + SIDEBAR_W, py + 2, px + SIDEBAR_W + 1, py + PANEL_H - 1, PANEL_BORDER);
        // Outer border (subtle).
        outline(g, px, py, px + PANEL_W, py + PANEL_H, PANEL_BORDER);

        // Title.
        drawTitle(g, px + 8, py + 8);

        // Sidebar tabs.
        int tabsY = py + 38;
        for (Tab tab : Tab.values()) {
            int x0 = px + 4;
            int x1 = px + SIDEBAR_W - 4;
            int y0 = tabsY;
            int y1 = tabsY + TAB_H;
            boolean hover = mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
            boolean selected = tab == active;
            renderTab(g, x0, y0, x1, y1, tab, selected, hover);
            hotspots.add(new Rect(x0, y0, x1, y1, "tab:" + tab.name()));
            tabsY += TAB_H + 2;
        }

        // Content area.
        int cx = px + SIDEBAR_W + 14;
        int cy = py + 12;
        int cw = PANEL_W - SIDEBAR_W - 22;
        int ch = PANEL_H - 24;
        renderContent(g, cx, cy, cw, ch, mouseX, mouseY);

        // Footer hint.
        String hint = "ESC to close · click tab to switch";
        g.drawString(this.font, hint, px + PANEL_W - 4 - this.font.width(hint), py + PANEL_H - 11, TEXT_DIM, false);
    }

    private void drawTitle(GuiGraphics g, int x, int y) {
        Font f = this.font;
        // Stacked title — caps, accent dot, subtitle.
        g.drawString(f, "OTTERS CIV.", x, y, ACCENT_GOLD, false);
        g.drawString(f, "REVIVED", x, y + 10, ACCENT_AQUA, false);
        g.fill(x + 62, y + 5, x + 66, y + 9, ACCENT_GOLD); // accent dot
    }

    private void renderTab(GuiGraphics g, int x0, int y0, int x1, int y1, Tab tab, boolean selected, boolean hover) {
        int bg = selected ? HOVER_BG : (hover ? 0xFF182334 : 0x00000000);
        if (bg != 0) {
            g.fill(x0, y0, x1, y1, bg);
        }
        if (selected) {
            // Left accent bar.
            g.fill(x0, y0, x0 + 2, y1, ACCENT_GOLD);
        }
        int color = selected ? TEXT_PRIMARY : (hover ? TEXT_PRIMARY : TEXT_MUTED);
        g.drawString(this.font, tab.label, x0 + 8, y0 + 6, color, false);
        g.drawString(this.font, tab.subtitle, x0 + 8, y0 + 17, selected ? ACCENT_AQUA : TEXT_DIM, false);
    }

    private void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        switch (active) {
            case HOME -> renderHome(g, x, y, w, h, mouseX, mouseY);
            case WALLET -> renderWallet(g, x, y, w, h, mouseX, mouseY);
            case REWARDS -> renderRewards(g, x, y, w, h, mouseX, mouseY);
            case CIV -> renderCiv(g, x, y, w, h);
            case HELP -> renderHelp(g, x, y, w, h);
        }
    }

    private void renderHome(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Welcome back");
        bodyLine(g, x, y + 14, "Project OOGA — an all-in-one civ suite.");
        bodyLine(g, x, y + 26, "Economy live. Factions & jobs incoming.");

        int bx = x;
        int by = y + 50;
        int bw = (w - 8) / 2;
        renderButton(g, bx,            by, bw, 22, "Open Wallet", "action:wallet", mouseX, mouseY);
        renderButton(g, bx + bw + 8,   by, bw, 22, "Rewards Info", "action:rewards", mouseX, mouseY);
        renderButton(g, bx,            by + 28, bw, 22, "Configs Folder", "action:open_config", mouseX, mouseY);
        renderButton(g, bx + bw + 8,   by + 28, bw, 22, "Run /money",     "action:money", mouseX, mouseY);

        g.drawString(this.font, "Mod id: fpsmod  ·  /otter  ·  /money", x, y + h - 11, TEXT_DIM, false);
    }

    private void renderWallet(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Wallet");
        bodyLine(g, x, y + 14, "Balance shown via /money in chat.");
        bodyLine(g, x, y + 26, "Server stores wallets at:");
        g.drawString(this.font, "config/otters_civ_revived/wallet.properties", x, y + 38, ACCENT_AQUA, false);

        renderButton(g, x,        y + 60, 130, 22, "Run /money",       "action:money",       mouseX, mouseY);
        renderButton(g, x + 138,  y + 60, 130, 22, "Open Wallet File", "action:open_wallet", mouseX, mouseY);

        bodyLineMuted(g, x, y + 92, "Operator: /money set <player> <amount> (gamemaster).");
        bodyLineMuted(g, x, y + 104, "Hint lines (# Name:) refresh on join & rewards.");
    }

    private void renderRewards(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        sectionHeading(g, x, y, "Rewards");
        bodyLine(g, x, y + 14, "Mining: tag otters_civ_revived:currency_blocks");
        bodyLine(g, x, y + 26, "Combat: tag otters_civ_revived:currency_mobs");
        bodyLineMuted(g, x, y + 40, "Per-id overrides in block_values.json / entity_values.json.");
        bodyLineMuted(g, x, y + 52, "Inline overrides in rewards.json (blockRewards/entityRewards).");

        renderButton(g, x,         y + 76, 140, 22, "Open rewards.json",   "action:open_rewards",       mouseX, mouseY);
        renderButton(g, x + 148,   y + 76, 140, 22, "Open block_values",   "action:open_block_values",  mouseX, mouseY);
        renderButton(g, x,         y + 102, 140, 22, "Open entity_values", "action:open_entity_values", mouseX, mouseY);
        renderButton(g, x + 148,   y + 102, 140, 22, "Configs Folder",     "action:open_config",        mouseX, mouseY);
    }

    private void renderCiv(GuiGraphics g, int x, int y, int w, int h) {
        sectionHeading(g, x, y, "Civilization");
        bodyLine(g, x, y + 14, "Factions, jobs, professions, player shops.");
        bodyLineMuted(g, x, y + 28, "Roadmap-tracked. See DOCS/ROADMAP.md.");

        // "Coming Soon" badge.
        int bx = x + (w - 110) / 2;
        int by = y + h / 2 - 8;
        g.fill(bx, by, bx + 110, by + 18, PANEL_BG_ALT);
        outline(g, bx, by, bx + 110, by + 18, ACCENT_GOLD);
        String s = "COMING SOON";
        g.drawString(this.font, s, bx + (110 - this.font.width(s)) / 2, by + 5, ACCENT_GOLD, false);
    }

    private void renderHelp(GuiGraphics g, int x, int y, int w, int h) {
        sectionHeading(g, x, y, "Help & Commands");
        bodyLine(g, x, y + 14,  "/otter — opens this menu");
        bodyLine(g, x, y + 26,  "/money — show your balance");
        bodyLine(g, x, y + 38,  "/money set <player> <amount> — op only");
        bodyLineMuted(g, x, y + 56,  "Datapacks: data/otters_civ_revived/tags/block/");
        bodyLineMuted(g, x, y + 68,  "             data/otters_civ_revived/tags/entity_type/");
        bodyLineMuted(g, x, y + 84,  "Precedence: sibling file > inline > tag flat reward.");
        bodyLineMuted(g, x, y + 96,  "Docs: README.md, index.html, DOCS/");
    }

    private void renderButton(GuiGraphics g, int x, int y, int w, int h, String label, String actionKey, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = hover ? BTN_BG_HOVER : BTN_BG;
        g.fill(x, y, x + w, y + h, bg);
        outline(g, x, y, x + w, y + h, hover ? ACCENT_AQUA : PANEL_BORDER);
        int tw = this.font.width(label);
        g.drawString(this.font, label, x + (w - tw) / 2, y + (h - 8) / 2, hover ? ACCENT_GOLD : TEXT_PRIMARY, false);
        hotspots.add(new Rect(x, y, x + w, y + h, actionKey));
    }

    private void sectionHeading(GuiGraphics g, int x, int y, String title) {
        g.drawString(this.font, title, x, y, ACCENT_GOLD, false);
        g.fill(x, y + 10, x + 18, y + 11, ACCENT_AQUA);
    }

    private void bodyLine(GuiGraphics g, int x, int y, String s) {
        g.drawString(this.font, s, x, y, TEXT_PRIMARY, false);
    }

    private void bodyLineMuted(GuiGraphics g, int x, int y, String s) {
        g.drawString(this.font, s, x, y, TEXT_MUTED, false);
    }

    private static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    /** Filled rect with 1px shaved corners for a soft rounded feel without extra textures. */
    private static void roundedFill(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0 + 1, y0, x1 - 1, y1, color);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Rect r : hotspots) {
                if (r.contains(mouseX, mouseY)) {
                    handleAction(r.key);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    /**
     * Opens a folder on the local machine. Only useful in singleplayer / on the host's box,
     * but harmless on dedicated clients (just opens their own config dir).
     */
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
            if (f.exists()) {
                Util.getPlatform().openFile(f);
            } else {
                Util.getPlatform().openFile(dir.toFile());
            }
        } catch (Exception e) {
            FpsMod.LOGGER.warn("[otters_civ_revived] open config file failed", e);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Wallpaper-style backdrop is already drawn in {@link #render}; suppress vanilla blur. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // intentionally empty — render() draws its own backdrop
    }

    private record Rect(int x0, int y0, int x1, int y1, String key) {
        boolean contains(double mx, double my) {
            return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
        }
    }
}
