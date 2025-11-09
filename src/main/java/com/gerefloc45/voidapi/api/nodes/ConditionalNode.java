package com.gerefloc45.voidapi.api.nodes;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;

import java.util.function.Predicate;

/**
 * Conditional node - executes child only if condition is true.
 * Returns FAILURE if condition is false without executing child.
 * 
 * @author VoidAPI Framework
 * @version 1.1.0
 */
public class ConditionalNode implements Behavior {
    private final Behavior child;
    private final Predicate<BehaviorContext> condition;

    /**
     * Creates a new conditional node.
     *
     * @param condition The condition to check
     * @param child The child behavior to execute if condition is true
     */
    public ConditionalNode(Predicate<BehaviorContext> condition, Behavior child) {
        this.condition = condition;
        this.child = child;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!condition.test(context)) {
            return Status.FAILURE;
        }

        return child.execute(context);
    }

    @Override
    public void onStart(BehaviorContext context) {
        if (condition.test(context)) {
            child.onStart(context);
        }
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        child.onEnd(context, status);
    }

    /**
     * Creates a conditional node that checks a blackboard value.
     *
     * @param key The blackboard key to check
     * @param child The child behavior
     * @return A new conditional node
     */
    public static ConditionalNode checkBlackboard(String key, Behavior child) {
        return new ConditionalNode(
            ctx -> ctx.getBlackboard().has(key),
            child
        );
    }

    /**
     * Creates a conditional node that checks if a blackboard value equals something.
     *
     * @param key The blackboard key
     * @param expectedValue The expected value
     * @param child The child behavior
     * @return A new conditional node
     */
    public static ConditionalNode checkEquals(String key, Object expectedValue, Behavior child) {
        return new ConditionalNode(
            ctx -> ctx.getBlackboard().get(key)
                .map(value -> value.equals(expectedValue))
                .orElse(false),
            child
        );
    }
}
