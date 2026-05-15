package com.fpsmod.client.ui;

/**
 * ARGB colors for the {@code /otter} hub. Pure data — no I/O.
 */
public record OtterUiPalette(
    int panelBg,
    int panelBgAlt,
    int panelBorder,
    int accentPrimary,
    int accentSecondary,
    int textPrimary,
    int textMuted,
    int textDim,
    int hoverBg,
    int tabHoverBg,
    int btnBg,
    int btnBgHover,
    int stripeJobs,
    int stripeGuild,
    int scrimRgb
) {}
