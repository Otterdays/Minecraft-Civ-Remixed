package com.fpsmod.jobs.net;

import com.fpsmod.OogaMod;
import com.fpsmod.jobs.JobCatalogSnapshot;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record JobCatalogPayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final CustomPacketPayload.Type<JobCatalogPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "job_catalog"));

    public static final StreamCodec<ByteBuf, JobCatalogPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        JobCatalogPayload::json,
        JobCatalogPayload::new
    );

    public static JobCatalogPayload fromSnapshot(JobCatalogSnapshot snapshot) {
        return new JobCatalogPayload(GSON.toJson(snapshot));
    }

    public JobCatalogSnapshot snapshot() {
        JobCatalogSnapshot parsed = GSON.fromJson(json, JobCatalogSnapshot.class);
        return parsed == null ? new JobCatalogSnapshot() : parsed;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
