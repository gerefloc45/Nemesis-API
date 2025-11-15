package com.gerefloc45.voidapi.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side initializer for VoidAPI.
 * This is optional and only loads on the client side.
 * Provides client-specific features like debug visualization.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class VoidAPIClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("VoidAPI-Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("VoidAPI Client initialized");
        // Client-specific initialization can go here
        // For now, this is just a placeholder for future client features
    }
}
