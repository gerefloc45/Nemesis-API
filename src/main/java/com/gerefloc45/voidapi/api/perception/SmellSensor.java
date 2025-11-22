package com.gerefloc45.voidapi.api.perception;

import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Predicate;

/**
 * Scent-based tracking sensor.
 * Entities leave scent trails that diffuse over distance and decay over time.
 * Useful for tracking entities beyond visual range.
 * 
 * @author VoidAPI Framework
 * @version 0.7.0
 */
public class SmellSensor<T extends LivingEntity> implements Sensor {
    private final Class<T> entityClass;
    private final double range;
    private final String blackboardKey;
    private final int updateFrequency;
    private final float scentStrength;
    private final float decayRate; // Scent strength loss per second
    private final Predicate<T> filter;
    private final int maxScentMarkers;

    // Scent trail storage (position -> scent data)
    private final Map<BlockPos, ScentMarker> scentTrail;

    /**
     * Creates a basic smell sensor.
     *
     * @param entityClass   The class of entities to track
     * @param range         Detection range in blocks
     * @param blackboardKey Key to store scent data in blackboard
     */
    public SmellSensor(Class<T> entityClass, double range, String blackboardKey) {
        this(entityClass, range, blackboardKey, 1.0f, 0.1f, entity -> true, 20, 500);
    }

