# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Epic 1.1 Complete**: Project Setup & Infrastructure
    - Multi-module Gradle project (kite-core, kite-dsl, kite-runtime, kite-cli)
    - Gradle 9.2.0 with configuration cache enabled
  - Kotlin 2.0.21 + Java 17 LTS toolchain support
  - Code quality tools: ktlint + detekt configured
  - Test infrastructure: JUnit 5 + MockK
    - Maven publishing configuration with POM metadata
  - GitHub Actions CI workflow (`.github/workflows/ci.yml`)
  - Comprehensive documentation structure

- **Epic 1.2 Complete**: Core Domain Models
    - `Segment` model with execution state and properties
    - `SegmentStatus` enum (PENDING, RUNNING, SUCCESS, FAILURE, SKIPPED, TIMEOUT)
    - `ExecutionContext` model with CI platform detection
    - `CIPlatform` enum (GITLAB, GITHUB, LOCAL, GENERIC)
    - `ArtifactManager` interface with `InMemoryArtifactManager` implementation
    - `Ride` configuration model with validation
    - `FlowNode` sealed class hierarchy (Sequential, Parallel, SegmentRef)
    - `SegmentOverrides` for ride-specific customization
    - `PlatformAdapter` interface with 4 implementations:
        - `GitLabCIPlatformAdapter` - GitLab CI support
        - `GitHubActionsPlatformAdapter` - GitHub Actions support
        - `LocalPlatformAdapter` - Local execution support
        - `GenericPlatformAdapter` - Generic CI platform support
    - `PlatformDetector` for automatic platform detection
    - Comprehensive unit tests (100+ tests, >90% coverage)

- **Epic 1.3 Complete**: Kotlin Scripting Integration
    - **Task 1.3.1**: Kotlin Scripting Engine
        - `KiteScript` base class for all `.kite.kts` files
        - `KiteScriptCompilationConfiguration` with implicit imports
        - `ScriptCompiler` with caching and error reporting
        - Coroutines support (kotlinx.coroutines-core and -test)
    - **Task 1.3.2**: Segment Definition DSL
        - `SegmentsBuilder` and `SegmentBuilder` for fluent segment definition
        - Support for all segment properties (description, timeout, dependencies, conditions, retries)
        - Type-safe DSL with `@SegmentDslMarker`
        - `segments{}` top-level function
    - **Task 1.3.3**: Ride Configuration DSL
        - `RideBuilder`, `FlowBuilder`, `ParallelFlowBuilder` for ride definition
        - `SegmentOverridesBuilder` for segment customization in rides
        - Sequential and parallel execution blocks
        - `ride{}` top-level function
    - **Task 1.3.4**: File Discovery
        - `FileDiscovery` class for scanning `.kite/segments/` and `.kite/rides/`
        - Parallel file loading with coroutines
        - Error collection and reporting (`FileLoadError`)
        - Result types: `SegmentLoadResult`, `RideLoadResult`, `KiteLoadResult`
        - Helper maps for quick lookups
    - Comprehensive tests (59 tests across 4 test files)

- **Epic 2.1 Complete**: Segment Graph Construction
    - **Task 2.1.1**: DAG Builder
        - `SegmentGraph` class with adjacency list representation
        - Dependency resolution algorithm
        - DFS-based cycle detection
        - Graph validation (missing dependencies, unreachable segments)
        - Comprehensive unit tests (24 tests)
    - **Task 2.1.2**: Topological Sort
        - `TopologicalSort` class using Kahn's algorithm
        - Execution level calculation for parallelization
        - Critical path analysis
        - Parallelization efficiency metrics
        - Unit tests (21 tests)

- **Epic 2.2 Complete**: Segment Scheduler
    - **Task 2.2.1**: Sequential Scheduler
        - `SegmentScheduler` interface
        - `SequentialScheduler` implementation
        - Topological order execution
        - Dependency checking with cascading skips
        - Conditional execution support
        - Duration tracking and exception handling
        - Unit tests (16 tests)
    - **Task 2.2.2**: Parallel Scheduler
        - `ParallelScheduler` using Kotlin coroutines
        - Semaphore-based concurrency control (configurable maxConcurrency)
        - Level-by-level parallel execution
        - ConcurrentHashMap for thread-safe result tracking
        - Independent segments execute concurrently
        - Comprehensive concurrency tests (17 tests)
    - **Results & Tracking**:
        - `SegmentResult` data class with status, duration, error details
        - `SchedulerResult` with aggregated statistics
        - Helper methods: `failedSegments()`, `successfulSegments()`
        - Beautiful `toString()` formatting with emoji

