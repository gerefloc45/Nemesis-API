package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.BehaviorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weighted selector node - randomly selects children based on assigned weights.
 * Higher weight = higher probability of selection.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class WeightedSelectorNode extends BehaviorNode {
    private final Random random;
    private final List<Float> weights;
    private Behavior currentChild;
    private boolean hasSelectedChild;
    private float totalWeight;

    /**
     * Creates a new weighted selector node.
     */
    public WeightedSelectorNode() {
        super();
        this.random = new Random();
        this.weights = new ArrayList<>();
        this.hasSelectedChild = false;
        this.totalWeight = 0.0f;
    }

    /**
     * Creates a new weighted selector node with a specific seed.
     *
     * @param seed Random seed for reproducible behavior
     */
    public WeightedSelectorNode(long seed) {
        super();
        this.random = new Random(seed);
        this.weights = new ArrayList<>();
        this.hasSelectedChild = false;
        this.totalWeight = 0.0f;
    }

    /**
     * Adds a child behavior with a specific weight.
     *
     * @param child The child behavior to add
     * @param weight The selection weight (must be > 0)
     * @return This node for method chaining
     */
    public WeightedSelectorNode addChild(Behavior child, float weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
        super.addChild(child);
        weights.add(weight);
        totalWeight += weight;
        return this;
    }

    @Override
    public BehaviorNode addChild(Behavior child) {
        // Default weight of 1.0
        return addChild(child, 1.0f);
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (children.isEmpty()) {
            return Status.FAILURE;
        }

        // Select a weighted random child on first execution
        if (!hasSelectedChild) {
            currentChild = selectWeightedChild();
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

    /**
     * Selects a child based on weighted probability.
     *
     * @return Selected child behavior
     */
    private Behavior selectWeightedChild() {
        float randomValue = random.nextFloat() * totalWeight;
        float cumulativeWeight = 0.0f;

        for (int i = 0; i < children.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue <= cumulativeWeight) {
                return children.get(i);
            }
        }

        // Fallback (should never happen)
        return children.get(children.size() - 1);
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
     * Gets the weight of a specific child.
     *
     * @param index Child index
     * @return Weight value
     */
    public float getWeight(int index) {
        if (index < 0 || index >= weights.size()) {
            throw new IndexOutOfBoundsException("Invalid child index");
        }
        return weights.get(index);
    }

    /**
     * Sets the weight of a specific child.
     *
     * @param index Child index
     * @param weight New weight value (must be > 0)
     */
    public void setWeight(int index, float weight) {
        if (index < 0 || index >= weights.size()) {
            throw new IndexOutOfBoundsException("Invalid child index");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }

        totalWeight -= weights.get(index);
        weights.set(index, weight);
        totalWeight += weight;
    }

    /**
     * Gets the total weight of all children.
     *
     * @return Total weight
     */
    public float getTotalWeight() {
        return totalWeight;
    }

    /**
     * Gets the probability of selecting a specific child.
     *
     * @param index Child index
     * @return Probability (0.0 to 1.0)
     */
    public float getProbability(int index) {
        if (totalWeight == 0) {
            return 0.0f;
        }
        return getWeight(index) / totalWeight;
    }
}
