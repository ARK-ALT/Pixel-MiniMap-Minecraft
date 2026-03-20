package com.pixelminimap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Draws the pixel-art minimap HUD overlay.
 *
 * Layout (top-left of screen):
 *   ┌──────────────────────┐
 *   │  [map pixels]        │  ← colored block grid
 *   │  [waypoint dots]     │
 *   │  [player arrow]      │
 *   │  [compass top-right] │
 *   ├──────────────────────┤
 *   │ DIM LABEL  COORDS    │  ← bottom info bar
 *   └──────────────────────┘
 */
public class MinimapRenderer {

    // Pixel-art wood/iron UI colors
    private static final int COL_BORDER_OUTER = 0xFF2A1A08;
    private static final int COL_BORDER_MID   = 0xFF5C3A0A;
    private static final int COL_BORDER_INNER = 0xFF8B6914;
    private static final int COL_BG           = 0xCC060402;
    private static final int COL_BAR_BG       = 0xCC0A0806;
    private static final int COL_BAR_BORDER   = 0xFF4A3828;
    private static final int COL_TEXT_GOLD    = 0xFFFFD700;
    private static final int COL_TEXT_CYAN    = 0xFF55FFFF;
    private static final int COL_TEXT_DIM     = 0xFF888888;
    private static final int COL_COMPASS_N    = 0xFFFF4444;
    private static final int COL_COMPASS_S    = 0xFF888888;

    // Info bar height in pixels
    private static final int BAR_H = 22;
    // Border thickness
    private static final int BORDER = 3;

