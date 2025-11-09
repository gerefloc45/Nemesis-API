package com.myname.nemesisapi.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/**
 * Utility class for common entity operations in AI behaviors.
 * Provides helper methods for entity queries and spatial operations.
 * 
 * @author Nemesis-API Framework
 * @version 1.0.0
 */
public class EntityUtil {

    /**
     * Finds the nearest player to an entity within a given range.
     *
     * @param entity The entity to search from
     * @param range The search range
     * @return Optional containing the nearest player, or empty if none found
     */
    public static Optional<PlayerEntity> findNearestPlayer(LivingEntity entity, double range) {
        World world = entity.getWorld();
        return Optional.ofNullable(world.getClosestPlayer(entity, range));
    }

    /**
     * Finds all living entities within a range of a position.
     *
     * @param world The world to search in
     * @param pos The center position
     * @param range The search range
     * @param entityClass The class of entities to find
     * @param <T> The entity type
     * @return List of entities found
     */
    public static <T extends LivingEntity> List<T> findEntitiesInRange(
            World world, BlockPos pos, double range, Class<T> entityClass) {
        Box box = new Box(pos).expand(range);
        return world.getEntitiesByClass(entityClass, box, entity -> true);
    }

    /**
     * Finds all living entities within a range of an entity.
     *
     * @param entity The entity to search from
     * @param range The search range
     * @param entityClass The class of entities to find
     * @param <T> The entity type
     * @return List of entities found
     */
    public static <T extends LivingEntity> List<T> findEntitiesInRange(
            LivingEntity entity, double range, Class<T> entityClass) {
        return findEntitiesInRange(entity.getWorld(), entity.getBlockPos(), range, entityClass);
    }

    /**
     * Calculates the distance between two entities.
     *
     * @param entity1 First entity
     * @param entity2 Second entity
     * @return The distance between the entities
     */
    public static double distanceBetween(LivingEntity entity1, LivingEntity entity2) {
        return entity1.getPos().distanceTo(entity2.getPos());
    }

    /**
     * Checks if an entity can see another entity (line of sight).
     *
     * @param entity The entity looking
     * @param target The target entity
     * @return True if the entity can see the target
     */
    public static boolean canSee(LivingEntity entity, LivingEntity target) {
        return entity.canSee(target);
    }

    /**
     * Checks if an entity is within range of another entity.
     *
     * @param entity The entity to check from
     * @param target The target entity
     * @param range The range to check
     * @return True if within range
     */
    public static boolean isInRange(LivingEntity entity, LivingEntity target, double range) {
        return distanceBetween(entity, target) <= range;
    }

    /**
     * Gets the horizontal distance between two entities (ignoring Y axis).
     *
     * @param entity1 First entity
     * @param entity2 Second entity
     * @return The horizontal distance
     */
    public static double horizontalDistanceBetween(LivingEntity entity1, LivingEntity entity2) {
        double dx = entity1.getX() - entity2.getX();
        double dz = entity1.getZ() - entity2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Checks if an entity is on the ground.
     *
     * @param entity The entity to check
     * @return True if on ground
     */
    public static boolean isOnGround(LivingEntity entity) {
        return entity.isOnGround();
    }

    /**
     * Checks if an entity is in water.
     *
     * @param entity The entity to check
     * @return True if in water
     */
    public static boolean isInWater(LivingEntity entity) {
        return entity.isTouchingWater();
    }

    /**
     * Checks if an entity is alive and not removed.
     *
     * @param entity The entity to check
     * @return True if valid
     */
    public static boolean isValid(LivingEntity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }
}
