package com.fpsmod.client.jobs;

import com.fpsmod.jobs.JobStatusSnapshotData;
import org.jetbrains.annotations.Nullable;

/** Latest server-pushed snapshot of the local player's job status. Read by the HUD overlay. */
public final class JobsClientState {
    @Nullable
    private static volatile JobStatusSnapshotData latest;

    private JobsClientState() {}

    public static void update(@Nullable JobStatusSnapshotData payload) {
        latest = payload;
    }

    @Nullable
    public static JobStatusSnapshotData latest() {
        return latest;
    }

    public static boolean hasActiveJob() {
        JobStatusSnapshotData p = latest;
        return p != null && p.activeJobIds != null && !p.activeJobIds.isEmpty();
    }

    @Nullable
    public static JobStatusSnapshotData.JobProgressEntry primaryActiveJob() {
        JobStatusSnapshotData snapshot = latest;
        if (snapshot == null || snapshot.activeJobIds == null || snapshot.activeJobIds.isEmpty()) {
            return null;
        }
        String primaryId = snapshot.activeJobIds.get(snapshot.activeJobIds.size() - 1);
        if (snapshot.progress == null) {
            return null;
        }
        for (JobStatusSnapshotData.JobProgressEntry entry : snapshot.progress) {
            if (entry != null && primaryId.equals(entry.jobId)) {
                return entry;
            }
        }
        return null;
    }
}
