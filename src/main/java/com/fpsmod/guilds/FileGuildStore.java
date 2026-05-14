package com.fpsmod.guilds;

import com.fpsmod.OogaMod;
import com.fpsmod.io.AtomicFileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FileGuildStore implements GuildStore {
    private static final String CONFIG_FOLDER = "otters_civ_revived";
    private static final String DATA_FILE = "guilds_data.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path filePath;

    public FileGuildStore() {
        this(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FOLDER).resolve(DATA_FILE));
    }

    FileGuildStore(Path explicit) {
        this.filePath = explicit;
    }

    @Override
    public GuildLedger load() {
        AtomicFileWriter.deleteStaleTemp(filePath);
        if (!Files.exists(filePath)) {
            return new GuildLedger(Map.of(), Map.of(), Set.of());
        }
        try (var reader = Files.newBufferedReader(filePath)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                return new GuildLedger(Map.of(), Map.of(), Set.of());
            }
            JsonObject obj = root.getAsJsonObject();

            Map<UUID, Guild> guilds = new HashMap<>();
            Map<String, UUID> byName = new HashMap<>();

            JsonElement guildsEl = obj.get("guilds");
            if (guildsEl != null && guildsEl.isJsonArray()) {
                for (JsonElement el : guildsEl.getAsJsonArray()) {
                    StoredGuild sg = GSON.fromJson(el, StoredGuild.class);
                    if (sg == null || sg.id == null) continue;
                    UUID guid = UUID.fromString(sg.id);
                    UUID ownerId = UUID.fromString(sg.owner);
                    Guild g = new Guild(guid, sg.name, ownerId);
                    g.description = sg.description == null ? "" : sg.description;
                    g.balance = sg.balance;
                    g.createdAt = sg.createdAt;
                    g.open = sg.open;
                    g.homePos = sg.homeX != null ? new BlockPos(sg.homeX, sg.homeY, sg.homeZ) : null;
                    g.homeDimension = sg.homeDimension;
                    if (sg.officers != null) {
                        for (String oid : sg.officers) g.officers.add(UUID.fromString(oid));
                    }
                    if (sg.members != null) {
                        for (String mid : sg.members) g.members.add(UUID.fromString(mid));
                    }
                    if (!g.members.contains(g.owner)) g.members.add(g.owner);
                    guilds.put(g.id, g);
                    byName.put(normalizeName(g.name), g.id);
                }
            }

            Set<ClaimedChunk> claims = new HashSet<>();
            JsonElement claimsEl = obj.get("claims");
            if (claimsEl != null && claimsEl.isJsonArray()) {
                for (JsonElement el : claimsEl.getAsJsonArray()) {
                    StoredClaim sc = GSON.fromJson(el, StoredClaim.class);
                    if (sc == null || sc.dimension == null || sc.guildId == null) continue;
                    claims.add(new ClaimedChunk(sc.dimension, sc.chunkX, sc.chunkZ,
                        UUID.fromString(sc.guildId), sc.claimedAt));
                }
            }

            return new GuildLedger(guilds, byName, claims);
        } catch (IOException e) {
            OogaMod.LOGGER.error("[guilds] Failed to read guilds data", e);
            return new GuildLedger(Map.of(), Map.of(), Set.of());
        }
    }

    @Override
    public void save(Map<UUID, Guild> guilds, Map<String, UUID> byName, Set<ClaimedChunk> claims) {
        try {
            Files.createDirectories(filePath.getParent());
            AtomicFileWriter.writeAtomicallyWithBackup(filePath, w -> {
                StoredData data = new StoredData();
                for (Guild g : guilds.values()) {
                    StoredGuild sg = new StoredGuild();
                    sg.id = g.id.toString();
                    sg.name = g.name;
                    sg.description = g.description;
                    sg.owner = g.owner.toString();
                    sg.balance = g.balance;
                    sg.createdAt = g.createdAt;
                    sg.open = g.open;
                    if (g.homePos != null) {
                        sg.homeX = g.homePos.getX();
                        sg.homeY = g.homePos.getY();
                        sg.homeZ = g.homePos.getZ();
                    }
                    sg.homeDimension = g.homeDimension;
                    for (UUID o : g.officers) sg.officers.add(o.toString());
                    for (UUID m : g.members) sg.members.add(m.toString());
                    data.guilds.add(sg);
                }
                for (ClaimedChunk c : claims) {
                    StoredClaim sc = new StoredClaim();
                    sc.dimension = c.dimension();
                    sc.chunkX = c.chunkX();
                    sc.chunkZ = c.chunkZ();
                    sc.guildId = c.guildId().toString();
                    sc.claimedAt = c.claimedAt();
                    data.claims.add(sc);
                }
                w.write(GSON.toJson(data));
                w.newLine();
            });
        } catch (IOException e) {
            OogaMod.LOGGER.error("[guilds] Failed to write guilds data", e);
        }
    }

    static String normalizeName(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static final class StoredData {
        List<StoredGuild> guilds = new ArrayList<>();
        List<StoredClaim> claims = new ArrayList<>();
    }

    private static final class StoredGuild {
        String id;
        String name;
        String description;
        String owner;
        long balance;
        long createdAt;
        boolean open;
        Integer homeX, homeY, homeZ;
        String homeDimension;
        List<String> officers = new ArrayList<>();
        List<String> members = new ArrayList<>();
    }

    private static final class StoredClaim {
        String dimension;
        int chunkX, chunkZ;
        String guildId;
        long claimedAt;
    }
}
