package com.pixelminimap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class MapState {

    public enum MapTab {
        SURFACE, UNDERGROUND, NETHER, END;

        public String displayName() {
            return switch (this) {
                case SURFACE     -> "OVERWORLD";
                case UNDERGROUND -> "UNDERGROUND";
                case NETHER      -> "NETHER";
                case END         -> "THE END";
            };
        }
    }

    // ── Position & size ──────────────────────────────────────────────────────
    /** Top-left corner of the minimap on screen */
    public static final int MAP_X = 4;
    public static final int MAP_Y = 4;

    /** Screen pixels per world block (zoom levels 1–3) */
    public static int pixelSize = 2;

    /** Half-width in blocks of the scanned area */
    public static int radius = 48;

    /** Total pixel width/height of the drawn map area */
    public static int mapDisplaySize() { return radius * 2 * pixelSize; }

    // ── State ─────────────────────────────────────────────────────────────────
    private static boolean visible = true;
    private static MapTab currentTab = MapTab.SURFACE;

    public static boolean isVisible() { return visible; }
    public static void toggleVisible() { visible = !visible; }

    public static MapTab getCurrentTab() { return currentTab; }

    public static void cycleTab() {
        MapTab[] tabs = MapTab.values();
        currentTab = tabs[(currentTab.ordinal() + 1) % tabs.length];
    }

    public static void zoomIn() {
        if (pixelSize < 4) {
            pixelSize++;
            if (pixelSize == 4) radius = 32;
            else if (pixelSize == 3) radius = 40;
        }
    }

    public static void zoomOut() {
        if (pixelSize > 1) {
            pixelSize--;
            if (pixelSize == 1) radius = 64;
            else if (pixelSize == 2) radius = 48;
        }
    }

    /**
     * Auto-sync current tab with the dimension the player is in.
     * Called each client tick so the map switches automatically on dimension change.
     */
    public static void syncDimension(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        RegistryKey<World> dim = client.world.getRegistryKey();
        if (dim == World.NETHER) {
            currentTab = MapTab.NETHER;
        } else if (dim == World.END) {
            currentTab = MapTab.END;
        } else {
            // Stay in UNDERGROUND if user manually selected it and player is underground
            if (currentTab != MapTab.UNDERGROUND) {
                currentTab = MapTab.SURFACE;
            }
        }
    }
}
