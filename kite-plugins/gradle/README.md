# Kite Gradle Plugin

Type-safe Gradle operations for Kite workflows.

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

## Usage

### Basic Build

```kotlin
segments {
    segment("build") {
        execute {
            gradle {
                build()
            }
        }
    }
}
```

### Build Operations

```kotlin
gradle {
    // Simple build
    build()
    
    // Custom tasks
    build(tasks = listOf("clean", "build"))
    
    // Parallel build
    build(parallel = true)
    
    // Without daemon
    build(daemon = false)
    
    // With stacktrace
    build(stacktrace = true)
    
    // With properties
    build(properties = mapOf(
        "version" to "1.0.0",
        "env" to "production"
    ))
}
```

### Test Operations

```kotlin
gradle {
    // Run all tests
    test()
    
    // Parallel tests
    test(parallel = true)
    
    // Continue after failures
    test(continueAfterFailure = true)
    
    // Run specific tests
    test(testNamePattern = "*IntegrationTest")
    test(testNamePattern = "com.example.UserServiceTest")
}
```

### Clean and Assemble

```kotlin
gradle {
    // Clean build artifacts
    clean()
    
    // Assemble without tests
    assemble()
    
    // Clean and build
    clean()
    build()
}
```

### Publishing

```kotlin
gradle {
    // Publish to Maven Local
    publishToMavenLocal()
    
    // Publish to all configured repositories
    publish()
    
    // Publish to specific repository
    publish(repository = "MavenCentral")
    publish(repository = "GitHubPackages")
}
```

### Dependency Management

```kotlin
gradle {
    // View dependencies
    dependencies()
    
    // Check for updates
    dependencyUpdates()
}
```

### Custom Tasks

```kotlin
gradle {
    // Run custom tasks
    tasks("customTask")
    tasks("task1", "task2", "task3")
    
    // With additional arguments
    tasks("myTask", args = listOf("--info", "--no-daemon"))
}
```

### Wrapper Management

```kotlin
gradle {
    // Update Gradle wrapper
    wrapper("8.5")
}
```

## Complete Examples

### CI Build Pipeline

```kotlin
segments {
    segment("ci-build") {
        description = "Complete CI build pipeline"
        execute {
            gradle {
                // Clean previous artifacts
                clean()
                
                // Build with tests
                build(
                    parallel = true,
                    stacktrace = true
                )
                
                // Publish to Maven Local for integration tests
                publishToMavenLocal()
            }
        }
    }
}
```

### Release Workflow

```kotlin
segments {
    segment("release") {
        description = "Build and publish release"
        execute {
            val version = env("VERSION") ?: error("VERSION not set")
            
            gradle {
                // Clean and build
                clean()
                build(
                    properties = mapOf("version" to version),
                    parallel = true
                )
                
                // Publish to Maven Central
                publish(repository = "MavenCentral")
            }
            
            logger.info("✅ Released version $version")
        }
    }
}
```

### Multi-Module Build

```kotlin
segments {
    segment("build-all") {
        execute {
            gradle {
                // Build all modules in parallel
                build(
                    tasks = listOf(
                        ":module1:build",
                        ":module2:build",
                        ":module3:build"
                    ),
                    parallel = true
                )
            }
        }
    }
}
```

### Test with Coverage

```kotlin
segments {
    segment("test-coverage") {
        execute {
            gradle {
                // Run tests with coverage
                tasks(
                    "test",
                    "jacocoTestReport",
                    args = listOf("--continue")
                )
            }
        }
    }
}
```

### Dependency Analysis

```kotlin
segments {
    segment("analyze-dependencies") {
        execute {
            gradle {
                // Check dependencies
                dependencies()
                
                // Check for updates
                dependencyUpdates()
            }
        }
    }
}
```

### Docker Build with Gradle

```kotlin
segments {
    segment("docker-build") {
        execute {
            gradle {
                // Build application
                build(tasks = listOf("clean", "bootJar"))
            }
            
            // Build Docker image (assuming jib plugin)
            gradle {
                tasks("jib", args = listOf("--image=myapp:latest"))
            }
        }
    }
}
```

### Conditional Build

```kotlin
segments {
    segment("build-snapshot") {
        condition { ctx ->
            // Only for snapshot versions
            ctx.env("VERSION")?.endsWith("-SNAPSHOT") == true
        }
        
        execute {
            gradle {
                build(properties = mapOf(
                    "version" to env("VERSION")!!
                ))
                
                publish(repository = "Snapshots")
            }
        }
    }
}
```

## API Reference

### Build Operations

- `build(tasks, parallel, daemon, stacktrace, properties)` - Build the project
- `clean()` - Clean build artifacts
- `assemble()` - Assemble artifacts without tests

### Test Operations

- `test(parallel, continueAfterFailure, testNamePattern)` - Run tests

### Publishing Operations

- `publishToMavenLocal()` - Publish to Maven Local
- `publish(repository)` - Publish to remote repository

### Dependency Operations

- `dependencies()` - Generate dependency report
- `dependencyUpdates()` - Check for dependency updates

### Task Execution

- `tasks(vararg tasks, args)` - Run custom Gradle tasks

### Wrapper Operations

- `wrapper(version)` - Update Gradle wrapper

## Requirements

- Gradle wrapper (`./gradlew`) in project root
- Kite 0.1.0+

## Tips

### Using with Environment Variables

```kotlin
gradle {
    build(properties = mapOf(
        "mavenUser" to requireSecret("MAVEN_USER"),
        "mavenPass" to requireSecret("MAVEN_PASS")
    ))
}
```

### Parallel Builds

```kotlin
gradle {
    // Enable parallel execution for faster builds
    build(
        tasks = listOf("clean", "build"),
        parallel = true
    )
}
```

### Debugging Build Issues

```kotlin
gradle {
    build(
        stacktrace = true,  // Show full stacktrace
        daemon = false      // Disable daemon for clean state
    )
}
```

## License

Apache License 2.0
