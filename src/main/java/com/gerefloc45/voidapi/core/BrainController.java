package com.gerefloc45.voidapi.core;

import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.BehaviorTree;
import com.gerefloc45.voidapi.api.Blackboard;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central controller for managing AI brains attached to entities.
 * Handles registration, updates, and cleanup of behavior trees.
 * 
 * @author VoidAPI Framework
 * @version 1.0.0
 */
public class BrainController {
    private static final BrainController INSTANCE = new BrainController();
    
    private final Map<UUID, BrainInstance> brains;

    private BrainController() {
        this.brains = new HashMap<>();
    }

    /**
     * Gets the singleton instance of the brain controller.
     *
     * @return The brain controller instance
     */
    public static BrainController getInstance() {
        return INSTANCE;
    }

    /**
     * Attaches a behavior tree to an entity.
     *
     * @param entity The entity to attach the brain to
     * @param tree The behavior tree to attach
     */
    public void attachBrain(LivingEntity entity, BehaviorTree tree) {
        UUID uuid = entity.getUuid();
        BrainInstance instance = new BrainInstance(tree, new Blackboard());
        brains.put(uuid, instance);
    }

    /**
     * Detaches the behavior tree from an entity.
     *
     * @param entity The entity to detach from
     */
    public void detachBrain(LivingEntity entity) {
        brains.remove(entity.getUuid());
    }

    /**
     * Checks if an entity has a brain attached.
     *
     * @param entity The entity to check
     * @return True if the entity has a brain
     */
    public boolean hasBrain(LivingEntity entity) {
        return brains.containsKey(entity.getUuid());
    }

    /**
     * Gets the blackboard for an entity.
     *
     * @param entity The entity
     * @return The blackboard, or null if no brain is attached
     */
    public Blackboard getBlackboard(LivingEntity entity) {
        BrainInstance instance = brains.get(entity.getUuid());
        return instance != null ? instance.blackboard : null;
    }

    /**
     * Ticks the brain for an entity.
     *
     * @param entity The entity to tick
     * @param deltaTime Time since last tick in seconds
     */
    public void tick(LivingEntity entity, float deltaTime) {
        BrainInstance instance = brains.get(entity.getUuid());
        if (instance == null || !(entity.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        BehaviorContext context = new BehaviorContext(entity, serverWorld, instance.blackboard, deltaTime);
        instance.tree.tick(context);
    }

    /**
     * Clears all brains. Used for cleanup.
     */
    public void clearAll() {
        brains.clear();
    }

    /**
     * Gets the number of active brains.
     *
     * @return The number of brains
     */
    public int getBrainCount() {
        return brains.size();
    }

    /**
     * Internal class to hold brain instance data.
     */
    private static class BrainInstance {
        final BehaviorTree tree;
        final Blackboard blackboard;

        BrainInstance(BehaviorTree tree, Blackboard blackboard) {
            this.tree = tree;
            this.blackboard = blackboard;
        }
    }
}
