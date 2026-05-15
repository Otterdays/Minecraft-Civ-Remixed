package com.fpsmod.client.ui;

/**
 * Built-in color themes for the {@code /otter} civ hub screen.
 */
public enum OtterUiTheme {
    OTTER(
        "Otter",
        new OtterUiPalette(
            0xFF111827,
            0xFF0B1220,
            0xFF1F2937,
            0xFFE9B949,
            0xFF67E8F9,
            0xFFE5E7EB,
            0xFF9CA3AF,
            0xFF6B7280,
            0xFF1E293B,
            0xFF182334,
            0xFF1F2937,
            0xFF334155,
            0xFF22C55E,
            0xFF67E8F9,
            0x000A0E1A
        )
    ),
    MIDNIGHT(
        "Midnight",
        new OtterUiPalette(
            0xFF0C1020,
            0xFF080C18,
            0xFF1E2A4A,
            0xFFC4B5FD,
            0xFF38BDF8,
            0xFFE2E8F0,
            0xFF94A3B8,
            0xFF64748B,
            0xFF1A2440,
            0xFF141C32,
            0xFF162038,
            0xFF243252,
            0xFF34D399,
            0xFF7DD3FC,
            0x00060A14
        )
    ),
    SUNSET(
        "Sunset",
        new OtterUiPalette(
            0xFF1F1410,
            0xFF120C0A,
            0xFF3F2D26,
            0xFFFBBF24,
            0xFFFB7185,
            0xFFFFF1F2,
            0xFFFBCFE8,
            0xFF9D6B53,
            0xFF3A2420,
            0xFF2A1814,
            0xFF352018,
            0xFF5C3028,
            0xFF4ADE80,
            0xFFF472B6,
            0x0014080C
        )
    ),
    MINT(
        "Mint",
        new OtterUiPalette(
            0xFF0F1A16,
            0xFF081210,
            0xFF1F3D34,
            0xFF5EEAD4,
            0xFFA7F3D0,
            0xFFECFDF5,
            0xFF86EFAC,
            0xFF4B5563,
            0xFF143028,
            0xFF102820,
            0xFF1A332C,
            0xFF265445,
            0xFF34D399,
            0xFF6EE7B7,
            0x00081410
        )
    );

    private final String displayName;
    private final OtterUiPalette palette;

    OtterUiTheme(String displayName, OtterUiPalette palette) {
        this.displayName = displayName;
        this.palette = palette;
    }

    public String displayName() {
        return displayName;
    }

    public OtterUiPalette palette() {
        return palette;
    }

    public OtterUiTheme next() {
        OtterUiTheme[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static OtterUiTheme fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTTER;
        }
        String s = raw.trim();
        for (OtterUiTheme t : values()) {
            if (t.name().equalsIgnoreCase(s)) {
                return t;
            }
        }
        return OTTER;
    }
}
