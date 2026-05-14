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
            "Mine rewarded stone, deepslate, ore, and hard-material blocks.",
            10,
            JobEventType.BLOCK_BREAK,
            List.of("otters_civ_revived:job/miner_blocks"),
            starterProgression(5L),
            minerBoosts()
        ));
        defaults.add(defaultJob(
            "lumberjack",
            "Lumberjack",
            "▲",
            "Chop rewarded logs, leaves, planks, and bamboo blocks.",
            20,
            JobEventType.BLOCK_BREAK,
            List.of("otters_civ_revived:job/lumberjack_blocks"),
            starterProgression(5L),
            lumberjackBoosts()
        ));
        defaults.add(defaultJob(
            "farmer",
            "Farmer",
            "✿",
            "Harvest rewarded farm outputs and organic blocks.",
            30,
            JobEventType.BLOCK_BREAK,
            List.of("otters_civ_revived:job/farmer_blocks"),
            starterProgression(6L),
            farmerBoosts()
        ));
        defaults.add(defaultJob(
            "excavator",
            "Excavator",
            "▤",
            "Dig rewarded soil, sand, gravel, clay, mud, and cold-terrain blocks.",
            40,
            JobEventType.BLOCK_BREAK,
            List.of("otters_civ_revived:job/excavator_blocks"),
            starterProgression(4L),
            excavatorBoosts()
        ));
        defaults.add(defaultJob(
            "fighter",
            "Fighter",
            "⚔",
            "Defeat rewarded hostile mobs.",
            50,
            JobEventType.MOB_KILL,
            List.of("otters_civ_revived:job/fighter_mobs"),
            starterProgression(6L),
            fighterBoosts()
        ));
        return defaults;
    }

    private static Job defaultJob(
        String id,
        String displayName,
        String iconGlyph,
        String description,
        int sortOrder,
        JobEventType eventType,
        List<String> tagIds,
        JobProgression progression,
        JobBoosts boosts
    ) {
        Job job = new Job();
        job.id = id;
        job.displayName = displayName;
        job.shortLabel = displayName;
        job.description = description;
        job.sortOrder = sortOrder;
        job.iconGlyph = iconGlyph;
        job.progression = progression;
        job.boosts = boosts;
        JobTrigger trigger = new JobTrigger();
        trigger.eventType = eventType.id();
        trigger.tagIds = new ArrayList<>(tagIds);
        trigger.requireEconomyReward = true;
        job.triggers.add(trigger);
        return job;
    }

    private static JobProgression starterProgression(long xpPerEvent) {
        JobProgression progression = JobProgression.defaults();
        progression.xpPerEvent = xpPerEvent;
        return progression;
    }

    private static JobBoosts minerBoosts() {
        return starterBoosts(
            List.of(doubleLevel(10, 1.03D), doubleLevel(20, 1.06D), doubleLevel(30, 1.10D), doubleLevel(40, 1.14D)),
            List.of(longLevel(25, 1L), longLevel(40, 2L)),
            List.of(doubleLevel(10, 1.02D), doubleLevel(20, 1.05D), doubleLevel(30, 1.08D), doubleLevel(40, 1.10D)),
            List.of(longLevel(30, 1L))
        );
    }

    private static JobBoosts lumberjackBoosts() {
        return starterBoosts(
            List.of(doubleLevel(10, 1.02D), doubleLevel(20, 1.05D), doubleLevel(30, 1.08D), doubleLevel(40, 1.10D)),
            List.of(longLevel(35, 1L)),
            List.of(doubleLevel(5, 1.04D), doubleLevel(10, 1.08D), doubleLevel(20, 1.12D), doubleLevel(30, 1.14D), doubleLevel(40, 1.16D)),
            List.of(longLevel(20, 1L))
        );
    }

    private static JobBoosts farmerBoosts() {
        return starterBoosts(
            List.of(doubleLevel(10, 1.02D), doubleLevel(20, 1.04D), doubleLevel(30, 1.06D), doubleLevel(40, 1.08D)),
            List.of(longLevel(35, 1L)),
            List.of(doubleLevel(5, 1.05D), doubleLevel(10, 1.10D), doubleLevel(20, 1.14D), doubleLevel(30, 1.17D), doubleLevel(40, 1.20D)),
            List.of(longLevel(18, 1L))
        );
    }

    private static JobBoosts excavatorBoosts() {
        return starterBoosts(
            List.of(doubleLevel(10, 1.03D), doubleLevel(20, 1.06D), doubleLevel(30, 1.08D), doubleLevel(40, 1.10D)),
            List.of(longLevel(22, 1L)),
            List.of(doubleLevel(10, 1.03D), doubleLevel(20, 1.06D), doubleLevel(30, 1.09D), doubleLevel(40, 1.12D)),
            List.of(longLevel(28, 1L))
        );
    }

    private static JobBoosts fighterBoosts() {
        return starterBoosts(
            List.of(doubleLevel(10, 1.04D), doubleLevel(20, 1.08D), doubleLevel(30, 1.12D), doubleLevel(40, 1.16D)),
            List.of(longLevel(20, 1L), longLevel(40, 2L)),
            List.of(doubleLevel(10, 1.02D), doubleLevel(20, 1.05D), doubleLevel(30, 1.08D), doubleLevel(40, 1.10D)),
            List.of(longLevel(25, 1L))
        );
    }

    private static JobBoosts starterBoosts(
        List<JobLevelDouble> moneyMultiplierByLevel,
        List<JobLevelLong> moneyFlatBonusByLevel,
        List<JobLevelDouble> xpMultiplierByLevel,
        List<JobLevelLong> xpFlatBonusByLevel
    ) {
        JobBoosts boosts = JobBoosts.defaults();
        boosts.moneyMultiplierByLevel = new ArrayList<>(moneyMultiplierByLevel);
        boosts.moneyFlatBonusByLevel = new ArrayList<>(moneyFlatBonusByLevel);
        boosts.xpMultiplierByLevel = new ArrayList<>(xpMultiplierByLevel);
        boosts.xpFlatBonusByLevel = new ArrayList<>(xpFlatBonusByLevel);
        return boosts;
    }

    private static JobLevelDouble doubleLevel(int level, double value) {
        JobLevelDouble entry = new JobLevelDouble();
        entry.level = level;
        entry.value = value;
        return entry;
    }

    private static JobLevelLong longLevel(int level, long value) {
        JobLevelLong entry = new JobLevelLong();
        entry.level = level;
        entry.value = value;
        return entry;
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
