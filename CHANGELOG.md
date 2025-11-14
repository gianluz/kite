# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial project structure with multi-module Gradle setup
- Core modules: `kite-core`, `kite-dsl`, `kite-runtime`, `kite-cli`
- Code quality tools: ktlint and detekt configuration
- Project documentation: SPECS.md, DEVELOPMENT_PLAN.md, CONTRIBUTING.md

### Added

- **Epic 1.1 Complete**: Project Setup & Infrastructure
    - Multi-module Gradle project (kite-core, kite-dsl, kite-runtime, kite-cli)
  - Kotlin 2.0.21 with Java 17+ toolchain support
    - Code quality tools: ktlint (1.0.1) and detekt (1.23.7)
    - Dependency management: coroutines, serialization, Clikt CLI, Kotlin Scripting
    - Maven publishing configuration with POM metadata
    - GitHub Actions CI workflow
    - Test infrastructure: JUnit 5, MockK
    - Project documentation: SPECS.md, DEVELOPMENT_PLAN.md, CONTRIBUTING.md, CHANGELOG.md
    - .gitignore for Kotlin/Gradle projects
    - Gradle wrapper (v8.14.1)
    - Build and run verification successful

- **Epic 1.1 Complete**: Project Setup & Infrastructure
    - Multi-module Gradle project (kite-core, kite-dsl, kite-runtime, kite-cli)
    - Kotlin 2.0.21 with Java 17 LTS toolchain support
    - Gradle 9.2.0 with configuration cache enabled
    - Code quality tools: ktlint (1.0.1) and detekt (1.23.7)
    - Dependency management: coroutines, serialization, Clikt CLI, Kotlin Scripting
    - Maven publishing configuration with POM metadata
    - GitHub Actions CI workflow
    - Test infrastructure: JUnit 5, MockK
    - Project documentation: SPECS.md, DEVELOPMENT_PLAN.md, CONTRIBUTING.md, CHANGELOG.md
    - .gitignore for Kotlin/Gradle projects
    - Build and run verification successful

## [0.1.0-SNAPSHOT] - 2025-11-14

### Added

- Project initialization (Task 1.1.1 complete)
- Gradle multi-module configuration
- Kotlin 2.0.21 with Java 17+ compatibility
- Testing infrastructure (JUnit 5, MockK)
- Code quality configuration (ktlint, detekt)

[Unreleased]: https://github.com/yourusername/kite/compare/v0.1.0-SNAPSHOT...HEAD

[0.1.0-SNAPSHOT]: https://github.com/yourusername/kite/releases/tag/v0.1.0-SNAPSHOT
