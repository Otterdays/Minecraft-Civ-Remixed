package com.fpsmod.jobs;

import com.fpsmod.OogaMod;
import com.fpsmod.ottersciv.reward.JobsHooks;
import com.fpsmod.ottersciv.reward.RewardContext;
import com.fpsmod.ottersciv.reward.RewardReason;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVP jobs runtime: one active job slot per player, XP accrues on matching reward events,
 * payout multiplier applied to wallet credit.
 *
 * <p>Tag membership is resolved per-job via {@link BuiltInRegistries#getTagOrEmpty} at
 * {@link #refresh(MinecraftServer) refresh} time (server-started / data-pack reload). Per-event
 * lookups are O(1) set membership against the cached id set — same static-registry path that
 * works around the {@code level.registryAccess()} tag-empty quirk documented in
 * {@code DOCS/ARCHITECTURE.md}.</p>
 */
public class JobsService implements JobsHooks {
    private final JobsStore store;
    private final Map<UUID, JobState> states;
    private final Map<UUID, String> displayHints;
    private final EnumMap<Job, Set<String>> blockIdsByJob = new EnumMap<>(Job.class);
    private final EnumMap<Job, Set<String>> entityIdsByJob = new EnumMap<>(Job.class);

    /** Optional callback fired after any per-player state mutation so the HUD can sync. */
    private java.util.function.Consumer<ServerPlayer> statusListener = p -> {};

    public void setStatusListener(java.util.function.Consumer<ServerPlayer> listener) {
        this.statusListener = listener == null ? p -> {} : listener;
    }

    public JobsService(JobsStore store) {
        this.store = Objects.requireNonNull(store, "store");
        JobsLedger loaded = store.load();
        this.states = new ConcurrentHashMap<>(loaded.states());
        this.displayHints = new ConcurrentHashMap<>(loaded.displayHints());
        for (Job j : Job.values()) {
            blockIdsByJob.put(j, Set.of());
            entityIdsByJob.put(j, Set.of());
        }
    }

    public static JobsService createDefault() {
        return new JobsService(new FileJobsStore());
    }

    /** Rebuild the per-job id sets from current registries. Call on SERVER_STARTED + END_DATA_PACK_RELOAD. */
    public void refresh(MinecraftServer server) {
        for (Job job : Job.values()) {
            switch (job.kind()) {
                case BLOCK -> blockIdsByJob.put(job, resolveBlockIds(job));
                case ENTITY -> entityIdsByJob.put(job, resolveEntityIds(job));
            }
        }
    }

    private Set<String> resolveBlockIds(Job job) {
        Set<String> out = new HashSet<>();
        var tag = job.blockTagKey();
        if (tag == null) return out;
        for (Holder<Block> h : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
            Block b = h.value();
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            if (id != null) out.add(id.toString());
        }
        OogaMod.LOGGER.info("[otters_civ_revived/jobs] {} block tag resolved {} entries", job.slug(), out.size());
        return out;
    }

    private Set<String> resolveEntityIds(Job job) {
        Set<String> out = new HashSet<>();
        var tag = job.entityTagKey();
        if (tag == null) return out;
        for (Holder<EntityType<?>> h : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
            EntityType<?> t = h.value();
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
            if (id != null) out.add(id.toString());
        }
        OogaMod.LOGGER.info("[otters_civ_revived/jobs] {} entity tag resolved {} entries", job.slug(), out.size());
        return out;
    }

    public JobState stateOf(UUID id) {
        return states.computeIfAbsent(id, k -> new JobState());
    }

    public void rememberPlayerName(UUID id, @Nullable String name) {
        String s = FileJobsStore.sanitizeHintForStorage(name);
        if (!s.isEmpty()) {
            displayHints.put(id, s);
        }
    }

    public synchronized boolean joinJob(UUID id, Job job, ServerPlayer player) {
        JobState s = stateOf(id);
        if (s.active() == job) return false;
        s.setActive(job);
        persist();
        statusListener.accept(player);
        return true;
    }

    public synchronized boolean leaveJob(UUID id, ServerPlayer player) {
        JobState s = stateOf(id);
        if (s.active() == null) return false;
        s.setActive(null);
        persist();
        statusListener.accept(player);
        return true;
    }

    private synchronized void persist() {
        store.save(Map.copyOf(states), Map.copyOf(displayHints));
    }

    @Nullable
    private Job jobMatchingBlock(BlockState block) {
        if (block == null) return null;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        if (id == null) return null;
        String s = id.toString();
        for (Job j : Job.values()) {
            if (j.kind() == Job.Kind.BLOCK && blockIdsByJob.get(j).contains(s)) {
                return j;
            }
        }
        return null;
    }

    @Nullable
    private Job jobMatchingEntity(EntityType<?> type) {
        if (type == null) return null;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return null;
        String s = id.toString();
        for (Job j : Job.values()) {
            if (j.kind() == Job.Kind.ENTITY && entityIdsByJob.get(j).contains(s)) {
                return j;
            }
        }
        return null;
    }

    @Override
    public long multiplyPayout(ServerPlayer player, RewardContext ctx, long basePayout) {
        if (player == null || ctx == null || basePayout <= 0L) return basePayout;
        JobState s = states.get(player.getUUID());
        if (s == null || s.active() == null) return basePayout;
        Job active = s.active();
        Job matched = matchedJob(ctx);
        if (matched != active) return basePayout;
        double mult = JobsConfig.multiplierForLevel(s.levelOf(active));
        long scaled = (long) Math.floor(basePayout * mult);
        return Math.max(basePayout, scaled);
    }

    @Override
    public void onEconomyReward(ServerPlayer player, RewardContext ctx) {
        if (player == null || ctx == null) return;
        rememberPlayerName(player.getUUID(), player.getName().getString());
        JobState s = stateOf(player.getUUID());
        if (s.active() == null) return;
        Job matched = matchedJob(ctx);
        if (matched != s.active()) return;
        int prevLevel = s.levelOf(s.active());
        long newXp = s.addXp(s.active(), JobsConfig.XP_PER_EVENT);
        int newLevel = JobsConfig.levelForXp(newXp);
        persist();
        statusListener.accept(player);
        if (newLevel > prevLevel) {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "[" + s.active().slug() + "] level up → " + newLevel
                )
            );
        }
    }

    @Nullable
    private Job matchedJob(RewardContext ctx) {
        if (ctx.reason() == RewardReason.BLOCK_BREAK) return jobMatchingBlock(ctx.blockState());
        if (ctx.reason() == RewardReason.MOB_KILL) return jobMatchingEntity(ctx.entityType());
        return null;
    }
}
