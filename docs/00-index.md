# Kite Documentation

**Kite** is a modern, type-safe CI/CD workflow runner for Kotlin projects. Define pipelines as code with a powerful DSL,
compose reusable segments, and run them locally or in any CI platform.

## Quick Links

- 🚀 [Getting Started](01-getting-started.md) - Get up and running in 5 minutes
- 📦 [Installation](02-installation.md) - Installation methods
- 💡 [Core Concepts](03-core-concepts.md) - Understand rides, segments, and flows
- 📖 [API Reference](api/00-index.md) - Complete API documentation

## Documentation Sections

### User Guide

1. [Getting Started](01-getting-started.md) - Quick start guide
2. [Installation](02-installation.md) - Installation methods
3. [Core Concepts](03-core-concepts.md) - Rides, Segments, and Flows
4. [Writing Segments](04-writing-segments.md) - Create reusable units of work
5. [Writing Rides](05-writing-rides.md) - Compose segments into workflows
6. [Execution Context](06-execution-context.md) - Using the context API
7. [Parallel Execution](07-parallel-execution.md) - Running segments in parallel
8. [Artifacts](08-artifacts.md) - Managing build artifacts
9. [Secrets](09-secrets.md) - Handling sensitive data
10. [External Dependencies](10-external-dependencies.md) - Using @DependsOn
11. [CI Integration](11-ci-integration.md) - GitHub Actions, GitLab CI, etc.
12. [CLI Reference](12-cli-reference.md) - Command-line usage
99. [Troubleshooting](99-troubleshooting.md) - Common issues and solutions

### API Documentation

- [API Overview](api/00-index.md) - API documentation index
- [DSL API](api/dsl.md) - DSL builder functions
- [Core API](api/core.md) - Core classes and interfaces
- [Runtime API](api/runtime.md) - Execution and scheduling

### Developer Guide

- [Contributing](dev/contributing.md) - How to contribute
- [Code Quality](dev/code-quality.md) - Quality standards
- [Architecture](dev/architecture.md) - System design
- [Testing](dev/testing.md) - Testing strategy

## What is Kite?

Kite replaces bash scripts, Fastlane, and YAML-based CI configs with:

✅ **Type-safe Kotlin DSL** - Catch errors at compile time  
✅ **Reusable segments** - Define once, use everywhere  
✅ **Local testing** - Test pipelines before pushing  
✅ **Platform agnostic** - Works on any CI platform  
✅ **Parallel execution** - Run tasks concurrently  
✅ **Artifact management** - Share files between segments  
✅ **Secret masking** - Automatic secret protection

## Quick Example

```kotlin
// .kite/segments/build.kite.kts
segments {
    segment("build") {
        description = "Build the application"
        execute {
            exec("./gradlew", "build")
        }
    }
    
    segment("test") {
        description = "Run tests"
        dependsOn("build")
        execute {
            exec("./gradlew", "test")
        }
    }
}

// .kite/rides/ci.kite.kts
ride {
    name = "CI"
    
    flow {
        segment("build")
        
        parallel {
            segment("test")
            segment("lint")
            segment("detekt")
        }
    }
}
```

```bash
# Run the CI ride
kite ride CI

# Run specific segments
kite run build test

# List available rides and segments
kite rides
kite segments
```

## Key Features

### Type-Safe DSL

Write CI/CD logic in Kotlin with full IDE support, autocomplete, and type checking.

### Reusable Segments

Define segments once and compose them into different workflows for different scenarios (MR, release, nightly, etc.).

### Local Testing

Test your entire CI/CD pipeline locally before pushing. No more "fix CI" commits!

### Platform Agnostic

Kite runs anywhere: GitHub Actions, GitLab CI, Jenkins, locally on your machine, or any CI platform that supports
running binaries.

### Parallel Execution

Automatically parallelize independent segments for faster builds.

### Artifact Management

Share files between segments with automatic upload/download and cross-ride artifact support.

### Secret Management

Automatic secret masking in logs and environment variable support.

## Community & Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/kite/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/kite/discussions)
- **Contributing**: See [Contributing Guide](dev/contributing.md)

## License

Kite is open source software licensed under the [Apache License 2.0](../LICENSE).
