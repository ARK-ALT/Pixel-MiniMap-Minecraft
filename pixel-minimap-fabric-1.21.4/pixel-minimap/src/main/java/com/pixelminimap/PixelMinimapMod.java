package com.pixelminimap;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PixelMinimapMod implements ModInitializer {

    public static final String MOD_ID = "pixelminimap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[PixelMinimap] Mod initialized.");
    }
}
