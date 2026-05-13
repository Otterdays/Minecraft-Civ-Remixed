package com.fpsmod.command;

import com.fpsmod.jobs.Job;
import com.fpsmod.jobs.JobState;
import com.fpsmod.jobs.JobsConfig;
import com.fpsmod.jobs.JobsService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

/** /job · /job list · /job join <name> · /job leave · /job stats */
public final class JobCommand {
    private JobCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, JobsService jobs) {
        SuggestionProvider<CommandSourceStack> jobSlugs = (ctx, b) -> {
            for (Job j : Job.values()) b.suggest(j.slug());
            return b.buildFuture();
        };

        dispatcher.register(
            Commands.literal("job")
                .executes(ctx -> runStats(ctx.getSource(), jobs))
                .then(Commands.literal("list").executes(ctx -> runList(ctx.getSource())))
                .then(Commands.literal("stats").executes(ctx -> runStats(ctx.getSource(), jobs)))
                .then(Commands.literal("leave").executes(ctx -> runLeave(ctx.getSource(), jobs)))
                .then(
                    Commands.literal("join")
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .suggests(jobSlugs)
                                .executes(ctx ->
                                    runJoin(ctx.getSource(), jobs, StringArgumentType.getString(ctx, "name"))
                                )
                        )
                )
        );
    }

    private static int runList(CommandSourceStack source) {
        send(source, "Jobs:");
        for (Job j : Job.values()) {
            send(source, "  " + j.slug() + " — tag " + j.tagId() + " (" + j.kind().name().toLowerCase(Locale.ROOT) + ")");
        }
        send(source, "Use /job join <name> to pick one. Only one active at a time.");
        return 1;
    }

    private static int runJoin(CommandSourceStack source, JobsService jobs, String rawName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "/job join requires a player source.");
            return 0;
        }
        Optional<Job> picked = Job.bySlug(rawName);
        if (picked.isEmpty()) {
            send(source, "Unknown job '" + rawName + "'. Try one of: miner, lumberjack, farmer, fighter.");
            return 0;
        }
        boolean changed = jobs.joinJob(player.getUUID(), picked.get(), player);
        if (changed) {
            send(source, "Active job: " + picked.get().slug() + ". XP retained per job across switches.");
        } else {
            send(source, "Already on " + picked.get().slug() + ".");
        }
        return 1;
    }

    private static int runLeave(CommandSourceStack source, JobsService jobs) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "/job leave requires a player source.");
            return 0;
        }
        boolean changed = jobs.leaveJob(player.getUUID(), player);
        send(source, changed ? "Job cleared. No multiplier on payouts." : "No active job.");
        return 1;
    }

    private static int runStats(CommandSourceStack source, JobsService jobs) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "/job stats requires a player source.");
            return 0;
        }
        JobState state = jobs.stateOf(player.getUUID());
        Job active = state.active();
        send(source, "Active: " + (active == null ? "(none)" : active.slug()));
        for (Job j : Job.values()) {
            long xp = state.getXp(j);
            int lvl = state.levelOf(j);
            long next = JobsConfig.xpForLevel(Math.min(JobsConfig.MAX_LEVEL, lvl + 1));
            double mult = JobsConfig.multiplierForLevel(lvl);
            send(source, String.format(Locale.ROOT,
                "  %s: lvl %d (%d / %d xp) — payout ×%.2f%s",
                j.slug(), lvl, xp, next, mult, active == j ? "  [active]" : ""));
        }
        return 1;
    }

    private static void send(CommandSourceStack source, String text) {
        Component line = Component.literal(text);
        if (source.getEntity() instanceof ServerPlayer p) {
            p.sendSystemMessage(line);
        } else {
            source.sendSuccess(() -> line, false);
        }
    }
}
