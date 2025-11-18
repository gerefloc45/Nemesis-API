[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/gerefloc45/VoidAPI/wiki/MIT-License)
[![GitHub Repo](https://img.shields.io/badge/GitHub-VoidAPI-181717?logo=github)](https://github.com/gerefloc45/VoidAPI)
[![Donate](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal)](https://paypal.me/Ghad22)

# VoidAPI

Advanced AI framework for **Minecraft Fabric 1.21.1** mods. VoidAPI is a pure library mod that gives your entities smart, composable behaviors using Behavior Trees, Finite State Machines, GOAP, Utility AI, a perception system and basic Machine Learning building blocks.

> **TL;DR**: VoidAPI does *not* add mobs, items or blocks. It is a **tools & AI systems** library you plug into your own mods.

---

## 🔍 Overview

VoidAPI centralizes all AI logic for your mods into a single, reusable framework:

- 🧠 **Behavior Trees** for high‑level decision making
- 🔁 **Finite State Machines (FSM)** for stable stateful logic
- 🎯 **GOAP (Goal‑Oriented Action Planning)** for dynamic planning
- 📡 **Perception System** (sensors + memory)
- 📊 **Utility AI** with response curves and scorers
- 📈 **Machine Learning hooks** for adaptive behaviors

If you want bosses, companions, NPCs or mobs that actually feel *smart*, VoidAPI is the layer that powers them.

---

## 📦 Requirements

- Minecraft **1.21.1**
- **Fabric Loader** (latest recommended for 1.21.1)
- **Java 17+** (standard for modern Fabric modding)

Exact setup details, Gradle examples and dev‑env configuration are documented in the Wiki.

---

## 🚀 Getting Started

For a smooth first experience, follow the step‑by‑step guides in the Wiki:

- 📥 **Installation** – How to add VoidAPI to your mod project  
  <https://github.com/gerefloc45/VoidAPI/wiki/Installation>
- ⚡ **Quick Start** – Build your first AI behavior tree in a few minutes  
  <https://github.com/gerefloc45/VoidAPI/wiki/Quick-Start>
- 🛠 **Project Setup** – Recommended project structure & dev environment  
  <https://github.com/gerefloc45/VoidAPI/wiki/Project-Setup>

Once you have the dependency configured, you can start attaching brains to entities using the `BrainController` and `BehaviorTree` APIs.

---

## 🧠 Core Systems

VoidAPI is split into clear, documented subsystems. Each one has its own dedicated Wiki section:

- **Core Concepts**  
  Behavior Trees, FSM, GOAP, Machine Learning, Blackboard, Brain Controller  
  <https://github.com/gerefloc45/VoidAPI/wiki/Core-Concepts>

- **Behavior Nodes**  
  Basic nodes (Selector, Sequence, Action) and advanced nodes (Parallel, Repeat, Conditional, Cooldown, Inverter, Utility nodes)  
  <https://github.com/gerefloc45/VoidAPI/wiki/Behavior-Nodes>

- **Perception System**  
  Sensors, entity detection, block checks, sound perception, perception memory  
  <https://github.com/gerefloc45/VoidAPI/wiki/Perception-System>

- **Utility AI System**  
  Scorers, considerations, response curves and how they drive decisions  
  <https://github.com/gerefloc45/VoidAPI/wiki/Utility-AI-System>

- **Machine Learning**  
  Learning behavior patterns, adaptive difficulty and training workflows  
  <https://github.com/gerefloc45/VoidAPI/wiki/Machine-Learning>

For a full, always‑up‑to‑date list of pages, start from the main Wiki hub:

👉 <https://github.com/gerefloc45/VoidAPI/wiki>

---

## 💡 Typical Use Cases

VoidAPI is designed for:

- 🐲 **Custom boss fights** with multi‑phase AI logic
- 🧟 **Smarter mobs** (flanking, retreating, searching, patrolling)
- 🧑‍🤝‍🧑 **NPC companions** that react to the player and the world
- 🏰 **Strategy / RTS‑like mechanics** inside Minecraft
- 🎮 Any entity that needs **reactive, layered decision making**

The Wiki provides concrete examples under:

- <https://github.com/gerefloc45/VoidAPI/wiki/Examples>

---

## 🧪 Code Snapshot

```java
// Example: attach a behavior tree brain to an entity
BehaviorTree brain = new BehaviorTree(
    new SelectorNode()
        .addChild(new SequenceNode()
            .addChild(findEnemyBehavior)
            .addChild(new CooldownNode(attackBehavior, 1.0f))
        )
        .addChild(patrolBehavior)
);

BrainController.getInstance().attachBrain(entity, brain);
```

More complete, commented examples are available in the **Examples & Tutorials** section of the Wiki.

---

## 📚 Documentation

All detailed documentation lives in the Wiki and is kept in sync with the current version:

- 🧭 **Home / Overview**  
  <https://github.com/gerefloc45/VoidAPI/wiki>
- 🚀 **Getting Started** (Installation, Quick Start, Project Setup)
- 🧠 **Core Concepts** (BT, FSM, GOAP, ML, Blackboard, Brain Controller)
- 🧩 **Nodes & APIs** (Behavior, Nodes, Perception, Utility AI)
- 🧪 **Examples & Tutorials** (basic AI, combat AI, utility setups)
- 🛠 **Advanced Topics** (performance, debugging, best practices)
- ❓ **Help** (FAQ, Troubleshooting)
- 📜 **Changelog**  
  <https://github.com/gerefloc45/VoidAPI/wiki/Changelog>

Use this README as an entry point; for everything else, the Wiki is the source of truth.

---

## 🔄 Version & Status

- **Current version:** `v0.5.1`  
- **Focus:** Machine Learning integration and extended Utility AI  
- **Scope:** Library‑only (no content added to the game)

See the full change history here:  
<https://github.com/gerefloc45/VoidAPI/wiki/Changelog>

---

## 🤝 Contributing

Contributions, bug reports and suggestions are welcome.

1. Check the **FAQ** and **Troubleshooting** pages first  
   <https://github.com/gerefloc45/VoidAPI/wiki/FAQ>  
   <https://github.com/gerefloc45/VoidAPI/wiki/Troubleshooting>
2. Read the **Contributing** guidelines  
   <https://github.com/gerefloc45/VoidAPI/wiki/Contributing>
3. Open an issue or a pull request on GitHub:  
   <https://github.com/gerefloc45/VoidAPI>

The Wiki also documents coding conventions and best practices so that all AI modules stay consistent.

---

## 📄 License

VoidAPI is released under the **MIT License**.  
See the `LICENSE` file in this repository (if present) or the license entry in the Wiki:

- <https://github.com/gerefloc45/VoidAPI/wiki/MIT-License>

---

## 🔗 Quick Links

- 💻 **Repository:** <https://github.com/gerefloc45/VoidAPI>
- 📘 **Main Wiki:** <https://github.com/gerefloc45/VoidAPI/wiki>
