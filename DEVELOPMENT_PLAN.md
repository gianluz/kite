# Kite Development Plan

## Overview

This document outlines the phased development plan for Kite, organized by epics and individual tasks. Each phase builds
upon the previous, delivering incremental value.

**Total Estimated Timeline**: 10-12 weeks for MVP

---

## Phase 1: Foundation & Core DSL (Weeks 1-2)

**Goal**: Establish project structure, core data models, and basic DSL parsing

### Epic 1.1: Project Setup & Infrastructure

**Story Points**: 5 | **Duration**: 2 days

- [x] **Task 1.1.1**: Initialize Kotlin project structure ✅ COMPLETE
    - ✅ Create multi-module Gradle project
  - ✅ Configure Kotlin 2.0.21 and Java 17 LTS compatibility with toolchain
    - ✅ Set up module structure: `kite-core`, `kite-cli`, `kite-dsl`, `kite-runtime`
    - ✅ Configure ktlint/detekt for code quality
  - ✅ Gradle wrapper created (v9.2.0)
  - ✅ Configuration cache enabled
    - ✅ Build successful
    - ✅ CLI runs successfully

- [x] **Task 1.1.2**: Set up dependency management ✅ COMPLETE
    - ✅ Add kotlinx.coroutines (kite-core, kite-runtime)
    - ✅ Add kotlinx.serialization (kite-core)
    - ✅ Add Clikt (kite-cli v4.2.1)
    - ✅ Add Kotlin Scripting dependencies (kite-dsl)

- [x] **Task 1.1.3**: Configure build and publishing ✅ COMPLETE
    - ✅ Set up version management (0.1.0-SNAPSHOT)
    - ✅ Configure Maven publishing (with POM metadata)
    - ✅ Create GitHub Actions for CI (.github/workflows/ci.yml)
    - ✅ Set up test infrastructure (JUnit 5, MockK)
    - ✅ Added .gitignore

- [x] **Task 1.1.4**: Create project documentation structure ✅ COMPLETE
    - ✅ README with quick start (already exists)
    - ✅ CONTRIBUTING.md (comprehensive guide)
    - ✅ LICENSE (already exists)
    - ✅ CHANGELOG.md (created with initial entries)

### Epic 1.2: Core Domain Models

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 1.2.1**: Define Task model
    - Create `Task` data class with properties: id, description, dependencies, timeout, retries
    - Implement task validation logic
    - Add serialization support
    - Write unit tests

- [ ] **Task 1.2.2**: Define ExecutionContext model
    - Create `ExecutionContext` with CI platform detection
    - Implement branch, commit, MR detection
    - Add environment variable access
    - Write unit tests

- [ ] **Task 1.2.3**: Define Configuration model
    - Create `PipelineConfig` data class
    - Support task references and dependencies
    - Support parallel blocks
    - Write unit tests

- [ ] **Task 1.2.4**: Define Platform Adapters interface
    - Create `CIPlatform` enum (GitLab, GitHub, Local, Generic)
    - Define `PlatformAdapter` interface
    - Add platform detection logic
    - Write unit tests

### Epic 1.3: Kotlin Scripting Engine

**Story Points**: 13 | **Duration**: 4 days

- [ ] **Task 1.3.1**: Set up Kotlin Script engine
    - Configure kotlin-scripting-jvm
    - Create script host configuration
    - Implement script compilation
    - Handle script errors gracefully

- [ ] **Task 1.3.2**: Implement task definition DSL (`.tasks.kts`)
    - Create `tasks { }` DSL builder
    - Implement `task("name") { }` builder
    - Support `execute { }`, `dependsOn`, `timeout`, `description`
    - Write DSL tests

- [ ] **Task 1.3.3**: Implement configuration DSL (`.config.kts`)
    - Create `config { }` DSL builder
    - Implement `pipeline { }` builder
    - Support `task()` references and `parallel { }` blocks
    - Write DSL tests

