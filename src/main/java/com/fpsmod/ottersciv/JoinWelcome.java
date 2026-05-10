package com.fpsmod.ottersciv;

import com.fpsmod.economy.WalletService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.SavedDataStorage;

/** One-shot chat lines when a player finishes joining—points them at Otters Civ. commands and economy. */
public final class JoinWelcome {
    private JoinWelcome() {
    }

    /**
     * Registers join messages and refreshes {@code wallet.properties} name hints for the joining player
     * (when disk should show a human-readable label next to their UUID).
     */
    public static void register(WalletService wallets) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            wallets.touchPlayerLabelForOps(player.getUUID(), player.getName().getString());

            SavedDataStorage storage = server.overworld().getDataStorage();
            JoinAttendanceSavedData attendance =
                storage.computeIfAbsent(JoinAttendanceSavedData.TYPE);

            boolean returning = attendance.hasSeenBefore(player.getUUID());
            if (returning) {
                sendReturningWelcome(player);
            } else {
                sendFirstTimeWelcome(player);
                attendance.markSeen(player.getUUID());
            }
        });
    }

    private static void sendReturningWelcome(ServerPlayer player) {
        var nameGlow = player.getName().plainCopy().withStyle(ChatFormatting.AQUA);
        player.sendSystemMessage(
            Component.literal("Welcome back, ~")
                .withStyle(ChatFormatting.GOLD)
                .append(nameGlow)
                .append(Component.literal(".").withStyle(ChatFormatting.GOLD))
        );
        player.sendSystemMessage(
            Component.literal("Otters Civ. Revived — /otter for help, /money for your purse.")
                .withStyle(ChatFormatting.GRAY)
        );
    }

    private static void sendFirstTimeWelcome(ServerPlayer player) {
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
