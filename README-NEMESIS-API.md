# Nemesis-API Framework

A standalone, modular AI framework for Minecraft Fabric 1.21.1. This mod provides a complete behavior tree system, blackboard memory, and async utilities for building advanced AI logic in your custom mods.

**Nemesis-API contains no gameplay content** - it's purely a library/API for other mods to use.

---

## 🎯 Features

### Core System (v0.1.0)
- **Behavior Tree System**: Selector, Sequence, and Action nodes for complex AI logic
- **Blackboard Memory**: Per-entity shared memory system for storing AI state
- **Brain Controller**: Centralized management of AI brains attached to entities
- **Async Utilities**: Non-blocking AI operations using CompletableFuture
- **Auto-Ticking**: Automatic brain updates integrated with Fabric server tick events
- **Fully Documented**: Complete Javadoc for all public APIs
- **Server-Safe**: Designed for server-side AI logic

### Advanced Nodes (v0.1.0) 🆕
- **ParallelNode**: Execute multiple behaviors simultaneously
- **InverterNode**: Invert SUCCESS/FAILURE results
- **RepeatNode**: Repeat behaviors with configurable limits
- **ConditionalNode**: Execute only when conditions are met
- **CooldownNode**: Add cooldowns between behavior executions

### Perception System (v0.1.0) 🆕
- **Sensor API**: Detect entities, blocks, and events
- **EntitySensor**: Find and track nearby entities with filters
- **PerceptionMemory**: Remember entities even after they leave range
- **SensorManager**: Manage multiple sensors with different update frequencies

---

## 📦 Installation

### As a Dependency in Your Mod

Add Nemesis-API to your `build.gradle`:

```gradle
repositories {
    mavenLocal() // If published locally
    // Or add your maven repository
}

dependencies {
    modImplementation "com.myname:nemesis-api:0.1.0"
    include "com.myname:nemesis-api:0.1.0" // Bundle it with your mod
}
```

Add to your `fabric.mod.json`:

```json
{
  "depends": {
    "nemesisapi": ">=0.1.0"
  }
}
```

---

## 🚀 Quick Start

### 1. Create a Simple Behavior

```java
import com.myname.nemesisapi.api.Behavior;
import com.myname.nemesisapi.api.BehaviorContext;

public class WanderBehavior implements Behavior {
    @Override
    public Status execute(BehaviorContext context) {
        LivingEntity entity = context.getEntity();
        
        // Your AI logic here
        if (entity.getRandom().nextFloat() < 0.1f) {
            // Move to random position
            return Status.SUCCESS;
        }
        
        return Status.RUNNING;
    }
}
```

### 2. Build a Behavior Tree

```java
import com.myname.nemesisapi.api.BehaviorTree;
import com.myname.nemesisapi.api.nodes.*;

// Create a behavior tree with selector (OR) logic
BehaviorTree tree = new BehaviorTree(
    new SelectorNode()
        .addChild(new ActionNode(new AttackBehavior()))
        .addChild(new ActionNode(new FleeBehavior()))
        .addChild(new ActionNode(new WanderBehavior()))
);
```

### 3. Attach to an Entity

```java
import com.myname.nemesisapi.core.BrainController;
import com.myname.nemesisapi.core.BrainTicker;

// In your entity initialization or spawn event
LivingEntity entity = ...; // Your entity

// Attach the brain
BrainController.getInstance().attachBrain(entity, tree);

// Register for automatic ticking
BrainTicker.registerEntity(entity);
```

### 4. Use the Blackboard

```java
public class ChaseBehavior implements Behavior {
    @Override
    public Status execute(BehaviorContext context) {
        Blackboard blackboard = context.getBlackboard();
        
        // Store data
        blackboard.set("target", targetEntity);
        blackboard.set("chaseStartTime", System.currentTimeMillis());
        
        // Retrieve data
        Optional<LivingEntity> target = blackboard.get("target");
        long startTime = blackboard.getOrDefault("chaseStartTime", 0L);
        
        return Status.RUNNING;
    }
}
```

---

## 📚 Core Concepts

### Behavior Tree Nodes

#### SelectorNode (OR Logic)
Executes children until one **succeeds**. Returns `SUCCESS` if any child succeeds, `FAILURE` if all fail.

