# Kite Gradle Plugin

Flexible Gradle task execution for Kite workflows.

**Design Philosophy:** This plugin is intentionally minimal and flexible to work with **any** Gradle setup - Java,
Kotlin, Android, custom plugins, multi-module projects, etc.

## Installation

### From Maven Central

```kotlin
@file:DependsOn("io.kite.plugins:gradle:1.0.0")

import io.kite.plugins.gradle.*
```

### From Maven Local (Development)

```bash
./gradlew :kite-plugins:gradle:publishToMavenLocal
```

```kotlin
@file:DependsOn("io.kite.plugins:gradle:1.0.0-SNAPSHOT")

import io.kite.plugins.gradle.*
```

## Core Concept

The plugin provides a **single powerful method** `execute()` that can run any Gradle tasks with full control over
arguments. Convenience methods (`build()`, `clean()`, `test()`) are just wrappers.

## Usage

### Basic Task Execution

```kotlin
gradle {
    // Execute any Gradle task
    execute("build")
    execute("clean", "build")
    execute(":app:assembleDebug")
    execute(":module1:test", ":module2:test")
}
```

### With Options

```kotlin
gradle {
    execute("build") {
        parallel = true
        stacktrace = true
        property("version", "1.0.0")
        property("env", "production")
    }
}
```

### Android Examples

```kotlin
gradle {
    // Build Android app
    execute(":app:assembleDebug")
    
    // Release build
    execute(":app:assembleRelease") {
        property("android.injected.signing.store.file", keystorePath)
        property("android.injected.signing.store.password", keystorePass)
    }
    
    // Run Android tests
    execute(":app:testDebugUnitTest")
    execute(":app:connectedAndroidTest")
    
    // Lint check
    execute(":app:lintDebug")
    
    // Bundle AAB
    execute(":app:bundleRelease")
}
```

### Multi-Module Projects

```kotlin
gradle {
    // Build specific modules
    execute(":core:build", ":api:build", ":app:build") {
        parallel = true
    }
    
    // Test all modules
    execute("test") {
        continueOnFailure = true  // Don't stop on first failure
    }
    
    // Publish library modules
    execute(":core:publish", ":api:publish")
}
```

### Convenience Methods

```kotlin
gradle {
    // These are just wrappers around execute()
    clean()
    build()
    test()
    
    // With options
    build {
        parallel = true
        stacktrace = true
    }
}
```

## Complete Examples

### Standard Java/Kotlin Project

```kotlin
segments {
    segment("ci-build") {
        execute {
            gradle {
                clean()
                execute("build") {
                    parallel = true
                    stacktrace = true
                }
            }
        }
    }
}
```

### Android App Build

```kotlin
segments {
    segment("android-release") {
        execute {
            val version = env("VERSION") ?: error("VERSION not set")
            val keystorePath = requireSecret("KEYSTORE_PATH")
            val keystorePass = requireSecret("KEYSTORE_PASSWORD")
            
            gradle {
                execute("clean")
                execute(":app:bundleRelease") {
                    property("versionName", version)
                    property("android.injected.signing.store.file", keystorePath)
                    property("android.injected.signing.store.password", keystorePass)
                    property("android.injected.signing.key.alias", "release")
                    property("android.injected.signing.key.password", keystorePass)
                }
            }
            
            logger.info("✅ Android release build complete")
        }
    }
}
```

### Multi-Module Android Project

```kotlin
segments {
    segment("build-all") {
        execute {
            gradle {
                // Build all modules in parallel
                execute(
                    ":app:assembleDebug",
                    ":library1:assemble",
                    ":library2:assemble"
                ) {
                    parallel = true
                }
            }
        }
    }
    
    segment("test-all") {
        execute {
            gradle {
                // Test all modules, continue even if some fail
                execute("test") {
                    parallel = true
                    continueOnFailure = true
                }
            }
        }
    }
}
```

### Publishing Workflow

```kotlin
segments {
    segment("publish-release") {
        execute {
            gradle {
                execute("clean")
                execute("build") {
                    property("version", env("VERSION")!!)
                }
                execute("publish") {
                    property("mavenUser", requireSecret("MAVEN_USER"))
                    property("mavenPassword", requireSecret("MAVEN_PASSWORD"))
                }
            }
        }
    }
}
```