- [ ] **Task 1.3.4**: Implement settings DSL (`kite.settings.kts`)
    - Create `settings { }` DSL builder
    - Support directory configuration
    - Support environment variables
    - Support parallel execution defaults
    - Write DSL tests

- [ ] **Task 1.3.5**: Create DSL evaluation engine
    - Load and compile `.kts` files
    - Extract task/config definitions
    - Handle script dependencies
    - Provide helpful error messages
    - Write integration tests

**Deliverables**:

- Core models defined and tested
- Kotlin scripting engine working
- Can parse `.tasks.kts` and `.config.kts` files
- Basic project structure established

---

## Phase 2: Task Graph & Execution Engine (Weeks 3-4)

**Goal**: Build task scheduling, dependency resolution, and basic execution

### Epic 2.1: Task Graph Construction

**Story Points**: 13 | **Duration**: 4 days

- [ ] **Task 2.1.1**: Implement task registry
    - Create `TaskRegistry` to store all defined tasks
    - Support task lookup by name
    - Support module-namespaced tasks (e.g., `app:build`)
    - Write unit tests

- [ ] **Task 2.1.2**: Implement dependency resolution
    - Create `DependencyResolver`
    - Build directed acyclic graph (DAG) from tasks
    - Detect circular dependencies
    - Write unit tests with various dependency scenarios

- [ ] **Task 2.1.3**: Implement topological sort
    - Sort tasks in execution order
    - Handle multiple valid orderings
    - Write unit tests

- [ ] **Task 2.1.4**: Identify parallelizable tasks
    - Analyze DAG to find independent tasks
    - Group tasks by execution level
    - Support `parallel { }` blocks from config
    - Write unit tests

- [ ] **Task 2.1.5**: Task graph validation
    - Validate all referenced tasks exist
    - Check for cycles
    - Verify conditional task dependencies
    - Provide clear error messages
    - Write integration tests

### Epic 2.2: Task Scheduler

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 2.2.1**: Implement sequential scheduler
    - Execute tasks in topological order
    - Handle task failures (stop on error)
    - Track execution state
    - Write unit tests

- [ ] **Task 2.2.2**: Implement parallel scheduler
    - Use Kotlin coroutines for parallelism
    - Respect `maxConcurrency` setting
    - Execute independent tasks in parallel
    - Handle failures (continue others or stop all)
    - Write unit tests

- [ ] **Task 2.2.3**: Implement task timeout handling
    - Add timeout support using coroutines
    - Kill processes that exceed timeout
    - Trigger `onTimeout` callbacks
    - Write unit tests

- [ ] **Task 2.2.4**: Implement retry logic
    - Retry failed tasks based on `maxRetries`
    - Respect `retryDelay` settings
    - Only retry on specified exceptions
    - Write unit tests

### Epic 2.3: Basic Execution Runtime

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 2.3.1**: Implement process execution
    - Create `ProcessExecutor` for running commands
    - Capture stdout/stderr
    - Handle process exit codes
    - Support environment variables
    - Write unit tests

- [ ] **Task 2.3.2**: Implement task execution context
    - Provide `context` object to tasks
    - Make CI platform info available
    - Provide artifact manager
    - Write unit tests

- [ ] **Task 2.3.3**: Implement basic logging
    - Console output for task execution
    - Task start/finish/failure messages
    - Execution time tracking
    - Write tests

- [ ] **Task 2.3.4**: Implement condition evaluation
    - Evaluate task `condition` lambda
    - Skip tasks when condition is false
    - Log skipped tasks
    - Write unit tests

**Deliverables**:

- Task graph construction working
- Sequential and parallel execution functional
- Basic task execution with timeout and retry
- Simple logging to console

---

## Phase 3: CLI & File Discovery (Weeks 5-6)

**Goal**: Build command-line interface and file discovery system

### Epic 3.1: CLI Framework

**Story Points**: 13 | **Duration**: 4 days

- [ ] **Task 3.1.1**: Implement main CLI entry point
    - Set up Clikt command structure
    - Parse command-line arguments
    - Handle `--help`, `--version`
    - Write tests