```java
new SelectorNode()
    .addChild(behavior1) // Try this first
    .addChild(behavior2) // If first fails, try this
    .addChild(behavior3) // Fallback option
```

#### SequenceNode (AND Logic)
Executes children until one **fails**. Returns `SUCCESS` if all succeed, `FAILURE` if any fails.

```java
new SequenceNode()
    .addChild(checkCondition)  // Must succeed
    .addChild(performAction)   // Then do this
    .addChild(cleanup)         // Then cleanup
```

#### ActionNode (Leaf Node)
Wraps a single behavior for execution.

```java
new ActionNode(context -> {
    // Your logic here
    return Behavior.Status.SUCCESS;
})
```

### Behavior Status

- **SUCCESS**: Behavior completed successfully
- **FAILURE**: Behavior failed to complete
- **RUNNING**: Behavior is still executing (will continue next tick)

### Blackboard Memory

Thread-safe key-value storage per entity:

```java
blackboard.set("key", value);
Optional<Type> value = blackboard.get("key");
Type value = blackboard.getOrDefault("key", defaultValue);
boolean exists = blackboard.has("key");
blackboard.remove("key");
blackboard.clear();
```

---

## 🔧 Advanced Usage

### Async Behaviors

```java
import com.myname.nemesisapi.util.AsyncHelper;

public class PathfindingBehavior implements Behavior {
    private CompletableFuture<Path> pathFuture;
    
    @Override
    public Status execute(BehaviorContext context) {
        if (pathFuture == null) {
            // Start async pathfinding
            pathFuture = AsyncHelper.runAsync(() -> {
                return calculateExpensivePath();
            });
            return Status.RUNNING;
        }
        
        if (pathFuture.isDone()) {
            Path path = pathFuture.join();
            // Use the path
            pathFuture = null;
            return Status.SUCCESS;
        }
        
        return Status.RUNNING;
    }
}
```

### Entity Utilities

```java
import com.myname.nemesisapi.util.EntityUtil;

// Find nearest player
Optional<PlayerEntity> player = EntityUtil.findNearestPlayer(entity, 16.0);

// Find entities in range
List<ZombieEntity> zombies = EntityUtil.findEntitiesInRange(
    entity, 10.0, ZombieEntity.class
);

// Check line of sight
boolean canSee = EntityUtil.canSee(entity, target);

// Distance checks
double distance = EntityUtil.distanceBetween(entity1, entity2);
boolean inRange = EntityUtil.isInRange(entity, target, 5.0);
```

### Manual Brain Ticking

If you don't want automatic ticking:

```java
// Don't register with BrainTicker
// Instead, manually tick in your entity's tick method:

@Override
public void tick() {
    super.tick();
    
    if (!world.isClient) {
        BrainController.getInstance().tick(this, 0.05f);
    }
}
```

### Detaching Brains

```java
// When entity is removed or no longer needs AI
BrainController.getInstance().detachBrain(entity);
BrainTicker.unregisterEntity(entity);
```

---

## 🏗️ Project Structure

```
com.myname.nemesisapi
├── api/                    # Public API
│   ├── Behavior.java       # Base behavior interface
│   ├── BehaviorTree.java   # Behavior tree wrapper
│   ├── BehaviorNode.java   # Abstract composite node
│   ├── BehaviorContext.java # Execution context
│   ├── Blackboard.java     # Memory system
│   └── nodes/              # Node implementations
│       ├── SelectorNode.java
│       ├── SequenceNode.java
│       └── ActionNode.java
├── core/                   # Core systems
│   ├── BrainController.java # Brain management
│   └── BrainTicker.java    # Auto-tick system
└── util/                   # Utilities
    ├── AsyncHelper.java    # Async operations
    └── EntityUtil.java     # Entity helpers
```

---

## 🔨 Building from Source

```bash
# Clone the repository
git clone https://github.com/Gerefloc45/Nemesis-API.git
cd Nemesis-API

# Build the mod
./gradlew build

# Publish to local Maven
./gradlew publishToMavenLocal
```

The compiled JAR will be in `build/libs/nemesis-api-0.1.0.jar`.

---

## 📖 Example: Complete AI System

