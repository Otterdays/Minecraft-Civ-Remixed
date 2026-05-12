package com.fpsmod;

import com.fpsmod.command.MoneyCommand;
import com.fpsmod.command.OtterCommand;
import com.fpsmod.economy.WalletService;
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

public class FpsMod implements ModInitializer {
    public static final String MOD_ID = "fpsmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static WalletService walletService;
    private static RewardOrchestrator ottersRewardGameplay;
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

        walletService = WalletService.createDefault();
        RewardRules bootstrapRules = RewardRulesLoader.loadBootstrapRewards();
        ottersRewardGameplay = OttersCivGameplay.register(walletService, bootstrapRules);

        ServerLifecycleEvents.SERVER_STARTED.register(FpsMod::onLogicalServerFullyStarted);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
            (server, resourceManager, success) -> {
                if (success) {
                    onLogicalServerFullyStarted(server);
                }
            }
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            OtterCommand.register(dispatcher);
            MoneyCommand.register(dispatcher, walletService);
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
            RewardRules finalized = RewardRulesLoader.finalizeRewardsForRunningServer(server);
            if (ottersRewardGameplay != null) {
                ottersRewardGameplay.replaceRules(finalized);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[otters_civ_revived] Reward config hydrate failed — mining/kill payouts may be incomplete.", e);
        }
    }

    public static WalletService walletService() {
        return walletService;
    }
}
