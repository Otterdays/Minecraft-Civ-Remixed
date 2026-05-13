package com.fpsmod.ottersciv.reward;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardOrchestratorMessageTest {

    @Test
    void coinMessageUsesCoinsLabel() {
        assertEquals("+1 coins", RewardOrchestrator.coinMessageText(1L));
    }
}
