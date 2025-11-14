# Kite Specification

## Overview

Kite is a Kotlin-based CI task runner designed to replace Fastlane and improve CI/CD workflows by providing a
programmatic, type-safe, and testable way to define build pipelines. It addresses the limitations of bash scripting in
CI configuration files while enabling better code reuse, debugging, and local testing.

**Key Design Principle**: Modular task definitions with composable configurations for different pipeline scenarios.

## Problem Statement

### Current Pain Points

1. **Inefficient Stage Separation**: In GitLab CI (and similar platforms), splitting build and test stages often results
   in redundant rebuilds
2. **Limited Bash Scripting**: CI YAML files rely on bash scripts that are:
    - Hard to test and debug
    - Lack type safety
    - Difficult to reuse across projects
    - Not easily testable with unit tests
3. **Fastlane Limitations**: Ruby-based Fastlane scripts are:
    - Slower than compiled languages
    - Limited ecosystem for mobile/backend development
    - Additional dependency to manage
4. **No Logical Parallelization**: Cannot parallelize tasks within a single CI stage/container

### Kite's Solution

- **Single Stage, Multiple Tasks**: Run multiple logical steps (build, test, integration test) within one CI
  stage/container
- **Kotlin DSL**: Type-safe, IDE-friendly task definitions
- **Modular Task Library**: Define tasks once, reuse across multiple configurations
- **Configuration Composition**: Different configs for different scenarios (ordinary MR, release MR, etc.)
- **Internal Parallelization**: Simulate parallel stages within a single Docker container
- **Testable Infrastructure**: Unit test your CI/CD scripts
- **Conditional Execution**: Smart task execution based on context (MR type, branch, environment)
- **Artifact Management**: Efficient sharing of build outputs between tasks

---

## Core Features

### 1. Modular Task Definition

Tasks are defined in a shared task library, separate from pipeline configurations:

```kotlin
// .kite/tasks/build.tasks.kts
tasks {
    task("build") {
        description = "Build the Android application"
        
        execute {
            exec("./gradlew", "assembleRelease")
        }
        
        outputs {
            artifact("apk", file("app/build/outputs/apk/release/app-release.apk"))
        }
    }
    
    task("buildDebug") {
        description = "Build debug variant"
        
        execute {
            exec("./gradlew", "assembleDebug")
        }
    }
}
```

```kotlin
// .kite/tasks/test.tasks.kts
tasks {
    task("unitTest") {
        description = "Run unit tests"
        
        execute {
            exec("./gradlew", "testReleaseUnitTest")
        }
        
        outputs {
            artifact("test-results", file("app/build/test-results/**/*.xml"))
        }
    }
    
    task("roborazzi") {
        description = "Run Roborazzi screenshot tests"
        
        execute {
            exec("./gradlew", "verifyRoborazziRelease")
        }
    }
    
    task("robolectric") {
        description = "Run Robolectric tests"
        
        execute {
            exec("./gradlew", "testReleaseUnitTest", "-Probolectric=true")
        }
    }
    
    task("integrationTest") {
        description = "Run integration tests"
        
        execute {
            exec("./gradlew", "connectedAndroidTest")
        }
    }
}
```

### 2. Configuration Files

Define different pipeline configurations for different scenarios:

```kotlin
// .kite/configs/mr.config.kts
config {
    name = "Ordinary MR Pipeline"
    
    pipeline {
        // Sequential: build first
        task("build")
        
        // Parallel: run tests in parallel after build
        parallel {
            task("unitTest")
            task("roborazzi")
            task("robolectric")
        }
    }
}
```

```kotlin
// .kite/configs/release-mr.config.kts
config {
    name = "Release MR Pipeline"
    
    pipeline {
        // Sequential: build first
        task("build")
        
        // Parallel: run all tests in parallel
        parallel {
            task("unitTest")
            task("integrationTest")
        }
    }
}
```

```kotlin
// .kite/configs/quick-check.config.kts
config {
    name = "Quick Check (Local Development)"
    
    pipeline {
        task("buildDebug")
        task("unitTest")
    }
}
```

### File Extension Strategy

Kite uses a specific file extension strategy to differentiate between task definitions, configuration files, and
settings.

- Task files: `*.tasks.kts` → `.kite/tasks/build.tasks.kts`
- Config files: `*.config.kts` → `.kite/configs/mr.config.kts`
- Settings file: `kite.settings.kts` → `.kite/kite.settings.kts`

This approach allows for easy identification and organization of different types of files within the Kite project
structure.

### Open Question

One open question related to the file extension strategy is how to handle potential conflicts or overlaps between
different file types. For example, if a user accidentally uses the wrong file extension for a task definition or
configuration file, how will Kite handle this situation?

This question highlights the need for clear documentation and potentially built-in validation mechanisms to ensure that
users are using the correct file extensions and formats for their Kite files.

**Current MVP Approach**: Different extensions for different file types

- Task files: `*.tasks.kts` → `.kite/tasks/build.tasks.kts`
- Config files: `*.config.kts` → `.kite/configs/mr.config.kts`
- Settings file: `kite.settings.kts` → `.kite/kite.settings.kts`

**Pros:**

- ✅ Clear distinction between file types
- ✅ Easy to glob for specific types (e.g., `*.tasks.kts`)
- ✅ Self-documenting filenames

**Cons:**

- ❌ More verbose
- ❌ Less consistent (different suffixes)
- ❌ Harder for IDE to associate all Kite files with one pattern

**Future Consideration**: Unified `.kite.kts` extension (under evaluation)

- Task files: `*.kite.kts` → `.kite/tasks/build.kite.kts`
- Config files: `*.kite.kts` → `.kite/configs/mr.kite.kts`
- Settings file: `settings.kite.kts` → `.kite/settings.kite.kts`

**Pros:**

- ✅ Consistent branding
- ✅ Easier IDE file type association (one pattern: `*.kite.kts`)
- ✅ Cleaner, more distinctive
- ✅ Clear indication that files are Kite-specific

**Cons:**

- ❌ Cannot distinguish file type by extension alone
- ❌ Must rely on directory structure for file type identification
- ❌ May be confusing if files are moved outside standard directories

**Hybrid Approach** (for future consideration):

- Task files: `*.tasks.kite.kts`
- Config files: `*.config.kite.kts`
- Settings file: `settings.kite.kts`

**Pros:**

- ✅ Both distinction AND branding
- ✅ Most explicit

**Cons:**

- ❌ Most verbose
- ❌ Longest filenames

