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
        sendLine(source, "Otters Civ. Revived (technical mod id: project_ooga)");
        sendLine(source, "--- Commands ---");
        sendLine(source, "/otter — show this list");
        sendLine(source, "/guide — spawn the in-game Otters Civ. handbook");
        sendLine(source, "/guide give <player> — admin give the handbook to a player (gamemaster-only)");
        sendLine(source, "/money — show your wallet balance");
        sendLine(source, "/pay <player> <amount> — send coins to another player (atomic; cap/cooldown/fee from economy.json)");
        sendLine(source, "/money set|add|take <player> <amount> — admin balance ops (gamemaster / OP-band only)");
        sendLine(source, "All balance changes are logged to config/otters_civ_revived/project_ooga.db (wallet_ledger table).");
        sendLine(source, "/economy reload — re-read economy.json (gamemaster-only)");
        sendLine(source, "/economy log [count] — show recent ledger entries (gamemaster-only)");
        sendLine(source, "/economy log player <player> [count] — filter ledger by player (gamemaster-only)");
        sendLine(source, "/job — show your jobs/levels   /job list   /job join <id>   /job leave [id]");
        sendLine(source, "/job info <id>   /job reload   /job validate");
        sendLine(source, "Jobs are server-authoritative from config/otters_civ_revived/jobs.json with configurable triggers, progression, boosts, and active-slot rules.");
        sendLine(source, "Starter pack ships miner / lumberjack / farmer / excavator / fighter in single-slot mode by default.");
        sendLine(source, "/guild — guild / faction system");
        sendLine(source, "/guild create <name> — create a guild ($250)");
        sendLine(source, "/guild disband — disband your guild");
        sendLine(source, "/guild invite <player> — invite a player");
        sendLine(source, "/guild join — accept pending invite");
        sendLine(source, "/guild leave — leave your guild");
        sendLine(source, "/guild kick <player> — kick a member");
        sendLine(source, "/guild transfer <player> — transfer ownership to another member");
        sendLine(source, "/guild promote|demote <player> — officer management");
        sendLine(source, "/guild claim — claim the chunk you stand in ($100)");
        sendLine(source, "/guild unclaim — unclaim current chunk");
        sendLine(source, "/guild unclaimall — unclaim all your guild's chunks");
        sendLine(source, "/guild map — ASCII chunk claim map");
        sendLine(source, "/guild sethome — set guild teleport point");
        sendLine(source, "/guild home — teleport to guild home");
        sendLine(source, "/guild open | /guild close — toggle public joining (owner-only)");
        sendLine(source, "/guild info — your guild details");
        sendLine(source, "/guild list — all guilds");
        sendLine(source, "/ooga db status|migrate — SQLite database management (admin)");
        sendLine(source, "--- Passive rewards ---");
        sendLine(source, "Economy/guild/jobs state: config/otters_civ_revived/project_ooga.db (SQLite, WAL mode).");
        sendLine(source, "Economy policy: config/otters_civ_revived/economy.json");
        sendLine(source, "Mining + combat payouts: config/otters_civ_revived/rewards.json (tags + cooldowns + …)");
        sendLine(source, "Jobs catalog: config/otters_civ_revived/jobs.json   Runtime jobs state: config/otters_civ_revived/project_ooga.db");
        sendLine(source, "Guild tuning: config/otters_civ_revived/guilds.json");
        sendLine(source, "block_values.json / entity_values.json list every tagged block/rewarded entity with default payouts;");
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
