package com.gerefloc45.voidapi.api.debug;

import com.gerefloc45.voidapi.api.Blackboard;
import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Blackboard inspector - provides runtime inspection of entity blackboard data.
 * Useful for debugging AI behavior and understanding entity state.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class BlackboardInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlackboardInspector.class);
    private static BlackboardInspector instance;

    private final Map<UUID, BlackboardSnapshot> snapshots;
    private final Map<UUID, List<BlackboardChange>> changeHistory;
    private boolean trackChanges;

    /**
     * Snapshot of blackboard state at a point in time.
     */
    public static class BlackboardSnapshot {
        public final UUID entityId;
        public final long timestamp;
        public final Map<String, Object> data;

        public BlackboardSnapshot(UUID entityId, Map<String, Object> data) {
            this.entityId = entityId;
            this.timestamp = System.currentTimeMillis();
            this.data = new HashMap<>(data);
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Blackboard Snapshot ===\n");
            sb.append("Entity: ").append(entityId).append("\n");
            sb.append("Timestamp: ").append(new Date(timestamp)).append("\n");
            sb.append("Data:\n");

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ");
                sb.append(formatValue(entry.getValue())).append("\n");
            }

            return sb.toString();
        }

        private String formatValue(Object value) {
            if (value == null) {
                return "null";
            }
            return value.getClass().getSimpleName() + " = " + value.toString();
        }
    }

    /**
     * Record of a blackboard change.
     */
    public static class BlackboardChange {
        public final String key;
        public final Object oldValue;
        public final Object newValue;
        public final long timestamp;
        public final ChangeType type;

        public enum ChangeType {
            SET, REMOVE, UPDATE
        }

        public BlackboardChange(String key, Object oldValue, Object newValue, ChangeType type) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = System.currentTimeMillis();
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s -> %s", 
                type, key, 
                oldValue != null ? oldValue : "null", 
                newValue != null ? newValue : "null");
        }
    }

    private BlackboardInspector() {
        this.snapshots = new HashMap<>();
        this.changeHistory = new HashMap<>();
        this.trackChanges = false;
    }

    /**
     * Gets the singleton instance.
     *
     * @return Inspector instance
     */
    public static BlackboardInspector getInstance() {
        if (instance == null) {
            instance = new BlackboardInspector();
        }
        return instance;
    }

    /**
     * Enables change tracking.
     */
    public void enableChangeTracking() {
        trackChanges = true;
        LOGGER.info("Blackboard change tracking enabled");
    }

    /**
     * Disables change tracking.
     */
    public void disableChangeTracking() {
        trackChanges = false;
        LOGGER.info("Blackboard change tracking disabled");
    }

    /**
     * Takes a snapshot of an entity's blackboard.
     *
     * @param entity Entity
     * @param blackboard Blackboard to snapshot
     * @return Snapshot
     */
    public BlackboardSnapshot takeSnapshot(Entity entity, Blackboard blackboard) {
        UUID entityId = entity.getUuid();
        
        // Get all data from blackboard (this would need a method in Blackboard class)
        Map<String, Object> data = new HashMap<>();
        // Note: This is a simplified version. Real implementation would need
        // a way to iterate over all blackboard entries.
        
        BlackboardSnapshot snapshot = new BlackboardSnapshot(entityId, data);
        snapshots.put(entityId, snapshot);
        
        LOGGER.debug("Took blackboard snapshot for entity: {}", entityId);
        return snapshot;
    }

    /**
     * Gets the latest snapshot for an entity.
     *
     * @param entity Entity
     * @return Latest snapshot or null
     */
    public BlackboardSnapshot getLatestSnapshot(Entity entity) {
        return snapshots.get(entity.getUuid());
    }

    /**
     * Records a blackboard change.
     *
     * @param entity Entity
     * @param key Blackboard key
     * @param oldValue Old value
     * @param newValue New value
     * @param type Change type
     */
    public void recordChange(Entity entity, String key, Object oldValue, Object newValue, BlackboardChange.ChangeType type) {
        if (!trackChanges) {
            return;
        }

        UUID entityId = entity.getUuid();
        BlackboardChange change = new BlackboardChange(key, oldValue, newValue, type);
        
        changeHistory.computeIfAbsent(entityId, k -> new ArrayList<>()).add(change);

        // Limit history size
        List<BlackboardChange> history = changeHistory.get(entityId);
        if (history.size() > 500) {
            history.remove(0);
        }
    }

    /**
     * Gets the change history for an entity.
     *
     * @param entity Entity
     * @return List of changes
     */
    public List<BlackboardChange> getChangeHistory(Entity entity) {
        return new ArrayList<>(changeHistory.getOrDefault(entity.getUuid(), Collections.emptyList()));
    }

    /**
     * Prints the change history for an entity.
     *
     * @param entity Entity
     * @return Formatted change history
     */
    public String printChangeHistory(Entity entity) {
        List<BlackboardChange> history = getChangeHistory(entity);
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Blackboard Change History ===\n");
        sb.append("Entity: ").append(entity.getUuid()).append("\n");
        sb.append("Total Changes: ").append(history.size()).append("\n\n");

        for (BlackboardChange change : history) {
            sb.append(change.toString()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Compares two snapshots and returns the differences.
     *
     * @param snapshot1 First snapshot
     * @param snapshot2 Second snapshot
     * @return List of differences
     */
    public List<String> compareSnapshots(BlackboardSnapshot snapshot1, BlackboardSnapshot snapshot2) {
        List<String> differences = new ArrayList<>();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(snapshot1.data.keySet());
        allKeys.addAll(snapshot2.data.keySet());

        for (String key : allKeys) {
            Object value1 = snapshot1.data.get(key);
            Object value2 = snapshot2.data.get(key);

            if (value1 == null && value2 != null) {
                differences.add(key + ": added = " + value2);
            } else if (value1 != null && value2 == null) {
                differences.add(key + ": removed");
            } else if (value1 != null && !value1.equals(value2)) {
                differences.add(key + ": " + value1 + " -> " + value2);
            }
        }

        return differences;
    }

    /**
     * Clears all snapshots and history.
     */
    public void clear() {
        snapshots.clear();
        changeHistory.clear();
        LOGGER.info("Cleared all blackboard inspection data");
    }

    /**
     * Clears data for a specific entity.
     *
     * @param entity Entity
     */
    public void clearEntity(Entity entity) {
        UUID entityId = entity.getUuid();
        snapshots.remove(entityId);
        changeHistory.remove(entityId);
    }

    /**
     * Gets statistics about blackboard usage.
     *
     * @param entity Entity
     * @return Statistics string
     */
    public String getStatistics(Entity entity) {
        UUID entityId = entity.getUuid();
        List<BlackboardChange> history = changeHistory.getOrDefault(entityId, Collections.emptyList());

        Map<String, Integer> keyUsage = new HashMap<>();
        for (BlackboardChange change : history) {
            keyUsage.merge(change.key, 1, Integer::sum);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Blackboard Statistics ===\n");
        sb.append("Entity: ").append(entityId).append("\n");
        sb.append("Total Changes: ").append(history.size()).append("\n");
        sb.append("Unique Keys: ").append(keyUsage.size()).append("\n\n");

        sb.append("Most Used Keys:\n");
        keyUsage.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> sb.append("  ").append(entry.getKey())
                .append(": ").append(entry.getValue()).append(" changes\n"));

        return sb.toString();
    }
}
