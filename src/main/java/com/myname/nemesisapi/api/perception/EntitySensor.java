package com.myname.nemesisapi.api.perception;

import com.myname.nemesisapi.api.BehaviorContext;
import com.myname.nemesisapi.util.EntityUtil;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Sensor for detecting nearby entities.
 * Supports filtering by type and custom predicates.
 * 
 * @author Nemesis-API Framework
 * @version 1.1.0
 */
public class EntitySensor<T extends LivingEntity> implements Sensor {
    private final Class<T> entityClass;
    private final double range;
    private final boolean requireLineOfSight;
    private final Predicate<T> filter;
    private final String blackboardKey;
    private final int updateFrequency;

    /**
     * Creates a new entity sensor.
     *
     * @param entityClass The class of entities to detect
     * @param range Detection range in blocks
     * @param blackboardKey Key to store detected entities in blackboard
     */
    public EntitySensor(Class<T> entityClass, double range, String blackboardKey) {
        this(entityClass, range, blackboardKey, false, entity -> true, 1);
    }

    /**
     * Creates a new entity sensor with full configuration.
     *
     * @param entityClass The class of entities to detect
     * @param range Detection range in blocks
     * @param blackboardKey Key to store detected entities in blackboard
     * @param requireLineOfSight If true, only detect entities in line of sight
     * @param filter Additional filter predicate
     * @param updateFrequency Update frequency in ticks
     */
    public EntitySensor(Class<T> entityClass, double range, String blackboardKey,
                       boolean requireLineOfSight, Predicate<T> filter, int updateFrequency) {
        this.entityClass = entityClass;
        this.range = range;
        this.blackboardKey = blackboardKey;
        this.requireLineOfSight = requireLineOfSight;
        this.filter = filter;
        this.updateFrequency = updateFrequency;
    }

    @Override
    public void update(BehaviorContext context) {
        List<T> detectedEntities = EntityUtil.findEntitiesInRange(
            context.getEntity(),
            range,
            entityClass
        );

        // Apply filters
        List<T> filteredEntities = new ArrayList<>();
        for (T entity : detectedEntities) {
            // Skip self
            if (entity.equals(context.getEntity())) {
                continue;
            }

            // Check line of sight if required
            if (requireLineOfSight && !EntityUtil.canSee(context.getEntity(), entity)) {
                continue;
            }

            // Apply custom filter
            if (!filter.test(entity)) {
                continue;
            }

            filteredEntities.add(entity);
        }

        // Store in blackboard
        context.getBlackboard().set(blackboardKey, filteredEntities);
        context.getBlackboard().set(blackboardKey + "_count", filteredEntities.size());
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
    }

    /**
     * Gets the detected entities from the blackboard.
     *
     * @param context The behavior context
     * @return List of detected entities
     */
    @SuppressWarnings("unchecked")
    public List<T> getDetectedEntities(BehaviorContext context) {
        return context.getBlackboard()
            .<List<T>>get(blackboardKey)
            .orElse(new ArrayList<>());
    }

    /**
     * Gets the nearest detected entity.
     *
     * @param context The behavior context
     * @return The nearest entity, or null if none detected
     */
    public T getNearestEntity(BehaviorContext context) {
        List<T> entities = getDetectedEntities(context);
        if (entities.isEmpty()) {
            return null;
        }

        T nearest = entities.get(0);
        double nearestDistance = EntityUtil.distanceBetween(context.getEntity(), nearest);

        for (T entity : entities) {
            double distance = EntityUtil.distanceBetween(context.getEntity(), entity);
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}
