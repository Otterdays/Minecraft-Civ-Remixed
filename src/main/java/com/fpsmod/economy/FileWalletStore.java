package com.fpsmod.economy;

import com.fpsmod.FpsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class FileWalletStore implements WalletStore {
    private static final String FOLDER_NAME = "fpsmod";
    private static final String FILE_NAME = "wallet.properties";
    private final Path filePath;

    public FileWalletStore() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(FOLDER_NAME);
        this.filePath = configDir.resolve(FILE_NAME);
    }

    @Override
    public Map<UUID, Long> load() {
        Map<UUID, Long> balances = new HashMap<>();
        if (!Files.exists(filePath)) {
            return balances;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            FpsMod.LOGGER.warn("Failed to read wallet store at {}", filePath, e);
            return balances;
        }

        for (String key : properties.stringPropertyNames()) {
            try {
                UUID playerId = UUID.fromString(key);
                long balance = Long.parseLong(properties.getProperty(key));
                balances.put(playerId, balance);
            } catch (IllegalArgumentException e) {
                FpsMod.LOGGER.warn("Skipping invalid wallet entry key={} value={}", key, properties.getProperty(key));
            }
        }

        return balances;
    }

    @Override
    public void save(Map<UUID, Long> balances) {
        Properties properties = new Properties();
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            properties.setProperty(entry.getKey().toString(), Long.toString(entry.getValue()));
        }

        try {
            Files.createDirectories(filePath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                properties.store(outputStream, "Project OOGA wallet balances");
            }
        } catch (IOException e) {
            FpsMod.LOGGER.error("Failed to write wallet store at {}", filePath, e);
        }
    }
}
