package com.fpsmod.command;

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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, WalletService walletService) {
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
        );
    }

    private static int runSelfBalance(CommandSourceStack source, WalletService walletService) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run this as a player in-world."));
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
}
