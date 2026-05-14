package com.fpsmod.guilds.net;

import com.fpsmod.OogaMod;
import com.fpsmod.guilds.ClaimedChunk;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record ClaimsPayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final CustomPacketPayload.Type<ClaimsPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "claims"));

    public static final StreamCodec<ByteBuf, ClaimsPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ClaimsPayload::json,
        ClaimsPayload::new
    );

    public static ClaimsPayload fromClaims(Set<ClaimedChunk> claims) {
        List<ClaimedChunk> list = new ArrayList<>(claims);
        return new ClaimsPayload(GSON.toJson(list));
    }

    public List<ClaimedChunk> claims() {
        Type type = new TypeToken<List<ClaimedChunk>>() {}.getType();
        List<ClaimedChunk> parsed = GSON.fromJson(json, type);
        return parsed == null ? List.of() : parsed;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
