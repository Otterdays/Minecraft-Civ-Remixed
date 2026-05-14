package com.fpsmod.jobs;

import java.util.ArrayList;
import java.util.List;

public final class JobStatusSnapshotData {
    public List<String> activeJobIds = new ArrayList<>();
    public List<JobProgressEntry> progress = new ArrayList<>();

    public static final class JobProgressEntry {
        public String jobId = "";
        public String displayName = "";
        public String shortLabel = "";
        public String iconGlyph = "";
        public String iconKey = "";
        public int level = 0;
        public long xp = 0L;
        public long xpForLevel = 0L;
        public long xpForNextLevel = 0L;
        public int maxLevel = 1;
        public boolean active = false;
    }
}
