# Kite Development Plan

## Overview

This document outlines the phased development plan for Kite, a Kotlin-based CI ride runner. The development is organized
into 8 phases with clear deliverables, dependencies, and success criteria.

## Terminology

- **Ride**: A workflow/pipeline composed of segments (formerly "pipeline")
- **Segment**: A unit of work in a ride (formerly "task")
- **Flow**: The execution order of segments within a ride

## Project Structure

```
kite/
├── specs/                         # Complete specifications (9 documents)
├── kite-core/                     # Core domain models
├── kite-dsl/                      # Kotlin DSL and scripting
├── kite-runtime/                  # Execution runtime
├── kite-cli/                      # CLI interface
├── DEVELOPMENT_PLAN.md            # This file
└── CHANGELOG.md                   # Version history
```

## Development Phases

- **Phase 1**: Foundation & Core DSL (Weeks 1-2) -
- **Phase 2**: Segment Graph & Execution Engine (Weeks 3-4)
- **Phase 3**: CLI & File Discovery (Weeks 5-6)
- **Phase 4**: Platform Adapters (Week 7)
- **Phase 5**: Built-in Helpers & Features (Week 8)
- **Phase 6**: Documentation & Examples (Week 9)
- **Phase 7**: Testing & Refinement (Week 10)
- **Phase 8**: Plugin System MVP (Weeks 11-12) - Optional

---

## Phase 1: Foundation & Core DSL (Weeks 1-2)

**Goal**: Set up project infrastructure and define core domain models

### Epic 1.1: Project Setup & Infrastructure

**Story Points**: 5 | **Duration**: 2 days

- [x] **Task 1.1.1**: Initialize Kotlin project structure
    - Create multi-module Gradle project
    - Configure Kotlin 2.0.21 with Java 17 LTS compatibility
    - Set up modules: `kite-core`, `kite-dsl`, `kite-runtime`, `kite-cli`
    - Configure ktlint and detekt for code quality
    - Set up Gradle wrapper (v9.2.0) with configuration cache

- [x] **Task 1.1.2**: Set up dependency management
    - Add kotlinx.coroutines to `kite-core` and `kite-runtime`
    - Add kotlinx.serialization to `kite-core`
    - Add Clikt CLI framework to `kite-cli`
    - Add Kotlin Scripting dependencies to `kite-dsl`

- [x] **Task 1.1.3**: Configure build and publishing
    - Set up version management (0.1.0-SNAPSHOT)
    - Configure Maven publishing with POM metadata
    - Set up GitHub Actions CI workflow
    - Configure test infrastructure (JUnit 5, MockK)

- [x] **Task 1.1.4**: Create project documentation structure
    - Create comprehensive specification (9 modular documents in `specs/`)
    - Update README.md with project overview
    - Create CONTRIBUTING.md
    - Initialize CHANGELOG.md

**Deliverables**:

- Multi-module Gradle project with all dependencies
- CI/CD pipeline configured
- Complete specification documentation
- Code quality tools operational

---

### Epic 1.2: Core Domain Models

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 1.2.1**: Define Segment model
    - Create `Segment` data class in `kite-core`
    - Add properties: `name`, `description`, `dependsOn`, `condition`, `timeout`, `retries`
    - Create `SegmentStatus` enum (PENDING, RUNNING, SUCCESS, FAILURE, SKIPPED, TIMEOUT)
    - Add execution lambda with `ExecutionContext` receiver
    - Write unit tests

- [ ] **Task 1.2.2**: Define ExecutionContext model
    - Create `ExecutionContext` data class
    - Add properties: `branch`, `commitSha`, `mrNumber`, `isRelease`, `isLocal`, `ciPlatform`
    - Add `environment`, `workspace`, `artifacts` accessors
    - Write unit tests

