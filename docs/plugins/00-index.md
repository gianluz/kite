# Kite Plugins

Kite plugins extend Kite's functionality with type-safe DSLs for specific tools and platforms.

## 📦 What are Kite Plugins?

Plugins provide **type-safe DSLs** that replace raw `exec()` calls with structured, validated APIs:

```kotlin
// Without plugin (raw exec)
execute {
    exec("git", "tag", "v1.0.0")
    exec("git", "push", "origin", "v1.0.0")
    exec("./gradlew", "clean", "build", "--parallel")
}

// With plugin (type-safe DSL)
execute {
    git {
        tag("v1.0.0")
        push(tags = true)
    }

    gradle {
        build {
            parallel = true
        }
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

| Plugin                        | Description                                                          | Status     |
|-------------------------------|----------------------------------------------------------------------|------------|
| [Git](01-plugin-git.md)       | Version control operations (tag, push, fetch, pull, merge, checkout) | ✅ Complete |
| [Gradle](02-plugin-gradle.md) | Flexible Gradle task execution (build, test, Android, multi-module)  | ✅ Complete |
| [Docker](03-plugin-docker.md) | Container operations                                                 | 📋 Planned |
| [Maven](04-plugin-maven.md)   | Maven publishing                                                     | 📋 Planned |

## 🔌 Using Plugins

### From Maven Central

```kotlin
// .kite/segments/build.kite.kts
@file:DependsOn("com.gianluz.kite:git:0.1.0-alpha8")
@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha8")

import io.kite.plugins.git.*
import io.kite.plugins.gradle.*

segments {
    segment("release") {
        execute {
            git {
                fetch()
                checkout("main")
                pull(rebase = true)
                tag("v1.0.0")
                push(tags = true)
            }

            gradle {
                clean()
                build {
                    parallel = true
                    stacktrace = true
                }
            }
        }
    }
}
```

### From Maven Local (Plugin Development Only)

When developing a plugin locally, you can publish it to Maven Local and use it immediately:

```bash
# Publish your plugin to Maven Local
./gradlew :kite-plugins:git:publishToMavenLocal
```

```kotlin
// Kite checks Maven Local automatically before Maven Central
@file:DependsOn("com.gianluz.kite:git:0.1.0-SNAPSHOT")

import io.kite.plugins.git.*
```

**Note:** This workflow is only needed when working on the plugins themselves. For normal usage, just use Maven Central.

### From Local JAR (Quick Testing)

```kotlin
// .kite/segments/build.kite.kts
@file:DependsOnJar("../kite-plugins/git/build/libs/git-0.1.0-alpha8.jar")
@file:DependsOnJar("../kite-plugins/gradle/build/libs/gradle-0.1.0-alpha8.jar")

import io.kite.plugins.git.*
import io.kite.plugins.gradle.*
```

**To build the JARs:**

```bash
# Build specific plugin
./gradlew :kite-plugins:git:build
./gradlew :kite-plugins:gradle:build

# Or build all plugins
./gradlew :kite-plugins:git:build :kite-plugins:gradle:build
```

**Note:** Using `@DependsOnJar` doesn't include transitive dependencies. For production use, prefer Maven Local (
`@DependsOn`) which automatically resolves all dependencies.

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
