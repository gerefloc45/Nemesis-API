# Frequently Asked Questions

## General

### What is VoidAPI?

VoidAPI is a pure library mod for Minecraft Fabric 1.21.1 that provides advanced AI capabilities through Behavior Trees, Sensors, and Utility AI systems. It contains no gameplay content - just the API.

### Is it compatible with my mod?

Yes! VoidAPI is designed to work alongside any mod. Just add it as a dependency and use it for your entities' AI.

### Can I use it for vanilla mobs?

Yes! You can attach VoidAPI brains to any `LivingEntity`, including vanilla mobs.

### Does it work on servers?

Yes, VoidAPI is server-side only. No client installation needed.

## Installation & Setup

### How do I add VoidAPI to my mod?

See the [Installation](Installation) guide. TL;DR:
```gradle
modImplementation "com.gerefloc45:voidapi:0.3.0"
include "com.gerefloc45:voidapi:0.3.0"
```

### Do I need to bundle it with my mod?

Use `include` in Gradle to bundle it, or declare it as a required dependency and let users install it separately.

### Can I use it in development without publishing?

Yes! Use a local JAR or composite build. See [Installation](Installation) for details.

## Usage

### How do I create a simple AI?

See the [Quick Start](Quick-Start) guide. Basic example:
```java
BehaviorTree brain = new BehaviorTree(
    new SelectorNode()
        .addChild(attackBehavior)
        .addChild(patrolBehavior)
);
BrainController.getInstance().attachBrain(entity, brain);
BrainTicker.registerEntity(entity);
```

### How do I remove AI from an entity?

```java
BrainController.getInstance().removeBrain(entity);
BrainTicker.unregisterEntity(entity);
```

### Can I have multiple behavior trees per entity?

Yes, but only one active at a time. Switch between them:
```java
BrainController.getInstance().attachBrain(entity, combatTree);
// Later...
BrainController.getInstance().attachBrain(entity, peacefulTree);
```

### How do I share data between behaviors?

Use the Blackboard:
```java
// Set
ctx.getBlackboard().set("target_position", pos);

// Get
Vec3d pos = ctx.getBlackboard().<Vec3d>get("target_position").orElse(null);
```

## Performance

### How many entities can use VoidAPI?

Tested with 100+ entities. Performance depends on tree complexity and tick frequency.

### Is it laggy?

No, if used correctly:
- Keep trees shallow (< 50 nodes)
- Use appropriate sensor update frequencies
- Avoid expensive calculations in scorers
- Cache values in Blackboard

### How often do trees tick?

Every game tick (20 TPS) by default. You can control sensor update frequencies and utility re-evaluation intervals.

### Can I reduce CPU usage?

Yes:
- Increase sensor update frequencies
- Increase utility re-evaluation intervals
- Use simpler trees
- Cache calculations

## Behavior Trees

### What's the difference between Selector and Sequence?

- **Selector**: Tries children until one **succeeds** (OR logic)
- **Sequence**: Runs children until one **fails** (AND logic)

### When should I use ParallelNode?

When multiple behaviors need to run simultaneously, like:
- Track enemy + dodge + attack
- Move + play animation + emit particles

### How do I make a behavior repeat?

Use `RepeatNode`:
```java
new RepeatNode(behavior, 3) // Repeat 3 times
new RepeatNode(behavior, -1) // Infinite loop
```

### Can I create custom nodes?

Yes! Extend `BehaviorNode` or `ActionNode`:
```java
public class MyCustomNode extends ActionNode {
    public MyCustomNode() {
        super(ctx -> {
            // Your logic here
            return Behavior.Status.SUCCESS;
        });
    }
}
```

## Sensors

### How do sensors work?

Sensors detect the environment and store results in the Blackboard:
```java
EntitySensor<PlayerEntity> sensor = new EntitySensor<>(
    PlayerEntity.class, 16.0, "nearby_players"
);
sensor.update(ctx);
List<PlayerEntity> players = sensor.getDetectedEntities(ctx);
```