- [ ] **Task 3.1.2**: Implement `run` command
    - `kite run --config <name>` - run with config
    - `kite run <task1> <task2>` - run specific tasks
    - `--debug` flag for verbose logging
    - `--dry-run` flag to show execution plan
    - Write tests

- [ ] **Task 3.1.3**: Implement `tasks` command
    - `kite tasks` - list all available tasks
    - Show descriptions
    - Filter by module
    - Write tests

- [ ] **Task 3.1.4**: Implement `configs` command
    - `kite configs` - list all available configs
    - Show config descriptions
    - Write tests

- [ ] **Task 3.1.5**: Implement `graph` command
    - `kite graph --config <name>` - visualize task graph
    - Show dependencies
    - Show parallel execution groups
    - ASCII art visualization
    - Write tests

- [ ] **Task 3.1.6**: Implement error handling and user feedback
    - Clear error messages
    - Suggestions for common mistakes
    - Exit codes (0 = success, 1 = failure)
    - Write tests

### Epic 3.2: File Discovery System

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 3.2.1**: Implement task file discovery
    - Scan `.kite/tasks/` directory
    - Find all `*.tasks.kts` files
    - Support subdirectories
    - Cache compilation results
    - Write tests

- [ ] **Task 3.2.2**: Implement config file discovery
    - Scan `.kite/configs/` directory
    - Find all `*.config.kts` files
    - Load config by name
    - Write tests

- [ ] **Task 3.2.3**: Implement settings loading
    - Look for `.kite/kite.settings.kts`
    - Apply default settings if not found
    - Merge with command-line options
    - Write tests

- [ ] **Task 3.2.4**: Implement workspace detection
    - Find project root (look for `.kite/` directory)
    - Support running from subdirectories
    - Write tests

### Epic 3.3: Parallel Execution Implementation

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 3.3.1**: Implement parallel process spawning
    - Spawn separate processes for parallel tasks
    - Use Kotlin coroutines for coordination
    - Respect `maxConcurrency` limit
    - Write tests

- [ ] **Task 3.3.2**: Implement process isolation
    - Separate stdout/stderr per task
    - Independent environment variables
    - Write tests

- [ ] **Task 3.3.3**: Implement parallel failure handling
    - Option to continue on failure
    - Option to stop all on first failure
    - Collect and report all failures
    - Write tests

- [ ] **Task 3.3.4**: Performance optimization
    - Efficient coroutine dispatching
    - Resource monitoring
    - Write performance tests

**Deliverables**:

- Functional CLI with all commands
- File discovery working
- Parallel execution fully implemented
- Can run complete pipelines

---

## Phase 4: Platform Adapters (Week 7)

**Goal**: Implement CI platform integrations

### Epic 4.1: Platform Adapter Framework

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 4.1.1**: Define platform adapter interface
    - `detectPlatform()` method
    - `getEnvironmentInfo()` method
    - `getBranch()`, `getCommit()`, `getMRNumber()` methods
    - Write interface tests

- [ ] **Task 4.1.2**: Implement base adapter
    - Common functionality for all platforms
    - Environment variable reading
    - Write tests

### Epic 4.2: GitLab CI Adapter

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 4.2.1**: Implement GitLab CI detection
    - Check for `CI_SERVER_NAME=GitLab`
    - Implement `detectPlatform()`
    - Write tests

- [ ] **Task 4.2.2**: Implement GitLab environment parsing
    - Parse `CI_COMMIT_BRANCH`, `CI_COMMIT_SHA`
    - Parse `CI_MERGE_REQUEST_*` variables
    - Detect if MR is labeled as release
    - Write tests

- [ ] **Task 4.2.3**: Integration testing
    - Test with GitLab CI environment variables
    - Verify context population
    - Write integration tests

### Epic 4.3: GitHub Actions Adapter

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 4.3.1**: Implement GitHub Actions detection
    - Check for `GITHUB_ACTIONS=true`
    - Implement `detectPlatform()`
    - Write tests

