package com.fpsmod.client;

import com.fpsmod.client.ui.OttersCivScreen;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

/**
 * Client-side {@code /otter} — opens the stylized in-game hub instead of dumping chat text.
 * Takes precedence over the server-side fallback when this mod is installed locally.
 */
public final class OtterClientCommand {
    private OtterClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
            dispatcher.register(ClientCommands.literal("otter").executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> mc.setScreen(new OttersCivScreen()));
                return Command.SINGLE_SUCCESS;
            }))
        );
    }
}
