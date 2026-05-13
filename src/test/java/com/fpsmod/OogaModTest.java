package com.fpsmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OogaModTest {
    @Test
    void modIdStaysStableForMetadataAndLogging() {
        assertEquals("project_ooga", OogaMod.MOD_ID);
    }
}