**Decision for MVP**: Use current approach (`*.tasks.kts`, `*.config.kts`) for simplicity and clarity. Evaluate user
feedback and consider migration to `.kite.kts` in a future version with proper migration tooling.

### 3. CI Integration

```yaml
# .gitlab-ci.yml
android-mr:
  stage: build
  image: android-build-image:latest
  script:
    - ./kite run --config mr
  artifacts:
    paths:
      - build/artifacts/
    reports:
      junit: build/test-results/**/*.xml

android-release-mr:
  stage: build
  image: android-build-image:latest
  rules:
    - if: '$CI_MERGE_REQUEST_LABELS =~ /release/'
  script:
    - ./kite run --config release-mr
  artifacts:
    paths:
      - build/artifacts/
```

```bash
# Local usage
./kite run --config quick-check
./kite run --config mr
./kite run --config release-mr

# Run specific tasks directly (without config)
./kite run build unitTest
```

### 4. Task Dependencies in Configs

You can override or add dependencies in the configuration:

```kotlin
// .kite/configs/custom.config.kts
config {
    name = "Custom Pipeline"
    
    pipeline {
        task("lint")
        
        task("build") {
            dependsOn = listOf("lint")  // Add dependency
        }
        
        parallel {
            task("unitTest") {
                dependsOn = listOf("build")
            }
            task("staticAnalysis")
        }
        
        task("packageApp") {
            dependsOn = listOf("unitTest")
        }
    }
}
```

### 5. Advanced Configuration Features

```kotlin
// .kite/configs/advanced.config.kts
config {
    name = "Advanced Pipeline"
    
    // Global configuration
    parallel {
        maxConcurrency = 4  // Limit parallel tasks
    }
    
    // Environment variables for all tasks
    environment {
        put("GRADLE_OPTS", "-Xmx4g")
        put("ANDROID_SDK_ROOT", "/opt/android-sdk")
    }
    
    pipeline {
        task("build")
        
        parallel {
            task("unitTest")
            
            task("integrationTest") {
                condition = { context.isReleaseMR }  // Conditional execution
                timeout = 30.minutes
            }
            
            task("e2eTest") {
                enabled = false  // Disable this task
            }
        }
        
        task("deploy") {
            condition = { context.branch == "main" }
        }
    }
    
    // Failure handling
    onFailure {
        notifySlack(webhookUrl, "Pipeline failed: ${it.message}")
    }
}
```

### Task Timeout Feature

To add a task timeout feature with CI environment variable support, you can modify the task execution logic as follows:

```kotlin
// .kite/configs/advanced.kite.kts
config {
    name = "Advanced Pipeline"
    
    pipeline {
        task("longRunningTask") {
            timeout = System.getenv("CI_TIMEOUT")?.toLongOrNull() ?: 30.minutes
            execute {
                // Task implementation
            }
        }
    }
}
```

In this example, the `longRunningTask` has a timeout that defaults to 30 minutes if the `CI_TIMEOUT` environment
variable is not set.

### 6. Multi-module Project Support

```kotlin
// .kite/tasks/app.tasks.kts
module("app") {
    task("build") {
        execute { exec("./gradlew", ":app:assembleRelease") }
    }
    
    task("test") {
        execute { exec("./gradlew", ":app:testReleaseUnitTest") }
    }
}

// .kite/tasks/library.tasks.kts
module("library") {
    task("build") {
        execute { exec("./gradlew", ":library:assembleRelease") }
    }
    
    task("test") {
        execute { exec("./gradlew", ":library:testReleaseUnitTest") }
    }
}

// .kite/configs/full.config.kts
config {
    pipeline {
        // Build all modules in parallel
        parallel {
            task("app:build")
            task("library:build")
        }
        
        // Test all modules in parallel
        parallel {
            task("app:test")
            task("library:test")
        }
    }
}
```

### 7. Task Execution Engine

#### Sequential Execution

- Respect `dependsOn` relationships defined in tasks or configs
- Build a task graph (DAG)
- Execute in topological order
- Skip tasks based on conditions

#### Parallel Execution

- Identify independent tasks (no dependency relationship)
- Execute in parallel within the same container
- Configurable parallelism level
- Thread-safe execution context

#### Parallelization Strategy

**Important**: Kite parallelizes at the **task/process level**, not within individual tool executions.

When you define:

```kotlin
parallel {
    task("unitTest")      // Runs: ./gradlew testReleaseUnitTest
    task("roborazzi")     // Runs: ./gradlew verifyRoborazziRelease
    task("robolectric")   // Runs: ./gradlew testReleaseUnitTest -Probolectric=true
}
```

Kite will:

1. **Launch separate processes** for each task simultaneously
2. Each process runs independently (separate Gradle daemon invocations)
3. Each Gradle process can use its own internal parallelization (e.g., `org.gradle.parallel=true`)

**Why this works:**

- **Different Gradle tasks**: `testReleaseUnitTest`, `verifyRoborazziRelease`, etc. are different Gradle tasks that can
  run concurrently
- **Separate JVM processes**: Each Kite task spawns a new process, so they don't compete for the same Gradle daemon
- **Resource utilization**: You can control overall parallelism to avoid resource exhaustion

**Example with resource limits:**

```kotlin
// Example: 8 core machine
config {
    parallel {
        maxConcurrency = 2  // Run max 2 tasks in parallel
    }
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g")  // Limit each Gradle process
    }
    
    pipeline {
        task("build")
        
        parallel {
            task("unitTest")
            task("roborazzi")
            task("lint")           // If maxConcurrency=2, this waits
        }
    }
}
```

**Contrast with single Gradle invocation:**

```bash
# Traditional approach (one process, Gradle internal parallelization)
./gradlew testReleaseUnitTest verifyRoborazziRelease lintRelease

# Kite approach (multiple processes, explicit control)
./kite run --config mr
# Spawns 3 separate processes:
# Process 1: ./gradlew testReleaseUnitTest
# Process 2: ./gradlew verifyRoborazziRelease  
# Process 3: ./gradlew lintRelease
```

**Benefits of Kite's approach:**

- **Better failure isolation**: One test suite failure doesn't stop others
- **Clearer logs**: Each task has separate, dedicated logs
- **More control**: Configure timeout, retries per task
- **Debuggability**: Re-run individual tasks without re-running everything
- **Cross-tool parallelization**: Mix Gradle, shell scripts, Docker commands, etc.

**When NOT to use Kite parallelization:**

