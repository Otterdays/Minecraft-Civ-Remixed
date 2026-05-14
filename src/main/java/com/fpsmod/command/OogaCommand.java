package com.fpsmod.command;

import com.fpsmod.persistence.SchemaMigrator;
import com.fpsmod.persistence.SqliteDatabase;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public final class OogaCommand {
    private static final Permission ADMIN = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private OogaCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, SqliteDatabase db, SchemaMigrator migrator) {
        dispatcher.register(Commands.literal("ooga")
            .requires(source -> source.permissions().hasPermission(ADMIN))
            .then(Commands.literal("db")
                .then(Commands.literal("status")
                    .executes(ctx -> runDbStatus(ctx.getSource(), db, migrator)))
                .then(Commands.literal("migrate")
                    .executes(ctx -> runDbMigrate(ctx.getSource(), migrator)))
            )
        );
    }

    private static int runDbStatus(CommandSourceStack source, SqliteDatabase db, SchemaMigrator migrator) {
        int version = migrator.readVersion();
        String path = "config/otters_civ_revived/project_ooga.db";
        source.sendSuccess(() -> Component.literal(
            "§6Database status:§r\n"
                + "  Path: " + path + "\n"
                + "  Schema version: " + version
                + (version == 1 ? " (latest)" : "")
        ), false);
        return 1;
    }

    private static int runDbMigrate(CommandSourceStack source, SchemaMigrator migrator) {
        try {
            migrator.migrate();
            source.sendSuccess(() -> Component.literal("Schema migration completed."), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Migration failed: " + e.getMessage()));
            return 0;
        }
    }
}
