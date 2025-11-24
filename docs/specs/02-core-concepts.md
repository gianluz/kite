# Core Concepts

> **⚠️ Deprecation Notice**: This document references deprecated properties like `mrNumber`, `isRelease`, and
`ciPlatform`.
> These are deprecated in favor of Kite's platform-agnostic design. See
> the [Platform-Agnostic Design](../dev/platform-agnostic-design.md)
> document for the recommended approach. Users should check environment variables directly using `env()`.

## Segments

A **segment** is the fundamental unit of work in Kite. It represents a single, discrete step in your CI/CD process.

### Characteristics

- **Self-contained**: Each segment performs one specific operation
- **Reusable**: Segments can be used in multiple rides
- **Testable**: Segments can be unit tested
- **Composable**: Segments can depend on other segments
- **Conditional**: Segments can execute conditionally based on context

### Segment Definition

Segments are defined in `.kite.kts` files under `.kite/segments/`:

```kotlin
// .kite/segments/build.kite.kts
segments {
    segment("build") {
        description = "Build the Android application"
        
        execute {
            exec("./gradlew", "assembleRelease")
        }
        
        outputs {
            artifact("apk", file("app/build/outputs/apk/release/app-release.apk"))
        }
    }
    
    segment("buildDebug") {
        description = "Build debug variant"
        
        execute {
            exec("./gradlew", "assembleDebug")
        }
    }
}
```

### Segment Properties

- **name**: Unique identifier (required)
- **description**: Human-readable description
- **dependsOn**: List of segment names that must execute first
- **inputs**: Files or artifacts required by this segment
- **outputs**: Files or artifacts produced by this segment
- **condition**: Lambda determining if segment should execute
- **execute**: The actual work to perform
- **onSuccess**: Callback invoked after successful execution
- **onFailure**: Callback invoked after failure
- **onComplete**: Callback invoked regardless of outcome

> **Note:** The DSL also supports `timeout`, `maxRetries`, `retryDelay`, and `retryOn` properties, but these are not yet
> enforced by the runtime. They are planned for a future release.

## Rides

A **ride** is a configured sequence of segments that defines a complete CI/CD workflow.

### Characteristics

- **Composable**: Combine any segments in any order
- **Scenario-specific**: Different rides for different scenarios (MR, release, nightly)
- **Declarative**: Define what should happen, Kite figures out how
- **Parallelizable**: Define which segments can run in parallel

### Ride Definition

Rides are defined in `.kite.kts` files under `.kite/rides/`:

```kotlin
// .kite/rides/mr.kite.kts
ride {
    name = "Ordinary MR Ride"
    
    flow {
        // Sequential: build first
        segment("build")
        
        // Parallel: run tests in parallel after build
        parallel {
            segment("unitTest")
            segment("roborazzi")
            segment("robolectric")
        }
    }
}
```

### Flow Control

Rides define the **flow** - how segments are executed:

#### Sequential Execution

```kotlin
flow {
    segment("build")
    segment("test")
    segment("deploy")
}
```

Segments execute one after another.

#### Parallel Execution

```kotlin
flow {
    segment("build")
    
    parallel {
        segment("unitTest")
        segment("integrationTest")
        segment("lint")
    }
}
```

Segments in a `parallel` block execute concurrently.

#### Mixed Flow

```kotlin
flow {
    // Phase 1: Build
    segment("build")
    
    // Phase 2: Parallel tests
    parallel {
        segment("unitTest")
        segment("lint")
    }
    
    // Phase 3: Deploy (waits for all tests)
    segment("deploy")
}
```

#### Dependencies

```kotlin
flow {
    segment("build")
    
    parallel {
        segment("unitTest") {
            dependsOn = listOf("build")
        }
        
        segment("integrationTest") {
            dependsOn = listOf("build", "unitTest")
        }
    }
}
```

Dependencies are automatically resolved.

## File Structure

### Recommended Organization

```
project-root/
├── .kite/
│   ├── segments/
│   │   ├── build.kite.kts        # Build-related segments
│   │   ├── test.kite.kts         # Test-related segments
│   │   ├── deploy.kite.kts       # Deployment segments
│   │   └── common.kite.kts       # Shared/utility segments
│   ├── rides/
│   │   ├── mr.kite.kts           # Ordinary MR ride
│   │   ├── release.kite.kts      # Release MR ride
│   │   ├── nightly.kite.kts      # Nightly build ride
│   │   └── local.kite.kts        # Local development ride
│   └── settings.kite.kts         # Global settings (optional)
└── kite                          # Kite executable
```

