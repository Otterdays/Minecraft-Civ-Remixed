package com.fpsmod.command;

import com.fpsmod.guilds.ClaimedChunk;
import com.fpsmod.guilds.Guild;
import com.fpsmod.guilds.GuildProtection;
import com.fpsmod.guilds.GuildService;
import com.fpsmod.guilds.net.GuildNetworking;
import com.fpsmod.guilds.net.MapTogglePayload;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Set;

public final class GuildCommand {
    private static final Permission ADMIN = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private GuildCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, GuildService guilds) {
        dispatcher.register(Commands.literal("guild")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> runCreate(ctx.getSource(), guilds, StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("disband")
                .executes(ctx -> runDisband(ctx.getSource(), guilds)))
            .then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> runInvite(ctx.getSource(), guilds, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("join")
                .executes(ctx -> runJoin(ctx.getSource(), guilds)))
            .then(Commands.literal("leave")
                .executes(ctx -> runLeave(ctx.getSource(), guilds)))
            .then(Commands.literal("kick")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> runKick(ctx.getSource(), guilds, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("transfer")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> runTransfer(ctx.getSource(), guilds, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("promote")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> runPromote(ctx.getSource(), guilds, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("demote")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> runDemote(ctx.getSource(), guilds, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("sethome")
                .executes(ctx -> runSetHome(ctx.getSource(), guilds)))
            .then(Commands.literal("home")
                .executes(ctx -> runHome(ctx.getSource(), guilds)))
            .then(Commands.literal("info")
                .executes(ctx -> runInfo(ctx.getSource(), guilds, ctx.getSource().getPlayer())))
            .then(Commands.literal("list")
                .executes(ctx -> runList(ctx.getSource(), guilds)))
            .then(Commands.literal("claim")
                .executes(ctx -> runClaim(ctx.getSource(), guilds)))
            .then(Commands.literal("unclaim")
                .executes(ctx -> runUnclaim(ctx.getSource(), guilds)))
            .then(Commands.literal("unclaimall")
                .executes(ctx -> runUnclaimAll(ctx.getSource(), guilds)))
            .then(Commands.literal("map")
                .executes(ctx -> runMap(ctx.getSource(), guilds)))
            .then(Commands.literal("reload")
                .requires(source -> source.permissions().hasPermission(ADMIN))
                .executes(ctx -> runReload(ctx.getSource(), guilds)))
        );
    }

    private static int runCreate(CommandSourceStack source, GuildService guilds, String name) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        String msg = guilds.createGuild(p, name);
        send(source, msg);
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return msg.contains("created") ? 1 : 0;
    }

    private static int runDisband(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.disbandGuild(p));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return 1;
    }

    private static int runInvite(CommandSourceStack source, GuildService guilds, ServerPlayer target) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        String result = guilds.invitePlayer(p, target);
        send(source, result);
        GuildNetworking.sendGuildStatusTo(guilds, p);
        if (result.startsWith("Invited")) {
            target.sendSystemMessage(Component.literal(
                "You have been invited to join " + guilds.guildByPlayer(p.getUUID()).name
                    + ". Use /guild join to accept."));
        }
        return 1;
    }

    private static int runJoin(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.joinGuild(p));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return 1;
    }

    private static int runLeave(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.leaveGuild(p));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return 1;
    }

    private static int runKick(CommandSourceStack source, GuildService guilds, ServerPlayer target) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.kickPlayer(p, target));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        GuildNetworking.sendGuildStatusTo(guilds, target);
        return 1;
    }

    private static int runTransfer(CommandSourceStack source, GuildService guilds, ServerPlayer target) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.transferOwnership(p, target));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        GuildNetworking.sendGuildStatusTo(guilds, target);
        return 1;
    }

    private static int runPromote(CommandSourceStack source, GuildService guilds, ServerPlayer target) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.promote(p, target));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        GuildNetworking.sendGuildStatusTo(guilds, target);
        return 1;
    }

    private static int runDemote(CommandSourceStack source, GuildService guilds, ServerPlayer target) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.demote(p, target));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        GuildNetworking.sendGuildStatusTo(guilds, target);
        return 1;
    }

    private static int runSetHome(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.setHome(p));
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return 1;
    }

    private static int runHome(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.teleportHome(p));
        return 1;
    }

    private static int runInfo(CommandSourceStack source, GuildService guilds, ServerPlayer target) {
        if (target == null) return 0;
        Guild g = guilds.guildByPlayer(target.getUUID());
        if (g == null) { send(source, "You are not in a guild."); return 0; }
        send(source, "§6=== " + g.name + " ===§r");
        send(source, "Owner: UUID " + g.owner);
        send(source, "Members: " + g.memberCount() + "/" + guilds.config().maxMembers);
        send(source, "Balance: $" + g.balance);
        send(source, "Claims: " + guilds.claimsForGuild(g.id).size() + "/" + guilds.config().maxClaims);
        send(source, "Open: " + (g.open ? "yes" : "invite-only"));
        if (g.homePos != null) send(source, "Home: " + g.homePos.getX() + ", " + g.homePos.getY() + ", " + g.homePos.getZ());
        return 1;
    }

    private static int runList(CommandSourceStack source, GuildService guilds) {
        var all = guilds.allGuilds();
        if (all.isEmpty()) { send(source, "No guilds yet."); return 0; }
        send(source, "§6Guilds (" + all.size() + "):§r");
        for (Guild g : all) {
            send(source, "  " + g.name + " — " + g.memberCount() + " members, " + guilds.claimsForGuild(g.id).size() + " claims");
        }
        return 1;
    }

    private static int runClaim(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        int cx = p.blockPosition().getX() >> 4;
        int cz = p.blockPosition().getZ() >> 4;
        send(source, guilds.claimChunk(p, cx, cz));
        GuildNetworking.sendClaimsTo(guilds, p);
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return 1;
    }

    private static int runUnclaim(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        int cx = p.blockPosition().getX() >> 4;
        int cz = p.blockPosition().getZ() >> 4;
        send(source, guilds.unclaimChunk(p, cx, cz));
        GuildNetworking.sendClaimsTo(guilds, p);
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return 1;
    }

    private static int runUnclaimAll(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        send(source, guilds.unclaimAll(p));
        GuildNetworking.broadcastClaims(guilds, source.getServer());
        GuildNetworking.sendGuildStatusTo(guilds, p);
        return 1;
    }

    private static int runMap(CommandSourceStack source, GuildService guilds) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        int radius = 4;
        int cx = p.blockPosition().getX() >> 4;
        int cz = p.blockPosition().getZ() >> 4;
        Guild playerGuild = guilds.guildByPlayer(p.getUUID());

        send(source, "§6Chunk Map (radius " + radius + "):§r");
        for (int dz = -radius; dz <= radius; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -radius; dx <= radius; dx++) {
                int rx = cx + dx;
                int rz = cz + dz;
                String dim = p.level().dimension().identifier().toString();
                ClaimedChunk claim = guilds.claimAt(rx, rz, dim);
                if (claim == null) {
                    row.append("§7▢ ");
                } else if (playerGuild != null && claim.guildId().equals(playerGuild.id)) {
                    row.append("§a■ ");
                } else {
                    row.append("§c■ ");
                }
            }
            p.sendSystemMessage(Component.literal(row.toString()));
        }
        send(source, "§7▢ §runclaimed  §a■ §ryour guild  §c■ §rother guild");
        send(source, "§7Overlay toggled. (GUI overlay drawn in top-right)");
        send(source, "§7Chunk borders shown as particles (30s).");

        GuildProtection.showChunkBorders(p, cx, cz, radius, p.level().dimension().identifier().toString(), guilds);
        ServerPlayNetworking.send(p, new MapTogglePayload(true));
        return 1;
    }

    private static int runReload(CommandSourceStack source, GuildService guilds) {
        guilds.refresh();
        send(source, "Reloaded guilds.json config.");
        return 1;
    }

    private static void send(CommandSourceStack source, String text) {
        Component line = Component.literal(text);
        if (source.getEntity() instanceof ServerPlayer p) {
            p.sendSystemMessage(line);
        } else {
            source.sendSuccess(() -> line, false);
        }
    }
}
