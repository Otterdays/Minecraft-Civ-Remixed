package com.fpsmod.guilds.net;

import com.fpsmod.OogaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MapTogglePayload(boolean show) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MapTogglePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "map_toggle"));

    public static final StreamCodec<ByteBuf, MapTogglePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, MapTogglePayload::show,
        MapTogglePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