- Tasks that share mutable state (e.g., writing to same database)
- Extremely resource-constrained environments (set `maxConcurrency = 1`)
- Tasks with implicit dependencies not captured in config

#### Incremental Execution

- Cache task outputs
- Skip tasks when inputs haven't changed (optional feature)
- Smart dependency detection

### 8. Execution Context

Provide rich context information to tasks:

```kotlin
data class ExecutionContext(
    val branch: String,
    val commitSha: String,
    val mrNumber: String?,
    val isReleaseMR: Boolean,
    val environment: Map<String, String>,
    val ciPlatform: CIPlatform, // GitLab, GitHub, Local, etc.
    val workspace: Path,
    val artifacts: ArtifactManager
)
```

### 9. Built-in Helpers

#### Command Execution

```kotlin
exec("command", "arg1", "arg2") // Execute and throw on failure
execOrNull("command", "arg1") // Execute and return null on failure
shell("complex | bash | pipeline") // Execute shell commands
```

#### File Operations

```kotlin
copy(source, destination)
move(source, destination)
delete(path)
createDirectory(path)
zipFiles(source, destination)
unzipFiles(source, destination)
```

#### Android/Mobile Specific

```kotlin
buildApk(variant = "release")
buildAab(variant = "release")
runTests(variant = "release")
uploadToPlayStore(track = "internal")
signApk(keystore, alias, password)
```

#### Version Management

```kotlin
bumpVersion(type = VersionBump.MINOR)
getCurrentVersion()
tagRelease(version)
```

#### Notifications

```kotlin
notifySlack(webhook, message)
notifyEmail(address, subject, body)
```

### 10. Artifact Management

```kotlin
// Produce artifacts
artifacts.put("apk", file("app/build/outputs/apk/release/app-release.apk"))

// Consume artifacts from previous tasks
val apk = artifacts.get("apk")
```

### 11. Conditional Execution

```kotlin
task("deployProduction") {
    condition = { 
        context.branch == "main" && 
        context.ciPlatform != CIPlatform.LOCAL 
    }
    execute {
        // Deploy logic
    }
}
```

### 12. Error Handling

```kotlin
task("resilientTask") {
    timeout = System.getenv("CI_TIMEOUT")?.toLongOrNull() ?: 30.minutes
    retryOn = listOf(IOException::class, TimeoutException::class)
    maxRetries = 3
    retryDelay = 5.seconds
    
    onFailure { error ->
        notifySlack(webhookUrl, "Task failed: ${error.message}")
    }

    onTimeout {
        println("Task timed out after ${timeout}")
        // Cleanup logic
    }

    execute {
        // Task logic
    }
}
```

### 13. Local Execution & Debugging

```kotlin
// Run with specific config
// $ kite run --config mr

// Run specific tasks
// $ kite run build unitTest

// Debug mode with verbose logging
// $ kite run --config mr --debug

// Dry run to see what would execute
// $ kite run --config mr --dry-run

// List all available tasks
// $ kite tasks

// List all available configs
// $ kite configs

// Visualize the task graph for a config
// $ kite graph --config mr
```

---

## Parallelization Deep Dive

### The Core Concept

Kite parallelizes at the **task/process level**, not within individual tool executions.

Think of it this way:

- **Gradle**: Parallelizes internally (multiple modules, multiple workers)
- **Kite**: Parallelizes different Gradle invocations (or any other tools)

### How Kite and Gradle Parallelization Coexist

#### Gradle's Internal Parallelization

Gradle parallelizes in several ways:

1. **Multi-module projects**: Builds independent modules in parallel
2. **Task parallelization**: Runs independent tasks in parallel (with `--parallel`)
3. **Worker API**: Parallel work within a single task

#### Kite's Parallelization Layer

Kite adds a **higher-level orchestration layer**:

```
Kite Level:     [unitTest Process]  [roborazzi Process]  [lint Process]
                        ↓                    ↓                   ↓
Gradle Level:   [Gradle: internal    [Gradle: internal    [Gradle: internal
                 parallelization]     parallelization]     parallelization]
```

#### Why Both Work Together

**Different Gradle Tasks = Different Concerns**

```kotlin
parallel {
    task("unitTest")      // Tests module A, B, C
    task("roborazzi")     // Screenshot tests
    task("lint")          // Static analysis
}
```

Each task:

- Targets different Gradle tasks
- Has different resource profiles
- Can fail independently
- Produces different outputs

**Resource Example:**

If you have 8 CPU cores:

```kotlin
// Example: 8 core machine
config {
    parallel {
        maxConcurrency = 2  // 2 Kite tasks
    }
    
    environment {
        put("org.gradle.workers.max", "4")  // 4 workers per Gradle
    }
}
// Total: 2 × 4 = 8 workers (fully utilizing CPU)
```

### Practical Scenarios

#### Scenario 1: Android Multi-Module Project

```kotlin
// .kite/tasks/modules.tasks.kts
tasks {
    task("buildAllModules") {
        execute {
            // Gradle parallelizes module builds internally
            exec("./gradlew", "assembleRelease", "--parallel")
        }
    }
    
    task("testApp") {
        execute {
            exec("./gradlew", ":app:testReleaseUnitTest")
        }
    }
    
    task("testCore") {
        execute {
            exec("./gradlew", ":core:testReleaseUnitTest")
        }
    }
}

// .kite/configs/full.config.kts
config {
    pipeline {
        // Step 1: Build all modules (Gradle parallelizes internally)
        task("buildAllModules")
        
        // Step 2: Test modules in parallel (Kite parallelizes externally)
        parallel {
            task("testApp")
            task("testCore")
        }
    }
}
```

**Result:**

- `buildAllModules`: Gradle builds `:app` and `:core` in parallel (one process)
- Parallel phase: Kite runs tests for `:app` and `:core` in parallel (two processes)

#### Scenario 2: Different Test Types

```kotlin
parallel {
    task("unitTest")          // Fast, CPU-bound
    task("integrationTest")   // Slow, I/O-bound
    task("screenshotTest")    // Medium, memory-bound
}
```

**Why this works:**

- Different resource profiles benefit from separate processes
- If one type fails, others continue
- Each can have different retry/timeout configurations

#### Scenario 3: Cross-Tool Parallelization

```kotlin
parallel {
    task("gradleTests") {
        execute { exec("./gradlew", "test") }
    }
    
    task("dockerBuild") {
        execute { exec("docker", "build", "-t", "myapp", ".") }
    }
    
    task("npmTests") {
        execute { exec("npm", "test") }
    }
}
```

