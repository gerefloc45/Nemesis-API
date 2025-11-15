package com.gerefloc45.voidapi.api.fsm.states;

import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.fsm.State;

/**
 * A state that automatically transitions after a duration.
 * Useful for temporary states or animations.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public abstract class TimedState extends State {
    private final float duration;
    private final String nextStateKey;

    /**
     * Creates a timed state.
     *
     * @param name State name
     * @param duration Duration in seconds before auto-transition
     */
    public TimedState(String name, float duration) {
        this(name, duration, null);
    }

    /**
     * Creates a timed state with blackboard integration.
     *
     * @param name State name
     * @param duration Duration in seconds
     * @param nextStateKey Blackboard key to store next state name
     */
    public TimedState(String name, float duration, String nextStateKey) {
        super(name);
        this.duration = duration;
        this.nextStateKey = nextStateKey;
    }

    @Override
    public void onUpdate(BehaviorContext context) {
        // Check if duration has elapsed
        if (getTimeInState() >= duration) {
            onTimeout(context);
            
            // Store timeout flag in blackboard if key provided
            if (nextStateKey != null) {
                context.getBlackboard().set(nextStateKey, true);
            }
        }
    }

    /**
     * Called when the timer expires.
     * Override to implement timeout logic.
     *
     * @param context The behavior context
     */
    protected abstract void onTimeout(BehaviorContext context);

    /**
     * Gets the duration of this state.
     *
     * @return Duration in seconds
     */
    public float getDuration() {
        return duration;
    }

    /**
     * Gets the remaining time in this state.
     *
     * @return Remaining time in seconds
     */
    public float getRemainingTime() {
        return Math.max(0, duration - getTimeInState());
    }

    /**
     * Checks if the timer has expired.
     *
     * @return True if time is up
     */
    public boolean isExpired() {
        return getTimeInState() >= duration;
    }
}
