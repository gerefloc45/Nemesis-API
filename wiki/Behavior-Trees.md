# Behavior Trees

Behavior Trees are the foundation of VoidAPI. This page explains what they are and how to use them effectively.

## What is a Behavior Tree?

A **Behavior Tree** is a hierarchical structure that controls AI decision-making. Think of it as a flowchart where:

- **Nodes** represent decisions or actions
- **Execution** flows from root to leaves
- **Status** returns up the tree (SUCCESS, FAILURE, RUNNING)

### Visual Example

```
         SelectorNode (Try until one succeeds)
         /                    \
    SequenceNode          PatrolBehavior
    (Do in order)              |
       /      \            (Wander around)
  FindEnemy  Attack
     |          |
  (Search)  (Fight)
```

## Core Concepts

### 1. Nodes

Every behavior tree is made of **nodes**:

- **Composite Nodes** - Have children, control flow
- **Decorator Nodes** - Modify child behavior
- **Leaf Nodes** - Actual actions

### 2. Execution Status

Every node returns one of three statuses:

| Status | Meaning | Example |
|--------|---------|---------|
| `SUCCESS` | Task completed successfully | Found an enemy |
| `FAILURE` | Task failed | No enemies nearby |
| `RUNNING` | Task still in progress | Moving to target |

### 3. Tick-based Execution

Trees are "ticked" (updated) every game tick:

```java
// Automatic ticking via BrainTicker
BrainTicker.registerEntity(entity);

// Or manual ticking
brain.tick(context);
```

## Node Types

### Composite Nodes

#### SelectorNode (OR)
Tries children in order until one **succeeds**.

```java
new SelectorNode()
    .addChild(attackIfEnemyNearby)  // Try this first
    .addChild(healIfLowHealth)       // Then this
    .addChild(patrol)                // Finally this
```

**Use when**: You want fallback behavior

#### SequenceNode (AND)
Runs children in order until one **fails**.

```java
new SequenceNode()
    .addChild(findEnemy)      // Must succeed
    .addChild(moveToEnemy)    // Then this
    .addChild(attack)         // Then this
```

**Use when**: Steps must happen in order

#### ParallelNode
Runs all children simultaneously.

```java
new ParallelNode(ParallelNode.Policy.REQUIRE_ALL)
    .addChild(trackEnemy)
    .addChild(dodgeAttacks)
    .addChild(counterAttack)
```

**Use when**: Multiple things happen at once

### Decorator Nodes

Modify how a child behaves:

```java
// Repeat 3 times
new RepeatNode(attackBehavior, 3)

// Only if condition is true
new ConditionalNode(attackBehavior, ctx -> ctx.getEntity().getHealth() > 10)

// Add cooldown
new CooldownNode(specialAttack, 5.0f) // 5 second cooldown

// Invert result
new InverterNode(isEnemyNearby) // SUCCESS becomes FAILURE
```

### Leaf Nodes (Actions)

The actual behaviors:

```java
// Simple action
new ActionNode(ctx -> {
    ctx.getEntity().jump();
    return Behavior.Status.SUCCESS;
});

// Custom behavior class
public class AttackBehavior extends ActionNode {
    public AttackBehavior() {
        super(ctx -> {
            // Attack logic here
            return Behavior.Status.SUCCESS;
        });
    }
}
```

## Building a Tree

### Method 1: Inline

```java
BehaviorTree brain = new BehaviorTree(
    new SelectorNode()
        .addChild(new SequenceNode()
            .addChild(findEnemy)
            .addChild(attack)
        )
        .addChild(patrol)
);
```

### Method 2: Builder Pattern

```java
public class CombatAI {
    public static BehaviorTree create() {
        // Build sub-trees
        Behavior combatBehavior = createCombatBehavior();
        Behavior defenseBehavior = createDefenseBehavior();
        Behavior idleBehavior = createIdleBehavior();
        
        // Combine into main tree
        return new BehaviorTree(
            new SelectorNode()
                .addChild(combatBehavior)
                .addChild(defenseBehavior)
                .addChild(idleBehavior)
        );
    }
    
    private static Behavior createCombatBehavior() {
        return new SequenceNode()
            .addChild(new FindEnemyBehavior())
            .addChild(new AttackBehavior());
    }
    
    // ... more methods
}
```

### Method 3: Modular Components

