package com.gerefloc45.voidapi.api.perception;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Memory system for storing perceived entities and their information.
 * Entities are remembered even after they leave sensor range.
 * Enhanced with memory degradation, importance levels, and confidence tracking.
 * 
 * @author VoidAPI Framework
 * @version 0.7.0
 */
public class PerceptionMemory {
    private final Map<UUID, MemoryEntry> memories;
    private final float decayTimeSeconds;
    private final DecayMode decayMode;
    private final boolean useImportanceRetention;

    /**
     * Decay mode for memory degradation.
     */
    public enum DecayMode {
        LINEAR, // Linear decay over time
        EXPONENTIAL, // Exponential decay (faster at first)
        LOGARITHMIC // Logarithmic decay (slower at first)
    }

    /**
     * Creates a new perception memory with default settings.
     * Default: 10 second decay, linear mode, importance retention enabled
     */
    public PerceptionMemory() {
        this(10.0f, DecayMode.LINEAR, true);
    }

    /**
     * Creates a new perception memory with custom decay time.
     *
     * @param decayTimeSeconds Time in seconds before memories fully decay
     */
    public PerceptionMemory(float decayTimeSeconds) {
        this(decayTimeSeconds, DecayMode.LINEAR, true);
    }

    /**
     * Creates a new perception memory with full configuration.
     *
     * @param decayTimeSeconds       Time in seconds before memories fully decay
     * @param decayMode              Decay curve mode
     * @param useImportanceRetention Use importance-based memory retention
     */
    public PerceptionMemory(float decayTimeSeconds, DecayMode decayMode,
            boolean useImportanceRetention) {
        this.memories = new HashMap<>();
        this.decayTimeSeconds = decayTimeSeconds;
        this.decayMode = decayMode;
        this.useImportanceRetention = useImportanceRetention;
    }

    /**
     * Records or updates an entity in memory.
     *
     * @param entity      The entity to remember
     * @param threatLevel Threat level (0.0 to 1.0)
     */
    public void remember(LivingEntity entity, float threatLevel) {
        remember(entity, threatLevel, 0.5f);
    }

