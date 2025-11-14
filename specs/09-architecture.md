# Architecture

## Overview

Kite is designed as a modular, layered architecture with clear separation of concerns. The system is built around a core
execution engine that orchestrates segment execution based on configured rides.

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────┐
│                       CLI Layer                           │
│  (kite-cli: Command parsing, user interface)            │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│                    DSL Layer                              │
│  (kite-dsl: Script loading, DSL parsing, builders)      │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│                   Core Layer                              │
│  (kite-core: Models, execution engine, scheduling)      │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│                 Runtime Layer                             │
│  (kite-runtime: Process execution, platform adapters)   │
└──────────────────────────────────────────────────────────┘
```

## Module Structure

### kite-core

**Responsibility**: Core domain models and business logic

**Contents**:

- Domain models: `Segment`, `Ride`, `ExecutionContext`
- Execution engine: Scheduling, dependency resolution
- Graph algorithms: DAG construction, topological sort
- Artifact management
- Logging abstractions

**Dependencies**: Minimal (only Kotlin stdlib, kotlinx.coroutines, kotlinx.serialization)

### kite-dsl

**Responsibility**: Kotlin DSL and script loading

**Contents**:

- DSL builders: `SegmentBuilder`, `RideBuilder`, `SettingsBuilder`
- Script compilation and loading
- `.kite.kts` file discovery
- DSL validation

**Dependencies**:

- `kite-core`
- `kotlin-scripting-*` libraries

### kite-runtime

**Responsibility**: Execution runtime and platform integration

**Contents**:

- Process execution
- Platform adapters: GitLab CI, GitHub Actions, Local
- Built-in helpers: File operations, Git, HTTP, Docker
- Context detection and population

**Dependencies**:

- `kite-core`
- `kite-dsl`

### kite-cli

**Responsibility**: Command-line interface

**Contents**:

- CLI commands: `ride`, `run`, `segments`, `rides`, `graph`
- Argument parsing (Clikt)
- Console output formatting
- Entry point (`main()`)

**Dependencies**:

- All other modules
- Clikt

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         kite-cli                             │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ RideCommand  │  │ RunCommand   │  │ ListCommand  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│         └──────────────────┴──────────────────┘              │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                         kite-dsl                              │
│                                                               │
│  ┌───────────────────┐  ┌──────────────────┐               │
│  │ SegmentLoader     │  │ RideLoader        │               │
│  │ - Discover files  │  │ - Load config     │               │
│  │ - Compile scripts │  │ - Resolve refs    │               │
│  └─────────┬─────────┘  └────────┬──────────┘               │
│            │                      │                          │
│            │    ┌─────────────────▼────────┐                │
│            └────► DSL Builders              │                │
│                 │ - segments { }            │                │
│                 │ - ride { }                │                │
│                 │ - settings { }            │                │
│                 └─────────────┬──────────────┘                │
└─────────────────────────────┼───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                        kite-core                             │
│                                                               │
│  ┌──────────────────┐  ┌────────────────────┐              │
│  │ Domain Models    │  │ Execution Engine   │              │
│  │ - Segment        │  │ - Scheduler        │              │
│  │ - Ride           │  │ - DAG Builder      │              │
│  │ - Context        │  │ - Executor         │              │
│  └──────────────────┘  └────────┬───────────┘              │
│                                  │                          │
│                    ┌─────────────▼──────────┐               │
│                    │ Artifact Manager       │               │
│                    └────────────────────────┘               │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                       kite-runtime                           │
│                                                               │
│  ┌──────────────────┐  ┌────────────────────┐              │
│  │ Process Manager  │  │ Platform Adapters  │              │
│  │ - exec()         │  │ - GitLabCI         │              │
│  │ - Parallel exec  │  │ - GitHubActions    │              │
│  │ - Timeout        │  │ - Local            │              │
│  └──────────────────┘  └────────────────────┘              │
│                                                               │
│  ┌─────────────────────────────────────────┐                │
│  │        Built-in Helpers                 │                │
│  │ - FileOps  - Git    - Docker           │                │
│  │ - HTTP     - Notifications             │                │
│  └─────────────────────────────────────────┘                │
└──────────────────────────────────────────────────────────────┘
```

## Execution Flow

### 1. Initialization

```
User runs: kite ride mr

CLI Layer:
 └─> Parse arguments
 └─> Initialize Kite engine

DSL Layer:
 └─> Discover .kite/segments/*.kite.kts files
 └─> Compile and load segment definitions
 └─> Load .kite/rides/mr.kite.kts
 └─> Resolve segment references
```

### 2. Planning

```
Core Layer:
 └─> Build execution graph (DAG)
 └─> Validate dependencies (no cycles)
 └─> Perform topological sort
 └─> Identify parallel execution opportunities
 └─> Calculate resource requirements
```

### 3. Execution

