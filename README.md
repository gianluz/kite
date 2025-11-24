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
- 🌍 **Platform-agnostic** - Works on any CI/CD platform (no vendor lock-in)
- 🧩 **Reusable components** - Define segments once, compose into different workflows

**Platform-Agnostic Design:**

Kite doesn't assume you're using GitLab, GitHub, or any specific CI platform. Instead of providing opinionated
properties
like `isRelease` or `mrNumber`, you query environment variables directly using `env()`. This means Kite works on ANY
CI/CD platform without modification:

```kotlin
segment("deploy") {
    condition = { ctx ->
        // Your platform, your conventions
        ctx.env("CI_MERGE_REQUEST_LABELS")?.contains("release") == true
    }
}
```

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

> **Note:** Kite is under active development and not yet published to Maven Central. To try it out, you'll need to build
> from source.

### Try It Out (From Source)

**1. Clone and build Kite:**

```bash
git clone https://github.com/yourusername/kite.git
cd kite
./gradlew build
./gradlew :kite-cli:installDist
```

**2. Add kite to your PATH (optional):**

```bash
# Add to ~/.bashrc or ~/.zshrc
export PATH="$PATH:/path/to/kite/kite-cli/build/install/kite-cli/bin"

# Or create an alias
alias kite='/path/to/kite/kite-cli/build/install/kite-cli/bin/kite-cli'
```

**3. Create a sample project:**

```bash
mkdir my-kite-project
cd my-kite-project
mkdir -p .kite/segments .kite/rides
```

**4. Define your first segment:**

```kotlin
// .kite/segments/hello.kite.kts
segments {
    segment("hello") {
        description = "My first Kite segment"
        execute {
            logger.info("Hello from Kite!")
            exec("echo", "This works!")
        }
    }
}
```

**5. Run it:**

```bash
kite run hello
# Or if not in PATH:
# /path/to/kite/kite-cli/build/install/kite-cli/bin/kite-cli run hello
```

See **[Installation Guide](docs/02-installation.md)** for complete setup instructions and troubleshooting.

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

# Run tests and lint sequentially (slow!)
./gradlew test        # 5 minutes
./gradlew lint        # 2 minutes
# Total: 7 minutes wasted waiting

# Problems:
# - No parallelization
# - No dependency management
# - Secrets leak into logs
# - Hard to maintain
# - Difficult to test
# - No IDE support
```

### After: Kite ✨

```kotlin
segments {
    segment("clean") { 
        execute { exec("./gradlew", "clean") } 
    }
    
    segment("build") { 
        dependsOn("clean")
        execute { exec("./gradlew", "assembleRelease") } 
    }
    
    segment("test") { 
        dependsOn("build")
        execute { exec("./gradlew", "test") }  // 5 minutes
    }
    
    segment("lint") { 
        dependsOn("build")
        execute { exec("./gradlew", "lint") }  // 2 minutes
    }
}

ride {
    name = "CI"
    maxConcurrency = 4  // Run up to 4 tasks in parallel
    
    flow {
        segment("clean")
        segment("build")
        
        // Test and lint run in parallel!
        parallel {
            segment("test")   // 5 min ⎤
            segment("lint")   // 2 min ⎦ → Only 5 min total!
        }
    }
}
```

**Real Benefits:**

- ⚡ **Parallel execution** - Test and lint run simultaneously (save 2 minutes!)
- 🔗 **Dependency management** - Build always runs before tests
- 🔒 **Secret masking** - `requireSecret()` automatically masks sensitive data
- 🎯 **Type-safe** - Catch errors at compile time, not runtime
- ✨ **IDE support** - Full autocomplete and refactoring
- 🧪 **Testable** - Unit test your CI/CD logic
- 🔄 **Reusable** - Share segments across different workflows

**Time savings:** Sequential bash = 7 min, Kite parallel = 5 min (29% faster)

---

## Contributing

We welcome contributions! Kite is under active development.

**Start here:** **[CONTRIBUTING.md](CONTRIBUTING.md)** - Complete contribution guide

**Additional Resources:**

- **[Code Quality Standards](docs/dev/02-code-quality.md)** - Linting and quality requirements
- **[Testing Strategy](docs/dev/03-testing-strategy.md)** - Integration testing approach
- **[Architecture Specs](docs/specs/)** - System design and specifications

### Development Setup

```bash
# Clone repository
git clone https://github.com/yourusername/kite.git
cd kite

# Build (git hooks install automatically)
./gradlew build

# Run tests
./gradlew test
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
