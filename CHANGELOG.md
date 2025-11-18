# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **🎯 Lifecycle Hooks** (December 2025)
    - Added `onSuccess`, `onFailure`, `onComplete` hooks to Segment model
    - Added `onSuccess`, `onFailure`, `onComplete` hooks to Ride model
    - Full DSL support in `SegmentBuilder` and `RideBuilder`
    - Integrated execution in `SequentialScheduler` and `ParallelScheduler`
    - Ride-level hooks in `RideCommand`
    - Support for suspend functions in all hooks
    - Error-resilient execution - hook failures don't break segments
    - Access to `ExecutionContext` in segment hooks (can use `exec()`, `artifacts`, etc.)
    - **Use cases unlocked**: Slack notifications, GitHub PR comments, test result uploads, metrics collection, cleanup
      operations
    - Comprehensive documentation: `docs/LIFECYCLE_HOOKS.md`

- **📦 Artifact Management System** (December 2025)
    - `FileSystemArtifactManager` class (175 lines)
    - Thread-safe artifact tracking with `ConcurrentHashMap`
    - Automatic file/directory copying to `.kite/artifacts/`
    - Methods: `put(name, path)`, `get(name)`, `has(name)`, `list()`
    - DSL integration: `outputs { artifact("name", "path") }` and `inputs { artifact("name") }`
    - Integrated with `SequentialScheduler` and `ParallelScheduler`
    - **Artifact Manifest System** for cross-ride sharing:
        - `ArtifactManifest` class (142 lines) with JSON serialization
        - Thread-safe with `ReentrantReadWriteLock`
        - Atomic file operations (write to temp, atomic rename)
        - Auto-save manifest after ride completes (`.kite/artifacts/.manifest.json`)
        - Auto-restore manifest before ride starts
        - Enables artifact sharing across CI jobs and different rides
        - Minimal schema: only essential metadata (name, path, type, size, timestamp)
    - **Documentation** (3 comprehensive guides):
        - `docs/ARTIFACTS.md` - Complete guide (532 lines)
        - `docs/ARTIFACTS_SIMPLE.md` - Real-world patterns (311 lines)
        - `docs/ARTIFACTS_CROSS_RIDE.md` - Cross-ride sharing (470 lines)
    - 17 tests (13 unit + 4 integration)

- **🚀 GitHub Actions Integration** (December 2025)
    - Created `.github/workflows/mr.yml` for MR validation
    - Automatic test execution on pull requests and main pushes
    - Test results uploaded as artifacts (7 day retention)
    - Test reporting with `dorny/test-reporter` (shows results in PR checks)
    - 44% faster with parallel execution (30s sequential → 17s parallel)
    - Documentation: `docs/GITHUB_ACTIONS.md`

- **📊 CI Ride Enhancement** (December 2025)
    - Updated all test segments to save artifacts:
        - `test-results-core/`, `test-results-dsl/`, etc. (JUnit XML)
        - `test-reports-core/`, `test-reports-dsl/`, etc. (HTML reports)
    - New `publish-test-results` segment summarizes all test artifacts
    - 8 artifacts saved per CI run (ready for CI upload/download)
    - Documentation: `docs/CI_INTEGRATION.md` (497 lines)

- **🎉 Ivy-based Dependency Resolver** (November 2025)
    - Replaced Maven/Aether resolver with Apache Ivy for Java 17+ compatibility
    - `IvyDependenciesResolver` class (200 lines)
    - Maven-compatible dependency resolution from Maven Central
    - Transitive dependency support
    - Custom repository support via `@Repository` annotation
    - Cache management in `~/.ivy2/cache`
    - **Breaking Fix**: `@DependsOn` now works at runtime with Java 17+!
    - Previously had Java 17+ incompatibility due to Guice/Aether issues
    - Lightweight (~2MB vs Maven's complexity)
    - Battle-tested (same solution as kotlin-main.kts)

- **🧪 Integration Test Framework** (November 2025)
    - New `kite-integration-tests` module with JUnit-based testing
    - `IntegrationTestBase` class for programmatic ride execution (188 lines)
    - Temporary workspace per test with `@TempDir`
    - Output and error capture for assertions
    - Rich assertion API (`assertSuccess()`, `assertOutputContains()`, etc.)
    - **21 comprehensive integration tests covering**:
        - `BasicRideExecutionTest` (5 tests) - Single/multiple segments, dependencies
        - `ExternalDependenciesTest` (3 tests) - @DependsOn with Gson, Commons Lang3
        - `ErrorHandlingTest` (5 tests) - Exceptions, cascading failures
        - `ParallelExecutionTest` (4 tests) - Concurrency, maxConcurrency limits
        - `RealWorldScenariosTest` (4 tests) - CI pipelines, releases, matrix builds
    - All tests pass in ~9 seconds
    - Validates end-to-end functionality
    - Added to CI pipeline (segment: test-integration)

- **📚 Comprehensive Documentation** (November 2025)
    - `EXTERNAL_DEPENDENCIES.md` (570 lines) - Complete guide for @DependsOn
    - Updated status from "broken with Java 17" to "fully working"
    - Documented Ivy vs Maven resolver differences
    - Examples with Gson, Apache Commons, OkHttp, SLF4J
    - Troubleshooting section
    - Best practices and use cases

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

**Production Code**: 6,500+ lines

- kite-core: 900 lines (added lifecycle hooks)
- kite-dsl: 1,450 lines (IvyDependenciesResolver + lifecycle hooks DSL)
- kite-runtime: 1,800 lines (artifact management + lifecycle hook execution)
- kite-cli: 538 lines
- kite-integration-tests: 925 lines (test implementations)

**Test Code**: 5,700+ lines (0.88:1 test-to-code ratio)

- Unit tests: 28 tests (added artifact tests)
- Integration tests: 21 tests ✅
- **Total: 49 tests, all passing** ✅

**Documentation**: 6,800+ lines

- Original guides: 5 docs
- Artifact guides: 3 docs (1,313 lines)
- CI integration: 2 docs (700+ lines)
- Lifecycle hooks: 1 doc

**Phases Complete**: 5 of 8 (62.5%)

- Phase 1: Foundation & Core DSL ✅
- Phase 2: Segment Graph & Execution Engine ✅
- Phase 3: CLI & File Discovery ✅
- Phase 4: Platform Adapters (SKIPPED - CI-agnostic approach)
- **Phase 5: Built-in Helpers ✅ COMPLETE** (file ops, exec, logging, artifacts, lifecycle hooks)
- Phase 6: Documentation (90% - 11 comprehensive guides)
- **Phase 7: Integration Testing (70%)** - 49 tests covering all features

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
