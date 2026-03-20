package com.pixelminimap;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyBinding toggleMap;
    public static KeyBinding cycleTab;
    public static KeyBinding addWaypoint;
    public static KeyBinding zoomIn;
    public static KeyBinding zoomOut;

    public static void register() {
        toggleMap = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pixelminimap.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.pixelminimap"
        ));
        cycleTab = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pixelminimap.cycletab",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.pixelminimap"
        ));
        addWaypoint = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pixelminimap.addwaypoint",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.pixelminimap"
        ));
        zoomIn = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pixelminimap.zoomin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                "category.pixelminimap"
        ));
        zoomOut = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pixelminimap.zoomout",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                "category.pixelminimap"
        ));
    }

    public static void tick(MinecraftClient client) {
        while (toggleMap.wasPressed())  MapState.toggleVisible();
        while (cycleTab.wasPressed())   MapState.cycleTab();
        while (zoomIn.wasPressed())     MapState.zoomIn();
        while (zoomOut.wasPressed())    MapState.zoomOut();
        while (addWaypoint.wasPressed()) {
            if (client.player != null) {
                int idx = WaypointManager.getAll().size() + 1;
                WaypointManager.add(new Waypoint(
                        "Waypoint " + idx,
                        client.player.getBlockPos(),
                        MapState.getCurrentTab(),
                        0xFFFFD700
                ));
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("[PixelMap] Waypoint " + idx + " saved!"),
                        true
                );
            }
        }
    }
}
