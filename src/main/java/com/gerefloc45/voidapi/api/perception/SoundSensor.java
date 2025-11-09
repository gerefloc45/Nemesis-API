package com.gerefloc45.voidapi.api.perception;

import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Sensor for detecting sound events in the environment.
 * Tracks sounds played near the entity with configurable filtering.
 * 
 * <p>Note: This is an event-based sensor that requires integration with
 * Minecraft's sound system. For full functionality, register a sound event listener
 * in your mod initialization.</p>
 * 
 * @author VoidAPI Framework
 * @version 1.1.0
 */
public class SoundSensor implements Sensor {
    private final double range;
    private final String blackboardKey;
    private final int updateFrequency;
    private final Predicate<SoundEvent> soundFilter;
    private final long memoryDuration; // in milliseconds
    
    // Global sound event registry (shared across all sensors)
    private static final Map<UUID, List<DetectedSound>> GLOBAL_SOUND_EVENTS = new ConcurrentHashMap<>();
    
    /**
     * Represents a detected sound event.
     */
    public static class DetectedSound {
        private final SoundEvent sound;
        private final Vec3d position;
        private final SoundCategory category;
        private final float volume;
        private final float pitch;
        private final long timestamp;

        public DetectedSound(SoundEvent sound, Vec3d position, SoundCategory category, 
                           float volume, float pitch) {
            this.sound = sound;
            this.position = position;
            this.category = category;
            this.volume = volume;
            this.pitch = pitch;
            this.timestamp = System.currentTimeMillis();
        }

        public SoundEvent getSound() {
            return sound;
        }

        public Vec3d getPosition() {
            return position;
        }

        public SoundCategory getCategory() {
            return category;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isExpired(long duration) {
            return System.currentTimeMillis() - timestamp > duration;
        }
    }

    /**
     * Creates a new sound sensor.
     *
     * @param range Detection range in blocks
     * @param blackboardKey Key to store detected sounds in blackboard
     */
    public SoundSensor(double range, String blackboardKey) {
        this(range, blackboardKey, sound -> true, 1, 5000);
    }

    /**
     * Creates a new sound sensor with full configuration.
     *
     * @param range Detection range in blocks
     * @param blackboardKey Key to store detected sounds in blackboard
     * @param soundFilter Predicate to filter which sounds to detect
     * @param updateFrequency Update frequency in ticks
     * @param memoryDuration How long to remember sounds in milliseconds
     */
    public SoundSensor(double range, String blackboardKey, Predicate<SoundEvent> soundFilter,
                      int updateFrequency, long memoryDuration) {
        this.range = range;
        this.blackboardKey = blackboardKey;
        this.soundFilter = soundFilter;
        this.updateFrequency = updateFrequency;
        this.memoryDuration = memoryDuration;
    }

    @Override
    public void update(BehaviorContext context) {
        LivingEntity entity = context.getEntity();
        UUID entityId = entity.getUuid();
        Vec3d entityPos = entity.getPos();

        // Get sounds detected for this entity
        List<DetectedSound> allSounds = GLOBAL_SOUND_EVENTS.getOrDefault(entityId, new ArrayList<>());
        
        // Filter by range, time, and custom filter
        List<DetectedSound> recentSounds = new ArrayList<>();
        for (DetectedSound sound : allSounds) {
            // Check if expired
            if (sound.isExpired(memoryDuration)) {
                continue;
            }

            // Check range
            double distance = entityPos.distanceTo(sound.getPosition());
            if (distance > range) {
                continue;
            }

            // Apply custom filter
            if (!soundFilter.test(sound.getSound())) {
                continue;
            }

            recentSounds.add(sound);
        }

        // Clean up expired sounds
        allSounds.removeIf(sound -> sound.isExpired(memoryDuration));

        // Store in blackboard
        context.getBlackboard().set(blackboardKey, recentSounds);
        context.getBlackboard().set(blackboardKey + "_count", recentSounds.size());

        // Store nearest sound if any detected
        if (!recentSounds.isEmpty()) {
            DetectedSound nearest = findNearestSound(entityPos, recentSounds);
            context.getBlackboard().set(blackboardKey + "_nearest", nearest);
        }
    }

