package com.fpsmod.client.ui;

import com.fpsmod.OogaMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persists the {@code /otter} menu color theme. Stored next to {@link com.fpsmod.client.jobs.JobsHudConfig}.
 */
public final class OtterUiThemeConfig {
    private static final OtterUiThemeConfig INSTANCE = new OtterUiThemeConfig();

    private static final String FILE_NAME = "otter_ui.properties";
    private static final String KEY_THEME = "theme";

    private final Path file;
    private OtterUiTheme theme = OtterUiTheme.OTTER;

    private OtterUiThemeConfig() {
        this.file = FabricLoader.getInstance().getConfigDir()
            .resolve(OogaMod.MOD_ID)
            .resolve(FILE_NAME);
        load();
    }

    public static OtterUiThemeConfig instance() {
        return INSTANCE;
    }

    public OtterUiTheme theme() {
        return theme;
    }

    public OtterUiPalette palette() {
        return theme.palette();
    }

    /** Cycles OTTER → MIDNIGHT → SUNSET → MINT → … and saves. */
    public void cycleTheme() {
        this.theme = theme.next();
        save();
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[project_ooga] Could not read otter UI theme; using default", e);
            return;
        }
        this.theme = OtterUiTheme.fromId(p.getProperty(KEY_THEME, OtterUiTheme.OTTER.name()));
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[project_ooga] Could not create otter UI config dir", e);
            return;
        }
        Properties p = new Properties();
        p.setProperty(KEY_THEME, theme.name());
        try (Writer w = Files.newBufferedWriter(file)) {
            p.store(w, "Otters Civ. Revived — /otter menu theme");
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[project_ooga] Could not write otter UI theme", e);
        }
    }
}
