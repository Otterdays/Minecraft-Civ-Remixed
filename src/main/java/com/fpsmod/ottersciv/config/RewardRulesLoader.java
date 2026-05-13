package com.fpsmod.ottersciv.config;

import com.fpsmod.OogaMod;
import com.fpsmod.io.AtomicFileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RewardRulesLoader {
    private static final String CONFIG_SUBDIR = "otters_civ_revived";
    private static final String FILE_NAME = "rewards.json";
    /** Whole-file maps of block id → payout; merged on top of {@code rewards.json} {@code blockRewards}. */
    public static final String BLOCK_VALUES_FILE = "block_values.json";
    /** Whole-file maps of entity type id → payout; merged on top of {@code rewards.json} {@code entityRewards}. */
    public static final String ENTITY_VALUES_FILE = "entity_values.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final java.lang.reflect.Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private RewardRulesLoader() {
    }

    public static Path configFilePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_SUBDIR).resolve(FILE_NAME);
    }

    /** Config directory containing {@link #BLOCK_VALUES_FILE}, {@link #ENTITY_VALUES_FILE}, and {@code rewards.json}. */
    public static Path configDirectoryPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_SUBDIR);
    }

    /**
     * Loads {@code rewards.json} only — no sibling value JSON merge yet, no registry expansion.
     * Call {@link #finalizeRewardsForRunningServer} on the logical server started hook to hydrate
     * {@code block_values.json}/{@code entity_values.json} from tag expansion + disk.
     */
    public static RewardRules loadBootstrapRewards() {
        Path dir = configDirectoryPath();
        Path path = dir.resolve(FILE_NAME);
        RewardRules defaults = RewardRules.defaults();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived] Failed to create config directory {}", dir, e);
            return defaults;
        }

        if (!Files.exists(path)) {
            try {
                writeDefaults(path, defaults);
                OogaMod.LOGGER.info("[otters_civ_revived] Created default rewards config at {}", path);
            } catch (IOException e) {
                OogaMod.LOGGER.error("[otters_civ_revived] Failed to write default rewards config", e);
            }
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            RewardRules loaded = parseRewardsJson(reader, defaults, path.toAbsolutePath().normalize().toString());
            clampNonNegative(loaded);
            sanitizeTags(loaded);
            return loaded;
        } catch (JsonParseException | IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived] Failed to read {}; using defaults", path, e);
            return RewardRules.defaults();
        }
    }

    /**
     * Re-reads configs with registries/tags available, merges tag-derived defaults with rewards.json inline maps
     * and optional sibling overlays, and persists {@link #BLOCK_VALUES_FILE}/{@link #ENTITY_VALUES_FILE} when
     * those files were missing or contained no keys so operators always get editable per-id payouts.
     */
    public static RewardRules finalizeRewardsForRunningServer(MinecraftServer server) {
        Path dir = configDirectoryPath();
        Path path = dir.resolve(FILE_NAME);
        RewardRules defaults = RewardRules.defaults();
        RewardRules loaded;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived] Failed to prepare config dir {}", dir, e);
            return defaults;
        }

        if (!Files.exists(path)) {
            OogaMod.LOGGER.warn("[otters_civ_revived] finalize: missing {}; using coded defaults once", path);
            loaded = RewardRules.defaults();
        } else {
            try (Reader reader = Files.newBufferedReader(path)) {
                loaded = parseRewardsJson(reader, defaults, path.toAbsolutePath().normalize().toString());
            } catch (JsonParseException | IOException e) {
                OogaMod.LOGGER.error(
                    "[otters_civ_revived] finalize: failed reading {}; using defaults for this session",
                    path,
                    e
                );
                loaded = RewardRules.defaults();
            }
        }

        Path blkPath = dir.resolve(BLOCK_VALUES_FILE);
        Path entPath = dir.resolve(ENTITY_VALUES_FILE);

        Map<String, Long> blkSibling = readSiblingLongMapOrEmpty(blkPath);
        Map<String, Long> entSibling = readSiblingLongMapOrEmpty(entPath);

        ServerLevel level = server.overworld();
        if (level == null) {
            OogaMod.LOGGER.warn("[otters_civ_revived] finalize: overworld unavailable; tag expansion skipped");
        }

        LinkedHashMap<String, Long> tier1Blocks = level == null ? new LinkedHashMap<>()
            : RewardTagExpansion.payoutsForTaggedBlocks(
                level,
                RewardTagExpansion.parseBlockTagKey(loaded.blockTag),
                loaded.blockReward
            );

        LinkedHashMap<String, Long> blockEff = composeEffectiveIdMap(
            tier1Blocks,
            loaded.blockRewards,
            blkSibling,
            blkPath,
            level != null ? "[blocks]" : "[blocks-tags-skipped]"
        );
        loaded.blockRewards = blockEff;

        LinkedHashMap<String, Long> tier1Entities = level == null ? new LinkedHashMap<>()
            : RewardTagExpansion.payoutsForTaggedEntities(
                level,
                RewardTagExpansion.parseEntityTagKey(loaded.entityTag),
                loaded.entityReward
            );

        LinkedHashMap<String, Long> entityEff = composeEffectiveIdMap(
            tier1Entities,
            loaded.entityRewards,
            entSibling,
            entPath,
            level != null ? "[entities]" : "[entities-tags-skipped]"
        );
        loaded.entityRewards = entityEff;

        clampNonNegative(loaded);
        sanitizeTags(loaded);
        return loaded;
    }

    /**
     * Visible for tests: merges tier-1 tag-expanded payouts, inline {@code rewards.json} maps, and an existing
     * sibling-disk overlay (precedence: tier1 → inline → sibling), and persists the result to {@code siblingOut}
     * only when the on-disk sibling map was empty (missing, {@code {}}, or corrupt). Mirrors the production
     * path used inside {@link #finalizeRewardsForRunningServer}, but takes prebuilt maps so it runs without a server.
     */
    static LinkedHashMap<String, Long> composeEffectiveIdMap(
        LinkedHashMap<String, Long> tier1TagMembers,
        Map<String, Long> rewardsJsonInlineObj,
        Map<String, Long> siblingDisk,
        Path siblingOut,
        String label
    ) {
        LinkedHashMap<String, Long> merged = new LinkedHashMap<>();
        merged.putAll(tier1TagMembers);
        if (rewardsJsonInlineObj != null) {
            merged.putAll(rewardsJsonInlineObj);
        }
        merged.putAll(siblingDisk);

        boolean siblingHadKeys = !siblingDisk.isEmpty();
        if (!siblingHadKeys) {
            try {
                persistSortedLongMapJson(siblingOut, merged);
                OogaMod.LOGGER.info(
                    "[otters_civ_revived] Prefilled {} {} entries ({}) — edit amounts without touching tags.",
                    merged.size(),
                    siblingOut.getFileName(),
                    label
                );
            } catch (IOException e) {
                OogaMod.LOGGER.error("[otters_civ_revived] Could not persist {}", siblingOut, e);
            }
        }

        return merged;
    }

    private static Map<String, Long> readSiblingLongMapOrEmpty(Path file) {
        if (!Files.isRegularFile(file)) {
            return Collections.emptyMap();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement root = com.google.gson.JsonParser.parseReader(reader);
            Map<String, Long> mapped = parseIdLongMap(
                file.toAbsolutePath().normalize().toString(),
                file.getFileName().toString(),
                root
            );
            return mapped != null ? mapped : Collections.emptyMap();
        } catch (JsonParseException | IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived] Could not parse {}; ignoring", file, e);
            return Collections.emptyMap();
        }
    }

    private static void persistSortedLongMapJson(Path path, LinkedHashMap<String, Long> merged) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        TreeMap<String, Long> sorted = new TreeMap<>(merged);
        String json = GSON.toJson(sorted) + System.lineSeparator();
        AtomicFileWriter.writeAtomically(path, w -> w.write(json));
    }

    /**
     * Merges sibling {@link #BLOCK_VALUES_FILE} / {@link #ENTITY_VALUES_FILE} into {@code rules} if present.
     * File entries override overlapping keys already set from {@code rewards.json}.
     */
    static void mergeExternalValueFiles(Path configDir, RewardRules rules) {
        Path blockVals = configDir.resolve(BLOCK_VALUES_FILE);
        mergeFlatIdLongFile(blockVals, rules.blockRewards);
        Path entityVals = configDir.resolve(ENTITY_VALUES_FILE);
        mergeFlatIdLongFile(entityVals, rules.entityRewards);
    }

    private static void mergeFlatIdLongFile(Path file, Map<String, Long> destination) {
        if (destination == null || !Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement root = com.google.gson.JsonParser.parseReader(reader);
            String abs = file.toAbsolutePath().normalize().toString();
            String fname = file.getFileName().toString();
            Map<String, Long> overlay = parseIdLongMap(abs, fname, root);
            // When the whole JSON file is rejected (non-object), parseIdLongMap returns empty map.
            destination.putAll(overlay);
        } catch (JsonParseException | IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived] Failed to read value overlay {}; ignoring", file, e);
        }
    }

    /** Same-package tests may call with optional {@code diagnosticLabel}. */
    static RewardRules parseRewardsJson(Reader reader, RewardRules defaults, String diagnosticLabel)
        throws JsonParseException {
        JsonElement root = com.google.gson.JsonParser.parseReader(reader);
        if (root == null || !root.isJsonObject()) {
            OogaMod.LOGGER.warn("[otters_civ_revived] rewards.json root must be an object; using defaults");
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
            OogaMod.LOGGER.warn(
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
                OogaMod.LOGGER.warn(
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
        OogaMod.LOGGER.warn("[otters_civ_revived] skipping non-numeric reward for key \"{}\"", keyForLog);
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
        String json = GSON.toJson(rules) + System.lineSeparator();
        AtomicFileWriter.writeAtomically(path, w -> w.write(json));
    }
}
