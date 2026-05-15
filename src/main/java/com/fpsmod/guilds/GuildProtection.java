package com.fpsmod.guilds;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.fpsmod.OogaMod;
import java.util.Objects;
import java.util.Set;

public final class GuildProtection {
    private static final String MSG = "§cThis chunk is claimed by another guild.";
    private static final Set<String> CONTAINER_BLOCKS = Set.of(
        "minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel",
        "minecraft:shulker_box", "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
        "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box",
        "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
        "minecraft:pink_shulker_box", "minecraft:gray_shulker_box",
        "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box",
        "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
        "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
        "minecraft:red_shulker_box", "minecraft:black_shulker_box",
        "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
        "minecraft:hopper", "minecraft:dropper", "minecraft:dispenser",
        "minecraft:brewing_stand", "minecraft:ender_chest",
        "minecraft:jukebox", "minecraft:decorated_pot"
    );

    private GuildProtection() {}

    public static void register(GuildService guildService) {
        Objects.requireNonNull(guildService);

        // Block breaking
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel)) return true;
            if (!(player instanceof ServerPlayer sp)) return true;
            return canModifyBlock(sp, level, pos, guildService);
        });

        // Block placement via item use on block
        BlockEvents.USE_ITEM_ON.register((itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult) -> {
            if (!(level instanceof ServerLevel)) return null;
            if (!(player instanceof ServerPlayer sp)) return null;
            if (!canModifyBlock(sp, level, blockPos, guildService)) {
                return InteractionResult.FAIL;
            }
            return null;
        });

        // Container / interactive block use
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!(level instanceof ServerLevel)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            GuildConfig cfg = guildService.config();

            if (id != null && cfg.protectContainers && CONTAINER_BLOCKS.contains(id.toString())) {
                if (!canAccess(sp, level, pos, guildService)) {
                    return InteractionResult.FAIL;
                }
            }

            if (cfg.protectInteractables) {
                if (!canAccess(sp, level, pos, guildService)) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });

        // Block attack (left-click on block)
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!(level instanceof ServerLevel)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (!canModifyBlock(sp, level, pos, guildService)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    /**
     * Returns true if {@code player} is allowed to break/place blocks at {@code pos}.
     *
     * Precedence:
     *  1. Chunk not claimed → allow (unclaimed land is unprotected).
     *  2. Owning guild missing (orphan claim) → allow (defensive: stale data shouldn't hard-block players).
     *  3. Player is a guild member → honour {@code allowMemberBuild} config flag.
     *  4. Player is not a member → deny.
     *
     * Enable {@code guilds.json debugProtection:true} to log every decision.
     */
    public static boolean canModifyBlock(ServerPlayer player, Level level, BlockPos pos, GuildService guilds) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        String dim = level.dimension().identifier().toString();
        ClaimedChunk claim = guilds.claimAt(cx, cz, dim);
        boolean debug = guilds.config().debugProtection;
        if (claim == null) {
            if (debug) OogaMod.LOGGER.info("[guild-protect] canModify ALLOW unclaimed {},{} dim={} player={}", cx, cz, dim, player.getName().getString());
            return true;
        }
        Guild g = guilds.guildById(claim.guildId());
        if (g == null) {
            if (debug) OogaMod.LOGGER.info("[guild-protect] canModify ALLOW orphan claim {},{} dim={} player={}", cx, cz, dim, player.getName().getString());
            return true;
        }
        if (g.isMember(player.getUUID())) {
            boolean allowed = guilds.config().allowMemberBuild;
            if (debug) OogaMod.LOGGER.info("[guild-protect] canModify {} (member,allowMemberBuild={}) {},{} guild={} player={}", allowed ? "ALLOW" : "DENY", allowed, cx, cz, g.name, player.getName().getString());
            return allowed;
        }
        if (debug) OogaMod.LOGGER.info("[guild-protect] canModify DENY non-member {},{} guild={} player={}", cx, cz, g.name, player.getName().getString());
        return false;
    }

    /**
     * Returns true if {@code player} may interact with containers/interactables at {@code pos}.
     *
     * Precedence:
     *  1. Chunk not claimed → allow.
     *  2. Owning guild missing → allow.
     *  3. Player is a guild member → allow.
     *  4. Non-member → deny.
     *
     * Enable {@code guilds.json debugProtection:true} to log every decision.
     */
    public static boolean canAccess(ServerPlayer player, Level level, BlockPos pos, GuildService guilds) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        String dim = level.dimension().identifier().toString();
        ClaimedChunk claim = guilds.claimAt(cx, cz, dim);
        boolean debug = guilds.config().debugProtection;
        if (claim == null) {
            if (debug) OogaMod.LOGGER.info("[guild-protect] canAccess ALLOW unclaimed {},{} dim={} player={}", cx, cz, dim, player.getName().getString());
            return true;
        }
        Guild g = guilds.guildById(claim.guildId());
        if (g == null) {
            if (debug) OogaMod.LOGGER.info("[guild-protect] canAccess ALLOW orphan claim {},{} dim={} player={}", cx, cz, dim, player.getName().getString());
            return true;
        }
        boolean allowed = g.isMember(player.getUUID());
        if (debug) OogaMod.LOGGER.info("[guild-protect] canAccess {} {},{} guild={} player={}", allowed ? "ALLOW" : "DENY", cx, cz, g.name, player.getName().getString());
        return allowed;
    }

    public static void showChunkBorders(ServerPlayer player, int chunkX, int chunkZ, int radius, String dimension, GuildService guilds) {
        if (!(player.level() instanceof ServerLevel sLevel)) return;
        int px = player.blockPosition().getX();
        int pz = player.blockPosition().getZ();

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                ClaimedChunk claim = guilds.claimAt(cx, cz, dimension);
                if (claim == null) continue;

                int worldX = cx << 4;
                int worldZ = cz << 4;
                int y = sLevel.getMinY() + 1;

                double dist = Math.abs(cx * 16 + 8 - px) + Math.abs(cz * 16 + 8 - pz);
                if (dist > (radius + 1) * 20) continue;

                boolean isOwn = player.getUUID().equals(claim.guildId()) ||
                    (guilds.guildByPlayer(player.getUUID()) != null &&
                     guilds.guildByPlayer(player.getUUID()).id.equals(claim.guildId()));

                var particle = isOwn ? ParticleTypes.END_ROD : ParticleTypes.FLAME;

                for (int cornerX = 0; cornerX <= 16; cornerX += 16) {
                    for (int cornerZ = 0; cornerZ <= 16; cornerZ += 16) {
                        for (int h = 0; h < 4; h++) {
                            sLevel.sendParticles(
                                particle,
                                worldX + cornerX, y + h * 10, worldZ + cornerZ,
                                1, 0, 0, 0, 0.01
                            );
                        }
                    }
                }
            }
        }
    }
}
