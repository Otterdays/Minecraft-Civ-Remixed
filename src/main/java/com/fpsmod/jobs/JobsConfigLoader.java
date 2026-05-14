package com.fpsmod.jobs;

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

/** Loads and persists the operator-editable jobs tuning file. */
public final class JobsConfigLoader {
    private static final String CONFIG_SUBDIR = "otters_civ_revived";
    private static final String FILE_NAME = "jobs.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JobsConfigLoader() {}

    public static Path configDirectoryPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_SUBDIR);
    }

    public static Path configFilePath() {
        return configDirectoryPath().resolve(FILE_NAME);
    }

    public static JobsConfig loadOrCreate() {
        Path dir = configDirectoryPath();
        Path path = configFilePath();
        JobsConfig defaults = JobsConfig.defaults();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived/jobs] Failed to create config directory {}", dir, e);
            return defaults;
        }

        if (!Files.exists(path)) {
            try {
                writeDefaults(path, defaults);
                OogaMod.LOGGER.info("[otters_civ_revived/jobs] Created default jobs config at {}", path);
            } catch (IOException e) {
                OogaMod.LOGGER.error("[otters_civ_revived/jobs] Failed to write default jobs config", e);
            }
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                OogaMod.LOGGER.warn("[otters_civ_revived/jobs] {} must be a JSON object; using defaults", path);
                return defaults;
            }
            JobsConfig loaded = GSON.fromJson(root, JobsConfig.class);
            if (loaded == null) {
                OogaMod.LOGGER.warn("[otters_civ_revived/jobs] {} parsed as null; using defaults", path);
                return defaults;
            }
            loaded.sanitize();
            return loaded;
        } catch (RuntimeException | IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived/jobs] Failed to read {}; using defaults", path, e);
            return defaults;
        }
    }

    private static void writeDefaults(Path path, JobsConfig config) throws IOException {
        String json = GSON.toJson(config) + System.lineSeparator();
        AtomicFileWriter.writeAtomically(path, w -> w.write(json));
    }
}
