package com.gerefloc45.voidapi.api.pathfinding;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;

/**
 * Follow entity behavior - follows a target entity at a specified distance.
 * Automatically updates path as target moves.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class FollowEntityBehavior implements Behavior {
    private final String targetEntityKey;
    private final double speed;
    private final double minDistance;
    private final double maxDistance;
    private final boolean stopIfTooClose;
    private long lastUpdateTime;
    private static final long UPDATE_INTERVAL_MS = 500; // Update path twice per second

    /**
     * Creates a follow entity behavior with default settings.
     *
     * @param targetEntityKey Blackboard key for target entity
     */
    public FollowEntityBehavior(String targetEntityKey) {
        this(targetEntityKey, 1.0, 2.0, 10.0, true);
    }

    /**
     * Creates a follow entity behavior with custom distance.
     *
     * @param targetEntityKey Blackboard key for target entity
     * @param speed Movement speed multiplier
     * @param minDistance Minimum distance to maintain
     */
    public FollowEntityBehavior(String targetEntityKey, double speed, double minDistance) {
        this(targetEntityKey, speed, minDistance, minDistance * 5, true);
    }

    /**
     * Creates a follow entity behavior with full configuration.
     *
     * @param targetEntityKey Blackboard key for target entity
     * @param speed Movement speed multiplier
     * @param minDistance Minimum distance to maintain
     * @param maxDistance Maximum distance before giving up
     * @param stopIfTooClose Whether to stop moving if within minDistance
     */
    public FollowEntityBehavior(String targetEntityKey, double speed, double minDistance, double maxDistance, boolean stopIfTooClose) {
        this.targetEntityKey = targetEntityKey;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.stopIfTooClose = stopIfTooClose;
        this.lastUpdateTime = 0;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!(context.getEntity() instanceof MobEntity mobEntity)) {
            return Status.FAILURE;
        }

        // Get target entity from blackboard
        var targetOpt = context.getBlackboard().<Entity>get(targetEntityKey);
        if (targetOpt.isEmpty()) {
            return Status.FAILURE;
        }

        Entity target = targetOpt.get();
        if (!target.isAlive() || target.isRemoved()) {
            return Status.FAILURE;
        }

        double distanceToTarget = mobEntity.distanceTo(target);

        // Check if target is too far
        if (distanceToTarget > maxDistance) {
            return Status.FAILURE;
        }

        // Check if we're close enough
        if (distanceToTarget <= minDistance) {
            if (stopIfTooClose) {
                mobEntity.getNavigation().stop();
            }
            return Status.SUCCESS;
        }

        EntityNavigation navigation = mobEntity.getNavigation();
        long currentTime = System.currentTimeMillis();

        // Update path periodically
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL_MS || navigation.isIdle()) {
            boolean pathFound = navigation.startMovingTo(target, speed);
            lastUpdateTime = currentTime;

            if (!pathFound) {
                return Status.FAILURE;
            }
        }

        return Status.RUNNING;
    }

    @Override
    public void onStart(BehaviorContext context) {
        lastUpdateTime = 0;
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (context.getEntity() instanceof MobEntity mobEntity) {
            mobEntity.getNavigation().stop();
        }
    }

    /**
     * Gets the minimum follow distance.
     *
     * @return Minimum distance
     */
    public double getMinDistance() {
        return minDistance;
    }

    /**
     * Gets the maximum follow distance.
     *
     * @return Maximum distance
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Gets the movement speed multiplier.
     *
     * @return Speed multiplier
     */
    public double getSpeed() {
        return speed;
    }
}
