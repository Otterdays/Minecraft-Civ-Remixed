package com.fpsmod.jobs.net;

import com.fpsmod.OogaMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import io.netty.buffer.ByteBuf;

/**
 * Server → client: current active job + cumulative XP + cached level/next-level thresholds so the
 * client HUD doesn't need to know the XP curve. Sent on login, on /job join|leave, and after each
 * matching reward event (post-XP).
 */
public record JobStatusPayload(String slug, int level, long xp, long xpForLevel, long xpForNextLevel)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JobStatusPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "job_status"));

    public static final StreamCodec<ByteBuf, JobStatusPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, JobStatusPayload::slug,
        ByteBufCodecs.VAR_INT,     JobStatusPayload::level,
        ByteBufCodecs.VAR_LONG,    JobStatusPayload::xp,
        ByteBufCodecs.VAR_LONG,    JobStatusPayload::xpForLevel,
        ByteBufCodecs.VAR_LONG,    JobStatusPayload::xpForNextLevel,
        JobStatusPayload::new
    );

    /** Slug used when the player has no active job. Client HUD hides itself on this value. */
    public static final String NO_ACTIVE = "";

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