- [ ] **Task 1.2.3**: Define Ride Configuration model
    - Create `Ride` data class in `kite-core`
    - Add properties: `name`, `segments`, `environment`, `parallel settings`
    - Create `FlowNode` sealed class (Sequential, Parallel, Segment Reference)
    - Implement ride validation logic
    - Write unit tests

- [ ] **Task 1.2.4**: Define Platform Adapters interface
    - Create `PlatformAdapter` interface
    - Define detection and context population methods
    - Create stub implementations (GitLabCI, GitHub Actions, Local)
    - Write unit tests with mocks

**Deliverables**:

- Domain models with complete test coverage
- Platform adapter interfaces defined
- Models are immutable and thread-safe

---

### Epic 1.3: Kotlin Scripting Integration

**Story Points**: 13 | **Duration**: 5 days

- [ ] **Task 1.3.1**: Set up Kotlin scripting engine
    - Configure `kotlin-scripting-jvm` dependencies
    - Create `ScriptCompiler` class
    - Implement `.kite.kts` compilation
    - Add script caching mechanism
    - Write unit tests

- [ ] **Task 1.3.2**: Implement segment definition DSL
    - Create `SegmentBuilder` class with DSL markers
    - Implement `segment("name") { }` builder
    - Add support for `execute { }`, `outputs { }`, `inputs { }` blocks
    - Implement property delegates for `dependsOn`, `timeout`, `condition`
    - Write DSL tests

- [ ] **Task 1.3.3**: Implement ride configuration DSL
    - Create `RideBuilder` class
    - Implement `ride { }` builder
    - Support `segment()` references and `parallel { }` blocks
    - Add `environment { }` and `onFailure { }` blocks
    - Write DSL tests

- [ ] **Task 1.3.4**: Implement file discovery
    - Create `FileDiscovery` class in `kite-dsl`
    - Implement `.kite/segments/*.kite.kts` scanner
    - Implement `.kite/rides/*.kite.kts` scanner
    - Add file watching for hot-reload (optional)
    - Write integration tests

**Deliverables**:

- Working Kotlin scripting engine
- Segment and ride DSLs functional
- File discovery and loading complete

---

## Phase 2: Segment Graph & Execution Engine (Weeks 3-4)

**Goal**: Build the execution engine that schedules and runs segments

### Epic 2.1: Segment Graph Construction

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 2.1.1**: Implement DAG builder
    - Create `SegmentGraph` class
    - Implement dependency resolution algorithm
    - Detect and report circular dependencies
    - Build adjacency list representation
    - Write unit tests with various graph topologies

- [ ] **Task 2.1.2**: Implement topological sort
    - Implement Kahn's algorithm for topological sorting
    - Handle parallel execution groups
    - Calculate execution levels (for visualization)
    - Write unit tests

- [ ] **Task 2.1.3**: Implement graph validation
    - Validate all segment references exist
    - Check for unreachable segments
    - Verify no self-dependencies
    - Validate parallel block constraints
    - Write validation tests

**Deliverables**:

- Segment graph data structure
- Topological sort algorithm
- Comprehensive validation

---

### Epic 2.2: Segment Scheduler

**Story Points**: 10 | **Duration**: 4 days

- [ ] **Task 2.2.1**: Implement sequential scheduler
    - Create `SegmentScheduler` interface
    - Implement `SequentialScheduler`
    - Execute segments in topological order
    - Handle segment skipping (conditions)
    - Write tests

- [ ] **Task 2.2.2**: Implement parallel scheduler
    - Implement `ParallelScheduler` using coroutines
    - Add `maxConcurrency` support with Semaphore
    - Implement parallel block execution
    - Handle failure modes (fail-fast vs continue)
    - Write concurrency tests

- [ ] **Task 2.2.3**: Implement execution tracking
    - Create `ExecutionTracker` for monitoring progress
    - Track segment states (pending, running, complete, failed)
    - Implement execution time measurement
    - Add segment result aggregation
    - Write tests

**Deliverables**:

- Sequential and parallel schedulers
- Execution tracking infrastructure
- Thread-safe concurrent execution

