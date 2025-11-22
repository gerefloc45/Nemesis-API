package com.gerefloc45.voidapi.api.perception;

import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Attention system for prioritizing and focusing on important stimuli.
 * Limits the number of tracked entities and switches focus based on stimulus
 * priority.
 * 
 * @author VoidAPI Framework
 * @version 0.7.0
 */
public class AttentionSystem implements Sensor {
    private final String blackboardKey;
    private final int maxTrackedEntities;
    private final int updateFrequency;
    private final float focusSwitchThreshold; // Hysteresis for switching focus
    private final double range;

    // Current attention state
    private UUID currentFocusUuid;
    private final Map<UUID, StimulusScore> trackedStimuli;

    /**
     * Creates an attention system with default settings.
     *
     * @param range         Maximum attention range in blocks
     * @param blackboardKey Key prefix for storing attention data
     */
    public AttentionSystem(double range, String blackboardKey) {
        this(range, blackboardKey, 5, 0.3f, 20);
    }

    /**
     * Creates an attention system with custom configuration.
     *
     * @param range                Maximum attention range in blocks
     * @param blackboardKey        Key prefix for storing attention data
     * @param maxTrackedEntities   Maximum number of entities to track
     * @param focusSwitchThreshold Threshold for switching focus (0.0 to 1.0)
     * @param updateFrequency      Update frequency in ticks
     */
    public AttentionSystem(double range, String blackboardKey, int maxTrackedEntities,
            float focusSwitchThreshold, int updateFrequency) {
        this.range = range;
        this.blackboardKey = blackboardKey;
        this.maxTrackedEntities = maxTrackedEntities;
        this.focusSwitchThreshold = Math.max(0.0f, Math.min(1.0f, focusSwitchThreshold));
        this.updateFrequency = updateFrequency;
        this.currentFocusUuid = null;
        this.trackedStimuli = new HashMap<>();
    }

    @Override
    public void update(BehaviorContext context) {
        LivingEntity entity = context.getEntity();

        // Collect all potential stimuli from perception memory and sensors
        Map<UUID, StimulusData> allStimuli = collectStimuli(context, entity);

        // Score each stimulus
        List<ScoredStimulus> scoredStimuli = new ArrayList<>();
        for (Map.Entry<UUID, StimulusData> entry : allStimuli.entrySet()) {
            UUID stimulusUuid = entry.getKey();
            StimulusData data = entry.getValue();

            float score = calculateStimulusScore(entity, data);
            scoredStimuli.add(new ScoredStimulus(stimulusUuid, data, score));

            // Update tracked stimuli
            StimulusScore tracked = trackedStimuli.get(stimulusUuid);
            if (tracked == null) {
                trackedStimuli.put(stimulusUuid, new StimulusScore(score));
            } else {
                tracked.update(score);
            }
        }

        // Sort by score (highest first)
        scoredStimuli.sort((a, b) -> Float.compare(b.score, a.score));

        // Limit to max tracked entities
        if (scoredStimuli.size() > maxTrackedEntities) {
            scoredStimuli = scoredStimuli.subList(0, maxTrackedEntities);
        }

        // Update focus with hysteresis
        updateFocus(scoredStimuli);

        // Clean up old tracked stimuli
        Set<UUID> currentUuids = new HashSet<>();
        for (ScoredStimulus s : scoredStimuli) {
            currentUuids.add(s.uuid);
        }
        trackedStimuli.keySet().retainAll(currentUuids);

        // Store in blackboard
        context.getBlackboard().set(blackboardKey + "_tracked", scoredStimuli);
        context.getBlackboard().set(blackboardKey + "_focus", currentFocusUuid);

        if (currentFocusUuid != null) {
            ScoredStimulus focused = scoredStimuli.stream()
                    .filter(s -> s.uuid.equals(currentFocusUuid))
                    .findFirst()
                    .orElse(null);
            context.getBlackboard().set(blackboardKey + "_focus_data", focused);
        } else {
            context.getBlackboard().remove(blackboardKey + "_focus_data");
        }
    }

    /**
     * Collects all potential stimuli from various sources.
     *
     * @param context Behavior context
     * @param entity  The entity
     * @return Map of UUID to stimulus data
     */
    private Map<UUID, StimulusData> collectStimuli(BehaviorContext context, LivingEntity entity) {
        Map<UUID, StimulusData> stimuli = new HashMap<>();

        // Collect from perception memory
        PerceptionMemory memory = context.getBlackboard()
                .<PerceptionMemory>get("perception_memory")
                .orElse(null);

        if (memory != null) {
            for (PerceptionMemory.MemoryEntry entry : memory.getAllMemories()) {
                double distance = entity.getPos().distanceTo(entry.getLastKnownPosition());
                if (distance <= range) {
                    stimuli.put(entry.getEntityUuid(), new StimulusData(
                            entry.getLastKnownPosition(),
                            distance,
                            entry.getThreatLevel(),
                            entry.getImportance(),
                            entry.getConfidence(),
                            entry.getTimeSinceLastSeen(),
                            StimulusType.MEMORY));
                }
            }
        }

        // Could also collect from entity sensors, sound sensors, etc.
        // For simplicity, we primarily use perception memory

        return stimuli;
    }

