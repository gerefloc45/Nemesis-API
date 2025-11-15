package com.gerefloc45.voidapi.api.fsm;

import com.gerefloc45.voidapi.api.BehaviorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Finite State Machine implementation.
 * Manages states and transitions between them based on conditions.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class StateMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);

    private final String name;
    private final Map<String, State> states;
    private final List<Transition> transitions;
    private State currentState;
    private State initialState;
    private boolean isRunning;
    private final List<StateChangeListener> listeners;

    /**
     * Creates a new state machine with the given name.
     *
     * @param name State machine name
     */
    public StateMachine(String name) {
        this.name = name;
        this.states = new HashMap<>();
        this.transitions = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.isRunning = false;
    }

    /**
     * Adds a state to this state machine.
     *
     * @param state State to add
     * @return This state machine for chaining
     */
    public StateMachine addState(State state) {
        if (states.containsKey(state.getName())) {
            LOGGER.warn("State '{}' already exists in FSM '{}'", state.getName(), name);
            return this;
        }
        states.put(state.getName(), state);
        
        // Set as initial state if this is the first state
        if (initialState == null) {
            initialState = state;
        }
        
        return this;
    }

    /**
     * Sets the initial state of this state machine.
     *
     * @param stateName Name of the initial state
     * @return This state machine for chaining
     */
    public StateMachine setInitialState(String stateName) {
        State state = states.get(stateName);
        if (state == null) {
            throw new IllegalArgumentException("State '" + stateName + "' not found in FSM '" + name + "'");
        }
        this.initialState = state;
        return this;
    }

    /**
     * Adds a transition between two states.
     *
     * @param transition Transition to add
     * @return This state machine for chaining
     */
    public StateMachine addTransition(Transition transition) {
        // Verify states exist
        if (!states.containsKey(transition.getFromState().getName())) {
            throw new IllegalArgumentException("From state '" + transition.getFromState().getName() + "' not found");
        }
        if (!states.containsKey(transition.getToState().getName())) {
            throw new IllegalArgumentException("To state '" + transition.getToState().getName() + "' not found");
        }
        
        transitions.add(transition);
        // Sort by priority (higher first)
        transitions.sort((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
        
        return this;
    }

    /**
     * Starts the state machine.
     *
     * @param context The behavior context
     */
    public void start(BehaviorContext context) {
        if (isRunning) {
            LOGGER.warn("FSM '{}' is already running", name);
            return;
        }
        
        if (initialState == null) {
            throw new IllegalStateException("No initial state set for FSM '" + name + "'");
        }
        
        isRunning = true;
        transitionTo(initialState, context);
        LOGGER.debug("FSM '{}' started in state '{}'", name, currentState.getName());
    }

    /**
     * Stops the state machine.
     *
     * @param context The behavior context
     */
    public void stop(BehaviorContext context) {
        if (!isRunning) {
            return;
        }
        
        if (currentState != null) {
            currentState.onExit(context);
        }
        
        isRunning = false;
        currentState = null;
        LOGGER.debug("FSM '{}' stopped", name);
    }

    /**
     * Updates the state machine.
     * Checks transitions and updates current state.
     *
     * @param context The behavior context
     */
    public void update(BehaviorContext context) {
        if (!isRunning || currentState == null) {
            return;
        }

        // Check for transitions
        for (Transition transition : transitions) {
            if (transition.getFromState().equals(currentState)) {
                if (transition.shouldTransition(context)) {
                    transitionTo(transition.getToState(), context);
                    return; // Only one transition per update
                }
            }
        }

        // Update current state
        currentState.update(context);
    }

    /**
     * Forces a transition to a specific state.
     *
     * @param stateName Name of the target state
     * @param context The behavior context
     */
    public void forceTransition(String stateName, BehaviorContext context) {
        State state = states.get(stateName);
        if (state == null) {
            throw new IllegalArgumentException("State '" + stateName + "' not found in FSM '" + name + "'");
        }
        transitionTo(state, context);
    }

    /**
     * Internal method to perform state transition.
     *
     * @param newState Target state
     * @param context The behavior context
     */
    private void transitionTo(State newState, BehaviorContext context) {
        State oldState = currentState;
        
        // Exit old state
        if (currentState != null) {
            currentState.onExit(context);
        }
        
        // Enter new state
        currentState = newState;
        currentState.onEnter(context);
        
        // Notify listeners
        notifyStateChange(oldState, newState, context);
        
        LOGGER.debug("FSM '{}' transitioned: {} -> {}", 
            name, 
            oldState != null ? oldState.getName() : "null", 
            newState.getName());
    }

    /**
     * Adds a state change listener.
     *
     * @param listener Listener to add
     */
    public void addListener(StateChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a state change listener.
     *
     * @param listener Listener to remove
     */
    public void removeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all listeners of a state change.
     *
     * @param from Previous state (can be null)
     * @param to New state
     * @param context The behavior context
     */
    private void notifyStateChange(State from, State to, BehaviorContext context) {
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChange(from, to, context);
            } catch (Exception e) {
                LOGGER.error("Error in state change listener", e);
            }
        }
    }

    /**
     * Gets the current state.
     *
     * @return Current state, or null if not running
     */
    public State getCurrentState() {
        return currentState;
    }

    /**
     * Gets the name of the current state.
     *
     * @return Current state name, or null if not running
     */
    public String getCurrentStateName() {
        return currentState != null ? currentState.getName() : null;
    }

    /**
     * Checks if the state machine is running.
     *
     * @return True if running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Gets the name of this state machine.
     *
     * @return State machine name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets all states in this state machine.
     *
     * @return Unmodifiable collection of states
     */
    public Collection<State> getStates() {
        return Collections.unmodifiableCollection(states.values());
    }

    /**
     * Gets all transitions in this state machine.
     *
     * @return Unmodifiable list of transitions
     */
    public List<Transition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    /**
     * Gets a state by name.
     *
     * @param stateName State name
     * @return State, or null if not found
     */
    public State getState(String stateName) {
        return states.get(stateName);
    }

    @Override
    public String toString() {
        return "StateMachine{" + name + ", current=" + 
            (currentState != null ? currentState.getName() : "null") + 
            ", states=" + states.size() + 
            ", transitions=" + transitions.size() + "}";
    }

    /**
     * Listener interface for state changes.
     */
    @FunctionalInterface
    public interface StateChangeListener {
        /**
         * Called when the state machine transitions to a new state.
         *
         * @param from Previous state (can be null)
         * @param to New state
         * @param context The behavior context
         */
        void onStateChange(State from, State to, BehaviorContext context);
    }
}
