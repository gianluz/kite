# Parallel Execution

Learn how to optimize your CI/CD workflows with parallel segment execution in Kite.

---

## Overview

Kite allows segments to run in **parallel**, dramatically reducing total ride execution time. Instead of waiting for
each segment to finish before starting the next, multiple segments can run simultaneously.

**Example time savings:**

```
Sequential:  build(2m) + test(2m) + lint(1m) + deploy(1m) = 6 minutes
Parallel:    build(2m) + max(test(2m), lint(1m)) + deploy(1m) = 5 minutes
Savings:     17% faster
```

---

## The Basics

### Sequential Execution (Default)

```kotlin
ride {
    name = "Sequential"
    
    flow {
        segment("build")
        segment("test")
        segment("lint")
    }
}
```

**Execution order:**

```
build → test → lint
```

Total time: Sum of all segments

### Parallel Execution

```kotlin
ride {
    name = "Parallel"
    
    flow {
        segment("build")
        
        parallel {
            segment("test")
            segment("lint")
        }
    }
}
```

**Execution order:**

```
build
  ↓
test ⎫
lint ⎭ (run simultaneously)
```

Total time: build + max(test, lint)

---

## How It Works

### Parallel Blocks

Use `parallel {}` to group segments that should run simultaneously:

```kotlin
flow {
    // Sequential: runs first
    segment("setup")
    
    // Parallel: all run at the same time
    parallel {
        segment("unit-test")
        segment("integration-test")
        segment("lint")
    }
    
    // Sequential: runs after all parallel segments complete
    segment("deploy")
}
```

**Execution visualization:**

```
setup
  ↓
unit-test        ⎫
integration-test ⎬ (parallel)
lint             ⎭
  ↓
deploy
```

### Max Concurrency

Control how many segments run simultaneously with `maxConcurrency`:

```kotlin
ride {
    name = "Limited Parallelism"
    maxConcurrency = 2  // Only 2 segments at a time
    
    flow {
        parallel {
            segment("test-1")   // Runs immediately
            segment("test-2")   // Runs immediately
            segment("test-3")   // Waits for slot
            segment("test-4")   // Waits for slot
        }
    }
}
```

**Execution with `maxConcurrency = 2`:**

```
Time →
test-1  ████████
test-2  ████████
test-3          ████████
test-4          ████████
```

**Benefits:**

- Prevents resource exhaustion
- Controls memory/CPU usage
- Maintains system stability

**Default:** Number of available CPU cores (if not specified)

---

## Complete Example

### Android Build Pipeline

```kotlin
segments {
    segment("clean") {
        execute {
            exec("./gradlew", "clean")
        }
    }
    
    segment("compile") {
        dependsOn("clean")
        execute {
            exec("./gradlew", "compileDebugKotlin")
        }
    }
    
    segment("unit-tests") {
        dependsOn("compile")
        execute {
            exec("./gradlew", "testDebugUnitTest")
        }
    }
    
    segment("instrumented-tests") {
        dependsOn("compile")
        execute {
            exec("./gradlew", "connectedAndroidTest")
        }
    }
    
    segment("lint") {
        dependsOn("compile")
        execute {
            exec("./gradlew", "lintDebug")
        }
    }
    
    segment("detekt") {
        dependsOn("compile")
        execute {
            exec("./gradlew", "detekt")
        }
    }
    
    segment("assemble") {
        dependsOn("unit-tests", "instrumented-tests", "lint", "detekt")
        execute {
            exec("./gradlew", "assembleRelease")
        }
    }
}

ride {
    name = "Full CI"
    maxConcurrency = 3  // Run up to 3 segments in parallel
    
    flow {
        // Phase 1: Clean and compile (sequential)
        segment("clean")
        segment("compile")
        
        // Phase 2: All quality checks (parallel)
        parallel {
            segment("unit-tests")
            segment("instrumented-tests")
            segment("lint")
            segment("detekt")
        }
        
        // Phase 3: Final assembly (sequential)
        segment("assemble")
    }
}
```

**Execution timeline:**

```
clean            ████
compile              ████████
unit-tests                   ██████
instrumented-tests           ████████████
lint                         ████
(only 3 run at a time due to maxConcurrency=3)
assemble                                 ██
```