```
Runtime Layer:
 └─> Detect platform (GitLab/GitHub/Local)
 └─> Populate ExecutionContext
 └─> For each segment (in dependency order):
     ├─> Check condition (skip if false)
     ├─> Spawn process
     ├─> Execute segment logic
     ├─> Capture output
     ├─> Handle errors/retries
     └─> Store artifacts
```

### 4. Completion

```
 └─> Aggregate results
 └─> Generate reports
 └─> Clean up temporary resources
 └─> Exit with status code
```

## Key Design Patterns

### 1. Builder Pattern

Used extensively in DSL:

```kotlin
class SegmentBuilder {
    var name: String = ""
    var description: String? = null
    var dependsOn: List<String> = emptyList()
    var execute: ExecutionContext.() -> Unit = {}
    
    fun build(): Segment = Segment(name, description, dependsOn, execute)
}
```

### 2. Strategy Pattern

Platform adapters:

```kotlin
interface PlatformAdapter {
    fun detect(): Boolean
    fun populateContext(context: ExecutionContext)
}

class GitLabCIAdapter : PlatformAdapter { /* ... */ }
class GitHubActionsAdapter : PlatformAdapter { /* ... */ }
class LocalAdapter : PlatformAdapter { /* ... */ }
```

### 3. Template Method Pattern

Segment execution:

```kotlin
abstract class Segment {
    abstract fun execute(context: ExecutionContext)
    
    fun run(context: ExecutionContext): Result {
        if (!checkCondition(context)) return Result.Skipped
        if (!checkTimeout()) return Result.Timeout
        
        try {
            execute(context)
            return Result.Success
        } catch (e: Exception) {
            return handleError(e)
        }
    }
}
```

### 4. Visitor Pattern

Segment graph traversal:

```kotlin
interface SegmentVisitor {
    fun visit(segment: Segment)
}

class ExecutionVisitor : SegmentVisitor {
    override fun visit(segment: Segment) {
        segment.execute(context)
    }
}
```

## Concurrency Model

### Process-Level Parallelism

```kotlin
class ParallelExecutor {
    suspend fun executeParallel(
        segments: List<Segment>,
        maxConcurrency: Int
    ): List<Result> = coroutineScope {
        val semaphore = Semaphore(maxConcurrency)
        
        segments.map { segment ->
            async {
                semaphore.withPermit {
                    executeSegment(segment)
                }
            }
        }.awaitAll()
    }
}
```

### Thread Safety

- **Immutable models**: Segment, Ride definitions are immutable
- **Context isolation**: Each segment gets its own execution context
- **Artifact synchronization**: Artifact manager uses concurrent data structures

## Performance Considerations

### Startup Time

- **Goal**: < 1 second
- **Strategy**:
    - Lazy loading of segments
    - Compiled `.kite.kts` caching
    - Minimal dependency tree

### Execution Overhead

- **Goal**: < 100ms per segment
- **Strategy**:
    - Native process spawning
    - Efficient DAG algorithms
    - Minimal reflection

### Memory Footprint

- **Goal**: < 200MB base + segment overhead
- **Strategy**:
    - Streaming logs (don't buffer everything)
    - Release compiled scripts after loading
    - Efficient artifact storage

## Technical Requirements

### Language & Runtime

- **Language**: Kotlin 2.0+
- **JVM**: Java 17+ (LTS)
- **Scripting**: Kotlin Scripting API
- **Concurrency**: kotlinx.coroutines

### Dependencies

**Core Dependencies** (minimal):

- kotlin-stdlib
- kotlinx-coroutines-core
- kotlinx-serialization-json

**DSL Dependencies**:

- kotlin-scripting-common
- kotlin-scripting-jvm
- kotlin-scripting-dependencies

**CLI Dependencies**:

- clikt (CLI framework)

**Runtime Dependencies**:

- ktor-client (for HTTP operations)
- Process API (Java stdlib)

### Distribution

**Formats**:

- Executable JAR (with dependencies)
- Native binary (GraalVM)
- Docker image
- Homebrew formula

**Targets**:

- Linux (x86_64, arm64)
- macOS (x86_64, arm64)
- Windows (x86_64)

## Testing Strategy

### Unit Tests

- Core models and algorithms
- DSL builders
- Platform adapters (mocked)
- Individual helpers

### Integration Tests

- End-to-end ride execution
- Script loading and compilation
- Artifact management
- Platform detection

### Performance Tests

- Startup time benchmarks
- Execution overhead measurement
- Memory profiling
- Concurrent execution stress tests

## Summary

- **Modular**: Clear separation of concerns (CLI, DSL, Core, Runtime)
- **Layered**: Dependencies flow downward (no circular deps)
- **Extensible**: Plugin system, platform adapters
- **Concurrent**: Process-level parallelism with coroutines
- **Type-safe**: Full Kotlin type system throughout
- **Testable**: Interfaces and dependency injection
- **Performant**: < 1s startup, < 100ms overhead
- **Portable**: JVM-based, runs everywhere Java 17+ runs