---

### Epic 2.3: Basic Execution Runtime

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 2.3.1**: Implement process executor
    - Create `ProcessExecutor` class in `kite-runtime`
    - Implement `exec()` function with command execution
    - Add timeout support using `ProcessHandle`
    - Capture stdout/stderr
  - Write tests with mock processes

- [ ] **Task 2.3.2**: Implement segment execution context
    - Populate `ExecutionContext` from environment
    - Add Git information detection (branch, SHA)
    - Implement context isolation per segment
    - Write tests

- [ ] **Task 2.3.3**: Implement basic error handling
    - Add try-catch around segment execution
    - Implement retry logic with exponential backoff
    - Add `onFailure` callback support
    - Write error handling tests

**Deliverables**:

- Process execution capability
- Execution context population
- Basic error handling and retries

---

## Phase 3: CLI & File Discovery (Weeks 5-6)

**Goal**: Build the command-line interface

### Epic 3.1: CLI Framework

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 3.1.1**: Set up Clikt CLI structure
    - Create main CLI class with Clikt
    - Define command hierarchy: `ride`, `run`, `segments`, `rides`, `graph`
    - Add global options: `--debug`, `--dry-run`, `--verbose`
    - Implement help text and usage examples
    - Write CLI tests

- [ ] **Task 3.1.2**: Implement `ride` command
    - `kite ride <name>` - Execute named ride
    - Load ride configuration from `.kite/rides/<name>.kite.kts`
    - Display progress during execution
    - Show summary at completion
    - Write integration tests

- [ ] **Task 3.1.3**: Implement `run` command
    - `kite run <segment1> <segment2>...` - Execute specific segments
    - Build minimal graph from specified segments
    - Support direct segment execution without ride
    - Write tests

- [ ] **Task 3.1.4**: Implement listing commands
    - `kite segments` - List all available segments
    - `kite rides` - List all available rides
    - Format output nicely (table format)
    - Add `--json` flag for machine-readable output
    - Write tests

**Deliverables**:

- Complete CLI with all commands
- User-friendly output formatting
- Help documentation

---

### Epic 3.2: File Discovery & Loading

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 3.2.1**: Implement segment discovery
    - Scan `.kite/segments/` directory
    - Compile all `*.kite.kts` files
    - Build segment registry (name -> Segment)
    - Cache compiled scripts
    - Write discovery tests

- [ ] **Task 3.2.2**: Implement ride discovery
    - Scan `.kite/rides/` directory
    - Load ride configurations
    - Validate segment references
    - Write tests

- [ ] **Task 3.2.3**: Implement settings loading
    - Load `.kite/settings.kite.kts` if present
    - Apply global configuration
    - Merge with ride-specific settings
    - Write tests

**Deliverables**:

- Automatic file discovery
- Script compilation and caching
- Settings management

---

### Epic 3.3: Parallel Execution

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 3.3.1**: Implement process-level parallelism
    - Spawn separate OS processes for parallel segments
    - Manage process lifecycle (start, monitor, terminate)
    - Implement process pool with `maxConcurrency`
    - Write tests

- [ ] **Task 3.3.2**: Implement log multiplexing
    - Capture per-segment stdout/stderr
    - Write separate log files: `logs/<segment-name>.log`
    - Implement console output modes (interleaved, sequential, summary)
    - Write tests

- [ ] **Task 3.3.3**: Add dry-run mode
    - Implement `--dry-run` flag
    - Display execution plan without running
    - Show estimated times and resource usage
    - Write tests

**Deliverables**:

- Parallel process execution
- Per-segment logging
- Dry-run visualization

---

## Phase 4: Platform Adapters (Week 7)

**Goal**: Detect CI platform and populate execution context

### Epic 4.1: Platform Adapter Framework

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 4.1.1**: Create platform detection system
    - Implement `PlatformDetector` class
    - Check environment variables to detect platform
    - Return appropriate adapter
    - Write tests

