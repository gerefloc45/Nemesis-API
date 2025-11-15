# Quick Start Guide

Build your first AI in 5 minutes! This guide will walk you through creating a simple guard AI that patrols and attacks enemies.

## Prerequisites

- VoidAPI installed (see [Installation](Installation))
- Basic Java knowledge
- Familiarity with Minecraft modding

## Step 1: Create a Simple Behavior

Let's create a patrol behavior:

```java
import com.gerefloc45.voidapi.api.Behavior;
import com.gerefloc45.voidapi.api.BehaviorContext;
import com.gerefloc45.voidapi.api.nodes.ActionNode;

public class PatrolBehavior extends ActionNode {
    private int tickCount = 0;
    
    public PatrolBehavior() {
        super(ctx -> {
            // Simple patrol: just wander around
            if (ctx.getEntity().getNavigation().isIdle()) {
                // Find random position and move there
                ctx.getEntity().getNavigation().startMovingAlong(
                    ctx.getEntity().getNavigation().findPathTo(
                        ctx.getEntity().getBlockPos().add(
                            ctx.getEntity().getRandom().nextInt(20) - 10,
                            0,
                            ctx.getEntity().getRandom().nextInt(20) - 10
                        ),
                        1
                    ),
                    1.0
                );
            }
            return Behavior.Status.RUNNING;
        });
    }
}
```

## Step 2: Create an Attack Behavior

```java
public class AttackBehavior extends ActionNode {
    public AttackBehavior() {
        super(ctx -> {
            LivingEntity target = ctx.getEntity().getTarget();
            
            if (target == null || !target.isAlive()) {
                return Behavior.Status.FAILURE;
            }
            
            // Move towards target
            if (ctx.getEntity().distanceTo(target) > 2.0) {
                ctx.getEntity().getNavigation().startMovingTo(target, 1.2);
                return Behavior.Status.RUNNING;
            }
            
            // Attack when close enough
            ctx.getEntity().tryAttack(target);
            return Behavior.Status.SUCCESS;
        });
    }
}
```

## Step 3: Create a Find Enemy Behavior

```java
import com.gerefloc45. switch (import com.gerefloc45.nemesisapi.api.perception.EntitySensor;.Value) { "Nemesis-API" { "VoidAPI" } "Nemesis API" { "VoidAPI" } "nemesisapi" { "voidapi" } default { import com.gerefloc45.nemesisapi.api.perception.EntitySensor;.Value } } .api.perception.EntitySensor;
import net.minecraft.entity.player.PlayerEntity;

public class FindEnemyBehavior extends ActionNode {
    private final EntitySensor<PlayerEntity> sensor;
    
    public FindEnemyBehavior() {
        // Create sensor to detect players within 16 blocks
        this.sensor = new EntitySensor<>(
            PlayerEntity.class,
            16.0,
            "nearby_players"
        );
        
        super(ctx -> {
            // Update sensor
            sensor.update(ctx);
            
            // Get nearest player
            PlayerEntity nearest = sensor.getNearestEntity(ctx);
            
            if (nearest != null) {
                ctx.getEntity().setTarget(nearest);
                return Behavior.Status.SUCCESS;
            }
            
            return Behavior.Status.FAILURE;
        });
    }
}
```

## Step 4: Build the Behavior Tree

Now combine everything into a tree:

```java
import com.gerefloc45.voidapi.api.BehaviorTree;
import com.gerefloc45.voidapi.api.nodes.*;

public class GuardAI {
    public static BehaviorTree create() {
        return new BehaviorTree(
            new SelectorNode()
                // Try to find and attack enemies first
                .addChild(new SequenceNode()
                    .addChild(new FindEnemyBehavior())
                    .addChild(new CooldownNode(new AttackBehavior(), 1.0f))
                )
                // If no enemies, patrol
                .addChild(new PatrolBehavior())
        );
    }
}
```

### How it Works

1. **SelectorNode** tries children until one succeeds
2. **SequenceNode** runs children in order, fails if any fails
3. **CooldownNode** prevents attack spam (1 second cooldown)
4. If no enemies found, falls back to patrol

## Step 5: Attach to Entity

In your mod initialization or entity spawn event:

