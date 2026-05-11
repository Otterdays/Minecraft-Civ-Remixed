package com.fpsmod.ottersciv.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParseException;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public class RewardRulesLoaderTest {

    @Test
    void parsesBlockAndEntityRewardMaps() throws JsonParseException {
        String json =
            """
            {
              "blockReward": 1,
              "entityReward": 5,
              "blockRewards": {
                "minecraft:diamond_ore": 25,
                "bad id": 99
              },
              "entityRewards": {
                "minecraft:zombie": 3
              }
            }
            """;

        RewardRules r = RewardRulesLoader.parseRewardsJson(json, RewardRules.defaults());

        Assertions.assertEquals(25L, r.blockRewards.get("minecraft:diamond_ore"));
        Assertions.assertFalse(r.blockRewards.containsKey("bad id"));
        Assertions.assertEquals(3L, r.entityRewards.get("minecraft:zombie"));
    }

    @Test
    void blockValuesFileOverridesOverlappingRewardsJsonEntries(@TempDir Path dir) throws Exception {
        Path rewards = dir.resolve("rewards.json");
        Files.writeString(
            rewards,
            """
            {"blockRewards": {"minecraft:diamond_ore": 10, "minecraft:stone": 2}}
            """
        );

        RewardRules loaded;
        try (Reader reader = Files.newBufferedReader(rewards)) {
            loaded = RewardRulesLoader.parseRewardsJson(reader, RewardRules.defaults(), rewards.toString());
        }

        Path blockVals = dir.resolve(RewardRulesLoader.BLOCK_VALUES_FILE);
        Files.writeString(blockVals, """
            {"minecraft:diamond_ore": 42}
            """);

        RewardRulesLoader.mergeExternalValueFiles(dir, loaded);

        Assertions.assertEquals(42L, loaded.blockRewards.get("minecraft:diamond_ore"));
        Assertions.assertEquals(2L, loaded.blockRewards.get("minecraft:stone"));
    }

    @Test
    void entityValuesFileAddsEntriesAndOverrides(@TempDir Path dir) throws Exception {
        Path rewards = dir.resolve("rewards.json");
        Files.writeString(
            rewards,
            """
            {"entityRewards": {"minecraft:zombie": 1}}
            """
        );

        RewardRules loaded;
        try (Reader reader = Files.newBufferedReader(rewards)) {
            loaded = RewardRulesLoader.parseRewardsJson(reader, RewardRules.defaults(), rewards.toString());
        }

        Path entityVals = dir.resolve(RewardRulesLoader.ENTITY_VALUES_FILE);
        Files.writeString(
            entityVals,
            """
            {"minecraft:zombie": 9, "minecraft:skeleton": 4}
            """
        );

        RewardRulesLoader.mergeExternalValueFiles(dir, loaded);

        Assertions.assertEquals(9L, loaded.entityRewards.get("minecraft:zombie"));
        Assertions.assertEquals(4L, loaded.entityRewards.get("minecraft:skeleton"));
    }

    @Test
    void composeEffectiveIdMapMergesPrecedenceAndPersistsWhenSiblingEmpty(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("block_values.json");

        LinkedHashMap<String, Long> tier1 = new LinkedHashMap<>();
        tier1.put("minecraft:stone", 1L);
        tier1.put("minecraft:diamond_ore", 1L);

        LinkedHashMap<String, Long> inline = new LinkedHashMap<>();
        inline.put("minecraft:diamond_ore", 25L);
        inline.put("minecraft:emerald_ore", 30L);

        LinkedHashMap<String, Long> sibling = new LinkedHashMap<>();

        LinkedHashMap<String, Long> merged =
            RewardRulesLoader.composeEffectiveIdMap(tier1, inline, sibling, out, "[test-blocks]");

        Assertions.assertEquals(1L, merged.get("minecraft:stone"));
        Assertions.assertEquals(25L, merged.get("minecraft:diamond_ore"));
        Assertions.assertEquals(30L, merged.get("minecraft:emerald_ore"));
        Assertions.assertTrue(Files.isRegularFile(out), "sibling JSON should be written when disk map was empty");
        String body = Files.readString(out);
        Assertions.assertTrue(body.contains("\"minecraft:diamond_ore\""));
        Assertions.assertTrue(body.contains("\"minecraft:emerald_ore\""));
        Assertions.assertTrue(body.endsWith(System.lineSeparator()), "sibling JSON should end with a newline");
    }

    @Test
    void composeEffectiveIdMapKeepsExistingSiblingFileWhenItHasKeys(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("entity_values.json");
        String original = "{\"minecraft:zombie\":99}";
        Files.writeString(out, original);

        LinkedHashMap<String, Long> tier1 = new LinkedHashMap<>();
        tier1.put("minecraft:zombie", 5L);
        tier1.put("minecraft:skeleton", 5L);

        LinkedHashMap<String, Long> inline = new LinkedHashMap<>();

        LinkedHashMap<String, Long> sibling = new LinkedHashMap<>();
        sibling.put("minecraft:zombie", 99L);

        LinkedHashMap<String, Long> merged =
            RewardRulesLoader.composeEffectiveIdMap(tier1, inline, sibling, out, "[test-entities]");

        Assertions.assertEquals(99L, merged.get("minecraft:zombie"), "sibling overlay must win over tier1");
        Assertions.assertEquals(5L, merged.get("minecraft:skeleton"));
        Assertions.assertEquals(original, Files.readString(out),
            "existing sibling file with keys must not be overwritten");
    }

    @Test
    void corruptExternalValueFileLeavesPriorMapUntouched(@TempDir Path dir) throws Exception {
        Path rewards = dir.resolve("rewards.json");
        Files.writeString(rewards, """
            {"blockRewards": {"minecraft:diamond_ore": 77}}
            """);

        RewardRules loaded;
        try (Reader reader = Files.newBufferedReader(rewards)) {
            loaded = RewardRulesLoader.parseRewardsJson(reader, RewardRules.defaults(), rewards.toString());
        }

        Path blockVals = dir.resolve(RewardRulesLoader.BLOCK_VALUES_FILE);
        Files.writeString(blockVals, "{ invalid json ");

        RewardRulesLoader.mergeExternalValueFiles(dir, loaded);

        Assertions.assertEquals(77L, loaded.blockRewards.get("minecraft:diamond_ore"));
    }
}