**Time savings:**

- **Sequential**: 2 + 8 + 6 + 12 + 4 + 4 + 2 = 38 minutes
- **Parallel**: 2 + 8 + max(6, 12, 4, 4) + 2 = 24 minutes
- **Savings**: 37% faster (14 minutes saved)

---

## Best Practices

### 1. Parallelize Independent Segments

✅ **Good** - Segments are independent:

```kotlin
parallel {
    segment("unit-tests")         // Tests code
    segment("integration-tests")  // Tests integration
    segment("lint")               // Checks style
    segment("security-scan")      // Scans for vulns
}
```

❌ **Bad** - Segments have dependencies:

```kotlin
parallel {
    segment("build")    // Creates JAR
    segment("test")     // Needs JAR from build ❌
}
```

### 2. Use `dependsOn()` for Order

```kotlin
segment("test") {
    dependsOn("build")  // ✅ Explicit dependency
    execute { /* test code */ }
}

parallel {
    segment("build")   // Runs first
    segment("test")    // Waits for build
}
```

Kite respects dependencies even within `parallel {}` blocks.

### 3. Balance Resource Usage

```kotlin
ride {
    // 8 core machine, 16GB RAM
    maxConcurrency = 3  // Leave resources for OS
    
    environment {
        // Each Gradle gets ~4GB
        put("GRADLE_OPTS", "-Xmx4g")
    }
    
    flow {
        parallel {
            segment("test-1")  // ~4GB RAM, 2 cores
            segment("test-2")  // ~4GB RAM, 2 cores
            segment("test-3")  // ~4GB RAM, 2 cores
        }
    }
}
```

**Resource calculation:**

- **Memory**: `maxConcurrency × per-segment RAM ≤ total RAM - OS overhead`
- **CPU**: `maxConcurrency × per-segment cores ≤ total cores`

### 4. Group Similar Segments

```kotlin
flow {
    segment("build")
    
    // Group: All tests
    parallel {
        segment("unit-tests")
        segment("integration-tests")
        segment("e2e-tests")
    }
    
    // Group: All analysis
    parallel {
        segment("lint")
        segment("detekt")
        segment("ktlint")
    }
    
    segment("deploy")
}
```

**Benefits:**

- Clear execution phases
- Easier to understand
- Better resource utilization

### 5. Set Appropriate `maxConcurrency`

```kotlin
// ❌ Too high - will exhaust resources
maxConcurrency = 10  // On 4-core, 8GB machine

// ✅ Appropriate for resources
maxConcurrency = 3   // Leaves room for OS

// ✅ No limit - uses all cores
maxConcurrency = null  // Default: Runtime.getRuntime().availableProcessors()
```

**Guidelines:**

- **CI runners**: 2-3 (typically have limited resources)
- **Developer machines**: 4-6 (more resources available)
- **Powerful servers**: 8-16 (lots of resources)

---

## Advanced Patterns

### Pattern 1: Diamond Dependency

```kotlin
flow {
    segment("build")
    
    parallel {
        segment("unit-test") {
            dependsOn("build")
        }
        segment("integration-test") {
            dependsOn("build")
        }
    }
    
    segment("deploy") {
        dependsOn("unit-test", "integration-test")
    }
}
```

**Execution:**

```
build
  ├─→ unit-test ────┐
  └─→ integration-test ─┘
         ↓
      deploy
```

### Pattern 2: Phased Parallel Execution

```kotlin
flow {
    // Phase 1: Setup
    segment("checkout")
    segment("dependencies")
    
    // Phase 2: Build variants in parallel
    parallel {
        segment("build-debug")
        segment("build-release")
        segment("build-staging")
    }
    
    // Phase 3: Test each variant in parallel
    parallel {
        segment("test-debug") {
            dependsOn("build-debug")
        }
        segment("test-release") {
            dependsOn("build-release")
        }
        segment("test-staging") {
            dependsOn("build-staging")
        }
    }
    
    // Phase 4: Deploy
    segment("deploy-release") {
        dependsOn("test-release")
    }
}
```

### Pattern 3: Conditional Parallel Branches