### Custom Gradle Tasks

```kotlin
segments {
    segment("custom-tasks") {
        execute {
            gradle {
                // Any custom Gradle task
                execute("detekt")
                execute("ktlintCheck")
                execute("dependencyUpdates")
                execute("generateProto")
                execute("myCustomTask")
                
                // With arguments
                execute("myTask") {
                    arg("--custom-flag")
                    property("customProp", "value")
                    systemProperty("java.awt.headless", "true")
                }
            }
        }
    }
}
```

### Android CI/CD Pipeline

```kotlin
segments {
    segment("android-ci") {
        execute {
            gradle {
                // Lint
                execute(":app:lintDebug")
                
                // Unit tests
                execute(":app:testDebugUnitTest") {
                    continueOnFailure = true
                }
                
                // Build
                execute(":app:assembleDebug")
                
                // Integration tests (if emulator available)
                if (env("CI_HAS_EMULATOR") == "true") {
                    execute(":app:connectedAndroidTest")
                }
            }
        }
    }
}
```

### Dependency Management

```kotlin
segments {
    segment("check-dependencies") {
        execute {
            gradle {
                // View dependencies
                execute("dependencies")
                
                // Check for updates
                execute("dependencyUpdates")
                
                // Verify dependencies
                execute("dependencies", "--configuration", "runtimeClasspath")
            }
        }
    }
}
```

### Offline Build

```kotlin
segments {
    segment("offline-build") {
        execute {
            gradle {
                execute("build") {
                    offline = true  // Use cached dependencies
                }
            }
        }
    }
}
```

### Debug Build Issues

```kotlin
segments {
    segment("debug-build") {
        execute {
            gradle {
                execute("build") {
                    stacktrace = true
                    info = true  // or debug = true for more details
                    daemon = false  // Disable daemon for clean state
                }
            }
        }
    }
}
```

## API Reference

### Core Method

- `execute(vararg tasks: String, options: GradleOptions.() -> Unit = {})` - Execute any Gradle tasks

### Convenience Methods

- `build(options: GradleOptions.() -> Unit = {})` - Wrapper for `execute("build")`
- `clean(options: GradleOptions.() -> Unit = {})` - Wrapper for `execute("clean")`
- `test(options: GradleOptions.() -> Unit = {})` - Wrapper for `execute("test")`

### GradleOptions (DSL)

**Flags:**

- `parallel: Boolean = false` - Enable parallel execution (--parallel)
- `daemon: Boolean = true` - Enable Gradle daemon
- `stacktrace: Boolean = false` - Show stacktrace (--stacktrace)
- `info: Boolean = false` - Info logging (--info)
- `debug: Boolean = false` - Debug logging (--debug)
- `continueOnFailure: Boolean = false` - Continue after failures (--continue)
- `offline: Boolean = false` - Offline mode (--offline)
- `refreshDependencies: Boolean = false` - Refresh dependencies (--refresh-dependencies)

**Properties:**

- `property(key: String, value: String)` - Add Gradle property (-Pkey=value)
- `systemProperty(key: String, value: String)` - Add system property (-Dkey=value)

**Custom Arguments:**

- `arg(argument: String)` - Add custom argument

**Direct access:**

- `properties: MutableMap<String, String>` - Gradle properties map
- `systemProperties: MutableMap<String, String>` - System properties map
- `arguments: MutableList<String>` - Custom arguments list

## Why This Design?

### ❌ **Too Opinionated (Bad)**

```kotlin
// Doesn't work for Android!
gradle.publishToMavenCentral()
gradle.assembleDebug() // Method doesn't exist
gradle.bundleRelease() // Method doesn't exist
```

### ✅ **Flexible (Good)**

```kotlin
// Works for everything!
gradle.execute(":app:assembleDebug")
gradle.execute(":app:bundleRelease")
gradle.execute("anyCustomTask")
```

### Benefits

1. **Works with any Gradle setup** - Java, Kotlin, Android, custom plugins
2. **Future-proof** - New Gradle tasks automatically supported
3. **No maintenance** - Don't need to add methods for every possible task
4. **Type-safe** - Options are type-checked via DSL
5. **Flexible** - Full control over arguments and flags

## Requirements

- Gradle wrapper (`./gradlew`) in project root
- Kite 0.1.0+

## License

Apache License 2.0
