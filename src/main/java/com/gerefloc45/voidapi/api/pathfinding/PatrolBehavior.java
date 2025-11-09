package com.gerefloc45.voidapi.api.pathfinding;

import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Patrol behavior - moves entity through a list of waypoints.
 * Supports looping and reversing patrol patterns.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class PatrolBehavior implements Behavior {
    private final List<BlockPos> waypoints;
    private final double speed;
    private final double waypointReachedDistance;
    private final boolean loop;
    private final boolean reverse;
    private final float waitTimeSeconds;
    private int currentWaypointIndex;
    private boolean movingForward;
    private long waitStartTime;
    private boolean isWaiting;

    /**
     * Creates a patrol behavior with default settings.
     *
     * @param waypoints List of waypoints to patrol
     */
    public PatrolBehavior(List<BlockPos> waypoints) {
        this(waypoints, 1.0, 2.0, true, false, 0.0f);
    }

    /**
     * Creates a patrol behavior with custom speed and looping.
     *
     * @param waypoints List of waypoints to patrol
     * @param speed Movement speed multiplier
     * @param loop Whether to loop back to start after reaching end
     */
    public PatrolBehavior(List<BlockPos> waypoints, double speed, boolean loop) {
        this(waypoints, speed, 2.0, loop, false, 0.0f);
    }

    /**
     * Creates a patrol behavior with full configuration.
     *
     * @param waypoints List of waypoints to patrol
     * @param speed Movement speed multiplier
     * @param waypointReachedDistance Distance at which waypoint is considered reached
     * @param loop Whether to loop back to start
     * @param reverse Whether to reverse direction at end (ping-pong pattern)
     * @param waitTimeSeconds Time to wait at each waypoint
     */
    public PatrolBehavior(List<BlockPos> waypoints, double speed, double waypointReachedDistance, 
                         boolean loop, boolean reverse, float waitTimeSeconds) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException("Waypoints list cannot be null or empty");
        }
        this.waypoints = new ArrayList<>(waypoints);
        this.speed = speed;
        this.waypointReachedDistance = waypointReachedDistance;
        this.loop = loop;
        this.reverse = reverse;
        this.waitTimeSeconds = waitTimeSeconds;
        this.currentWaypointIndex = 0;
        this.movingForward = true;
        this.isWaiting = false;
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (!(context.getEntity() instanceof MobEntity mobEntity)) {
            return Status.FAILURE;
        }

        // Handle waiting at waypoint
        if (isWaiting) {
            long currentTime = System.currentTimeMillis();
            float elapsedSeconds = (currentTime - waitStartTime) / 1000.0f;
            
            if (elapsedSeconds >= waitTimeSeconds) {
                isWaiting = false;
                advanceToNextWaypoint();
            } else {
                return Status.RUNNING;
            }
        }

        // Get current waypoint
        BlockPos currentWaypoint = waypoints.get(currentWaypointIndex);
        Vec3d entityPos = mobEntity.getPos();
        double distanceToWaypoint = entityPos.distanceTo(Vec3d.ofCenter(currentWaypoint));

        // Check if waypoint reached
        if (distanceToWaypoint <= waypointReachedDistance) {
            mobEntity.getNavigation().stop();

            // Check if patrol complete
            if (isPatrolComplete()) {
                return Status.SUCCESS;
            }

            // Start waiting if configured
            if (waitTimeSeconds > 0) {
                isWaiting = true;
                waitStartTime = System.currentTimeMillis();
                return Status.RUNNING;
            }

            // Move to next waypoint
            advanceToNextWaypoint();
            currentWaypoint = waypoints.get(currentWaypointIndex);
        }

        // Navigate to current waypoint
        EntityNavigation navigation = mobEntity.getNavigation();
        if (navigation.isIdle()) {
            boolean pathFound = navigation.startMovingTo(
                currentWaypoint.getX(), 
                currentWaypoint.getY(), 
                currentWaypoint.getZ(), 
                speed
            );

            if (!pathFound) {
                return Status.FAILURE;
            }
        }

        return Status.RUNNING;
    }

    /**
     * Advances to the next waypoint based on patrol settings.
     */
    private void advanceToNextWaypoint() {
        if (reverse) {
            // Ping-pong pattern
            if (movingForward) {
                currentWaypointIndex++;
                if (currentWaypointIndex >= waypoints.size()) {
                    currentWaypointIndex = waypoints.size() - 2;
                    movingForward = false;
                }
            } else {
                currentWaypointIndex--;
                if (currentWaypointIndex < 0) {
                    currentWaypointIndex = 1;
                    movingForward = true;
                }
            }
        } else {
            // Linear pattern
            currentWaypointIndex++;
            if (currentWaypointIndex >= waypoints.size()) {
                if (loop) {
                    currentWaypointIndex = 0;
                } else {
                    currentWaypointIndex = waypoints.size() - 1;
                }
            }
        }
    }

    /**
     * Checks if the patrol is complete.
     *
     * @return True if patrol finished
     */
    private boolean isPatrolComplete() {
        if (loop || reverse) {
            return false; // Never complete if looping or reversing
        }
        return currentWaypointIndex >= waypoints.size() - 1;
    }

    @Override
    public void onStart(BehaviorContext context) {
        currentWaypointIndex = 0;
        movingForward = true;
        isWaiting = false;
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        if (context.getEntity() instanceof MobEntity mobEntity) {
            mobEntity.getNavigation().stop();
        }
        isWaiting = false;
    }

    /**
     * Gets the current waypoint index.
     *
     * @return Current waypoint index
     */
    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    /**
     * Gets the total number of waypoints.
     *
     * @return Waypoint count
     */
    public int getWaypointCount() {
        return waypoints.size();
    }

    /**
     * Gets a specific waypoint.
     *
     * @param index Waypoint index
     * @return Waypoint position
     */
    public BlockPos getWaypoint(int index) {
        return waypoints.get(index);
    }

    /**
     * Adds a waypoint to the patrol route.
     *
     * @param waypoint Waypoint to add
     */
    public void addWaypoint(BlockPos waypoint) {
        waypoints.add(waypoint);
    }

    /**
     * Removes a waypoint from the patrol route.
     *
     * @param index Waypoint index to remove
     */
    public void removeWaypoint(int index) {
        if (waypoints.size() > 1) {
            waypoints.remove(index);
            if (currentWaypointIndex >= waypoints.size()) {
                currentWaypointIndex = waypoints.size() - 1;
            }
        }
    }
}
