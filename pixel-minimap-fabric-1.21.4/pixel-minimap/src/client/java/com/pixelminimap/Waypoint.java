package com.pixelminimap;

import net.minecraft.util.math.BlockPos;

public class Waypoint {

    public String name;
    public BlockPos pos;
    public MapState.MapTab dimension;
    /** ARGB color, e.g. 0xFFFFD700 for gold */
    public int color;

    public Waypoint(String name, BlockPos pos, MapState.MapTab dimension, int color) {
        this.name = name;
        this.pos = pos;
        this.dimension = dimension;
        this.color = color;
    }

    /** Distance in blocks from another position (horizontal only) */
    public int distanceTo(BlockPos other) {
        int dx = pos.getX() - other.getX();
        int dz = pos.getZ() - other.getZ();
        return (int) Math.sqrt(dx * dx + dz * dz);
    }
}
