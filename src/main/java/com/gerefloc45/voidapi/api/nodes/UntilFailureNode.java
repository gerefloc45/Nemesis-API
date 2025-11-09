package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Until failure node - repeats its child behavior until it fails.
 * Converts SUCCESS to RUNNING to continue trying.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class UntilFailureNode implements Behavior {
    private final Behavior child;
    private final int maxAttempts;
    private int currentAttempt;

    /**
     * Creates an until failure node with unlimited attempts.
     *
     * @param child The child behavior to repeat
     */
    public UntilFailureNode(Behavior child) {
        this(child, -1);
    }

    /**
     * Creates an until failure node with a maximum number of attempts.
     *
     * @param child The child behavior to repeat
     * @param maxAttempts Maximum number of attempts (-1 for unlimited)
     */
    public UntilFailureNode(Behavior child, int maxAttempts) {
        this.child = child;
        this.maxAttempts = maxAttempts;
        this.currentAttempt = 0;
    }

    @Override
    public Status execute(BehaviorContext context) {
        // Check if max attempts reached
        if (maxAttempts > 0 && currentAttempt >= maxAttempts) {
            return Status.SUCCESS;
        }

        Status childStatus = child.execute(context);

        if (childStatus == Status.RUNNING) {
            return Status.RUNNING;
        }

        if (childStatus == Status.FAILURE) {
            // Failed! Reset and return success (we wanted it to fail)
            currentAttempt = 0;
            return Status.SUCCESS;
        }

        // Child succeeded, restart and continue
        currentAttempt++;
        child.onEnd(context, childStatus);
        child.onStart(context);

        // Return RUNNING to indicate we're still trying
        return Status.RUNNING;
    }

    @Override
    public void onStart(BehaviorContext context) {
        currentAttempt = 0;
        child.onStart(context);
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        child.onEnd(context, status);
        currentAttempt = 0;
    }

    /**
     * Gets the current attempt number.
     *
     * @return Current attempt (0-based)
     */
    public int getCurrentAttempt() {
        return currentAttempt;
    }

    /**
     * Gets the maximum number of attempts.
     *
     * @return Max attempts (-1 for unlimited)
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
