package com.fpsmod.jobs.net;

import com.fpsmod.OogaMod;
import com.fpsmod.jobs.JobStatusSnapshotData;
import com.google.gson.Gson;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import io.netty.buffer.ByteBuf;

/**
 * Server → client local-player jobs status snapshot.
 */
public record JobStatusPayload(String json)
    implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final CustomPacketPayload.Type<JobStatusPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "job_status"));

    public static final StreamCodec<ByteBuf, JobStatusPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, JobStatusPayload::json,
        JobStatusPayload::new
    );

    public static JobStatusPayload fromSnapshot(JobStatusSnapshotData snapshot) {
        return new JobStatusPayload(GSON.toJson(snapshot));
    }

    public JobStatusSnapshotData snapshot() {
        JobStatusSnapshotData parsed = GSON.fromJson(json, JobStatusSnapshotData.class);
        return parsed == null ? new JobStatusSnapshotData() : parsed;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
