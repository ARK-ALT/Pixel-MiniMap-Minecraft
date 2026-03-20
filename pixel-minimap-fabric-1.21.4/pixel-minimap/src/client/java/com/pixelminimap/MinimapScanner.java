package com.pixelminimap;

import net.minecraft.block.*;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.EnumMap;
import java.util.Map;

/**
 * Scans blocks from the ClientWorld and caches ARGB colors for each MapTab.
 * Uses incremental row-by-row scanning spread across ticks to avoid frame drops.
 */
public class MinimapScanner {

    /** Maximum scan radius (blocks from center). Must be >= MapState.radius. */
    public static final int MAX_RADIUS = 64;
    public static final int CACHE_SIZE = MAX_RADIUS * 2; // 128

    /** Rows scanned per tick (tune for performance vs freshness). */
    private static final int ROWS_PER_TICK = 8;

    // ── Per-dimension caches ─────────────────────────────────────────────────
    private final Map<MapState.MapTab, int[]> colorCache = new EnumMap<>(MapState.MapTab.class);
    private final Map<MapState.MapTab, int[]> elevCache  = new EnumMap<>(MapState.MapTab.class);

    /** Tracks which row we are currently scanning (0..CACHE_SIZE-1). */
    private int scanRow = 0;

    /** The world-centre used for the current scan cycle. Reset on significant player movement. */
    private BlockPos scanCenter = null;