- [ ] **Task 4.1.2**: Implement adapter registry
    - Register all platform adapters
    - Support custom/plugin adapters
    - Priority-based detection
    - Write tests

**Deliverables**:

- Platform detection system
- Adapter registry

---

### Epic 4.2: CI Platform Adapters

**Story Points**: 10 | **Duration**: 4 days

- [ ] **Task 4.2.1**: Implement GitLab CI adapter
    - Read GitLab CI environment variables
    - Populate context: branch, commit SHA, MR number, job ID
    - Detect release MRs from labels
    - Write tests

- [ ] **Task 4.2.2**: Implement GitHub Actions adapter
    - Read GitHub Actions environment variables
    - Populate context: branch, commit SHA, PR number, workflow
    - Detect release PRs from labels
    - Write tests

- [ ] **Task 4.2.3**: Implement Local adapter
    - Use Git commands to get branch/SHA
    - Set `isLocal = true`
    - Use current working directory as workspace
    - Write tests

- [ ] **Task 4.2.4**: Implement Generic adapter
    - Fallback for unknown CI platforms
    - Read standard environment variables (CI, BUILD_ID, etc.)
    - Write tests

**Deliverables**:

- 4 platform adapters (GitLab, GitHub, Local, Generic)
- Full test coverage
- Automatic platform detection

---

## Phase 5: Built-in Helpers & Features (Week 8)

**Goal**: Implement built-in helper functions

### Epic 5.1: Command Execution Helpers

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 5.1.1**: Implement exec functions
    - `exec(command, args...)` - throw on failure
    - `execOrNull(command, args...)` - return null on failure
    - `shell(command)` - execute shell command
    - `execAndCapture()` - capture output
    - Write tests

- [ ] **Task 5.1.2**: Add advanced exec options
    - Support working directory
    - Support environment variables
    - Support timeout per command
    - Write tests

**Deliverables**:

- Complete command execution API
- Tests for all execution modes

---

### Epic 5.2: File Operation Helpers

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 5.2.1**: Implement basic file operations
    - `copy()`, `move()`, `delete()`
    - `createDirectory()`
  - `zipFiles()`, `unzipFiles()`
    - Write tests

- [ ] **Task 5.2.2**: Implement file I/O
    - File reading: `file.readText()`, `file.readLines()`
    - File writing: `file.writeText()`, `file.appendText()`
    - Write tests

**Deliverables**:

- File operation helpers
- Tests with temporary files

---

### Epic 5.3: Artifact Management

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 5.3.1**: Implement ArtifactManager
    - Create `ArtifactManager` class
    - Store artifacts in `.kite/artifacts/`
    - `put(name, file)`, `get(name)`, `has(name)`, `list()`
    - Write tests

- [ ] **Task 5.3.2**: Integrate with segment execution
    - Call `outputs { }` block after segment execution
    - Make artifacts available to dependent segments
    - Write integration tests

- [ ] **Task 5.3.3**: Implement artifact cleanup
    - Clean up artifacts after ride completion
    - Add `--keep-artifacts` flag to preserve them
    - Write tests

**Deliverables**:

- Working artifact management
- Integration with segment execution
- Cleanup mechanism

---

### Epic 5.4: Logging System

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 5.4.1**: Implement structured logging
    - Create `Logger` interface
    - Implement console logger with levels (info, debug, warn, error)
    - Implement JSON logger for machine parsing
    - Write tests

- [ ] **Task 5.4.2**: Integrate logging throughout
    - Add logging to execution engine
    - Add logging to all helpers
    - Add `--verbose` and `--quiet` flags
    - Write tests

**Deliverables**:

- Structured logging system
- Integration throughout codebase

---

## Phase 6: Documentation & Examples (Week 9)

**Goal**: Create user documentation and examples

