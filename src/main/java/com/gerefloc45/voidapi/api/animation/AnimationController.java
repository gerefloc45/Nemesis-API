package com.gerefloc45.voidapi.api.animation;

import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Animation controller - manages entity animations across different systems.
 * Provides a unified interface for vanilla and GeckoLib animations.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class AnimationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationController.class);
    private static AnimationController instance;

    private final Map<UUID, AnimationState> activeAnimations;
    private final Map<String, AnimationProvider> providers;

    /**
     * Animation state for an entity.
     */
    public static class AnimationState {
        public String animationName;
        public long startTime;
        public float duration;
        public boolean looping;

        public AnimationState(String animationName, long startTime, float duration, boolean looping) {
            this.animationName = animationName;
            this.startTime = startTime;
            this.duration = duration;
            this.looping = looping;
        }

        public boolean isComplete() {
            if (looping) {
                return false;
            }
            long currentTime = System.currentTimeMillis();
            float elapsedSeconds = (currentTime - startTime) / 1000.0f;
            return elapsedSeconds >= duration;
        }

        public float getProgress() {
            long currentTime = System.currentTimeMillis();
            float elapsedSeconds = (currentTime - startTime) / 1000.0f;
            return Math.min(1.0f, elapsedSeconds / duration);
        }
    }

    /**
     * Interface for animation providers (vanilla, GeckoLib, etc.).
     */
    public interface AnimationProvider {
        /**
         * Plays an animation on an entity.
         *
         * @param entity Entity to animate
         * @param animationName Animation name
         * @param duration Animation duration
         * @param looping Whether to loop
         * @return True if animation started successfully
         */
        boolean playAnimation(Entity entity, String animationName, float duration, boolean looping);

        /**
         * Stops the current animation on an entity.
         *
         * @param entity Entity to stop animating
         */
        void stopAnimation(Entity entity);

        /**
         * Checks if this provider can handle the given entity.
         *
         * @param entity Entity to check
         * @return True if provider can handle entity
         */
        boolean canHandle(Entity entity);
    }

    private AnimationController() {
        this.activeAnimations = new HashMap<>();
        this.providers = new HashMap<>();
        
        // Register default vanilla provider
        registerProvider("vanilla", new VanillaAnimationProvider());
    }

    /**
     * Gets the singleton instance.
     *
     * @return AnimationController instance
     */
    public static AnimationController getInstance() {
        if (instance == null) {
            instance = new AnimationController();
        }
        return instance;
    }

    /**
     * Registers an animation provider.
     *
     * @param name Provider name
     * @param provider Provider implementation
     */
    public void registerProvider(String name, AnimationProvider provider) {
        providers.put(name, provider);
        LOGGER.info("Registered animation provider: {}", name);
    }

    /**
     * Plays an animation on an entity.
     *
     * @param entity Entity to animate
     * @param animationName Animation name
     * @param duration Animation duration in seconds
     * @param looping Whether to loop the animation
     * @return True if animation started successfully
     */
    public boolean playAnimation(Entity entity, String animationName, float duration, boolean looping) {
        UUID entityId = entity.getUuid();

        // Find a provider that can handle this entity
        for (AnimationProvider provider : providers.values()) {
            if (provider.canHandle(entity)) {
                boolean success = provider.playAnimation(entity, animationName, duration, looping);
                if (success) {
                    AnimationState state = new AnimationState(animationName, System.currentTimeMillis(), duration, looping);
                    activeAnimations.put(entityId, state);
                    return true;
                }
            }
        }

        LOGGER.warn("No animation provider found for entity: {}", entity.getType().getName().getString());
        return false;
    }

    /**
     * Stops the current animation on an entity.
     *
     * @param entity Entity to stop animating
     */
    public void stopAnimation(Entity entity) {
        UUID entityId = entity.getUuid();
        activeAnimations.remove(entityId);

        for (AnimationProvider provider : providers.values()) {
            if (provider.canHandle(entity)) {
                provider.stopAnimation(entity);
                break;
            }
        }
    }

    /**
     * Gets the current animation state for an entity.
     *
     * @param entity Entity to check
     * @return Animation state or null if not animating
     */
    public AnimationState getAnimationState(Entity entity) {
        return activeAnimations.get(entity.getUuid());
    }

    /**
     * Checks if an entity is currently animating.
     *
     * @param entity Entity to check
     * @return True if animating
     */
    public boolean isAnimating(Entity entity) {
        AnimationState state = activeAnimations.get(entity.getUuid());
        return state != null && !state.isComplete();
    }

    /**
     * Updates all active animations (call from tick).
     */
    public void tick() {
        // Remove completed animations
        activeAnimations.entrySet().removeIf(entry -> entry.getValue().isComplete());
    }

    /**
     * Clears all active animations.
     */
    public void clear() {
        activeAnimations.clear();
    }

    /**
     * Default vanilla animation provider.
     * This is a placeholder - actual implementation depends on entity type.
     */
    private static class VanillaAnimationProvider implements AnimationProvider {
        @Override
        public boolean playAnimation(Entity entity, String animationName, float duration, boolean looping) {
            // Vanilla entities don't have a unified animation system
            // This is a placeholder that can be extended by mod developers
            LOGGER.debug("Playing vanilla animation '{}' on {}", animationName, entity.getType().getName().getString());
            return true;
        }

        @Override
        public void stopAnimation(Entity entity) {
            // Placeholder
        }

        @Override
        public boolean canHandle(Entity entity) {
            // Handle all entities by default
            return true;
        }
    }
}