- [ ] **Task 4.3.2**: Implement GitHub environment parsing
    - Parse `GITHUB_REF`, `GITHUB_SHA`
    - Parse PR information
    - Check PR labels
    - Write tests

- [ ] **Task 4.3.3**: Integration testing
    - Test with GitHub Actions environment variables
    - Verify context population
    - Write integration tests

### Epic 4.4: Local Adapter

**Story Points**: 3 | **Duration**: 1 day

- [ ] **Task 4.4.1**: Implement local detection
    - Default when no CI detected
    - Write tests

- [ ] **Task 4.4.2**: Implement local environment parsing
    - Use git commands for branch/commit
    - Mark as local platform
    - Write tests

**Deliverables**:

- Platform adapters for GitLab CI, GitHub Actions, and Local
- Automatic platform detection
- Context properly populated based on platform

---

## Phase 5: Built-in Helpers & Features (Week 8)

**Goal**: Implement helper functions and additional features

### Epic 5.1: Command Execution Helpers

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 5.1.1**: Implement `exec()` helper
    - Execute command with arguments
    - Throw on non-zero exit
    - Return stdout/stderr
    - Write tests

- [ ] **Task 5.1.2**: Implement `execOrNull()` helper
    - Execute command
    - Return null on failure
    - Write tests

- [ ] **Task 5.1.3**: Implement `shell()` helper
    - Execute shell command (bash/sh)
    - Support pipes and redirects
    - Write tests

### Epic 5.2: File Operations Helpers

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 5.2.1**: Implement file helpers
    - `copy()`, `move()`, `delete()`
    - `createDirectory()`
    - Write tests

- [ ] **Task 5.2.2**: Implement archive helpers
    - `zipFiles()` - create zip archive
    - `unzipFiles()` - extract zip archive
    - Write tests

### Epic 5.3: Artifact Management

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 5.3.1**: Implement ArtifactManager
    - Store artifacts by name
    - Retrieve artifacts
    - Track artifact dependencies
    - Write tests

- [ ] **Task 5.3.2**: Implement artifact lifecycle
    - Register artifacts in `outputs { }`
    - Make available to dependent tasks
    - Clean up after pipeline
    - Write tests

- [ ] **Task 5.3.3**: Implement artifact copying
    - Copy artifacts to designated locations
    - Support glob patterns
    - Write tests

### Epic 5.4: Enhanced Logging

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 5.4.1**: Implement structured logging
    - JSON log format option
    - Human-readable console format
    - Write tests

- [ ] **Task 5.4.2**: Implement per-task logs
    - Create log file per task
    - Stream to console in real-time
    - Write tests

- [ ] **Task 5.4.3**: Implement log configuration
    - `perTaskLogs` setting
    - `consoleOutput` modes: interleaved, sequential, summary-only
    - Write tests

- [ ] **Task 5.4.4**: Implement dry-run visualization
    - Show execution plan
    - Estimate execution time
    - Show resource usage
    - Write tests

**Deliverables**:

- Helper functions available in tasks
- Artifact management working
- Enhanced logging with per-task logs
- Dry-run mode showing execution plan

---

## Phase 6: Documentation & Examples (Week 9)

**Goal**: Create comprehensive documentation and example projects

### Epic 6.1: User Documentation

**Story Points**: 13 | **Duration**: 4 days

- [ ] **Task 6.1.1**: Write getting started guide
    - Installation instructions
    - First pipeline tutorial
    - Common patterns

- [ ] **Task 6.1.2**: Write DSL reference
    - Task definition syntax
    - Configuration syntax
    - All available options
    - Examples for each feature

- [ ] **Task 6.1.3**: Write CLI reference
    - All commands documented
    - All flags explained
    - Usage examples

- [ ] **Task 6.1.4**: Write best practices guide
    - Task organization
    - Configuration strategies
    - Performance tips
    - Troubleshooting

- [ ] **Task 6.1.5**: Write platform integration guides
    - GitLab CI setup
    - GitHub Actions setup
    - Local development workflows

