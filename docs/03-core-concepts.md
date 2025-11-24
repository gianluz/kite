# Core Concepts

Learn the fundamental building blocks of Kite: **Segments**, **Rides**, and **Flows**.

## Overview

Kite workflows are built from three core concepts:

1. **Segments** - Reusable units of work (build, test, deploy, etc.)
2. **Rides** - Workflows that compose segments
3. **Flows** - Define execution order (sequential, parallel, or mixed)

## Segments

A **segment** is a discrete unit of work in your CI/CD pipeline. Think of it as a single step like "build the app", "run
tests", or "deploy to staging".

### Defining Segments

Segments are defined in `.kite/segments/*.kite.kts` files:

```kotlin
// .kite/segments/build.kite.kts
segments {
    segment("build") {
        description = "Build the application"
        timeout = 10.minutes
        
        execute {
            exec("./gradlew", "build")
        }
    }
}
```

### Segment Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Unique identifier (required) |
| `description` | String? | Human-readable description |
| `dependsOn` | List<String> | Segments that must run first |
| `condition` | Lambda? | When to execute this segment |
| `timeout` | Duration? | Maximum execution time |
| `maxRetries` | Int | Retry attempts on failure (default: 0) |
| `retryDelay` | Duration | Delay between retries |
| `retryOn` | List<String> | Exception types that trigger retry |
| `inputs` | List<String> | Required input artifacts |
| `outputs` | Map<String, String> | Produced output artifacts |
| `execute` | Lambda | The actual work (required) |

### Dependencies

Segments can depend on other segments:

```kotlin
segment("test") {
    description = "Run tests"
    dependsOn("build")  // Must run after "build"
    
    execute {
        exec("./gradlew", "test")
    }
}

segment("deploy") {
    description = "Deploy to production"
    dependsOn("build", "test")  // Must run after both
    
    execute {
        exec("./deploy.sh", "production")
    }
}
```

### Conditional Execution

Segments can execute based on conditions:

```kotlin
segment("deploy-prod") {
    description = "Deploy to production"
    condition = { ctx ->
        ctx.branch == "main" && ctx.isCI
    }
    
    execute {
        exec("./deploy.sh", "production")
    }
}
```

### Timeouts and Retries

Handle failures gracefully:

```kotlin
segment("flaky-test") {
    description = "Run flaky integration tests"
    timeout = 5.minutes
    maxRetries = 3
    retryDelay = 10.seconds
    retryOn("java.net.SocketTimeoutException", "java.io.IOException")
    
    execute {
        exec("./gradlew", "integrationTest")
    }
}
```

### Lifecycle Hooks

React to segment completion:

```kotlin
segment("notify-tests") {
    execute {
        exec("./gradlew", "test")
    }
    
    onSuccess {
        println("✅ Tests passed!")
        // Send success notification
    }
    
    onFailure { error ->
        println("❌ Tests failed: ${error.message}")
        // Send failure notification
    }
    
    onComplete { status ->
        println("Tests completed with status: $status")
        // Cleanup, logging, etc.
    }
}
```

### Artifacts

Segments can produce and consume artifacts:

```kotlin
segment("build-apk") {
    description = "Build Android APK"
    
    execute {
        exec("./gradlew", "assembleRelease")
    }
    
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")
        artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
    }
}

segment("upload-apk") {
    description = "Upload APK to Play Store"
    dependsOn("build-apk")
    
    inputs {
        artifact("apk")
    }
    
    execute {
        val apkPath = artifacts.get("apk")
        exec("./upload-to-play-store.sh", apkPath.toString())
    }
}
```

See [Artifacts](08-artifacts.md) for more details.

## Rides

A **ride** is a workflow that composes segments into a complete pipeline. Different rides can reuse the same segments
for different scenarios.

### Defining Rides

Rides are defined in `.kite/rides/*.kite.kts` files:

```kotlin
// .kite/rides/ci.kite.kts
ride {
    name = "CI"
    
    flow {
        segment("build")
        segment("test")
    }
}
```

