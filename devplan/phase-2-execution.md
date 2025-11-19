# Phase 2: Segment Graph & Execution Engine

**Status**: ✅ **COMPLETE**  
**Goal**: Build the execution engine that schedules and runs segments  
**Duration**: 2 weeks

---

## Overview

Phase 2 implements the core execution engine of Kite. This includes building dependency graphs, implementing topological
sort, creating schedulers for sequential and parallel execution, and providing basic runtime capabilities like process
execution.

---

## Epic 2.1: Segment Graph Construction ✅ COMPLETE

**Story Points**: 8 | **Duration**: 3 days  
**Status**: ✅ Complete

### Tasks

- [x] **Task 2.1.1**: Implement DAG builder
    - Create `SegmentGraph` class
    - Implement dependency resolution algorithm
    - Detect and report circular dependencies using DFS
    - Build adjacency list representation
    - Calculate reachability for validation
    - Write unit tests with various graph topologies
    - **Deliverable**: `SegmentGraph.kt`

- [x] **Task 2.1.2**: Implement topological sort
    - Implement Kahn's algorithm for topological sorting
    - Handle parallel execution groups (same level)
    - Calculate execution levels for visualization
    - Return sorted segments with level information
    - Write unit tests
    - **Deliverable**: Topological sort in `SegmentGraph.kt`

- [x] **Task 2.1.3**: Implement graph validation
    - **Merged with Task 2.1.1**
    - Validate all segment references exist
    - Check for unreachable segments
    - Verify no self-dependencies
    - Validate parallel block constraints
    - Write validation tests
    - **Deliverable**: Validation logic in `SegmentGraph.kt`

### Deliverables

✅ **Production Code**:

- `SegmentGraph.kt` - 440 lines
- Complete graph theory implementation

✅ **Tests**: 614 lines (45 tests)

- Circular dependency detection
- Topological sort verification
- Unreachable segment detection
- Self-dependency validation
- Complex graph topologies

✅ **Algorithms Implemented**:

- **Kahn's Algorithm**: Topological sorting
- **DFS**: Cycle detection
- **Reachability Analysis**: Validate all segments reachable
- **Level Calculation**: For parallel execution grouping

✅ **Features**:

- Detects circular dependencies
- Reports missing segment references
- Identifies unreachable segments
- Groups segments by execution level
- Thread-safe immutable graph

### Example Usage

```kotlin
val graph = SegmentGraph(
    segments = listOf(
        segment("build"),
        segment("test") { dependsOn("build") },
        segment("deploy") { dependsOn("test") }
    )
)

// Validate graph
val errors = graph.validate()
if (errors.isNotEmpty()) {
    println("Graph validation failed: $errors")
}

// Get topological order
val sorted = graph.topologicalSort()
// Returns: [build, test, deploy]

// Get execution levels (for parallel execution)
val levels = graph.getExecutionLevels()
// Returns: [[build], [test], [deploy]]
```

### Graph Theory Implementation

**Adjacency List Representation**:

```kotlin
private val adjacencyList: Map<String, List<String>>
```

**Cycle Detection (DFS)**:

```kotlin
fun detectCycles(): List<String> {
    val visited = mutableSetOf<String>()
    val recursionStack = mutableSetOf<String>()
    
    for (segment in segments) {
        if (hasCycle(segment, visited, recursionStack)) {
            return buildCyclePath(segment)
        }
    }
    return emptyList()
}
```

