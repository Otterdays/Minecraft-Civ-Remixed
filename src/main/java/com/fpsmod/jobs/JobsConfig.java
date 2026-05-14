package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Server-authoritative jobs schema loaded from {@code config/otters_civ_revived/jobs.json}.
 */
public final class JobsConfig {
    public GlobalSettings global = new GlobalSettings();
    public List<Job> jobs = defaultJobs();

    public static JobsConfig defaults() {
        JobsConfig config = new JobsConfig();
        config.sanitize();
        return config;
    }

    public void sanitize() {
        if (global == null) {
            global = new GlobalSettings();
        }
        global.sanitize();
        if (jobs == null || jobs.isEmpty()) {
            jobs = defaultJobs();
        }
        List<Job> sanitized = new ArrayList<>();
        Map<String, Job> seen = new LinkedHashMap<>();
        for (Job raw : jobs) {
            if (raw == null) {
                continue;
            }
            raw.sanitize(global);
            if (raw.id.isEmpty() || seen.containsKey(raw.id)) {
                continue;
            }
            seen.put(raw.id, raw);
        }
        sanitized.addAll(seen.values());
        sanitized.sort(Comparator.comparingInt((Job job) -> job.sortOrder).thenComparing(job -> job.id));
        jobs = sanitized;
    }

    public List<Job> visibleJobs() {
        List<Job> out = new ArrayList<>();
        for (Job job : jobs) {
            if (job != null && job.visibleInUi()) {
                out.add(job);
            }
        }
        return out;
    }

    public Job jobById(String raw) {
        String normalized = Job.normalizeId(raw);
        for (Job job : jobs) {
            if (job != null && job.id.equals(normalized)) {
                return job;
            }
        }
        return null;
    }

    public int effectiveMaxActiveJobs() {
        if ("multi".equals(global.activationPolicy)) {
            return Math.max(1, global.maxActiveJobs);
        }
        return 1;
    }

    public static List<Job> defaultJobs() {
        List<Job> defaults = new ArrayList<>();
        defaults.add(defaultJob(
            "miner",
            "Miner",
            "⛏",
            "Mine rewarded stone and ore blocks.",
            JobEventType.BLOCK_BREAK,
            List.of("otters_civ_revived:job/miner_blocks")
        ));
        defaults.add(defaultJob(
            "lumberjack",
            "Lumberjack",
            "▲",
            "Chop rewarded wood and tree blocks.",
            JobEventType.BLOCK_BREAK,
            List.of("otters_civ_revived:job/lumberjack_blocks")
        ));
        defaults.add(defaultJob(
            "farmer",
            "Farmer",
            "✿",
            "Harvest rewarded crop and farm blocks.",
            JobEventType.BLOCK_BREAK,
            List.of("otters_civ_revived:job/farmer_blocks")
        ));
        defaults.add(defaultJob(
            "fighter",
            "Fighter",
            "⚔",
            "Defeat rewarded hostile mobs.",
            JobEventType.MOB_KILL,
            List.of("otters_civ_revived:job/fighter_mobs")
        ));
        return defaults;
    }

    private static Job defaultJob(
        String id,
        String displayName,
        String iconGlyph,
        String description,
        JobEventType eventType,
        List<String> tagIds
    ) {
        Job job = new Job();
        job.id = id;
        job.displayName = displayName;
        job.shortLabel = displayName;
        job.description = description;
        job.iconGlyph = iconGlyph;
        JobTrigger trigger = new JobTrigger();
        trigger.eventType = eventType.id();
        trigger.tagIds = new ArrayList<>(tagIds);
        job.triggers.add(trigger);
        return job;
    }

    public static final class GlobalSettings {
        public boolean enabled = true;
        public String activationPolicy = "single";
        public int maxActiveJobs = 1;
        public String defaultIconGlyph = "*";
        public String defaultIconKey = "generic";
        public JobProgression defaultProgression = JobProgression.defaults();
        public JobBoosts defaultBoosts = JobBoosts.defaults();

        public void sanitize() {
            String normalizedPolicy = activationPolicy == null
                ? "single"
                : activationPolicy.trim().toLowerCase(Locale.ROOT);
            activationPolicy = "multi".equals(normalizedPolicy) ? "multi" : "single";
            if (maxActiveJobs < 1) {
                maxActiveJobs = 1;
            }
            defaultIconGlyph = defaultIconGlyph == null || defaultIconGlyph.isBlank()
                ? "*"
                : defaultIconGlyph.trim();
            defaultIconKey = Job.normalizeId(defaultIconKey);
            if (defaultProgression == null) {
                defaultProgression = JobProgression.defaults();
            }
            defaultProgression.sanitize(null);
            if (defaultBoosts == null) {
                defaultBoosts = JobBoosts.defaults();
            }
            defaultBoosts.sanitize(null);
        }
    }
}
