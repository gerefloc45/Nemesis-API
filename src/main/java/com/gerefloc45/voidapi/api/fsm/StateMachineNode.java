package com.gerefloc45.voidapi.api.fsm;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.BehaviorNode;

/**
 * Behavior tree node that runs a Finite State Machine.
 * Allows FSM to be used as part of a behavior tree.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class StateMachineNode extends BehaviorNode {
    private final StateMachine stateMachine;
    private final String blackboardKey;
    private boolean wasStarted;

    /**
     * Creates a state machine node.
     *
     * @param stateMachine The state machine to run
     */
    public StateMachineNode(StateMachine stateMachine) {
        this(stateMachine, null);
    }

    /**
     * Creates a state machine node with blackboard integration.
     * Stores current state name in blackboard.
     *
     * @param stateMachine The state machine to run
     * @param blackboardKey Key to store current state name (can be null)
     */
    public StateMachineNode(StateMachine stateMachine, String blackboardKey) {
        this.stateMachine = stateMachine;
        this.blackboardKey = blackboardKey;
        this.wasStarted = false;
    }

    @Override
    public Behavior.Status execute(BehaviorContext context) {
        // Start FSM on first tick
        if (!wasStarted) {
            stateMachine.start(context);
            wasStarted = true;
        }

        // Update FSM
        stateMachine.update(context);

        // Store current state in blackboard if key provided
        if (blackboardKey != null && stateMachine.getCurrentState() != null) {
            context.getBlackboard().set(blackboardKey, stateMachine.getCurrentStateName());
        }

        // FSM nodes always return RUNNING (they manage their own lifecycle)
        return Behavior.Status.RUNNING;
    }

    @Override
    protected void reset() {
        super.reset();
        wasStarted = false;
    }

    /**
     * Gets the state machine.
     *
     * @return The state machine
     */
    public StateMachine getStateMachine() {
        return stateMachine;
    }

    @Override
    public String toString() {
        return "StateMachineNode{" + stateMachine.getName() + "}";
    }
}
