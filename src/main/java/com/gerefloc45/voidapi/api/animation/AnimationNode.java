package com.gerefloc45.voidapi.api.animation;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Animation node - triggers and manages entity animations.
 * Supports both vanilla and GeckoLib animations.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class AnimationNode implements Behavior {
    private final String animationName;
    private final float duration;
    private final boolean waitForCompletion;
    private final boolean looping;
    private long startTime;
    private boolean hasStarted;

    /**
     * Creates an animation node that plays once.
     *
     * @param animationName Name of the animation to play
     * @param duration Animation duration in seconds
     */
    public AnimationNode(String animationName, float duration) {
        this(animationName, duration, true, false);
    }

    /**
     * Creates an animation node with custom settings.
     *
     * @param animationName Name of the animation to play
     * @param duration Animation duration in seconds
     * @param waitForCompletion Whether to wait for animation to complete
     */
    public AnimationNode(String animationName, float duration, boolean waitForCompletion) {
        this(animationName, duration, waitForCompletion, false);
    }

    /**
     * Creates an animation node with full configuration.
     *
     * @param animationName Name of the animation to play
     * @param duration Animation duration in seconds
     * @param waitForCompletion Whether to wait for animation to complete
     * @param looping Whether the animation should loop
     */
    public AnimationNode(String animationName, float duration, boolean waitForCompletion, boolean looping) {
        this.animationName = animationName;
        this.duration = duration;
        this.waitForCompletion = waitForCompletion;
        this.looping = looping;
        this.hasStarted = false;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!hasStarted) {
            // Trigger animation
            startAnimation(context);
            startTime = System.currentTimeMillis();
            hasStarted = true;

            if (!waitForCompletion) {
                return Status.SUCCESS;
            }
        }

        // Wait for animation completion
        if (waitForCompletion && !looping) {
            long currentTime = System.currentTimeMillis();
            float elapsedSeconds = (currentTime - startTime) / 1000.0f;

            if (elapsedSeconds >= duration) {
                return Status.SUCCESS;
            }
            return Status.RUNNING;
        }

        // Looping animations never complete
        if (looping) {
            return Status.RUNNING;
        }

        return Status.SUCCESS;
    }

    /**
     * Starts the animation on the entity.
     *
     * @param context Behavior context
     */
    private void startAnimation(BehaviorContext context) {
        // Store animation state in blackboard
        context.getBlackboard().set("current_animation", animationName);
        context.getBlackboard().set("animation_start_time", startTime);
        context.getBlackboard().set("animation_looping", looping);

        // Try to use AnimationController if available
        AnimationController controller = AnimationController.getInstance();
        if (controller != null) {
            controller.playAnimation(context.getEntity(), animationName, duration, looping);
        }
    }

    /**
     * Stops the current animation.
     *
     * @param context Behavior context
     */
    private void stopAnimation(BehaviorContext context) {
        context.getBlackboard().remove("current_animation");
        context.getBlackboard().remove("animation_start_time");
        context.getBlackboard().remove("animation_looping");

        AnimationController controller = AnimationController.getInstance();
        if (controller != null) {
            controller.stopAnimation(context.getEntity());
        }
    }

    @Override
    public void onStart(BehaviorContext context) {
        hasStarted = false;
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (!looping) {
            stopAnimation(context);
        }
        hasStarted = false;
    }

    /**
     * Gets the animation name.
     *
     * @return Animation name
     */
    public String getAnimationName() {
        return animationName;
    }

    /**
     * Gets the animation duration.
     *
     * @return Duration in seconds
     */
    public float getDuration() {
        return duration;
    }

    /**
     * Checks if this animation waits for completion.
     *
     * @return True if waiting for completion
     */
    public boolean isWaitingForCompletion() {
        return waitForCompletion;
    }

    /**
     * Checks if this animation is looping.
     *
     * @return True if looping
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Gets the elapsed time since animation started.
     *
     * @return Elapsed time in seconds
     */
    public float getElapsedTime() {
        if (!hasStarted) {
            return 0.0f;
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime) / 1000.0f;
    }
}
