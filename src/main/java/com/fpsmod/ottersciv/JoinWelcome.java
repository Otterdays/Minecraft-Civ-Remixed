package com.fpsmod.ottersciv;

import com.fpsmod.economy.EconomyConfig;
import com.fpsmod.economy.TransactionReason;
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
            EconomyConfig economyConfig = wallets.economyConfig();

            SavedDataStorage storage = server.overworld().getDataStorage();
            JoinAttendanceSavedData attendance =
                storage.computeIfAbsent(JoinAttendanceSavedData.TYPE);

            boolean returning = attendance.hasSeenBefore(player.getUUID());
            long startingBalanceGranted = 0L;
            if (!returning) {
                long configuredStart = Math.max(0L, economyConfig.newPlayerStartingBalance);
                if (configuredStart > 0L && wallets.getBalance(player.getUUID()) <= 0L) {
                    long previousBalance = wallets.getBalance(player.getUUID());
                    long nextBalance = wallets.addBalance(
                        player.getUUID(),
                        configuredStart,
                        player.getName().getString(),
                        TransactionReason.JOIN_STARTING_BALANCE
                    );
                    startingBalanceGranted = Math.max(0L, nextBalance - previousBalance);
                }
                attendance.markSeen(player.getUUID());
            }

            if (!economyConfig.showJoinWelcome) {
                return;
            }
            if (returning) {
                sendReturningWelcome(player);
            } else {
                sendFirstTimeWelcome(player, economyConfig, startingBalanceGranted);
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
            Component.literal("Otters Civ. Revived — /guide for the handbook, /otter for help, /money for your purse.")
                .withStyle(ChatFormatting.GRAY)
        );
    }

    private static void sendFirstTimeWelcome(
        ServerPlayer player,
        EconomyConfig economyConfig,
        long startingBalanceGranted
    ) {
        String firstLine = "Welcome — Otters Civ. Revived is active on this server.";
        if (startingBalanceGranted > 0L) {
            firstLine += " You received " + economyConfig.format(startingBalanceGranted) + " to get started.";
        }
        player.sendSystemMessage(Component.literal(firstLine));
        player.sendSystemMessage(
            Component.literal("Try /guide for the handbook, /otter for commands, /money for your balance.")
        );
        player.sendSystemMessage(
            Component.literal(
                "Earn money from mining payout blocks and killing rewarded entities "
                    + "(config/otters_civ_revived: rewards.json; optional block_values.json / entity_values.json)."
            )
        );
    }
}
