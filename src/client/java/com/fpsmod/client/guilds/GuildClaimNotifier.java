package com.fpsmod.client.guilds;

import com.fpsmod.guilds.ClaimedChunk;
import com.fpsmod.guilds.net.GuildStatusPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Action-bar hints when the player crosses into a chunk claimed by a guild (theirs or another's).
 */
@SuppressWarnings("null")
public final class GuildClaimNotifier {
    private static Integer lastChunkX;
    private static Integer lastChunkZ;
    private static String lastDim;

    private GuildClaimNotifier() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }
            String dim = client.level.dimension().identifier().toString();
            int cx = client.player.blockPosition().getX() >> 4;
            int cz = client.player.blockPosition().getZ() >> 4;

            if (lastChunkX == null) {
                lastChunkX = cx;
                lastChunkZ = cz;
                lastDim = dim;
                return;
            }

            if (lastChunkX == cx && lastChunkZ == cz && Objects.equals(lastDim, dim)) {
                return;
            }

            if (!Objects.equals(lastDim, dim)) {
                lastDim = dim;
                lastChunkX = cx;
                lastChunkZ = cz;
                return;
            }

            ClaimedChunk oldClaim = GuildClientState.claimAt(dim, lastChunkX, lastChunkZ);
            ClaimedChunk newClaim = GuildClientState.claimAt(dim, cx, cz);

            lastChunkX = cx;
            lastChunkZ = cz;

            if (newClaim == null) {
                return;
            }
            if (oldClaim != null && oldClaim.guildId().equals(newClaim.guildId())) {
                return;
            }

            UUID myGuild = myGuildId();
            boolean mine = myGuild != null && newClaim.guildId().equals(myGuild);
            String guildLabel = newClaim.guildDisplayName() != null && !newClaim.guildDisplayName().isBlank()
                ? newClaim.guildDisplayName()
                : "Unknown guild";

            Component msg = mine
                ? Component.literal("Entering your guild's claim: " + guildLabel + ".")
                : Component.literal("Entering claimed land: " + guildLabel + ".");
            client.gui.setOverlayMessage(msg, false);
        });
    }

    private static UUID myGuildId() {
        GuildStatusPayload.ClientGuildInfo info = GuildClientState.guildInfo();
        if (info == null || info.guildId == null || info.guildId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(info.guildId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
