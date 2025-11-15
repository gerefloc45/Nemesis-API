# Finite State Machine Guide

VoidAPI provides a complete Finite State Machine (FSM) system for managing entity AI states.

## Table of Contents

- [Basic FSM](#basic-fsm)
- [States](#states)
- [Transitions](#transitions)
- [Hierarchical FSM](#hierarchical-fsm)
- [FSM-Behavior Tree Integration](#fsm-behavior-tree-integration)
- [State Persistence](#state-persistence)
- [Examples](#examples)

---

## Basic FSM

A Finite State Machine consists of states and transitions between them.

### Creating a Simple FSM

```java
// Create states
State idleState = new IdleState();
State patrolState = new State("Patrol") {
    @Override
    public void onUpdate(BehaviorContext context) {
        // Patrol logic
    }
};
State combatState = new State("Combat") {
    @Override
    public void onUpdate(BehaviorContext context) {
        // Combat logic
    }
};

// Create state machine
StateMachine fsm = new StateMachine("EntityAI");
fsm.addState(idleState);
fsm.addState(patrolState);
fsm.addState(combatState);
fsm.setInitialState("Idle");

// Add transitions
fsm.addTransition(new Transition(
    idleState, 
    patrolState,
    ctx -> ctx.getBlackboard().get("shouldPatrol", Boolean.class, false)
));

fsm.addTransition(new Transition(
    patrolState,
    combatState,
    ctx -> ctx.getBlackboard().get("enemyNearby", Boolean.class, false),
    10 // High priority
));

// Start and update
fsm.start(context);
fsm.update(context); // Call every tick
```

---

## States

### State Lifecycle

Every state has three lifecycle methods:

```java
public class CustomState extends State {
    public CustomState() {
        super("CustomState");
    }

    @Override
    public void onEnter(BehaviorContext context) {
        // Called when entering this state
        super.onEnter(context);
        System.out.println("Entering state");
    }

    @Override
    public void onUpdate(BehaviorContext context) {
        // Called every tick while in this state
        System.out.println("Time in state: " + getTimeInState());
    }

    @Override
    public void onExit(BehaviorContext context) {
        // Called when leaving this state
        System.out.println("Exiting state");
        super.onExit(context);
    }
}
```

### Built-in States

#### IdleState

```java
State idle = new IdleState(); // Does nothing
```

#### BehaviorState

Executes a behavior tree behavior:

```java
Behavior attackBehavior = new SequenceNode()
    .addChild(new ActionNode(ctx -> {
        // Attack logic
        return Behavior.Status.SUCCESS;
    }));

State attackState = new BehaviorState("Attack", attackBehavior);
```

#### TimedState

Auto-transitions after a duration:

```java
State stunState = new TimedState("Stunned", 3.0f) {
    @Override
    protected void onTimeout(BehaviorContext context) {
        System.out.println("Stun expired!");
    }
};
```

---

## Transitions

### Basic Transition

```java
Transition transition = new Transition(
    fromState,
    toState,
    ctx -> /* condition */
);
```

### Transition with Priority

Higher priority transitions are checked first:

```java
Transition highPriority = new Transition(
    fromState,
    toState,
    ctx -> ctx.getBlackboard().get("emergency", Boolean.class, false),
    100 // High priority
);
```

### Transition Builder

```java
Transition transition = new Transition.Builder()
    .from(idleState)
    .to(combatState)
    .when(ctx -> ctx.getBlackboard().get("enemyNearby", Boolean.class, false))
    .priority(10)
    .name("IdleToCombat")
    .build();
```

### Common Transition Conditions

```java
// Health-based
ctx -> ctx.getEntity().getHealth() < 10.0f

// Blackboard-based
ctx -> ctx.getBlackboard().get("targetFound", Boolean.class, false)

// Time-based
ctx -> ctx.getBlackboard().get("currentState", State.class).getTimeInState() > 5.0f

// Distance-based
ctx -> {
    Entity target = ctx.getBlackboard().get("target", Entity.class);
    return target != null && ctx.getEntity().distanceTo(target) < 10.0;
}
```

---

## Hierarchical FSM

States can contain their own sub-state machines:

```java
// Create sub-state machine for combat
StateMachine combatSubMachine = new StateMachine("CombatSub");
combatSubMachine.addState(new State("Melee") { /* ... */ });
combatSubMachine.addState(new State("Ranged") { /* ... */ });
combatSubMachine.setInitialState("Melee");

// Create hierarchical state
HierarchicalState combatState = new HierarchicalState("Combat", combatSubMachine);

// Add to main FSM
StateMachine mainFSM = new StateMachine("MainAI");
mainFSM.addState(new IdleState());
mainFSM.addState(combatState);
mainFSM.setInitialState("Idle");

// When in combat state, the sub-machine will run automatically
```

---

## FSM-Behavior Tree Integration

### Using FSM in Behavior Tree

```java
StateMachine fsm = /* create FSM */;

BehaviorTree tree = new BehaviorTree(
    new SequenceNode()
        .addChild(new StateMachineNode(fsm, "current_state"))
        .addChild(/* other behaviors */)
);
```

### Using Behavior Tree in FSM

```java
Behavior treeRoot = new SelectorNode()
    .addChild(/* behaviors */);

State behaviorState = new BehaviorState("TreeState", treeRoot);
fsm.addState(behaviorState);
```

---

## State Persistence

Save and restore FSM state:

```java
// Save
NbtCompound nbt = StatePersistence.save(fsm, blackboard);
entity.writeCustomDataToNbt(nbt);

// Restore
NbtCompound nbt = entity.readCustomDataFromNbt();
StatePersistence.restore(fsm, nbt, blackboard);
```

### State Snapshots

```java
StateSnapshot snapshot = StatePersistence.createSnapshot(fsm);
System.out.println("Current state: " + snapshot.getCurrentState());
System.out.println("Running: " + snapshot.isRunning());
```

---

## Examples

### Guard AI

```java
StateMachine guardAI = StateMachineBuilder.create("GuardAI")
    .state(new IdleState())
    .transitionTo(patrolState, ctx -> true) // Always patrol from idle
    
    .state(patrolState)
    .transitionTo(alertState, ctx -> 
        ctx.getBlackboard().get("heardNoise", Boolean.class, false), 5)
    .transitionTo(combatState, ctx ->
        ctx.getBlackboard().get("enemyVisible", Boolean.class, false), 10)
    
    .state(alertState)
    .transitionTo(patrolState, ctx -> 
        alertState.getTimeInState() > 5.0f)
    .transitionTo(combatState, ctx ->
        ctx.getBlackboard().get("enemyVisible", Boolean.class, false), 10)
    
    .state(combatState)
    .transitionTo(patrolState, ctx ->
        !ctx.getBlackboard().get("enemyVisible", Boolean.class, false))
    
    .initialState("Idle")
    .build();
```

### Boss AI with Phases

```java
// Phase 1 sub-machine
StateMachine phase1 = new StateMachine("Phase1");
phase1.addState(new BehaviorState("BasicAttack", basicAttackBehavior));
phase1.addState(new BehaviorState("Dodge", dodgeBehavior));
phase1.setInitialState("BasicAttack");

// Phase 2 sub-machine
StateMachine phase2 = new StateMachine("Phase2");
phase2.addState(new BehaviorState("SpecialAttack", specialAttackBehavior));
phase2.addState(new BehaviorState("Summon", summonBehavior));
phase2.setInitialState("SpecialAttack");

// Main FSM
HierarchicalState phase1State = new HierarchicalState("Phase1", phase1);
HierarchicalState phase2State = new HierarchicalState("Phase2", phase2);

StateMachine bossAI = new StateMachine("BossAI");
bossAI.addState(phase1State);
bossAI.addState(phase2State);
bossAI.addTransition(new Transition(
    phase1State,
    phase2State,
    ctx -> ctx.getEntity().getHealth() < ctx.getEntity().getMaxHealth() * 0.5f
));
bossAI.setInitialState("Phase1");
```

### State Change Listener

```java
fsm.addListener((from, to, context) -> {
    System.out.println("State changed: " + 
        (from != null ? from.getName() : "null") + 
        " -> " + to.getName());
    
    // Play sound on state change
    if (to.getName().equals("Combat")) {
        context.getWorld().playSound(/* combat music */);
    }
});
```

---

## Best Practices

1. **Keep states focused**: Each state should have a single responsibility
2. **Use hierarchical FSM**: For complex AI with sub-behaviors
3. **Prioritize transitions**: Use priority for important transitions (flee, emergency)
4. **Combine with behavior trees**: Use FSM for high-level states, BT for detailed logic
5. **Add listeners**: For debugging and visual/audio feedback
6. **Save state**: For entities that persist across world reloads

---

## API Reference

### StateMachine

- `addState(State)` - Add a state
- `setInitialState(String)` - Set starting state
- `addTransition(Transition)` - Add transition
- `start(BehaviorContext)` - Start FSM
- `stop(BehaviorContext)` - Stop FSM
- `update(BehaviorContext)` - Update (call every tick)
- `forceTransition(String, BehaviorContext)` - Force state change
- `getCurrentState()` - Get current state
- `addListener(StateChangeListener)` - Add listener

### State

- `onEnter(BehaviorContext)` - Entry logic
- `onUpdate(BehaviorContext)` - Update logic
- `onExit(BehaviorContext)` - Exit logic
- `getTimeInState()` - Time since entering (seconds)
- `isActive()` - Check if currently active

### Transition

- `shouldTransition(BehaviorContext)` - Check condition
- `getFromState()` - Get source state
- `getToState()` - Get target state
- `getPriority()` - Get priority

---

For more information, see the [Behavior Trees Guide](Behavior-Trees.md) and [Quick Start](Quick-Start.md).
