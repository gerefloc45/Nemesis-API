package com.gerefloc45.voidapi;

import com.gerefloc45.voidapi.api.animation.AnimationHelper;
import com.gerefloc45.voidapi.core.BrainTicker;
import com.gerefloc45.voidapi.util.AsyncHelper;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for the VoidAPI framework mod.
 * This mod provides a complete AI framework for Minecraft entities.
 * It contains no gameplay content and is designed to be used as a library.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class VoidAPIMod implements ModInitializer {
    public static final String MOD_ID = "voidapi";
    public static final String MOD_NAME = "VoidAPI";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {} Framework v0.2.0", MOD_NAME);

        // Initialize the brain ticker system
        BrainTicker.initialize();

        // Initialize animation system
        AnimationHelper.initialize();

        LOGGER.info("{} Framework initialized successfully", MOD_NAME);
        LOGGER.info("API ready for use by dependent mods");
    }

    /**
     * Called when the mod is being shut down.
     * Cleans up resources.
     */
    public static void shutdown() {
        LOGGER.info("Shutting down {} Framework", MOD_NAME);
        AsyncHelper.shutdown();
    }
}
