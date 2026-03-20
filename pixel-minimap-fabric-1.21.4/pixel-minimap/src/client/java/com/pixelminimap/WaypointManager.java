package com.pixelminimap;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class WaypointManager {

    private static final List<Waypoint> waypoints = new ArrayList<>();

    static {
        // Pre-loaded demo waypoints (one per dimension to showcase all tabs)
        waypoints.add(new Waypoint("Home Base",        new BlockPos(0,   64,  0),   MapState.MapTab.SURFACE,      0xFF55FF55));
        waypoints.add(new Waypoint("Village",          new BlockPos(150, 63,  80),  MapState.MapTab.SURFACE,      0xFFFFD700));
        waypoints.add(new Waypoint("Pillager Outpost", new BlockPos(-90, 70,  120), MapState.MapTab.SURFACE,      0xFFFF5555));
        waypoints.add(new Waypoint("Stronghold",       new BlockPos(300, 30,  -200),MapState.MapTab.SURFACE,      0xFF5599FF));
        waypoints.add(new Waypoint("Nether Portal",    new BlockPos(30,  64,  20),  MapState.MapTab.SURFACE,      0xFFAA55FF));
        waypoints.add(new Waypoint("Diamond Vein",     new BlockPos(44,  -58, 220), MapState.MapTab.UNDERGROUND,  0xFF55FFFF));
        waypoints.add(new Waypoint("Ancient City",     new BlockPos(100, -45, 200), MapState.MapTab.UNDERGROUND,  0xFF00FFAA));
        waypoints.add(new Waypoint("Dungeon",          new BlockPos(80,  35,  90),  MapState.MapTab.UNDERGROUND,  0xFFFF5555));
        waypoints.add(new Waypoint("Nether Fortress",  new BlockPos(340, 60,  110), MapState.MapTab.NETHER,       0xFFFF4444));
        waypoints.add(new Waypoint("Bastion",          new BlockPos(220, 40,  330), MapState.MapTab.NETHER,       0xFFFF9900));
        waypoints.add(new Waypoint("End City",         new BlockPos(1240,63,  880), MapState.MapTab.END,          0xFFAA55FF));
        waypoints.add(new Waypoint("End Ship",         new BlockPos(1280,75,  910), MapState.MapTab.END,          0xFF5599FF));
    }

    public static List<Waypoint> getAll() { return waypoints; }

    public static List<Waypoint> forDimension(MapState.MapTab dim) {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints) {
            if (wp.dimension == dim) result.add(wp);
        }
        return result;
    }

    public static void add(Waypoint wp) { waypoints.add(wp); }

    public static void remove(int index) {
        if (index >= 0 && index < waypoints.size()) waypoints.remove(index);
    }
}
