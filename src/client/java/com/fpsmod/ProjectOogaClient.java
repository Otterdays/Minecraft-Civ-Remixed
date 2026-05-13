package com.fpsmod;

import com.fpsmod.client.OtterClientCommand;
import com.fpsmod.client.jobs.JobsClientNetworking;
import com.fpsmod.client.jobs.JobsHudOverlay;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client initializer for Project OOGA.
 *
 * <p>Renamed from {@code FpsModClient} to avoid class name collision with the
 * standalone FPS overlay mod (which also defines {@code com.fpsmod.FpsModClient}).
 * Fabric Loader's Knot classloader uses parent-first delegation, so with two jars
 * defining the same class, the wrong one's code can run.
 *
 * <p>The legacy FPS HUD overlay ({@code FpsHudOverlay}, {@code FpsHudScreenButton})
 * is <strong>deprecated and disabled</strong> — the standalone FPS overlay mod
 * (the original template this project was forked from) handles FPS display.
 * Having both active causes duplicate overlay conflicts.
 */
public class ProjectOogaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OtterClientCommand.register();
        JobsClientNetworking.register();
        JobsHudOverlay.register();
    }
}
