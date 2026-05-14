package com.fpsmod.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic jobs runtime: arbitrary config-defined jobs, string-keyed player state, and independent
 * gameplay-event XP tracking.
 */
public class JobsService implements IJobsService {
    private static final Logger LOGGER = LoggerFactory.getLogger("project_ooga/jobs");
    private final JobsStore store;
    private final Map<UUID, JobState> states;
    private final Map<UUID, String> displayHints;
    private final Map<String, Long> lastTriggerEventMs = new ConcurrentHashMap<>();
    private volatile JobsConfig config;
    private volatile CompiledJobCatalog catalog;

    /** Optional callback fired after any per-player state mutation so the HUD can sync. */
    private java.util.function.Consumer<ServerPlayer> statusListener = p -> {};

    public void setStatusListener(java.util.function.Consumer<ServerPlayer> listener) {
        this.statusListener = listener == null ? p -> {} : listener;
    }

    public JobsService(JobsStore store, JobsConfig config) {
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
        this.config.sanitize();
        this.catalog = CompiledJobCatalog.compile(this.config);
        JobsLedger loaded = store.load();
        this.states = new ConcurrentHashMap<>(loaded.states());
        this.displayHints = new ConcurrentHashMap<>(loaded.displayHints());
        pruneStatesToCatalog();
    }

    public CompiledJobCatalog catalog() {
        return catalog;
    }

    public List<Job> jobs() {
        return new ArrayList<>(catalog.jobs());
    }

    public List<Job> visibleJobs() {
        return catalog.visibleJobs();
    }

    public Job jobById(String rawJobId) {
        return catalog.jobById(rawJobId);
    }

    public List<String> validationMessages() {
        return new ArrayList<>(catalog.diagnostics());
    }

    public JobsConfig config() {
        return config;
    }

    public boolean jobsEnabled() {
        return catalog.enabled();
    }

    public int maxActiveJobs() {
        return catalog.maxActiveJobs();
    }

    public String defaultIconGlyph(Job job) {
        if (job != null && job.iconGlyph != null && !job.iconGlyph.isBlank()) {
            return job.iconGlyph;
        }
        return config.global.defaultIconGlyph;
    }

    public String defaultIconKey(Job job) {
        if (job != null && job.iconKey != null && !job.iconKey.isBlank()) {
            return job.iconKey;
        }
        return config.global.defaultIconKey;
    }

    public static JobsService createDefault() {
        return new JobsService(new FileJobsStore(), JobsConfigLoader.loadOrCreate());
    }

    public JobState stateOf(UUID id) {
        return states.computeIfAbsent(id, k -> new JobState());
    }

    public void refresh() {
        this.config = JobsConfigLoader.loadOrCreate();
        this.catalog = CompiledJobCatalog.compile(this.config);
        pruneStatesToCatalog();
        for (String line : validationMessages()) {
            LOGGER.warn("[otters_civ_revived/jobs] {}", line);
        }
    }

    public void rememberPlayerName(UUID id, @Nullable String name) {
        String hint = FileJobsStore.sanitizeHintForStorage(name);
        if (!hint.isEmpty()) {
            displayHints.put(id, hint);
        }
    }

    public synchronized boolean joinJob(UUID id, String rawJobId, @Nullable ServerPlayer player) {
        if (!jobsEnabled()) {
            return false;
        }
        Job job = jobById(rawJobId);
        if (job == null || !job.canJoin()) {
            return false;
        }
        JobState state = stateOf(id);
        boolean changed = state.activate(job.id, maxActiveJobs());
        if (!changed) {
            return false;
        }
        persist();
        if (player != null) {
            statusListener.accept(player);
        }
        return true;
    }

    public synchronized boolean leaveJob(UUID id, @Nullable String rawJobId, @Nullable ServerPlayer player) {
        JobState state = stateOf(id);
        boolean changed = state.deactivate(rawJobId);
        if (!changed) {
            return false;
        }
        persist();
        if (player != null) {
            statusListener.accept(player);
        }
        return true;
    }

