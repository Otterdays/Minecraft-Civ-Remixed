package com.fpsmod.client.jobs;

import com.fpsmod.OogaMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Client-side config for the Jobs HUD overlay. Lives next to the FPS HUD config so the legacy
 * {@code fpsmod} folder keeps housing pure client-display knobs.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code visible} — master toggle (default true)</li>
 *   <li>{@code offsetX} — horizontal nudge in pixels relative to the centered anchor (default 0)</li>
 *   <li>{@code offsetY} — pixels above the vanilla XP bar (positive = higher, default 12)</li>
 *   <li>{@code scale}   — render scale 0.75 … 2.0 (default 1.0)</li>
 * </ul>
 */
public final class JobsHudConfig {
    public static final float SCALE_MIN = 0.75f;
    public static final float SCALE_MAX = 2.00f;

    private static final String FILE_NAME = "jobs_hud.properties";
    private static final String KEY_VISIBLE = "visible";
    private static final String KEY_OFFSET_X = "offsetX";
    private static final String KEY_OFFSET_Y = "offsetY";
    private static final String KEY_SCALE = "scale";

    private final Path file;
    private boolean visible = true;
    private int offsetX = 0;
    private int offsetY = 12;
    private float scale = 1.0f;

    public JobsHudConfig() {
        this.file = FabricLoader.getInstance().getConfigDir()
            .resolve(OogaMod.MOD_ID)
            .resolve(FILE_NAME);
        load();
    }

    public boolean visible() { return visible; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }
    public float scale() { return scale; }

    public void setVisible(boolean v) { this.visible = v; save(); }
    public void setOffsetX(int x) { this.offsetX = clampOffset(x); save(); }
    public void setOffsetY(int y) { this.offsetY = clampOffset(y); save(); }
    public void nudgeOffsetX(int dx) { setOffsetX(offsetX + dx); }
    public void nudgeOffsetY(int dy) { setOffsetY(offsetY + dy); }

    public void setScale(float s) {
        this.scale = Math.max(SCALE_MIN, Math.min(SCALE_MAX, s));
        save();
    }
    public void nudgeScale(float ds) { setScale(scale + ds); }

    public void reset() {
        this.visible = true;
        this.offsetX = 0;
        this.offsetY = 12;
        this.scale = 1.0f;
        save();
    }

    private static int clampOffset(int v) {
        return Math.max(-400, Math.min(400, v));
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[fpsmod] Could not read jobs HUD config; using defaults", e);
            return;
        }
        this.visible = Boolean.parseBoolean(p.getProperty(KEY_VISIBLE, "true"));
        try {
            this.offsetX = clampOffset(Integer.parseInt(p.getProperty(KEY_OFFSET_X, "0")));
            this.offsetY = clampOffset(Integer.parseInt(p.getProperty(KEY_OFFSET_Y, "12")));
        } catch (NumberFormatException ignored) {
        }
        try {
            float s = Float.parseFloat(p.getProperty(KEY_SCALE, "1.0"));
            this.scale = Math.max(SCALE_MIN, Math.min(SCALE_MAX, s));
        } catch (NumberFormatException ignored) {
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[fpsmod] Could not create jobs HUD config dir", e);
            return;
        }
        Properties p = new Properties();
        p.setProperty(KEY_VISIBLE, Boolean.toString(visible));
        p.setProperty(KEY_OFFSET_X, Integer.toString(offsetX));
        p.setProperty(KEY_OFFSET_Y, Integer.toString(offsetY));
        p.setProperty(KEY_SCALE, Float.toString(scale));
        try (Writer w = Files.newBufferedWriter(file)) {
            p.store(w, "Otters Civ. Revived — Jobs HUD overlay");
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[fpsmod] Could not write jobs HUD config", e);
        }
    }
}
