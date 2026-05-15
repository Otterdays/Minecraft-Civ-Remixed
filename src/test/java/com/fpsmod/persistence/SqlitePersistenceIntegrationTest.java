package com.fpsmod.persistence;

import com.fpsmod.economy.EconomyConfig;
import com.fpsmod.economy.TransactionReason;
import com.fpsmod.economy.WalletLedger;
import com.fpsmod.economy.WalletService;
import com.fpsmod.guilds.ClaimedChunk;
import com.fpsmod.guilds.Guild;
import com.fpsmod.guilds.GuildLedger;
import com.fpsmod.jobs.JobState;
import com.fpsmod.jobs.JobsLedger;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlitePersistenceIntegrationTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID GUILD_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @TempDir
    Path tempDir;

    @Test
    void schemaMigrationCreatesExpectedTablesAndPragmas() throws Exception {
        try (SqliteDatabase db = migratedDb()) {
            try (Connection conn = db.connection(); Statement stmt = conn.createStatement()) {
                assertEquals("wal", queryString(stmt, "PRAGMA journal_mode").toLowerCase(Locale.ROOT));
                assertEquals(1, queryInt(stmt, "PRAGMA synchronous"));
                assertEquals(5000, queryInt(stmt, "PRAGMA busy_timeout"));
                assertEquals(1, queryInt(stmt, "PRAGMA foreign_keys"));
                assertEquals(1, queryInt(stmt, "SELECT max(version) FROM schema_version"));

                assertTrue(tableExists(stmt, "wallets"));
                assertTrue(tableExists(stmt, "wallet_ledger"));
                assertTrue(tableExists(stmt, "guilds"));
                assertTrue(tableExists(stmt, "guild_members"));
                assertTrue(tableExists(stmt, "claims"));
                assertTrue(tableExists(stmt, "jobs_state"));
            }
        }
    }

    @Test
    void walletServicePersistsBalancesAndLedgerToSqlite() throws Exception {
        try (SqliteDatabase db = migratedDb()) {
            SqliteWalletStore walletStore = new SqliteWalletStore(db);
            SqliteTransactionLog transactionLog = new SqliteTransactionLog(db);
            WalletService service = new WalletService(walletStore, transactionLog, EconomyConfig.defaults());

            service.setBalance(PLAYER, 100L);
            assertEquals(WalletService.TransferResult.OK, service.transfer(
                PLAYER,
                "Alice",
                OTHER_PLAYER,
                "Bob",
                10L,
                2L
            ));
            service.addBalance(OTHER_PLAYER, 5L, "Bob", TransactionReason.REWARD_BLOCK);

            WalletLedger loaded = walletStore.load();
            assertEquals(88L, loaded.balances().get(PLAYER));
            assertEquals(15L, loaded.balances().get(OTHER_PLAYER));
            assertEquals("Alice", loaded.displayHints().get(PLAYER));
            assertEquals("Bob", loaded.displayHints().get(OTHER_PLAYER));

            var recent = transactionLog.readRecent(10);
            assertEquals(5, recent.size());
            assertEquals(TransactionReason.REWARD_BLOCK, recent.get(0).reason());
            assertEquals(TransactionReason.PLAYER_PAY_FEE, recent.get(1).reason());
            assertEquals(TransactionReason.PLAYER_PAY_RECEIVED, recent.get(2).reason());
            assertEquals(TransactionReason.PLAYER_PAY_SENT, recent.get(3).reason());
            assertEquals(TransactionReason.ADMIN_SET, recent.get(4).reason());

            var playerEntries = transactionLog.readForPlayer(PLAYER, 10);
            assertEquals(3, playerEntries.size());
            assertEquals(TransactionReason.PLAYER_PAY_FEE, playerEntries.get(0).reason());
            assertEquals(TransactionReason.PLAYER_PAY_SENT, playerEntries.get(1).reason());
            assertEquals(TransactionReason.ADMIN_SET, playerEntries.get(2).reason());
        }
    }

    @Test
    void jobsAndGuildStateRoundTripThroughSqlite() throws Exception {
        try (SqliteDatabase db = migratedDb()) {
            SqliteJobsStore jobsStore = new SqliteJobsStore(db);
            JobState state = new JobState();
            state.setXp("miner", 125L);
            state.setXp("fighter", 25L);
            state.activate("miner", 1);
            jobsStore.save(Map.of(PLAYER, state), Map.of());

            JobsLedger jobsLedger = jobsStore.load();
            JobState loadedState = jobsLedger.states().get(PLAYER);
            assertEquals(125L, loadedState.getXp("miner"));
            assertEquals(25L, loadedState.getXp("fighter"));
            assertEquals(List.of("miner"), loadedState.activeJobIds());

            SqliteGuildStore guildStore = new SqliteGuildStore(db);
            Guild guild = new Guild(GUILD_ID, "Otters", PLAYER);
            guild.description = "SQLite guild";
            guild.balance = 250L;
            guild.homePos = new BlockPos(10, 64, -8);
            guild.homeDimension = "minecraft:overworld";
            guild.open = true;
            guild.officers.add(OTHER_PLAYER);
            guild.members.add(OTHER_PLAYER);

            ClaimedChunk claim = new ClaimedChunk("minecraft:overworld", 3, -4, GUILD_ID, 123456789L);
            Map<String, UUID> byName = new LinkedHashMap<>();
            byName.put("otters", GUILD_ID);
            guildStore.save(Map.of(GUILD_ID, guild), byName, Set.of(claim));

            GuildLedger loadedGuilds = guildStore.load();
            Guild loadedGuild = loadedGuilds.guilds().get(GUILD_ID);
            assertEquals("Otters", loadedGuild.name);
            assertEquals("SQLite guild", loadedGuild.description);
            assertEquals(250L, loadedGuild.balance);
            assertTrue(loadedGuild.open);
            assertEquals(new BlockPos(10, 64, -8), loadedGuild.homePos);
            assertTrue(loadedGuild.members.contains(OTHER_PLAYER));
            assertTrue(loadedGuild.officers.contains(OTHER_PLAYER));
            assertTrue(loadedGuilds.claims().contains(claim));
            assertEquals(GUILD_ID, loadedGuilds.byName().get("otters"));
        }
    }

    @Test
    void sqliteStateSurvivesCloseAndReopen() throws Exception {
        try (SqliteDatabase db = migratedDb()) {
            SqliteWalletStore walletStore = new SqliteWalletStore(db);
            SqliteTransactionLog transactionLog = new SqliteTransactionLog(db);
            SqliteJobsStore jobsStore = new SqliteJobsStore(db);

            WalletService service = new WalletService(walletStore, transactionLog, EconomyConfig.defaults());
            service.setBalance(PLAYER, 77L);
            service.addBalance(PLAYER, 5L, "Alice", TransactionReason.REWARD_BLOCK);

            JobState state = new JobState();
            state.setXp("miner", 33L);
            state.activate("miner", 1);
            jobsStore.save(Map.of(PLAYER, state), Map.of());
        }

        try (SqliteDatabase reopened = new SqliteDatabase(dbPath())) {
            reopened.initialize();
            WalletLedger walletLedger = new SqliteWalletStore(reopened).load();
            JobsLedger jobsLedger = new SqliteJobsStore(reopened).load();
            var ledgerEntries = new SqliteTransactionLog(reopened).readForPlayer(PLAYER, 10);

            assertEquals(82L, walletLedger.balances().get(PLAYER));
            assertEquals("Alice", walletLedger.displayHints().get(PLAYER));
            assertEquals(33L, jobsLedger.states().get(PLAYER).getXp("miner"));
            assertEquals(List.of("miner"), jobsLedger.states().get(PLAYER).activeJobIds());
            assertEquals(2, ledgerEntries.size());
            assertEquals(TransactionReason.REWARD_BLOCK, ledgerEntries.get(0).reason());
            assertEquals(TransactionReason.ADMIN_SET, ledgerEntries.get(1).reason());
        }
    }

    private SqliteDatabase migratedDb() {
        SqliteDatabase db = new SqliteDatabase(dbPath());
        db.initialize();
        new SchemaMigrator(db).migrate();
        return db;
    }

    private Path dbPath() {
        return tempDir.resolve("project_ooga.db");
    }

    private static boolean tableExists(Statement stmt, String tableName) throws Exception {
        try (ResultSet rs = stmt.executeQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "'")) {
            return rs.next();
        }
    }

    private static int queryInt(Statement stmt, String sql) throws Exception {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            assertTrue(rs.next(), "expected a row for: " + sql);
            return rs.getInt(1);
        }
    }

    private static String queryString(Statement stmt, String sql) throws Exception {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            assertTrue(rs.next(), "expected a row for: " + sql);
            return rs.getString(1);
        }
    }
}
