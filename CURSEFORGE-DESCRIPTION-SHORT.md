# 🧠 VoidAPI - Professional AI Framework for Minecraft

**Transform your mobs from predictable to intelligent.**

VoidAPI brings AAA-game AI technology to Minecraft modding. Create entities with sophisticated decision-making, environmental awareness, and adaptive behavior using industry-standard patterns like Behavior Trees, Utility AI, and advanced Perception Systems.

> ⚠️ **Library Mod:** VoidAPI provides AI tools for mod developers. Players need this only if required by other mods.

---

## ✨ Core Features

### 🌳 **Behavior Trees**
Build complex AI logic with composable nodes:
- **Selector & Sequence** - Hierarchical decision making
- **Parallel Execution** - Multi-task coordination
- **Decorators** - Cooldowns, retries, inverters, and conditions
- **Dynamic Adaptation** - Runtime behavior modification

### 💾 **Blackboard Memory System**
Intelligent data management for AI entities:
- **Type-Safe Storage** - Structured data with automatic validation
- **Automatic Cleanup** - Memory-efficient lifecycle management
- **Shared Context** - Cross-behavior communication and coordination

### 👁️ **Advanced Perception**
Give entities awareness of their environment:
- **Multi-Sensor System** - Vision, hearing, and proximity detection
- **Smart Filtering** - Custom predicates for target selection
- **Memory Decay** - Realistic forgetting and attention span
- **Sensor Fusion** - Combine multiple inputs for better decisions

### 🎯 **Utility AI Engine**
Score-based decision making for natural behavior:
- **Response Curves** - Linear, quadratic, exponential scoring
- **Dynamic Priorities** - Context-aware behavior selection
- **Composite Considerations** - Multi-factor decision making

### ⚡ **Production-Ready Performance**
Built for real-world server environments:
- **Async Processing** - Zero impact on game tick performance
- **Multiplayer Optimized** - Efficient network synchronization
- **Scalable Architecture** - Handle hundreds of AI entities

---

## 🎮 Use Cases

**What can you build with VoidAPI?**

- **🔥 Boss Encounters** - Multi-phase battles with adaptive tactics, enrage timers, and intelligent ability rotation
- **🏙️ Living NPCs** - Villagers with daily routines, emotional states, and context-aware dialogue
- **🐾 Smart Companions** - Pets that learn preferences, coordinate in combat, and respond to commands
- **🏭 Automation** - Self-organizing factory systems and adaptive resource management
- **🌍 Ecosystem Simulation** - Predator-prey dynamics, territorial behavior, and emergent interactions
- **🎯 Tactical Combat** - Squad coordination, cover systems, and strategic positioning

---

## 🚀 Quick Start Example

**Create an intelligent guard in just a few lines:**

```java
// Build behavior tree: "If enemy detected, attack; otherwise patrol"
BehaviorTree guardAI = new BehaviorTree(
    new SelectorNode()
        .addChild(new SequenceNode()
            .addChild(detectEnemyTask)      // Check for threats
            .addChild(new CooldownNode(attackTask, 1.0f))  // Attack with cooldown
        )
        .addChild(patrolTask)               // Default behavior
);

// Attach AI to your entity
BrainController.getInstance().attachBrain(entity, guardAI);
BrainTicker.registerEntity(entity);
```

**That's it!** Your entity now has professional-grade AI with decision-making, cooldowns, and fallback behavior.

---

## 📦 Installation

### For Mod Developers

Add VoidAPI to your `build.gradle`:

```gradle
repositories {
    maven { url "https://api.modrinth.com/maven" }
}

dependencies {
    modImplementation "maven.modrinth:void-api:0.1.0"
    include "maven.modrinth:void-api:0.1.0"
}
```

### For Players

VoidAPI is automatically bundled with mods that require it. No manual installation needed!

---

## 🛠️ Technical Specifications

| Requirement | Version |
|------------|---------|
| **Minecraft** | 1.21.1 |
| **Fabric Loader** | 0.15.0+ |
| **Java** | 17+ |
| **License** | MIT (Open Source) |

---

## 📚 Documentation & Resources

**Get Started:**
- 📖 **[Quick Start Guide](https://github.com/gerefloc45/VoidAPI/wiki)** - From zero to AI in 5 minutes
- 📘 **[Complete API Reference](https://github.com/gerefloc45/VoidAPI/wiki)** - Full documentation
- 💡 **[Code Examples](https://github.com/gerefloc45/VoidAPI/wiki)** - Ready-to-use implementations

**Advanced Topics:**
- 🎯 **[Utility AI Deep Dive](https://github.com/gerefloc45/VoidAPI/wiki)** - Score-based decision systems
- 🗺️ **[Development Roadmap](https://github.com/gerefloc45/VoidAPI/wiki)** - Upcoming features (coming soon)


---

## 🏆 Why Choose VoidAPI?

| Feature | Benefit |
|---------|---------|
| **🧠 Industry-Standard AI** | Use the same patterns as AAA games (Behavior Trees, Utility AI) |
| **⚡ Zero Performance Impact** | Async architecture keeps your server running smoothly |
| **🔧 Developer Experience** | Intuitive API with comprehensive documentation and examples |
| **🎨 Pure Library Design** | No gameplay changes - just tools for your creativity |
| **🌐 Open Source & Free** | MIT License - use it anywhere, modify as needed |
| **📈 Production Tested** | Battle-tested in real multiplayer environments |

