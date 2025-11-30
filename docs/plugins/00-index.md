# Kite Plugins

Kite plugins extend Kite's functionality with type-safe DSLs for specific tools and platforms.

## 📦 What are Kite Plugins?

Plugins provide **type-safe DSLs** that replace raw `exec()` calls with structured, validated APIs:

```kotlin
// Without plugin (raw exec)
execute {
    exec("git", "tag", "v1.0.0")
    exec("git", "push", "origin", "v1.0.0")
}

// With plugin (type-safe DSL)
execute {
    git {
        tag("v1.0.0")
        push(tags = true)
    }
}
```

## 🎯 Benefits

| Benefit | Description |
|---------|-------------|
| **Type Safety** | Catch errors at compile time, not runtime |
| **IDE Support** | Autocomplete, documentation, refactoring |
| **Validation** | Plugin validates inputs before execution |
| **Discoverability** | IDE shows available operations |
| **Reusability** | Share common patterns across projects |

## 📚 Official Plugins

| Plugin | Description | Status |
|--------|-------------|--------|
| [Git](01-plugin-git.md) | Version control operations | 🚧 In Development |
| [Gradle](02-plugin-gradle.md) | Build automation | 📋 Planned |
| [Docker](03-plugin-docker.md) | Container operations | 📋 Planned |
| [Maven](04-plugin-maven.md) | Maven publishing | 📋 Planned |

## 🔌 Using Plugins

### From Maven Central

```kotlin
// .kite/segments/build.kite.kts
@file:DependsOn("io.kite.plugins:git:1.0.0")

import io.kite.plugins.git.*

segments {
    segment("tag-release") {
        execute {
            git {
                tag("v1.0.0")
                push(tags = true)
            }
        }
    }
}
```

### From Maven Local (Development)

```kotlin
// .kite/segments/build.kite.kts
@file:DependsOnMavenLocal("io.kite.plugins:git:1.0.0-SNAPSHOT")

import io.kite.plugins.git.*
```

**To publish to Maven Local:**

```bash
./gradlew :kite-plugins:git:publishToMavenLocal
```

### From Local JAR

```kotlin
// .kite/segments/build.kite.kts
@file:DependsOnJar("../kite-plugins/git/build/libs/git-1.0.0.jar")

import io.kite.plugins.git.*
```

**To build the JAR:**

```bash
./gradlew :kite-plugins:git:build
```

## 🛠️ Creating Plugins

See the [Plugin Development Guide](../dev/05-plugin-development.md) for detailed instructions.

### Quick Start

```kotlin
// my-plugin/src/main/kotlin/com/company/MyPlugin.kt
package com.company

import io.kite.core.ExecutionContext

class MyPlugin(private val ctx: ExecutionContext) {
    fun doSomething() {
        ctx.logger.info("Hello from plugin!")
        ctx.exec("echo", "Plugin works!")
    }
}

// Extension function makes it available in execute {}
fun ExecutionContext.myPlugin(configure: MyPlugin.() -> Unit) {
    MyPlugin(this).configure()
}
```

**Usage:**

```kotlin
@file:DependsOnJar("./my-plugin.jar")

import com.company.*

execute {
    myPlugin {
        doSomething()
    }
}
```

## 📖 See Also

- [Plugin Development Guide](../dev/05-plugin-development.md)
- [External Dependencies](../10-external-dependencies.md)
- [Kite Core API](../06-execution-context.md)
