package com.gerefloc45.voidapi.api.perception;

import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;

/**
 * Advanced line-of-sight sensor with occlusion detection.
 * Uses raycasting to determine if entities are visible, accounting for block
 * occlusion.
 * 
 * @author VoidAPI Framework
 * @version 0.7.0
 */
public class LineOfSightSensor<T extends LivingEntity> implements Sensor {
    private final Class<T> entityClass;
    private final double range;
    private final String blackboardKey;
    private final int updateFrequency;
    private final VisionCone visionCone;
    private final boolean allowTransparentBlocks;
    private final Predicate<T> filter;

    // Cache for raycast results
    private final Map<UUID, CachedRaycastResult> raycastCache;
    private final long cacheLifetimeMs;

    /**
     * Creates a basic line-of-sight sensor.
     *
     * @param entityClass   The class of entities to detect
     * @param range         Detection range in blocks
     * @param blackboardKey Key to store detected entities in blackboard
     */
    public LineOfSightSensor(Class<T> entityClass, double range, String blackboardKey) {
        this(entityClass, range, blackboardKey, new VisionCone(range),
                true, entity -> true, 1, 100);
    }

    /**
     * Creates a line-of-sight sensor with vision cone.
     *
     * @param entityClass   The class of entities to detect
     * @param range         Detection range in blocks
     * @param blackboardKey Key to store detected entities in blackboard
     * @param visionCone    Vision cone for FOV filtering
     */
    public LineOfSightSensor(Class<T> entityClass, double range, String blackboardKey,
            VisionCone visionCone) {
        this(entityClass, range, blackboardKey, visionCone, true, entity -> true, 1, 100);
    }

    /**
     * Creates a line-of-sight sensor with full configuration.
     *
     * @param entityClass            The class of entities to detect
     * @param range                  Detection range in blocks
     * @param blackboardKey          Key to store detected entities in blackboard
     * @param visionCone             Vision cone for FOV filtering
     * @param allowTransparentBlocks Allow seeing through glass, leaves, etc.
     * @param filter                 Additional filter predicate
     * @param updateFrequency        Update frequency in ticks
     * @param cacheLifetimeMs        Raycast cache lifetime in milliseconds
     */
    public LineOfSightSensor(Class<T> entityClass, double range, String blackboardKey,
            VisionCone visionCone, boolean allowTransparentBlocks,
            Predicate<T> filter, int updateFrequency, long cacheLifetimeMs) {
        this.entityClass = entityClass;
        this.range = range;
        this.blackboardKey = blackboardKey;
        this.visionCone = visionCone;
        this.allowTransparentBlocks = allowTransparentBlocks;
        this.filter = filter;
        this.updateFrequency = updateFrequency;
        this.cacheLifetimeMs = cacheLifetimeMs;
        this.raycastCache = new HashMap<>();
    }

    @Override
    public void update(BehaviorContext context) {
        LivingEntity observer = context.getEntity();
        World world = observer.getWorld();

        // Find entities in range
        List<T> nearbyEntities = findEntitiesInRange(observer, world);
        List<T> visibleEntities = new ArrayList<>();

        long currentTime = System.currentTimeMillis();

        for (T target : nearbyEntities) {
            // Skip self
            if (target.equals(observer)) {
                continue;
            }

            // Apply custom filter
            if (!filter.test(target)) {
                continue;
            }

            // Check vision cone
            if (!visionCone.isInVisionCone(observer, target)) {
                continue;
            }

            // Check line of sight with caching
            if (hasLineOfSight(observer, target, world, currentTime)) {
                visibleEntities.add(target);
            }
        }

        // Clean up old cache entries
        raycastCache.entrySet().removeIf(entry -> currentTime - entry.getValue().timestamp > cacheLifetimeMs);

        // Store in blackboard
        context.getBlackboard().set(blackboardKey, visibleEntities);
        context.getBlackboard().set(blackboardKey + "_count", visibleEntities.size());

        // Store visibility factors
        Map<UUID, Float> visibilityFactors = new HashMap<>();
        for (T entity : visibleEntities) {
            float factor = visionCone.getVisibilityFactor(observer, entity.getEyePos());
            visibilityFactors.put(entity.getUuid(), factor);
        }
        context.getBlackboard().set(blackboardKey + "_visibility", visibilityFactors);
    }

