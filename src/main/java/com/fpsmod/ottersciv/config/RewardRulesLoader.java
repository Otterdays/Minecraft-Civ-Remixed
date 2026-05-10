package com.fpsmod.ottersciv.config;

import com.fpsmod.FpsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RewardRulesLoader {
    private static final String CONFIG_SUBDIR = "otters_civ_revived";
    private static final String FILE_NAME = "rewards.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final java.lang.reflect.Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private RewardRulesLoader() {
    }

    public static Path configFilePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_SUBDIR).resolve(FILE_NAME);
    }

    public static RewardRules loadOrCreate() {
        Path path = configFilePath();
        RewardRules defaults = RewardRules.defaults();
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                writeDefaults(path, defaults);
                FpsMod.LOGGER.info("[otters_civ_revived] Created default rewards config at {}", path);
            } catch (IOException e) {
                FpsMod.LOGGER.error("[otters_civ_revived] Failed to write default rewards config", e);
            }
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            return parseRewardsJson(reader, defaults, path.toAbsolutePath().normalize().toString());
        } catch (JsonParseException | IOException e) {
            FpsMod.LOGGER.error("[otters_civ_revived] Failed to read {}; using defaults", path, e);
            return defaults;
        }
    }

    /** Same-package tests may call with optional {@code diagnosticLabel}. */
    static RewardRules parseRewardsJson(Reader reader, RewardRules defaults, String diagnosticLabel)
        throws JsonParseException {
        JsonElement root = com.google.gson.JsonParser.parseReader(reader);
        if (root == null || !root.isJsonObject()) {
            FpsMod.LOGGER.warn("[otters_civ_revived] rewards.json root must be an object; using defaults");
            return defaults;
        }
        JsonObject jo = root.getAsJsonObject();
        RewardRules r = RewardRules.defaults();
        if (jo.has("enabled")) {
            r.enabled = jo.get("enabled").getAsBoolean();
        }
        if (jo.has("blockTag")) {
            r.blockTag = jo.get("blockTag").getAsString();
        }
        if (jo.has("entityTag")) {
            r.entityTag = jo.get("entityTag").getAsString();
        }
        if (jo.has("blockReward")) {
            r.blockReward = jo.get("blockReward").getAsLong();
        }
        if (jo.has("entityReward")) {
            r.entityReward = jo.get("entityReward").getAsLong();
        }
        if (jo.has("blockCooldownMs")) {
            r.blockCooldownMs = jo.get("blockCooldownMs").getAsLong();
        }
        if (jo.has("entityCooldownMs")) {
            r.entityCooldownMs = jo.get("entityCooldownMs").getAsLong();
        }
        if (jo.has("skipCreative")) {
            r.skipCreative = jo.get("skipCreative").getAsBoolean();
        }
        if (jo.has("skipSpectator")) {
            r.skipSpectator = jo.get("skipSpectator").getAsBoolean();
        }
        if (jo.has("announceRewards")) {
            r.announceRewards = jo.get("announceRewards").getAsBoolean();
        }
        if (jo.has("dimensionBlacklist")) {
            List<String> list = GSON.fromJson(jo.get("dimensionBlacklist"), STRING_LIST_TYPE);
            r.dimensionBlacklist = list != null ? new java.util.ArrayList<>(list) : new java.util.ArrayList<>();
        }
        if (jo.has("blockRewards")) {
            r.blockRewards = parseIdLongMap(diagnosticLabel, "blockRewards", jo.get("blockRewards"));
        } else if (r.blockRewards == null) {
            r.blockRewards = new LinkedHashMap<>();
        }
        if (jo.has("entityRewards")) {
            r.entityRewards = parseIdLongMap(diagnosticLabel, "entityRewards", jo.get("entityRewards"));
        } else if (r.entityRewards == null) {
            r.entityRewards = new LinkedHashMap<>();
        }
        clampNonNegative(r);
        sanitizeTags(r);
        return r;
    }

    /** Deserializes a JSON object of string ids → long amounts; skips invalid ids and logs warnings. */
    static Map<String, Long> parseIdLongMap(String fileLabel, String fieldName, JsonElement elem) {
        Map<String, Long> canonical = new LinkedHashMap<>();
        if (elem == null || elem.isJsonNull()) {
            return canonical;
        }
        if (!elem.isJsonObject()) {
            FpsMod.LOGGER.warn(
                "[otters_civ_revived] {}: {} must be a JSON object; ignoring",
                fileLabel,
                fieldName
            );
            return canonical;
        }
        JsonObject o = elem.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
            String keyTrim = entry.getKey() != null ? entry.getKey().trim() : "";
            Identifier id = Identifier.tryParse(keyTrim);
            if (id == null) {
                FpsMod.LOGGER.warn(
                    "[otters_civ_revived] {}: skipping invalid {} key \"{}\"",
                    fileLabel,
                    fieldName,
                    entry.getKey()
                );
                continue;
            }
            JsonElement vel = entry.getValue();
            if (vel == null || vel.isJsonNull()) {
                canonical.put(id.toString(), 0L);
                continue;
            }
            long v = readNonNegativeLong(vel, entry.getKey());
            canonical.put(id.toString(), v);
        }
        return canonical;
    }

    private static long readNonNegativeLong(JsonElement vel, String keyForLog) {
        try {
            if (vel.getAsJsonPrimitive().isNumber()) {
                long v = vel.getAsLong();
                return Math.max(0L, v);
            }
        } catch (NumberFormatException | UnsupportedOperationException | ClassCastException ignored) {
            // fall through
        }
        FpsMod.LOGGER.warn("[otters_civ_revived] skipping non-numeric reward for key \"{}\"", keyForLog);
        return 0L;
    }

    static RewardRules parseRewardsJson(String json, RewardRules defaults) throws JsonParseException {
        return parseRewardsJson(new java.io.StringReader(json), defaults, "<embedded>");
    }

    private static void sanitizeTags(RewardRules r) {
        if (r.blockTag == null || r.blockTag.isBlank()) {
            r.blockTag = RewardRules.defaults().blockTag;
        }
        if (r.entityTag == null || r.entityTag.isBlank()) {
            r.entityTag = RewardRules.defaults().entityTag;
        }
        if (r.dimensionBlacklist == null) {
            r.dimensionBlacklist = new java.util.ArrayList<>();
        }
        if (r.blockRewards == null) {
            r.blockRewards = new LinkedHashMap<>();
        }
        if (r.entityRewards == null) {
            r.entityRewards = new LinkedHashMap<>();
        }
    }

    private static void clampNonNegative(RewardRules r) {
        if (r.blockReward < 0L) {
            r.blockReward = 0L;
        }
        if (r.entityReward < 0L) {
            r.entityReward = 0L;
        }
        if (r.blockRewards != null) {
            for (Map.Entry<String, Long> e : r.blockRewards.entrySet()) {
                if (e.getValue() < 0L) {
                    r.blockRewards.put(e.getKey(), 0L);
                }
            }
        }
        if (r.entityRewards != null) {
            for (Map.Entry<String, Long> e : r.entityRewards.entrySet()) {
                if (e.getValue() < 0L) {
                    r.entityRewards.put(e.getKey(), 0L);
                }
            }
        }
        if (r.blockCooldownMs < 0L) {
            r.blockCooldownMs = 0L;
        }
        if (r.entityCooldownMs < 0L) {
            r.entityCooldownMs = 0L;
        }
    }

    private static void writeDefaults(Path path, RewardRules rules) throws IOException {
        Files.writeString(path, GSON.toJson(rules));
    }
}