**This is where Kite shines:**

- Mix different tools in one pipeline
- Each runs independently
- Clear execution model

### Resource Management Guidelines

#### Memory Planning

**Formula**: `Total RAM = (maxConcurrency × per-task RAM) + OS overhead`

```kotlin
// Example: 16GB machine
config {
    parallel {
        maxConcurrency = 4
    }
    
    environment {
        // 16GB / 4 tasks = 4GB per task
        // Reserve 3GB for Gradle, 1GB for OS/overhead
        put("GRADLE_OPTS", "-Xmx3g -XX:MaxMetaspaceSize=512m")
    }
}
```

#### CPU Planning

**Formula**: `Total cores = (maxConcurrency × workers per task)`

```kotlin
// Example: 8 core machine
config {
    parallel {
        maxConcurrency = 2  // 2 Kite tasks
    }
    
    environment {
        put("org.gradle.workers.max", "4")  // 4 workers per Gradle
    }
}
// Result: 2 × 4 = 8 workers (fully utilizing CPU)
```

#### Gradle Daemon Considerations

**Option 1: Share Daemons (Default, Usually Fine)**

```kotlin
tasks {
    task("test") {
        execute {
            exec("./gradlew", "test")  // Will share daemon
        }
    }
}
```

**Pros**: Faster startup, less memory  
**Cons**: Potential conflicts if tasks write to same locations

**Option 2: Disable Daemon (Safer, Slower)**

```kotlin
tasks {
    task("test") {
        execute {
            exec("./gradlew", "test", "--no-daemon")
        }
    }
}
```

**Pros**: Complete isolation  
**Cons**: Slower, more memory

**Option 3: Separate Daemon Directories (Advanced)**

```kotlin
tasks {
    task("testUnit") {
        execute {
            env("GRADLE_USER_HOME", ".gradle/unit")
            exec("./gradlew", "testReleaseUnitTest")
        }
    }
    
    task("testIntegration") {
        execute {
            env("GRADLE_USER_HOME", ".gradle/integration")
            exec("./gradlew", "connectedAndroidTest")
        }
    }
}
```

**Pros**: Complete isolation, daemon benefits  
**Cons**: More memory, more complex

### Decision Guide: Parallel vs Sequential

