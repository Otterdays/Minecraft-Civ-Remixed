package com.fpsmod.client.jobs;

import com.fpsmod.jobs.net.JobStatusPayload;
import org.jetbrains.annotations.Nullable;

/** Latest server-pushed snapshot of the local player's job status. Read by the HUD overlay. */
public final class JobsClientState {
    @Nullable
    private static volatile JobStatusPayload latest;

    private JobsClientState() {}

    public static void update(@Nullable JobStatusPayload payload) {
        latest = payload;
    }

    @Nullable
    public static JobStatusPayload latest() {
        return latest;
    }

    public static boolean hasActiveJob() {
        JobStatusPayload p = latest;
        return p != null && !p.slug().isEmpty();
    }
}
