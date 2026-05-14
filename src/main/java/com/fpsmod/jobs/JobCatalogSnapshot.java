package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.List;

public final class JobCatalogSnapshot {
    public boolean enabled;
    public String activationPolicy = "single";
    public int maxActiveJobs = 1;
    public List<JobDescriptor> jobs = new ArrayList<>();

    public static final class JobDescriptor {
        public String id = "";
        public String displayName = "";
        public String shortLabel = "";
        public String description = "";
        public String iconGlyph = "";
        public String iconKey = "";
        public boolean enabled = true;
        public boolean joinable = true;
        public boolean hidden = false;
        public int sortOrder = 0;
        public int maxLevel = 1;
        public long firstLevelXp = 0L;
        public List<String> triggerEventTypes = new ArrayList<>();
    }
}