### File Types

#### Segment Files: `.kite/segments/*.kite.kts`

Define reusable segments:

```kotlin
segments {
    segment("segmentName") {
        // Segment configuration
    }
}
```

#### Ride Files: `.kite/rides/*.kite.kts`

Define ride configurations:

```kotlin
ride {
    name = "Ride Name"
    
    flow {
        // Segment references and flow control
    }
}
```

#### Settings File: `.kite/settings.kite.kts`

Global configuration (optional):

```kotlin
settings {
    parallel {
        maxConcurrency = 4
    }
    
    environment {
        put("GRADLE_OPTS", "-Xmx4g")
    }
}
```

## Execution Context

Every segment has access to an **execution context** providing runtime information:

```kotlin
segment("deploy") {
    execute {
        if (context.branch == "main" && !context.isLocal) {
            // Deploy to production
            exec("./deploy.sh", "production")
        } else {
            println("Skipping deployment for branch ${context.branch}")
        }
    }
}
```

### Context Properties

```kotlin
data class ExecutionContext(
    val branch: String,              // Current git branch
    val commitSha: String,           // Git commit SHA
    val mrNumber: String?,           // MR/PR number (if applicable)
    val isRelease: Boolean,          // Is this a release MR?
    val isLocal: Boolean,            // Running locally vs CI
    val ciPlatform: CIPlatform,      // GitLab, GitHub, Local, etc.
    val environment: Map<String, String>,  // Environment variables
    val workspace: Path,             // Workspace directory
    val artifacts: ArtifactManager   // Artifact management
)
```

## Artifacts

**Artifacts** are outputs from segments that can be consumed by other segments:

### Producing Artifacts

```kotlin
segment("build") {
    execute {
        exec("./gradlew", "assembleRelease")
    }
    
    outputs {
        artifact("apk", file("app/build/outputs/apk/release/app-release.apk"))
        artifact("mapping", file("app/build/outputs/mapping/release/mapping.txt"))
    }
}
```

### Consuming Artifacts

```kotlin
segment("deploy") {
    dependsOn = listOf("build")
    
    execute {
        val apk = artifacts.get("apk")
        uploadToPlayStore(apk)
    }
}
```

## Multi-Module Projects

For projects with multiple modules, use namespacing:

```kotlin
// .kite/segments/app.kite.kts
module("app") {
    segment("build") {
        execute { exec("./gradlew", ":app:assembleRelease") }
    }
    
    segment("test") {
        execute { exec("./gradlew", ":app:testReleaseUnitTest") }
    }
}

// .kite/segments/library.kite.kts
module("library") {
    segment("build") {
        execute { exec("./gradlew", ":library:assembleRelease") }
    }
    
    segment("test") {
        execute { exec("./gradlew", ":library:testReleaseUnitTest") }
    }
}
```

### Using Module Segments

```kotlin
// .kite/rides/full.kite.kts
ride {
    name = "Full Build"
    
    flow {
        // Build all modules in parallel
        parallel {
            segment("app:build")
            segment("library:build")
        }
        
        // Test all modules in parallel
        parallel {
            segment("app:test")
            segment("library:test")
        }
    }
}
```

## Conditional Execution

Segments can execute conditionally:

```kotlin
segment("deploy") {
    condition = { 
        context.branch == "main" && context.isCI
    }

    execute {
        // Deploy logic
    }
}
```

Or within the ride:

```kotlin
ride {
    flow {
        segment("build")

        segment("integrationTest") {
            condition = { context ->
                // Check for release based on your convention
                context.env("CI_MERGE_REQUEST_LABELS")?.contains("release") == true
            }
        }
        
        segment("deploy") {
            condition = { context.branch == "main" }
        }
    }
}
```

## Summary

- **Segments** are reusable units of work defined in `.kite/segments/*.kite.kts`
- **Rides** compose segments into workflows defined in `.kite/rides/*.kite.kts`
- **Flow** controls execution order (sequential, parallel, dependencies)
- **Context** provides runtime information to segments
- **Artifacts** enable data sharing between segments
- **Modules** support multi-module project organization
- **Conditions** enable smart, context-aware execution
