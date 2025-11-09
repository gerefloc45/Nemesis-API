package com.gerefloc45.voidapi.api.pathfinding;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Wander behavior - randomly explores the environment.
 * Can be constrained to a specific area or wander freely.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class WanderBehavior implements Behavior {
    private final double speed;
    private final int wanderRadius;
    private final float waitTimeSeconds;
    private final BlockPos centerPos;
    private final boolean constrainToCenter;
    private final Random random;
    private BlockPos targetPos;
    private long waitStartTime;
    private boolean isWaiting;

    /**
     * Creates a wander behavior with default settings (free roaming).
     */
    public WanderBehavior() {
        this(1.0, 10, 0.0f, null, false);
    }

    /**
     * Creates a wander behavior with custom radius.
     *
     * @param speed Movement speed multiplier
     * @param wanderRadius Maximum distance to wander
     */
    public WanderBehavior(double speed, int wanderRadius) {
        this(speed, wanderRadius, 0.0f, null, false);
    }

    /**
     * Creates a wander behavior with wait time between movements.
     *
     * @param speed Movement speed multiplier
     * @param wanderRadius Maximum distance to wander
     * @param waitTimeSeconds Time to wait after reaching each position
     */
    public WanderBehavior(double speed, int wanderRadius, float waitTimeSeconds) {
        this(speed, wanderRadius, waitTimeSeconds, null, false);
    }

    /**
     * Creates a wander behavior constrained to an area.
     *
     * @param speed Movement speed multiplier
     * @param wanderRadius Maximum distance to wander from center
     * @param centerPos Center position for wandering
     */
    public WanderBehavior(double speed, int wanderRadius, BlockPos centerPos) {
        this(speed, wanderRadius, 0.0f, centerPos, true);
    }

    /**
     * Creates a wander behavior with full configuration.
     *
     * @param speed Movement speed multiplier
     * @param wanderRadius Maximum distance to wander
     * @param waitTimeSeconds Time to wait after reaching each position
     * @param centerPos Center position (null for entity's spawn position)
     * @param constrainToCenter Whether to constrain wandering to center area
     */
    public WanderBehavior(double speed, int wanderRadius, float waitTimeSeconds, 
                         BlockPos centerPos, boolean constrainToCenter) {
        this.speed = speed;
        this.wanderRadius = wanderRadius;
        this.waitTimeSeconds = waitTimeSeconds;
        this.centerPos = centerPos;
        this.constrainToCenter = constrainToCenter;
        this.random = new Random();
        this.targetPos = null;
        this.isWaiting = false;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!(context.getEntity() instanceof MobEntity mobEntity)) {
            return Status.FAILURE;
        }

        // Handle waiting
        if (isWaiting) {
            long currentTime = System.currentTimeMillis();
            float elapsedSeconds = (currentTime - waitStartTime) / 1000.0f;
            
            if (elapsedSeconds >= waitTimeSeconds) {
                isWaiting = false;
                targetPos = null; // Select new target
            } else {
                return Status.RUNNING;
            }
        }

        // Select new wander target if needed
        if (targetPos == null) {
            targetPos = selectWanderTarget(mobEntity);
            if (targetPos == null) {
                return Status.FAILURE;
            }
        }

        // Check if target reached
        Vec3d entityPos = mobEntity.getPos();
        double distanceToTarget = entityPos.distanceTo(Vec3d.ofCenter(targetPos));

        if (distanceToTarget <= 2.0) {
            mobEntity.getNavigation().stop();

            // Start waiting if configured
            if (waitTimeSeconds > 0) {
                isWaiting = true;
                waitStartTime = System.currentTimeMillis();
                return Status.RUNNING;
            }

            // Select new target immediately
            targetPos = null;
            return Status.SUCCESS;
        }

        // Navigate to target
        EntityNavigation navigation = mobEntity.getNavigation();
        if (navigation.isIdle()) {
            boolean pathFound = navigation.startMovingTo(
                targetPos.getX(), 
                targetPos.getY(), 
                targetPos.getZ(), 
                speed
            );

            if (!pathFound) {
                // Can't reach target, select new one
                targetPos = null;
                return Status.RUNNING;
            }
        }

        return Status.RUNNING;
    }

    /**
     * Selects a random wander target position.
     *
     * @param entity The wandering entity
     * @return Target position or null if none found
     */
    private BlockPos selectWanderTarget(MobEntity entity) {
        BlockPos center = centerPos != null ? centerPos : entity.getBlockPos();

        // Try multiple times to find a valid position
        for (int attempt = 0; attempt < 10; attempt++) {
            int offsetX = random.nextInt(wanderRadius * 2 + 1) - wanderRadius;
            int offsetZ = random.nextInt(wanderRadius * 2 + 1) - wanderRadius;
            int offsetY = random.nextInt(7) - 3; // Allow some vertical variation

            BlockPos candidatePos = center.add(offsetX, offsetY, offsetZ);

            // Validate position
            if (isValidWanderPosition(entity, candidatePos)) {
                return candidatePos;
            }
        }

        // Fallback: just move in a random direction
        int offsetX = random.nextInt(wanderRadius) - wanderRadius / 2;
        int offsetZ = random.nextInt(wanderRadius) - wanderRadius / 2;
        return entity.getBlockPos().add(offsetX, 0, offsetZ);
    }

    /**
     * Checks if a position is valid for wandering.
     *
     * @param entity The wandering entity
     * @param pos Position to check
     * @return True if position is valid
     */
    private boolean isValidWanderPosition(MobEntity entity, BlockPos pos) {
        // Check if position is within wander radius (if constrained)
        if (constrainToCenter && centerPos != null) {
            double distanceFromCenter = Math.sqrt(centerPos.getSquaredDistance(pos));
            if (distanceFromCenter > wanderRadius) {
                return false;
            }
        }

        // Basic validation - check if position is walkable
        return entity.getWorld().getBlockState(pos).isAir() && 
               !entity.getWorld().getBlockState(pos.down()).isAir();
    }

    @Override
    public void onStart(BehaviorContext context) {
        targetPos = null;
        isWaiting = false;

        // Set center to entity's current position if not specified
        if (centerPos == null && constrainToCenter && context.getEntity() instanceof MobEntity mobEntity) {
            // Store in blackboard for consistency
            context.getBlackboard().set("wander_center", mobEntity.getBlockPos());
        }
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (context.getEntity() instanceof MobEntity mobEntity) {
            mobEntity.getNavigation().stop();
        }
        targetPos = null;
        isWaiting = false;
    }

    /**
     * Gets the wander radius.
     *
     * @return Wander radius
     */
    public int getWanderRadius() {
        return wanderRadius;
    }

    /**
     * Gets the movement speed multiplier.
     *
     * @return Speed multiplier
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Gets the wait time between movements.
     *
     * @return Wait time in seconds
     */
    public float getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    /**
     * Gets the current wander target (if any).
     *
     * @return Target position or null
     */
    public BlockPos getTargetPos() {
        return targetPos;
    }
}
