package com.fpsmod.command;

import com.fpsmod.economy.EconomyConfig;
import com.fpsmod.guilds.GuildService;
import com.fpsmod.jobs.JobsService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public final class GuideCommand {
    private static final Permission ADMIN = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private GuideCommand() {
    }

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        EconomyConfig economyConfig,
        JobsService jobsService,
        GuildService guildService
    ) {
        dispatcher.register(
            Commands.literal("guide")
                .executes(ctx -> runSelf(ctx.getSource(), economyConfig, jobsService, guildService))
                .then(
                    Commands.literal("give")
                        .requires(source -> source.permissions().hasPermission(ADMIN))
                        .then(
                            Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> runGive(
                                    ctx.getSource(),
                                    EntityArgument.getPlayer(ctx, "target"),
                                    economyConfig,
                                    jobsService,
                                    guildService
                                ))
                        )
                )
        );
    }

    private static int runSelf(
        CommandSourceStack source,
        EconomyConfig economyConfig,
        JobsService jobsService,
        GuildService guildService
    ) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run /guide as a player, or use /guide give <player>."));
            return 0;
        }
        boolean addedToInventory = giveBookToInventory(player, economyConfig, jobsService, guildService);
        if (!addedToInventory) {
            source.sendFailure(Component.literal("Make room in your inventory before using /guide."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Otters Civ. handbook added to your inventory."), false);
        return 1;
    }

    private static int runGive(
        CommandSourceStack source,
        ServerPlayer target,
        EconomyConfig economyConfig,
        JobsService jobsService,
        GuildService guildService
    ) {
        boolean addedToInventory = giveBookToInventory(target, economyConfig, jobsService, guildService);
        if (!addedToInventory) {
            dropBook(target, economyConfig, jobsService, guildService);
        }
        source.sendSuccess(
            () -> Component.literal(
                addedToInventory
                    ? "Gave the Otters Civ. handbook to " + target.getName().getString() + "."
                    : "Gave the Otters Civ. handbook to " + target.getName().getString()
                        + ", but their inventory was full so it dropped at their feet."
            ),
            true
        );
        target.sendSystemMessage(Component.literal(
            "An admin gave you the Otters Civ. handbook. Use /guide anytime for another copy."
        ));
        return 1;
    }

    private static boolean giveBookToInventory(
        ServerPlayer target,
        EconomyConfig economyConfig,
        JobsService jobsService,
        GuildService guildService
    ) {
        ItemStack guideBook = GuideBookFactory.create(economyConfig, jobsService, guildService);
        return target.getInventory().add(guideBook);
    }

    private static void dropBook(
        ServerPlayer target,
        EconomyConfig economyConfig,
        JobsService jobsService,
        GuildService guildService
    ) {
        ItemStack guideBook = GuideBookFactory.create(economyConfig, jobsService, guildService);
        target.drop(guideBook, false);
    }
}