    /**
     * Finds the nearest sound to the given position.
     */
    private DetectedSound findNearestSound(Vec3d center, List<DetectedSound> sounds) {
        DetectedSound nearest = sounds.get(0);
        double nearestDist = center.distanceTo(nearest.getPosition());

        for (DetectedSound sound : sounds) {
            double dist = center.distanceTo(sound.getPosition());
            if (dist < nearestDist) {
                nearest = sound;
                nearestDist = dist;
            }
        }

        return nearest;
    }

    @Override
    public double getRange() {
        return range;
    }

    @Override
    public int getUpdateFrequency() {
        return updateFrequency;
    }

    @Override
    public void reset(BehaviorContext context) {
        context.getBlackboard().remove(blackboardKey);
        context.getBlackboard().remove(blackboardKey + "_count");
        context.getBlackboard().remove(blackboardKey + "_nearest");
        
        // Clear sounds for this entity
        UUID entityId = context.getEntity().getUuid();
        GLOBAL_SOUND_EVENTS.remove(entityId);
    }

    /**
     * Gets the detected sounds from the blackboard.
     *
     * @param context The behavior context
     * @return List of detected sounds
     */
    public List<DetectedSound> getDetectedSounds(BehaviorContext context) {
        return context.getBlackboard()
            .<List<DetectedSound>>get(blackboardKey)
            .orElse(new ArrayList<>());
    }

    /**
     * Gets the nearest detected sound.
     *
     * @param context The behavior context
     * @return The nearest sound, or null if none detected
     */
    public DetectedSound getNearestSound(BehaviorContext context) {
        return context.getBlackboard()
            .<DetectedSound>get(blackboardKey + "_nearest")
            .orElse(null);
    }

    /**
     * Registers a sound event for all listening entities.
     * This should be called from a sound event listener in your mod.
     *
     * @param sound The sound event
     * @param position The position where the sound was played
     * @param category The sound category
     * @param volume The sound volume
     * @param pitch The sound pitch
     */
    public static void registerSoundEvent(SoundEvent sound, Vec3d position, SoundCategory category,
                                         float volume, float pitch) {
        DetectedSound detectedSound = new DetectedSound(sound, position, category, volume, pitch);
        
        // Add to all entities (they will filter by range in their update)
        for (UUID entityId : GLOBAL_SOUND_EVENTS.keySet()) {
            GLOBAL_SOUND_EVENTS.get(entityId).add(detectedSound);
        }
    }

    /**
     * Registers an entity to receive sound events.
     * Call this when adding a SoundSensor to an entity.
     *
     * @param entityId The entity UUID
     */
    public static void registerEntity(UUID entityId) {
        GLOBAL_SOUND_EVENTS.putIfAbsent(entityId, Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * Unregisters an entity from receiving sound events.
     * Call this when removing a SoundSensor from an entity.
     *
     * @param entityId The entity UUID
     */
    public static void unregisterEntity(UUID entityId) {
        GLOBAL_SOUND_EVENTS.remove(entityId);
    }

    /**
     * Builder for creating SoundSensor instances.
     */
    public static class Builder {
        private double range;
        private String blackboardKey;
        private Predicate<SoundEvent> soundFilter = sound -> true;
        private int updateFrequency = 1;
        private long memoryDuration = 5000;

        public Builder range(double range) {
            this.range = range;
            return this;
        }

        public Builder blackboardKey(String key) {
            this.blackboardKey = key;
            return this;
        }

        public Builder filterByCategory(SoundCategory category) {
            this.soundFilter = sound -> true; // Category check would need additional data
            return this;
        }

        public Builder filterByPredicate(Predicate<SoundEvent> predicate) {
            this.soundFilter = predicate;
            return this;
        }

        public Builder updateFrequency(int frequency) {
            this.updateFrequency = frequency;
            return this;
        }

        public Builder memoryDuration(long milliseconds) {
            this.memoryDuration = milliseconds;
            return this;
        }

        public SoundSensor build() {
            if (range <= 0 || blackboardKey == null) {
                throw new IllegalStateException("Range and blackboardKey must be set");
            }
            return new SoundSensor(range, blackboardKey, soundFilter, updateFrequency, memoryDuration);
        }
    }

    /**
     * Creates a new builder for SoundSensor.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
