package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.BehaviorNode;

/**
 * Sequence node (AND logic) - executes children until one fails.
 * Returns SUCCESS if all children succeed.
 * Returns FAILURE if any child fails.
 * Returns RUNNING if current child is running.
 * 
 * @author VoidAPI Framework
 * @version 1.0.0
 */
public class SequenceNode extends BehaviorNode {
    private int currentChildIndex = 0;

    /**
     * Creates a new sequence node.
     */
    public SequenceNode() {
        super();
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (children.isEmpty()) {
            return Status.SUCCESS;
        }

        while (currentChildIndex < children.size()) {
            Behavior child = children.get(currentChildIndex);
            
            if (!isStarted()) {
                child.onStart(context);
                markStarted();
            }

            Status status = child.execute(context);

            switch (status) {
                case SUCCESS:
                    child.onEnd(context, status);
                    reset();
                    currentChildIndex++;
                    break;
                    
                case FAILURE:
                    child.onEnd(context, status);
                    reset();
                    currentChildIndex = 0;
                    return Status.FAILURE;
                    
                case RUNNING:
                    return Status.RUNNING;
            }
        }

        // All children succeeded
        currentChildIndex = 0;
        return Status.SUCCESS;
    }

    @Override
    public void onStart(BehaviorContext context) {
        super.onStart(context);
        currentChildIndex = 0;
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        super.onEnd(context, status);
        currentChildIndex = 0;
    }
}
