package com.gerefloc45.voidapi.api.fsm;

import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * A state that contains its own sub-state machine.
 * Allows for hierarchical finite state machines.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class HierarchicalState extends State {
    private final StateMachine subStateMachine;
    private boolean autoStartSubMachine;

    /**
     * Creates a hierarchical state with a sub-state machine.
     *
     * @param name State name
     * @param subStateMachine The sub-state machine
     */
    public HierarchicalState(String name, StateMachine subStateMachine) {
        this(name, subStateMachine, true);
    }

    /**
     * Creates a hierarchical state with control over auto-start.
     *
     * @param name State name
     * @param subStateMachine The sub-state machine
     * @param autoStartSubMachine Whether to automatically start sub-machine on enter
     */
    public HierarchicalState(String name, StateMachine subStateMachine, boolean autoStartSubMachine) {
        super(name);
        this.subStateMachine = subStateMachine;
        this.autoStartSubMachine = autoStartSubMachine;
    }

    @Override
    public void onEnter(BehaviorContext context) {
        super.onEnter(context);
        if (autoStartSubMachine) {
            subStateMachine.start(context);
        }
    }

    @Override
    public void onUpdate(BehaviorContext context) {
        // Update the sub-state machine
        if (subStateMachine.isRunning()) {
            subStateMachine.update(context);
        }
    }

    @Override
    public void onExit(BehaviorContext context) {
        // Stop the sub-state machine
        if (subStateMachine.isRunning()) {
            subStateMachine.stop(context);
        }
        super.onExit(context);
    }

    /**
     * Gets the sub-state machine.
     *
     * @return The sub-state machine
     */
    public StateMachine getSubStateMachine() {
        return subStateMachine;
    }

    /**
     * Gets the current state of the sub-state machine.
     *
     * @return Current sub-state, or null if not running
     */
    public State getCurrentSubState() {
        return subStateMachine.getCurrentState();
    }

    /**
     * Manually starts the sub-state machine.
     *
     * @param context The behavior context
     */
    public void startSubMachine(BehaviorContext context) {
        if (!subStateMachine.isRunning()) {
            subStateMachine.start(context);
        }
    }

    /**
     * Manually stops the sub-state machine.
     *
     * @param context The behavior context
     */
    public void stopSubMachine(BehaviorContext context) {
        if (subStateMachine.isRunning()) {
            subStateMachine.stop(context);
        }
    }

    @Override
    public String toString() {
        return "HierarchicalState{" + getName() + 
            ", subMachine=" + subStateMachine.getName() + 
            ", subState=" + (subStateMachine.getCurrentState() != null ? 
                subStateMachine.getCurrentStateName() : "null") + "}";
    }
}