```kotlin
flow {
    segment("build")
    
    parallel {
        // Only run if PR
        segment("pr-checks") {
            condition = { ctx ->
                ctx.env("CI_PULL_REQUEST") != null
            }
        }
        
        // Only run if main branch
        segment("deploy") {
            condition = { ctx ->
                ctx.env("BRANCH") == "main"
            }
        }
        
        // Always run
        segment("tests")
    }
}
```

### Pattern 4: Fan-Out/Fan-In

```kotlin
segments {
    segment("prepare-data") {
        execute {
            // Split data into chunks
            workspace.resolve("chunk-1.txt").toFile().writeText("data1")
            workspace.resolve("chunk-2.txt").toFile().writeText("data2")
            workspace.resolve("chunk-3.txt").toFile().writeText("data3")
        }
    }
    
    segment("process-chunk-1") {
        dependsOn("prepare-data")
        execute {
            val data = workspace.resolve("chunk-1.txt").toFile().readText()
            // Process data1
        }
    }
    
    segment("process-chunk-2") {
        dependsOn("prepare-data")
        execute {
            val data = workspace.resolve("chunk-2.txt").toFile().readText()
            // Process data2
        }
    }
    
    segment("process-chunk-3") {
        dependsOn("prepare-data")
        execute {
            val data = workspace.resolve("chunk-3.txt").toFile().readText()
            // Process data3
        }
    }
    
    segment("aggregate-results") {
        dependsOn("process-chunk-1", "process-chunk-2", "process-chunk-3")
        execute {
            // Combine results
        }
    }
}

ride {
    name = "Data Processing"
    maxConcurrency = 3
    
    flow {
        segment("prepare-data")
        
        parallel {
            segment("process-chunk-1")
            segment("process-chunk-2")
            segment("process-chunk-3")
        }
        
        segment("aggregate-results")
    }
}
```

---

## Resource Management

### Memory Planning

**Formula:**

```
Total RAM ≥ (maxConcurrency × per-segment RAM) + OS overhead
```

**Example (16GB machine):**

```kotlin
ride {
    maxConcurrency = 3  // 3 parallel segments
    
    environment {
        // 16GB / 3 = ~5GB per segment
        // Reserve 4GB for segment, 1GB buffer
        put("GRADLE_OPTS", "-Xmx4g")
    }
}
```

### CPU Planning

**Formula:**

```
Optimal maxConcurrency = total_cores / cores_per_segment
```

**Example (8 core machine):**

```kotlin
ride {
    // Each Gradle uses ~2 cores
    // 8 cores / 2 = 4 segments
    maxConcurrency = 4
    
    environment {
        put("org.gradle.workers.max", "2")
    }
}
```

### CI/CD Environment Considerations

**GitHub Actions (Standard runner: 2 cores, 7GB RAM):**

```kotlin
ride {
    maxConcurrency = 2  // Conservative
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g")
    }
}
```

**GitLab CI (Configurable, typically 4 cores, 8GB RAM):**

```kotlin
ride {
    maxConcurrency = 3
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g")
    }
}
```

**Self-hosted (16 cores, 32GB RAM):**

```kotlin
ride {
    maxConcurrency = 8  // Can be aggressive
    
    environment {
        put("GRADLE_OPTS", "-Xmx3g")
    }
}
```

---

## Performance Optimization

### Calculating Expected Speedup

**Amdahl's Law for parallel workflows:**

```
Speedup = 1 / ((1 - P) + (P / N))

Where:
- P = Portion that can be parallelized (0 to 1)
- N = Number of parallel segments
```

**Example:**

- Total time: 10 minutes
- Sequential portion: 2 minutes (20%)
- Parallel portion: 8 minutes (80%)
- Parallel segments: 4

```
Speedup = 1 / ((1 - 0.8) + (0.8 / 4))
        = 1 / (0.2 + 0.2)
        = 1 / 0.4
        = 2.5×
```

**Result:** 10 minutes → 4 minutes (6 minutes saved)

### Identifying Bottlenecks

```bash
# Run with timing information
kite ride CI --verbose

# Output shows segment durations:
✓ build          (2m 30s)
✓ unit-tests     (4m 15s)  ← Bottleneck
✓ lint           (1m 20s)
✓ detekt         (0m 45s)
```

