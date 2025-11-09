package com.myname.nemesisapi.api;

/**
 * Base interface for all AI behaviors.
 * Behaviors are executed within a BehaviorContext and return a status.
 * 
 * @author Nemesis-API Framework
 * @version 1.0.0
 */
@FunctionalInterface
public interface Behavior {
    
    /**
     * Execution status of a behavior.
     */
    enum Status {
        /** Behavior completed successfully */
        SUCCESS,
        /** Behavior failed to complete */
        FAILURE,
        /** Behavior is still running and needs more ticks */
        RUNNING
    }

    /**
     * Executes the behavior with the given context.
     *
     * @param context The behavior context containing entity, world, and blackboard
     * @return The status of the behavior execution
     */
    Status execute(BehaviorContext context);

    /**
     * Called when the behavior is initialized or reset.
     * Override this to perform setup logic.
     *
     * @param context The behavior context
     */
    default void onStart(BehaviorContext context) {
        // Default: no-op
    }

    /**
     * Called when the behavior completes (SUCCESS or FAILURE).
     * Override this to perform cleanup logic.
     *
     * @param context The behavior context
     * @param status The final status of the behavior
     */
    default void onEnd(BehaviorContext context, Status status) {
        // Default: no-op
    }
}