    public void render(DrawContext ctx, MinecraftClient mc, MinimapScanner scanner) {
        if (mc.player == null || mc.world == null) return;

        MapState.MapTab tab = MapState.getCurrentTab();
        int ps     = MapState.pixelSize;
        int radius = MapState.radius;
        int mapPx  = radius * 2 * ps; // pixel dimensions of the drawn map area

        int originX = MapState.MAP_X;
        int originY = MapState.MAP_Y;

        // ── 1. Outer pixel-art frame ─────────────────────────────────────────
        drawFrame(ctx, originX - BORDER, originY - BORDER,
                  mapPx + BORDER * 2, mapPx + BORDER * 2);

        // ── 2. Map pixel grid ────────────────────────────────────────────────
        int[] colors = scanner.getColors(tab);
        int[] elevs  = scanner.getElevs(tab);
        int cacheR   = MinimapScanner.MAX_RADIUS;
        int cacheS   = MinimapScanner.CACHE_SIZE;

        for (int dz = -radius; dz < radius; dz++) {
            int cacheRow = (dz + cacheR);
            if (cacheRow < 0 || cacheRow >= cacheS) continue;

            for (int dx = -radius; dx < radius; dx++) {
                int cacheCol = (dx + cacheR);
                if (cacheCol < 0 || cacheCol >= cacheS) continue;

                int idx = cacheRow * cacheS + cacheCol;
                int color = colors[idx];
                if (color == 0) color = 0xFF111111;

                // Elevation shading (Surface & Underground only)
                if (tab == MapState.MapTab.SURFACE || tab == MapState.MapTab.UNDERGROUND) {
                    int elev = elevs[idx];
                    float shade = MathHelper.clamp((elev - 64) / 80.0f, -0.30f, 0.30f);
                    color = applyShade(color, shade);
                }

                // Fog of war at cache edges
                float fx = (float) Math.min(Math.abs(dx), radius - Math.abs(dx)) / (radius * 0.15f);
                float fz = (float) Math.min(Math.abs(dz), radius - Math.abs(dz)) / (radius * 0.15f);
                float fog = Math.min(Math.min(fx, fz), 1.0f);
                color = applyFog(color, fog);

                int sx = originX + (dx + radius) * ps;
                int sy = originY + (dz + radius) * ps;
                ctx.fill(sx, sy, sx + ps, sy + ps, color);
            }
        }

        // ── 3. Waypoints ─────────────────────────────────────────────────────
        BlockPos pPos = mc.player.getBlockPos();
        List<Waypoint> wps = WaypointManager.forDimension(tab);
        for (Waypoint wp : wps) {
            int wdx = wp.pos.getX() - pPos.getX();
            int wdz = wp.pos.getZ() - pPos.getZ();
            if (Math.abs(wdx) >= radius || Math.abs(wdz) >= radius) continue;
            int wx = originX + (wdx + radius) * ps;
            int wy = originY + (wdz + radius) * ps;
            drawWaypointPin(ctx, wx, wy, wp.color);
        }

        // ── 4. Player arrow ──────────────────────────────────────────────────
        int cx = originX + radius * ps;
        int cy = originY + radius * ps;
        drawPlayerArrow(ctx, cx, cy);

        // ── 5. Compass (top-right of map) ────────────────────────────────────
        drawCompass(ctx, originX + mapPx - 18, originY + 2);

        // ── 6. Bottom info bar ───────────────────────────────────────────────
        drawInfoBar(ctx, mc, tab, originX - BORDER, originY + mapPx + BORDER,
                    mapPx + BORDER * 2, pPos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-draw methods
    // ─────────────────────────────────────────────────────────────────────────

    /** Pixel-art wooden frame (3 concentric border rects). */
    private void drawFrame(DrawContext ctx, int x, int y, int w, int h) {
        fillRect(ctx, x,   y,   w,   h,   COL_BORDER_OUTER);
        fillRect(ctx, x+1, y+1, w-2, h-2, COL_BORDER_MID);
        fillRect(ctx, x+2, y+2, w-4, h-4, COL_BORDER_INNER);
        // Dark background behind map
        ctx.fill(x+3, y+3, x+w-3, y+h-3, COL_BG);
    }

    /** Draws a waypoint pin: black outline + colored 3×3 dot. */
    private void drawWaypointPin(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x - 2, y - 2, x + 4, y + 4, 0xFF000000); // shadow
        ctx.fill(x - 1, y - 1, x + 3, y + 3, color);
        ctx.fill(x,     y,     x + 2, y + 2, brighten(color, 0.4f)); // highlight
    }

    /**
     * 5×7 upward-pointing pixel-art arrow representing the player.
     * (Full yaw rotation would need matrix math; arrow always points "up = north" on map.)
     */
    private void drawPlayerArrow(DrawContext ctx, int cx, int cy) {
        // Arrow template (5 wide × 7 tall, 1 = white, 2 = dark outline)
        int[][] shape = {
            {0,0,2,0,0},
            {0,2,1,2,0},
            {2,1,1,1,2},
            {2,1,1,1,2},
            {0,2,1,2,0},
            {0,2,1,2,0},
            {0,2,2,2,0},
        };
        for (int py = 0; py < shape.length; py++) {
            for (int px = 0; px < shape[py].length; px++) {
                int v = shape[py][px];
                if (v == 0) continue;
                int col = (v == 1) ? 0xFFFFFFFF : 0xFF222222;
                ctx.fill(cx - 2 + px, cy - 3 + py, cx - 1 + px, cy - 2 + py, col);
            }
        }
    }

    /** 16×16 pixel-art compass. */
    private void drawCompass(DrawContext ctx, int x, int y) {
        ctx.fill(x, y, x + 16, y + 16, 0xCC000000);
        fillRect(ctx, x, y, 16, 16, COL_BAR_BORDER);
        // N needle (red, points up)
        ctx.fill(x + 7, y + 1, x + 9, y + 7,  COL_COMPASS_N);
        // S needle (grey, points down)
        ctx.fill(x + 7, y + 9, x + 9, y + 15, COL_COMPASS_S);
        // E (right)
        ctx.fill(x + 9, y + 7, x + 15, y + 9, COL_COMPASS_S);
        // W (left)
        ctx.fill(x + 1, y + 7, x + 7,  y + 9, COL_COMPASS_S);
        // Center dot
        ctx.fill(x + 7, y + 7, x + 9, y + 9, 0xFFFFFFFF);
    }

    /** Bottom info bar: dimension name + coordinates. */
    private void drawInfoBar(DrawContext ctx, MinecraftClient mc,
                              MapState.MapTab tab,
                              int x, int y, int w, BlockPos pPos) {
        // Background
        ctx.fill(x, y, x + w, y + BAR_H, COL_BAR_BG);
        fillRect(ctx, x, y, w, BAR_H, COL_BAR_BORDER);

        TextRenderer tr = mc.textRenderer;

        // Dimension label (left)
        String dimLabel = tab.displayName();
        if (tab == MapState.MapTab.UNDERGROUND) {
            dimLabel += " Y:" + pPos.getY();
        }
        ctx.drawText(tr, dimLabel, x + 4, y + 4,  COL_TEXT_GOLD, false);

        // XYZ coords (left, second line)
        String coords = "X:" + pPos.getX() + " Y:" + pPos.getY() + " Z:" + pPos.getZ();
        ctx.drawText(tr, coords, x + 4, y + 13, COL_TEXT_CYAN, false);

        // Key hint (right-aligned)
        String hint = "[M]MAP [N]TAB [B]WP";
        int hintX = x + w - tr.getWidth(hint) - 4;
        ctx.drawText(tr, hint, hintX, y + 4, COL_TEXT_DIM, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Color helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Draw only the 1-px outline of a rectangle. */
    private void fillRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,       y,       x + w, y + 1,     color); // top
        ctx.fill(x,       y + h-1, x + w, y + h,     color); // bottom
        ctx.fill(x,       y,       x + 1, y + h,     color); // left
        ctx.fill(x + w-1, y,       x + w, y + h,     color); // right
    }

    private int applyShade(int color, float shade) {
        int a = (color >> 24) & 0xFF;
        int r = MathHelper.clamp((int)(((color >> 16) & 0xFF) * (1f + shade)), 0, 255);
        int g = MathHelper.clamp((int)(((color >>  8) & 0xFF) * (1f + shade)), 0, 255);
        int b = MathHelper.clamp((int)(( color        & 0xFF) * (1f + shade)), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int applyFog(int color, float fog) {
        if (fog >= 1.0f) return color;
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * fog);
        int g = (int)(((color >>  8) & 0xFF) * fog);
        int b = (int)(( color        & 0xFF) * fog);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int brighten(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = MathHelper.clamp((int)(((color >> 16) & 0xFF) + 255 * amount), 0, 255);
        int g = MathHelper.clamp((int)(((color >>  8) & 0xFF) + 255 * amount), 0, 255);
        int b = MathHelper.clamp((int)(( color        & 0xFF) + 255 * amount), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
