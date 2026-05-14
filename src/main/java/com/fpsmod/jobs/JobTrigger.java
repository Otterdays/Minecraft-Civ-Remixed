package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Declarative event matcher for one job.
 */
public final class JobTrigger {
    public boolean enabled = true;
    public String eventType = JobEventType.BLOCK_BREAK.id();
    public List<String> tagIds = new ArrayList<>();
    public List<String> ids = new ArrayList<>();
    public List<String> dimensionAllowlist = new ArrayList<>();
    public List<String> dimensionBlacklist = new ArrayList<>();
    public List<String> requiredMainHandItemIds = new ArrayList<>();
    public List<String> requiredMainHandItemTags = new ArrayList<>();
    public long cooldownMs = 0L;
    public boolean requireEconomyReward = false;
    public boolean directPlayerKillOnly = true;

    public void sanitize() {
        eventType = JobEventType.byId(eventType).orElse(JobEventType.BLOCK_BREAK).id();
        tagIds = normalizeDistinct(tagIds);
        ids = normalizeDistinct(ids);
        dimensionAllowlist = normalizeDistinct(dimensionAllowlist);
        dimensionBlacklist = normalizeDistinct(dimensionBlacklist);
        requiredMainHandItemIds = normalizeDistinct(requiredMainHandItemIds);
        requiredMainHandItemTags = normalizeDistinct(requiredMainHandItemTags);
        if (cooldownMs < 0L) {
            cooldownMs = 0L;
        }
    }

    public JobEventType parsedEventType() {
        return JobEventType.byId(eventType).orElse(JobEventType.BLOCK_BREAK);
    }

    private static List<String> normalizeDistinct(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }
}
