package com.fpsmod.command;

import com.fpsmod.economy.EconomyConfig;
import com.fpsmod.guilds.GuildConfig;
import com.fpsmod.guilds.GuildService;
import com.fpsmod.jobs.Job;
import com.fpsmod.jobs.JobsService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
final class GuideBookFactory {
    private static final String TITLE = "Otters Civ. Guide";
    private static final String AUTHOR = "Project OOGA";

    private GuideBookFactory() {
    }

    static ItemStack create(EconomyConfig economyConfig, JobsService jobsService, GuildService guildService) {
        List<Filterable<Component>> pages = List.of(
            page(coverPage()),
            page(quickStartPage()),
            page(economyPage(economyConfig)),
            page(jobsPage(jobsService)),
            page(guildsPage(guildService.config(), economyConfig)),
            page(operatorPage())
        );

        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(Filterable.passThrough(TITLE), AUTHOR, 0, pages, true)
        );
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static Filterable<Component> page(Component page) {
        return Filterable.passThrough(page);
    }

    private static Component coverPage() {
        return Component.empty()
            .append(heading("OTTERS CIV.\n"))
            .append(accent("REVIVED\n\n"))
            .append(text("Wallets, jobs, guilds, rewards,\nand quick start help.\n\n"))
            .append(text("Use "))
            .append(command("/guide"))
            .append(text(" any time for a fresh copy."));
    }

    private static Component quickStartPage() {
        return Component.empty()
            .append(heading("START HERE\n\n"))
            .append(text("- "))
            .append(command("/otter"))
            .append(text(" shows civ help; modded clients open the hub UI.\n"))
            .append(text("- "))
            .append(command("/money"))
            .append(text(" checks your wallet.\n"))
            .append(text("- "))
            .append(command("/pay <player> <amount>"))
            .append(text(" sends coins.\n"))
            .append(text("- "))
            .append(command("/job list"))
            .append(text(" shows live jobs.\n"))
            .append(text("- "))
            .append(command("/guild list"))
            .append(text(" shows guilds.\n\n"))
            .append(text("The server/host remembers the state even in singleplayer."));
    }

    private static Component economyPage(EconomyConfig economyConfig) {
        return Component.empty()
            .append(heading("ECONOMY\n\n"))
            .append(text("Starting balance: "))
            .append(value(formatStartingBalance(economyConfig)))
            .append(text("\n"))
            .append(text("/pay cap: "))
            .append(value(formatAmountOrOff(economyConfig, economyConfig.maxTransferPerCommand)))
            .append(text("\n"))
            .append(text("/pay cooldown: "))
            .append(value(formatSecondsOrOff(economyConfig.transferCooldownSeconds)))
            .append(text("\n"))
            .append(text("/pay fee: "))
            .append(value(formatAmountOrOff(economyConfig, economyConfig.transferFlatFee)))
            .append(text("\n\n"))
            .append(text("Mining payout blocks and rewarded entities can also pay coins.\n"))
            .append(text("Admins can audit the ledger with "))
            .append(command("/economy log"))
            .append(text("."));
    }

    private static Component jobsPage(JobsService jobsService) {
        return Component.empty()
            .append(heading("JOBS\n\n"))
            .append(text("Catalog loaded: "))
            .append(value(String.valueOf(jobsService.jobs().size())))
            .append(text(" job(s)\n"))
            .append(text("Activation: "))
            .append(value(String.valueOf(jobsService.config().global.activationPolicy)))
            .append(text(" · max active "))
            .append(value(String.valueOf(jobsService.maxActiveJobs())))
            .append(text("\n"))
            .append(text("Roster: "))
            .append(value(summarizeJobs(jobsService.jobs())))
            .append(text("\n\n"))
            .append(command("/job join <id>"))
            .append(text(" to activate one.\n"))
            .append(command("/job info <id>"))
            .append(text(" shows triggers and boosts.\n"))
            .append(text("Modded clients also get the jobs HUD above vanilla XP."));
    }

    private static Component guildsPage(GuildConfig guildConfig, EconomyConfig economyConfig) {
        return Component.empty()
            .append(heading("GUILDS\n\n"))
            .append(text("Create cost: "))
            .append(value(economyConfig.format(guildConfig.creationCost)))
            .append(text("\n"))
            .append(text("Claim cost: "))
            .append(value(economyConfig.format(guildConfig.claimCost)))
            .append(text(" per chunk\n"))
            .append(text("Max claims: "))
            .append(value(String.valueOf(guildConfig.maxClaims)))
            .append(text(" · max members: "))
            .append(value(String.valueOf(guildConfig.maxMembers)))
            .append(text("\n\n"))
            .append(command("/guild invite <player>"))
            .append(text(" handles invites.\n"))
            .append(command("/guild claim"))
            .append(text(" and "))
            .append(command("/guild map"))
            .append(text(" manage land.\n"))
            .append(command("/guild sethome"))
            .append(text(" / "))
            .append(command("/guild home"))
            .append(text(" handle home TP."));
    }

    private static Component operatorPage() {
        return Component.empty()
            .append(heading("FILES + OPS\n\n"))
            .append(text("Main folder:\nconfig/otters_civ_revived/\n\n"))
            .append(text("economy.json = money rules\n"))
            .append(text("rewards.json = payouts\n"))
            .append(text("jobs.json = live jobs catalog\n"))
            .append(text("guilds.json = guild tuning\n"))
            .append(text("project_ooga.db = runtime state\n\n"))
            .append(text("Admins can give this book with "))
            .append(command("/guide give <player>"))
            .append(text("."));
    }

    private static String formatStartingBalance(EconomyConfig economyConfig) {
        long amount = Math.max(0L, economyConfig.newPlayerStartingBalance);
        return amount <= 0L ? "none" : economyConfig.format(amount);
    }

    private static String formatAmountOrOff(EconomyConfig economyConfig, long amount) {
        return amount <= 0L ? "off" : economyConfig.format(amount);
    }

    private static String formatSecondsOrOff(int seconds) {
        return seconds <= 0 ? "off" : seconds + "s";
    }

    private static String summarizeJobs(List<Job> jobs) {
        if (jobs.isEmpty()) {
            return "(none)";
        }
        List<String> ids = new ArrayList<>();
        int limit = Math.min(5, jobs.size());
        for (int i = 0; i < limit; i++) {
            ids.add(jobs.get(i).id);
        }
        if (jobs.size() > limit) {
            ids.add("...");
        }
        return String.join(", ", ids);
    }

    private static MutableComponent heading(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static MutableComponent accent(String text) {
        return Component.literal(text).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    private static MutableComponent text(String text) {
        return Component.literal(text).withStyle(ChatFormatting.BLACK);
    }

    private static MutableComponent command(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
    }

    private static MutableComponent value(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);
    }
}