**Use Parallel when:**
✅ Tasks are completely independent (different Gradle tasks, different modules)  
✅ Tasks don't write to the same output directories  
✅ You have sufficient resources (RAM, CPU)  
✅ Failure isolation is important (one test failure shouldn't stop others)  
✅ You want better log separation

**Use Sequential when:**
✅ Tasks have dependencies (build before test)  
✅ Tasks share resources (same database, same output directory)  
✅ Tasks are very fast (<10 seconds) - parallelization overhead not worth it  
✅ Resources are constrained (CI runners with limited RAM/CPU)

### Decision Tree

```
Should I use Kite parallelization?
│
├─ Are tasks completely independent? ─── NO ──> Use sequential
│       │
│       YES
│       │
├─ Do I have sufficient resources? ─── NO ──> Use sequential or reduce maxConcurrency
│       │
│       YES
│       │
├─ Will tasks write to same locations? ─── YES ──> Use sequential or isolate
│       │
│       NO
│       │
└─────> ✅ USE PARALLEL

How many tasks should run in parallel?
│
├─ Calculate: available_ram / per_task_ram = max tasks
├─ Calculate: available_cores / workers_per_task = max tasks
└─ Use minimum of both, minus 1-2 for safety
```

### Real-World Performance Example

#### Android App Pipeline

**Sequential Execution:**

```
build:        2m 00s
unitTest:     1m 30s
roborazzi:    2m 30s
lint:         1m 00s
detekt:       0m 45s
─────────────────────
Total:        7m 45s
```

**Kite Parallel Execution:**

```
build:                           2m 00s
parallel {
    unitTest:   1m 30s
    roborazzi:  2m 30s  ← longest
    lint:       1m 00s
}
parallel {
    detekt:     0m 45s
}
─────────────────────────────────────
Total:        2m 00s + 2m 30s + 0m 45s = 5m 15s
```

**Savings: 32% faster** (7m 45s → 5m 15s)

#### Why Not 4× Faster?

Parallelization has limits:

1. **Dependencies**: Some tasks must run sequentially (build before test)
2. **Resource contention**: Limited RAM/CPU
3. **Longest task**: Parallel time = time of longest task
4. **Overhead**: Process startup, scheduling

### Best Practices Summary

When running parallel tasks with tools like Gradle, consider:

**1. Memory Allocation**

```kotlin
// .kite/configs/mr.config.kts
config {
    // If you have 16GB RAM and run 4 parallel Gradle tasks
    parallel {
        maxConcurrency = 4
    }
    
    environment {
        // Reserve ~3GB per Gradle process = 12GB total
        // Leave 4GB for OS and other processes
        put("GRADLE_OPTS", "-Xmx3g -XX:MaxMetaspaceSize=512m")
    }
    
    pipeline {
        task("build")
        
        parallel {
            task("unitTest")
            task("roborazzi")
            task("robolectric")
            task("integrationTest")
        }
    }
}
```

**2. CPU Utilization**

```kotlin
config {
    parallel {
        // Machine has 8 cores
        maxConcurrency = 4  // Use 4 for Kite tasks
    }
    
    environment {
        // Each Gradle can use 2 workers internally
        put("GRADLE_OPTS", "-Xmx2g")
        put("org.gradle.workers.max", "2")
    }
}
```

**3. Optimized Real-World Example**

```kotlin
// .kite/configs/optimized-mr.config.kts
config {
    parallel {
        maxConcurrency = 3  // Sweet spot for most CI runners
    }
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g -Dorg.gradle.daemon=true")
    }
    
    pipeline {
        // Phase 1: Build (cannot be parallelized)
        task("build")
        
        // Phase 2: Independent test suites (parallel)
        parallel {
            task("unitTest")        // ~2 min
            task("roborazzi")       // ~3 min
            task("lint")            // ~1 min
        }
        
        // Phase 3: Analysis tasks (parallel, run after tests)
        parallel {
            task("detekt") {
                dependsOn = listOf("unitTest")
            }
            task("jacoco") {
                dependsOn = listOf("unitTest")
            }
        }
    }
}

// Time savings:
// Sequential: build(2m) + unitTest(2m) + roborazzi(3m) + lint(1m) = 8 minutes
// Parallel:   build(2m) + max(unitTest(2m), roborazzi(3m), lint(1m)) = 5 minutes
// Savings:    37.5% faster
```

### Monitoring and Debugging

**Logging parallel tasks:**

```kotlin
// Each task gets its own log file
config {
    logging {
        perTaskLogs = true  // Creates logs/build.log, logs/unitTest.log, etc.
        consoleOutput = "interleaved"  // or "sequential" or "summary-only"
    }
    
    pipeline {
        parallel {
            task("unitTest")
            task("roborazzi")
        }
    }
}
```

**Dry run to visualize execution:**

```bash
$ ./kite run --config mr --dry-run

Pipeline: Ordinary MR Pipeline
===============================
Phase 1 (Sequential):
  → build (estimated: 2m)

Phase 2 (Parallel, max 3):
  ⇉ unitTest (estimated: 2m)
  ⇉ roborazzi (estimated: 3m)
  ⇉ lint (estimated: 1m)

Total estimated time: 5 minutes
Max memory usage: ~6GB (3 tasks × 2GB)
```

### Key Takeaways

- **Kite parallelizes processes**, not internal tool execution
- **Gradle parallelizes tasks**, within its own process
- **Both coexist**: Different layers, different purposes
- **Control resources**: Use `maxConcurrency` and environment variables
- **Isolate failures**: Each task fails independently
- **Better debugging**: Separate logs, individual retries
- **Cross-tool support**: Mix Gradle, Docker, npm, custom scripts

**Kite doesn't replace Gradle's parallelization—it orchestrates multiple Gradle invocations intelligently.**

---

## Architecture

### Project Structure

```
project-root/
├── .kite/
│   ├── tasks/
│   │   ├── build.tasks.kts        # Build-related tasks
│   │   ├── test.tasks.kts         # Test-related tasks
│   │   ├── deploy.tasks.kts       # Deployment tasks
│   │   └── common.tasks.kts       # Shared/common tasks
│   ├── configs/
│   │   ├── mr.config.kts          # Ordinary MR pipeline
│   │   ├── release-mr.config.kts  # Release MR pipeline
│   │   ├── nightly.config.kts     # Nightly build pipeline
│   │   └── local.config.kts       # Local development
│   └── kite.settings.kts          # Global Kite settings (optional)
├── kite                           # Kite executable
└── build.gradle.kts
```

### Components

#### 1. CLI Entry Point

- Parse command line arguments (`--config`, `--debug`, `--dry-run`, etc.)
- Discover and load task definitions from `.kite/tasks/`
- Load specified configuration from `.kite/configs/`
- Initialize execution context
- Trigger task execution

#### 2. Task Definition Loader

- Scan `.kite/tasks/` directory for `*.tasks.kts` files
- Parse and compile Kotlin scripts
- Build a registry of available tasks
- Validate task definitions
- Support for modular task organization

#### 3. Configuration Loader

- Load configuration file from `.kite/configs/<name>.config.kts`
- Resolve task references to task definitions
- Merge task properties (dependencies, conditions, etc.)
- Validate configuration
- Build final task graph

#### 4. Task Scheduler

- Topological sort of tasks
- Identify parallelizable tasks
- Manage task execution order
- Handle task failures and retries

#### 5. Execution Runtime

- Provide execution context
- Manage artifacts
- Handle process execution
- Capture logs and outputs

#### 6. Platform Adapters

- GitLab CI adapter (read CI variables)
- GitHub Actions adapter
- Local execution adapter
- Generic CI adapter

### Configuration Files

#### Task Definition: `*.tasks.kts`

```kotlin
import io.kite.dsl.*

tasks {
    task("taskName") {
        description = "Task description"
        execute {
            // Task implementation
        }
    }
}
```

#### Pipeline Configuration: `*.config.kts`

```kotlin
import io.kite.dsl.*

config {
    name = "Pipeline Name"
    
    pipeline {
        task("taskName")
        
        parallel {
            task("task1")
            task("task2")
        }
    }
}
```

#### Global Settings: `kite.settings.kts` (optional)

```kotlin
import io.kite.dsl.*

settings {
    // Default task directory
    taskDirectory = ".kite/tasks"
    
    // Default config directory
    configDirectory = ".kite/configs"
    
    // Default parallel execution settings
    parallel {
        maxConcurrency = Runtime.getRuntime().availableProcessors()
    }
    
    // Global environment variables
    environment {
        put("CI", "true")
    }
}
```

---

## Plugin System

### Overview

Kite's plugin system allows extending functionality with reusable, modular components. Plugins can:

- Add new task types
- Provide helper functions and DSL extensions
- Integrate with external services (Play Store, translation services, etc.)
- Share common functionality across projects

### Plugin Definition

Plugins are defined as Kotlin libraries that implement the `KitePlugin` interface:

```kotlin
// Example: Play Store Plugin
// build.gradle.kts
dependencies {
    implementation("io.kite:kite-core:1.0.0")
}

// src/main/kotlin/io/kite/plugins/playstore/PlayStorePlugin.kt
package io.kite.plugins.playstore

import io.kite.plugin.KitePlugin
import io.kite.dsl.TaskBuilder

class PlayStorePlugin : KitePlugin {
    override val id = "playstore"
    override val version = "1.0.0"
    
    override fun apply(context: PluginContext) {
        // Register helper functions
        context.registerHelper("uploadToPlayStore") { track: String, apkPath: String ->
            uploadToPlayStoreImpl(track, apkPath)
        }
        
        // Register task type
        context.registerTaskType("playStoreUpload") { builder ->
            PlayStoreUploadTask(builder)
        }
    }
    
    private fun uploadToPlayStoreImpl(track: String, apkPath: String) {
        // Implementation using Google Play Developer API
        val service = AndroidPublisher.Builder(/* ... */).build()
        // ... upload logic
    }
}
```

### Plugin Discovery and Loading

#### Option 1: Classpath-based Discovery (Recommended for MVP)

Plugins are discovered via classpath scanning using Java ServiceLoader:

```kotlin
// src/main/resources/META-INF/services/io.kite.plugin.KitePlugin
io.kite.plugins.playstore.PlayStorePlugin
```

```kotlin
// In project's build.gradle.kts
dependencies {
    kitePlugin("io.kite.plugins:playstore:1.0.0")
}
```

#### Option 2: Explicit Configuration

```kotlin
// .kite/kite.settings.kts
settings {
    plugins {
        plugin("io.kite.plugins:playstore:1.0.0")
        plugin("io.kite.plugins:translations:1.0.0")
        plugin("com.example:custom-plugin:2.0.0")
    }
}
```

#### Option 3: Plugin Directory (Phase 2)

```
.kite/
├── plugins/
│   ├── playstore-plugin.jar
│   ├── translations-plugin.jar
│   └── custom-plugin.jar
```

### Using Plugins in Tasks

Once loaded, plugins extend the Kite DSL:

```kotlin
// .kite/tasks/deploy.tasks.kts
tasks {
    task("deployToPlayStore") {
        description = "Upload APK to Play Store"
        
        inputs {
            file("app/build/outputs/apk/release/app-release.apk")
        }
        
        execute {
            // Plugin-provided helper function
            uploadToPlayStore(
                track = "internal",
                apkPath = "app/build/outputs/apk/release/app-release.apk",
                releaseNotes = mapOf(
                    "en-US" to "Bug fixes and improvements"
                )
            )
        }
    }
}
```

### Built-in Plugin Examples

#### 1. Play Store Plugin

```kotlin
// Using the Play Store plugin
// .kite/kite.settings.kts
settings {
    plugins {
        plugin("io.kite.plugins:playstore:1.0.0") {
            serviceAccountJson = env("GOOGLE_SERVICE_ACCOUNT_JSON")
            packageName = "com.example.app"
        }
    }
}

// .kite/tasks/deploy.tasks.kts
tasks {
    task("uploadToPlayStoreInternal") {
        execute {
            playStore {
                track = "internal"
                apk = file("app/build/outputs/apk/release/app-release.apk")
                releaseNotes {
                    locale("en-US", "Bug fixes and performance improvements")
                    locale("es-ES", "Corrección de errores y mejoras de rendimiento")
                }
                obfuscationMapping = file("app/build/outputs/mapping/release/mapping.txt")
            }
        }
    }
    
    task("promoteToProduction") {
        execute {
            playStore {
                promoteTrack(from = "beta", to = "production")
                userFraction = 0.1  // 10% rollout
            }
        }
    }
}
```

#### 2. Translation/Localization Plugin

```kotlin
// Using the translation plugin
// .kite/kite.settings.kts
settings {
    plugins {
        plugin("io.kite.plugins:translations:1.0.0") {
            provider = "crowdin"  // or "lokalise", "phrase", etc.
            apiKey = env("CROWDIN_API_KEY")
            projectId = env("CROWDIN_PROJECT_ID")
        }
    }
}

// .kite/tasks/translations.tasks.kts
tasks {
    task("uploadStrings") {
        description = "Upload strings to translation service"
        
        inputs {
            file("app/src/main/res/values/strings.xml")
        }
        
        execute {
            translations {
                upload {
                    source = file("app/src/main/res/values/strings.xml")
                    branch = context.branch
                }
            }
        }
    }
    
    task("downloadTranslations") {
        description = "Download translated strings"
        
        outputs {
            directory("app/src/main/res/")
        }
        
        execute {
            translations {
                download {
                    destination = file("app/src/main/res/")
                    languages = listOf("es", "fr", "de", "it", "pt")
                    exportApproved = true
                }
            }
        }
    }
    
    task("syncTranslations") {
        dependsOn = listOf("uploadStrings", "downloadTranslations")
    }
}
```

#### 3. Docker Plugin

```kotlin
// .kite/tasks/docker.tasks.kts
tasks {
    task("buildDockerImage") {
        execute {
            docker {
                build {
                    context = file(".")
                    dockerfile = file("Dockerfile")
                    tag = "myapp:${context.commitSha}"
                    buildArgs = mapOf(
                        "VERSION" to getCurrentVersion()
                    )
                }
            }
        }
    }
    
    task("pushDockerImage") {
        dependsOn = listOf("buildDockerImage")
        
        execute {
            docker {
                push {
                    image = "myapp:${context.commitSha}"
                    registry = "gcr.io/my-project"
                }
            }
        }
    }
}
```

#### 4. Notification Plugin

```kotlin
// .kite/tasks/notifications.tasks.kts
tasks {
    task("notifyDeployment") {
        execute {
            notifications {
                slack {
                    webhook = env("SLACK_WEBHOOK")
                    channel = "#deployments"
                    message = """
                        🚀 Deployed version ${getCurrentVersion()}
                        Branch: ${context.branch}
                        Commit: ${context.commitSha}
                    """.trimIndent()
                    
                    attachment {
                        color = "good"
                        fields {
                            field("Environment", "Production")
                            field("Deploy Time", currentTimestamp())
                        }
                    }
                }
                
                email {
                    to = listOf("team@example.com")
                    subject = "Deployment Successful: ${getCurrentVersion()}"
                    body = "..."
                }
            }
        }
    }
}
```

### Plugin Development Guide

#### Creating a Custom Plugin

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.0"
}

