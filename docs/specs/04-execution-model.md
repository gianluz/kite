# Execution Model

> **⚠️ Deprecation Notice**: This document references deprecated properties like `mrNumber`, `isRelease`, and `ciPlatform`.
> These are deprecated in favor of Kite's platform-agnostic design. See [Execution Context - Platform-Specific Environment Variables](../06-execution-context.md#platform-specific-environment-variables)
> document for the recommended approach. Users should check environment variables directly using `env()`.


## Overview

Kite's execution model defines how segments are scheduled, executed, and managed. The execution engine builds a directed
acyclic graph (DAG) of segments and executes them according to their dependencies and flow control directives.

## Segment Execution Engine

### Sequential Execution

Sequential execution runs segments one after another in the order they appear in the flow:

```kotlin
flow {
    segment("build")
    segment("test")
    segment("deploy")
}
```

**Execution order:**

1. `build` completes
2. `test` starts and completes
3. `deploy` starts and completes

### Parallel Execution

Parallel execution runs independent segments concurrently:

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

**Execution strategy:**

- Kite spawns separate processes for each segment
- Each segment runs independently
- Kite waits for all parallel segments to complete before continuing
- Configurable concurrency limits via `maxConcurrency`

### Dependency Resolution

Segments can declare dependencies that must complete before they execute:

```kotlin
segment("deploy") {
    dependsOn = listOf("build", "test")
    execute { /* ... */ }
}
```

The execution engine:

1. Builds a dependency graph (DAG)
2. Performs topological sort
3. Executes segments in dependency order
4. Fails if circular dependencies are detected

### Conditional Execution

Segments can conditionally execute based on context:

```kotlin
segment("deploy") {
    condition = { context.branch == "main" && !context.isLocal }
    execute { /* ... */ }
}
```

**Evaluation:**

- Conditions are evaluated before execution
- Skipped segments are logged
- Dependent segments still execute (unless they also have conditions)

## Process Model

### Process Isolation

Each segment execution:

- Spawns a new OS process
- Has its own environment variables
- Has isolated stdout/stderr
- Can be terminated independently

### Resource Management

**Concurrency Control:**

```kotlin
ride {
    parallel {
        maxConcurrency = 4  // Max 4 segments running simultaneously
    }
    
    flow {
        parallel {
            segment("task1")
            segment("task2")
            segment("task3")
            segment("task4")
            segment("task5")  // Waits for slot
        }
    }
}
```

**Memory Management:**

```kotlin
ride {
    environment {
        put("GRADLE_OPTS", "-Xmx2g")  // Limit per-process memory
    }
}
```

## Execution Context

Every segment receives an execution context with runtime information:

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

### Context Access

```kotlin
segment("contextAware") {
    execute {
        println("Branch: ${context.branch}")
        println("Commit: ${context.commitSha}")
        
        if (context.isLocal) {
            println("Running locally")
        } else {
            println("Running on ${context.ciPlatform}")
        }
    }
}
```

## Timeout Management

### Segment Timeouts

```kotlin
segment("longRunning") {
    timeout = 30.minutes
    
    onTimeout {
        println("Segment timed out, cleaning up...")
        exec("docker-compose", "down")
    }
    
    execute {
        // Long-running work
    }
}
```

### Dynamic Timeouts from Environment

```kotlin
segment("flexibleTimeout") {
    timeout = env("CI_TIMEOUT")?.toIntOrNull()?.minutes ?: 15.minutes
    execute { /* ... */ }
}
```

## Error Handling

### Retry Logic

```kotlin
segment("resilient") {
    maxRetries = 3
    retryDelay = 5.seconds
    retryOn = listOf(IOException::class, TimeoutException::class)
    
    execute {
        // Potentially flaky operation
    }
}
```

### Failure Handlers

**Segment-level:**

```kotlin
segment("critical") {
    onFailure { error ->
        log.error("Segment failed: ${error.message}")
        notifySlack(webhookUrl, "Segment failed!")
    }
    
    execute { /* ... */ }
}
```

**Ride-level:**

```kotlin
ride {
    flow {
        segment("build")
        segment("test")
    }
    
    onFailure { error ->
        log.error("Ride failed: ${error.message}")
        // Cleanup, notifications, etc.
    }
}
```

## Execution Phases

