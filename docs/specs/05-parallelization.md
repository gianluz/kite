# Parallelization Deep Dive

## The Core Concept

Kite parallelizes at the **segment/process level**, not within individual tool executions.

Think of it this way:

- **Gradle**: Parallelizes internally (multiple modules, multiple workers)
- **Kite**: Parallelizes different Gradle invocations (or any other tools)

## How Kite and Gradle Parallelization Coexist

### Gradle's Internal Parallelization

Gradle parallelizes in several ways:

1. **Multi-module projects**: Builds independent modules in parallel
2. **Task parallelization**: Runs independent tasks in parallel (with `--parallel`)
3. **Worker API**: Parallel work within a single task

### Kite's Parallelization Layer

Kite adds a **higher-level orchestration layer**:

```
Kite Level:     [unitTest Process]  [roborazzi Process]  [lint Process]
                        ↓                    ↓                   ↓
Gradle Level:   [Gradle: internal    [Gradle: internal    [Gradle: internal
                 parallelization]     parallelization]     parallelization]
```

### Why Both Work Together

**Different Gradle Tasks = Different Concerns**

```kotlin
parallel {
    segment("unitTest")      // Tests module A, B, C
    segment("roborazzi")     // Screenshot tests
    segment("lint")          // Static analysis
}
```

Each segment:

- Targets different Gradle tasks
- Has different resource profiles
- Can fail independently
- Produces different outputs

**Resource Example:**

If you have 8 CPU cores:

```kotlin
// Example: 8 core machine
ride {
    parallel {
        maxConcurrency = 2  // 2 Kite segments
    }
    
    environment {
        put("org.gradle.workers.max", "4")  // 4 workers per Gradle
    }
}
// Total: 2 × 4 = 8 workers (fully utilizing CPU)
```

## Practical Scenarios

### Scenario 1: Android Multi-Module Project

```kotlin
// .kite/segments/modules.kite.kts
segments {
    segment("buildAllModules") {
        execute {
            // Gradle parallelizes module builds internally
            exec("./gradlew", "assembleRelease", "--parallel")
        }
    }
    
    segment("testApp") {
        execute {
            exec("./gradlew", ":app:testReleaseUnitTest")
        }
    }
    
    segment("testCore") {
        execute {
            exec("./gradlew", ":core:testReleaseUnitTest")
        }
    }
}

// .kite/rides/full.kite.kts
ride {
    flow {
        // Step 1: Build all modules (Gradle parallelizes internally)
        segment("buildAllModules")
        
        // Step 2: Test modules in parallel (Kite parallelizes externally)
        parallel {
            segment("testApp")
            segment("testCore")
        }
    }
}
```

**Result:**

- `buildAllModules`: Gradle builds `:app` and `:core` in parallel (one process)
- Parallel phase: Kite runs tests for `:app` and `:core` in parallel (two processes)

### Scenario 2: Different Test Types

```kotlin
parallel {
    segment("unitTest")          // Fast, CPU-bound
    segment("integrationTest")   // Slow, I/O-bound
    segment("screenshotTest")    // Medium, memory-bound
}
```

**Why this works:**

- Different resource profiles benefit from separate processes
- If one type fails, others continue
- Each can have different retry/timeout configurations

### Scenario 3: Cross-Tool Parallelization

```kotlin
parallel {
    segment("gradleTests") {
        execute { exec("./gradlew", "test") }
    }
    
    segment("dockerBuild") {
        execute { exec("docker", "build", "-t", "myapp", ".") }
    }
    
    segment("npmTests") {
        execute { exec("npm", "test") }
    }
}
```

**This is where Kite shines:**

- Mix different tools in one ride
- Each runs independently
- Clear execution model

## Resource Management Guidelines

### Memory Planning

**Formula**: `Total RAM = (maxConcurrency × per-segment RAM) + OS overhead`

```kotlin
// Example: 16GB machine
ride {
    parallel {
        maxConcurrency = 4
    }
    
    environment {
        // 16GB / 4 segments = 4GB per segment
        // Reserve 3GB for Gradle, 1GB for OS/overhead
        put("GRADLE_OPTS", "-Xmx3g -XX:MaxMetaspaceSize=512m")
    }
}
```

### CPU Planning

**Formula**: `Total cores = (maxConcurrency × workers per segment)`

```kotlin
// Example: 8 core machine
ride {
    parallel {
        maxConcurrency = 2  // 2 Kite segments
    }
    
    environment {
        put("org.gradle.workers.max", "4")  // 4 workers per Gradle
    }
}
// Result: 2 × 4 = 8 workers (fully utilizing CPU)
```

### Gradle Daemon Considerations

**Option 1: Share Daemons (Default, Usually Fine)**

```kotlin
segments {
    segment("test") {
        execute {
            exec("./gradlew", "test")  // Will share daemon
        }
    }
}
```

**Pros**: Faster startup, less memory  
**Cons**: Potential conflicts if segments write to same locations

**Option 2: Disable Daemon (Safer, Slower)**

```kotlin
segments {
    segment("test") {
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
segments {
    segment("testUnit") {
        execute {
            env("GRADLE_USER_HOME", ".gradle/unit")
            exec("./gradlew", "testReleaseUnitTest")
        }
    }
    
    segment("testIntegration") {
        execute {
            env("GRADLE_USER_HOME", ".gradle/integration")
            exec("./gradlew", "connectedAndroidTest")
        }
    }
}
```

