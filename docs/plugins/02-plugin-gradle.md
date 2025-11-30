# Gradle Plugin

Flexible Gradle task execution for Kite workflows.

## Overview

The Gradle plugin provides a type-safe DSL for executing Gradle tasks with full support for:

- ✅ Standard Java/Kotlin projects
- ✅ Android projects (AGP tasks)
- ✅ Multi-module builds
- ✅ Custom Gradle plugins
- ✅ All Gradle command-line options

**Design Philosophy:** One flexible `task()` method that works with **any** Gradle setup, plus optional convenience
methods for common operations.

## Installation

```kotlin
@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")

import io.kite.plugins.gradle.*
```

## Quick Start

```kotlin
segments {
    segment("build") {
        execute {
            gradle {
                // Execute any Gradle task
                task("build")
                
                // Multiple tasks
                task("clean", "build", "test")
                
                // With options
                task("build") {
                    parallel = true
                    stacktrace = true
                    property("version", "1.0.0")
                }
            }
        }
    }
}
```

## Core API

### `task()` - Execute Any Task

The main method that works with **all** Gradle setups:

```kotlin
gradle {
    // Single task
    task("build")
    
    // Multiple tasks
    task("clean", "build", "test")
    
    // With options
    task("build") {
        parallel = true
        daemon = false
        stacktrace = true
        info = true
        continueOnFailure = true
        offline = true
        refreshDependencies = true
        
        // Gradle properties (-P)
        property("version", "1.0.0")
        property("env", "production")
        
        // System properties (-D)
        systemProperty("file.encoding", "UTF-8")
        
        // Custom arguments
        arg("--scan")
        arg("--warning-mode=all")
    }
}
```

### Available Options

| Option | Type | Default | Description | Gradle Flag |
|--------|------|---------|-------------|-------------|
| `parallel` | Boolean | `false` | Enable parallel execution | `--parallel` |
| `daemon` | Boolean | `true` | Use Gradle daemon | `--no-daemon` (if false) |
| `stacktrace` | Boolean | `false` | Show full stacktraces | `--stacktrace` |
| `info` | Boolean | `false` | Set log level to INFO | `--info` |
| `debug` | Boolean | `false` | Set log level to DEBUG | `--debug` |
| `continueOnFailure` | Boolean | `false` | Continue after test failures | `--continue` |
| `offline` | Boolean | `false` | Execute build offline | `--offline` |
| `refreshDependencies` | Boolean | `false` | Refresh dependencies | `--refresh-dependencies` |

## Convenience Methods

Optional shortcuts for common tasks:

```kotlin
gradle {
    // Equivalent to task("build")
    build()
    
    // Equivalent to task("clean")
    clean()
    
    // Equivalent to task("test")
    test()
    
    // All support options
    build {
        parallel = true
        stacktrace = true
    }
}
```

## Standard Java/Kotlin Projects

### Basic Build

```kotlin
gradle {
    clean()
    build()
}
```

### Build with Tests

```kotlin
gradle {
    task("clean", "build", "test") {
        parallel = true
        continueOnFailure = true  // Continue if tests fail
    }
}
```

### Publishing

```kotlin
gradle {
    task("clean", "build", "publishToMavenLocal") {
        property("version", "1.0.0")
    }
}
```

### Dependency Analysis

```kotlin
gradle {
    // View dependency tree
    task("dependencies")
    
    // Check for updates (requires plugin)
    task("dependencyUpdates") {
        property("revision", "release")
    }
}
```

## Android Projects

### Build Variants

```kotlin
gradle {
    // Debug build
    task(":app:assembleDebug")
    
    // Release build
    task(":app:assembleRelease")
    
    // Bundle for Play Store
    task(":app:bundleRelease")
}
```

### With Signing Configuration

```kotlin
gradle {
    task(":app:bundleRelease") {
        property("android.injected.signing.store.file", "/path/to/keystore.jks")
        property("android.injected.signing.store.password", requireSecret("KEYSTORE_PASSWORD"))
        property("android.injected.signing.key.alias", "release")
        property("android.injected.signing.key.password", requireSecret("KEY_PASSWORD"))
    }
}
```

### Testing

```kotlin
gradle {
    // Unit tests
    task(":app:testDebugUnitTest")
    
    // Instrumentation tests
    task(":app:connectedAndroidTest")
    
    // Lint checks
    task(":app:lintDebug")
}
```

### Multiple Flavors

```kotlin
gradle {
    // Build all flavors
    task(":app:assembleProdRelease", ":app:assembleStagingRelease") {
        parallel = true
    }
}
```

## Multi-Module Projects

### Build Specific Modules

```kotlin
gradle {
    task(":core:build", ":api:build", ":app:build") {
        parallel = true
    }
}
```

### Clean and Build All

```kotlin
gradle {
    clean()
    build {
        parallel = true
        stacktrace = true
    }
}
```

### Module-Specific Tasks

```kotlin
gradle {
    // Test only core module
    task(":core:test")
    
    // Publish only API module
    task(":api:publishToMavenLocal") {
        property("version", "1.0.0")
    }
}
```

## CI/CD Examples

### Basic CI Build

```kotlin
segment("ci-build") {
    execute {
        gradle {
            task("clean", "build", "test") {
                parallel = true
                daemon = false  // CI environments often disable daemon
                stacktrace = true
                continueOnFailure = false  // Fail fast in CI
            }
        }
    }
}
```

### Release Workflow

```kotlin
segment("release") {
    execute {
        val version = requireEnv("RELEASE_VERSION")
        
        gradle {
            clean()
            
            task("build", "test") {
                parallel = true
                property("version", version)
            }
            
            task("publishToSonatype", "closeAndReleaseSonatypeStagingRepository") {
                property("version", version)
            }
        }
    }
}
```

