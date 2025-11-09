package com.gerefloc45.voidapi.api.pathfinding;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Pathfinding behavior - navigates entity to a target position.
 * Uses Minecraft's built-in pathfinding system.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class PathfindingBehavior implements Behavior {
    private final String targetPosKey;
    private final double speed;
    private final double acceptableDistance;
    private final boolean recalculateOnMove;
    private BlockPos lastTargetPos;
    private long lastRecalculateTime;
    private static final long RECALCULATE_INTERVAL_MS = 1000; // Recalculate every second

    /**
     * Creates a pathfinding behavior with default settings.
     *
     * @param targetPosKey Blackboard key for target position (BlockPos or Vec3d)
     */
    public PathfindingBehavior(String targetPosKey) {
        this(targetPosKey, 1.0, 2.0, true);
    }

    /**
     * Creates a pathfinding behavior with custom speed.
     *
     * @param targetPosKey Blackboard key for target position
     * @param speed Movement speed multiplier
     */
    public PathfindingBehavior(String targetPosKey, double speed) {
        this(targetPosKey, speed, 2.0, true);
    }

    /**
     * Creates a pathfinding behavior with full configuration.
     *
     * @param targetPosKey Blackboard key for target position
     * @param speed Movement speed multiplier
     * @param acceptableDistance Distance at which target is considered reached
     * @param recalculateOnMove Whether to recalculate path if target moves
     */
    public PathfindingBehavior(String targetPosKey, double speed, double acceptableDistance, boolean recalculateOnMove) {
        this.targetPosKey = targetPosKey;
        this.speed = speed;
        this.acceptableDistance = acceptableDistance;
        this.recalculateOnMove = recalculateOnMove;
        this.lastTargetPos = null;
        this.lastRecalculateTime = 0;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!(context.getEntity() instanceof MobEntity mobEntity)) {
            return Status.FAILURE;
        }

        // Get target position from blackboard
        BlockPos targetPos = getTargetPosition(context);
        if (targetPos == null) {
            return Status.FAILURE;
        }

        EntityNavigation navigation = mobEntity.getNavigation();

        // Check if we've reached the target
        double distanceToTarget = mobEntity.getPos().distanceTo(Vec3d.ofCenter(targetPos));
        if (distanceToTarget <= acceptableDistance) {
            navigation.stop();
            return Status.SUCCESS;
        }

        // Check if we need to recalculate path
        boolean shouldRecalculate = false;
        long currentTime = System.currentTimeMillis();

        if (lastTargetPos == null || !lastTargetPos.equals(targetPos)) {
            // Target moved
            shouldRecalculate = recalculateOnMove;
            lastTargetPos = targetPos;
        } else if (currentTime - lastRecalculateTime > RECALCULATE_INTERVAL_MS) {
            // Periodic recalculation
            shouldRecalculate = true;
        }

        if (shouldRecalculate || navigation.isIdle()) {
            boolean pathFound = navigation.startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
            lastRecalculateTime = currentTime;

            if (!pathFound) {
                return Status.FAILURE;
            }
        }

        // Check if navigation is stuck
        if (navigation.isIdle()) {
            return Status.FAILURE;
        }

        return Status.RUNNING;
    }

    /**
     * Gets the target position from the blackboard.
     *
     * @param context Behavior context
     * @return Target position or null if not found
     */
    private BlockPos getTargetPosition(BehaviorContext context) {
        // Try BlockPos first
        var blockPosOpt = context.getBlackboard().<BlockPos>get(targetPosKey);
        if (blockPosOpt.isPresent()) {
            return blockPosOpt.get();
        }

        // Try Vec3d
        var vec3dOpt = context.getBlackboard().<Vec3d>get(targetPosKey);
        if (vec3dOpt.isPresent()) {
            Vec3d vec = vec3dOpt.get();
            return BlockPos.ofFloored(vec.x, vec.y, vec.z);
        }

        return null;
    }

    @Override
    public void onStart(BehaviorContext context) {
        lastTargetPos = null;
        lastRecalculateTime = 0;
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (context.getEntity() instanceof MobEntity mobEntity) {
            mobEntity.getNavigation().stop();
        }
        lastTargetPos = null;
    }

    /**
     * Gets the acceptable distance to target.
     *
     * @return Acceptable distance
     */
    public double getAcceptableDistance() {
        return acceptableDistance;
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
