package com.fpsmod.economy.net;

import com.fpsmod.OogaMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WalletBalancePayload(long balance) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WalletBalancePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "wallet_balance"));

    public static final StreamCodec<ByteBuf, WalletBalancePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, WalletBalancePayload::balance,
        WalletBalancePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