### Ride Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Human-readable name (required) |
| `flow` | FlowNode | Execution flow (required) |
| `environment` | Map<String, String> | Environment variables for all segments |
| `maxConcurrency` | Int? | Max parallel segments (null = unlimited) |
| `onSuccess` | Lambda? | Called when all segments succeed |
| `onFailure` | Lambda? | Called when any segment fails |
| `onComplete` | Lambda? | Called when ride completes |

### Multiple Rides

Create different rides for different scenarios:

```kotlin
// .kite/rides/mr.kite.kts
ride {
    name = "MR Validation"
    maxConcurrency = 3
    
    flow {
        segment("build")
        parallel {
            segment("unit-tests")
            segment("lint")
        }
    }
}

// .kite/rides/release.kite.kts
ride {
    name = "Release"
    
    flow {
        segment("build")
        parallel {
            segment("unit-tests")
            segment("integration-tests")
            segment("lint")
        }
        segment("deploy-prod")
    }
}

// .kite/rides/nightly.kite.kts
ride {
    name = "Nightly"
    
    flow {
        segment("build")
        segment("full-test-suite")
        segment("performance-tests")
        segment("deploy-staging")
    }
}
```

### Environment Variables

Set environment variables for the ride:

```kotlin
ride {
    name = "Staging Deploy"
    
    env("ENVIRONMENT", "staging")
    env("LOG_LEVEL", "debug")
    
    flow {
        segment("deploy")
    }
}
```

### Ride Hooks

React to ride completion:

```kotlin
ride {
    name = "CI"
    
    flow {
        segment("build")
        segment("test")
    }
    
    onSuccess {
        println("🎉 CI passed!")
        // Trigger deployment, send notification, etc.
    }
    
    onFailure { error ->
        println("❌ CI failed: ${error.message}")
        // Send alert, create issue, etc.
    }
    
    onComplete { success ->
        println("CI completed. Success: $success")
        // Report metrics, cleanup, etc.
    }
}
```

## Flows

**Flows** define the execution order of segments in a ride. Kite supports three flow patterns:

### 1. Sequential Flow

Segments execute one after another:

```kotlin
flow {
    segment("clean")
    segment("compile")
    segment("test")
    segment("package")
}
```

**Execution**: clean → compile → test → package

### 2. Parallel Flow

Segments execute concurrently:

```kotlin
flow {
    parallel {
        segment("unit-tests")
        segment("integration-tests")
        segment("lint")
    }
}
```

**Execution**: All three segments run at the same time.

### 3. Mixed Flow

Combine sequential and parallel execution:

```kotlin
flow {
    // Phase 1: Build
    segment("compile")
    
    // Phase 2: Run tests in parallel
    parallel {
        segment("unit-tests")
        segment("integration-tests")
        segment("lint")
        segment("detekt")
    }
    
    // Phase 3: Package (waits for all tests)
    segment("package")
    
    // Phase 4: Deploy (only on main branch)
    segment("deploy")
}
```

**Execution**:

1. compile
2. unit-tests, integration-tests, lint, detekt (parallel)
3. package (waits for all tests)
4. deploy

### Segment Overrides in Rides

Override segment properties within a ride:

```kotlin
flow {
    segment("build")
    
    segment("test") {
        timeout = 15.minutes  // Override default timeout
        dependsOn("lint")     // Add extra dependency
    }
    
    segment("deploy") {
        enabled = false       // Disable this segment
    }
    
    segment("staging-deploy") {
        condition = { ctx -> ctx.branch == "develop" }
    }
}
```

## Execution Context

Every segment has access to an **execution context** providing runtime information:

```kotlin
segment("deploy") {
    execute {
        // Access environment
        val branch = env["BRANCH_NAME"]
        
        // Get secrets (auto-masked in logs)
        val apiKey = requireSecret("API_KEY")
        
        // Check platform
        if (isCI && !isLocal) {
            exec("./deploy.sh")
        }
        
        // Use artifacts
        val apk = artifacts.get("apk")
        
        // Work with files
        writeFile("config.txt", "environment=prod")
        val content = readFile("config.txt")
    }
}
```