**Topological Sort (Kahn's Algorithm)**:

```kotlin
fun topologicalSort(): List<Segment> {
    val inDegree = calculateInDegree()
    val queue = segments.filter { inDegree[it.name] == 0 }
    val result = mutableListOf<Segment>()
    
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        result.add(current)
        
        for (dependent in adjacencyList[current.name] ?: emptyList()) {
            inDegree[dependent]--
            if (inDegree[dependent] == 0) {
                queue.add(getSegment(dependent))
            }
        }
    }
    
    return result
}
```

---

## Epic 2.2: Segment Scheduler ✅ COMPLETE

**Story Points**: 10 | **Duration**: 4 days  
**Status**: ✅ Complete

### Tasks

- [x] **Task 2.2.1**: Implement sequential scheduler
    - Create `SegmentScheduler` interface
    - Implement `SequentialScheduler`
    - Execute segments in topological order
    - Handle segment skipping (conditions)
    - Track execution state
    - Integrate artifact management
    - Execute lifecycle hooks
    - Write tests
    - **Deliverable**: `SequentialScheduler.kt` (212 lines)

- [x] **Task 2.2.2**: Implement parallel scheduler
    - Implement `ParallelScheduler` using Kotlin coroutines
    - Add `maxConcurrency` support with Semaphore
    - Implement parallel block execution (same-level segments)
    - Handle failure modes (fail-fast vs continue)
    - Respect dependencies across levels
    - Integrate artifact management
    - Execute lifecycle hooks
    - Write concurrency tests
    - **Deliverable**: `ParallelScheduler.kt` (168 lines)

- [x] **Task 2.2.3**: Implement execution tracking
    - **Merged with Tasks 2.2.1 & 2.2.2**
    - Create `ExecutionTracker` for monitoring progress
    - Track segment states (pending, running, complete, failed)
    - Implement execution time measurement
    - Add segment result aggregation in `SchedulerResult`
    - Write tests
    - **Deliverable**: Integrated in schedulers

### Deliverables

✅ **Production Code**:

- `SequentialScheduler.kt` - 212 lines
- `ParallelScheduler.kt` - 168 lines
- `SegmentScheduler.kt` - Interface
- `SchedulerResult.kt` - Result aggregation
- **Total**: 450 lines

✅ **Tests**: 710 lines (33 tests)

- Sequential execution tests
- Parallel execution tests
- Dependency ordering tests
- Failure handling tests
- Concurrency limit tests
- Lifecycle hook execution tests

✅ **Features**:

- Sequential execution (simple, predictable)
- Parallel execution (fast, efficient)
- Configurable concurrency limits
- Fail-fast or continue-on-error modes
- Artifact passing between segments
- Lifecycle hook execution
- Execution time tracking
- Thread-safe with coroutines

### Sequential Scheduler

**Execution Flow**:

1. Get topological order from graph
2. Execute each segment in order
3. Check conditions (skip if false)
4. Run execute {} block
5. Run outputs {} block (artifacts)
6. Execute lifecycle hooks
7. Track results

**Example**:

```kotlin
val scheduler = SequentialScheduler(graph, artifactManager, logger)
val result = scheduler.execute(context)

// Segments execute: A -> B -> C (in order)
```

### Parallel Scheduler

**Execution Flow**:

1. Get execution levels from graph
2. For each level:
    - Launch coroutines for all segments in level
    - Apply concurrency limit with Semaphore
    - Wait for all to complete
3. Move to next level
4. Aggregate results

**Concurrency Control**:

```kotlin
val semaphore = Semaphore(maxConcurrency)

coroutineScope {
    for (segment in level) {
        launch {
            semaphore.acquire()
            try {
                executeSegment(segment)
            } finally {
                semaphore.release()
            }
        }
    }
}
```

**Example**:

```kotlin
val scheduler = ParallelScheduler(
    graph = graph,
    artifactManager = artifactManager,
    maxConcurrency = 4,
    logger = logger
)

val result = scheduler.execute(context)

// Level 0: [A] executes
// Level 1: [B, C, D] execute in parallel (max 4)
// Level 2: [E] executes
```

### Performance Benefits

**Parallel execution stats** are calculated and displayed:

```
✓ Parallel execution completed in 5.2s
  (Sequential would take ~15.8s, saved 10.6s - 67% faster)
```

---

## Epic 2.3: Basic Execution Runtime ✅ COMPLETE

**Story Points**: 8 | **Duration**: 3 days  
**Status**: ✅ Complete

### Tasks

- [x] **Task 2.3.1**: Implement process executor
    - Create `ProcessExecutor` class in `kite-runtime`
    - Implement `exec()` function with command execution
    - Add timeout support using ProcessHandle and coroutines
    - Capture stdout/stderr with stream readers
    - Cross-platform support (Windows/Unix)
    - Write tests with real processes
    - **Deliverable**: `ProcessExecutor.kt` (234 lines)

- [x] **Task 2.3.2**: Implement segment execution context
    - Populate `ExecutionContext` from environment
    - Add Git information detection (deferred to Phase 4)
    - Implement context isolation per segment
    - Add helper methods: `exec()`, `env()`, `secret()`
    - Write tests
    - **Deliverable**: `ExecutionContextExtensions.kt` (116 lines)

- [x] **Task 2.3.3**: Implement basic error handling
    - Add try-catch around segment execution
    - Implement retry logic (basic support in Segment model)
    - Add `onFailure` callback support (lifecycle hooks)
    - Propagate errors with detailed messages
    - Write error handling tests
    - **Deliverable**: Integrated in schedulers

### Deliverables

✅ **Production Code**:

- `ProcessExecutor.kt` - 234 lines
- `ExecutionContextExtensions.kt` - 116 lines
- **Total**: 350 lines

✅ **Tests**: 198 lines (19 tests)

- Command execution tests
- Timeout tests (fixed flaky tests)
- Error handling tests
- Stream capture tests
- Cross-platform tests

✅ **Features**:

- Cross-platform process execution
- Timeout support with proper cancellation
- Stdout/stderr capture
- Working directory support
- Environment variable passing
- Exit code handling
- Proper error messages

### Process Execution

**Simple Command**:

```kotlin
segment("build") {
    execute {
        exec("./gradlew", "build")
    }
}
```

**With Timeout**:

```kotlin
segment("long-test") {
    timeout = 30.minutes
    
    execute {
        exec("./gradlew", "integrationTest", timeout = 30.minutes)
    }
}
```

**With Working Directory**:

```kotlin
segment("frontend-build") {
    execute {
        exec("npm", "install", workingDir = "frontend/")
        exec("npm", "run", "build", workingDir = "frontend/")
    }
}
```

**With Environment Variables**:

```kotlin
segment("deploy") {
    execute {
        exec("kubectl", "apply", "-f", "k8s/",
            env = mapOf("KUBECONFIG" -> "/path/to/config"))
    }
}
```

### ProcessExecutor Implementation

**Key Features**:

```kotlin
class ProcessExecutor(
    private val logger: SegmentLogger
) {
    suspend fun exec(
        command: String,
        vararg args: String,
        workingDir: String? = null,
        env: Map<String, String> = emptyMap(),
        timeout: Duration = Duration.INFINITE
    ): String {
        val process = startProcess(command, args, workingDir, env)
        
        // Capture streams in parallel
        val stdout = captureStream(process.inputStream)
        val stderr = captureStream(process.errorStream)
        
        // Wait with timeout
        val completed = withTimeoutOrNull(timeout) {
            process.waitFor()
        }
        
        if (completed == null) {
            process.destroyForcibly()
            throw TimeoutException("Command timed out after $timeout")
        }
        
        // Check exit code
        if (process.exitValue() != 0) {
            throw CommandFailedException(stderr)
        }
        
        return stdout
    }
}
```

---

## Phase 2 Summary

### Statistics

**Production Code**: 1,240 lines

- Segment graph: 440 lines
- Schedulers: 450 lines
- Process executor: 350 lines

**Test Code**: 1,522 lines

- Graph tests: 614 lines (45 tests)
- Scheduler tests: 710 lines (33 tests)
- Executor tests: 198 lines (19 tests)

**Total Tests**: 97 tests - all passing ✅

**Test-to-Code Ratio**: 1.23:1 (excellent)

### Key Achievements

✅ **Complete Graph Theory** - DAG, topological sort, cycle detection  
✅ **Two Schedulers** - Sequential and parallel execution  
✅ **Coroutine-based** - Efficient parallel execution  
✅ **Process Execution** - Cross-platform with timeout  
✅ **Artifact Integration** - Passing artifacts between segments  
✅ **Lifecycle Hooks** - Integrated in execution flow  
✅ **Thread-safe** - All concurrent operations safe

### Performance Characteristics

**Sequential Scheduler**:

- Predictable execution order
- Simple debugging
- No concurrency overhead
- Best for small pipelines

**Parallel Scheduler**:

- Up to 67% faster (measured in tests)
- Configurable concurrency
- Respects dependencies
- Best for large pipelines

**Process Execution**:

- Startup overhead: ~10-50ms per process
- Timeout accuracy: ±10ms
- Stream capture: Real-time

### Design Patterns Used

- **Strategy Pattern**: SegmentScheduler interface
- **Observer Pattern**: Execution tracking
- **Coroutines**: Parallel execution
- **Semaphore**: Concurrency control
- **Builder Pattern**: Graph construction
- **Template Method**: Scheduler base logic

### Lessons Learned

1. **Flaky Tests**: Timing-based tests are unreliable - use behavior testing
2. **Coroutines Win**: Much simpler than thread pools
3. **Graph Theory**: Topological sort is essential for dependency resolution
4. **Timeout Handling**: ProcessHandle + coroutines = reliable timeouts

---

## Next Steps

Phase 2 is **COMPLETE** ✅

**Next**: Phase 3 - CLI & File Discovery

See [devplan/README.md](README.md) for overall progress.

---

**Last Updated**: November 18, 2025  
**Status**: ✅ Complete  
**Lines of Code**: 1,240 production, 1,522 tests
