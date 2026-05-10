package com.fpsmod.ottersciv;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** One-shot chat lines when a player finishes joining—points them at Otters Civ. commands and economy. */
public final class JoinWelcome {
    private JoinWelcome() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendWelcome(handler.player));
    }

    private static void sendWelcome(ServerPlayer player) {
        player.sendSystemMessage(
            Component.literal("Welcome — Otters Civ. Revived is active on this server.")
        );
        player.sendSystemMessage(
            Component.literal("Try /otter for commands, /money for your balance.")
        );
        player.sendSystemMessage(
            Component.literal(
                "Earn money from mining payout blocks and killing hostile mobs (see rewards.json on the server)."
            )
        );
    }
}
