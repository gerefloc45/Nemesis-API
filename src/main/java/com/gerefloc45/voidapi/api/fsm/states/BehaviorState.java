package com.gerefloc45.voidapi.api.fsm.states;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.fsm.State;

/**
 * A state that executes a behavior.
 * Bridges FSM and Behavior Tree systems.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class BehaviorState extends State {
    private final Behavior behavior;
    private Behavior.Status lastStatus;

    /**
     * Creates a behavior state.
     *
     * @param name State name
     * @param behavior Behavior to execute
     */
    public BehaviorState(String name, Behavior behavior) {
        super(name);
        this.behavior = behavior;
        this.lastStatus = null;
    }

    @Override
    public void onEnter(BehaviorContext context) {
        super.onEnter(context);
        behavior.onStart(context);
        lastStatus = null;
    }

    @Override
    public void onUpdate(BehaviorContext context) {
        lastStatus = behavior.execute(context);
    }

    @Override
    public void onExit(BehaviorContext context) {
        if (lastStatus != null && lastStatus != Behavior.Status.RUNNING) {
            behavior.onEnd(context, lastStatus);
        }
        super.onExit(context);
    }

    /**
     * Gets the last execution status of the behavior.
     *
     * @return Last status, or null if not yet executed
     */
    public Behavior.Status getLastStatus() {
        return lastStatus;
    }

    /**
     * Gets the behavior being executed.
     *
     * @return The behavior
     */
    public Behavior getBehavior() {
        return behavior;
    }
}
