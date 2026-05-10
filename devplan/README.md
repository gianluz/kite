# Kite Development Plan

## Overview

This directory contains the detailed development plan for Kite, organized by phase. Each phase contains epics with
detailed tasks, story points, and deliverables.

## Quick Status

| Phase                                                      | Status        | Completion |
|------------------------------------------------------------|---------------|------------|
| [Phase 1: Foundation & Core DSL](phase-1-foundation.md)    | ✅ Complete    | 100%       |
| [Phase 2: Graph & Execution Engine](phase-2-execution.md)  | ✅ Complete    | 100%       |
| [Phase 3: CLI & File Discovery](phase-3-cli.md)            | ✅ Complete    | 100%       |
| [Phase 4: Platform Adapters](phase-4-platform-adapters.md) | ✅ Refactored  | 100%       |
| [Phase 5: Built-in Features](phase-5-features.md)          | ✅ Complete    | 100%       |
| [Phase 6: Documentation](phase-6-documentation.md)         | ✅ Complete    | 100%       |
| [Phase 7: Testing & Refinement](phase-7-testing.md)        | ✅ Complete    | 100%       |
| [Phase 8: Plugin System](phase-8-plugins.md)               | 📋 Planned    | 0%         |
| Maven Central Distribution                                  | ✅ Complete    | 100%       |
| Docker Distribution (GHCR + Docker Hub)                     | ✅ Complete    | 100%       |
| Binary distribution (install script + GitHub Releases)      | ✅ Complete    | 100%       |

**Additional**: [Security Roadmap](security-roadmap.md) - Cross-phase security features

## Live Status

Real-time build and test status — no hardcoded counts:

[![CI](https://github.com/gianluz/kite/actions/workflows/ci.yml/badge.svg)](https://github.com/gianluz/kite/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.gianluz.kite/kite-core.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/com.gianluz.kite)

Tests pass or they don't — the badge tells the truth. Line counts and test counts go stale the moment they're written.

## Current Roadmap (Post-MVP)

### 🔮 Next Up

- **Swift Script Support** (Exploratory) - Write segments in Swift
    - Danger-style inline dependency comments
    - Automatic Swift compilation and caching
    - Swift Package Manager integration
    - Estimated: 4-6 weeks

- **Plugin System** (Phase 8) - Extensibility framework
    - Plugin discovery and loading
    - API for custom segment types
    - Plugin marketplace
    - Estimated: 3-4 weeks

### 🌟 Future Enhancements

- Homebrew tap for macOS/Linux (`brew install gianluz/kite/kite-cli`)
- Remote caching for distributed teams
- Distributed execution across multiple machines
- Web dashboard for visualization
- Additional language support (Python, Go, Rust)

## Terminology

- **Ride**: A workflow/pipeline composed of segments (formerly "pipeline")
- **Segment**: A unit of work in a ride (formerly "task")
- **Flow**: The execution order of segments within a ride

## Project Structure

```
kite/
├── devplan/                       # This directory - development plans
├── docs/                          # User documentation
├── kite-core/                     # Core domain models
├── kite-dsl/                      # Kotlin DSL and scripting
├── kite-runtime/                  # Execution runtime
├── kite-cli/                      # CLI interface
├── kite-integration-tests/        # Integration tests
├── kite-plugins/                  # Official plugins (git, gradle)
├── .kite/                         # Kite's own CI/CD
│   ├── segments/                  # Reusable segments
│   └── rides/                     # CI, MR, and Deploy rides
└── CHANGELOG.md                   # Version history
```

## Key Achievements

✅ **Core Infrastructure** (Phases 1-3)

- Type-safe Kotlin DSL for segments and rides
- Complete graph theory (DAG, topological sort, cycle detection)
- Sequential AND parallel execution engines
- Real process execution with timeout and retry
- Beautiful CLI with colors and emojis
- File discovery and script compilation

✅ **Built-in Features** (Phase 5)

- 20+ file operation helpers
- Process execution helpers (`exec`, `shell`, `execOrNull`)
- Artifact management with cross-ride sharing
- Per-segment logging system
- Lifecycle hooks (onSuccess/onFailure/onComplete)
- Secret masking and security features

✅ **Testing & Integration** (Phase 7)

- Unit tests across all modules
- Integration test framework (`kite-integration-tests`)
- GitHub Actions workflows (`ci.yml` on push, `pr.yml` on PR, `release.yml` on tag)
- Kite managing its own CI/CD — eats its own dog food

✅ **Documentation** (Phase 6)

- 12+ comprehensive guides
- IDE setup and troubleshooting
- External dependencies guide
- CI/CD integration guide
- Security documentation

✅ **Distribution** (Post-MVP)

- **Maven Central** — `com.gianluz.kite:kite-core`, `kite-dsl`, `kite-runtime`, `git`, `gradle`
- **Docker** — `ghcr.io/gianluz/kite` (GHCR) and `gianluz/kite` (Docker Hub), both auto-published on every release
- **GitHub Releases** — CLI binary archives (`.tar` / `.zip`) attached to every tag
- **Install script** — `curl -sSL .../install.sh | bash` for macOS/Linux
- **Automated version sync** — `kite-cli run update-version-refs` keeps all docs current from a single source of truth in `build.gradle.kts`

## Contact

For questions or contributions, see the main [README.md](../README.md) and [CONTRIBUTING.md](../CONTRIBUTING.md).