### How often do sensors update?

Configurable via `updateFrequency`:
```java
new EntitySensor<>(PlayerEntity.class, 16.0, "key", false, filter, 20);
// Updates every 20 ticks (1 second)
```

### Can I create custom sensors?

Yes! Implement the `Sensor` interface:
```java
public class MySensor implements Sensor {
    @Override
    public void update(BehaviorContext context) {
        // Detection logic
    }
    
    @Override
    public double getRange() {
        return 16.0;
    }
}
```

## Utility AI

### When should I use Utility AI?

When you need:
- Context-dependent decisions
- Smooth transitions between behaviors
- Balancing multiple factors
- Emergent behavior

### What's a good re-evaluation interval?

- **Fast combat**: 5-10 ticks
- **Normal gameplay**: 20 ticks (1 second)
- **Strategic decisions**: 40-60 ticks

### How do I tune response curves?

Start with `linear()`, then adjust:
- High values important? → `quadratic()`
- Low values important? → `inverseQuadratic()`
- Clear threshold? → `logistic()`

### Can I combine multiple considerations?

Yes:
```java
Scorer combined = health.multiply(distance).multiply(ammo);
```

## Debugging

### How do I debug my AI?

1. Add logging:
```java
LOGGER.info("Behavior status: {}", status);
```

2. Use Blackboard for state:
```java
ctx.getBlackboard().set("debug_state", "attacking");
```

3. Print tree structure (see [Behavior Trees](Behavior-Trees))

### My AI isn't working, what do I check?

1. Is the brain attached? `BrainController.getInstance().attachBrain()`
2. Is it registered for ticking? `BrainTicker.registerEntity()`
3. Are behaviors returning correct status?
4. Check logs for errors

### How do I visualize the behavior tree?

Currently manual. Future versions will include debug tools. For now:
```java
public void printTree(BehaviorNode node, int depth) {
    System.out.println("  ".repeat(depth) + node.getClass().getSimpleName());
    // ... print children
}
```

## Common Issues

### "Cannot resolve symbol 'voidapi'"

- Refresh Gradle: `./gradlew build --refresh-dependencies`
- Check dependency is correct in `build.gradle`
- Verify JAR path (for local builds)

### ClassNotFoundException at runtime

- Make sure you used `include` in dependencies
- Check `fabric.mod.json` has the dependency
- Verify JAR is bundled: check `META-INF/jars/` in your mod JAR

### AI stops working after entity respawns

Re-attach the brain on spawn:
```java
ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
    if (entity instanceof MyEntity) {
        attachAI(entity);
    }
});
```

### Memory leak with many entities

Make sure to cleanup:
```java
ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
    BrainController.getInstance().removeBrain(entity);
    BrainTicker.unregisterEntity(entity);
});
```

## Advanced

### Can I use GOAP (Goal-Oriented Action Planning)?

Not yet! GOAP is planned for v0.3.0. See [Roadmap](../AI-ROADMAP.md).

### Can I integrate with other AI mods?

Yes, but they may conflict if both try to control the same entity. Use one system per entity.

### Is there a visual editor?

Not currently. Planned for future versions.

### Can I save/load behavior trees?

Not built-in, but you can serialize them yourself using JSON or NBT.

## Contributing

### How can I contribute?

See [Contributing](Contributing) guide. We welcome:
- Bug reports
- Feature requests
- Code contributions
- Documentation improvements

### Where do I report bugs?

Open an issue on GitHub with:
- Minecraft version
- VoidAPI version
- Steps to reproduce
- Error logs

### Can I suggest features?

Yes! Open a feature request on GitHub or discuss in the community.

## Still Have Questions?

- Check [Troubleshooting](Troubleshooting)
- Read the [Examples](Basic-Examples)
- Open an issue on GitHub
- Join the community Discord (if available)

---

**Can't find your answer?** Open an issue on GitHub and we'll add it to this FAQ!
