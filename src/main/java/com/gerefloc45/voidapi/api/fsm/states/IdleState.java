package com.gerefloc45.voidapi.api.fsm.states;

import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.fsm.State;

/**
 * A simple idle state that does nothing.
 * Useful as a default or waiting state.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class IdleState extends State {
    
    /**
     * Creates an idle state with default name.
     */
    public IdleState() {
        this("Idle");
    }

    /**
     * Creates an idle state with custom name.
     *
     * @param name State name
     */
    public IdleState(String name) {
        super(name);
    }

    @Override
    public void onUpdate(BehaviorContext context) {
        // Do nothing - this is an idle state
    }
}