```java
public class AIComponents {
    public static Behavior createCombatModule() { /* ... */ }
    public static Behavior createExplorationModule() { /* ... */ }
    public static Behavior createSocialModule() { /* ... */ }
}

// Mix and match
BehaviorTree brain = new BehaviorTree(
    new SelectorNode()
        .addChild(AIComponents.createCombatModule())
        .addChild(AIComponents.createExplorationModule())
);
```

## Execution Flow

### Example Tree

```java
SelectorNode root
├─ SequenceNode (combat)
│  ├─ FindEnemy
│  └─ Attack
└─ Patrol
```

### Execution Steps

1. **Root (Selector)** starts
2. Try **first child** (Sequence)
3. Sequence tries **FindEnemy**
   - Returns `SUCCESS` (enemy found)
4. Sequence tries **Attack**
   - Returns `RUNNING` (attacking)
5. Sequence returns `RUNNING` to Selector
6. Selector returns `RUNNING` (first child still running)

Next tick:
1. Selector continues with **first child** (still running)
2. Sequence continues with **Attack**
   - Returns `SUCCESS` (enemy defeated)
3. Sequence returns `SUCCESS` to Selector
4. Selector returns `SUCCESS` (first child succeeded)

Next tick:
1. Tree resets and starts fresh
2. FindEnemy returns `FAILURE` (no enemies)
3. Sequence returns `FAILURE` to Selector
4. Selector tries **second child** (Patrol)
5. Patrol returns `RUNNING`
6. Selector returns `RUNNING`

## Best Practices

### ✅ DO

- **Keep trees shallow** - Max 3-4 levels deep
- **Reuse behaviors** - Create modular components
- **Use descriptive names** - `findNearestEnemy` not `behavior1`
- **Test incrementally** - Build and test small parts first
- **Use Blackboard** - Share data between behaviors

### ❌ DON'T

- **Don't make huge trees** - Split into multiple trees if needed
- **Don't hardcode values** - Use Blackboard or config
- **Don't forget cleanup** - Remove brains when entity dies
- **Don't block the thread** - Keep actions fast

## Common Patterns

### Priority-based Behavior

```java
new SelectorNode()
    .addChild(fleeIfLowHealth)      // Highest priority
    .addChild(attackIfEnemyClose)   // Medium priority
    .addChild(patrol)               // Lowest priority (fallback)
```

### State Machine Simulation

```java
new SelectorNode()
    .addChild(new ConditionalNode(combatBehavior, 
        ctx -> ctx.getBlackboard().get("state").equals("combat")))
    .addChild(new ConditionalNode(exploreBehavior,
        ctx -> ctx.getBlackboard().get("state").equals("explore")))
```

### Looping Behavior

```java
new RepeatNode(
    new SequenceNode()
        .addChild(findResource)
        .addChild(gatherResource)
        .addChild(returnToBase),
    -1 // Infinite loop
)
```

## Debugging Tips

### 1. Add Logging

```java
new ActionNode(ctx -> {
    LOGGER.info("Executing attack behavior");
    // ... attack logic
    return Behavior.Status.SUCCESS;
});
```

### 2. Use Blackboard for State

```java
ctx.getBlackboard().set("last_action", "attack");
ctx.getBlackboard().set("attack_count", count + 1);
```

### 3. Visualize the Tree

```java
public void printTree(BehaviorNode node, int depth) {
    String indent = "  ".repeat(depth);
    System.out.println(indent + node.getClass().getSimpleName());
    
    if (node instanceof CompositeNode composite) {
        for (Behavior child : composite.getChildren()) {
            printTree((BehaviorNode) child, depth + 1);
        }
    }
}
```

## Performance Considerations

- **Tick frequency**: Trees tick every game tick (20 TPS)
- **Node count**: Keep under 50 nodes per tree
- **Sensor updates**: Use update frequencies to reduce load
- **Blackboard**: Prefer over recalculating values

## Next Steps

- **[Basic Nodes](Basic-Nodes)** - Learn about Selector, Sequence, Action
- **[Advanced Nodes](Advanced-Nodes)** - Parallel, Repeat, Conditional
- **[Blackboard](Blackboard)** - Share data between behaviors
- **[Examples](Basic-Examples)** - See trees in action

---

**Questions?** Check the **[FAQ](FAQ)** or **[Troubleshooting](Troubleshooting)** pages!
