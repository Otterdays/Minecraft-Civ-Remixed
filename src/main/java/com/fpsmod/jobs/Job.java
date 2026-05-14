package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fully data-driven job definition loaded from {@code jobs.json}.
 */
public final class Job {
    public String id = "";
    public String displayName = "";
    public String shortLabel = "";
    public String description = "";
    public boolean enabled = true;
    public boolean joinable = true;
    public boolean hidden = false;
    public int sortOrder = 0;
    public String iconGlyph = "";
    public String iconKey = "";
    public List<JobTrigger> triggers = new ArrayList<>();
    public JobProgression progression = new JobProgression();
    public JobBoosts boosts = new JobBoosts();

    public void sanitize(JobsConfig.GlobalSettings global) {
        id = normalizeId(id);
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = humanizeId(id);
        } else {
            displayName = displayName.trim();
        }
        shortLabel = shortLabel == null ? "" : shortLabel.trim();
        description = description == null ? "" : description.trim();
        iconGlyph = iconGlyph == null ? "" : iconGlyph.trim();
        iconKey = normalizeId(iconKey);
        if (sortOrder < 0) {
            sortOrder = 0;
        }
        if (triggers == null) {
            triggers = new ArrayList<>();
        }
        for (JobTrigger trigger : triggers) {
            if (trigger != null) {
                trigger.sanitize();
            }
        }
        if (progression == null) {
            progression = new JobProgression();
        }
        progression.sanitize(global == null ? null : global.defaultProgression);
        if (boosts == null) {
            boosts = new JobBoosts();
        }
        boosts.sanitize(global == null ? null : global.defaultBoosts);
    }

    public boolean visibleInUi() {
        return enabled && !hidden;
    }

    public boolean canJoin() {
        return enabled && joinable;
    }

    public String labelForUi() {
        return shortLabel == null || shortLabel.isEmpty() ? displayName : shortLabel;
    }

    public static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String humanizeId(String raw) {
        String normalized = normalizeId(raw);
        if (normalized.isEmpty()) {
            return "(unnamed job)";
        }
        String[] words = normalized.replace(':', ' ').replace('-', ' ').replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1));
            }
        }
        return out.isEmpty() ? normalized : out.toString();
    }
}