### Epic 6.2: Example Projects

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 6.2.1**: Create Android example
    - Complete Android app pipeline
    - Build, test, lint, deploy tasks
    - Multiple configs (MR, release, local)
    - Document in README

- [ ] **Task 6.2.2**: Create backend/server example
    - Docker-based pipeline
    - Build, test, deploy
    - Document in README

- [ ] **Task 6.2.3**: Create monorepo example
    - Multi-module project
    - Coordinated builds
    - Document in README

### Epic 6.3: API Documentation

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 6.3.1**: Generate KDoc documentation
    - Document all public APIs
    - Generate HTML docs
    - Publish to GitHub Pages

- [ ] **Task 6.3.2**: Write plugin development guide
    - How to create plugins
    - API reference
    - Publishing guide

**Deliverables**:

- Complete user documentation
- 3+ example projects
- Published API docs

---

## Phase 7: Testing & Refinement (Week 10)

**Goal**: Comprehensive testing, bug fixes, and polish

### Epic 7.1: Integration Testing

**Story Points**: 13 | **Duration**: 4 days

- [ ] **Task 7.1.1**: Create end-to-end test suite
    - Test complete pipelines
    - Test all CLI commands
    - Test error scenarios
    - Write tests

- [ ] **Task 7.1.2**: Test platform adapters
    - Test with real CI environments
    - Verify GitLab CI integration
    - Verify GitHub Actions integration
    - Write tests

- [ ] **Task 7.1.3**: Performance testing
    - Measure startup time
    - Measure execution overhead
    - Optimize bottlenecks
    - Write performance benchmarks

- [ ] **Task 7.1.4**: Load testing
    - Test with large pipelines (100+ tasks)
    - Test with high parallelism
    - Identify and fix issues

### Epic 7.2: Bug Fixes & Polish

**Story Points**: 8 | **Duration**: 3 days

- [ ] **Task 7.2.1**: Fix discovered bugs
    - Triage and prioritize
    - Fix critical issues
    - Write regression tests

- [ ] **Task 7.2.2**: Improve error messages
    - Make errors more helpful
    - Add suggestions
    - Test error scenarios

- [ ] **Task 7.2.3**: Performance optimization
    - Optimize hot paths
    - Reduce memory usage
    - Improve startup time

- [ ] **Task 7.2.4**: Polish user experience
    - Improve CLI output
    - Better progress indicators
    - Consistent formatting

### Epic 7.3: Release Preparation

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 7.3.1**: Prepare release artifacts
    - Build executable JAR
    - Create installation scripts
    - Test on different platforms (macOS, Linux)

- [ ] **Task 7.3.2**: Write release notes
    - Document features
    - Known limitations
    - Upgrade guide

- [ ] **Task 7.3.3**: Set up distribution
    - GitHub Releases
    - Maven Central (optional)
    - Docker image

**Deliverables**:

- Fully tested system
- All bugs fixed
- Release artifacts ready
- v1.0.0-alpha or v0.1.0 ready to ship

---

## Phase 8 (Optional): Plugin System MVP (Weeks 11-12)

**Goal**: Implement plugin architecture and one example plugin

### Epic 8.1: Plugin Framework

**Story Points**: 13 | **Duration**: 4 days

- [ ] **Task 8.1.1**: Implement KitePlugin interface
    - Define plugin lifecycle
    - Plugin configuration
    - Write interface

- [ ] **Task 8.1.2**: Implement PluginContext
    - Register extensions
    - Register helpers
    - Register task types
    - Write implementation

- [ ] **Task 8.1.3**: Implement ServiceLoader discovery
    - Scan classpath for plugins
    - Load plugin classes
    - Initialize plugins
    - Write tests

- [ ] **Task 8.1.4**: Implement plugin configuration
    - Read from `kite.settings.kts`
    - Pass config to plugins
    - Write tests

### Epic 8.2: Example Plugin (Play Store)

**Story Points**: 13 | **Duration**: 4 days

