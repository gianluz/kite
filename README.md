# kite

A Kotlin-based CI ride runner for GitHub Actions, GitLab CI, and beyond.

## Overview

Kite replaces Fastlane and bash scripting in CI/CD with a type-safe, testable Kotlin DSL. Define **segments** (units of
work) once, compose them into different **rides** (workflows) for different scenarios.

## Key Terminology

- **Ride**: A pipeline/workflow (e.g., "MR Ride", "Release Ride")
- **Segment**: A single unit of work (e.g., "build", "test", "deploy")
- **Flow**: Execution order and dependencies of segments

## Quick Example

```kotlin
// .kite/segments/build.kite.kts
segments {
    segment("build") {
        execute { exec("./gradlew", "assembleRelease") }
    }
}

// .kite/rides/mr.kite.kts
ride {
    name = "MR Ride"
    
    flow {
        segment("build")
        
        parallel {
            segment("unitTest")
            segment("lint")
        }
    }
}
```

```bash
# Run a ride
./kite ride mr

# Run specific segments
./kite run build test

# List available rides
./kite rides
```

## Documentation

**📚 See [docs/](./docs/)** for comprehensive guides:

- **[Getting Started](./docs/EXTERNAL_PROJECT_SETUP.md)** - Using Kite in your projects
- **[External Dependencies](./docs/EXTERNAL_DEPENDENCIES.md)** - Using `@DependsOn` for libraries
- **[IDE Setup](./docs/IDE_SETUP.md)** - Setting up IntelliJ IDEA
- **[Troubleshooting](./docs/IDE_AUTOCOMPLETE_TROUBLESHOOTING.md)** - Fixing common IDE issues

**📋 See [specs/](./specs/)** for detailed specifications:

- [Overview & Problem Statement](./specs/01-overview.md) - What Kite is and why
- [Core Concepts](./specs/02-core-concepts.md) - Rides, segments, and flows
- [DSL & Configuration](./specs/03-dsl-configuration.md) - Kotlin DSL syntax
- [Full specifications](./specs/) for execution model, parallelization, plugins, and more

**🗺️ See [DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md)** for the implementation roadmap.

## Development

This project is under active development. Contributions will be welcomed after the initial release.

### Prerequisites

- JDK 17 or higher (LTS)
- Kotlin 2.0+
- Gradle 9.2+ (wrapper included)

### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew :kite-cli:run
```

### For Contributors

See **[docs/IDE_SETUP.md](./docs/IDE_SETUP.md)** to set up your development environment with full IDE autocomplete
support.

## License

Apache License 2.0 - see [LICENSE](LICENSE)