### Context Properties

| Property | Type | Description |
|----------|------|-------------|
| `branch` | String | Current Git branch |
| `commitSha` | String | Git commit SHA |
| `mrNumber` | String? | Merge/pull request number |
| `isRelease` | Boolean | Is this a release build? |
| `isLocal` | Boolean | Running locally? |
| `isCI` | Boolean | Running in CI? |
| `isMergeRequest` | Boolean | Is this an MR/PR? |
| `ciPlatform` | CIPlatform | Platform (GitLab, GitHub, Local, Generic) |
| `environment` | Map | Environment variables |
| `workspace` | Path | Workspace root directory |
| `artifacts` | ArtifactManager | Artifact manager |

### Context Functions

| Function | Description |
|----------|-------------|
| `env(key)` | Get environment variable (nullable) |
| `requireEnv(key)` | Get environment variable (throws if missing) |
| `envOrDefault(key, default)` | Get env var with default |
| `secret(key)` | Get secret (auto-masked in logs) |
| `requireSecret(key)` | Get secret (throws if missing) |
| `exec(command, *args)` | Execute command |
| `shell(command)` | Execute shell command |
| `readFile(path)` | Read file contents |
| `writeFile(path, content)` | Write file |
| `artifacts.get(name)` | Get artifact by name |

See [Execution Context](06-execution-context.md) for complete API.

## File Organization

### Recommended Structure

```
your-project/
├── .kite/
│   ├── segments/
│   │   ├── build.kite.kts       # Build segments
│   │   ├── test.kite.kts        # Test segments
│   │   ├── deploy.kite.kts      # Deployment segments
│   │   └── android/
│   │       ├── lint.kite.kts    # Android-specific segments
│   │       └── assemble.kite.kts
│   └── rides/
│       ├── ci.kite.kts          # CI workflow
│       ├── mr.kite.kts          # MR validation
│       ├── release.kite.kts     # Release workflow
│       └── nightly.kite.kts     # Nightly builds
└── build.gradle.kts
```

### Discovery

Kite automatically discovers:

- All `.kite.kts` files in `.kite/segments/` (recursively)
- All `.kite.kts` files in `.kite/rides/` (recursively)

You can organize files however you like!

## Real-World Example

Here's a complete workflow for an Android app:

### Segments

```kotlin
// .kite/segments/android.kite.kts
segments {
    segment("compile") {
        description = "Compile Kotlin code"
        execute {
            exec("./gradlew", "compileDebugKotlin")
        }
    }
    
    segment("lint") {
        description = "Run Android lint"
        dependsOn("compile")
        execute {
            exec("./gradlew", "lintDebug")
        }
    }
    
    segment("unit-tests") {
        description = "Run unit tests"
        dependsOn("compile")
        timeout = 10.minutes
        execute {
            exec("./gradlew", "testDebugUnitTest")
        }
    }
    
    segment("assemble-apk") {
        description = "Build APK"
        dependsOn("lint", "unit-tests")
        execute {
            exec("./gradlew", "assembleDebug")
        }
        outputs {
            artifact("apk", "app/build/outputs/apk/debug/app-debug.apk")
        }
    }
}
```

### Ride

```kotlin
// .kite/rides/ci.kite.kts
ride {
    name = "CI"
    maxConcurrency = 2
    
    flow {
        segment("compile")
        
        parallel {
            segment("lint")
            segment("unit-tests")
        }
        
        segment("assemble-apk")
    }
    
    onSuccess {
        println("✅ CI passed! APK ready for testing.")
    }
}
```

### Run It

```bash
kite ride CI
```

## Next Steps

- **[Writing Segments](04-writing-segments.md)** - Master segment authoring
- **[Writing Rides](05-writing-rides.md)** - Create complex workflows
- **[Execution Context](06-execution-context.md)** - Use the full context API
- **[Parallel Execution](07-parallel-execution.md)** - Optimize with parallelism
- **[Artifacts](08-artifacts.md)** - Share data between segments

---

**You now understand the core concepts!** 🎯 Ready to build powerful CI/CD workflows.
