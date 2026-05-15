package com.fpsmod.client;

import com.fpsmod.OogaMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * @deprecated Legacy FPS overlay from the original template mod. Disabled in
 * {@link com.fpsmod.FpsModClient} to avoid conflicts with the standalone
 * FPS overlay mod. Kept only as reference; do not re-enable without verifying
 * no other mod in the pack uses the same {@code fpsmod} mod ID.
 */
@SuppressWarnings({"removal", "null"})
@Deprecated(since = "1.0.0", forRemoval = true)
public final class FpsHudOverlay {
    private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "fps_overlay");
    private static final int PAD = 6;

    private static final FpsHudConfig CONFIG = new FpsHudConfig();
    private static volatile boolean showHud = true;
    private static volatile int displayedFps;
    private static volatile long lastFpsSampleMs;

    private FpsHudOverlay() {
    }

    public static void register() {
        showHud = CONFIG.loadShowHud(true);

        HudElementRegistry.attachElementBefore(
            VanillaHudElements.MISC_OVERLAYS,
            OVERLAY_ID,
            FpsHudOverlay::render
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) {
                return;
            }

            long now = Util.getMillis();
            if (now - lastFpsSampleMs >= 1000L) {
                lastFpsSampleMs = now;
                displayedFps = client.getFps();
            }
        });
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.options.hideGui) {
            return;
        }

        if (!showHud) {
            return;
        }

        int x = PAD;
        int y = PAD;
        String fpsLine = "FPS: " + displayedFps;
        int textX = x;
        int textY = y;
        int shadow = ARGB.color(255, 0, 0, 0);
        graphics.text(client.font, fpsLine, textX + 1, textY + 1, shadow, false);
        int fpsColor = ARGB.color(255, 200, 255, 200);
        graphics.text(client.font, fpsLine, textX, textY, fpsColor, false);
    }

    public static boolean isHudShown() {
        return showHud;
    }

    public static void setHudShown(boolean enabled) {
        if (showHud == enabled) {
            return;
        }
        showHud = enabled;
        CONFIG.saveShowHud(showHud);
        OogaMod.LOGGER.info("{} 🔁 FPS HUD {}", OogaMod.MOD_ID, showHud ? "enabled" : "disabled");
    }

    public static void toggleHud() {
        setHudShown(!showHud);
    }
}
