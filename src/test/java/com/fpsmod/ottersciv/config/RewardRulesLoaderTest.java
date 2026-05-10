package com.fpsmod.ottersciv.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParseException;

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
}
