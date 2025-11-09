package com.gerefloc45.voidapi.api.perception;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Memory system for storing perceived entities and their information.
 * Entities are remembered even after they leave sensor range.
 * 
 * @author VoidAPI Framework
 * @version 1.1.0
 */
public class PerceptionMemory {
    private final Map<UUID, MemoryEntry> memories;
    private final float decayTimeSeconds;

    /**
     * Creates a new perception memory with default decay time (10 seconds).
     */
    public PerceptionMemory() {
        this(10.0f);
    }

    /**
     * Creates a new perception memory with custom decay time.
     *
     * @param decayTimeSeconds Time in seconds before memories decay
     */
    public PerceptionMemory(float decayTimeSeconds) {
        this.memories = new HashMap<>();
        this.decayTimeSeconds = decayTimeSeconds;
    }

    /**
     * Records or updates an entity in memory.
     *
     * @param entity The entity to remember
     * @param threatLevel Threat level (0.0 to 1.0)
     */
    public void remember(LivingEntity entity, float threatLevel) {
        UUID uuid = entity.getUuid();
        long currentTime = System.currentTimeMillis();
        Vec3d position = entity.getPos();

        MemoryEntry entry = memories.get(uuid);
        if (entry == null) {
            entry = new MemoryEntry(entity, position, threatLevel, currentTime);
        } else {
            entry.update(position, threatLevel, currentTime);
        }

        memories.put(uuid, entry);
    }

    /**
     * Forgets an entity.
     *
     * @param entityUuid The entity UUID to forget
     */
    public void forget(UUID entityUuid) {
        memories.remove(entityUuid);
    }

    /**
     * Gets a memory entry for an entity.
     *
     * @param entityUuid The entity UUID
     * @return Optional containing the memory entry
     */
    public Optional<MemoryEntry> getMemory(UUID entityUuid) {
        return Optional.ofNullable(memories.get(entityUuid));
    }

    /**
     * Gets all remembered entities.
     *
     * @return List of all memory entries
     */
    public List<MemoryEntry> getAllMemories() {
        return new ArrayList<>(memories.values());
    }

    /**
     * Gets all remembered entities sorted by threat level.
     *
     * @return List of memory entries sorted by threat (highest first)
     */
    public List<MemoryEntry> getMemoriesByThreat() {
        List<MemoryEntry> sorted = new ArrayList<>(memories.values());
        sorted.sort((a, b) -> Float.compare(b.threatLevel, a.threatLevel));
        return sorted;
    }

    /**
     * Updates memory and removes decayed entries.
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        memories.entrySet().removeIf(entry -> {
            float elapsedSeconds = (currentTime - entry.getValue().lastSeenTime) / 1000.0f;
            return elapsedSeconds > decayTimeSeconds;
        });
    }

    /**
     * Clears all memories.
     */
    public void clear() {
        memories.clear();
    }

    /**
     * Gets the number of remembered entities.
     *
     * @return Memory count
     */
    public int getMemoryCount() {
        return memories.size();
    }

    /**
     * Memory entry for a perceived entity.
     */
    public static class MemoryEntry {
        private final UUID entityUuid;
        private Vec3d lastKnownPosition;
        private float threatLevel;
        private long lastSeenTime;
        private long firstSeenTime;

        MemoryEntry(LivingEntity entity, Vec3d position, float threatLevel, long time) {
            this.entityUuid = entity.getUuid();
            this.lastKnownPosition = position;
            this.threatLevel = threatLevel;
            this.lastSeenTime = time;
            this.firstSeenTime = time;
        }

        void update(Vec3d position, float threatLevel, long time) {
            this.lastKnownPosition = position;
            this.threatLevel = Math.max(this.threatLevel, threatLevel); // Keep highest threat
            this.lastSeenTime = time;
        }

        public UUID getEntityUuid() {
            return entityUuid;
        }

        public Vec3d getLastKnownPosition() {
            return lastKnownPosition;
        }

        public float getThreatLevel() {
            return threatLevel;
        }

        public long getLastSeenTime() {
            return lastSeenTime;
        }

        public long getFirstSeenTime() {
            return firstSeenTime;
        }

        public float getTimeSinceLastSeen() {
            return (System.currentTimeMillis() - lastSeenTime) / 1000.0f;
        }

        public float getTotalTimeKnown() {
            return (System.currentTimeMillis() - firstSeenTime) / 1000.0f;
        }
    }
}
