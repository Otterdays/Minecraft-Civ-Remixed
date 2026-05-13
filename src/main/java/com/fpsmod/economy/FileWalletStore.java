package com.fpsmod.economy;

import com.fpsmod.OogaMod;
import com.fpsmod.io.AtomicFileWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileWalletStore implements WalletStore {
    private static final String CONFIG_FOLDER = "otters_civ_revived";
    private static final String LEGACY_CONFIG_FOLDER = "fpsmod";
    private static final String FILE_NAME = "wallet.properties";

    private static final Pattern LINE_NAME_HINT = Pattern.compile("^#\\s*[Nn]ame:\\s*(.+)$");
    private static final Pattern LINE_UUID_BALANCE =
        Pattern.compile(
            "^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})=(\\d+)$"
        );

    private final Path filePath;

    public FileWalletStore() {
        this(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FOLDER).resolve(FILE_NAME));
    }

    FileWalletStore(Path explicit) {
        this.filePath = explicit;
    }

    private void migrateLegacyWalletIfNeeded() {
        if (Files.exists(filePath)) {
            return;
        }
        Path legacy;
        try {
            legacy = FabricLoader.getInstance().getConfigDir()
                .resolve(LEGACY_CONFIG_FOLDER).resolve(FILE_NAME);
        } catch (RuntimeException e) {
            // FabricLoader not available (e.g. test environment)
            return;
        }
        if (!Files.isRegularFile(legacy)) {
            return;
        }
        try {
            byte[] content = Files.readAllBytes(legacy);
            Files.createDirectories(filePath.getParent());
            AtomicFileWriter.writeAtomically(filePath, w -> {
                w.write(new String(content, StandardCharsets.UTF_8));
            });
            Files.delete(legacy);
            OogaMod.LOGGER.info(
                "[otters_civ_revived] Migrated {} to {} (economy config lives with Otters Civ., not FPS HUD)",
                legacy,
                filePath
            );
        } catch (IOException e) {
            OogaMod.LOGGER.warn(
                "[otters_civ_revived] Could not migrate legacy wallet from {} to {} — check permissions or move manually",
                legacy,
                filePath,
                e
            );
        }
    }

    private static Map<UUID, Long> parseLegacyPropertiesFormat(Path path) throws IOException {
        Map<UUID, Long> balances = new HashMap<>();
        java.util.Properties properties = new java.util.Properties();
        try (var inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        }
        for (String key : properties.stringPropertyNames()) {
            try {
                UUID playerId = UUID.fromString(key);
                long balance = Long.parseLong(properties.getProperty(key));
                balances.put(playerId, balance);
            } catch (IllegalArgumentException e) {
                OogaMod.LOGGER.warn(
                    "[otters_civ_revived] Skipping invalid wallet entry key={} value={}",
                    key,
                    properties.getProperty(key)
                );
            }
        }
        return balances;
    }

    private WalletLedger parseCustomFormat(Path path) throws IOException {
        Map<UUID, Long> balances = new HashMap<>();
        Map<UUID, String> hints = new HashMap<>();

        String pendingName = null;
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher nameMatcher = LINE_NAME_HINT.matcher(line);
            if (nameMatcher.matches()) {
                pendingName = sanitizeHintForStorage(nameMatcher.group(1));
                continue;
            }

            Matcher uuidMatcher = LINE_UUID_BALANCE.matcher(line);
            if (uuidMatcher.matches()) {
                UUID id = UUID.fromString(uuidMatcher.group(1));
                long amount = Long.parseLong(uuidMatcher.group(2));
                balances.put(id, amount);
                if (pendingName != null && !pendingName.isEmpty()) {
                    hints.put(id, pendingName);
                }
                pendingName = null;
                continue;
            }

            pendingName = null;
        }

        return new WalletLedger(balances, hints);
    }

    @Override
    public WalletLedger load() {
        AtomicFileWriter.deleteStaleTemp(filePath);
        migrateLegacyWalletIfNeeded();

        if (!Files.exists(filePath)) {
            return WalletLedger.empty();
        }

        try {
            WalletLedger custom = parseCustomFormat(filePath);
            if (!custom.balances().isEmpty()) {
                return custom;
            }
            Map<UUID, Long> legacyBalances = parseLegacyPropertiesFormat(filePath);
            return new WalletLedger(legacyBalances, Map.of());
        } catch (IOException e) {
            OogaMod.LOGGER.warn("[otters_civ_revived] Failed to read wallet store at {}", filePath, e);
            return WalletLedger.empty();
        }
    }

    @Override
    public void save(Map<UUID, Long> balances, Map<UUID, String> displayHints) {
        Objects.requireNonNull(balances, "balances");
        Objects.requireNonNull(displayHints, "displayHints");

        Map<UUID, Long> sorted = new TreeMap<>((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        sorted.putAll(balances);

        try {
            Files.createDirectories(filePath.getParent());
            AtomicFileWriter.writeAtomicallyWithBackup(filePath, w -> {
                w.write("# Project OOGA wallet balances");
                w.newLine();
                w.write("# UUID=balance is authoritative. Lines \"# Name: ...\" above an entry are plain-text hints.");
                w.newLine();
                for (Map.Entry<UUID, Long> e : sorted.entrySet()) {
                    UUID id = e.getKey();
                    String hint = sanitizeHintForStorage(displayHints.get(id));
                    if (!hint.isEmpty()) {
                        w.write("# Name: ");
                        w.write(hint);
                        w.newLine();
                    }
                    w.write(id.toString());
                    w.write('=');
                    w.write(Long.toString(e.getValue()));
                    w.newLine();
                }
            });
        } catch (IOException e) {
            OogaMod.LOGGER.error("[otters_civ_revived] Failed to write wallet store at {}", filePath, e);
        }
    }

    static String sanitizeHintForStorage(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.replace('\n', ' ').replace('\r', ' ').replace('#', ' ').trim();
        return cleaned.length() > 128 ? cleaned.substring(0, 128) : cleaned;
    }
}
