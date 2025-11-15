package com.gerefloc45.voidapi.api.fsm;

import com.gerefloc45.voidapi.api.BehaviorContext;

import java.util.function.Predicate;

/**
 * Fluent builder for creating state machines.
 * Simplifies FSM construction with a clean API.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class StateMachineBuilder {
    private final StateMachine stateMachine;
    private State lastAddedState;

    /**
     * Creates a new builder for a state machine.
     *
     * @param name State machine name
     */
    public StateMachineBuilder(String name) {
        this.stateMachine = new StateMachine(name);
    }

    /**
     * Adds a state to the state machine.
     *
     * @param state State to add
     * @return This builder for chaining
     */
    public StateMachineBuilder state(State state) {
        stateMachine.addState(state);
        lastAddedState = state;
        return this;
    }

    /**
     * Sets the initial state.
     *
     * @param stateName Initial state name
     * @return This builder for chaining
     */
    public StateMachineBuilder initialState(String stateName) {
        stateMachine.setInitialState(stateName);
        return this;
    }

    /**
     * Adds a transition from the last added state.
     *
     * @param toState Target state
     * @param condition Transition condition
     * @return This builder for chaining
     */
    public StateMachineBuilder transitionTo(State toState, Predicate<BehaviorContext> condition) {
        if (lastAddedState == null) {
            throw new IllegalStateException("No state added yet. Call state() first.");
        }
        stateMachine.addTransition(new Transition(lastAddedState, toState, condition));
        return this;
    }

    /**
     * Adds a transition with priority from the last added state.
     *
     * @param toState Target state
     * @param condition Transition condition
     * @param priority Transition priority
     * @return This builder for chaining
     */
    public StateMachineBuilder transitionTo(State toState, Predicate<BehaviorContext> condition, int priority) {
        if (lastAddedState == null) {
            throw new IllegalStateException("No state added yet. Call state() first.");
        }
        stateMachine.addTransition(new Transition(lastAddedState, toState, condition, priority));
        return this;
    }

    /**
     * Adds a named transition from the last added state.
     *
     * @param toState Target state
     * @param condition Transition condition
     * @param priority Transition priority
     * @param name Transition name
     * @return This builder for chaining
     */
    public StateMachineBuilder transitionTo(State toState, Predicate<BehaviorContext> condition, int priority, String name) {
        if (lastAddedState == null) {
            throw new IllegalStateException("No state added yet. Call state() first.");
        }
        stateMachine.addTransition(new Transition(lastAddedState, toState, condition, priority, name));
        return this;
    }

    /**
     * Adds a custom transition.
     *
     * @param transition Transition to add
     * @return This builder for chaining
     */
    public StateMachineBuilder transition(Transition transition) {
        stateMachine.addTransition(transition);
        return this;
    }

    /**
     * Adds a state change listener.
     *
     * @param listener Listener to add
     * @return This builder for chaining
     */
    public StateMachineBuilder listener(StateMachine.StateChangeListener listener) {
        stateMachine.addListener(listener);
        return this;
    }

    /**
     * Builds and returns the state machine.
     *
     * @return The constructed state machine
     */
    public StateMachine build() {
        return stateMachine;
    }

    /**
     * Creates a new builder.
     *
     * @param name State machine name
     * @return New builder instance
     */
    public static StateMachineBuilder create(String name) {
        return new StateMachineBuilder(name);
    }
}