    /**
     * Creates a smell sensor with custom configuration.
     *
     * @param entityClass     The class of entities to track
     * @param range           Detection range in blocks
     * @param blackboardKey   Key to store scent data in blackboard
     * @param scentStrength   Initial scent strength (0.0 to 1.0)
     * @param decayRate       Scent decay rate per second (0.0 to 1.0)
     * @param filter          Additional filter predicate
     * @param updateFrequency Update frequency in ticks
     * @param maxScentMarkers Maximum scent markers to track
     */
    public SmellSensor(Class<T> entityClass, double range, String blackboardKey,
            float scentStrength, float decayRate, Predicate<T> filter,
            int updateFrequency, int maxScentMarkers) {
        this.entityClass = entityClass;
        this.range = range;
        this.blackboardKey = blackboardKey;
        this.scentStrength = Math.max(0.0f, Math.min(1.0f, scentStrength));
        this.decayRate = Math.max(0.0f, Math.min(1.0f, decayRate));
        this.filter = filter;
        this.updateFrequency = updateFrequency;
        this.maxScentMarkers = maxScentMarkers;
        this.scentTrail = new LinkedHashMap<BlockPos, ScentMarker>(maxScentMarkers, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<BlockPos, ScentMarker> eldest) {
                return size() > maxScentMarkers;
            }
        };
    }

    @Override
    public void update(BehaviorContext context) {
        LivingEntity observer = context.getEntity();
        long currentTime = System.currentTimeMillis();

        // Update scent markers (decay over time)
        updateScentTrail(currentTime);

        // Detect entities and their scents
        List<T> nearbyEntities = findEntitiesInRange(observer);

        for (T target : nearbyEntities) {
            if (target.equals(observer)) {
                continue;
            }

            if (!filter.test(target)) {
                continue;
            }

            // Add scent marker at entity position
            addScentMarker(target.getBlockPos(), target.getUuid(), scentStrength, currentTime);
        }

        // Find strongest scent in range
        ScentData strongestScent = findStrongestScent(observer.getBlockPos(), currentTime);

        // Store in blackboard
        context.getBlackboard().set(blackboardKey + "_scent", strongestScent);
        context.getBlackboard().set(blackboardKey + "_has_scent", strongestScent != null);

        if (strongestScent != null) {
            context.getBlackboard().set(blackboardKey + "_scent_direction",
                    calculateScentDirection(observer.getPos(), strongestScent.position));
            context.getBlackboard().set(blackboardKey + "_scent_strength",
                    strongestScent.strength);
        }
    }

    /**
     * Updates scent trail, removing expired scents.
     *
     * @param currentTime Current timestamp
     */
    private void updateScentTrail(long currentTime) {
        Iterator<Map.Entry<BlockPos, ScentMarker>> iterator = scentTrail.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ScentMarker> entry = iterator.next();
            ScentMarker marker = entry.getValue();

            // Calculate decay
            float elapsedSeconds = (currentTime - marker.timestamp) / 1000.0f;
            float currentStrength = marker.initialStrength - (decayRate * elapsedSeconds);

            // Remove if too weak
            if (currentStrength <= 0.05f) {
                iterator.remove();
            }
        }
    }

    /**
     * Adds a scent marker at a position.
     *
     * @param pos        Position of the scent
     * @param sourceUuid UUID of the entity that left the scent
     * @param strength   Scent strength
     * @param timestamp  Time the scent was created
     */
    private void addScentMarker(BlockPos pos, UUID sourceUuid, float strength, long timestamp) {
        ScentMarker existing = scentTrail.get(pos);

        if (existing != null && existing.sourceUuid.equals(sourceUuid)) {
            // Refresh existing marker
            existing.timestamp = timestamp;
            existing.initialStrength = Math.max(existing.initialStrength, strength);
        } else {
            // Add new marker
            scentTrail.put(pos, new ScentMarker(sourceUuid, strength, timestamp));
        }
    }

    /**
     * Finds the strongest scent within range.
     *
     * @param observerPos Observer's position
     * @param currentTime Current timestamp
     * @return Strongest scent data, or null if none found
     */
    private ScentData findStrongestScent(BlockPos observerPos, long currentTime) {
        ScentData strongest = null;
        float maxStrength = 0.0f;

        for (Map.Entry<BlockPos, ScentMarker> entry : scentTrail.entrySet()) {
            BlockPos pos = entry.getKey();
            ScentMarker marker = entry.getValue();

            // Calculate distance
            double distance = Math.sqrt(observerPos.getSquaredDistance(pos));

            if (distance > range) {
                continue;
            }

            // Calculate current strength with decay and diffusion
            float elapsedSeconds = (currentTime - marker.timestamp) / 1000.0f;
            float decayedStrength = marker.initialStrength - (decayRate * elapsedSeconds);

            // Scent diffuses (weakens) over distance
            float diffusionFactor = 1.0f - (float) (distance / range);
            float effectiveStrength = decayedStrength * diffusionFactor;

            if (effectiveStrength > maxStrength) {
                maxStrength = effectiveStrength;
                strongest = new ScentData(
                        Vec3d.ofCenter(pos),
                        marker.sourceUuid,
                        effectiveStrength,
                        distance);
            }
        }

        return strongest;
    }

    /**
     * Calculates direction vector to scent source.
     *
     * @param from Observer position
     * @param to   Scent position
     * @return Normalized direction vector
     */
    private Vec3d calculateScentDirection(Vec3d from, Vec3d to) {
        return to.subtract(from).normalize();
    }

    /**
     * Finds entities in range.
     *
     * @param observer The observing entity
     * @return List of entities in range
     */
    private List<T> findEntitiesInRange(LivingEntity observer) {
        return observer.getWorld().getEntitiesByClass(
                entityClass,
                observer.getBoundingBox().expand(range),
                entity -> entity.squaredDistanceTo(observer) <= range * range);
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
        context.getBlackboard().remove(blackboardKey + "_scent");
        context.getBlackboard().remove(blackboardKey + "_has_scent");
        context.getBlackboard().remove(blackboardKey + "_scent_direction");
        context.getBlackboard().remove(blackboardKey + "_scent_strength");
        scentTrail.clear();
    }

    /**
     * Gets the current scent data from blackboard.
     *
     * @param context Behavior context
     * @return Scent data, or null if no scent detected
     */
    public ScentData getScentData(BehaviorContext context) {
        return context.getBlackboard()
                .<ScentData>get(blackboardKey + "_scent")
                .orElse(null);
    }

    /**
     * Checks if a scent is detected.
     *
     * @param context Behavior context
     * @return True if scent detected
     */
    public boolean hasScentDetected(BehaviorContext context) {
        return context.getBlackboard()
                .<Boolean>get(blackboardKey + "_has_scent")
                .orElse(false);
    }

    /**
     * Scent marker data.
     */
    private static class ScentMarker {
        final UUID sourceUuid;
        float initialStrength;
        long timestamp;

        ScentMarker(UUID sourceUuid, float initialStrength, long timestamp) {
            this.sourceUuid = sourceUuid;
            this.initialStrength = initialStrength;
            this.timestamp = timestamp;
        }
    }

    /**
     * Scent data returned to behaviors.
     */
    public static class ScentData {
        public final Vec3d position;
        public final UUID sourceUuid;
        public final float strength;
        public final double distance;

        public ScentData(Vec3d position, UUID sourceUuid, float strength, double distance) {
            this.position = position;
            this.sourceUuid = sourceUuid;
            this.strength = strength;
            this.distance = distance;
        }
    }
}
