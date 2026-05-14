package com.fpsmod.jobs;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IJobsService {
    boolean jobsEnabled();
    CompiledJobCatalog catalog();
    List<Job> jobs();
    List<Job> visibleJobs();
    Job jobById(String rawJobId);
    JobsConfig config();
    int maxActiveJobs();
    String defaultIconGlyph(Job job);
    String defaultIconKey(Job job);
    JobState stateOf(UUID id);
    boolean joinJob(UUID id, String rawJobId, @Nullable ServerPlayer player);
    boolean leaveJob(UUID id, @Nullable String rawJobId, @Nullable ServerPlayer player);
    long modifyPayout(ServerPlayer player, JobEventContext context, long basePayout);
    void onGameplayEvent(ServerPlayer player, JobEventContext context);
    JobCatalogSnapshot catalogSnapshot();
    JobStatusSnapshotData statusSnapshot(UUID playerId);
    String joinFailureReason(String rawJobId, UUID playerId);
    void setStatusListener(java.util.function.Consumer<ServerPlayer> listener);
    List<String> validationMessages();
    void refresh();
    void rememberPlayerName(UUID id, @Nullable String name);
}