### Android CI/CD

```kotlin
segment("android-release") {
    execute {
        val versionCode = requireEnv("VERSION_CODE")
        val versionName = requireEnv("VERSION_NAME")
        
        gradle {
            clean()
            
            // Lint checks
            task(":app:lintRelease") {
                arg("--continue")  // Continue if lint warnings
            }
            
            // Unit tests
            task(":app:testReleaseUnitTest")
            
            // Build release bundle
            task(":app:bundleRelease") {
                property("versionCode", versionCode)
                property("versionName", versionName)
                property("android.injected.signing.store.file", requireSecret("KEYSTORE_PATH"))
                property("android.injected.signing.store.password", requireSecret("KEYSTORE_PASSWORD"))
                property("android.injected.signing.key.alias", "release")
                property("android.injected.signing.key.password", requireSecret("KEY_PASSWORD"))
            }
        }
    }
}
```

## Complete Example

### Full Build + Release Pipeline

```kotlin
@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")
@file:DependsOn("com.gianluz.kite:git:0.1.0-alpha")

import io.kite.plugins.gradle.*
import io.kite.plugins.git.*

segments {
    segment("prepare") {
        execute {
            git {
                fetch()
                checkout("main")
                pull(rebase = true)
            }
        }
    }
    
    segment("build") {
        dependsOn("prepare")
        execute {
            gradle {
                clean()
                
                task("build", "test") {
                    parallel = true
                    stacktrace = true
                    continueOnFailure = false
                }
            }
        }
    }
    
    segment("publish") {
        dependsOn("build")
        condition { isCI }
        execute {
            val version = requireEnv("VERSION")
            
            gradle {
                task("publishToMavenLocal") {
                    property("version", version)
                }
            }
            
            git {
                tag("v$version", message = "Release $version")
                push(branch = "main")
                push(tags = true)
            }
        }
    }
}
```

## Custom Gradle Plugins

The plugin works seamlessly with any custom Gradle plugin:

```kotlin
gradle {
    // Detekt (Kotlin linting)
    task("detekt")
    
    // ktlint
    task("ktlintCheck", "ktlintFormat")
    
    // Dependency updates
    task("dependencyUpdates") {
        property("revision", "release")
        property("outputFormatter", "json")
    }
    
    // SonarQube
    task("sonar") {
        property("sonar.projectKey", "my-project")
        property("sonar.host.url", "https://sonarcloud.io")
    }
    
    // Dokka (Kotlin docs)
    task("dokkaHtml")
}
```

## Advanced Configuration

### Gradle Wrapper Version

```kotlin
gradle {
    // Update Gradle wrapper
    task("wrapper") {
        arg("--gradle-version=8.5")
    }
}
```

### Build Scans

```kotlin
gradle {
    task("build") {
        arg("--scan")
        property("gradle.scan.termsOfServiceUrl", "https://gradle.com/terms-of-service")
        property("gradle.scan.termsOfServiceAgree", "yes")
    }
}
```

### JVM Arguments

```kotlin
gradle {
    task("test") {
        systemProperty("file.encoding", "UTF-8")
        systemProperty("user.timezone", "UTC")
        arg("-Xmx4g")
    }
}
```

## Troubleshooting

### Gradle Wrapper Not Found

**Problem:** `./gradlew: No such file or directory`

**Solution:** Ensure you're running from project root or the wrapper exists:

```kotlin
gradle {
    task("wrapper") {
        arg("--gradle-version=8.5")
    }
}
```

### Build Cache Issues

**Problem:** Stale build artifacts

**Solution:** Use `clean` and `refreshDependencies`:

```kotlin
gradle {
    clean()
    task("build") {
        refreshDependencies = true
    }
}
```

### Android Signing Errors

**Problem:** Missing signing configuration

**Solution:** Verify all signing properties are set:

```kotlin
gradle {
    task(":app:bundleRelease") {
        property("android.injected.signing.store.file", requireSecret("KEYSTORE_PATH"))
        property("android.injected.signing.store.password", requireSecret("KEYSTORE_PASSWORD"))
        property("android.injected.signing.key.alias", requireSecret("KEY_ALIAS"))
        property("android.injected.signing.key.password", requireSecret("KEY_PASSWORD"))
    }
}
```

## API Reference

### GradlePlugin

```kotlin
class GradlePlugin(private val ctx: ExecutionContext) {
    suspend fun task(vararg tasks: String, options: GradleOptions.() -> Unit = {})
    suspend fun build(options: GradleOptions.() -> Unit = {})
    suspend fun clean(options: GradleOptions.() -> Unit = {})
    suspend fun test(options: GradleOptions.() -> Unit = {})
}
```

### GradleOptions

```kotlin
data class GradleOptions(
    var parallel: Boolean = false,
    var daemon: Boolean = true,
    var stacktrace: Boolean = false,
    var info: Boolean = false,
    var debug: Boolean = false,
    var continueOnFailure: Boolean = false,
    var offline: Boolean = false,
    var refreshDependencies: Boolean = false,
) {
    fun property(key: String, value: String)
    fun systemProperty(key: String, value: String)
    fun arg(argument: String)
}
```

### Extension Function

```kotlin
suspend fun ExecutionContext.gradle(configure: suspend GradlePlugin.() -> Unit)
```

## See Also

- [Gradle Documentation](https://docs.gradle.org/)
- [Android Gradle Plugin](https://developer.android.com/studio/build)
- [Plugin Development Guide](../dev/05-plugin-development.md)
- [Git Plugin](01-plugin-git.md)
