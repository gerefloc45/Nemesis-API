package com.myname.nemesisapi.api.perception;

import com.myname.nemesisapi.api.BehaviorContext;

/**
 * Base interface for all sensors.
 * Sensors detect and process information about the environment.
 * 
 * @author Nemesis-API Framework
 * @version 1.1.0
 */
public interface Sensor {
    
    /**
     * Updates the sensor with the current context.
     * Called every tick or at configured intervals.
     *
     * @param context The behavior context
     */
    void update(BehaviorContext context);

    /**
     * Gets the update frequency in ticks.
     * Return 1 for every tick, 20 for once per second, etc.
     *
     * @return Update frequency in ticks
     */
    default int getUpdateFrequency() {
        return 1; // Every tick by default
    }

    /**
     * Gets the sensor range in blocks.
     *
     * @return Sensor range
     */
    double getRange();

    /**
     * Checks if the sensor is currently active.
     *
     * @return True if active
     */
    default boolean isActive() {
        return true;
    }

    /**
     * Resets the sensor state.
     *
     * @param context The behavior context
     */
    default void reset(BehaviorContext context) {
        // Default: no-op
    }
}
