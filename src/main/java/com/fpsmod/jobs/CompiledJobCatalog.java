package com.fpsmod.jobs;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validated, tag-expanded runtime jobs catalog.
 */
public final class CompiledJobCatalog {
    private final JobsConfig config;
    private final List<CompiledJob> jobs;
    private final Map<String, CompiledJob> jobsById;
    private final List<String> diagnostics;

    private CompiledJobCatalog(
        JobsConfig config,
        List<CompiledJob> jobs,
        Map<String, CompiledJob> jobsById,
        List<String> diagnostics
    ) {
        this.config = config;
        this.jobs = jobs;
        this.jobsById = jobsById;
        this.diagnostics = diagnostics;
    }

    public static CompiledJobCatalog compile(JobsConfig config) {
        JobsConfig effective = config == null ? JobsConfig.defaults() : config;
        effective.sanitize();
        List<String> diagnostics = new ArrayList<>();
        List<CompiledJob> jobs = new ArrayList<>();
        Map<String, CompiledJob> jobsById = new LinkedHashMap<>();
        for (Job job : effective.jobs) {
            CompiledJob compiled = CompiledJob.compile(job, effective.global, diagnostics);
            jobs.add(compiled);
            jobsById.put(job.id, compiled);
        }
        return new CompiledJobCatalog(effective, List.copyOf(jobs), Map.copyOf(jobsById), List.copyOf(diagnostics));
    }

    public JobsConfig config() {
        return config;
    }

    public Collection<Job> jobs() {
        List<Job> out = new ArrayList<>();
        for (CompiledJob compiled : jobs) {
            out.add(compiled.job());
        }
        return out;
    }

    public Job jobById(String id) {
        CompiledJob compiled = jobsById.get(Job.normalizeId(id));
        return compiled == null ? null : compiled.job();
    }

    public List<Job> visibleJobs() {
        List<Job> out = new ArrayList<>();
        for (CompiledJob compiled : jobs) {
            if (compiled.job().visibleInUi()) {
                out.add(compiled.job());
            }
        }
        return out;
    }

    public List<String> diagnostics() {
        return diagnostics;
    }

    public int maxActiveJobs() {
        return config.effectiveMaxActiveJobs();
    }

    public boolean enabled() {
        return config.global.enabled;
    }

    public Match matchedJob(String jobId, JobEventContext context) {
        CompiledJob compiled = jobsById.get(Job.normalizeId(jobId));
        if (compiled == null || context == null) {
            return null;
        }
        CompiledTrigger trigger = compiled.firstMatchingTrigger(context);
        return trigger == null ? null : new Match(compiled.job(), trigger);
    }

    List<CompiledJob> compiledJobs() {
        return jobs;
    }

    public record Match(Job job, CompiledTrigger trigger) {
    }

    public static final class CompiledJob {
        private final Job job;
        private final List<CompiledTrigger> triggers;

        private CompiledJob(Job job, List<CompiledTrigger> triggers) {
            this.job = job;
            this.triggers = triggers;
        }

        static CompiledJob compile(Job job, JobsConfig.GlobalSettings global, List<String> diagnostics) {
            List<CompiledTrigger> triggers = new ArrayList<>();
            for (int i = 0; i < job.triggers.size(); i++) {
                JobTrigger trigger = job.triggers.get(i);
                if (trigger == null) {
                    continue;
                }
                triggers.add(CompiledTrigger.compile(job, trigger, i, global, diagnostics));
            }
            if (triggers.isEmpty()) {
                diagnostics.add("Job '" + job.id + "' has no valid triggers and will never match.");
            }
            return new CompiledJob(job, List.copyOf(triggers));
        }

        public Job job() {
            return job;
        }

        public CompiledTrigger firstMatchingTrigger(JobEventContext context) {
            for (CompiledTrigger trigger : triggers) {
                if (trigger.matches(context)) {
                    return trigger;
                }
            }
            return null;
        }

        List<CompiledTrigger> triggers() {
            return triggers;
        }
    }

    public static final class CompiledTrigger {
        private final String key;
        private final JobTrigger source;
        private final JobEventType eventType;
        private final Set<String> targetIds;
        private final Set<String> dimensionAllowlist;
        private final Set<String> dimensionBlacklist;
        private final Set<String> requiredMainHandIds;

        private CompiledTrigger(
            String key,
            JobTrigger source,
            JobEventType eventType,
            Set<String> targetIds,
            Set<String> dimensionAllowlist,
            Set<String> dimensionBlacklist,
            Set<String> requiredMainHandIds
        ) {
            this.key = key;
            this.source = source;
            this.eventType = eventType;
            this.targetIds = targetIds;
            this.dimensionAllowlist = dimensionAllowlist;
            this.dimensionBlacklist = dimensionBlacklist;
            this.requiredMainHandIds = requiredMainHandIds;
        }

        static CompiledTrigger compile(
            Job job,
            JobTrigger trigger,
            int index,
            JobsConfig.GlobalSettings global,
            List<String> diagnostics
        ) {
            JobEventType eventType = trigger.parsedEventType();
            Set<String> targetIds = new LinkedHashSet<>(normalizeIds(trigger.ids, diagnostics, job.id, "ids"));
            if (eventType == JobEventType.BLOCK_BREAK) {
                targetIds.addAll(resolveBlockTags(trigger.tagIds, diagnostics, job.id));
            } else {
                targetIds.addAll(resolveEntityTags(trigger.tagIds, diagnostics, job.id));
            }
            Set<String> requiredItems = new LinkedHashSet<>(normalizeIds(
                trigger.requiredMainHandItemIds,
                diagnostics,
                job.id,
                "requiredMainHandItemIds"
            ));
            requiredItems.addAll(resolveItemTags(trigger.requiredMainHandItemTags, diagnostics, job.id));
            if (targetIds.isEmpty()) {
                diagnostics.add("Job '" + job.id + "' trigger #" + index + " has no resolved ids/tags.");
            }
            return new CompiledTrigger(
                job.id + "#" + index,
                trigger,
                eventType,
                Set.copyOf(targetIds),
                Set.copyOf(new LinkedHashSet<>(trigger.dimensionAllowlist)),
                Set.copyOf(new LinkedHashSet<>(trigger.dimensionBlacklist)),
                Set.copyOf(requiredItems)
            );
        }

