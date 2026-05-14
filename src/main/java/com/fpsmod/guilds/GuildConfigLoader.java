package com.fpsmod.guilds;

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

public final class GuildConfigLoader {
    private static final String CONFIG_SUBDIR = "otters_civ_revived";
    private static final String FILE_NAME = "guilds.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GuildConfigLoader() {}

    public static Path configDirectoryPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_SUBDIR);
    }

    public static Path configFilePath() {
        return configDirectoryPath().resolve(FILE_NAME);
    }

    public static GuildConfig loadOrCreate() {
        Path path = configFilePath();
        GuildConfig defaults = new GuildConfig();
        try {
            Files.createDirectories(configDirectoryPath());
        } catch (IOException e) {
            OogaMod.LOGGER.error("[guilds] Failed to create config dir", e);
            return defaults;
        }
        if (!Files.exists(path)) {
            try {
                String json = GSON.toJson(defaults) + System.lineSeparator();
                AtomicFileWriter.writeAtomically(path, w -> w.write(json));
                OogaMod.LOGGER.info("[guilds] Created default guilds config at {}", path);
            } catch (IOException e) {
                OogaMod.LOGGER.error("[guilds] Failed to write default config", e);
            }
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) return defaults;
            GuildConfig loaded = GSON.fromJson(root, GuildConfig.class);
            return loaded == null ? defaults : loaded;
        } catch (RuntimeException | IOException e) {
            OogaMod.LOGGER.error("[guilds] Failed to read {}; using defaults", path, e);
            return defaults;
        }
    }
}
