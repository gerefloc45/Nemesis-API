package com.gerefloc45.voidapi.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles automatic ticking of all registered brains on server tick.
 * Integrates with Fabric's server tick event system.
 * 
 * @author VoidAPI Framework
 * @version 1.0.0
 */
public class BrainTicker {
    private static final float TICK_RATE = 0.05f; // 20 TPS = 0.05 seconds per tick
    private static boolean initialized = false;
    private static final List<LivingEntity> entitiesToTick = new ArrayList<>();

    /**
     * Initializes the brain ticker system.
     * Should be called once during mod initialization.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(BrainTicker::onServerTick);
        initialized = true;
    }

    /**
     * Registers an entity to be ticked automatically.
     *
     * @param entity The entity to register
     */
    public static void registerEntity(LivingEntity entity) {
        if (!entitiesToTick.contains(entity)) {
            entitiesToTick.add(entity);
        }
    }

    /**
     * Unregisters an entity from automatic ticking.
     *
     * @param entity The entity to unregister
     */
    public static void unregisterEntity(LivingEntity entity) {
        entitiesToTick.remove(entity);
    }

    /**
     * Checks if an entity is registered for automatic ticking.
     *
     * @param entity The entity to check
     * @return True if registered
     */
    public static boolean isRegistered(LivingEntity entity) {
        return entitiesToTick.contains(entity);
    }

    /**
     * Called every server tick to update all registered brains.
     *
     * @param server The minecraft server
     */
    private static void onServerTick(MinecraftServer server) {
        BrainController controller = BrainController.getInstance();
        
        // Remove dead or invalid entities
        entitiesToTick.removeIf(entity -> entity.isRemoved() || !entity.isAlive() || !controller.hasBrain(entity));

        // Tick all registered entities
        for (LivingEntity entity : entitiesToTick) {
            try {
                controller.tick(entity, TICK_RATE);
            } catch (Exception e) {
                // Log error but continue ticking other entities
                System.err.println("Error ticking brain for entity " + entity.getUuid() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Clears all registered entities.
     */
    public static void clearAll() {
        entitiesToTick.clear();
    }

    /**
     * Gets the number of entities registered for ticking.
     *
     * @return The number of registered entities
     */
    public static int getRegisteredCount() {
        return entitiesToTick.size();
    }
}
