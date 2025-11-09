package com.gerefloc45.voidapi.api.pathfinding;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Flee from entity behavior - moves away from a threatening entity.
 * Calculates escape direction and navigates to safety.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class FleeFromEntityBehavior implements Behavior {
    private final String threatEntityKey;
    private final double speed;
    private final double fleeDistance;
    private final double safeDistance;
    private final int fleeRange;
    private BlockPos fleeTarget;
    private long lastRecalculateTime;
    private static final long RECALCULATE_INTERVAL_MS = 1000;

    /**
     * Creates a flee behavior with default settings.
     *
     * @param threatEntityKey Blackboard key for threat entity
     */
    public FleeFromEntityBehavior(String threatEntityKey) {
        this(threatEntityKey, 1.2, 16.0, 24.0, 16);
    }

    /**
     * Creates a flee behavior with custom speed and distance.
     *
     * @param threatEntityKey Blackboard key for threat entity
     * @param speed Movement speed multiplier (usually > 1.0 for fleeing)
     * @param fleeDistance Distance to flee from threat
     */
    public FleeFromEntityBehavior(String threatEntityKey, double speed, double fleeDistance) {
        this(threatEntityKey, speed, fleeDistance, fleeDistance * 1.5, (int) fleeDistance);
    }

    /**
     * Creates a flee behavior with full configuration.
     *
     * @param threatEntityKey Blackboard key for threat entity
     * @param speed Movement speed multiplier
     * @param fleeDistance Minimum distance to maintain from threat
     * @param safeDistance Distance at which entity feels safe
     * @param fleeRange Maximum range to search for flee position
     */
    public FleeFromEntityBehavior(String threatEntityKey, double speed, double fleeDistance, 
                                  double safeDistance, int fleeRange) {
        this.threatEntityKey = threatEntityKey;
        this.speed = speed;
        this.fleeDistance = fleeDistance;
        this.safeDistance = safeDistance;
        this.fleeRange = fleeRange;
        this.fleeTarget = null;
        this.lastRecalculateTime = 0;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!(context.getEntity() instanceof MobEntity mobEntity)) {
            return Status.FAILURE;
        }

        // Get threat entity from blackboard
        var threatOpt = context.getBlackboard().<Entity>get(threatEntityKey);
        if (threatOpt.isEmpty()) {
            return Status.FAILURE;
        }

        Entity threat = threatOpt.get();
        if (!threat.isAlive() || threat.isRemoved()) {
            return Status.SUCCESS; // Threat gone, no need to flee
        }

        double distanceToThreat = mobEntity.distanceTo(threat);

        // Check if we're safe
        if (distanceToThreat >= safeDistance) {
            mobEntity.getNavigation().stop();
            return Status.SUCCESS;
        }

        EntityNavigation navigation = mobEntity.getNavigation();
        long currentTime = System.currentTimeMillis();

        // Recalculate flee position periodically or if we don't have one
        if (fleeTarget == null || currentTime - lastRecalculateTime > RECALCULATE_INTERVAL_MS) {
            fleeTarget = calculateFleePosition(mobEntity, threat);
            lastRecalculateTime = currentTime;

            if (fleeTarget == null) {
                // Can't find flee position, just run away
                return Status.FAILURE;
            }

            navigation.startMovingTo(fleeTarget.getX(), fleeTarget.getY(), fleeTarget.getZ(), speed);
        }

        // Check if navigation is stuck
        if (navigation.isIdle()) {
            // Try to recalculate
            fleeTarget = calculateFleePosition(mobEntity, threat);
            if (fleeTarget != null) {
                navigation.startMovingTo(fleeTarget.getX(), fleeTarget.getY(), fleeTarget.getZ(), speed);
            } else {
                return Status.FAILURE;
            }
        }

        return Status.RUNNING;
    }

    /**
     * Calculates a safe position to flee to.
     *
     * @param entity The fleeing entity
     * @param threat The threat to flee from
     * @return Safe position or null if none found
     */
    private BlockPos calculateFleePosition(MobEntity entity, Entity threat) {
        Vec3d entityPos = entity.getPos();
        Vec3d threatPos = threat.getPos();

        // Calculate direction away from threat
        Vec3d fleeDirection = entityPos.subtract(threatPos).normalize();

        // Try to find a valid position in the flee direction
        for (int distance = (int) fleeDistance; distance <= fleeRange; distance += 4) {
            Vec3d targetPos = entityPos.add(fleeDirection.multiply(distance));
            BlockPos blockPos = BlockPos.ofFloored(targetPos);

            // Check if position is valid (not in wall, has ground, etc.)
            if (isValidFleePosition(entity, blockPos)) {
                return blockPos;
            }

            // Try slight variations
            for (int angle = -45; angle <= 45; angle += 15) {
                double radians = Math.toRadians(angle);
                double cos = Math.cos(radians);
                double sin = Math.sin(radians);
                
                Vec3d rotatedDir = new Vec3d(
                    fleeDirection.x * cos - fleeDirection.z * sin,
                    fleeDirection.y,
                    fleeDirection.x * sin + fleeDirection.z * cos
                );

                Vec3d variantPos = entityPos.add(rotatedDir.multiply(distance));
                BlockPos variantBlockPos = BlockPos.ofFloored(variantPos);

                if (isValidFleePosition(entity, variantBlockPos)) {
                    return variantBlockPos;
                }
            }
        }

        return null;
    }

    /**
     * Checks if a position is valid for fleeing.
     *
     * @param entity The fleeing entity
     * @param pos Position to check
     * @return True if position is valid
     */
    private boolean isValidFleePosition(MobEntity entity, BlockPos pos) {
        // Basic validation - check if position is pathable
        return entity.getWorld().getBlockState(pos).isAir() && 
               !entity.getWorld().getBlockState(pos.down()).isAir();
    }

    @Override
    public void onStart(BehaviorContext context) {
        fleeTarget = null;
        lastRecalculateTime = 0;
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (context.getEntity() instanceof MobEntity mobEntity) {
            mobEntity.getNavigation().stop();
        }
        fleeTarget = null;
    }

    /**
     * Gets the flee distance.
     *
     * @return Flee distance
     */
    public double getFleeDistance() {
        return fleeDistance;
    }

    /**
     * Gets the safe distance.
     *
     * @return Safe distance
     */
    public double getSafeDistance() {
        return safeDistance;
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
