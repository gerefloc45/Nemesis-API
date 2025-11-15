package com.gerefloc45.voidapi.api.fsm;

import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Represents a state in a Finite State Machine.
 * Each state can have entry, update, and exit logic.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public abstract class State {
    private final String name;
    private boolean isActive;
    private long enterTime;
    private long lastUpdateTime;

    /**
     * Creates a new state with the given name.
     *
     * @param name Unique identifier for this state
     */
    public State(String name) {
        this.name = name;
        this.isActive = false;
        this.enterTime = 0;
        this.lastUpdateTime = 0;
    }

    /**
     * Called when entering this state.
     * Override to implement entry logic.
     *
     * @param context The behavior context
     */
    public void onEnter(BehaviorContext context) {
        this.isActive = true;
        this.enterTime = System.currentTimeMillis();
        this.lastUpdateTime = this.enterTime;
    }

    /**
     * Called every tick while in this state.
     * Override to implement state logic.
     *
     * @param context The behavior context
     */
    public abstract void onUpdate(BehaviorContext context);

    /**
     * Called when exiting this state.
     * Override to implement exit logic.
     *
     * @param context The behavior context
     */
    public void onExit(BehaviorContext context) {
        this.isActive = false;
    }

    /**
     * Gets the name of this state.
     *
     * @return State name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this state is currently active.
     *
     * @return True if active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Gets the time when this state was entered (in milliseconds).
     *
     * @return Enter time
     */
    public long getEnterTime() {
        return enterTime;
    }

    /**
     * Gets the time elapsed since entering this state (in seconds).
     *
     * @return Time in state (seconds)
     */
    public float getTimeInState() {
        if (!isActive) {
            return 0.0f;
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - enterTime) / 1000.0f;
    }

    /**
     * Gets the delta time since last update (in seconds).
     *
     * @return Delta time (seconds)
     */
    protected float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float delta = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        return delta;
    }

    /**
     * Internal update method that tracks timing.
     *
     * @param context The behavior context
     */
    final void update(BehaviorContext context) {
        if (isActive) {
            onUpdate(context);
        }
    }

    @Override
    public String toString() {
        return "State{" + name + ", active=" + isActive + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        State state = (State) obj;
        return name.equals(state.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
