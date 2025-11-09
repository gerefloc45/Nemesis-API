package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Action node - wraps a behavior function for use in behavior trees.
 * This is a leaf node that executes a single action.
 * 
 * @author VoidAPI Framework
 * @version 1.0.0
 */
public class ActionNode implements Behavior {
    private final Behavior action;

    /**
     * Creates a new action node with the given behavior.
     *
     * @param action The behavior to execute
     */
    public ActionNode(Behavior action) {
        this.action = action;
    }

    /**
     * Creates a new action node with a lambda expression.
     *
     * @param action The behavior function to execute
     * @return A new action node
     */
    public static ActionNode of(Behavior action) {
        return new ActionNode(action);
    }

    @Override
    public Status execute(BehaviorContext context) {
        return action.execute(context);
    }

    @Override
    public void onStart(BehaviorContext context) {
        action.onStart(context);
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        action.onEnd(context, status);
    }
}