**Pros**: Complete isolation, daemon benefits  
**Cons**: More memory, more complex

## Decision Guide: Parallel vs Sequential

**Use Parallel when:**

✅ Segments are completely independent (different Gradle tasks, different modules)  
✅ Segments don't write to the same output directories  
✅ You have sufficient resources (RAM, CPU)  
✅ Failure isolation is important (one test failure shouldn't stop others)  
✅ You want better log separation

**Use Sequential when:**

✅ Segments have dependencies (build before test)  
✅ Segments share resources (same database, same output directory)  
✅ Segments are very fast (<10 seconds) - parallelization overhead not worth it  
✅ Resources are constrained (CI runners with limited RAM/CPU)

## Decision Tree

```
Should I use Kite parallelization?
│
├─ Are segments completely independent? ─── NO ──> Use sequential
│       │
│       YES
│       │
├─ Do I have sufficient resources? ─── NO ──> Use sequential or reduce maxConcurrency
│       │
│       YES
│       │
├─ Will segments write to same locations? ─── YES ──> Use sequential or isolate
│       │
│       NO
│       │
└─────> ✅ USE PARALLEL

How many segments should run in parallel?
│
├─ Calculate: available_ram / per_segment_ram = max segments
├─ Calculate: available_cores / workers_per_segment = max segments
└─ Use minimum of both, minus 1-2 for safety
```

## Real-World Performance Example

### Android App Ride

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

### Why Not 4× Faster?

Parallelization has limits:

1. **Dependencies**: Some segments must run sequentially (build before test)
2. **Resource contention**: Limited RAM/CPU
3. **Longest segment**: Parallel time = time of longest segment
4. **Overhead**: Process startup, scheduling

## Best Practices

### 1. Memory Allocation

```kotlin
// .kite/rides/mr.kite.kts
ride {
    // If you have 16GB RAM and run 4 parallel Gradle segments
    parallel {
        maxConcurrency = 4
    }
    
    environment {
        // Reserve ~3GB per Gradle process = 12GB total
        // Leave 4GB for OS and other processes
        put("GRADLE_OPTS", "-Xmx3g -XX:MaxMetaspaceSize=512m")
    }
    
    flow {
        segment("build")
        
        parallel {
            segment("unitTest")
            segment("roborazzi")
            segment("robolectric")
            segment("integrationTest")
        }
    }
}
```

### 2. CPU Utilization

```kotlin
ride {
    parallel {
        // Machine has 8 cores
        maxConcurrency = 4  // Use 4 for Kite segments
    }
    
    environment {
        // Each Gradle can use 2 workers internally
        put("GRADLE_OPTS", "-Xmx2g")
        put("org.gradle.workers.max", "2")
    }
}
```

### 3. Optimized Real-World Example

```kotlin
// .kite/rides/optimized-mr.kite.kts
ride {
    parallel {
        maxConcurrency = 3  // Sweet spot for most CI runners
    }
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g -Dorg.gradle.daemon=true")
    }
    
    flow {
        // Phase 1: Build (cannot be parallelized)
        segment("build")
        
        // Phase 2: Independent test suites (parallel)
        parallel {
            segment("unitTest")        // ~2 min
            segment("roborazzi")       // ~3 min
            segment("lint")            // ~1 min
        }
        
        // Phase 3: Analysis segments (parallel, run after tests)
        parallel {
            segment("detekt") {
                dependsOn = listOf("unitTest")
            }
            segment("jacoco") {
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

## Monitoring and Debugging

### Logging Parallel Segments

```kotlin
// Each segment gets its own log file
ride {
    logging {
        perSegmentLogs = true  // Creates logs/build.log, logs/unitTest.log, etc.
        consoleOutput = "interleaved"  // or "sequential" or "summary-only"
    }
    
    flow {
        parallel {
            segment("unitTest")
            segment("roborazzi")
        }
    }
}
```

### Dry Run Visualization

```bash
$ kite ride mr --dry-run

Ride: Ordinary MR Ride
═══════════════════════════════════════

Phase 1 (Sequential):
  → build (estimated: 2m)

Phase 2 (Parallel, max 3):
  ⇉ unitTest (estimated: 2m)
  ⇉ roborazzi (estimated: 3m)
  ⇉ lint (estimated: 1m)

Total estimated time: 5 minutes
Max memory usage: ~6GB (3 segments × 2GB)
```

## Key Takeaways

- **Kite parallelizes processes**, not internal tool execution
- **Gradle parallelizes tasks**, within its own process
- **Both coexist**: Different layers, different purposes
- **Control resources**: Use `maxConcurrency` and environment variables
- **Isolate failures**: Each segment fails independently
- **Better debugging**: Separate logs, individual retries
- **Cross-tool support**: Mix Gradle, Docker, npm, custom scripts

**Kite doesn't replace Gradle's parallelization—it orchestrates multiple Gradle invocations intelligently.**

## Summary

- Kite parallelizes at the **process level** (separate tool invocations)
- Gradle parallelizes at the **task level** (within a single process)
- Use `maxConcurrency` to limit concurrent segments
- Plan resources: memory and CPU allocation per segment
- Best for: Independent segments with different resource profiles
- Avoid for: Segments with shared state or dependencies
- Performance gains: 30-40% typical, limited by longest segment