### Epic 6.1: User Documentation

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 6.1.1**: Write Getting Started guide
    - Installation instructions
  - First ride tutorial
  - Basic concepts explanation
  - Publish to `docs/getting-started.md`

- [ ] **Task 6.1.2**: Write CLI reference
    - Document all commands with examples
    - Document all flags and options
    - Publish to `docs/cli-reference.md`

- [ ] **Task 6.1.3**: Write DSL reference
    - Document all DSL functions
    - Show examples for each feature
    - Publish to `docs/dsl-reference.md`

**Deliverables**:

- Comprehensive user documentation
- Published to `docs/` directory

---

### Epic 6.2: Example Projects

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 6.2.1**: Create Android example
    - Sample Android project with Kite
    - MR ride: build + parallel tests
    - Release ride: build + integration tests + deploy
    - Publish to `examples/android/`

- [ ] **Task 6.2.2**: Create backend example
    - Sample Kotlin backend project
    - Build, test, Docker build, deploy ride
    - Publish to `examples/backend/`

- [ ] **Task 6.2.3**: Create monorepo example
    - Multi-module project example
    - Per-module segments
    - Full ride orchestration
    - Publish to `examples/monorepo/`

**Deliverables**:

- 3 complete example projects
- README for each example

---

### Epic 6.3: API Documentation

**Story Points**: 3 | **Duration**: 1 day

- [ ] **Task 6.3.1**: Generate KDoc
    - Add KDoc comments to all public APIs
    - Generate HTML documentation with Dokka
    - Publish to `docs/api/`

**Deliverables**:

- Complete API documentation
- Published HTML docs

---

## Phase 7: Testing & Refinement (Week 10)

**Goal**: Comprehensive testing and bug fixes

### Epic 7.1: Integration Testing

**Story Points**: 10 | **Duration**: 4 days

- [ ] **Task 7.1.1**: Write end-to-end tests
    - Test complete ride execution
    - Test error scenarios
  - Test parallel execution
  - Test artifact passing

- [ ] **Task 7.1.2**: Write CLI integration tests
    - Test all CLI commands
    - Test flag combinations
    - Test error handling

- [ ] **Task 7.1.3**: Write platform adapter tests
    - Test in Docker with simulated CI env vars
    - Test local execution
    - Test context population

**Deliverables**:

- Comprehensive integration test suite
- > 80% code coverage

---

### Epic 7.2: Bug Fixes & Polish

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 7.2.1**: Fix identified bugs
    - Review and fix reported issues
    - Add regression tests

- [ ] **Task 7.2.2**: Performance optimization
    - Profile startup time
    - Optimize script compilation
    - Reduce memory footprint

- [ ] **Task 7.2.3**: UX improvements
    - Improve error messages
    - Better progress indicators
  - Colored output

**Deliverables**:

- All known bugs fixed
- Performance targets met
- Polished UX

---

### Epic 7.3: Release Preparation

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 7.3.1**: Version bump to 1.0.0
    - Update version in build files
    - Update CHANGELOG
    - Create release notes

- [ ] **Task 7.3.2**: Build distribution artifacts
    - Build executable JAR
  - Create Homebrew formula
  - Create Docker image
  - Test installations

- [ ] **Task 7.3.3**: Tag release
    - Create v1.0.0 tag
    - Push to GitHub
    - Create GitHub release

**Deliverables**:

- v1.0.0 release ready
- Distribution artifacts published

---

## Phase 8: Plugin System MVP (Weeks 11-12) - Optional

**Goal**: Implement basic plugin system

### Epic 8.1: Plugin Framework

**Story Points**: 10 | **Duration**: 4 days

- [ ] **Task 8.1.1**: Define plugin API
    - Create `KitePlugin` interface
    - Define `PluginContext` with registration methods
    - Write plugin API documentation

- [ ] **Task 8.1.2**: Implement plugin loading
    - ServiceLoader-based discovery
    - Plugin initialization
    - Plugin configuration

