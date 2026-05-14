package com.fpsmod.ottersciv.config;

import com.fpsmod.jobs.Job;
import com.fpsmod.jobs.JobEventType;
import com.fpsmod.jobs.JobsService;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JobRewardDiagnostics {
    private JobRewardDiagnostics() {}

    public static List<String> diagnostics(JobsService jobs, MinecraftServer server) {
        RewardRules rewards = RewardRulesLoader.inspectEffectiveRewardsForRunningServer(server);
        Set<String> rewardableBlocks = rewardableIds(rewards.blockRewards);
        Set<String> rewardableEntities = rewardableIds(rewards.entityRewards);
        List<String> diagnostics = new ArrayList<>();

        for (Job job : jobs.jobs()) {
            for (var trigger : job.triggers) {
                if (trigger.ids == null || trigger.ids.isEmpty()) continue;
                Set<String> rewardable = trigger.parsedEventType() == JobEventType.BLOCK_BREAK
                    ? rewardableBlocks
                    : rewardableEntities;
                int covered = intersectionCount(trigger.ids, rewardable);
                if (covered == trigger.ids.size()) continue;
                String eventLabel = trigger.parsedEventType() == JobEventType.BLOCK_BREAK ? "block" : "mob";
                Set<String> uncovered = new LinkedHashSet<>(trigger.ids);
                uncovered.removeAll(rewardable);
                if (covered == 0) {
                    diagnostics.add(
                        "Job '" + job.id + "' has 0/" + trigger.ids.size() + " rewardable "
                            + eventLabel + " targets with the current rewards surface."
                            + " These are not in any reward tag: " + truncateList(uncovered, 5));
                } else {
                    diagnostics.add(
                        "Job '" + job.id + "' — " + covered + "/" + trigger.ids.size() + " "
                            + eventLabel + " targets match the rewards surface."
                            + " Unmatched: " + truncateList(uncovered, 3));
                }
            }
        }
        return diagnostics;
    }

    private static Set<String> rewardableIds(java.util.Map<String, Long> map) {
        return map == null ? Set.of() : map.keySet();
    }

    private static int intersectionCount(Set<String> a, Set<String> b) {
        int count = 0;
        for (String s : a) {
            if (b.contains(s)) count++;
        }
        return count;
    }

    private static int intersectionCount(List<String> a, Set<String> b) {
        return intersectionCount(new LinkedHashSet<>(a), b);
    }

    private static String truncateList(Set<String> items, int limit) {
        List<String> list = new ArrayList<>(items);
        if (list.size() <= limit) return String.join(", ", list);
        return String.join(", ", list.subList(0, limit)) + " … (+" + (list.size() - limit) + " more)";
    }
}