dependencies {
    implementation("io.kite:kite-plugin-api:1.0.0")
    
    // Add dependencies for your plugin
    implementation("com.example:external-api:1.0.0")
}

// src/main/kotlin/com/example/MyPlugin.kt
package com.example.kite.plugins

import io.kite.plugin.*

class CustomServicePlugin : KitePlugin {
    override val id = "custom-service"
    override val version = "1.0.0"
    override val description = "Integration with custom service"
    
    private lateinit var apiKey: String
    private lateinit var endpoint: String
    
    override fun configure(config: Map<String, Any>) {
        apiKey = config["apiKey"] as? String ?: error("apiKey required")
        endpoint = config["endpoint"] as? String ?: "https://api.example.com"
    }
    
    override fun apply(context: PluginContext) {
        // Register DSL extension
        context.registerExtension("customService") {
            CustomServiceDsl(apiKey, endpoint)
        }
        
        // Register helper functions
        context.registerHelper("uploadToService") { file: File ->
            uploadImpl(file)
        }
        
        // Register task type
        context.registerTaskType("customServiceTask") { builder ->
            CustomServiceTask(builder, apiKey, endpoint)
        }
    }
    
    private fun uploadImpl(file: File) {
        // Implementation
    }
}

// DSL Extension
class CustomServiceDsl(
    private val apiKey: String,
    private val endpoint: String
) {
    fun upload(file: File, metadata: Map<String, String> = emptyMap()) {
        // Upload logic
    }
    
    fun download(id: String, destination: File) {
        // Download logic
    }
}

