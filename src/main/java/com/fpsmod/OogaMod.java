package com.fpsmod;

import com.fpsmod.command.EconomyCommand;
import com.fpsmod.command.GuideCommand;
import com.fpsmod.command.GuildCommand;
import com.fpsmod.command.JobCommand;
import com.fpsmod.command.MoneyCommand;
import com.fpsmod.command.OogaCommand;
import com.fpsmod.command.OtterCommand;
import com.fpsmod.command.PayCommand;
import com.fpsmod.config.ConfigReadmeWriter;
import com.fpsmod.economy.EconomyConfig;
import com.fpsmod.economy.EconomyConfigLoader;
import com.fpsmod.economy.WalletService;
import com.fpsmod.guilds.GuildConfigLoader;
import com.fpsmod.jobs.JobsConfigLoader;
import com.fpsmod.persistence.PersistenceService;
import com.fpsmod.guilds.GuildProtection;
import com.fpsmod.guilds.GuildService;
import com.fpsmod.guilds.net.GuildNetworking;
import com.fpsmod.jobs.JobsService;
import com.fpsmod.ottersciv.config.JobRewardDiagnostics;
import com.fpsmod.jobs.net.JobsNetworking;
import com.fpsmod.ottersciv.OttersCivGameplay;
import com.fpsmod.ottersciv.config.RewardRules;
import com.fpsmod.ottersciv.config.RewardRulesLoader;
import com.fpsmod.ottersciv.reward.RewardOrchestrator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OogaMod implements ModInitializer {
    /**
     * Mod ID for Fabric Loader and logging. Changed from {@code fpsmod} to
     * {@code project_ooga} so this mod does not conflict with the standalone FPS
     * overlay mod it was originally forked from.
     */
    public static final String MOD_ID = "project_ooga";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static PersistenceService persistence;
    private static WalletService walletService;
    private static JobsService jobsService;
    private static GuildService guildService;
    private static RewardOrchestrator ottersRewardGameplay;
    private static EconomyConfig economyConfig;
    private static final String EMOJI_DEBUG = "🔍";
    private static final String EMOJI_OK = "✅";
    private static final String EMOJI_WARN = "⚠️";

    @Override
    public void onInitialize() {
        logDebug("Starting mod initialization...");

        FabricLoader loader = FabricLoader.getInstance();
        String gameVersion = loader.getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

        String modVersion = loader.getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");

        logDebug("Environment: java=" + System.getProperty("java.version")
            + ", minecraft=" + gameVersion
            + ", modVersion=" + modVersion);

        persistence = new PersistenceService();
        persistence.initialize();
        ConfigReadmeWriter.writeOrUpdate();
        economyConfig = EconomyConfigLoader.loadOrCreate();
        walletService = new WalletService(persistence.walletStore(), persistence.transactionLog(), economyConfig);
        jobsService = new JobsService(persistence.jobsStore(), JobsConfigLoader.loadOrCreate());
        guildService = new GuildService(persistence.guildStore(), walletService, GuildConfigLoader.loadOrCreate());
        GuildProtection.register(guildService);
        RewardRules bootstrapRules = RewardRulesLoader.loadBootstrapRewards();
        ottersRewardGameplay = OttersCivGameplay.register(walletService, bootstrapRules, jobsService);

        // Network wiring (server-side payload type + join sync + post-mutation push).
        JobsNetworking.registerServer(jobsService);
        jobsService.setStatusListener(player -> JobsNetworking.sendStatusFor(jobsService, player));
        GuildNetworking.registerServer(guildService);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            onLogicalServerFullyStarted(server);
            GuildNetworking.broadcastClaims(guildService, server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
            (server, resourceManager, success) -> {
                if (success) {
                    onLogicalServerFullyStarted(server);
                }
            }
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                if (jobsService != null) {
                    jobsService.flushNow();
                }
            } catch (RuntimeException e) {
                LOGGER.error("[otters_civ_revived/jobs] flushNow on shutdown failed", e);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            OtterCommand.register(dispatcher);
            GuideCommand.register(dispatcher, economyConfig, jobsService, guildService);
            MoneyCommand.register(dispatcher, walletService, economyConfig);
            PayCommand.register(dispatcher, walletService, economyConfig);
            EconomyCommand.register(dispatcher, walletService, economyConfig);
            JobCommand.register(dispatcher, jobsService);
            GuildCommand.register(dispatcher, guildService);
            OogaCommand.register(dispatcher, persistence.database(), persistence.migrator());
        });

        // Keep this startup heartbeat obvious so template users can quickly confirm load order.
        LOGGER.info("{} {} loaded and ready.", EMOJI_OK, MOD_ID);

        if (!loader.isDevelopmentEnvironment()) {
            LOGGER.warn("{} Running outside dev environment.", EMOJI_WARN);
        } else {
            logDebug("Running in development environment.");
        }
    }

    private static void logDebug(String message) {
        LOGGER.info("{} {}", EMOJI_DEBUG, message);
    }

    private static void onLogicalServerFullyStarted(MinecraftServer server) {
        try {
            if (economyConfig != null && walletService != null) {
                economyConfig.copyFrom(EconomyConfigLoader.loadOrCreate());
                walletService.setEconomyConfig(economyConfig);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[otters_civ_revived/economy] Economy config hydrate failed — pay rules may be stale.", e);
        }
        try {
            RewardRules finalized = RewardRulesLoader.finalizeRewardsForRunningServer(server);
            if (ottersRewardGameplay != null) {
                ottersRewardGameplay.replaceRules(finalized);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[otters_civ_revived] Reward config hydrate failed — mining/kill payouts may be incomplete.", e);
        }
        try {
            if (jobsService != null) {
                jobsService.refresh();
                // Surface diagnostics formerly inside JobsService — now cross-module utility
                for (String line : JobRewardDiagnostics.diagnostics(jobsService, server)) {
                    LOGGER.warn("[otters_civ_revived/jobs/surface] {}", line);
                }
                JobsNetworking.broadcastCatalogAndStatuses(jobsService, server);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[otters_civ_revived/jobs] Jobs config hydrate failed — job tags/progression may be stale.", e);
        }
    }

    public static WalletService walletService() {
        return walletService;
    }

    public static JobsService jobsService() {
        return jobsService;
    }

    public static GuildService guildService() {
        return guildService;
    }
}
