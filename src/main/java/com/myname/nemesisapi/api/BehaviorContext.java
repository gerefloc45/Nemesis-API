package com.myname.nemesisapi.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Context object passed to behaviors during execution.
 * Contains the entity, world, blackboard, and delta time information.
 * 
 * @author Nemesis-API Framework
 * @version 1.0.0
 */
public class BehaviorContext {
    private final LivingEntity entity;
    private final ServerWorld world;
    private final Blackboard blackboard;
    private final float deltaTime;

    /**
     * Creates a new behavior context.
     *
     * @param entity The entity executing the behavior
     * @param world The server world the entity is in
     * @param blackboard The blackboard memory system
     * @param deltaTime Time since last tick in seconds
     */
    public BehaviorContext(LivingEntity entity, ServerWorld world, Blackboard blackboard, float deltaTime) {
        this.entity = entity;
        this.world = world;
        this.blackboard = blackboard;
        this.deltaTime = deltaTime;
    }

    /**
     * Gets the entity executing the behavior.
     *
     * @return The living entity
     */
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * Gets the server world the entity is in.
     *
     * @return The server world
     */
    public ServerWorld getWorld() {
        return world;
    }

    /**
     * Gets the blackboard memory system for this entity.
     *
     * @return The blackboard instance
     */
    public Blackboard getBlackboard() {
        return blackboard;
    }

    /**
     * Gets the time since last tick in seconds.
     *
     * @return Delta time in seconds
     */
    public float getDeltaTime() {
        return deltaTime;
    }
}