// Custom Task Type
class CustomServiceTask(
    builder: TaskBuilder,
    private val apiKey: String,
    private val endpoint: String
) : Task(builder) {
    var uploadFile: File? = null
    var metadata: Map<String, String> = emptyMap()
    
    override suspend fun execute(context: ExecutionContext) {
        val file = uploadFile ?: error("uploadFile must be set")
        // Execute task logic
    }
}
```

#### Publishing a Plugin

```kotlin
// build.gradle.kts
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.example.kite.plugins"
            artifactId = "custom-service"
            version = "1.0.0"
            
            from(components["java"])
        }
    }
    
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/example/kite-plugins")
        }
    }
}
```

### Plugin Registry (Future)

```bash
# Search for plugins
kite plugin search playstore

# Install plugin
kite plugin install playstore

# List installed plugins
kite plugin list

# Update plugins
kite plugin update

# Remove plugin
kite plugin remove playstore
```

### Plugin API Surface

```kotlin
// Core plugin interfaces
interface KitePlugin {
    val id: String
    val version: String
    val description: String?
    
    fun configure(config: Map<String, Any>) {}
    fun apply(context: PluginContext)
}

interface PluginContext {
    // Register DSL extensions
    fun registerExtension(name: String, extension: Any)
    
    // Register helper functions
    fun registerHelper(name: String, function: KFunction<*>)
    
    // Register task types
    fun registerTaskType(name: String, factory: (TaskBuilder) -> Task)
    
    // Access to Kite internals
    val kiteVersion: String
    val workspaceDir: File
    val logger: Logger
}
```

### Plugin Security and Sandboxing (Phase 2)

```kotlin
// Plugin permissions
class SecurePlugin : KitePlugin {
    override val permissions = setOf(
        Permission.NETWORK_ACCESS,
        Permission.FILE_WRITE,
        Permission.ENV_READ
    )
    
    // ... implementation
}
```

### Plugin Testing

```kotlin
// Test your plugin
class PlayStorePluginTest {
    @Test
    fun `plugin uploads APK successfully`() {
        val plugin = PlayStorePlugin()
        val mockContext = mockPluginContext()
        
        plugin.configure(mapOf(
            "serviceAccountJson" to "test-credentials.json",
            "packageName" to "com.example.test"
        ))
        
        plugin.apply(mockContext)
        
        // Verify plugin registered correctly
        verify(mockContext).registerHelper("uploadToPlayStore", any())
    }
}
```

### Official Plugin Ecosystem (Planned)

**Core Plugins** (maintained by Kite team):

- `playstore` - Google Play Store integration
- `appstore` - Apple App Store integration
- `docker` - Docker build and deployment
- `notifications` - Slack, email, Teams notifications
- `git` - Advanced Git operations
- `firebase` - Firebase App Distribution, Crashlytics

**Community Plugins**:

- `translations` - Crowdin, Lokalise, Phrase integration
- `sentry` - Sentry error tracking integration
- `jira` - JIRA issue tracking integration
- `kubernetes` - Kubernetes deployment
- `terraform` - Infrastructure as code

### Plugin Best Practices

1. **Versioning**: Use semantic versioning
2. **Configuration**: Accept configuration via DSL and environment variables
3. **Error Handling**: Provide clear error messages
4. **Documentation**: Include comprehensive docs and examples
5. **Testing**: Write tests for your plugin
6. **Dependencies**: Minimize external dependencies
7. **Compatibility**: Specify compatible Kite versions

### Migration Path from Fastlane

For users migrating from Fastlane, plugins can provide familiar APIs:

```kotlin
// Fastlane-compatible syntax via plugin
tasks {
    task("deployToPlayStore") {
        execute {
            // Familiar Fastlane-style syntax
            supply {
                track = "internal"
                apk = "app/build/outputs/apk/release/app-release.apk"
                json_key = env("GOOGLE_CREDENTIALS")
            }
        }
    }
}
```

---

## CI Integration

### GitLab CI Example

```yaml
# .gitlab-ci.yml
variables:
  KITE_VERSION: "1.0.0"

.kite-base:
  image: android-build-image:latest
  before_script:
    - curl -L https://github.com/kite/releases/download/v${KITE_VERSION}/kite -o kite
    - chmod +x kite
  artifacts:
    paths:
      - build/artifacts/
    reports:
      junit: build/test-results/**/*.xml

android-mr:
  extends: .kite-base
  stage: build
  script:
    - ./kite run --config mr
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'

android-release-mr:
  extends: .kite-base
  stage: build
  script:
    - ./kite run --config release-mr
  rules:
    - if: '$CI_MERGE_REQUEST_LABELS =~ /release/'

android-nightly:
  extends: .kite-base
  stage: build
  script:
    - ./kite run --config nightly
  rules:
    - if: '$CI_PIPELINE_SOURCE == "schedule"'
```

### GitHub Actions Example

```yaml
name: Android CI

on:
  pull_request:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Kite
        run: |
          curl -L https://github.com/kite/releases/download/v1.0.0/kite -o kite
          chmod +x kite
      
      - name: Run MR Pipeline
        if: github.event_name == 'pull_request'
        run: ./kite run --config mr
      
      - name: Run Release Pipeline
        if: contains(github.event.pull_request.labels.*.name, 'release')
        run: ./kite run --config release-mr
