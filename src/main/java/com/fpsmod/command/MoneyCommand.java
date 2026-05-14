package com.fpsmod.command;

import com.fpsmod.economy.WalletService;
import com.fpsmod.economy.WalletService.TransferResult;
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
    /**
     * Same band as many built-in cheat-style commands (gamemaster / OP in {@code ops.json} terms).
     * Future: swap for mod-specific permission atoms (e.g. LuckPerms/Fabric Permissions API integration).
     */
    private static final Permission BALANCE_MUTATION =
        new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private MoneyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, WalletService walletService) {
        // /money [set|add|take]
        dispatcher.register(Commands.literal("money")
            .executes(context -> runSelfBalance(context.getSource(), walletService))
            .then(Commands.literal("set")
                .requires(source -> source.permissions().hasPermission(BALANCE_MUTATION))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(context -> runSetBalance(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "target"),
                            LongArgumentType.getLong(context, "amount"),
                            walletService
                        ))
                    )
                )
            )
            .then(Commands.literal("add")
                .requires(source -> source.permissions().hasPermission(BALANCE_MUTATION))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(context -> runAdminAdd(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "target"),
                            LongArgumentType.getLong(context, "amount"),
                            walletService
                        ))
                    )
                )
            )
            .then(Commands.literal("take")
                .requires(source -> source.permissions().hasPermission(BALANCE_MUTATION))
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(context -> runAdminTake(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "target"),
                            LongArgumentType.getLong(context, "amount"),
                            walletService
                        ))
                    )
                )
            )
        );

        // /pay <player> <amount>
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                    .executes(context -> runPay(
                        context.getSource(),
                        EntityArgument.getPlayer(context, "target"),
                        LongArgumentType.getLong(context, "amount"),
                        walletService
                    ))
                )
            )
        );
    }

    private static int runSelfBalance(CommandSourceStack source, WalletService walletService) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal(
                "Run this as a player in-world. Operators: /money set requires gamemaster-level command permission."));
            return 0;
        }
        walletService.touchPlayerLabelForOps(player.getUUID(), player.getName().getString());
        long balance = walletService.getBalance(player.getUUID());
        player.sendSystemMessage(Component.literal("Balance: $" + balance));
        return 1;
    }

    private static int runSetBalance(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService
    ) {
        walletService.rememberPlayerName(target.getUUID(), target.getName().getString());
        long updated = walletService.setBalance(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal("Set " + target.getName().getString() + " balance to $" + updated), true);
        target.sendSystemMessage(Component.literal("Your balance was set to $" + updated));
        return 1;
    }

    private static int runAdminAdd(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService
    ) {
        walletService.rememberPlayerName(target.getUUID(), target.getName().getString());
        long updated = walletService.adminAdd(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal("Added $" + amount + " to " + target.getName().getString() + " → $" + updated), true);
        target.sendSystemMessage(Component.literal("An admin added $" + amount + " to your balance. New balance: $" + updated));
        return 1;
    }

    private static int runAdminTake(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService
    ) {
        walletService.rememberPlayerName(target.getUUID(), target.getName().getString());
        long updated = walletService.adminTake(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal("Took up to $" + amount + " from " + target.getName().getString() + " → $" + updated), true);
        target.sendSystemMessage(Component.literal("An admin adjusted your balance. New balance: $" + updated));
        return 1;
    }

    private static int runPay(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService
    ) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Component.literal("Run /pay as a player in-world."));
            return 0;
        }
        TransferResult result = walletService.transfer(
            sender.getUUID(), sender.getName().getString(),
            target.getUUID(), target.getName().getString(),
            amount
        );
        return switch (result) {
            case OK -> {
                long senderBalance = walletService.getBalance(sender.getUUID());
                sender.sendSystemMessage(Component.literal("Paid $" + amount + " to " + target.getName().getString() + ". Balance: $" + senderBalance));
                target.sendSystemMessage(Component.literal(sender.getName().getString() + " paid you $" + amount + "."));
                yield 1;
            }
            case INSUFFICIENT_FUNDS -> {
                sender.sendSystemMessage(Component.literal("Not enough coins. Balance: $" + walletService.getBalance(sender.getUUID())));
                yield 0;
            }
            case SAME_PLAYER -> {
                sender.sendSystemMessage(Component.literal("You can't pay yourself."));
                yield 0;
            }
        };
    }
}
