# VoidAPI Development Roadmap

## Current Version: v0.1.0-beta

**Status:** Active Development  
**Target:** Minecraft 1.21.1 Fabric

---

## ✅ Completed (v0.1.0-beta)

### Core Framework
- ✅ Behavior Tree System
  - Basic node types (Selector, Sequence, Action)
  - Behavior execution and status handling
  - Tree lifecycle management
- ✅ Blackboard Memory System
  - Type-safe data storage
  - Get/Set/Has/Remove operations
  - Per-entity memory isolation
- ✅ Brain Controller
  - Entity-to-tree attachment
  - Centralized brain management
  - Brain ticking system
- ✅ Brain Ticker
  - Automatic server tick integration
  - Entity registration/unregistration
  - Fabric event integration

### Advanced Nodes
- ✅ ParallelNode - Execute multiple behaviors simultaneously
- ✅ RepeatNode - Loop behaviors with iteration limits
- ✅ CooldownNode - Time-based execution throttling
- ✅ ConditionalNode - Conditional behavior execution
- ✅ InverterNode - Result inversion

### Perception System
- ✅ Sensor API - Base interface for all sensors
- ✅ EntitySensor - Detect and filter nearby entities
- ✅ BlockSensor - Detect specific blocks in range
- ✅ SoundSensor - React to sound events
- ✅ PerceptionMemory - Remember entities after detection
- ✅ SensorManager - Multi-sensor coordination

### Utility AI
- ✅ UtilitySelector - Score-based behavior selection
- ✅ Scorer - Utility score calculation
- ✅ Consideration - Multi-factor scoring
- ✅ ResponseCurve - Value transformation curves
- ✅ DynamicPrioritySelector - Priority-based selection

### Utilities
- ✅ AsyncHelper - Thread pool management
- ✅ EntityUtil - Entity helper methods
- ✅ CompletableFuture integration

---

## 🚧 In Progress (v0.2.0)

### Enhanced Behavior Nodes
- 🔄 **TimeoutNode** - Fail behavior after timeout
- 🔄 **RetryNode** - Retry failed behaviors with backoff
- 🔄 **RandomSelectorNode** - Random child selection
- 🔄 **WeightedSelectorNode** - Weighted random selection
- 🔄 **UntilSuccessNode** - Repeat until success
- 🔄 **UntilFailureNode** - Repeat until failure

### Pathfinding Integration
- 🔄 **PathfindingBehavior** - Navigate to target positions
- 🔄 **FollowEntityBehavior** - Follow moving entities
- 🔄 **PatrolBehavior** - Patrol waypoint lists
- 🔄 **FleeFromEntityBehavior** - Escape from threats
- 🔄 **WanderBehavior** - Random exploration

### Animation Support
- 🔄 **AnimationNode** - Trigger entity animations
- 🔄 **AnimationController** - Animation state management
- 🔄 **GeckoLib integration** - Optional GeckoLib support

### Debugging Tools
- 🔄 **BehaviorTreeDebugger** - Visual tree debugging
- 🔄 **BlackboardInspector** - Runtime memory inspection
- 🔄 **Performance Profiler** - Identify bottlenecks
- 🔄 **Logging System** - Detailed behavior execution logs

---

## 📋 Planned Features

### v0.3.0 - State Machines
**ETA:** Q1 2026

- **Finite State Machine (FSM)** - Traditional state-based AI
- **Hierarchical FSM** - Nested state machines
- **FSM-Behavior Tree hybrid** - Best of both worlds
- **State transitions** - Condition-based state switching
- **State persistence** - Save/load state data

### v0.3.5 - Goal-Oriented Action Planning (GOAP)
**ETA:** Q2 2026

- **Goal system** - Define entity goals
- **Action planner** - Dynamic action sequencing
- **Precondition checking** - Action prerequisites
- **Cost-based planning** - Optimal plan selection
- **Dynamic replanning** - Adapt to changing conditions

### v0.4.0 - Machine Learning Integration
**ETA:** Q3 2026

- **Behavior Learning** - Learn from player interactions
- **Pattern Recognition** - Detect player behavior patterns
- **Adaptive AI** - Adjust difficulty dynamically
- **Neural Network integration** - Optional ML backends
- **Training mode** - Supervised learning support

### v0.5.0 - Multiplayer & Networking
**ETA:** Q4 2026

- **Synchronized AI** - Client-side prediction
- **AI sharing** - Share AI between players
- **Network optimization** - Reduce bandwidth usage
- **Spectator mode** - Watch AI decisions live
- **Remote debugging** - Debug AI over network

