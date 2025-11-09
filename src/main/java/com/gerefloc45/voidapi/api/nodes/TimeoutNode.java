package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Timeout node - fails the child behavior if it doesn't complete within a time limit.
 * Returns FAILURE if the timeout is exceeded while child is still RUNNING.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class TimeoutNode implements Behavior {
    private final Behavior child;
    private final float timeoutSeconds;
    private final String timeoutKey;
    private long startTime;
    private boolean hasStarted;

    /**
     * Creates a new timeout node.
     *
     * @param child The child behavior
     * @param timeoutSeconds Timeout duration in seconds
     */
    public TimeoutNode(Behavior child, float timeoutSeconds) {
        this.child = child;
        this.timeoutSeconds = timeoutSeconds;
        this.timeoutKey = "timeout_" + System.identityHashCode(this);
        this.hasStarted = false;
    }

    /**
     * Creates a new timeout node with custom blackboard key.
     *
     * @param child The child behavior
     * @param timeoutSeconds Timeout duration in seconds
     * @param timeoutKey Custom blackboard key for tracking timeout
     */
    public TimeoutNode(Behavior child, float timeoutSeconds, String timeoutKey) {
        this.child = child;
        this.timeoutSeconds = timeoutSeconds;
        this.timeoutKey = timeoutKey;
        this.hasStarted = false;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!hasStarted) {
            startTime = System.currentTimeMillis();
            hasStarted = true;
            context.getBlackboard().set(timeoutKey, startTime);
        }

        // Check if timeout exceeded
        long currentTime = System.currentTimeMillis();
        float elapsedSeconds = (currentTime - startTime) / 1000.0f;

        if (elapsedSeconds >= timeoutSeconds) {
            // Timeout exceeded
            child.onEnd(context, Status.FAILURE);
            return Status.FAILURE;
        }

        // Execute child
        Status childStatus = child.execute(context);

        // If child completed, reset
        if (childStatus != Status.RUNNING) {
            hasStarted = false;
        }

        return childStatus;
    }

    @Override
    public void onStart(BehaviorContext context) {
        hasStarted = false;
        startTime = System.currentTimeMillis();
        child.onStart(context);
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        child.onEnd(context, status);
        hasStarted = false;
        context.getBlackboard().remove(timeoutKey);
    }

    /**
     * Gets the elapsed time since the behavior started.
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

    /**
     * Gets the remaining time before timeout.
     *
     * @return Remaining time in seconds
     */
    public float getRemainingTime() {
        return Math.max(0, timeoutSeconds - getElapsedTime());
    }
}
