package com.gerefloc45.voidapi.api.fsm;

import com.gerefloc45.voidapi.api.BehaviorContext;

import java.util.function.Predicate;

/**
 * Represents a transition between two states in a Finite State Machine.
 * Transitions are triggered when their condition evaluates to true.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class Transition {
    private final State fromState;
    private final State toState;
    private final Predicate<BehaviorContext> condition;
    private final int priority;
    private final String name;

    /**
     * Creates a transition with default priority.
     *
     * @param fromState Source state
     * @param toState Target state
     * @param condition Condition that triggers this transition
     */
    public Transition(State fromState, State toState, Predicate<BehaviorContext> condition) {
        this(fromState, toState, condition, 0);
    }

    /**
     * Creates a transition with custom priority.
     * Higher priority transitions are evaluated first.
     *
     * @param fromState Source state
     * @param toState Target state
     * @param condition Condition that triggers this transition
     * @param priority Transition priority (higher = evaluated first)
     */
    public Transition(State fromState, State toState, Predicate<BehaviorContext> condition, int priority) {
        this(fromState, toState, condition, priority, fromState.getName() + "->" + toState.getName());
    }

    /**
     * Creates a named transition with custom priority.
     *
     * @param fromState Source state
     * @param toState Target state
     * @param condition Condition that triggers this transition
     * @param priority Transition priority
     * @param name Transition name for debugging
     */
    public Transition(State fromState, State toState, Predicate<BehaviorContext> condition, int priority, String name) {
        this.fromState = fromState;
        this.toState = toState;
        this.condition = condition;
        this.priority = priority;
        this.name = name;
    }

    /**
     * Checks if this transition should be triggered.
     *
     * @param context The behavior context
     * @return True if condition is met
     */
    public boolean shouldTransition(BehaviorContext context) {
        try {
            return condition.test(context);
        } catch (Exception e) {
            // Log error but don't crash
            return false;
        }
    }

    /**
     * Gets the source state.
     *
     * @return From state
     */
    public State getFromState() {
        return fromState;
    }

    /**
     * Gets the target state.
     *
     * @return To state
     */
    public State getToState() {
        return toState;
    }

    /**
     * Gets the transition priority.
     *
     * @return Priority value
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the transition name.
     *
     * @return Transition name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Transition{" + name + ", priority=" + priority + "}";
    }

    /**
     * Builder for creating transitions with fluent API.
     */
    public static class Builder {
        private State fromState;
        private State toState;
        private Predicate<BehaviorContext> condition;
        private int priority = 0;
        private String name;

        public Builder from(State state) {
            this.fromState = state;
            return this;
        }

        public Builder to(State state) {
            this.toState = state;
            return this;
        }

        public Builder when(Predicate<BehaviorContext> condition) {
            this.condition = condition;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Transition build() {
            if (fromState == null || toState == null || condition == null) {
                throw new IllegalStateException("From state, to state, and condition are required");
            }
            if (name == null) {
                name = fromState.getName() + "->" + toState.getName();
            }
            return new Transition(fromState, toState, condition, priority, name);
        }
    }
}
