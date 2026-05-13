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
        sendLine(source, "/job — show your jobs/levels   /job list   /job join <name>   /job leave");
        sendLine(source, "Jobs (MVP): miner · lumberjack · farmer · fighter. One active slot. XP from matching events. Payout ×1.0→×2.0 at lvl 0→50.");
        sendLine(source, "--- Passive rewards ---");
        sendLine(source, "Wallets: config/otters_civ_revived/wallet.properties (# Name: hints + UUID=balance; auto-migrates legacy fpsmod folder once)");
        sendLine(source, "Mining + combat payouts: config/otters_civ_revived/rewards.json (tags + cooldowns + …)");
        sendLine(source, "block_values.json / entity_values.json list every tagged block/hostile mob with default payouts;");
        sendLine(source, "filled from blockTag & entityTag on SERVER_STARTED + END_DATA_PACK_RELOAD when files have no ids yet.");
        sendLine(source, "--- Adding custom blocks/mobs ---");
        sendLine(source, "1) Edit block_values.json / entity_values.json: add \"namespace:id\": amount lines. /reload.");
        sendLine(source, "2) Or inline in rewards.json under blockRewards / entityRewards. Sibling files override on overlap.");
        sendLine(source, "3) Or ship a server datapack extending otters_civ_revived:currency_blocks / currency_mobs");
        sendLine(source, "   at data/otters_civ_revived/tags/block/ or tags/entity_type/ (singular dirs — MC 1.21+).");
        sendLine(source, "Precedence: sibling file > rewards.json inline > tag membership flat reward.");
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
