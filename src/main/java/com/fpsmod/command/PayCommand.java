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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public final class PayCommand {
    private static final Map<UUID, Long> LAST_SUCCESSFUL_PAY_MS = new ConcurrentHashMap<>();

    private PayCommand() {}

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        WalletService walletService,
        EconomyConfig economyConfig
    ) {
        dispatch(dispatcher, walletService, economyConfig);
    }

    public static void dispatch(
        CommandDispatcher<CommandSourceStack> dispatcher,
        WalletService walletService,
        EconomyConfig economyConfig
    ) {
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                    .executes(ctx -> runPay(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "target"),
                        LongArgumentType.getLong(ctx, "amount"),
                        walletService,
                        economyConfig
                    ))
                )
            )
        );
    }

    private static int runPay(
        CommandSourceStack source,
        ServerPlayer target,
        long amount,
        WalletService walletService,
        EconomyConfig economyConfig
    ) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Component.literal("Run this as a player in-world."));
            return 0;
        }
        boolean selfPay = sender.getUUID().equals(target.getUUID());
        if (!economyConfig.allowSelfPay && selfPay) {
            source.sendFailure(Component.literal("You cannot pay yourself."));
            return 0;
        }

        long minAmount = Math.max(1L, economyConfig.minTransferAmount);
        if (amount < minAmount) {
            source.sendFailure(Component.literal("Minimum transfer is " + economyConfig.format(minAmount) + "."));
            return 0;
        }

        long cap = economyConfig.maxTransferPerCommand;
        if (cap > 0L && amount > cap) {
            source.sendFailure(Component.literal("Maximum transfer is " + economyConfig.format(cap) + "."));
            return 0;
        }

        long cooldownSeconds = Math.max(0, economyConfig.transferCooldownSeconds);
        long now = System.currentTimeMillis();
        if (cooldownSeconds > 0L) {
            Long lastSuccessAt = LAST_SUCCESSFUL_PAY_MS.get(sender.getUUID());
            if (lastSuccessAt != null) {
                long elapsedMs = now - lastSuccessAt;
                long cooldownMs = cooldownSeconds * 1000L;
                if (elapsedMs < cooldownMs) {
                    long remainingSeconds = (cooldownMs - elapsedMs + 999L) / 1000L;
                    source.sendFailure(Component.literal(
                        "You must wait " + remainingSeconds + "s before using /pay again."
                    ));
                    return 0;
                }
            }
        }

        long fee = Math.max(0L, economyConfig.transferFlatFee);
        if (selfPay) {
            if (walletService.getBalance(sender.getUUID()) < fee) {
                source.sendFailure(Component.literal(
                    "You need " + economyConfig.format(fee) + " to cover the self-pay fee."
                ));
                return 0;
            }
            if (fee > 0L) {
                walletService.addBalance(
                    sender.getUUID(),
                    -fee,
                    sender.getName().getString(),
                    com.fpsmod.economy.TransactionReason.PLAYER_PAY_FEE
                );
            }
            LAST_SUCCESSFUL_PAY_MS.put(sender.getUUID(), now);
            if (fee > 0L) {
                sender.sendSystemMessage(Component.literal(
                    "You paid yourself " + economyConfig.format(amount)
                        + ". Balance stayed the same except for the "
                        + economyConfig.format(fee) + " fee."
                ));
            } else {
                sender.sendSystemMessage(Component.literal(
                    "You paid yourself " + economyConfig.format(amount) + ". Balance unchanged."
                ));
            }
            return 1;
        }
        long totalCost;
        try {
            totalCost = Math.addExact(amount, fee);
        } catch (ArithmeticException e) {
            source.sendFailure(Component.literal("That transfer is too large."));
            return 0;
        }
        if (walletService.getBalance(sender.getUUID()) < totalCost) {
            source.sendFailure(Component.literal(
                "You need " + economyConfig.format(totalCost) + " total for that payment."
            ));
            return 0;
        }

        var result = walletService.transfer(
            sender.getUUID(), sender.getName().getString(),
            target.getUUID(), target.getName().getString(),
            amount,
            fee
        );

        switch (result) {
            case INSUFFICIENT_FUNDS -> {
                source.sendFailure(Component.literal(
                    "You need " + economyConfig.format(totalCost) + " total for that payment."
                ));
                return 0;
            }
            case RECEIVER_MAX_BALANCE -> {
                if (economyConfig.maxBalance > 0L) {
                    source.sendFailure(Component.literal(
                        target.getName().getString() + " cannot receive that much; max balance is "
                            + economyConfig.format(economyConfig.maxBalance) + "."
                    ));
                } else {
                    source.sendFailure(Component.literal("That player cannot receive that payment."));
                }
                return 0;
            }
            case SAME_PLAYER -> {
                source.sendFailure(Component.literal("You cannot pay yourself."));
                return 0;
            }
            case OK -> {
                LAST_SUCCESSFUL_PAY_MS.put(sender.getUUID(), now);
                String amountText = economyConfig.format(amount);
                if (fee > 0L) {
                    sender.sendSystemMessage(Component.literal(
                        "You paid " + amountText + " to " + target.getName().getString()
                            + " (" + economyConfig.format(fee) + " fee, total "
                            + economyConfig.format(totalCost) + ")."
                    ));
                } else {
                    sender.sendSystemMessage(Component.literal(
                        "You paid " + amountText + " to " + target.getName().getString() + "."
                    ));
                }
                target.sendSystemMessage(Component.literal(
                    sender.getName().getString() + " paid you " + amountText + "."
                ));
                return 1;
            }
        }
        return 0;
    }
}
