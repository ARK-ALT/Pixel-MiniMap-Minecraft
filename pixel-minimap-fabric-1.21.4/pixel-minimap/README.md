# Pixel Minimap — Fabric Mod (Minecraft 1.21.4)

A pixel-art minimap covering all four dimensions, with a full waypoint system.

## Features

| Key | Action |
|-----|--------|
| `M` | Toggle minimap on/off |
| `N` | Cycle map tab (Surface → Underground → Nether → End) |
| `B` | Add waypoint at current position |
| `=` | Zoom in |
| `-` | Zoom out |

### Map tabs
- **🌍 Surface** — top-down view with biome tinting, elevation shading, fog of war
- **⛏ Underground** — horizontal slice at player Y; ore deposits highlighted by type
- **🔥 Nether** — scans below the bedrock ceiling; shows biome terrain
- **🌌 The End** — end stone islands, purpur structures, void gaps

### Waypoints
- Press `B` to drop a gold waypoint at your feet
- Default waypoints loaded on first launch for all four dimensions
- Colored dots on the minimap indicate waypoint positions

---

## Building

### Requirements
- **Java 21+** — [Adoptium](https://adoptium.net/)
- Internet connection (Gradle downloads dependencies automatically)

### Steps

```bash
# 1. Download the Gradle wrapper jar (not included due to size)
#    Either run:  gradle wrapper
#    Or download: https://services.gradle.org/distributions/gradle-8.8-bin.zip
#    and place the jar at:  gradle/wrapper/gradle-wrapper.jar

# 2. Build the mod
./gradlew build

# 3. The compiled .jar will be at:
#    build/libs/pixel-minimap-1.0.0.jar
```

Copy `pixel-minimap-1.0.0.jar` into your `.minecraft/mods/` folder alongside:
- [Fabric Loader ≥ 0.16](https://fabricmc.net/use/)
- [Fabric API 0.114.0+1.21.4](https://modrinth.com/mod/fabric-api)

---

## Project structure

```
src/
├── main/java/com/pixelminimap/
│   └── PixelMinimapMod.java          ← Common (server-safe) init
└── client/java/com/pixelminimap/
    ├── PixelMinimapClient.java        ← Client entry-point (wires events)
    ├── MapState.java                  ← Current tab, zoom, visibility
    ├── MinimapScanner.java            ← Incremental block scanner & color cache
    ├── MinimapRenderer.java           ← Pixel-art HUD renderer
    ├── WaypointManager.java           ← Waypoint CRUD
    ├── Waypoint.java                  ← Waypoint data class
    └── KeyBindings.java               ← Key binding registration & processing
```

---

## Customisation tips

- **Change minimap size**: edit `MapState.radius` (default `48`) and `MapState.pixelSize` (default `2`)
- **Add a block color**: add a case in `MinimapScanner.staticColor()`
- **Persistent waypoints**: serialise `WaypointManager.getAll()` to JSON in `%appdata%/.minecraft/config/pixelminimap_waypoints.json` using Gson (already on the classpath via Minecraft)
