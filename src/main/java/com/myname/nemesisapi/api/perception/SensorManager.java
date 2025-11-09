package com.myname.nemesisapi.api.perception;

import com.myname.nemesisapi.api.BehaviorContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages multiple sensors for an entity.
 * Handles sensor updates and lifecycle.
 * 
 * @author Nemesis-API Framework
 * @version 1.1.0
 */
public class SensorManager {
    private final List<Sensor> sensors;
    private final List<Integer> tickCounters;
    private int currentTick;

    /**
     * Creates a new sensor manager.
     */
    public SensorManager() {
        this.sensors = new ArrayList<>();
        this.tickCounters = new ArrayList<>();
        this.currentTick = 0;
    }

    /**
     * Adds a sensor to the manager.
     *
     * @param sensor The sensor to add
     */
    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
        tickCounters.add(0);
    }

    /**
     * Removes a sensor from the manager.
     *
     * @param sensor The sensor to remove
     */
    public void removeSensor(Sensor sensor) {
        int index = sensors.indexOf(sensor);
        if (index >= 0) {
            sensors.remove(index);
            tickCounters.remove(index);
        }
    }

    /**
     * Updates all sensors based on their update frequency.
     *
     * @param context The behavior context
     */
    public void update(BehaviorContext context) {
        currentTick++;

        for (int i = 0; i < sensors.size(); i++) {
            Sensor sensor = sensors.get(i);
            
            if (!sensor.isActive()) {
                continue;
            }

            int counter = tickCounters.get(i);
            counter++;

            if (counter >= sensor.getUpdateFrequency()) {
                sensor.update(context);
                counter = 0;
            }

            tickCounters.set(i, counter);
        }
    }

    /**
     * Resets all sensors.
     *
     * @param context The behavior context
     */
    public void reset(BehaviorContext context) {
        for (Sensor sensor : sensors) {
            sensor.reset(context);
        }
        tickCounters.clear();
        for (int i = 0; i < sensors.size(); i++) {
            tickCounters.add(0);
        }
        currentTick = 0;
    }

    /**
     * Gets all registered sensors.
     *
     * @return List of sensors
     */
    public List<Sensor> getSensors() {
        return new ArrayList<>(sensors);
    }

    /**
     * Gets the number of registered sensors.
     *
     * @return Sensor count
     */
    public int getSensorCount() {
        return sensors.size();
    }

    /**
     * Clears all sensors.
     */
    public void clear() {
        sensors.clear();
        tickCounters.clear();
    }
}
