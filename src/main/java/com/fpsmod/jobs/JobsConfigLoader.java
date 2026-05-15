package com.fpsmod.jobs;

import com.fpsmod.OogaMod;
import com.fpsmod.io.AtomicFileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            JsonObject object = root.getAsJsonObject();
            if (object.has("jobs") && object.get("jobs").isJsonObject()) {
                OogaMod.LOGGER.warn(
                    "[otters_civ_revived/jobs] Detected legacy keyed jobs.json format at {}; loading with compatibility migration.",
                    path
                );
            }
            JobsConfig loaded = parseConfig(object, defaults);
            if (loaded == null) {
                OogaMod.LOGGER.warn("[otters_civ_revived/jobs] {} parsed as null; using defaults", path);
                return defaults;
            }
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

    static JobsConfig parseConfig(JsonObject root, JobsConfig defaults) {
        if (root == null) {
            return defaults;
        }
        JsonElement jobsElement = root.get("jobs");
        JobsConfig loaded;
        if (jobsElement != null && jobsElement.isJsonObject()) {
            loaded = migrateLegacyKeyedJobs(root, defaults);
        } else {
            loaded = GSON.fromJson(root, JobsConfig.class);
        }
        if (loaded == null) {
            return defaults;
        }
        loaded.sanitize();
        return loaded;
    }

    private static JobsConfig migrateLegacyKeyedJobs(JsonObject root, JobsConfig defaults) {
        JobsConfig migrated = cloneConfig(defaults);
        if (root.has("global") && root.get("global").isJsonObject()) {
            JobsConfig.GlobalSettings parsed = GSON.fromJson(root.get("global"), JobsConfig.GlobalSettings.class);
            if (parsed != null) {
                migrated.global = parsed;
            }
        }
        overlayLegacyGlobal(root, migrated);

        Map<String, Job> jobsById = new LinkedHashMap<>();
        for (Job job : migrated.jobs) {
            jobsById.put(job.id, job);
        }
        JsonObject jobsObject = root.getAsJsonObject("jobs");
        for (Map.Entry<String, JsonElement> entry : jobsObject.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            String jobId = Job.normalizeId(entry.getKey());
            if (jobId.isEmpty()) {
                continue;
            }
            Job job = jobsById.get(jobId);
            if (job == null) {
                job = new Job();
                job.id = jobId;
            }
            overlayLegacyJob(job, jobId, entry.getValue().getAsJsonObject());
            jobsById.put(jobId, job);
        }
        migrated.jobs = new ArrayList<>(jobsById.values());
        return migrated;
    }

    private static JobsConfig cloneConfig(JobsConfig config) {
        JobsConfig copy = GSON.fromJson(GSON.toJson(config == null ? JobsConfig.defaults() : config), JobsConfig.class);
        return copy == null ? JobsConfig.defaults() : copy;
    }

    private static void overlayLegacyGlobal(JsonObject root, JobsConfig config) {
        config.global.enabled = readBoolean(root, "enabled", config.global.enabled);
        config.global.activationPolicy = readString(root, "activationPolicy", config.global.activationPolicy);
        config.global.maxActiveJobs = readInt(root, "maxActiveJobs", config.global.maxActiveJobs);
        config.global.defaultIconGlyph = readString(root, "defaultIconGlyph", config.global.defaultIconGlyph);
        config.global.defaultIconKey = readString(root, "defaultIconKey", config.global.defaultIconKey);
        if (hasLegacyProgressionFields(root)) {
            applyLegacyProgression(root, config.global.defaultProgression);
            for (Job job : config.jobs) {
                applyLegacyProgression(root, job.progression);
            }
        }
        if (root.has("multiplierTopBonus")) {
            double topBonus = readDouble(root, "multiplierTopBonus", 0.0D);
            for (Job job : config.jobs) {
                applyLegacyMultiplierTopBonus(job, topBonus);
            }
        }
    }

    private static void overlayLegacyJob(Job job, String jobId, JsonObject rawJob) {
        Job parsed = GSON.fromJson(rawJob, Job.class);
        job.id = jobId;
        if (rawJob.has("displayName")) {
            job.displayName = parsed.displayName;
        }
        if (rawJob.has("shortLabel")) {
            job.shortLabel = parsed.shortLabel;
        }
        if (rawJob.has("description")) {
            job.description = parsed.description;
        }
        if (rawJob.has("enabled")) {
            job.enabled = parsed.enabled;
        }
        if (rawJob.has("joinable")) {
            job.joinable = parsed.joinable;
        }
        if (rawJob.has("hidden")) {
            job.hidden = parsed.hidden;
        }
        if (rawJob.has("sortOrder")) {
            job.sortOrder = parsed.sortOrder;
        }
        if (rawJob.has("iconGlyph")) {
            job.iconGlyph = parsed.iconGlyph;
        }
        if (rawJob.has("iconKey")) {
            job.iconKey = parsed.iconKey;
        }
        if (rawJob.has("triggers") && parsed.triggers != null) {
            job.triggers = parsed.triggers;
        }
        if (rawJob.has("progression") && parsed.progression != null) {
            job.progression = parsed.progression;
        }
        if (rawJob.has("boosts") && parsed.boosts != null) {
            job.boosts = parsed.boosts;
        }
        if (hasLegacyProgressionFields(rawJob)) {
            applyLegacyProgression(rawJob, job.progression);
        }
        if (rawJob.has("multiplierTopBonus")) {
            applyLegacyMultiplierTopBonus(job, readDouble(rawJob, "multiplierTopBonus", 0.0D));
        }

        List<String> legacyTagIds = readStringArray(rawJob, "tagIds");
        String legacyTagId = readString(rawJob, "tagId", "");
        if (!legacyTagId.isBlank()) {
            legacyTagIds.add(legacyTagId);
        }
        if (rawJob.has("xpPerEvent")) {
            job.progression.xpPerEvent = readLong(rawJob, "xpPerEvent", job.progression.xpPerEvent);
        }
        if (!legacyTagIds.isEmpty() || rawJob.has("eventType")) {
            JobTrigger trigger = ensurePrimaryTrigger(job);
            trigger.eventType = readString(rawJob, "eventType", inferredLegacyEventType(jobId));
            trigger.tagIds = legacyTagIds.isEmpty() ? trigger.tagIds : new ArrayList<>(legacyTagIds);
            trigger.requireEconomyReward = readBoolean(rawJob, "requireEconomyReward", true);
        }
    }

    private static JobTrigger ensurePrimaryTrigger(Job job) {
        if (job.triggers == null) {
            job.triggers = new ArrayList<>();
        }
        if (job.triggers.isEmpty()) {
            job.triggers.add(new JobTrigger());
        }
        return job.triggers.get(0);
    }

    private static boolean hasLegacyProgressionFields(JsonObject object) {
        return object.has("xpPerEvent")
            || object.has("maxLevel")
            || object.has("xpBase")
            || object.has("xpExponent")
            || object.has("levelThresholds");
    }

    private static void applyLegacyProgression(JsonObject source, JobProgression progression) {
        if (progression == null) {
            return;
        }
        if (source.has("levelThresholds")) {
            progression.levelThresholds = readLongArray(source, "levelThresholds");
        } else if (source.has("maxLevel") || source.has("xpBase") || source.has("xpExponent")) {
            progression.levelThresholds = new ArrayList<>();
        }
        progression.maxLevel = readInt(source, "maxLevel", progression.maxLevel);
        progression.xpPerEvent = readLong(source, "xpPerEvent", progression.xpPerEvent);
        progression.xpBase = readDouble(source, "xpBase", progression.xpBase);
        progression.xpExponent = readDouble(source, "xpExponent", progression.xpExponent);
    }

    private static void applyLegacyMultiplierTopBonus(Job job, double topBonus) {
        if (job == null || job.boosts == null || !(topBonus > 0.0D)) {
            return;
        }
        int maxLevel = Math.max(1, job.progression == null ? 1 : job.progression.maxLevel);
        List<JobLevelDouble> curve = new ArrayList<>();
        for (int level = 1; level <= maxLevel; level++) {
            JobLevelDouble entry = new JobLevelDouble();
            entry.level = level;
            entry.value = 1.0D + (topBonus * ((double) level / (double) maxLevel));
            curve.add(entry);
        }
        job.boosts.moneyMultiplier = 1.0D;
        job.boosts.moneyMultiplierByLevel = curve;
    }

    private static String inferredLegacyEventType(String jobId) {
        return "fighter".equals(Job.normalizeId(jobId)) ? JobEventType.MOB_KILL.id() : JobEventType.BLOCK_BREAK.id();
    }

    private static String readString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return fallback;
        }
        return element.getAsString();
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long readLong(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double readDouble(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static List<String> readStringArray(JsonObject object, String key) {
        List<String> out = new ArrayList<>();
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonArray()) {
            return out;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (item != null && item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                out.add(item.getAsString());
            }
        }
        return out;
    }

    private static List<Long> readLongArray(JsonObject object, String key) {
        List<Long> out = new ArrayList<>();
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonArray()) {
            return out;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (item == null || !item.isJsonPrimitive()) {
                continue;
            }
            try {
                out.add(item.getAsLong());
            } catch (RuntimeException ignored) {
                // Skip malformed legacy threshold entries.
            }
        }
        return out;
    }
}
