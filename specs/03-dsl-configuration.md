# DSL & Configuration

## Kotlin DSL

Kite uses a Kotlin-based DSL (Domain-Specific Language) for defining segments and rides. This provides type safety, IDE
support, and the full power of Kotlin.

## Segment Definition DSL

### Basic Segment

```kotlin
// .kite/segments/build.kite.kts
import io.kite.dsl.*

segments {
    segment("build") {
        description = "Build the application"
        
        execute {
            exec("./gradlew", "assembleRelease")
        }
    }
}
```

### Segment with Outputs

```kotlin
segment("build") {
    description = "Build release APK"
    
    execute {
        exec("./gradlew", "assembleRelease")
    }
    
    outputs {
        artifact("apk", file("app/build/outputs/apk/release/app-release.apk"))
        artifact("mapping", file("app/build/outputs/mapping/release/mapping.txt"))
    }
}
```

### Segment with Dependencies

```kotlin
segment("deploy") {
    description = "Deploy to Play Store"
    dependsOn = listOf("build", "unitTest")
    
    execute {
        val apk = artifacts.get("apk")
        uploadToPlayStore(apk)
    }
}
```

### Segment with Timeout

```kotlin
segment("integrationTest") {
    description = "Run integration tests"
    timeout = env("CI_TIMEOUT")?.toIntOrNull()?.minutes ?: 30.minutes
    
    onTimeout {
        println("Integration tests timed out, cleaning up...")
        exec("docker-compose", "down")
    }
    
    execute {
        exec("./gradlew", "connectedAndroidTest")
    }
}
```

### Segment with Retries

```kotlin
segment("flaky") {
    description = "Flaky test suite"
    maxRetries = 3
    retryDelay = 5.seconds
    retryOn = listOf(IOException::class, TimeoutException::class)
    
    execute {
        exec("./run-flaky-tests.sh")
    }
    
    onFailure { error ->
        println("Segment failed: ${error.message}")
        notifySlack("Test failed: ${error.message}")
    }
}
```

### Conditional Segment

```kotlin
segment("deployProduction") {
    description = "Deploy to production"
    condition = { 
        context.branch == "main" && 
        context.isRelease &&
        !context.isLocal
    }
    
    execute {
        exec("./deploy.sh", "production")
    }
}
```

## Ride Definition DSL

### Basic Ride

```kotlin
// .kite/rides/mr.kite.kts
import io.kite.dsl.*

ride {
    name = "MR Ride"
    
    flow {
        segment("build")
        segment("test")
    }
}
```

### Ride with Parallel Execution

```kotlin
ride {
    name = "MR Ride"
    
    flow {
        // Build sequentially
        segment("build")
        
        // Run tests in parallel
        parallel {
            segment("unitTest")
            segment("roborazzi")
            segment("lint")
        }
    }
}
```

### Ride with Environment Variables

```kotlin
ride {
    name = "Release Ride"
    
    environment {
        put("GRADLE_OPTS", "-Xmx4g -XX:MaxMetaspaceSize=512m")
        put("ANDROID_SDK_ROOT", "/opt/android-sdk")
    }
    
    flow {
        segment("build")
        parallel {
            segment("unitTest")
            segment("integrationTest")
        }
    }
}
```

### Ride with Concurrency Limits

```kotlin
ride {
    name = "Resource-Limited Ride"
    
    parallel {
        maxConcurrency = 2  // Max 2 parallel segments
    }
    
    flow {
        segment("build")
        
        parallel {
            segment("test1")  // Will run
            segment("test2")  // Will run
            segment("test3")  // Will wait
        }
    }
}
```

### Ride with Failure Handling

```kotlin
ride {
    name = "Production Deployment"
    
    flow {
        segment("build")
        segment("test")
        segment("deploy")
    }
    
    onFailure { error ->
        notifySlack(
            webhook = env("SLACK_WEBHOOK")!!,
            message = "Deployment failed: ${error.message}"
        )
        
        // Rollback
        exec("./rollback.sh")
    }
}
```

### Ride with Segment Overrides

```kotlin
ride {
    name = "Custom Ride"
    
    flow {
        segment("build")
        
        segment("test") {
            // Override timeout for this ride
            timeout = 60.minutes
            
            // Add extra dependencies
            dependsOn = listOf("build", "lint")
        }
        
        segment("deploy")
    }
}
```

## Settings DSL

### Global Settings

```kotlin
// .kite/settings.kite.kts
import io.kite.dsl.*

settings {
    // Directories
    segmentDirectory = ".kite/segments"
    rideDirectory = ".kite/rides"
    
    // Default parallelism
    parallel {
        maxConcurrency = Runtime.getRuntime().availableProcessors()
    }
    
    // Global environment
    environment {
        put("CI", "true")
        put("GRADLE_OPTS", "-Xmx2g")
    }
    
    // Default timeout
    defaultTimeout = 30.minutes
}
```

