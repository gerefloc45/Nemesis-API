package com.myname.nemesisapi.api.nodes;

import com.myname.nemesisapi.api.Behavior;
import com.myname.nemesisapi.api.BehaviorContext;

/**
 * Cooldown node - adds a cooldown period between executions of child behavior.
 * Returns FAILURE if still on cooldown.
 * 
 * @author Nemesis-API Framework
 * @version 1.1.0
 */
public class CooldownNode implements Behavior {
    private final Behavior child;
    private final float cooldownSeconds;
    private final String cooldownKey;

    /**
     * Creates a new cooldown node.
     *
     * @param child The child behavior
     * @param cooldownSeconds Cooldown duration in seconds
     */
    public CooldownNode(Behavior child, float cooldownSeconds) {
        this.child = child;
        this.cooldownSeconds = cooldownSeconds;
        this.cooldownKey = "cooldown_" + System.identityHashCode(this);
    }

    /**
     * Creates a new cooldown node with custom blackboard key.
     *
     * @param child The child behavior
     * @param cooldownSeconds Cooldown duration in seconds
     * @param cooldownKey Custom blackboard key for tracking cooldown
     */
    public CooldownNode(Behavior child, float cooldownSeconds, String cooldownKey) {
        this.child = child;
        this.cooldownSeconds = cooldownSeconds;
        this.cooldownKey = cooldownKey;
    }

    @Override
    public Status execute(BehaviorContext context) {
        long currentTime = System.currentTimeMillis();
        Long lastExecutionTime = context.getBlackboard().<Long>get(cooldownKey).orElse(0L);

        float elapsedSeconds = (currentTime - lastExecutionTime) / 1000.0f;

        if (elapsedSeconds < cooldownSeconds) {
            // Still on cooldown
            return Status.FAILURE;
        }

        // Execute child
        Status childStatus = child.execute(context);

        // Update cooldown time only on completion
        if (childStatus != Status.RUNNING) {
            context.getBlackboard().set(cooldownKey, currentTime);
        }

        return childStatus;
    }

    @Override
    public void onStart(BehaviorContext context) {
        child.onStart(context);
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        child.onEnd(context, status);
    }

    /**
     * Resets the cooldown for this node.
     *
     * @param context The behavior context
     */
    public void resetCooldown(BehaviorContext context) {
        context.getBlackboard().remove(cooldownKey);
    }

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @param context The behavior context
     * @return Remaining cooldown time, or 0 if not on cooldown
     */
    public float getRemainingCooldown(BehaviorContext context) {
        long currentTime = System.currentTimeMillis();
        Long lastExecutionTime = context.getBlackboard().<Long>get(cooldownKey).orElse(0L);
        float elapsedSeconds = (currentTime - lastExecutionTime) / 1000.0f;
        return Math.max(0, cooldownSeconds - elapsedSeconds);
    }
}