        public String key() {
            return key;
        }

        public long cooldownMs() {
            return source.cooldownMs;
        }

        public boolean requireEconomyReward() {
            return source.requireEconomyReward;
        }

        public JobEventType eventType() {
            return eventType;
        }

        public Set<String> targetIds() {
            return targetIds;
        }

        public boolean matches(JobEventContext context) {
            if (!source.enabled || context == null || eventType != context.eventType()) {
                return false;
            }
            if (source.requireEconomyReward && !context.economyRewarded()) {
                return false;
            }
            if (!targetIds.contains(context.targetId())) {
                return false;
            }
            if (!dimensionAllowlist.isEmpty() && !dimensionAllowlist.contains(context.dimensionId())) {
                return false;
            }
            if (dimensionBlacklist.contains(context.dimensionId())) {
                return false;
            }
            if (!requiredMainHandIds.isEmpty() && !requiredMainHandIds.contains(context.mainHandItemId())) {
                return false;
            }
            return !source.directPlayerKillOnly || context.directPlayerKill();
        }

        private static Set<String> resolveBlockTags(List<String> tagIds, List<String> diagnostics, String jobId) {
            Set<String> out = new LinkedHashSet<>();
            for (String tagId : tagIds) {
                if (tagId == null) {
                    continue;
                }
                Identifier parsed = Identifier.tryParse(tagId);
                if (parsed == null) {
                    diagnostics.add("Job '" + jobId + "' has invalid block tag '" + tagId + "'.");
                    continue;
                }
                Identifier parsedId = Objects.requireNonNull(parsed);
                TagKey<Block> tag = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, parsedId);
                int before = out.size();
                try {
                    for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                        Block block = Objects.requireNonNull(holder.value());
                        out.add(BuiltInRegistries.BLOCK.getKey(block).toString());
                    }
                } catch (IllegalStateException e) {
                    diagnostics.add(
                        "Job '" + jobId + "' block tag '" + tagId
                            + "' is not bound yet during early startup; it will retry on server refresh."
                    );
                    continue;
                }
                if (out.size() == before) {
                    diagnostics.add("Job '" + jobId + "' block tag '" + tagId + "' resolved 0 entries.");
                }
            }
            return out;
        }

        private static Set<String> resolveEntityTags(List<String> tagIds, List<String> diagnostics, String jobId) {
            Set<String> out = new LinkedHashSet<>();
            for (String tagId : tagIds) {
                if (tagId == null) {
                    continue;
                }
                Identifier parsed = Identifier.tryParse(tagId);
                if (parsed == null) {
                    diagnostics.add("Job '" + jobId + "' has invalid entity tag '" + tagId + "'.");
                    continue;
                }
                Identifier parsedId = Objects.requireNonNull(parsed);
                TagKey<EntityType<?>> tag = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, parsedId);
                int before = out.size();
                try {
                    for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
                        EntityType<?> entityType = Objects.requireNonNull(holder.value());
                        out.add(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
                    }
                } catch (IllegalStateException e) {
                    diagnostics.add(
                        "Job '" + jobId + "' entity tag '" + tagId
                            + "' is not bound yet during early startup; it will retry on server refresh."
                    );
                    continue;
                }
                if (out.size() == before) {
                    diagnostics.add("Job '" + jobId + "' entity tag '" + tagId + "' resolved 0 entries.");
                }
            }
            return out;
        }

        private static Set<String> resolveItemTags(List<String> tagIds, List<String> diagnostics, String jobId) {
            Set<String> out = new LinkedHashSet<>();
            for (String tagId : tagIds) {
                if (tagId == null) {
                    continue;
                }
                Identifier parsed = Identifier.tryParse(tagId);
                if (parsed == null) {
                    diagnostics.add("Job '" + jobId + "' has invalid item tag '" + tagId + "'.");
                    continue;
                }
                Identifier parsedId = Objects.requireNonNull(parsed);
                TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, parsedId);
                int before = out.size();
                try {
                    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                        Item item = Objects.requireNonNull(holder.value());
                        out.add(BuiltInRegistries.ITEM.getKey(item).toString());
                    }
                } catch (IllegalStateException e) {
                    diagnostics.add(
                        "Job '" + jobId + "' item tag '" + tagId
                            + "' is not bound yet during early startup; it will retry on server refresh."
                    );
                    continue;
                }
                if (out.size() == before) {
                    diagnostics.add("Job '" + jobId + "' item tag '" + tagId + "' resolved 0 entries.");
                }
            }
            return out;
        }

        private static Set<String> normalizeIds(
            List<String> ids,
            List<String> diagnostics,
            String jobId,
            String field
        ) {
            Set<String> out = new LinkedHashSet<>();
            if (ids == null) {
                return out;
            }
            for (String id : ids) {
                if (id == null) {
                    continue;
                }
                Identifier parsed = Identifier.tryParse(id);
                if (parsed == null) {
                    diagnostics.add("Job '" + jobId + "' has invalid id '" + id + "' in " + field + ".");
                    continue;
                }
                out.add(Objects.requireNonNull(parsed).toString());
            }
            return out;
        }
    }
}