    private synchronized void persist() {
        store.save(Map.copyOf(states), Map.copyOf(displayHints));
    }

    public long modifyPayout(ServerPlayer player, JobEventContext context, long basePayout) {
        if (player == null || context == null || basePayout <= 0L || !jobsEnabled()) {
            return basePayout;
        }
        JobState state = states.get(player.getUUID());
        if (state == null || state.activeJobIds().isEmpty()) {
            return basePayout;
        }
        double multiplier = 1.0D;
        long flatBonus = 0L;
        boolean matched = false;
        for (CompiledJobCatalog.Match match : matchingActiveJobs(state, context)) {
            Job job = match.job();
            int level = state.levelOf(job.id, job.progression);
            multiplier *= job.boosts.moneyMultiplierForLevel(level);
            flatBonus += job.boosts.moneyFlatBonusForLevel(level);
            matched = true;
        }
        if (!matched) {
            return basePayout;
        }
        long scaled = (long) Math.floor(basePayout * multiplier) + flatBonus;
        return Math.max(0L, scaled);
    }

    public void onGameplayEvent(ServerPlayer player, JobEventContext context) {
        if (player == null || context == null || !jobsEnabled()) {
            return;
        }
        rememberPlayerName(player.getUUID(), player.getName().getString());
        JobState state = stateOf(player.getUUID());
        if (state.activeJobIds().isEmpty()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        boolean changed = false;
        for (CompiledJobCatalog.Match match : matchingActiveJobs(state, context)) {
            if (!pastCooldown(player.getUUID(), match.trigger().key(), match.trigger().cooldownMs())) {
                continue;
            }
            Job job = match.job();
            int prevLevel = state.levelOf(job.id, job.progression);
            long xpAward = resolvedXpAward(job, prevLevel);
            if (xpAward <= 0L) {
                continue;
            }
            long newXp = state.addXp(job.id, xpAward);
            int newLevel = job.progression.levelForXp(newXp);
            lines.add(progressMessageText(job, newLevel, newXp, xpAward));
            if (newLevel > prevLevel) {
                lines.add("[" + job.labelForUi() + "] level up → " + newLevel);
            }
            changed = true;
        }
        if (!changed) {
            return;
        }
        persist();
        statusListener.accept(player);
        for (String line : lines) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(line));
        }
    }

    public JobCatalogSnapshot catalogSnapshot() {
        JobCatalogSnapshot snapshot = new JobCatalogSnapshot();
        snapshot.enabled = jobsEnabled();
        snapshot.activationPolicy = config.global.activationPolicy;
        snapshot.maxActiveJobs = maxActiveJobs();
        for (Job job : jobs()) {
            JobCatalogSnapshot.JobDescriptor descriptor = new JobCatalogSnapshot.JobDescriptor();
            descriptor.id = job.id;
            descriptor.displayName = job.displayName;
            descriptor.shortLabel = job.labelForUi();
            descriptor.description = job.description;
            descriptor.iconGlyph = defaultIconGlyph(job);
            descriptor.iconKey = defaultIconKey(job);
            descriptor.enabled = job.enabled;
            descriptor.joinable = job.joinable;
            descriptor.hidden = job.hidden;
            descriptor.sortOrder = job.sortOrder;
            descriptor.maxLevel = job.progression.maxLevel;
            descriptor.firstLevelXp = job.progression.xpForLevel(1);
            for (JobTrigger trigger : job.triggers) {
                if (trigger != null) {
                    descriptor.triggerEventTypes.add(trigger.parsedEventType().id());
                }
            }
            snapshot.jobs.add(descriptor);
        }
        return snapshot;
    }

    public JobStatusSnapshotData statusSnapshot(UUID playerId) {
        JobState state = stateOf(playerId);
        JobStatusSnapshotData snapshot = new JobStatusSnapshotData();
        snapshot.activeJobIds.addAll(state.activeJobIds());
        for (Job job : jobs()) {
            long xp = state.getXp(job.id);
            int level = state.levelOf(job.id, job.progression);
            long floor = job.progression.xpForLevel(level);
            long ceil = job.progression.isMaxLevel(level)
                ? job.progression.xpForLevel(job.progression.maxLevel)
                : job.progression.xpForLevel(level + 1);
            JobStatusSnapshotData.JobProgressEntry entry = new JobStatusSnapshotData.JobProgressEntry();
            entry.jobId = job.id;
            entry.displayName = job.displayName;
            entry.shortLabel = job.labelForUi();
            entry.iconGlyph = defaultIconGlyph(job);
            entry.iconKey = defaultIconKey(job);
            entry.level = level;
            entry.xp = xp;
            entry.xpForLevel = floor;
            entry.xpForNextLevel = ceil;
            entry.maxLevel = job.progression.maxLevel;
            entry.active = state.isActive(job.id);
            snapshot.progress.add(entry);
        }
        return snapshot;
    }

    public String joinFailureReason(String rawJobId, UUID playerId) {
        if (!jobsEnabled()) {
            return "Jobs are disabled in jobs.json.";
        }
        Job job = jobById(rawJobId);
        if (job == null) {
            return "Unknown job '" + rawJobId + "'.";
        }
        if (!job.enabled) {
            return "Job '" + job.id + "' is disabled.";
        }
        if (!job.joinable) {
            return "Job '" + job.id + "' is not joinable right now.";
        }
        JobState state = stateOf(playerId);
        if (state.isActive(job.id)) {
            return "Already on " + job.id + ".";
        }
        if ("multi".equals(config.global.activationPolicy)
            && state.activeJobIds().size() >= maxActiveJobs()) {
            return "Active job slots full (" + maxActiveJobs() + "). Leave one first.";
        }
        return "";
    }

    private void pruneStatesToCatalog() {
        Map<String, Job> byId = new LinkedHashMap<>();
        for (Job job : jobs()) {
            byId.put(job.id, job);
        }
        for (JobState state : states.values()) {
            List<String> validActive = new ArrayList<>();
            for (String active : state.activeJobIds()) {
                Job job = byId.get(active);
                if (job != null && job.enabled) {
                    validActive.add(active);
                }
            }
            state.setActiveJobs(validActive, maxActiveJobs());
        }
    }

    private long resolvedXpAward(Job job, int level) {
        double multiplier = job.boosts.xpMultiplierForLevel(level);
        long flatBonus = job.boosts.xpFlatBonusForLevel(level);
        long scaled = (long) Math.floor(job.progression.xpPerEvent * multiplier) + flatBonus;
        return Math.max(0L, scaled);
    }

    private List<CompiledJobCatalog.Match> matchingActiveJobs(JobState state, JobEventContext context) {
        List<CompiledJobCatalog.Match> matches = new ArrayList<>();
        for (String activeJobId : state.activeJobIds()) {
            CompiledJobCatalog.Match match = catalog.matchedJob(activeJobId, context);
            if (match != null && match.job().enabled) {
                matches.add(match);
            }
        }
        return matches;
    }

    static String progressMessageText(Job job, int level, long totalXp, long xpAward) {
        if (job == null) {
            return "";
        }
        if (job.progression.isMaxLevel(level)) {
            return "[" + job.labelForUi() + "] +" + xpAward + " xp · MAX";
        }
        long floor = job.progression.xpForLevel(level);
        long ceil = job.progression.xpForLevel(level + 1);
        long inLevel = Math.max(0L, totalXp - floor);
        long range = Math.max(1L, ceil - floor);
        return "[" + job.labelForUi() + "] +" + xpAward + " xp · Lvl " + level + " · " + inLevel + "/" + range;
    }

    private boolean pastCooldown(UUID playerId, String triggerKey, long cooldownMs) {
        if (cooldownMs <= 0L) {
            return true;
        }
        String key = playerId + "|" + triggerKey;
        long now = System.currentTimeMillis();
        Long prev = lastTriggerEventMs.get(key);
        if (prev != null && now - prev < cooldownMs) {
            return false;
        }
        lastTriggerEventMs.put(key, now);
        return true;
    }

}