A ride execution goes through several phases:

### 1. Discovery Phase

- Scan `.kite/segments/` for segment definitions
- Load all segment metadata
- Build segment registry

### 2. Configuration Phase

- Load specified ride configuration
- Resolve segment references
- Apply overrides from ride config

### 3. Validation Phase

- Validate segment definitions
- Check for circular dependencies
- Verify all referenced segments exist

### 4. Planning Phase

- Build execution graph (DAG)
- Perform topological sort
- Identify parallel execution opportunities
- Calculate resource requirements

### 5. Execution Phase

- Execute segments according to plan
- Monitor progress
- Capture logs and outputs
- Handle failures and retries

### 6. Cleanup Phase

- Aggregate results
- Publish artifacts
- Generate reports
- Cleanup temporary resources

## Scheduling Algorithm

### Topological Sort

```
Input: Set of segments with dependencies
Output: Execution order that respects dependencies

Algorithm:
1. Build adjacency list from dependencies
2. Calculate in-degree for each segment
3. Start with segments with in-degree 0
4. Process each segment:
   - Execute segment
   - Decrement in-degree of dependent segments
   - Add segments with in-degree 0 to ready queue
5. Repeat until all segments processed
```

### Parallel Scheduling

```
Input: Parallel block with N segments
maxConcurrency: Maximum concurrent segments

Algorithm:
1. Identify segments with no dependencies
2. Sort by priority (if specified)
3. Start up to maxConcurrency segments
4. When a segment completes:
   - Start next waiting segment
   - Check if new segments are ready
5. Wait for all segments to complete
```

## Logging and Output

### Per-Segment Logging

```kotlin
ride {
    logging {
        perSegmentLogs = true  // Creates logs/segmentName.log
        consoleOutput = "interleaved"  // or "sequential" or "summary-only"
    }
}
```

### Log Formats

**Console (Human-readable):**

```
[12:34:56] Starting segment: build
[12:35:23] ✓ build completed (27s)
[12:35:23] Starting parallel execution (3 segments)
[12:35:24] ⇉ unitTest started
[12:35:24] ⇉ lint started
[12:35:24] ⇉ integrationTest started
[12:36:45] ✓ unitTest completed (1m 21s)
[12:36:52] ✓ lint completed (1m 28s)
[12:38:15] ✓ integrationTest completed (2m 51s)
[12:38:15] Ride completed successfully (3m 59s)
```

**JSON (Machine-readable):**

```json
{
  "timestamp": "2025-01-14T12:34:56Z",
  "level": "INFO",
  "segment": "build",
  "event": "started"
}
```

## Dry Run Mode

Visualize execution without actually running:

```bash
$ kite ride mr --dry-run
```

Output:

```
Ride: MR Ride
═══════════════════════════════════════

Phase 1 (Sequential):
  → build (est: 2m)

Phase 2 (Parallel, max concurrency: 3):
  ⇉ unitTest (est: 2m)
  ⇉ roborazzi (est: 3m)
  ⇉ lint (est: 1m)

Total estimated time: 5m
Total segments: 4
Parallel segments: 3
Max memory: ~6GB (3 × 2GB)
```

## Artifact Management

Artifacts enable data sharing between segments:

### Publishing Artifacts

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
        val mapping = artifacts.get("mapping")
        
        uploadToPlayStore(apk, mapping)
    }
}
```

### Artifact Storage

**Local (MVP):**

- Artifacts stored in `.kite/artifacts/`
- Persisted between segments
- Cleaned up after ride completes

**Remote (Phase 2):**

- Upload to S3, GCS, or artifact repository
- Enable distributed execution
- Cache across ride runs

## Summary

- **Sequential execution**: Segments run one after another
- **Parallel execution**: Independent segments run concurrently
- **Dependencies**: Automatic dependency resolution via DAG
- **Conditions**: Skip segments based on runtime context
- **Timeouts**: Configurable per-segment timeouts
- **Retries**: Automatic retry on specified exceptions
- **Error handling**: Segment and ride-level failure handlers
- **Execution phases**: Discovery → Configuration → Validation → Planning → Execution → Cleanup
- **Logging**: Per-segment logs with multiple output formats
- **Dry run**: Visualize execution plan without running
- **Artifacts**: Share data between segments