    public MinimapScanner() {
        for (MapState.MapTab tab : MapState.MapTab.values()) {
            colorCache.put(tab, new int[CACHE_SIZE * CACHE_SIZE]);
            elevCache.put(tab,  new int[CACHE_SIZE * CACHE_SIZE]);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public void tick(ClientWorld world, MapState.MapTab currentTab, BlockPos playerPos) {
        // Re-anchor scan when player moves more than 8 blocks
        if (scanCenter == null
                || Math.abs(playerPos.getX() - scanCenter.getX()) > 8
                || Math.abs(playerPos.getZ() - scanCenter.getZ()) > 8) {
            scanCenter = playerPos;
            scanRow = 0;
        }
        scanRows(world, currentTab, ROWS_PER_TICK);
    }

    public int[] getColors(MapState.MapTab tab) {
        return colorCache.get(tab);
    }

    public int[] getElevs(MapState.MapTab tab) {
        return elevCache.get(tab);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal scanning
    // ─────────────────────────────────────────────────────────────────────────

    private void scanRows(ClientWorld world, MapState.MapTab tab, int rowCount) {
        if (scanCenter == null) return;
        int[] colors = colorCache.get(tab);
        int[] elevs  = elevCache.get(tab);

        int px = scanCenter.getX();
        int py = scanCenter.getBlockY();
        int pz = scanCenter.getZ();

        for (int r = 0; r < rowCount; r++) {
            int dzOffset = scanRow - MAX_RADIUS; // maps [0..127] → [-64..63]
            int wz = pz + dzOffset;

            for (int dxOffset = -MAX_RADIUS; dxOffset < MAX_RADIUS; dxOffset++) {
                int wx = px + dxOffset;
                int idx = scanRow * CACHE_SIZE + (dxOffset + MAX_RADIUS);
                if (idx < 0 || idx >= colors.length) continue;

                switch (tab) {
                    case SURFACE     -> fillSurface    (world, wx, wz,     idx, colors, elevs);
                    case UNDERGROUND -> fillUnderground(world, wx, py, wz, idx, colors, elevs);
                    case NETHER      -> fillNether     (world, wx, wz,     idx, colors, elevs);
                    case END         -> fillEnd        (world, wx, wz,     idx, colors, elevs);
                }
            }

            scanRow = (scanRow + 1) % CACHE_SIZE;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-dimension fill logic
    // ─────────────────────────────────────────────────────────────────────────

    private void fillSurface(ClientWorld world, int wx, int wz,
                             int idx, int[] colors, int[] elevs) {
        try {
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, wx, wz);
            BlockPos pos = new BlockPos(wx, topY - 1, wz);
            BlockState state = world.getBlockState(pos);
            colors[idx] = surfaceColor(world, state, pos);
            elevs[idx]  = topY;
        } catch (Exception ignored) {
            colors[idx] = 0xFF222222;
            elevs[idx]  = 64;
        }
    }

    private void fillUnderground(ClientWorld world, int wx, int py, int wz,
                                 int idx, int[] colors, int[] elevs) {
        try {
            // Scan 3-block vertical window centred on player Y to catch ores on adjacent layers
            int bestColor = 0xFF444444;
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos pos   = new BlockPos(wx, py + dy, wz);
                BlockState state = world.getBlockState(pos);
                int c = undergroundColor(state, py + dy);
                if (c != 0xFF444444 && c != 0xFF3A3A48) { bestColor = c; break; }
                if (dy == 1) bestColor = c; // fallback to stone shade
            }
            colors[idx] = bestColor;
            elevs[idx]  = py;
        } catch (Exception ignored) {
            colors[idx] = 0xFF333333;
        }
    }

    private void fillNether(ClientWorld world, int wx, int wz,
                            int idx, int[] colors, int[] elevs) {
        try {
            // Scan downward from Y=110 to find the first solid non-ceiling block
            for (int y = 110; y >= 4; y--) {
                BlockPos pos   = new BlockPos(wx, y, wz);
                BlockState state = world.getBlockState(pos);
                if (!state.isAir()) {
                    colors[idx] = netherColor(state);
                    elevs[idx]  = y;
                    return;
                }
            }
            colors[idx] = 0xFF1A0000;
        } catch (Exception ignored) {
            colors[idx] = 0xFF1A0000;
        }
    }

    private void fillEnd(ClientWorld world, int wx, int wz,
                         int idx, int[] colors, int[] elevs) {
        try {
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, wx, wz);
            BlockPos pos   = new BlockPos(wx, topY - 1, wz);
            BlockState state = world.getBlockState(pos);
            colors[idx] = endColor(state);
            elevs[idx]  = topY;
        } catch (Exception ignored) {
            colors[idx] = 0xFF080010;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Color lookup helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int surfaceColor(ClientWorld world, BlockState state, BlockPos pos) {
        Block b = state.getBlock();

        // Biome-tinted blocks
        if (b == Blocks.GRASS_BLOCK)
            return 0xFF000000 | BiomeColors.getGrassColor(world, pos);
        if (b instanceof LeavesBlock || b == Blocks.VINE || b == Blocks.GLOW_LICHEN)
            return 0xFF000000 | BiomeColors.getFoliageColor(world, pos);
        if (!state.getFluidState().isEmpty() || b == Blocks.WATER || b == Blocks.BUBBLE_COLUMN)
            return 0xFF000000 | BiomeColors.getWaterColor(world, pos);

        // Grass-colored surface plants
        if (b == Blocks.SHORT_GRASS || b == Blocks.TALL_GRASS || b == Blocks.FERN || b == Blocks.LARGE_FERN)
            return 0xFF000000 | BiomeColors.getGrassColor(world, pos);

        return staticColor(b);
    }

    private int undergroundColor(BlockState state, int y) {
        Block b = state.getBlock();
        // Ores – bright highlight so they're easy to spot
        if (b == Blocks.DIAMOND_ORE   || b == Blocks.DEEPSLATE_DIAMOND_ORE)  return 0xFF55FFFF;
        if (b == Blocks.GOLD_ORE      || b == Blocks.DEEPSLATE_GOLD_ORE)     return 0xFFFFD700;
        if (b == Blocks.IRON_ORE      || b == Blocks.DEEPSLATE_IRON_ORE)     return 0xFFCCAA88;
        if (b == Blocks.REDSTONE_ORE  || b == Blocks.DEEPSLATE_REDSTONE_ORE) return 0xFFFF3333;
        if (b == Blocks.EMERALD_ORE   || b == Blocks.DEEPSLATE_EMERALD_ORE)  return 0xFF55FF55;
        if (b == Blocks.COPPER_ORE    || b == Blocks.DEEPSLATE_COPPER_ORE)   return 0xFFFF9933;
        if (b == Blocks.COAL_ORE      || b == Blocks.DEEPSLATE_COAL_ORE)     return 0xFF333333;
        if (b == Blocks.LAPIS_ORE     || b == Blocks.DEEPSLATE_LAPIS_ORE)    return 0xFF3366CC;
        if (b == Blocks.AMETHYST_CLUSTER || b == Blocks.BUDDING_AMETHYST)    return 0xFFAA55FF;
        if (b == Blocks.ANCIENT_DEBRIS)                                        return 0xFF8B4513;
        if (b == Blocks.LAVA)                                                  return 0xFFFF6600;
        if (b == Blocks.WATER)                                                 return 0xFF1A6ECC;
        if (state.isAir())                                                     return 0xFF111111; // tunnel
        // Stone vs deepslate layer
        return y < 0 ? 0xFF3A3A48 : 0xFF666666;
    }

    private int netherColor(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.NETHERRACK)                                     return 0xFF702222;
        if (b == Blocks.CRIMSON_NYLIUM)                                 return 0xFF992222;
        if (b == Blocks.WARPED_NYLIUM)                                  return 0xFF226666;
        if (b == Blocks.SOUL_SAND || b == Blocks.SOUL_SOIL)            return 0xFF4A3818;
        if (b == Blocks.BASALT || b == Blocks.BLACKSTONE)              return 0xFF282830;
        if (b == Blocks.NETHER_BRICKS || b == Blocks.RED_NETHER_BRICKS)return 0xFF2C1010;
        if (b == Blocks.GLOWSTONE)                                      return 0xFFFFCC44;
        if (b == Blocks.LAVA)                                           return 0xFFFF6600;
        if (b == Blocks.MAGMA_BLOCK)                                    return 0xFFCC4400;
        if (b == Blocks.ANCIENT_DEBRIS)                                 return 0xFF8B4513;
        if (b == Blocks.NETHER_QUARTZ_ORE)                              return 0xFFDDDDCC;
        if (b == Blocks.NETHER_GOLD_ORE)                                return 0xFFFFCC00;
        if (b == Blocks.CRIMSON_STEM || b == Blocks.STRIPPED_CRIMSON_STEM) return 0xFF8B1C1C;
        if (b == Blocks.WARPED_STEM  || b == Blocks.STRIPPED_WARPED_STEM)  return 0xFF1C6060;
        if (b == Blocks.BEDROCK)                                        return 0xFF222222;
        return 0xFF5C1A1A;
    }

    private int endColor(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.END_STONE || b == Blocks.END_STONE_BRICKS)     return 0xFFDDDAA8;
        if (b == Blocks.PURPUR_BLOCK || b == Blocks.PURPUR_PILLAR
                || b == Blocks.PURPUR_STAIRS || b == Blocks.PURPUR_SLAB) return 0xFFAA66AA;
        if (b == Blocks.OBSIDIAN)                                       return 0xFF0E0A1E;
        if (b == Blocks.CHORUS_PLANT || b == Blocks.CHORUS_FLOWER)     return 0xFF8855AA;
        if (b == Blocks.BEDROCK)                                        return 0xFF222222;
        if (state.isAir())                                              return 0xFF080010; // void
        return 0xFF9A9870;
    }

    /** Fallback static color table for common blocks. */
    private int staticColor(Block b) {
        if (b == Blocks.STONE || b == Blocks.COBBLESTONE || b == Blocks.MOSSY_COBBLESTONE) return 0xFF888888;
        if (b == Blocks.GRAVEL)           return 0xFF888880;
        if (b == Blocks.SAND || b == Blocks.SANDSTONE || b == Blocks.CHISELED_SANDSTONE) return 0xFFDDCC88;
        if (b == Blocks.RED_SAND || b == Blocks.RED_SANDSTONE) return 0xFFCC7744;
        if (b == Blocks.DIRT || b == Blocks.COARSE_DIRT || b == Blocks.ROOTED_DIRT) return 0xFF8B6440;
        if (b == Blocks.PODZOL)           return 0xFF6B5020;
        if (b == Blocks.MYCELIUM)         return 0xFF8B6090;
        if (b == Blocks.CLAY)             return 0xFF9BAAB6;
        if (b == Blocks.FARMLAND || b == Blocks.DIRT_PATH) return 0xFF997755;
        if (b == Blocks.SNOW_BLOCK || b == Blocks.POWDER_SNOW) return 0xFFE8F0F8;
        if (b == Blocks.ICE || b == Blocks.PACKED_ICE) return 0xFF88AADD;
        if (b == Blocks.BLUE_ICE)         return 0xFF6699FF;
        if (b == Blocks.LAVA)             return 0xFFFF6600;
        if (b == Blocks.OBSIDIAN)         return 0xFF1A0A2E;
        if (b == Blocks.BEDROCK)          return 0xFF333333;
        // Logs / wood
        if (b == Blocks.OAK_LOG || b == Blocks.OAK_PLANKS)             return 0xFF8B7040;
        if (b == Blocks.BIRCH_LOG || b == Blocks.BIRCH_PLANKS)         return 0xFFCCBB88;
        if (b == Blocks.SPRUCE_LOG || b == Blocks.SPRUCE_PLANKS)       return 0xFF664422;
        if (b == Blocks.DARK_OAK_LOG || b == Blocks.DARK_OAK_PLANKS)  return 0xFF3C2810;
        if (b == Blocks.JUNGLE_LOG || b == Blocks.JUNGLE_PLANKS)       return 0xFF887733;
        if (b == Blocks.ACACIA_LOG || b == Blocks.ACACIA_PLANKS)       return 0xFF776655;
        if (b == Blocks.MANGROVE_LOG || b == Blocks.MANGROVE_PLANKS)   return 0xFF6B3020;
        if (b == Blocks.CHERRY_LOG || b == Blocks.CHERRY_PLANKS)       return 0xFFDD99AA;
        // Terracotta
        if (b == Blocks.TERRACOTTA)       return 0xFFBB8B60;
        if (b == Blocks.WHITE_TERRACOTTA) return 0xFFD8C8B0;
        if (b == Blocks.RED_TERRACOTTA)   return 0xFFAA4422;
        if (b == Blocks.ORANGE_TERRACOTTA)return 0xFFCC8844;
        if (b == Blocks.YELLOW_TERRACOTTA)return 0xFFBBAA44;
        if (b == Blocks.BROWN_TERRACOTTA) return 0xFF7B5030;
        // Surface plants
        if (b == Blocks.DEAD_BUSH)        return 0xFF887744;
        if (b == Blocks.CACTUS)           return 0xFF228822;
        if (b == Blocks.SUGAR_CANE)       return 0xFF55AA44;
        if (b == Blocks.LILY_PAD)         return 0xFF226622;
        // Crops
        if (b == Blocks.WHEAT || b == Blocks.CARROTS
                || b == Blocks.POTATOES || b == Blocks.BEETROOTS) return 0xFFCCBB44;
        // Nether
        if (b == Blocks.NETHERRACK)       return 0xFF702222;
        // Default
        return 0xFF888888;
    }
}