### v0.6.0 - Advanced Perception
**ETA:** Q1 2027

- **Vision cones** - Realistic field of view
- **Line-of-sight** - Occlusion detection
- **Smell sensor** - Track by scent
- **Touch sensor** - React to physical contact
- **Memory degradation** - Forget over time
- **Attention system** - Focus on important stimuli

### v0.7.0 - Social AI
**ETA:** Q2 2027

- **Faction system** - Friend/foe relationships
- **Reputation tracking** - Remember player actions
- **Communication** - Entity-to-entity messaging
- **Cooperation behaviors** - Teamwork and coordination
- **Leadership system** - Follow/command hierarchies
- **Emotion system** - Mood-based behavior changes

### v0.8.0 - Optimization & Performance
**ETA:** Q3 2027

- **LOD AI** - Simplified AI at distance
- **Budget system** - CPU time management
- **Caching system** - Reuse calculations
- **Parallel processing** - Multi-threaded AI
- **Incremental updates** - Spread work over frames
- **Memory pooling** - Reduce allocations

---

## 🎯 Long-term Vision

### Modding Ecosystem
- **AI Marketplace** - Share custom behaviors
- **Behavior libraries** - Reusable AI components
- **Templates** - Pre-made AI configurations
- **Documentation portal** - Interactive guides
- **Community examples** - User-contributed AIs

### Platform Support
- **Forge compatibility** - Multi-loader support
- **Quilt support** - Modern loader integration
- **Sponge support** - Server-side AI
- **Datapack integration** - JSON-based AI configs
- **Command interface** - Control AI via commands

### Developer Tools
- **Visual editor** - Drag-and-drop tree creation
- **Live reloading** - Hot-swap behaviors
- **Unit testing** - Automated AI testing
- **Benchmarking suite** - Performance testing
- **CI/CD integration** - Automated builds

### Advanced Features
- **Procedural generation** - Generate AI dynamically
- **Story system** - Quest and narrative AI
- **Economy AI** - Trading and resource management
- **Builder AI** - Construction behaviors
- **Combat system** - Advanced combat mechanics

---

## 🔬 Research & Experiments

### Under Investigation
- **Quantum computing** - Explore quantum algorithms
- **Swarm intelligence** - Collective behaviors
- **Evolutionary algorithms** - Genetic AI programming
- **Fuzzy logic** - Handle uncertainty
- **Bayesian networks** - Probabilistic reasoning
- **Reinforcement learning** - Reward-based learning

### Proof of Concepts
- **Voice commands** - Control AI with voice
- **Gesture recognition** - React to player movements
- **Emotional intelligence** - Understand player emotions
- **Natural language** - Understand text commands
- **Computer vision** - Recognize player actions

---

## 📊 Version History

| Version | Release Date | Features |
|---------|-------------|----------|
| v0.1.0-beta | 2025-11 | Core framework, behavior trees, perception, utility AI |
| v0.2.0 | TBA | Enhanced nodes, pathfinding, debugging |
| v0.3.0 | TBA | State machines, FSM |
| v0.4.0 | TBA | Machine learning |
| v0.5.0 | TBA | Multiplayer & networking |
| v1.0.0 | TBA | Stable release |

---

## 🤝 Contributing

Vogliamo il tuo feedback! Se hai idee per nuove feature o miglioramenti:

1. **Apri una Issue** su GitHub
2. **Discuti** nella community Discord
3. **Contribuisci** con Pull Requests
4. **Condividi** i tuoi use cases

---

## 📝 Priority System

- 🔥 **High Priority** - Essential features
- ⭐ **Medium Priority** - Important improvements
- 💡 **Low Priority** - Nice to have
- 🔬 **Research** - Experimental ideas

---

## ⚠️ Breaking Changes

Seguiamo il **Semantic Versioning**:
- **Major** (X.0.0) - Breaking API changes
- **Minor** (0.X.0) - New features, backward compatible
- **Patch** (0.0.X) - Bug fixes only

Durante la **beta** (v0.x.x), le API possono cambiare tra versioni minor.

---

## 📞 Feedback

- **GitHub Issues**: [Report bugs & request features](https://github.com/Gerefloc45/VoidAPI/issues)
- **Discord**: Join our community (link TBA)
- **Email**: gerefloc45@example.com

---

**Last Updated:** November 2025  
**Maintainer:** Gerefloc45  
**License:** MIT
