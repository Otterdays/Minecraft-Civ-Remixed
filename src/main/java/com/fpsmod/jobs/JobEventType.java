package com.fpsmod.jobs;

import java.util.Locale;
import java.util.Optional;

public enum JobEventType {
    BLOCK_BREAK,
    MOB_KILL;

    public static Optional<JobEventType> byId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "block_break", "block-break", "block" -> Optional.of(BLOCK_BREAK);
            case "mob_kill", "mob-kill", "kill", "entity_kill", "entity-kill", "mob" -> Optional.of(MOB_KILL);
            default -> Optional.empty();
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
