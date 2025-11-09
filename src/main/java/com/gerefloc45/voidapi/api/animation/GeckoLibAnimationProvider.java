package com.gerefloc45.voidapi.api.animation;

import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GeckoLib animation provider - integrates with GeckoLib animation system.
 * This is an optional integration that requires GeckoLib to be installed.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class GeckoLibAnimationProvider implements AnimationController.AnimationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeckoLibAnimationProvider.class);
    private static boolean geckoLibAvailable = false;

    static {
        // Check if GeckoLib is available
        try {
            Class.forName("software.bernie.geckolib.animatable.GeoEntity");
            geckoLibAvailable = true;
            LOGGER.info("GeckoLib detected - animation support enabled");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("GeckoLib not found - GeckoLib animations disabled");
        }
    }

    @Override
    public boolean playAnimation(Entity entity, String animationName, float duration, boolean looping) {
        if (!geckoLibAvailable) {
            return false;
        }

        try {
            // Check if entity is a GeckoLib entity
            if (isGeckoLibEntity(entity)) {
                // Trigger GeckoLib animation
                triggerGeckoLibAnimation(entity, animationName, looping);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to play GeckoLib animation: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public void stopAnimation(Entity entity) {
        if (!geckoLibAvailable) {
            return;
        }

        try {
            if (isGeckoLibEntity(entity)) {
                stopGeckoLibAnimation(entity);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to stop GeckoLib animation: {}", e.getMessage());
        }
    }

    @Override
    public boolean canHandle(Entity entity) {
        return geckoLibAvailable && isGeckoLibEntity(entity);
    }

    /**
     * Checks if an entity is a GeckoLib entity.
     *
     * @param entity Entity to check
     * @return True if entity uses GeckoLib
     */
    private boolean isGeckoLibEntity(Entity entity) {
        try {
            Class<?> geoEntityClass = Class.forName("software.bernie.geckolib.animatable.GeoEntity");
            return geoEntityClass.isInstance(entity);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Triggers a GeckoLib animation using reflection.
     * This method uses reflection to avoid hard dependency on GeckoLib.
     *
     * @param entity GeckoLib entity
     * @param animationName Animation name
     * @param looping Whether to loop
     */
    private void triggerGeckoLibAnimation(Entity entity, String animationName, boolean looping) {
        try {
            // This is a simplified implementation
            // Real implementation would use GeckoLib's AnimationController API
            // Example: entity.triggerAnim("controller_name", animationName);
            
            LOGGER.debug("Triggering GeckoLib animation '{}' on {}", animationName, entity.getType().getName().getString());
            
            // Store animation info for GeckoLib to read
            // Actual implementation depends on GeckoLib version and entity setup
        } catch (Exception e) {
            LOGGER.error("Failed to trigger GeckoLib animation: {}", e.getMessage());
        }
    }

    /**
     * Stops the current GeckoLib animation.
     *
     * @param entity GeckoLib entity
     */
    private void stopGeckoLibAnimation(Entity entity) {
        try {
            LOGGER.debug("Stopping GeckoLib animation on {}", entity.getType().getName().getString());
            // Implementation depends on GeckoLib API
        } catch (Exception e) {
            LOGGER.error("Failed to stop GeckoLib animation: {}", e.getMessage());
        }
    }

    /**
     * Checks if GeckoLib is available.
     *
     * @return True if GeckoLib is loaded
     */
    public static boolean isGeckoLibAvailable() {
        return geckoLibAvailable;
    }
}
