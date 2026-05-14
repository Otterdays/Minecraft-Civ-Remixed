package com.fpsmod.persistence;

import com.fpsmod.economy.TransactionLog;
import com.fpsmod.economy.WalletStore;
import com.fpsmod.guilds.GuildStore;
import com.fpsmod.jobs.JobsStore;

public class PersistenceService implements AutoCloseable {
    private final SqliteDatabase database;
    private final SchemaMigrator migrator;
    private final WalletStore walletStore;
    private final TransactionLog transactionLog;
    private final JobsStore jobsStore;
    private final GuildStore guildStore;

    public PersistenceService() {
        this.database = new SqliteDatabase();
        this.migrator = new SchemaMigrator(database);
        this.walletStore = new SqliteWalletStore(database);
        this.transactionLog = new SqliteTransactionLog(database);
        this.jobsStore = new SqliteJobsStore(database);
        this.guildStore = new SqliteGuildStore(database);
    }

    public void initialize() {
        database.initialize();
        migrator.migrate();
    }

    public SchemaMigrator migrator() {
        return migrator;
    }

    public WalletStore walletStore() {
        return walletStore;
    }

    public TransactionLog transactionLog() {
        return transactionLog;
    }

    public JobsStore jobsStore() {
        return jobsStore;
    }

    public GuildStore guildStore() {
        return guildStore;
    }

    public SqliteDatabase database() {
        return database;
    }

    @Override
    public void close() {
        database.close();
    }
}
