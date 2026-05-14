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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileJobsStore implements JobsStore {
    private static final String CONFIG_FOLDER = "otters_civ_revived";
    private static final String FILE_NAME = "jobs_state.json";
    private static final String LEGACY_FILE_NAME = "jobs.properties";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Pattern LINE_NAME = Pattern.compile("^#\\s*[Nn]ame:\\s*(.+)$");
    private static final Pattern LINE_ACTIVE =
        Pattern.compile("^([0-9a-fA-F\\-]{36})\\.active=([a-z0-9_:-]*)$");
    private static final Pattern LINE_XP =
        Pattern.compile("^([0-9a-fA-F\\-]{36})\\.xp\\.([a-z0-9_:-]+)=(\\d+)$");

    private final Path filePath;

    public FileJobsStore() {
        this(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FOLDER).resolve(FILE_NAME));
    }

    FileJobsStore(Path explicit) {
        this.filePath = explicit;
    }

    @Override
    public JobsLedger load() {
        AtomicFileWriter.deleteStaleTemp(filePath);

        if (Files.exists(filePath)) {
            return loadJsonState(filePath);
        }
        Path legacyPath = filePath.resolveSibling(LEGACY_FILE_NAME);
        if (Files.exists(legacyPath)) {
            JobsLedger migrated = loadLegacyProperties(legacyPath);
            save(migrated.states(), migrated.displayHints());
            try {
                Files.deleteIfExists(legacyPath);
            } catch (IOException e) {
                OogaMod.LOGGER.warn("[otters_civ_revived/jobs] Failed to delete legacy jobs store {}", legacyPath, e);
            }
            return migrated;
        }
        return new JobsLedger(Map.of(), Map.of());
    }

    private JobsLedger loadJsonState(Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                return new JobsLedger(Map.of(), Map.of());
            }
            JsonObject players = root.getAsJsonObject().getAsJsonObject("players");
            if (players == null) {
                return new JobsLedger(Map.of(), Map.of());
            }
            Map<UUID, JobState> states = new HashMap<>();
            Map<UUID, String> hints = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
                UUID id = tryUuid(entry.getKey());
                if (id == null || entry.getValue() == null || !entry.getValue().isJsonObject()) {
                    continue;
                }
                StoredPlayerState stored = GSON.fromJson(entry.getValue(), StoredPlayerState.class);
                JobState state = new JobState();
                state.setActiveJobs(stored.activeJobs, Integer.MAX_VALUE);
                if (stored.xpByJobId != null) {
                    for (Map.Entry<String, Long> xp : stored.xpByJobId.entrySet()) {
                        if (xp.getValue() != null) {
                            state.setXp(xp.getKey(), xp.getValue());
                        }
                    }
                }
                states.put(id, state);
                String hint = sanitizeHintForStorage(stored.nameHint);
                if (!hint.isEmpty()) {
                    hints.put(id, hint);
                }
            }
            return new JobsLedger(states, hints);
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[otters_civ_revived] Failed to read jobs store at {}", path, e);
            return new JobsLedger(Map.of(), Map.of());
        }
    }

    private JobsLedger loadLegacyProperties(Path legacyPath) {
        Map<UUID, JobState> states = new HashMap<>();
        Map<UUID, String> hints = new HashMap<>();
        try {
            String pendingName = null;
            UUID lastSeen = null;
            for (String raw : Files.readAllLines(legacyPath)) {
                String line = raw.trim();
                if (line.isEmpty() || (line.startsWith("#") && !LINE_NAME.matcher(line).matches())) {
                    continue;
                }
                Matcher nm = LINE_NAME.matcher(line);
                if (nm.matches()) {
                    pendingName = sanitizeHintForStorage(nm.group(1));
                    continue;
                }
                Matcher am = LINE_ACTIVE.matcher(line);
                if (am.matches()) {
                    UUID id = UUID.fromString(am.group(1));
                    JobState s = states.computeIfAbsent(id, k -> new JobState());
                    if (!am.group(2).isBlank()) {
                        s.setActiveJobs(List.of(am.group(2)), 1);
                    }
                    if (pendingName != null && !pendingName.isEmpty() && !id.equals(lastSeen)) {
                        hints.put(id, pendingName);
                        pendingName = null;
                    }
                    lastSeen = id;
                    continue;
                }
                Matcher xm = LINE_XP.matcher(line);
                if (xm.matches()) {
                    UUID id = UUID.fromString(xm.group(1));
                    JobState s = states.computeIfAbsent(id, k -> new JobState());
                    s.setXp(xm.group(2), Long.parseLong(xm.group(3)));
                    if (pendingName != null && !pendingName.isEmpty() && !id.equals(lastSeen)) {
                        hints.put(id, pendingName);
                        pendingName = null;
                    }
                    lastSeen = id;
                }
            }
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[otters_civ_revived/jobs] Failed to migrate legacy jobs store at {}", legacyPath, e);
            return new JobsLedger(Map.of(), Map.of());
        }
        return new JobsLedger(states, hints);
    }

    @Override
    public void save(Map<UUID, JobState> states, Map<UUID, String> displayHints) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(displayHints, "displayHints");

        TreeMap<UUID, JobState> sorted =
            new TreeMap<>((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        sorted.putAll(states);

        try {
            Files.createDirectories(filePath.getParent());
            AtomicFileWriter.writeAtomicallyWithBackup(filePath, w -> {
                StoredLedger ledger = new StoredLedger();
                for (Map.Entry<UUID, JobState> e : sorted.entrySet()) {
                    UUID id = e.getKey();
                    JobState state = e.getValue();
                    StoredPlayerState stored = new StoredPlayerState();
                    stored.activeJobs = new ArrayList<>(state.activeJobIds());
                    stored.xpByJobId = new LinkedHashMap<>(state.snapshotXp());
                    String hint = sanitizeHintForStorage(displayHints.get(id));
                    stored.nameHint = hint.isEmpty() ? null : hint;
                    ledger.players.put(id.toString(), stored);
                }
                w.write(GSON.toJson(ledger));
                w.newLine();
            });
        } catch (IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived] Failed to write jobs store at {}", filePath, e);
        }
    }

    static String sanitizeHintForStorage(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replace('\n', ' ').replace('\r', ' ').replace('#', ' ').trim();
        return cleaned.length() > 128 ? cleaned.substring(0, 128) : cleaned;
    }

    @SuppressWarnings("SameParameterValue")
    private static UUID tryUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static final class StoredLedger {
        Map<String, StoredPlayerState> players = new LinkedHashMap<>();
    }

    private static final class StoredPlayerState {
        List<String> activeJobs = List.of();
        Map<String, Long> xpByJobId = Map.of();
        String nameHint;
    }
}
