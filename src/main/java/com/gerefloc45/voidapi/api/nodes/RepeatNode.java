package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Repeat node - repeats its child behavior a specified number of times.
 * Can repeat infinitely or until failure.
 * 
 * @author VoidAPI Framework
 * @version 1.1.0
 */
public class RepeatNode implements Behavior {
    private final Behavior child;
    private final int maxRepeats;
    private final boolean repeatUntilFailure;
    private int currentRepeat;

    /**
     * Creates a repeat node that repeats infinitely.
     *
     * @param child The child behavior to repeat
     */
    public RepeatNode(Behavior child) {
        this(child, -1, false);
    }

    /**
     * Creates a repeat node with a maximum number of repeats.
     *
     * @param child The child behavior to repeat
     * @param maxRepeats Maximum number of repeats (-1 for infinite)
     */
    public RepeatNode(Behavior child, int maxRepeats) {
        this(child, maxRepeats, false);
    }

    /**
     * Creates a repeat node with full configuration.
     *
     * @param child The child behavior to repeat
     * @param maxRepeats Maximum number of repeats (-1 for infinite)
     * @param repeatUntilFailure If true, repeats until child fails
     */
    public RepeatNode(Behavior child, int maxRepeats, boolean repeatUntilFailure) {
        this.child = child;
        this.maxRepeats = maxRepeats;
        this.repeatUntilFailure = repeatUntilFailure;
        this.currentRepeat = 0;
    }

    @Override
    public Status execute(BehaviorContext context) {
        while (true) {
            // Check if we've reached max repeats
            if (maxRepeats > 0 && currentRepeat >= maxRepeats) {
                return Status.SUCCESS;
            }

            Status childStatus = child.execute(context);

            if (childStatus == Status.RUNNING) {
                return Status.RUNNING;
            }

            // Handle completion
            child.onEnd(context, childStatus);

            if (repeatUntilFailure && childStatus == Status.FAILURE) {
                currentRepeat = 0;
                return Status.SUCCESS;
            }

            if (!repeatUntilFailure && childStatus == Status.FAILURE) {
                currentRepeat = 0;
                return Status.FAILURE;
            }

            // Restart child for next repeat
            currentRepeat++;
            child.onStart(context);

            // If we're in a single tick, break to avoid infinite loop
            // The behavior will continue on next tick
            if (childStatus == Status.SUCCESS) {
                break;
            }
        }

        return Status.RUNNING;
    }

    @Override
    public void onStart(BehaviorContext context) {
        currentRepeat = 0;
        child.onStart(context);
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        child.onEnd(context, status);
        currentRepeat = 0;
    }
}
