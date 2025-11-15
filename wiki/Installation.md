# Installation

This guide shows you how to add VoidAPI to your Fabric mod project.

## Prerequisites

- Minecraft 1.21.1
- Fabric Loader 0.16.9+
- Fabric API 0.106.0+
- Java 21

## Gradle Setup

### Step 1: Add Repository

Add the VoidAPI repository to your `build.gradle`:

```gradle
repositories {
    maven { url 'https://maven.fabricmc.net/' }
    mavenCentral()
    // Add VoidAPI repository here when published
}
```

### Step 2: Add Dependency

Add VoidAPI as a dependency:

```gradle
dependencies {
    minecraft "com.mojang:minecraft:1.21.1"
    mappings "net.fabricmc:yarn:1.21.1+build.2:v2"
    modImplementation "net.fabricmc:fabric-loader:0.16.9"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.106.0+1.21.1"
    
    // Add VoidAPI
    modImplementation "com.gerefloc45:voidapi:0.3.0"
    include "com.gerefloc45:voidapi:0.3.0" // Bundle it with your mod
}
```

### Step 3: Refresh Gradle

Run in terminal:
```bash
./gradlew build --refresh-dependencies
```

## Local Development Setup

If you're developing locally or the mod isn't published yet:

### Option 1: Local JAR

1. Build VoidAPI:
```bash
cd path/to/VoidAPI
./gradlew build
```

2. Add local JAR to your mod:
```gradle
dependencies {
    modImplementation files("path/to/VoidAPI/build/libs/voidapi-0.3.0.jar")
    include files("path/to/VoidAPI/build/libs/voidapi-0.3.0.jar")
}
```

### Option 2: Composite Build (Recommended for Development)

1. Create `settings.gradle` in your mod:
```gradle
rootProject.name = 'your-mod-name'

includeBuild('../VoidAPI') {
    dependencySubstitution {
        substitute module('com.gerefloc45:voidapi') using project(':')
    }
}
```

2. Add dependency normally:
```gradle
dependencies {
    modImplementation "com.gerefloc45:voidapi:0.3.0"
    include "com.gerefloc45:voidapi:0.3.0"
}
```

This way, changes to VoidAPI are immediately available!

## Verify Installation

Create a test class to verify everything works:

```java
package com.yourmod;

import com.gerefloc45.voidapi.api.BehaviorTree;
import com.gerefloc45.voidapi.api.nodes.SelectorNode;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YourMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("yourmod");

    @Override
    public void onInitialize() {
        // Test VoidAPI is loaded
        BehaviorTree testTree = new BehaviorTree(new SelectorNode());
        LOGGER.info("VoidAPI loaded successfully!");
    }
}
```

Run your mod. You should see the log message without errors.

## fabric.mod.json

Make sure to declare VoidAPI as a dependency in your `fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "yourmod",
  "version": "1.0.0",
  
  "depends": {
    "fabricloader": ">=0.16.9",
    "fabric-api": "*",
    "minecraft": "1.21.1",
    "java": ">=21",
    "voidapi": ">=0.3.0"
  }
}
```

## Troubleshooting

### "Cannot resolve symbol 'voidapi'"

- Make sure you refreshed Gradle dependencies
- Check that the JAR path is correct (for local builds)
- Verify your Gradle cache: `./gradlew clean build`

### ClassNotFoundException at runtime

- Make sure you used `include` in your dependencies
- Verify `fabric.mod.json` has the dependency listed
- Check that VoidAPI JAR is in your mod's JAR under `META-INF/jars/`

### Version Conflicts

- Make sure all dependencies use compatible versions
- Check Fabric API version matches Minecraft version
- Use `./gradlew dependencies` to see the dependency tree

## Next Steps

✅ Installation complete! Now:

1. **[Quick Start Guide](Quick-Start)** - Build your first AI
2. **[Basic Examples](Basic-Examples)** - See it in action
3. **[Core Concepts](Behavior-Trees)** - Understand the system

---

**Need help?** Check the **[Troubleshooting](Troubleshooting)** page or open an issue on GitHub!