- [ ] **Task 8.1.3**: Enable plugin DSL extensions
    - Allow plugins to register helpers
    - Allow plugins to extend DSL
    - Write tests

**Deliverables**:

- Working plugin system
- Plugin API documentation

---

### Epic 8.2: Example Plugin (Play Store)

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 8.2.1**: Create Play Store plugin
    - Implement `PlayStorePlugin`
    - Add DSL extensions
    - Integrate with Google Play API
    - Write tests

- [ ] **Task 8.2.2**: Document plugin
    - Write plugin usage guide
    - Create example project
    - Publish to separate repo

**Deliverables**:

- Working Play Store plugin
- Plugin documentation

---

### Epic 8.3: Plugin Testing

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 8.3.1**: Test plugin system
    - Test plugin loading
    - Test plugin isolation
    - Test plugin errors

**Deliverables**:

- Tested plugin system
- Plugin development guide

---

## Success Criteria

### MVP (Phase 1-7)

- [ ] Can define segments in `.kite.kts` files
- [ ] Can define rides with sequential and parallel flows
- [ ] Can execute rides locally and in CI
- [ ] GitLab CI and GitHub Actions integration works
- [ ] Built-in helpers (exec, file ops) functional
- [ ] Artifact passing between segments works
- [ ] Documentation complete and published
- [ ] 3 example projects available
- [ ] Performance: <1s startup, <100ms overhead per segment
- [ ] Test coverage: >80%

### With Plugins (Phase 8)

- [ ] Plugin system functional
- [ ] At least one official plugin (Play Store) available
- [ ] Plugin development guide published

---

## Risk Mitigation

### Technical Risks

1. **Kotlin Scripting Performance**:
    - *Mitigation*: Implement aggressive caching, lazy loading

2. **Parallel Execution Complexity**:
    - *Mitigation*: Use well-tested coroutines library, extensive testing

3. **Platform Detection Reliability**:
    - *Mitigation*: Fallback to generic adapter, allow manual override

### Schedule Risks

1. **Scope Creep**:
    - *Mitigation*: Stick to MVP features, defer Phase 8 if needed

2. **Integration Issues**:
    - *Mitigation*: Early integration testing, modular architecture

---

## Definition of Done

### For Tasks

- [ ] Code written and reviewed
- [ ] Unit tests written with >80% coverage
- [ ] Documentation updated
- [ ] No linter errors

### For Epics

- [ ] All tasks complete
- [ ] Integration tests pass
- [ ] Epic deliverables met

### For Phases

- [ ] All epics complete
- [ ] Phase deliverables met
- [ ] Demo/presentation prepared

---

## Timeline Summary

| Phase    | Duration | Cumulative | Status      |
|----------|----------|------------|-------------|
| Phase 1  | 2 weeks  | 2 weeks    | (Epic 1.1 ) |
| Phase 2  | 2 weeks  | 4 weeks    |
| Phase 3  | 2 weeks  | 6 weeks    |
| Phase 4  | 1 week   | 7 weeks    |
| Phase 5  | 1 week   | 8 weeks    |
| Phase 6  | 1 week   | 9 weeks    |
| Phase 7  | 1 week   | 10 weeks   |
| Phase 8* | 2 weeks  | 12 weeks   | Optional    |

\* Phase 8 (Plugin System) is optional for MVP

**Target MVP**: 10 weeks (2.5 months)  
**With Plugins**: 12 weeks (3 months)

---

## Next Steps

**Current Status**: Epic 1.1 complete, ready to start Epic 1.2

**Immediate Actions**:

1. Begin Epic 1.2: Core Domain Models
2. Define Segment, ExecutionContext, Ride models
3. Set up unit testing infrastructure

**This Week**:

- Complete Epic 1.2 (Core Domain Models)
- Start Epic 1.3 (Kotlin Scripting Integration)

**This Month**:

- Complete Phase 1 (Foundation & Core DSL)
- Start Phase 2 (Segment Graph & Execution Engine)