**Optimization strategies:**

1. **Split the bottleneck** into smaller segments
2. **Increase resources** for that segment
3. **Cache dependencies** to speed it up
4. **Run less frequently** (e.g., only on main branch)

### Split Long-Running Segments

```kotlin
// ❌ Before: One long segment (10 minutes)
segment("all-tests") {
    execute {
        exec("./gradlew", "test")  // Tests 20 modules
    }
}

// ✅ After: Split into parallel segments
parallel {
    segment("test-core") {
        execute {
            exec("./gradlew", ":core:test")  // 2 minutes
        }
    }
    segment("test-api") {
        execute {
            exec("./gradlew", ":api:test")  // 2 minutes
        }
    }
    segment("test-ui") {
        execute {
            exec("./gradlew", ":ui:test")  // 3 minutes
        }
    }
}
// Total: 3 minutes (max of all) vs 10 minutes
```

---

## Troubleshooting

### Problem: Segments fail with OOM errors

**Symptom:**

```
OutOfMemoryError: Java heap space
```

**Solution:**
Reduce `maxConcurrency` or increase memory per segment:

```kotlin
ride {
    maxConcurrency = 2  // Reduced from 4
    
    environment {
        put("GRADLE_OPTS", "-Xmx6g")  // Increased from 4g
    }
}
```

### Problem: No speedup from parallelization

**Cause 1:** Dependencies prevent parallel execution

**Solution:** Check dependencies:

```kotlin
segment("test") {
    dependsOn("build")  // Must wait for build
}
```

**Cause 2:** One segment is much longer than others

**Solution:** Split long segments or optimize them

**Cause 3:** `maxConcurrency` is too low

**Solution:** Increase if resources allow:

```kotlin
maxConcurrency = 4  // Increased from 2
```

### Problem: Resource contention

**Symptom:**

```
System is unresponsive
High CPU/memory usage
Segments taking longer than usual
```

**Solution:**
Reduce `maxConcurrency`:

```kotlin
ride {
    maxConcurrency = 2  // Reduced from 4
}
```

---

## Dry Run Visualization

Preview parallel execution without running:

```bash
$ kite ride CI --dry-run

Ride: CI Pipeline
═════════════════════════════════════════

Phase 1 (Sequential):
  → clean (estimated: 30s)
  → compile (estimated: 2m)

Phase 2 (Parallel, max 3):
  ⇉ unit-tests (estimated: 3m)
  ⇉ integration-tests (estimated: 5m)
  ⇉ lint (estimated: 1m)
  ⇉ detekt (estimated: 45s)

Phase 3 (Sequential):
  → assemble (estimated: 1m)

Total estimated time: 8m 30s
  Sequential: 3m 30s
  Parallel: 5m (bottleneck: integration-tests)
Max concurrent segments: 3
Estimated memory usage: ~12GB (3 segments × 4GB)
```

---

## Summary

| Feature | Usage | Benefit |
|---------|-------|---------|
| **`parallel {}`** | Group independent segments | Run multiple segments simultaneously |
| **`maxConcurrency`** | Limit parallel segments | Control resource usage |
| **`dependsOn()`** | Declare dependencies | Ensure correct execution order |
| **Resource planning** | Calculate memory/CPU needs | Prevent system overload |
| **Bottleneck analysis** | Identify slow segments | Target optimization efforts |

**Key takeaways:**

- ✅ Parallelize independent segments for dramatic speedup
- ✅ Use `maxConcurrency` to prevent resource exhaustion
- ✅ Respect dependencies with `dependsOn()`
- ✅ Monitor and optimize bottlenecks
- ✅ Plan resources: memory and CPU allocation

**Typical speedup:** 30-50% faster execution times

---

## Related Topics

- [Writing Rides](05-writing-rides.md) - Complete ride composition guide
- [Writing Segments](04-writing-segments.md) - Segment configuration including dependencies
- [Core Concepts](03-core-concepts.md) - Understanding the execution model

---

## Next Steps

- [Learn about external dependencies →](10-external-dependencies.md)
- [Integrate with CI/CD →](11-ci-integration.md)
- [Explore CLI options →](12-cli-reference.md)