### Plugin Configuration

```kotlin
settings {
    plugins {
        plugin("io.kite.plugins:playstore:1.0.0") {
            serviceAccountJson = env("GOOGLE_SERVICE_ACCOUNT_JSON")
            packageName = "com.example.app"
        }
        
        plugin("io.kite.plugins:slack:1.0.0") {
            webhook = env("SLACK_WEBHOOK")
            channel = "#ci-notifications"
        }
    }
}
```

## Helper Functions

### Command Execution

```kotlin
// Execute command and throw on failure
exec("./gradlew", "build")

// Execute and return null on failure
val result = execOrNull("./test.sh")

// Execute shell command
shell("find . -name '*.kt' | wc -l")

// Execute with working directory
exec("npm", "install", workingDir = file("frontend"))

// Execute with environment variables
exec("./script.sh", env = mapOf("ENV" to "production"))
```

### File Operations

```kotlin
// Copy files
copy(source = file("app/build/outputs/apk"), destination = file("artifacts/"))

// Move files
move(source = file("temp/report.html"), destination = file("reports/"))

// Delete files
delete(file("build/"))

// Create directory
createDirectory(file("artifacts/reports"))

// Zip files
zipFiles(source = file("reports/"), destination = file("reports.zip"))

// Unzip files
unzipFiles(source = file("artifacts.zip"), destination = file("extracted/"))
```

### Environment Access

```kotlin
// Read environment variable
val apiKey = env("API_KEY")

// Read with default
val timeout = env("TIMEOUT")?.toIntOrNull() ?: 30

// Check if variable exists
if (envExists("CI")) {
    println("Running in CI")
}
```

### Artifact Management

```kotlin
// Store artifact
artifacts.put("apk", file("app/build/outputs/apk/release/app-release.apk"))

// Retrieve artifact
val apk = artifacts.get("apk")

// Check if artifact exists
if (artifacts.has("apk")) {
    val apk = artifacts.get("apk")
}

// List all artifacts
val allArtifacts = artifacts.list()
```

### Context Access

```kotlin
segment("contextAware") {
    execute {
        println("Branch: ${context.branch}")
        println("Commit: ${context.commitSha}")
        println("CI Platform: ${context.ciPlatform}")
        println("Is Local: ${context.isLocal}")
        println("Is Release: ${context.isRelease}")
        println("MR Number: ${context.mrNumber}")
    }
}
```

## Type-Safe Duration

```kotlin
segment("timeout") {
    timeout = 30.minutes
    retryDelay = 5.seconds
    
    execute {
        // ...
    }
}

// Available units
val t1 = 1.seconds
val t2 = 5.minutes
val t3 = 2.hours
```

## File Handling

```kotlin
segment("fileHandling") {
    execute {
        // File references
        val apk = file("app/build/outputs/apk/release/app-release.apk")
        
        // Check existence
        if (apk.exists()) {
            println("APK exists at ${apk.absolutePath}")
        }
        
        // Read file
        val content = apk.readText()
        
        // Write file
        file("output.txt").writeText("Hello, Kite!")
    }
}
```

## Logging

```kotlin
segment("logging") {
    execute {
        // Info logging
        log.info("Starting build")
        
        // Debug logging
        log.debug("Configuration loaded")
        
        // Warning
        log.warn("Using default configuration")
        
        // Error
        log.error("Build failed", exception)
    }
}
```

## DSL Design Principles

1. **Type Safety**: Leverage Kotlin's type system
2. **IDE Support**: Full autocomplete and refactoring
3. **Composability**: Functions compose naturally
4. **Readability**: Natural language-like syntax
5. **Extensibility**: Easy to add custom helpers
6. **Kotlin Idioms**: Follow Kotlin best practices

## Import Organization

```kotlin
// Minimal imports for most use cases
import io.kite.dsl.*

// Specific imports when needed
import io.kite.dsl.segments
import io.kite.dsl.ride
import io.kite.dsl.settings
import io.kite.core.ExecutionContext
import io.kite.core.CIPlatform
```

## Summary

- **Segments DSL**: Define reusable work units with `segment { }`
- **Rides DSL**: Compose workflows with `ride { }` and `flow { }`
- **Settings DSL**: Configure global options with `settings { }`
- **Helpers**: Rich set of helper functions for common operations
- **Type Safety**: Full Kotlin type system support
- **IDE Support**: Autocomplete, refactoring, and navigation
- **Extensibility**: Easy to add custom helpers and plugins
