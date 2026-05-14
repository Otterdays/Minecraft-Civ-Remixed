package com.fpsmod.command;

import com.fpsmod.economy.EconomyConfig;
import com.fpsmod.economy.WalletService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public final class MoneyCommand {
    private static final Permission BALANCE_MUTATION =
        new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private MoneyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, WalletService walletService, EconomyConfig economyConfig) {
        dispatcher.register(Commands.literal("money")
            .executes(context -> runSelfBalance(context.getSource(), walletService, economyConfig))
            .then(Commands.literal("set")
                .requires(source -> source.permissions().hasPermission(BALANCE_MUTATION))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(context -> runSetBalance(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "target"),
                            LongArgumentType.getLong(context, "amount"),
                            walletService, economyConfig
                        ))
                    )
                )
            )
            .then(Commands.literal("add")
                .requires(source -> source.permissions().hasPermission(BALANCE_MUTATION))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(context -> runAddBalance(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "target"),
                            LongArgumentType.getLong(context, "amount"),
                            walletService, economyConfig
                        ))
                    )
                )
            )
            .then(Commands.literal("take")
                .requires(source -> source.permissions().hasPermission(BALANCE_MUTATION))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(context -> runTakeBalance(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "target"),
                            LongArgumentType.getLong(context, "amount"),
                            walletService, economyConfig
                        ))
                    )
                )
            )
        );
    }

    private static int runSelfBalance(CommandSourceStack source, WalletService walletService, EconomyConfig cfg) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run this as a player in-world."));
            return 0;
        }
        walletService.touchPlayerLabelForOps(player.getUUID(), player.getName().getString());
        long balance = walletService.getBalance(player.getUUID());
        player.sendSystemMessage(Component.literal("Balance: " + cfg.format(balance)));
        return 1;
    }

    private static int runSetBalance(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService,
        EconomyConfig cfg
    ) {
        walletService.rememberPlayerName(target.getUUID(), target.getName().getString());
        long updated = walletService.setBalance(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal("Set " + target.getName().getString() + " balance to " + cfg.format(updated)), true);
        target.sendSystemMessage(Component.literal("Your balance was set to " + cfg.format(updated)));
        return 1;
    }

    private static int runAddBalance(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService,
        EconomyConfig cfg
    ) {
        walletService.rememberPlayerName(target.getUUID(), target.getName().getString());
        long updated = walletService.adminAdd(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal("Added " + cfg.format(amount) + " to " + target.getName().getString() + ". New balance: " + cfg.format(updated)), true);
        target.sendSystemMessage(Component.literal("An admin added " + cfg.format(amount) + " to your balance. New balance: " + cfg.format(updated)));
        return 1;
    }

    private static int runTakeBalance(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService,
        EconomyConfig cfg
    ) {
        walletService.rememberPlayerName(target.getUUID(), target.getName().getString());
        long updated = walletService.adminTake(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal("Took " + cfg.format(amount) + " from " + target.getName().getString() + ". New balance: " + cfg.format(updated)), true);
        target.sendSystemMessage(Component.literal("An admin took " + cfg.format(amount) + " from your balance. New balance: " + cfg.format(updated)));
        return 1;
    }
}