```java
import com.gerefloc45.voidapi.core.BrainController;
import com.gerefloc45.voidapi.core.BrainTicker;
import net.minecraft.entity.mob.HostileEntity;

public void attachGuardAI(HostileEntity entity) {
    // Create the AI
    BehaviorTree guardBrain = GuardAI.create();
    
    // Attach to entity
    BrainController.getInstance().attachBrain(entity, guardBrain);
    
    // Register for automatic ticking
    BrainTicker.registerEntity(entity);
}
```

## Step 6: Test It!

1. Build your mod: `./gradlew build`
2. Run Minecraft with your mod
3. Spawn your entity
4. Watch it patrol and attack players!

## Complete Example

Here's the full code in one place:

```java
package com.yourmod.ai;

import com.gerefloc45.voidapi.api.*;
import com.gerefloc45.voidapi.api.nodes.*;
import com.gerefloc45.voidapi.api.perception.EntitySensor;
import com.gerefloc45.voidapi.core.BrainController;
import com.gerefloc45.voidapi.core.BrainTicker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

public class SimpleGuardAI {
    
    public static void apply(HostileEntity entity) {
        BehaviorTree brain = new BehaviorTree(
            new SelectorNode()
                .addChild(new SequenceNode()
                    .addChild(createFindEnemyBehavior())
                    .addChild(new CooldownNode(createAttackBehavior(), 1.0f))
                )
                .addChild(createPatrolBehavior())
        );
        
        BrainController.getInstance().attachBrain(entity, brain);
        BrainTicker.registerEntity(entity);
    }
    
    private static Behavior createFindEnemyBehavior() {
        EntitySensor<PlayerEntity> sensor = new EntitySensor<>(
            PlayerEntity.class, 16.0, "nearby_players"
        );
        
        return new ActionNode(ctx -> {
            sensor.update(ctx);
            PlayerEntity nearest = sensor.getNearestEntity(ctx);
            
            if (nearest != null) {
                ctx.getEntity().setTarget(nearest);
                return Behavior.Status.SUCCESS;
            }
            return Behavior.Status.FAILURE;
        });
    }
    
    private static Behavior createAttackBehavior() {
        return new ActionNode(ctx -> {
            LivingEntity target = ctx.getEntity().getTarget();
            if (target == null || !target.isAlive()) {
                return Behavior.Status.FAILURE;
            }
            
            if (ctx.getEntity().distanceTo(target) > 2.0) {
                ctx.getEntity().getNavigation().startMovingTo(target, 1.2);
                return Behavior.Status.RUNNING;
            }
            
            ctx.getEntity().tryAttack(target);
            return Behavior.Status.SUCCESS;
        });
    }
    
    private static Behavior createPatrolBehavior() {
        return new ActionNode(ctx -> {
            if (ctx.getEntity().getNavigation().isIdle()) {
                ctx.getEntity().getNavigation().startMovingAlong(
                    ctx.getEntity().getNavigation().findPathTo(
                        ctx.getEntity().getBlockPos().add(
                            ctx.getEntity().getRandom().nextInt(20) - 10,
                            0,
                            ctx.getEntity().getRandom().nextInt(20) - 10
                        ),
                        1
                    ),
                    1.0
                );
            }
            return Behavior.Status.RUNNING;
        });
    }
}
```

## What's Next?

Now that you have a basic AI working, explore:

- **[Finite State Machines](FSM-Guide)** - State-based AI architecture
- **[Advanced Nodes](Advanced-Nodes)** - ParallelNode, RepeatNode, ConditionalNode
- **[Utility AI](Utility-AI-Overview)** - Score-based decision making
- **[More Sensors](Sensors-Overview)** - BlockSensor, SoundSensor
- **[Combat AI Tutorial](Combat-AI-Tutorial)** - Build smarter fighters

## Tips

💡 **Use Blackboard** for sharing data between behaviors:
```java
ctx.getBlackboard().set("last_target_pos", target.getPos());
```

💡 **Add Logging** for debugging:
```java
LOGGER.info("Guard found enemy: {}", target.getName().getString());
```

💡 **Cleanup** when entity is removed:
```java
BrainController.getInstance().removeBrain(entity);
BrainTicker.unregisterEntity(entity);
```

---

**Questions?** Check the **[FAQ](FAQ)** or **[Examples](Basic-Examples)** page!