    /**
     * Calculates the attention score for a stimulus.
     * Higher scores mean more attention-worthy.
     *
     * @param observer The observing entity
     * @param data     Stimulus data
     * @return Attention score (0.0 to 1.0+)
     */
    private float calculateStimulusScore(LivingEntity observer, StimulusData data) {
        // Multi-factor scoring

        // 1. Proximity factor (closer = more attention)
        float proximityScore = 1.0f - (float) (data.distance / range);
        proximityScore = Math.max(0.0f, proximityScore);

        // 2. Threat factor
        float threatScore = data.threatLevel;

        // 3. Importance factor
        float importanceScore = data.importance;

        // 4. Novelty factor (new stimuli get bonus attention)
        StimulusScore existing = trackedStimuli.values().stream()
                .filter(s -> s.currentScore > 0)
                .findFirst()
                .orElse(null);
        float noveltyScore = existing == null ? 1.0f : 0.5f;

        // 5. Recency factor (recently seen = more reliable)
        float recencyScore = data.timeSinceSeen < 5.0f ? 1.0f : Math.max(0.1f, 1.0f - (data.timeSinceSeen / 30.0f));

        // 6. Confidence factor
        float confidenceScore = data.confidence;

        // Weighted combination
        float combinedScore = proximityScore * 0.25f +
                threatScore * 0.30f +
                importanceScore * 0.20f +
                noveltyScore * 0.10f +
                recencyScore * 0.10f +
                confidenceScore * 0.05f;

        return Math.max(0.0f, Math.min(2.0f, combinedScore));
    }

    /**
     * Updates the current focus with hysteresis to prevent rapid switching.
     *
     * @param scoredStimuli List of scored stimuli (sorted by score)
     */
    private void updateFocus(List<ScoredStimulus> scoredStimuli) {
        if (scoredStimuli.isEmpty()) {
            currentFocusUuid = null;
            return;
        }

        ScoredStimulus highest = scoredStimuli.get(0);

        if (currentFocusUuid == null) {
            // No current focus, take highest
            currentFocusUuid = highest.uuid;
            return;
        }

        // Find current focus in list
        ScoredStimulus currentFocus = scoredStimuli.stream()
                .filter(s -> s.uuid.equals(currentFocusUuid))
                .findFirst()
                .orElse(null);

        if (currentFocus == null) {
            // Current focus no longer tracked, switch to highest
            currentFocusUuid = highest.uuid;
            return;
        }

        // Apply hysteresis: only switch if new target is significantly better
        if (highest.score > currentFocus.score + focusSwitchThreshold) {
            currentFocusUuid = highest.uuid;
        }
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
        context.getBlackboard().remove(blackboardKey + "_tracked");
        context.getBlackboard().remove(blackboardKey + "_focus");
        context.getBlackboard().remove(blackboardKey + "_focus_data");
        trackedStimuli.clear();
        currentFocusUuid = null;
    }

    /**
     * Gets the currently focused stimulus UUID.
     *
     * @param context Behavior context
     * @return UUID of focused stimulus, or null if none
     */
    public UUID getFocusedStimulus(BehaviorContext context) {
        return context.getBlackboard()
                .<UUID>get(blackboardKey + "_focus")
                .orElse(null);
    }

    /**
     * Gets all tracked stimuli.
     *
     * @param context Behavior context
     * @return List of scored stimuli
     */
    public List<ScoredStimulus> getTrackedStimuli(BehaviorContext context) {
        return context.getBlackboard()
                .<List<ScoredStimulus>>get(blackboardKey + "_tracked")
                .orElse(new ArrayList<>());
    }

    /**
     * Stimulus type enumeration.
     */
    public enum StimulusType {
        MEMORY, // From perception memory
        VISUAL, // From visual sensors
        AUDITORY, // From sound sensors
        OLFACTORY, // From smell sensors
        TACTILE // From touch sensors
    }

    /**
     * Stimulus data collected from sensors.
     */
    public static class StimulusData {
        public final Vec3d position;
        public final double distance;
        public final float threatLevel;
        public final float importance;
        public final float confidence;
        public final float timeSinceSeen;
        public final StimulusType type;

        public StimulusData(Vec3d position, double distance, float threatLevel,
                float importance, float confidence, float timeSinceSeen,
                StimulusType type) {
            this.position = position;
            this.distance = distance;
            this.threatLevel = threatLevel;
            this.importance = importance;
            this.confidence = confidence;
            this.timeSinceSeen = timeSinceSeen;
            this.type = type;
        }
    }

    /**
     * Scored stimulus for attention ranking.
     */
    public static class ScoredStimulus {
        public final UUID uuid;
        public final StimulusData data;
        public final float score;

        public ScoredStimulus(UUID uuid, StimulusData data, float score) {
            this.uuid = uuid;
            this.data = data;
            this.score = score;
        }
    }

    /**
     * Tracks stimulus score over time.
     */
    private static class StimulusScore {
        float currentScore;
        long lastUpdateTime;

        StimulusScore(float initialScore) {
            this.currentScore = initialScore;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void update(float newScore) {
            this.currentScore = newScore;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }
}
