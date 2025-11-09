package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.BehaviorNode;

/**
 * Selector node (OR logic) - executes children until one succeeds.
 * Returns SUCCESS if any child succeeds.
 * Returns FAILURE if all children fail.
 * Returns RUNNING if current child is running.
 * 
 * @author VoidAPI Framework
 * @version 1.0.0
 */
public class SelectorNode extends BehaviorNode {
    private int currentChildIndex = 0;

    /**
     * Creates a new selector node.
     */
    public SelectorNode() {
        super();
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (children.isEmpty()) {
            return Status.FAILURE;
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
                    currentChildIndex = 0;
                    return Status.SUCCESS;
                    
                case FAILURE:
                    child.onEnd(context, status);
                    reset();
                    currentChildIndex++;
                    break;
                    
                case RUNNING:
                    return Status.RUNNING;
            }
        }

        // All children failed
        currentChildIndex = 0;
        return Status.FAILURE;
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
