package com.fpsmod.client.jobs;

import com.fpsmod.jobs.JobCatalogSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Latest server-pushed jobs catalog metadata. */
public final class JobsClientCatalog {
    @Nullable
    private static volatile JobCatalogSnapshot latest;

    private JobsClientCatalog() {}

    public static void update(@Nullable JobCatalogSnapshot snapshot) {
        latest = snapshot;
    }

    @Nullable
    public static JobCatalogSnapshot latest() {
        return latest;
    }

    public static List<JobCatalogSnapshot.JobDescriptor> visibleJobs() {
        JobCatalogSnapshot snapshot = latest;
        if (snapshot == null || snapshot.jobs == null) {
            return List.of();
        }
        List<JobCatalogSnapshot.JobDescriptor> out = new ArrayList<>();
        for (JobCatalogSnapshot.JobDescriptor job : snapshot.jobs) {
            if (job != null && !job.hidden) {
                out.add(job);
            }
        }
        return out;
    }

    @Nullable
    public static JobCatalogSnapshot.JobDescriptor jobById(String rawJobId) {
        JobCatalogSnapshot snapshot = latest;
        if (snapshot == null || snapshot.jobs == null || rawJobId == null) {
            return null;
        }
        for (JobCatalogSnapshot.JobDescriptor job : snapshot.jobs) {
            if (job != null && rawJobId.equals(job.id)) {
                return job;
            }
        }
        return null;
    }
}
