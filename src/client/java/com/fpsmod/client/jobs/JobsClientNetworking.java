package com.fpsmod.client.jobs;

import com.fpsmod.jobs.net.JobCatalogPayload;
import com.fpsmod.jobs.net.JobStatusPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-side receiver for server-to-client job status packets (type registered once on the server side). */
public final class JobsClientNetworking {
    private JobsClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(JobCatalogPayload.TYPE, (payload, context) -> {
            JobsClientCatalog.update(payload.snapshot());
        });
        ClientPlayNetworking.registerGlobalReceiver(JobStatusPayload.TYPE, (payload, context) -> {
            JobsClientState.update(payload.snapshot());
        });
    }
}
