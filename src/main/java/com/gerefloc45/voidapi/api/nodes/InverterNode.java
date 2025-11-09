package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

/**
 * Inverter node - inverts the result of its child behavior.
 * SUCCESS becomes FAILURE and vice versa.
 * RUNNING remains RUNNING.
 * 
 * @author VoidAPI Framework
 * @version 1.1.0
 */
public class InverterNode implements Behavior {
    private final Behavior child;

    /**
     * Creates a new inverter node.
     *
     * @param child The child behavior to invert
     */
    public InverterNode(Behavior child) {
        this.child = child;
    }

    @Override
    public Status execute(BehaviorContext context) {
        Status childStatus = child.execute(context);
        
        switch (childStatus) {
            case SUCCESS:
                return Status.FAILURE;
            case FAILURE:
                return Status.SUCCESS;
            case RUNNING:
                return Status.RUNNING;
            default:
                return Status.FAILURE;
        }
    }

    @Override
    public void onStart(BehaviorContext context) {
        child.onStart(context);
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        // Invert status for child callback
        Status invertedStatus = status == Status.SUCCESS ? Status.FAILURE : 
                               status == Status.FAILURE ? Status.SUCCESS : status;
        child.onEnd(context, invertedStatus);
    }
}
