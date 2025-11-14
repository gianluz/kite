# kite
A Kotlin-based CI task runner for GitHub Actions, GitLab CI, and beyond.

## Overview

Kite is a modern, type-safe CI/CD task runner that replaces bash scripting and tools like Fastlane with Kotlin. Define
your build pipeline as modular, testable, and reusable tasks with full IDE support.

### Key Features

- **Modular Task Definitions**: Define tasks once, reuse across multiple pipeline configurations
- **Configuration Composition**: Different configs for different scenarios (MR, release, nightly, local)
- **Internal Parallelization**: Run independent tasks in parallel within a single CI stage/container
- **Type-Safe DSL**: Full Kotlin with IDE autocomplete, refactoring, and type checking
- **Local Execution**: Test your CI pipeline locally before pushing
- **Cross-Platform**: GitLab CI, GitHub Actions, and local development

### Quick Example

```kotlin
// .kite/tasks/build.tasks.kts
tasks {
    task("build") {
        execute { exec("./gradlew", "assembleRelease") }
    }
}

// .kite/configs/mr.config.kts
config {
    pipeline {
        task("build")
        parallel {
            task("unitTest")
            task("roborazzi")
        }
    }
}
```

```bash
# Run your pipeline
./kite run --config mr
```

## Documentation

**See [SPECS.md](./SPECS.md) for the complete specification**, including:

- Problem statement and design principles
- Core features and DSL examples (modular tasks, configurations, dependencies)
- Architecture and components
- **In-depth parallelization guide**: How Kite works with Gradle, resource management, decision trees, real-world
  performance examples
- CI integration for GitLab CI and GitHub Actions
- Use cases and best practices
- Testing strategy and timeline

## Development

**See [DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md) for the detailed development roadmap**, including:

- 8 phases with epics and individual tasks
- Story point estimates and durations
- Dependencies between phases
- Success criteria and deliverables
- Risk mitigation strategies
- Timeline: 10-12 weeks for MVP

## Status

🚧 **Work in Progress** - Currently in specification and design phase.

## License

See [LICENSE](./LICENSE) for details.
