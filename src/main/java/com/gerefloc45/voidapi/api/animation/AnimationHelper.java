package com.gerefloc45.voidapi.api.animation;

import net.minecraft.entity.Entity;

/**
 * Animation helper - utility methods for working with animations.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class AnimationHelper {
    
    /**
     * Initializes animation system and registers providers.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        AnimationController controller = AnimationController.getInstance();
        
        // Register GeckoLib provider if available
        if (GeckoLibAnimationProvider.isGeckoLibAvailable()) {
            controller.registerProvider("geckolib", new GeckoLibAnimationProvider());
        }
    }

    /**
     * Plays an animation on an entity (convenience method).
     *
     * @param entity Entity to animate
     * @param animationName Animation name
     * @param duration Duration in seconds
     * @return True if animation started
     */
    public static boolean playAnimation(Entity entity, String animationName, float duration) {
        return AnimationController.getInstance().playAnimation(entity, animationName, duration, false);
    }

    /**
     * Plays a looping animation on an entity.
     *
     * @param entity Entity to animate
     * @param animationName Animation name
     * @param duration Duration in seconds
     * @return True if animation started
     */
    public static boolean playLoopingAnimation(Entity entity, String animationName, float duration) {
        return AnimationController.getInstance().playAnimation(entity, animationName, duration, true);
    }

    /**
     * Stops the current animation on an entity.
     *
     * @param entity Entity to stop animating
     */
    public static void stopAnimation(Entity entity) {
        AnimationController.getInstance().stopAnimation(entity);
    }

    /**
     * Checks if an entity is currently animating.
     *
     * @param entity Entity to check
     * @return True if animating
     */
    public static boolean isAnimating(Entity entity) {
        return AnimationController.getInstance().isAnimating(entity);
    }

    /**
     * Gets the progress of the current animation (0.0 to 1.0).
     *
     * @param entity Entity to check
     * @return Animation progress or 0.0 if not animating
     */
    public static float getAnimationProgress(Entity entity) {
        AnimationController.AnimationState state = AnimationController.getInstance().getAnimationState(entity);
        return state != null ? state.getProgress() : 0.0f;
    }

    /**
     * Gets the name of the current animation.
     *
     * @param entity Entity to check
     * @return Animation name or null if not animating
     */
    public static String getCurrentAnimation(Entity entity) {
        AnimationController.AnimationState state = AnimationController.getInstance().getAnimationState(entity);
        return state != null ? state.animationName : null;
    }
}
