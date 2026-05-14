package com.fpsmod.persistence;

import com.fpsmod.OogaMod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class SqliteDatabase implements AutoCloseable {
    private final Path dbPath;
    private Connection connection;

    public SqliteDatabase() {
        this(FabricLoader.getInstance().getConfigDir().resolve("otters_civ_revived").resolve("project_ooga.db"));
    }

    SqliteDatabase(Path dbPath) {
        this.dbPath = dbPath;
    }

    public synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                java.nio.file.Files.createDirectories(dbPath.getParent());
            } catch (java.io.IOException e) {
                throw new SQLException("Failed to create database directory", e);
            }
            Properties props = new Properties();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath(), props);

            try (var stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA busy_timeout=5000");
                stmt.execute("PRAGMA foreign_keys=ON");
            }
        }
        return connection;
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection();
            OogaMod.LOGGER.info("[persistence] SQLite database opened at {}", dbPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SQLite database", e);
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
                OogaMod.LOGGER.info("[persistence] SQLite database closed");
            } catch (SQLException e) {
                OogaMod.LOGGER.warn("[persistence] Error closing database", e);
            }
        }
    }
}
