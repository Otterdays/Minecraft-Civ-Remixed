package com.fpsmod.command;

import com.fpsmod.economy.EconomyConfig;
import com.fpsmod.economy.EconomyConfigLoader;
import com.fpsmod.economy.LedgerEntry;
import com.fpsmod.economy.TransactionReason;
import com.fpsmod.economy.WalletService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("null")
public final class EconomyCommand {
    private static final Permission ADMIN = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);
    private static final int DEFAULT_LOG_COUNT = 10;
    private static final int MAX_LOG_COUNT = 50;

    private EconomyCommand() {}

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        WalletService walletService,
        EconomyConfig economyConfig
    ) {
        dispatcher.register(Commands.literal("economy")
            .requires(source -> source.permissions().hasPermission(ADMIN))
            .then(Commands.literal("reload")
                .executes(ctx -> runReload(ctx.getSource(), walletService, economyConfig)))
            .then(Commands.literal("log")
                .executes(ctx -> runLogRecent(ctx.getSource(), walletService, economyConfig, DEFAULT_LOG_COUNT))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_LOG_COUNT))
                    .executes(ctx -> runLogRecent(
                        ctx.getSource(),
                        walletService,
                        economyConfig,
                        IntegerArgumentType.getInteger(ctx, "count")
                    )))
                .then(Commands.literal("player")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> runLogPlayer(
                            ctx.getSource(),
                            walletService,
                            economyConfig,
                            EntityArgument.getPlayer(ctx, "target"),
                            DEFAULT_LOG_COUNT
                        ))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_LOG_COUNT))
                            .executes(ctx -> runLogPlayer(
                                ctx.getSource(),
                                walletService,
                                economyConfig,
                                EntityArgument.getPlayer(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "count")
                            ))))))
        );
    }

    private static int runReload(CommandSourceStack source, WalletService walletService, EconomyConfig economyConfig) {
        economyConfig.copyFrom(EconomyConfigLoader.loadOrCreate());
        walletService.setEconomyConfig(economyConfig);
        send(source,
            "Reloaded economy.json. /pay cap="
                + formatDisabledLong(economyConfig, economyConfig.maxTransferPerCommand)
                + ", cooldown="
                + formatDisabledInt(economyConfig.transferCooldownSeconds, "s")
                + ", fee="
                + economyConfig.format(Math.max(0L, economyConfig.transferFlatFee))
                + ".");
        return 1;
    }

    private static int runLogRecent(
        CommandSourceStack source,
        WalletService walletService,
        EconomyConfig economyConfig,
        int count
    ) {
        var entries = walletService.transactionLog().readRecent(count);
        if (entries.isEmpty()) {
            send(source, "No economy ledger entries yet.");
            return 1;
        }
        send(source, "Recent economy entries (" + entries.size() + "):");
        for (LedgerEntry entry : entries) {
            send(source, formatEntry(entry, economyConfig, true));
        }
        return 1;
    }

    private static int runLogPlayer(
        CommandSourceStack source,
        WalletService walletService,
        EconomyConfig economyConfig,
        ServerPlayer target,
        int count
    ) {
        var entries = walletService.transactionLog().readForPlayer(target.getUUID(), count);
        if (entries.isEmpty()) {
            send(source, "No economy ledger entries for " + target.getName().getString() + ".");
            return 1;
        }
        send(source, "Recent economy entries for " + target.getName().getString() + " (" + entries.size() + "):");
        for (LedgerEntry entry : entries) {
            send(source, formatEntry(entry, economyConfig, false));
        }
        return 1;
    }

    private static String formatEntry(LedgerEntry entry, EconomyConfig economyConfig, boolean includePlayerId) {
        String age = formatAge(entry.timestamp());
        String amount = formatSignedAmount(entry.delta(), economyConfig);
        StringBuilder line = new StringBuilder()
            .append(age)
            .append("  ");
        if (includePlayerId) {
            line.append(shortUuid(entry.playerId().toString())).append("  ");
        }
        line.append(reasonLabel(entry.reason()))
            .append("  ")
            .append(amount)
            .append(" -> ")
            .append(economyConfig.format(entry.balanceAfter()));
        String note = formatNote(entry.note());
        if (!note.isEmpty()) {
            line.append("  · ").append(note);
        }
        return line.toString();
    }

    private static String formatAge(Instant timestamp) {
        long seconds = Math.max(0L, Duration.between(timestamp, Instant.now()).getSeconds());
        if (seconds < 60L) {
            return seconds + "s";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + "m";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "h";
        }
        long days = hours / 24L;
        return days + "d";
    }

    private static String formatSignedAmount(long amount, EconomyConfig economyConfig) {
        String sign = amount >= 0L ? "+" : "-";
        return sign + economyConfig.format(Math.abs(amount));
    }

    private static String reasonLabel(TransactionReason reason) {
        return switch (reason) {
            case PLAYER_PAY_FEE -> "PAY_FEE";
            case JOIN_STARTING_BALANCE -> "JOIN_START";
            default -> reason.name();
        };
    }

    private static String formatNote(String note) {
        if (note == null || note.isBlank()) {
            return "";
        }
        if (note.startsWith("to:")) {
            return "to:" + shortUuid(note.substring(3));
        }
        if (note.startsWith("from:")) {
            return "from:" + shortUuid(note.substring(5));
        }
        return note;
    }

    private static String shortUuid(String value) {
        return value.length() <= 8 ? value : value.substring(0, 8);
    }

    private static String formatDisabledLong(EconomyConfig economyConfig, long value) {
        return value <= 0L ? "off" : economyConfig.format(value);
    }

    private static String formatDisabledInt(int value, String suffix) {
        return value <= 0 ? "off" : value + suffix;
    }

    private static void send(CommandSourceStack source, String text) {
        Component line = Component.literal(text);
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(line);
        } else {
            source.sendSuccess(() -> line, false);
        }
    }
}