    /**
     * Checks if the observer has line of sight to the target.
     * Uses caching to improve performance.
     *
     * @param observer    The observing entity
     * @param target      The target entity
     * @param world       The world
     * @param currentTime Current timestamp for cache management
     * @return True if line of sight exists
     */
    private boolean hasLineOfSight(LivingEntity observer, T target, World world, long currentTime) {
        UUID targetUuid = target.getUuid();

        // Check cache
        CachedRaycastResult cached = raycastCache.get(targetUuid);
        if (cached != null && currentTime - cached.timestamp < cacheLifetimeMs) {
            // Verify entity hasn't moved significantly
            if (cached.targetPos.distanceTo(target.getPos()) < 0.5) {
                return cached.hasLineOfSight;
            }
        }

        // Perform raycast
        Vec3d observerEye = observer.getEyePos();
        Vec3d targetEye = target.getEyePos();

        boolean hasLOS = performRaycast(world, observerEye, targetEye);

        // Cache result
        raycastCache.put(targetUuid, new CachedRaycastResult(
                hasLOS, target.getPos(), currentTime));

        return hasLOS;
    }

    /**
     * Performs a raycast between two points.
     *
     * @param world The world
     * @param from  Start position
     * @param to    End position
     * @return True if line of sight exists (no solid blocks in the way)
     */
    private boolean performRaycast(World world, Vec3d from, Vec3d to) {
        RaycastContext.FluidHandling fluidHandling = RaycastContext.FluidHandling.NONE;

        RaycastContext context = new RaycastContext(
                from,
                to,
                RaycastContext.ShapeType.COLLIDER,
                fluidHandling,
                (net.minecraft.entity.Entity) null);

        BlockHitResult result = world.raycast(context);

        if (result.getType() == HitResult.Type.MISS) {
            return true; // No blocks hit
        }

        // Check if we hit a transparent block and it's allowed
        if (allowTransparentBlocks) {
            BlockPos hitPos = result.getBlockPos();
            BlockState hitState = world.getBlockState(hitPos);

            // Consider transparent blocks as "see-through"
            if (!hitState.isOpaque() || hitState.getBlock().getBlastResistance() < 0.5f) {
                // Continue raycast from hit point to check further blocks
                Vec3d newFrom = result.getPos().add(
                        to.subtract(from).normalize().multiply(0.1));

                if (newFrom.distanceTo(to) > 0.2) {
                    return performRaycast(world, newFrom, to);
                }
                return true;
            }
        }

        return false; // Solid block in the way
    }

    /**
     * Finds entities in range.
     *
     * @param observer The observing entity
     * @param world    The world
     * @return List of entities in range
     */
    private List<T> findEntitiesInRange(LivingEntity observer, World world) {
        return world.getEntitiesByClass(
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
        context.getBlackboard().remove(blackboardKey);
        context.getBlackboard().remove(blackboardKey + "_count");
        context.getBlackboard().remove(blackboardKey + "_visibility");
        raycastCache.clear();
    }

    /**
     * Gets the detected entities from the blackboard.
     *
     * @param context The behavior context
     * @return List of visible entities
     */
    public List<T> getVisibleEntities(BehaviorContext context) {
        return context.getBlackboard()
                .<List<T>>get(blackboardKey)
                .orElse(new ArrayList<>());
    }

    /**
     * Gets the visibility factor for an entity.
     *
     * @param context    The behavior context
     * @param entityUuid The entity UUID
     * @return Visibility factor (0.0 to 1.0), or 0.0 if not visible
     */
    public float getVisibilityFactor(BehaviorContext context, UUID entityUuid) {
        return context.getBlackboard()
                .<Map<UUID, Float>>get(blackboardKey + "_visibility")
                .map(map -> map.getOrDefault(entityUuid, 0.0f))
                .orElse(0.0f);
    }

    /**
     * Cached raycast result to improve performance.
     */
    private static class CachedRaycastResult {
        final boolean hasLineOfSight;
        final Vec3d targetPos;
        final long timestamp;

        CachedRaycastResult(boolean hasLineOfSight, Vec3d targetPos, long timestamp) {
            this.hasLineOfSight = hasLineOfSight;
            this.targetPos = targetPos;
            this.timestamp = timestamp;
        }
    }
}
