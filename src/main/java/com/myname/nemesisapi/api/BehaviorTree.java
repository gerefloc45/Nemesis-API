package com.myname.nemesisapi.api;

/**
 * Main behavior tree class that wraps a root behavior.
 * Manages the execution lifecycle of the entire tree.
 * 
 * @author Nemesis-API Framework
 * @version 1.0.0
 */
public class BehaviorTree {
    private final Behavior rootBehavior;
    private Behavior.Status lastStatus;
    private boolean isRunning;

    /**
     * Creates a new behavior tree with the given root behavior.
     *
     * @param rootBehavior The root behavior of the tree
     */
    public BehaviorTree(Behavior rootBehavior) {
        this.rootBehavior = rootBehavior;
        this.lastStatus = Behavior.Status.SUCCESS;
        this.isRunning = false;
    }

    /**
     * Ticks the behavior tree, executing the root behavior.
     *
     * @param context The behavior context
     * @return The status of the root behavior execution
     */
    public Behavior.Status tick(BehaviorContext context) {
        if (!isRunning) {
            rootBehavior.onStart(context);
            isRunning = true;
        }

        lastStatus = rootBehavior.execute(context);

        if (lastStatus != Behavior.Status.RUNNING) {
            rootBehavior.onEnd(context, lastStatus);
            isRunning = false;
        }

        return lastStatus;
    }

    /**
     * Gets the last execution status of the tree.
     *
     * @return The last status
     */
    public Behavior.Status getLastStatus() {
        return lastStatus;
    }

    /**
     * Checks if the tree is currently running.
     *
     * @return True if running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Resets the behavior tree to its initial state.
     *
     * @param context The behavior context
     */
    public void reset(BehaviorContext context) {
        if (isRunning) {
            rootBehavior.onEnd(context, lastStatus);
        }
        isRunning = false;
        lastStatus = Behavior.Status.SUCCESS;
    }

    /**
     * Gets the root behavior of this tree.
     *
     * @return The root behavior
     */
    public Behavior getRootBehavior() {
        return rootBehavior;
    }
}