```java
public class GuardAI {
    public static BehaviorTree createGuardBrain() {
        return new BehaviorTree(
            new SelectorNode()
                // Priority 1: Attack nearby enemies
                .addChild(new SequenceNode()
                    .addChild(ActionNode.of(ctx -> {
                        Optional<PlayerEntity> enemy = EntityUtil.findNearestPlayer(
                            ctx.getEntity(), 8.0
                        );
                        if (enemy.isPresent()) {
                            ctx.getBlackboard().set("target", enemy.get());
                            return Behavior.Status.SUCCESS;
                        }
                        return Behavior.Status.FAILURE;
                    }))
                    .addChild(new ActionNode(new AttackTargetBehavior()))
                )
                
                // Priority 2: Patrol area
                .addChild(new SequenceNode()
                    .addChild(ActionNode.of(ctx -> {
                        if (!ctx.getBlackboard().has("patrolPoint")) {
                            // Generate patrol point
                            return Behavior.Status.SUCCESS;
                        }
                        return Behavior.Status.FAILURE;
                    }))
                    .addChild(new ActionNode(new MoveToPointBehavior()))
                )
                
                // Priority 3: Idle
                .addChild(new ActionNode(new IdleBehavior()))
        );
    }
    
    public static void attachToEntity(LivingEntity entity) {
        BehaviorTree brain = createGuardBrain();
        BrainController.getInstance().attachBrain(entity, brain);
        BrainTicker.registerEntity(entity);
    }
}
```

---

## 🆕 New in v0.1.0

### Advanced Behavior Nodes

```java
// Parallel execution
new ParallelNode(ParallelNode.Policy.REQUIRE_ALL)
    .addChild(moveBehavior)
    .addChild(soundBehavior);

// Conditional execution
new ConditionalNode(
    ctx -> ctx.getEntity().getHealth() < 10.0f,
    fleeBehavior
);

// Cooldown
new CooldownNode(attackBehavior, 2.0f); // 2 second cooldown

// Repeat
new RepeatNode(patrolBehavior, 5); // Repeat 5 times

// Inverter
new InverterNode(isEnemyNearby); // Inverts result
```

### Perception System

```java
// Create sensor manager
SensorManager sensors = new SensorManager();

// Add entity sensor
EntitySensor<PlayerEntity> enemySensor = new EntitySensor<>(
    PlayerEntity.class,
    16.0,  // range
    "enemies",  // blackboard key
    true,  // require line of sight
    player -> !player.isCreative(),  // filter
    10  // update every 10 ticks
);
sensors.addSensor(enemySensor);

// Use perception memory
PerceptionMemory memory = new PerceptionMemory(15.0f);
memory.remember(enemy, 0.8f); // Remember with threat level

// Update in behavior
sensors.update(context);
memory.update();
```

**📖 For complete examples, see [EXAMPLES.md](EXAMPLES.md)**  
**🗺️ For future features, see [AI-ROADMAP.md](AI-ROADMAP.md)**

---

## 🤝 Contributing

This is a library mod designed for personal use, but contributions are welcome! Please ensure:

- All public APIs have Javadoc
- Code follows existing style conventions
- No gameplay content is added (keep it pure API)

---

## 📄 License

MIT License - See LICENSE file for details.

---

## 🔗 Links

- **Documentation**: [Javadoc](https://gerefloc45.github.io/Nemesis-API/javadoc)
- **Issues**: [GitHub Issues](https://github.com/Gerefloc45/Nemesis-API/issues)
- **Discord**: [Your Discord Server](https://discord.gg/yourserver)

---

## 🎓 Credits

Built for Minecraft 1.21.1 with Fabric Loader 0.15+ and Fabric API.

**Author**: Your Name  
**Version**: 0.1.0-beta  
**Minecraft**: 1.21.1  
**Java**: 17+

---

## 📋 Changelog

### v0.1.0-beta (2025-11-08)
- ✨ Added 5 advanced behavior tree nodes (Parallel, Inverter, Repeat, Conditional, Cooldown)
- ✨ Implemented perception system with sensors
- ✨ Added EntitySensor for entity detection
- ✨ Implemented PerceptionMemory for persistent entity tracking
- ✨ Added SensorManager for managing multiple sensors
- 📚 Added EXAMPLES.md with practical use cases
- 📚 Added AI-ROADMAP.md for future development