- **Epic 2.3 Complete**: Basic Execution Runtime
    - **Task 2.3.1**: Process Executor
        - `ProcessExecutor` class for external command execution
        - Timeout support with graceful/forceful termination
        - Async output capture using coroutines
        - Working directory and environment variable support
        - Shell command execution (`sh -c` / `cmd /c`)
        - Cross-platform (Windows/Unix)
        - Methods: `execute()`, `executeOrNull()`, `shell()`
        - Comprehensive tests (19 tests)
    - **Task 2.3.2**: Execution Context Extensions
        - `ProcessExecutionProvider` interface for runtime integration
        - Thread-local context for segment execution
        - Extension functions on `ExecutionContext`:
            - `exec()` - execute command, throw on failure
            - `execOrNull()` - execute command, return null on failure
            - `shell()` - execute shell commands
        - `ProcessExecutionResult` data class
        - `ExecutionContextExtensions.kt` (116 lines)

- **Task 3.1.1 Complete**: CLI Framework Setup
    - Beautiful CLI interface with Clikt framework
    - Mordant library for terminal colors and emoji support
    - ASCII art logo with cyan styling
    - Global options: `--debug`, `--verbose`, `--quiet`, `--version`
    - Command structure:
        - `kite ride <name>` - Execute a ride
        - `kite run <segments...>` - Run specific segments
        - `kite segments` - List available segments
        - `kite rides` - List available rides
        - `kite graph <name>` - Visualize dependency graph
    - `Output` utility object with colorful functions:
        - `success()` (✓ green), `error()` (✗ red), `warning()` (⚠ yellow)
        - `info()` (ℹ cyan), `header()`, `section()`, `progress()`
        - `result()` for segment results, `summary()` for execution summary
    - Help system with Mordant formatting
    - 7 files created (295 lines)

- **Modular Specification Structure**
    - Restructured documentation into `specs/` directory
    - Separate documents for different aspects (overview, core concepts, DSL, etc.)
    - Updated terminology: "pipeline" → "ride", "task" → "segment"
    - Unified `.kite.kts` file extension for all Kite files
    - Clear distinction between segments (`.kite/segments/`) and rides (`.kite/rides/`)

### Changed

- **Terminology Updates**
    - "Pipeline" is now called "Ride"
    - "Task" is now called "Segment"
    - "Config" is now called "Ride Configuration"
    - CLI commands updated: `./kite ride mr` instead of `./kite run --config mr`

- **File Structure**
    - Segment definitions: `.kite/segments/*.kite.kts` (was `.kite/tasks/*.tasks.kts`)
    - Ride configurations: `.kite/rides/*.kite.kts` (was `.kite/configs/*.config.kts`)
    - Settings file: `.kite/settings.kite.kts` (was `.kite/kite.settings.kts`)

- **Documentation**
    - Main specification split into modular documents in `specs/` directory
    - README simplified with quick reference to detailed specs
    - Development plan remains at root level for easy access

### Technical Details

- **Gradle**: 9.2.0 (latest stable)
- **Kotlin**: 2.0.21 (project) + 2.2.20 (Gradle)
- **Java**: 17 LTS
- **Dependencies**:
    - Clikt 4.2.1 (CLI framework)
    - Mordant 2.2.0 (terminal colors and styling)
    - kotlinx.coroutines (async execution)
    - kotlin-scripting-jvm (script compilation)
- **Configuration Cache**: Enabled for faster builds
- **Build Time**: 3s initial, 1s cached (67% improvement with configuration cache)

### Statistics

**Production Code**: 3,866 lines

- kite-core: 784 lines
- kite-dsl: 1,155 lines
- kite-runtime: 1,632 lines
- kite-cli: 295 lines

**Test Code**: 4,670 lines (1.21:1 test-to-code ratio)

- 175+ tests, all passing

**Phases Complete**: 2 of 8 (25%)

- Phase 1: Foundation & Core DSL
- Phase 2: Segment Graph & Execution Engine
- Phase 3: CLI & File Discovery (Task 3.1.1 complete)

## [0.1.0-SNAPSHOT] - Work in Progress

### Status

Under active development - specification and initial implementation phase.

### Roadmap

See [DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md) for detailed implementation roadmap:

- Phase 1: Foundation & Core DSL (Weeks 1-2)
- Phase 2: Segment Graph & Execution Engine (Weeks 3-4)
- Phase 3: CLI & File Discovery (Weeks 5-6)
- Phase 4: Platform Adapters (Week 7)
- Phase 5: Built-in Helpers & Features (Week 8)
- Phase 6: Documentation & Examples (Week 9)
- Phase 7: Testing & Refinement (Week 10)
- Phase 8: Plugin System MVP (Weeks 11-12) - Optional

[Unreleased]: https://github.com/yourusername/kite/compare/v0.1.0...HEAD

[0.1.0-SNAPSHOT]: https://github.com/yourusername/kite/releases/tag/v0.1.0
