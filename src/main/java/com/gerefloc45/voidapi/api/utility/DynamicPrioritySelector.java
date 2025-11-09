package com.gerefloc45.voidapi.api.utility;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.nodes.SelectorNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A selector that dynamically reorders children based on their priority scores.
 * Unlike standard SelectorNode which tries children in fixed order,
 * this node sorts children by priority before each evaluation.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class DynamicPrioritySelector extends SelectorNode {
    private final List<PrioritizedBehavior> prioritizedBehaviors;
    private boolean needsReordering;
    
    /**
     * Represents a behavior with a dynamic priority scorer.
     */
    private static class PrioritizedBehavior {
        final Behavior behavior;
        final Scorer priorityScorer;
        double lastPriority;
        
        PrioritizedBehavior(Behavior behavior, Scorer priorityScorer) {
            this.behavior = behavior;
            this.priorityScorer = priorityScorer;
            this.lastPriority = 0.0;
        }
    }
    
    /**
     * Creates a new dynamic priority selector.
     */
    public DynamicPrioritySelector() {
        super();
        this.prioritizedBehaviors = new ArrayList<>();
        this.needsReordering = true;
    }
    
    /**
     * Adds a child behavior with dynamic priority.
     * 
     * @param behavior The behavior to add
     * @param priorityScorer Scorer that determines this behavior's priority
     * @return This selector for method chaining
     */
    public DynamicPrioritySelector addPrioritized(Behavior behavior, Scorer priorityScorer) {
        prioritizedBehaviors.add(new PrioritizedBehavior(behavior, priorityScorer));
        needsReordering = true;
        return this;
    }
    
    /**
     * Adds a child behavior with fixed priority.
     * 
     * @param behavior The behavior to add
     * @param priority Fixed priority value (higher = more important)
     * @return This selector for method chaining
     */
    public DynamicPrioritySelector addPrioritized(Behavior behavior, double priority) {
        return addPrioritized(behavior, Scorer.constant(priority));
    }
    
    @Override
    public void onStart(BehaviorContext context) {
        reorderChildren(context);
        super.onStart(context);
    }
    
    @Override
    public Status execute(BehaviorContext context) {
        // Reorder before each execution if needed
        if (needsReordering) {
            reorderChildren(context);
        }
        
        return super.execute(context);
    }
    
    /**
     * Reorders children based on current priority scores.
     * Higher priority behaviors are tried first.
     */
    private void reorderChildren(BehaviorContext context) {
        if (prioritizedBehaviors.isEmpty()) {
            needsReordering = false;
            return;
        }
        
        // Calculate current priorities
        for (PrioritizedBehavior pb : prioritizedBehaviors) {
            pb.lastPriority = pb.priorityScorer.score(context);
        }
        
        // Sort by priority (highest first)
        prioritizedBehaviors.sort(Comparator.comparingDouble((PrioritizedBehavior pb) -> pb.lastPriority).reversed());
        
        // Update parent's children list
        children.clear();
        for (PrioritizedBehavior pb : prioritizedBehaviors) {
            children.add(pb.behavior);
        }
        
        needsReordering = false;
    }
    
    /**
     * Forces reordering on next execution.
     * Useful if you know priorities have changed significantly.
     * 
     * @return This selector for method chaining
     */
    public DynamicPrioritySelector forceReorder() {
        this.needsReordering = true;
        return this;
    }
    
    /**
     * Gets the current priorities for all behaviors.
     * Useful for debugging.
     * 
     * @return List of priorities in current order
     */
    public List<Double> getCurrentPriorities() {
        List<Double> priorities = new ArrayList<>();
        for (PrioritizedBehavior pb : prioritizedBehaviors) {
            priorities.add(pb.lastPriority);
        }
        return priorities;
    }
}
