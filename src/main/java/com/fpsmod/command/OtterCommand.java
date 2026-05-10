package com.fpsmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Lists Otters Civ. Revived / fpsmod commands (helps players and ops discover the surface). */
public final class OtterCommand {
    private OtterCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("otter").executes(ctx -> runHelp(ctx.getSource())));
    }

    private static int runHelp(CommandSourceStack source) {
        sendLine(source, "Otters Civ. Revived (technical mod id: fpsmod)");
        sendLine(source, "--- Commands ---");
        sendLine(source, "/otter — show this list");
        sendLine(source, "/money — show your wallet balance");
        sendLine(source, "/money set <player> <amount> — set balance (gamemaster / OP-band only; roadmap: custom permission nodes)");
        sendLine(source, "--- Passive rewards ---");
        sendLine(source, "Wallets: config/otters_civ_revived/wallet.properties (# Name: hints + UUID=balance; auto-migrates legacy fpsmod folder once)");
        sendLine(source, "Mining + combat payouts use config/otters_civ_revived/rewards.json");
        sendLine(source, "(tags, per-block/per-mob maps blockRewards/entityRewards, flat blockReward/entityReward fallback, cooldowns)");
        return 1;
    }

    private static void sendLine(CommandSourceStack source, String text) {
        Component line = Component.literal(text);
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(line);
        } else {
            source.sendSuccess(() -> line, false);
        }
    }
}