```

---

## Use Cases

### 1. Ordinary MR Pipeline

```kotlin
// .kite/configs/mr.config.kts
config {
    name = "Ordinary MR Pipeline"
    
    pipeline {
        // Build first
        task("build")
        
        // Run tests in parallel
        parallel {
            task("unitTest")
            task("roborazzi")
            task("robolectric")
        }
        
        // Lint and static analysis in parallel (optional)
        parallel {
            task("lint")
            task("detekt")
        }
    }
}
```

### 2. Release MR Pipeline

```kotlin
// .kite/configs/release-mr.config.kts
config {
    name = "Release MR Pipeline"
    
    pipeline {
        // Build first
        task("build")
        
        // Run comprehensive tests in parallel
        parallel {
            task("unitTest")
            task("integrationTest")
        }
        
        // Security and compliance checks
        parallel {
            task("securityScan")
            task("licenseCheck")
        }
        
        // Package release artifacts
        task("packageRelease") {
            dependsOn = listOf("unitTest", "integrationTest")
        }
    }
}
```

### 3. Nightly Build Pipeline

```kotlin
// .kite/configs/nightly.config.kts
config {
    name = "Nightly Build Pipeline"
    
    pipeline {
        task("build")
        
        parallel {
            task("unitTest")
            task("integrationTest")
            task("e2eTest")
            task("performanceTest")
        }
        
        task("generateReports")
        
        task("notifyTeam") {
            onFailure {
                notifySlack(webhookUrl, "Nightly build failed!")
            }
        }
    }
}
```

### 4. Local Development Pipeline

```kotlin
// .kite/configs/local.config.kts
config {
    name = "Local Quick Check"
    
    pipeline {
        task("buildDebug")
        task("unitTest")
        task("lint")
    }
}
```

---

## Technical Requirements

### Language & Runtime

- **Language**: Kotlin 2.0+
- **JVM**: Java 17+ (LTS) compatibility
- **Scripting**: Kotlin Scripting for `.kts` files
- **Coroutines**: For parallel execution

### Dependencies (Minimal)

- Kotlin stdlib
- Kotlinx.coroutines
- Kotlinx.serialization (for config/artifacts)
- Clikt or similar (CLI framework)

### Distribution

- **Binary**: Single executable JAR or native binary (GraalVM)
- **Installation**: Homebrew, apt/yum, direct download
- **CI Integration**: Docker image with Kite pre-installed

### Performance Goals

- **Startup**: < 1 second for simple pipelines
- **Overhead**: < 100ms per task execution
- **Parallel**: Efficiently utilize all CPU cores

---

## Testing Strategy

### Unit Tests for Kite Itself

- Task definition parsing
- Configuration loading and merging
- Task graph construction
- Scheduling algorithms
- Platform adapters

### Testing User Pipelines

```kotlin
// tests/PipelineTest.kt
class AndroidPipelineTest {
    @Test
    fun `mr config executes build and tests`() {
        val result = kiteTest {
            loadConfig("mr")
        }.run()
        
        assertTrue(result.success)
        assertTrue(result.executedTasks.contains("build"))
        assertTrue(result.executedTasks.contains("unitTest"))
    }
    
    @Test
    fun `parallel tasks execute concurrently`() {
        val result = kiteTest {
            loadConfig("mr")
        }.run()
        
        val parallelTasks = listOf("unitTest", "roborazzi", "robolectric")
        assertTrue(result.wereExecutedInParallel(parallelTasks))
    }
}
```

---

## Future Enhancements (Post-MVP)

### Phase 2

- Remote caching (S3, GCS)
- Distributed execution
- Web dashboard for pipeline visualization
- The plugin system is now designed and ready for implementation, with the following plugins to be built:
    - Play Store plugin
    - Translation services plugin
    - Docker plugin
    - Notifications plugin
    - Firebase plugin
- Task templates and inheritance

### Phase 3

- IDE plugin (IntelliJ)
- AI-powered pipeline optimization
- Cost estimation
- Performance profiling
- Configuration validation and auto-complete

---

## Open Questions

1. **Configuration Format**: Should we support both `.kts` scripts and compiled Kotlin classes, or just one?
    - **Recommendation**: Start with `.kts` for simplicity and flexibility

2. **Parallelism Default**: Should parallel execution be opt-in or automatic for independent tasks?
    - **Recommendation**: Opt-in via `parallel { }` block for explicit control

3. **Artifact Storage**: Local filesystem only, or support remote storage from MVP?
    - **Recommendation**: Local filesystem for MVP, design API for remote storage later

4. **Plugin System**: How should plugins be loaded and managed?
    - **Recommendation for MVP**: Classpath-based discovery using Java ServiceLoader with explicit configuration in
      `kite.settings.kts`. Design the `KitePlugin` interface and `PluginContext` API.
    - **Phase 2**: Implement official plugins (playstore, translations, docker, notifications) and plugin directory
      support
    - **Phase 3**: Add plugin CLI commands (`kite plugin install/list/update`) and plugin registry/marketplace
    - **Decision**: Start with simple, extensible architecture that can evolve

5. **Versioning**: How to version pipeline definitions? Lock file for dependencies?
    - **Recommendation**: Version Kite itself; task definitions are part of the repo

6. **Secrets Management**: How to handle sensitive data (API keys, passwords)?
    - **Recommendation**: Read from environment variables, support CI platform secret mechanisms

7. **Logging**: Structured logging format? Support for log aggregation services?
    - **Recommendation**: Structured JSON logs with human-readable console output

8. **Interop**: Should we support calling bash scripts or other tools easily, or pure Kotlin only?
    - **Recommendation**: Full bash interop via `exec()` and `shell()` helpers

9. **Task Discovery**: Should tasks auto-discover, or require explicit imports in configs?
    - **Recommendation**: Auto-discover all `*.tasks.kts` files in `.kite/tasks/`

10. **Config Inheritance**: Should configs support inheritance/composition?
    - **Recommendation**: Phase 2 feature; start with simple standalone configs

11. **File Extension Strategy**: Should we use unified `.kite.kts` extension or keep separate `*.tasks.kts` and
    `*.config.kts`?
    - **Recommendation**: Start with separate extensions for MVP (clearer distinction), evaluate unified `.kite.kts` for
      v1.0 based on user feedback and IDE tooling maturity

---

## Success Metrics

- **Developer Experience**: Reduce pipeline definition time by 50%
- **Execution Speed**: No more than 5% overhead compared to raw bash
- **Debuggability**: Ability to run and debug pipelines locally before CI
- **Adoption**: Replace Fastlane in at least one production project
- **Testability**: Enable unit testing of 80%+ of CI logic
- **Modularity**: Tasks defined once, reused across 3+ different configs

---

## Timeline Estimate (MVP)

- **Week 1-2**: Core DSL, task definitions, and configuration loading
- **Week 3-4**: Task graph construction and execution engine
- **Week 5-6**: CLI and parallel execution support
- **Week 7**: Platform adapters (GitLab, GitHub, Local)
- **Week 8**: Built-in helpers (exec, file operations, etc.)
- **Week 9**: Documentation and examples
- **Week 10**: Testing and refinement

---

## Non-Goals (For MVP)

- GUI/Web interface
- Remote execution/distribution
- Support for non-JVM languages
- Complex workflow orchestration (Airflow/Temporal features)
- Integration with 10+ CI platforms (focus on GitLab, GitHub, Local)
- Configuration inheritance/composition
- Remote artifact caching
- **Plugin implementations** (Plugin system architecture will be designed, but official plugins like Play Store,
  translations, etc. will be built in Phase 2)
- Plugin registry/marketplace
- Plugin CLI commands

