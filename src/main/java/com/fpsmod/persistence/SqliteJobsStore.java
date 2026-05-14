package com.fpsmod.persistence;

import com.fpsmod.jobs.Job;
import com.fpsmod.jobs.JobState;
import com.fpsmod.jobs.JobsLedger;
import com.fpsmod.jobs.JobsStore;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SqliteJobsStore implements JobsStore {
    private final SqliteDatabase db;

    public SqliteJobsStore(SqliteDatabase db) {
        this.db = db;
    }

    @Override
    public JobsLedger load() {
        Map<UUID, JobState> states = new HashMap<>();
        Map<UUID, String> hints = new HashMap<>();
        try (Connection conn = db.connection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT player_uuid, job_id, xp, active FROM jobs_state ORDER BY player_uuid, job_id")) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_uuid"));
                JobState state = states.computeIfAbsent(id, k -> new JobState());
                String jobId = rs.getString("job_id");
                long xp = rs.getLong("xp");
                boolean active = rs.getInt("active") != 0;
                state.setXp(jobId, xp);
                if (active) {
                    state.activate(jobId, Integer.MAX_VALUE);
                }
            }
        } catch (Exception e) {
            // Table may not exist yet
        }
        return new JobsLedger(states, hints);
    }

    @Override
    public void save(Map<UUID, JobState> states, Map<UUID, String> displayHints) {
        try (Connection conn = db.connection()) {
            conn.setAutoCommit(false);
            // Clear existing state for all players we're saving
            for (UUID id : states.keySet()) {
                try (var del = conn.prepareStatement("DELETE FROM jobs_state WHERE player_uuid = ?")) {
                    del.setString(1, id.toString());
                    del.executeUpdate();
                }
            }
            // Insert current state
            try (var ins = conn.prepareStatement(
                "INSERT INTO jobs_state (player_uuid, job_id, xp, active) VALUES (?, ?, ?, ?)")) {
                for (Map.Entry<UUID, JobState> e : states.entrySet()) {
                    UUID id = e.getKey();
                    JobState state = e.getValue();
                    List<String> activeJobs = state.activeJobIds();
                    Map<String, Long> xpMap = state.snapshotXp();
                    for (Map.Entry<String, Long> xpEntry : xpMap.entrySet()) {
                        ins.setString(1, id.toString());
                        ins.setString(2, xpEntry.getKey());
                        ins.setLong(3, xpEntry.getValue());
                        ins.setInt(4, activeJobs.contains(xpEntry.getKey()) ? 1 : 0);
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }
            conn.commit();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save jobs state", ex);
        }
    }
}
