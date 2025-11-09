package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.BehaviorNode;

import java.util.Random;

/**
 * Random selector node - randomly selects one child to execute.
 * Each execution picks a new random child.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class RandomSelectorNode extends BehaviorNode {
    private final Random random;
    private Behavior currentChild;
    private boolean hasSelectedChild;

    /**
     * Creates a new random selector node.
     */
    public RandomSelectorNode() {
        super();
        this.random = new Random();
        this.hasSelectedChild = false;
    }

    /**
     * Creates a new random selector node with a specific seed.
     *
     * @param seed Random seed for reproducible behavior
     */
    public RandomSelectorNode(long seed) {
        super();
        this.random = new Random(seed);
        this.hasSelectedChild = false;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (children.isEmpty()) {
            return Status.FAILURE;
        }

        // Select a random child on first execution
        if (!hasSelectedChild) {
            int randomIndex = random.nextInt(children.size());
            currentChild = children.get(randomIndex);
            currentChild.onStart(context);
            hasSelectedChild = true;
        }

        // Execute the selected child
        Status status = currentChild.execute(context);

        // If child completed, reset selection
        if (status != Status.RUNNING) {
            currentChild.onEnd(context, status);
            hasSelectedChild = false;
        }

        return status;
    }

    @Override
    public void onStart(BehaviorContext context) {
        super.onStart(context);
        hasSelectedChild = false;
        currentChild = null;
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (currentChild != null && hasSelectedChild) {
            currentChild.onEnd(context, status);
        }
        super.onEnd(context, status);
        hasSelectedChild = false;
        currentChild = null;
    }

    /**
     * Gets the currently selected child (if any).
     *
     * @return Current child or null
     */
    public Behavior getCurrentChild() {
        return currentChild;
    }
}