    /**
     * Records or updates an entity in memory with importance level.
     *
     * @param entity      The entity to remember
     * @param threatLevel Threat level (0.0 to 1.0)
     * @param importance  Importance level (0.0 to 1.0) - affects retention
     */
    public void remember(LivingEntity entity, float threatLevel, float importance) {
        UUID uuid = entity.getUuid();
        long currentTime = System.currentTimeMillis();
        Vec3d position = entity.getPos();

        MemoryEntry entry = memories.get(uuid);
        if (entry == null) {
            entry = new MemoryEntry(entity, position, threatLevel, importance, currentTime);
        } else {
            entry.refresh(position, threatLevel, importance, currentTime);
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
        MemoryEntry entry = memories.get(entityUuid);
        if (entry != null) {
            entry.updateConfidence(decayTimeSeconds, decayMode);
        }
        return Optional.ofNullable(entry);
    }

    /**
     * Gets all remembered entities.
     *
     * @return List of all memory entries
     */
    public List<MemoryEntry> getAllMemories() {
        List<MemoryEntry> result = new ArrayList<>();
        for (MemoryEntry entry : memories.values()) {
            entry.updateConfidence(decayTimeSeconds, decayMode);
            result.add(entry);
        }
        return result;
    }

    /**
     * Gets all remembered entities sorted by threat level.
     *
     * @return List of memory entries sorted by threat (highest first)
     */
    public List<MemoryEntry> getMemoriesByThreat() {
        List<MemoryEntry> sorted = getAllMemories();
        sorted.sort((a, b) -> Float.compare(b.threatLevel, a.threatLevel));
        return sorted;
    }

    /**
     * Gets memories sorted by confidence level.
     *
     * @return List of memory entries sorted by confidence (highest first)
     */
    public List<MemoryEntry> getMemoriesByConfidence() {
        List<MemoryEntry> sorted = getAllMemories();
        sorted.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        return sorted;
    }

    /**
     * Gets memories sorted by importance.
     *
     * @return List of memory entries sorted by importance (highest first)
     */
    public List<MemoryEntry> getMemoriesByImportance() {
        List<MemoryEntry> sorted = getAllMemories();
        sorted.sort((a, b) -> Float.compare(b.importance, a.importance));
        return sorted;
    }

    /**
     * Updates memory and removes decayed entries.
     * Considers importance levels if enabled.
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        memories.entrySet().removeIf(entry -> {
            MemoryEntry memoryEntry = entry.getValue();
            memoryEntry.updateConfidence(decayTimeSeconds, decayMode);

            float elapsedSeconds = (currentTime - memoryEntry.lastSeenTime) / 1000.0f;
            float effectiveDecayTime = decayTimeSeconds;

            // Extend decay time for important memories
            if (useImportanceRetention) {
                effectiveDecayTime *= (1.0f + memoryEntry.importance);
            }

            // Remove if fully decayed and confidence is too low
            return elapsedSeconds > effectiveDecayTime || memoryEntry.getConfidence() < 0.05f;
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
     * Enhanced with degradation, importance, and confidence tracking.
     */
    public static class MemoryEntry {
        private final UUID entityUuid;
        private Vec3d lastKnownPosition;
        private float threatLevel;
        private float importance;
        private long lastSeenTime;
        private long firstSeenTime;
        private int refreshCount;
        private float confidence; // 0.0 to 1.0, affected by time and refreshes

        MemoryEntry(LivingEntity entity, Vec3d position, float threatLevel,
                float importance, long time) {
            this.entityUuid = entity.getUuid();
            this.lastKnownPosition = position;
            this.threatLevel = threatLevel;
            this.importance = Math.max(0.0f, Math.min(1.0f, importance));
            this.lastSeenTime = time;
            this.firstSeenTime = time;
            this.refreshCount = 1;
            this.confidence = 1.0f;
        }

        /**
         * Refreshes the memory with updated information.
         *
         * @param position    New position
         * @param threatLevel New threat level
         * @param importance  New importance level
         * @param time        Current time
         */
        void refresh(Vec3d position, float threatLevel, float importance, long time) {
            this.lastKnownPosition = position;
            this.threatLevel = Math.max(this.threatLevel, threatLevel); // Keep highest threat
            this.importance = Math.max(this.importance, importance); // Keep highest importance
            this.lastSeenTime = time;
            this.refreshCount++;
            this.confidence = 1.0f; // Reset confidence on refresh
        }

        /**
         * Updates the confidence level based on decay.
         *
         * @param decayTimeSeconds Decay time setting
         * @param decayMode        Decay curve mode
         */
        void updateConfidence(float decayTimeSeconds, DecayMode decayMode) {
            float elapsedSeconds = (System.currentTimeMillis() - lastSeenTime) / 1000.0f;
            float decayProgress = Math.min(1.0f, elapsedSeconds / decayTimeSeconds);

            switch (decayMode) {
                case LINEAR:
                    confidence = 1.0f - decayProgress;
                    break;

                case EXPONENTIAL:
                    // Exponential decay: confidence = e^(-k*t)
                    confidence = (float) Math.exp(-3.0 * decayProgress);
                    break;

                case LOGARITHMIC:
                    // Logarithmic decay: slower at first
                    if (decayProgress < 0.01f) {
                        confidence = 1.0f;
                    } else {
                        confidence = 1.0f - (float) (Math.log(1 + 9 * decayProgress) / Math.log(10));
                    }
                    break;
            }

            // Boost confidence for frequently refreshed memories
            if (refreshCount > 1) {
                float refreshBonus = Math.min(0.2f, refreshCount * 0.02f);
                confidence = Math.min(1.0f, confidence + refreshBonus);
            }

            confidence = Math.max(0.0f, Math.min(1.0f, confidence));
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

        public float getImportance() {
            return importance;
        }

        public long getLastSeenTime() {
            return lastSeenTime;
        }

        public long getFirstSeenTime() {
            return firstSeenTime;
        }

        public int getRefreshCount() {
            return refreshCount;
        }

        public float getConfidence() {
            return confidence;
        }

        public float getTimeSinceLastSeen() {
            return (System.currentTimeMillis() - lastSeenTime) / 1000.0f;
        }

        public float getTotalTimeKnown() {
            return (System.currentTimeMillis() - firstSeenTime) / 1000.0f;
        }

        /**
         * Gets the quality of the memory (confidence weighted by importance).
         *
         * @return Memory quality from 0.0 to 1.0
         */
        public float getMemoryQuality() {
            return confidence * (0.7f + importance * 0.3f);
        }
    }
}
