# 🪁 Kite

A modern, type-safe CI/CD workflow runner for Kotlin projects.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)

---

## What is Kite?

Kite helps you define and execute CI/CD workflows using a **type-safe Kotlin DSL**. Replace bash scripts and YAML
configuration with testable, reusable Kotlin code.

**Key Features:**

- 🎯 **Type-safe DSL** - Catch errors at compile time, not runtime
- ⚡ **Parallel execution** - Run segments in parallel for faster builds
- 🔒 **Automatic secret masking** - Secrets never leak into logs
- 📦 **Artifact management** - Share build outputs between segments
- 🔄 **CI/CD ready** - Works with GitHub Actions, GitLab CI, Jenkins, and more
- 🧩 **Reusable components** - Define segments once, compose into different workflows

---

## Quick Example

**Define segments** (units of work):

```kotlin
// .kite/segments/build.kite.kts
segments {
    segment("build") {
        description = "Build the application"
        execute {
            exec("./gradlew", "assembleRelease")
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
```

**Compose into rides** (workflows):

```kotlin
// .kite/rides/ci.kite.kts
ride {
    name = "CI"
    maxConcurrency = 4
    
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

**Execute:**

```bash
# Run a complete workflow
kite ride CI

# Run specific segments
kite run test lint

# List available workflows
kite rides
```

---

## Documentation

📚 **[Complete Documentation](docs/00-index.md)**

### Getting Started

- **[Quick Start Guide](docs/01-getting-started.md)** - Get up and running in 5 minutes
- **[Installation](docs/02-installation.md)** - Setup and configuration
- **[Core Concepts](docs/03-core-concepts.md)** - Understand rides, segments, and flows

### Writing Workflows

- **[Writing Segments](docs/04-writing-segments.md)** - Create reusable units of work
- **[Writing Rides](docs/05-writing-rides.md)** - Compose segments into workflows
- **[Execution Context](docs/06-execution-context.md)** - Complete API reference

### Advanced Topics

- **[Parallel Execution](docs/07-parallel-execution.md)** - Optimize performance
- **[Artifacts](docs/08-artifacts.md)** - Share files between segments
- **[Secrets Management](docs/09-secrets.md)** - Handle sensitive data securely
- **[External Dependencies](docs/10-external-dependencies.md)** - Use external libraries

### Integration & Reference

- **[CI/CD Integration](docs/11-ci-integration.md)** - GitHub Actions, GitLab CI, Jenkins
- **[CLI Reference](docs/12-cli-reference.md)** - Command-line documentation
- **[Troubleshooting](docs/99-troubleshooting.md)** - Common issues and solutions

---

## Installation

### Using in Your Project

**1. Create Kite structure:**

```bash
mkdir -p .kite/segments .kite/rides
```

**2. Add to your project:**

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.kite:kite-dsl:0.1.0")
}
```

**3. Define your first segment:**

```kotlin
// .kite/segments/hello.kite.kts
segments {
    segment("hello") {
        execute {
            logger.info("Hello from Kite!")
        }
    }
}
```

**4. Run it:**

```bash
./gradlew :kite-cli:installDist
kite-cli/build/install/kite-cli/bin/kite-cli run hello
```

See **[Installation Guide](docs/02-installation.md)** for complete setup instructions.

---

## Example Workflows

### Android Build Pipeline

```kotlin
ride {
    name = "Android CI"
    maxConcurrency = 3
    
    flow {
        segment("clean")
        segment("compile")
        
        parallel {
            segment("unit-tests")
            segment("instrumented-tests")
            segment("lint")
        }
        
        segment("assemble")
    }
}
```

### Multi-Module Project

```kotlin
parallel {
    segment("test-core")
    segment("test-api")
    segment("test-ui")
}
```

### Conditional Deployment

```kotlin
segment("deploy") {
    condition = { ctx: ExecutionContext ->
        ctx.env("BRANCH") == "main"
    }
    execute {
        val token = requireSecret("DEPLOY_TOKEN")
        exec("./deploy.sh", "--prod")
    }
}
```

---

## Why Kite?

### Before: Bash Scripts 😱

```bash
#!/bin/bash
set -e

./gradlew clean
./gradlew assembleRelease
./gradlew test
./gradlew lint

# No type safety, hard to test, difficult to debug
```

### After: Kite ✨

```kotlin
segments {
    segment("clean") { execute { exec("./gradlew", "clean") } }
    segment("build") { execute { exec("./gradlew", "assembleRelease") } }
    segment("test") { dependsOn("build"); execute { exec("./gradlew", "test") } }
    segment("lint") { dependsOn("build"); execute { exec("./gradlew", "lint") } }
}

ride {
    name = "CI"
    flow {
        segment("clean")
        segment("build")
        parallel {
            segment("test")
            segment("lint")
        }
    }
}
```

**Benefits:**

- ✅ Type-safe at compile time
- ✅ IDE autocomplete and refactoring
- ✅ Testable in unit tests
- ✅ Reusable across projects
- ✅ Parallel execution
- ✅ Automatic secret masking

---

## Contributing

We welcome contributions! Kite is under active development.

**For Contributors:**

- **[Contributing Guide](docs/dev/01-contributing.md)** - Development setup and guidelines
- **[Code Quality](docs/dev/02-code-quality.md)** - Standards and linting
- **[Testing Strategy](docs/dev/03-testing-strategy.md)** - Integration testing approach
- **[Architecture](docs/specs/)** - System design and specifications

### Development Setup

```bash
# Clone repository
git clone https://github.com/yourusername/kite.git
cd kite

# Build
./gradlew build

# Run tests
./gradlew test

# Install git hooks (runs quality checks before push)
./scripts/install-git-hooks.sh
```

### Project Structure

```
kite/
├── kite-core/          # Core domain models
├── kite-dsl/           # DSL and scripting
├── kite-runtime/       # Execution engine
├── kite-cli/           # Command-line interface
├── kite-integration-tests/  # End-to-end tests
└── docs/               # Documentation
```

---

## Requirements

- **Java:** 17 or higher (LTS)
- **Kotlin:** 2.0.21
- **Gradle:** 8.0+ (wrapper included)

---

## Roadmap

- [x] Core DSL and execution engine
- [x] Parallel execution
- [x] Artifact management
- [x] Secret masking
- [x] CLI interface
- [x] CI/CD integration
- [ ] Plugin system
- [ ] Remote caching
- [ ] Distributed execution

See **[Development Plan](devplan/)** for detailed roadmap.

---

## License

Apache License 2.0 - see [LICENSE](LICENSE)

---

## Support

- **📚 Documentation:** [docs/](docs/00-index.md)
- **🐛 Issues:** [GitHub Issues](https://github.com/yourusername/kite/issues)
- **💬 Discussions:** [GitHub Discussions](https://github.com/yourusername/kite/discussions)

---

**Made with ❤️ using Kotlin**
