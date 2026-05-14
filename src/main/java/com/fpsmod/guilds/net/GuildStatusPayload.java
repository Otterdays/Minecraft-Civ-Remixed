package com.fpsmod.guilds.net;

import com.fpsmod.OogaMod;
import com.fpsmod.guilds.Guild;
import com.fpsmod.guilds.GuildService;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record GuildStatusPayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final CustomPacketPayload.Type<GuildStatusPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "guild_status"));

    public static final StreamCodec<ByteBuf, GuildStatusPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, GuildStatusPayload::json,
        GuildStatusPayload::new
    );

    public static GuildStatusPayload forPlayer(ServerPlayer player, GuildService guilds) {
        Guild g = guilds.guildByPlayer(player.getUUID());
        if (g == null) return new GuildStatusPayload("");
        ClientGuildInfo info = new ClientGuildInfo();
        info.name = g.name;
        info.description = g.description;
        info.owner = g.owner.toString();
        info.role = g.roleOf(player.getUUID());
        info.memberCount = g.memberCount();
        info.maxMembers = guilds.config().maxMembers;
        info.claimCount = guilds.claimsForGuild(g.id).size();
        info.maxClaims = guilds.config().maxClaims;
        info.balance = g.balance;
        info.hasHome = g.homePos != null;
        info.open = g.open;
        return new GuildStatusPayload(GSON.toJson(info));
    }

    public ClientGuildInfo info() {
        if (json == null || json.isEmpty()) return null;
        return GSON.fromJson(json, ClientGuildInfo.class);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class ClientGuildInfo {
        public String name;
        public String description;
        public String owner;
        public String role;
        public int memberCount;
        public int maxMembers;
        public int claimCount;
        public int maxClaims;
        public long balance;
        public boolean hasHome;
        public boolean open;
    }
}
