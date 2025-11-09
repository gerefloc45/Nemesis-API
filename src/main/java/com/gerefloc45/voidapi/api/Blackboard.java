package com.gerefloc45.voidapi.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Blackboard memory system for storing and retrieving arbitrary data per entity.
 * Thread-safe implementation for concurrent access.
 * 
 * @author VoidAPI Framework
 * @version 1.0.0
 */
public class Blackboard {
    private final Map<String, Object> data;

    /**
     * Creates a new empty blackboard.
     */
    public Blackboard() {
        this.data = new HashMap<>();
    }

    /**
     * Sets a value in the blackboard.
     *
     * @param key The key to store the value under
     * @param value The value to store
     */
    public synchronized void set(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Gets a value from the blackboard.
     *
     * @param key The key to retrieve
     * @param <T> The expected type of the value
     * @return Optional containing the value if present and of correct type
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> Optional<T> get(String key) {
        try {
            return Optional.ofNullable((T) data.get(key));
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets a value from the blackboard with a default fallback.
     *
     * @param key The key to retrieve
     * @param defaultValue The default value if key is not present
     * @param <T> The expected type of the value
     * @return The value or default if not present
     */
    public synchronized <T> T getOrDefault(String key, T defaultValue) {
        return this.<T>get(key).orElse(defaultValue);
    }

    /**
     * Checks if a key exists in the blackboard.
     *
     * @param key The key to check
     * @return True if the key exists
     */
    public synchronized boolean has(String key) {
        return data.containsKey(key);
    }

    /**
     * Removes a value from the blackboard.
     *
     * @param key The key to remove
     */
    public synchronized void remove(String key) {
        data.remove(key);
    }

    /**
     * Clears all data from the blackboard.
     */
    public synchronized void clear() {
        data.clear();
    }

    /**
     * Gets the number of entries in the blackboard.
     *
     * @return The size of the blackboard
     */
    public synchronized int size() {
        return data.size();
    }
}
