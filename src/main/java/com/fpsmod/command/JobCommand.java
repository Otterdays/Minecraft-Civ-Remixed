package com.fpsmod.command;

import com.fpsmod.jobs.Job;
import com.fpsmod.jobs.JobsService;
import com.fpsmod.jobs.net.JobsNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;
import java.util.Locale;

/** `/job` surface for fully configurable jobs. */
public final class JobCommand {
    private static final Permission ADMIN = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private JobCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, JobsService jobs) {
        SuggestionProvider<CommandSourceStack> jobSlugs = (ctx, b) -> {
            for (Job job : jobs.jobs()) {
                b.suggest(job.id);
            }
            return b.buildFuture();
        };

        dispatcher.register(
            Commands.literal("job")
                .executes(ctx -> runStats(ctx.getSource(), jobs))
                .then(Commands.literal("list").executes(ctx -> runList(ctx.getSource(), jobs)))
                .then(Commands.literal("stats").executes(ctx -> runStats(ctx.getSource(), jobs)))
                .then(
                    Commands.literal("leave")
                        .executes(ctx -> runLeave(ctx.getSource(), jobs, null))
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .suggests(jobSlugs)
                                .executes(ctx ->
                                    runLeave(ctx.getSource(), jobs, StringArgumentType.getString(ctx, "name"))
                                )
                        )
                )
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
                .then(
                    Commands.literal("info")
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .suggests(jobSlugs)
                                .executes(ctx ->
                                    runInfo(ctx.getSource(), jobs, StringArgumentType.getString(ctx, "name"))
                                )
                        )
                )
                .then(
                    Commands.literal("reload")
                        .requires(source -> source.permissions().hasPermission(ADMIN))
                        .executes(ctx -> runReload(ctx.getSource(), jobs))
                )
                .then(
                    Commands.literal("validate")
                        .requires(source -> source.permissions().hasPermission(ADMIN))
                        .executes(ctx -> runValidate(ctx.getSource(), jobs))
                )
        );
    }

    private static int runList(CommandSourceStack source, JobsService jobs) {
        if (!jobs.jobsEnabled()) {
            send(source, "Jobs disabled in jobs.json.");
            return 0;
        }
        send(source, "Jobs (" + jobs.jobs().size() + " total, policy: "
            + jobs.config().global.activationPolicy + ", active slots: " + jobs.maxActiveJobs() + "):");
        for (Job job : jobs.jobs()) {
            String triggerSummary = summarizeTriggers(job);
            send(source,
                "  " + job.id + " — " + job.displayName
                    + " [" + (job.enabled ? (job.joinable ? "joinable" : "read-only") : "disabled") + "]"
                    + " · " + triggerSummary
                    + " · lvl cap " + job.progression.maxLevel
                    + " · +" + job.progression.xpPerEvent + " xp/event"
            );
        }
        send(source, "Use /job join <id> to activate. /job info <id> shows full detail.");
        return 1;
    }

    private static int runJoin(CommandSourceStack source, JobsService jobs, String rawName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "/job join requires a player source.");
            return 0;
        }
        String failure = jobs.joinFailureReason(rawName, player.getUUID());
        if (!failure.isEmpty()) {
            send(source, failure);
            return 0;
        }
        jobs.joinJob(player.getUUID(), rawName, player);
        var status = jobs.statusSnapshot(player.getUUID());
        send(source, "Active jobs: " + joinIds(status.activeJobIds) + ".");
        return 1;
    }

    private static int runLeave(CommandSourceStack source, JobsService jobs, String rawName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "/job leave requires a player source.");
            return 0;
        }
        boolean changed = jobs.leaveJob(player.getUUID(), rawName, player);
        if (!changed) {
            send(source, rawName == null || rawName.isBlank() ? "No active jobs." : "Job '" + rawName + "' not active.");
            return 0;
        }
        var status = jobs.statusSnapshot(player.getUUID());
        send(source, status.activeJobIds.isEmpty() ? "All active jobs cleared." : "Active jobs: " + joinIds(status.activeJobIds) + ".");
        return 1;
    }

    private static int runStats(CommandSourceStack source, JobsService jobs) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "/job stats requires a player source.");
            return 0;
        }
        var status = jobs.statusSnapshot(player.getUUID());
        send(source, "Active jobs: " + joinIds(status.activeJobIds) + ".");
        for (var entry : status.progress) {
            Job job = jobs.jobById(entry.jobId);
            double moneyMult = job == null ? 1.0D : job.boosts.moneyMultiplierForLevel(entry.level);
            long moneyFlat = job == null ? 0L : job.boosts.moneyFlatBonusForLevel(entry.level);
            double xpMult = job == null ? 1.0D : job.boosts.xpMultiplierForLevel(entry.level);
            long xpFlat = job == null ? 0L : job.boosts.xpFlatBonusForLevel(entry.level);
            send(source, String.format(Locale.ROOT,
                "  %s: lvl %d (%d / %d xp) — money ×%.2f +%d · xp ×%.2f +%d%s",
                entry.jobId,
                entry.level,
                entry.xp,
                entry.xpForNextLevel,
                moneyMult,
                moneyFlat,
                xpMult,
                xpFlat,
                entry.active ? "  [active]" : ""
            ));
        }
        return 1;
    }

    private static int runInfo(CommandSourceStack source, JobsService jobs, String rawName) {
        Job job = jobs.jobById(rawName);
        if (job == null) {
            send(source, "Unknown job '" + rawName + "'.");
            return 0;
        }
        send(source, job.displayName + " (" + job.id + ")");
        send(source, "  enabled=" + job.enabled + " joinable=" + job.joinable + " hidden=" + job.hidden);
        send(source, "  icon=" + jobs.defaultIconGlyph(job) + " / " + jobs.defaultIconKey(job));
        send(source, "  progression: maxLevel=" + job.progression.maxLevel
            + " xpPerEvent=" + job.progression.xpPerEvent
            + " thresholds=" + (job.progression.levelThresholds.isEmpty() ? "curve" : "table"));
        send(source, String.format(Locale.ROOT,
            "  boosts: money ×%.2f +%d · xp ×%.2f +%d",
            job.boosts.moneyMultiplier,
            job.boosts.moneyFlatBonus,
            job.boosts.xpMultiplier,
            job.boosts.xpFlatBonus
        ));
        for (int i = 0; i < job.triggers.size(); i++) {
            var trigger = job.triggers.get(i);
            send(source,
                "  trigger#" + i
                    + ": " + trigger.parsedEventType().id()
                    + " tags=" + trigger.tagIds
                    + " ids=" + trigger.ids
                    + " cooldownMs=" + trigger.cooldownMs
                    + " requireEconomyReward=" + trigger.requireEconomyReward
            );
        }
        return 1;
    }

    private static int runReload(CommandSourceStack source, JobsService jobs) {
        jobs.refresh(source.getServer());
        JobsNetworking.broadcastCatalogAndStatuses(jobs, source.getServer());
        send(source, "Reloaded jobs.json and rebuilt catalog.");
        return 1;
    }

    private static int runValidate(CommandSourceStack source, JobsService jobs) {
        jobs.refresh(source.getServer());
        if (jobs.validationMessages().isEmpty()) {
            send(source, "Jobs validation OK. No diagnostics.");
            return 1;
        }
        send(source, "Jobs validation diagnostics:");
        for (String line : jobs.validationMessages()) {
            send(source, "  - " + line);
        }
        return 1;
    }

    private static String summarizeTriggers(Job job) {
        List<String> parts = new java.util.ArrayList<>();
        for (var trigger : job.triggers) {
            parts.add(trigger.parsedEventType().id());
        }
        return String.join(", ", parts);
    }

    private static String joinIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "(none)";
        }
        return String.join(", ", ids);
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
