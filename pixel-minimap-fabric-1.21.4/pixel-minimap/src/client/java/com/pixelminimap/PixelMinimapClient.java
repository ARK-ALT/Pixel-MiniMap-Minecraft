package com.pixelminimap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class PixelMinimapClient implements ClientModInitializer {

    // Singleton scanner & renderer — shared across the session
    private static final MinimapScanner SCANNER  = new MinimapScanner();
    private static final MinimapRenderer RENDERER = new MinimapRenderer();

    @Override
    public void onInitializeClient() {
        // ── Register key bindings ─────────────────────────────────────────────
        KeyBindings.register();

        // ── Client tick: process keys + run incremental world scan ────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeyBindings.tick(client);

            if (client.world == null || client.player == null) return;

            // Auto-switch tab to match current dimension
            MapState.syncDimension(client);

            // Feed the scanner one batch of rows
            SCANNER.tick(
                    client.world,
                    MapState.getCurrentTab(),
                    client.player.getBlockPos()
            );
        });

        // ── HUD render: draw the minimap every frame ──────────────────────────
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!MapState.isVisible()) return;
            if (client.world == null || client.player == null) return;
            // Hide while any screen (inventory, pause menu, etc.) is open
            if (client.currentScreen != null) return;

            RENDERER.render(drawContext, client, SCANNER);
        });

        PixelMinimapMod.LOGGER.info("[PixelMinimap] Client initialized. M=toggle, N=tab, B=waypoint.");
    }
}