- [ ] **Task 8.2.1**: Create playstore plugin project
    - Set up build
    - Add Google Play API dependency
    - Configure ServiceLoader

- [ ] **Task 8.2.2**: Implement Play Store DSL
    - `playStore { }` block
    - Upload APK functionality
    - Release notes support
    - Write tests

- [ ] **Task 8.2.3**: Implement authentication
    - Service account JSON
    - API client initialization
    - Write tests

- [ ] **Task 8.2.4**: Document plugin
    - Usage examples
    - Configuration guide
    - API reference

### Epic 8.3: Plugin Testing

**Story Points**: 5 | **Duration**: 2 days

- [ ] **Task 8.3.1**: Test plugin loading
    - Verify discovery
    - Verify initialization
    - Write tests

- [ ] **Task 8.3.2**: Integration test plugin usage
    - Use plugin in real pipeline
    - Verify DSL works
    - Write tests

**Deliverables**:

- Plugin system functional
- One working example plugin (Play Store)
- Plugin development guide

---

## Success Criteria

### MVP (Phases 1-7)

- [ ] Can define tasks in `.tasks.kts` files
- [ ] Can define configs in `.config.kts` files
- [ ] CLI can run pipelines
- [ ] Sequential execution works
- [ ] Parallel execution works with proper resource limits
- [ ] GitLab CI and GitHub Actions integration works
- [ ] Local execution works
- [ ] Timeout and retry work
- [ ] Artifact management works
- [ ] Logging is clear and helpful
- [ ] Documentation is complete
- [ ] At least one example project works end-to-end
- [ ] Startup time < 1 second
- [ ] Execution overhead < 5%

### Phase 8 (Plugin System)

- [ ] Plugin interface defined
- [ ] Plugin discovery works
- [ ] At least one plugin works (Play Store)
- [ ] Plugin development guide published

---

## Risk Mitigation

### Technical Risks

1. **Kotlin Scripting Complexity**
    - **Risk**: Script compilation/execution may be slow or unstable
    - **Mitigation**: Cache compiled scripts, implement timeout fallbacks

2. **Parallel Execution Edge Cases**
    - **Risk**: Race conditions, resource exhaustion
    - **Mitigation**: Extensive testing, conservative defaults, user controls

3. **Platform Detection Reliability**
    - **Risk**: CI platforms change environment variables
    - **Mitigation**: Fallback detection, explicit configuration option

### Schedule Risks

1. **Scope Creep**
    - **Risk**: Adding too many features delays MVP
    - **Mitigation**: Strict adherence to MVP scope, Phase 2 for extras

2. **Testing Time**
    - **Risk**: Bugs found late in development
    - **Mitigation**: Write tests alongside features, continuous integration

---

## Dependencies Between Epics

```
Phase 1 (Foundation)
  └── Phase 2 (Execution Engine)
      ├── Phase 3 (CLI)
      │   └── Phase 4 (Platform Adapters)
      │       └── Phase 5 (Helpers)
      │           └── Phase 6 (Documentation)
      │               └── Phase 7 (Testing)
      │                   └── Phase 8 (Plugins - Optional)
      └── Phase 5 (Helpers - parallel track)
```

---

## Resource Requirements

- **Developers**: 1-2 developers
- **Time**: 10-12 weeks (MVP), 14-16 weeks (with plugins)
- **Infrastructure**:
    - GitHub repository
    - CI/CD (GitHub Actions)
    - Documentation hosting (GitHub Pages)

---

## Definition of Done

### Per Task

- [ ] Code written and reviewed
- [ ] Unit tests written (>80% coverage)
- [ ] Documentation updated
- [ ] Integration tests written (if applicable)
- [ ] Code merged to main branch

### Per Epic

- [ ] All tasks completed
- [ ] Epic tested end-to-end
- [ ] Demo prepared
- [ ] Documented in user guide

### Per Phase

- [ ] All epics completed
- [ ] Phase deliverables achieved
- [ ] Integration testing passed
- [ ] Demo to stakeholders
