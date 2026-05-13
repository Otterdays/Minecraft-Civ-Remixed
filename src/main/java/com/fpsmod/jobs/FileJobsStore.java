package com.fpsmod.jobs;

import com.fpsmod.OogaMod;
import com.fpsmod.io.AtomicFileWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileJobsStore implements JobsStore {
    private static final String CONFIG_FOLDER = "otters_civ_revived";
    private static final String FILE_NAME = "jobs.properties";

    private static final Pattern LINE_NAME = Pattern.compile("^#\\s*[Nn]ame:\\s*(.+)$");
    private static final Pattern LINE_ACTIVE =
        Pattern.compile("^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\\.active=(\\w*)$");
    private static final Pattern LINE_XP =
        Pattern.compile("^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\\.xp\\.(\\w+)=(\\d+)$");

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

        if (!Files.exists(filePath)) {
            return new JobsLedger(Map.of(), Map.of());
        }
        Map<UUID, JobState> states = new HashMap<>();
        Map<UUID, String> hints = new HashMap<>();
        try {
            String pendingName = null;
            UUID lastSeen = null;
            for (String raw : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
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
                    Job.bySlug(am.group(2)).ifPresent(s::setActive);
                    if (pendingName != null && !pendingName.isEmpty() && lastSeen == null) {
                        hints.put(id, pendingName);
                    }
                    if (!id.equals(lastSeen)) {
                        if (pendingName != null && !pendingName.isEmpty()) {
                            hints.put(id, pendingName);
                        }
                        lastSeen = id;
                        pendingName = null;
                    }
                    continue;
                }
                Matcher xm = LINE_XP.matcher(line);
                if (xm.matches()) {
                    UUID id = UUID.fromString(xm.group(1));
                    Optional<Job> job = Job.bySlug(xm.group(2));
                    long amount = Long.parseLong(xm.group(3));
                    if (job.isPresent()) {
                        JobState s = states.computeIfAbsent(id, k -> new JobState());
                        s.addXp(job.get(), amount);
                    }
                    if (!id.equals(lastSeen)) {
                        if (pendingName != null && !pendingName.isEmpty()) {
                            hints.put(id, pendingName);
                        }
                        lastSeen = id;
                        pendingName = null;
                    }
                }
            }
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[otters_civ_revived] Failed to read jobs store at {}", filePath, e);
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
                w.write("# Project OOGA jobs ledger");
                w.newLine();
                w.write("# <uuid>.active=<job> and <uuid>.xp.<job>=<amount> are authoritative.");
                w.newLine();
                w.write("# `# Name: ...` above an id group is a plain-text operator hint.");
                w.newLine();
                for (Map.Entry<UUID, JobState> e : sorted.entrySet()) {
                    UUID id = e.getKey();
                    JobState s = e.getValue();
                    String hint = sanitizeHintForStorage(displayHints.get(id));
                    if (!hint.isEmpty()) {
                        w.write("# Name: ");
                        w.write(hint);
                        w.newLine();
                    }
                    Job active = s.active();
                    w.write(id.toString());
                    w.write(".active=");
                    w.write(active == null ? "" : active.slug());
                    w.newLine();
                    for (Map.Entry<Job, Long> xp : s.snapshotXp().entrySet()) {
                        if (xp.getValue() <= 0L) continue;
                        w.write(id.toString());
                        w.write(".xp.");
                        w.write(xp.getKey().slug());
                        w.write('=');
                        w.write(Long.toString(xp.getValue()));
                        w.newLine();
                    }
                }
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
}
