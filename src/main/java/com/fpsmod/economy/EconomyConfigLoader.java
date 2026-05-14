package com.fpsmod.economy;

import com.fpsmod.OogaMod;
import com.fpsmod.io.AtomicFileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EconomyConfigLoader {
    private static final String CONFIG_SUBDIR = "otters_civ_revived";
    private static final String FILE_NAME = "economy.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EconomyConfigLoader() {}

    public static Path configDirectoryPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_SUBDIR);
    }

    public static Path configFilePath() {
        return configDirectoryPath().resolve(FILE_NAME);
    }

    public static EconomyConfig loadOrCreate() {
        Path path = configFilePath();
        EconomyConfig defaults = EconomyConfig.defaults();
        try {
            Files.createDirectories(configDirectoryPath());
        } catch (IOException e) {
            OogaMod.LOGGER.error("[economy] Failed to create config dir", e);
            return defaults;
        }
        if (!Files.exists(path)) {
            try {
                String json = GSON.toJson(defaults) + System.lineSeparator();
                AtomicFileWriter.writeAtomically(path, w -> w.write(json));
                OogaMod.LOGGER.info("[economy] Created default economy config at {}", path);
            } catch (IOException e) {
                OogaMod.LOGGER.error("[economy] Failed to write default config", e);
            }
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) return defaults;
            EconomyConfig loaded = GSON.fromJson(root, EconomyConfig.class);
            return loaded == null ? defaults : loaded;
        } catch (RuntimeException | IOException e) {
            OogaMod.LOGGER.error("[economy] Failed to read {}; using defaults", path, e);
            return defaults;
        }
    }
}
