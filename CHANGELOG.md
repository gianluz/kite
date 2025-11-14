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
- **Configuration Cache**: Enabled for faster builds
- **Build Time**: 3s initial, 1s cached (67% improvement with configuration cache)

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
