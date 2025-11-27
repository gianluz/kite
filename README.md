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
kite-cli ride CI

# Run specific segments
kite-cli run test lint

# List available workflows
kite-cli rides
```

**Use in GitHub Actions:**

```yaml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run CI Workflow
        uses: ./.github/actions/run-kite
        with:
          command: ride CI
```

See **[GitHub Actions documentation](.github/actions/README.md)** for more examples.

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
- **[GitHub Actions](.github/actions/README.md)** - Use Kite in GitHub Actions workflows
- **[CLI Reference](docs/12-cli-reference.md)** - Command-line documentation
- **[Troubleshooting](docs/99-troubleshooting.md)** - Common issues and solutions

---

## Installation

> **Note:** Kite is under active development and not yet published to Maven Central. To try it out, you'll need to build
> from source.

### Try It Out (From Source)

**1. Clone and build Kite:**

```bash
git clone https://github.com/gianluz/kite.git
cd kite
./gradlew build
./gradlew :kite-cli:installDist
```

**2. Add to your PATH (recommended):**

Choose one of these options:

```bash
# Option A: Add to PATH (recommended)
# Add to ~/.bashrc or ~/.zshrc:
export PATH="$PATH:/path/to/kite/kite-cli/build/install/kite-cli/bin"
# Then run: kite-cli run hello

# Option B: Create an alias
# Add to ~/.bashrc or ~/.zshrc:
alias kite='/path/to/kite/kite-cli/build/install/kite-cli/bin/kite-cli'
# Then run: kite run hello

# Option C: Use full path (no setup needed)
# /path/to/kite/kite-cli/build/install/kite-cli/bin/kite-cli run hello
```

**Note:** The executable is named `kite-cli`. Examples in this README use `kite-cli` unless an alias is mentioned.

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
# If you set up the alias (step 2):
kite run hello

# Or use the full path:
/path/to/kite/kite-cli/build/install/kite-cli/bin/kite-cli run hello

# Or if you added to PATH:
kite-cli run hello
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

Kite was created to solve the complexity and limitations of existing CI/CD tools like **Fastlane** and **bash scripts**.

### Before: Fastlane/Fastfile 😓

```ruby
# Fastfile
lane :ci do
  clean
  build
  test    # Sequential only
  lint    # Can't run in parallel
end

lane :test do
  gradle(task: "test")
end

lane :lint do
  gradle(task: "lint")
end
```

**Problems with Fastlane:**

- ❌ **Ruby dependency** - Requires Ruby environment setup
- ❌ **No type safety** - Errors only at runtime
- ❌ **Limited parallelization** - Hard to run lanes in parallel
- ❌ **Complex syntax** - Learning curve for Ruby/Fastlane DSL
- ❌ **Android/iOS specific** - Not ideal for other JVM projects
- ❌ **No IDE support** - Limited autocomplete and refactoring
- ❌ **Difficult debugging** - Stack traces through Ruby/Fastlane layers

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

**Real Benefits vs Fastlane/Bash:**

- ⚡ **Parallel execution** - Test and lint run simultaneously (save 2 minutes!)
- 🔗 **Dependency management** - Build always runs before tests
- 🔒 **Secret masking** - `requireSecret()` automatically masks sensitive data
- 🎯 **Type-safe** - Catch errors at compile time, not runtime
- ✨ **Full IDE support** - Autocomplete, refactoring, and debugging with Kotlin
- 🧪 **Testable** - Unit test your CI/CD logic (impossible with Fastlane)
- 🔄 **Reusable** - Share segments across different workflows
- 🚀 **No Ruby needed** - Pure Kotlin/JVM, no additional runtime
- 🌍 **Platform-agnostic** - Not limited to Android/iOS like Fastlane
- 📦 **Better for Kotlin projects** - Native Kotlin integration

**Time savings:** Sequential (Fastlane/bash) = 7 min, Kite parallel = 5 min (29% faster)

**Comparison:**

|                           | Fastlane             | Bash Scripts   | Kite                  |
|---------------------------|----------------------|----------------|-----------------------|
| **Type Safety**           | ❌ Runtime only       | ❌ None         | ✅ Compile-time        |
| **IDE Support**           | ⚠️ Limited           | ❌ None         | ✅ Full Kotlin support |
| **Parallel Execution**    | ⚠️ Complex           | ❌ Manual       | ✅ Built-in            |
| **Testing**               | ❌ Hard to test       | ❌ Hard to test | ✅ Unit testable       |
| **Dependency Management** | ⚠️ Manual            | ❌ Manual       | ✅ Automatic           |
| **Secret Masking**        | ⚠️ Manual setup      | ❌ Manual       | ✅ Automatic           |
| **Platform Support**      | ⚠️ iOS/Android focus | ✅ Any          | ✅ Any CI/CD           |
| **Learning Curve**        | 🟡 Ruby + Fastlane   | 🟢 Low         | 🟢 Kotlin only        |

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
git clone https://github.com/gianluz/kite.git
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

### ✅ Completed

- [x] Core DSL and execution engine
- [x] Parallel execution with dependency management
- [x] Artifact management
- [x] Automatic secret masking
- [x] CLI interface
- [x] CI/CD integration (GitHub Actions, GitLab CI)
- [x] Timeout and retry mechanisms
- [x] Platform-agnostic design

### 🚧 In Progress

- [ ] **Maven Central distribution** - Publish to Maven Central for easy dependency management
- [ ] **Swift script support** - Write Kite segments in Swift (`.kite.swift` files)
    - Using Danger-style inline dependency comments
    - Automatic compilation and caching
    - Full Swift Package Manager integration

### 🔮 Planned

- [ ] Plugin system for extensibility
- [ ] Remote caching for distributed teams
- [ ] Distributed execution across multiple machines
- [ ] Web dashboard for visualization
- [ ] Additional language support (Python, Go, Rust)

See **[Development Plan](devplan/)** for detailed roadmap.

---

## License

Apache License 2.0 - see [LICENSE](LICENSE)

---

## Support

- **📚 Documentation:** [docs/](docs/00-index.md)
- **🐛 Issues:** [GitHub Issues](https://github.com/gianluz/kite/issues)
- **💬 Discussions:** [GitHub Discussions](https://github.com/gianluz/kite/discussions)

---

## Supporters

Special thanks to **[Luno](https://www.luno.com)** for supporting the development of Kite! 🙏

Luno is a leading cryptocurrency platform that believes in empowering developers with better tools. Their support has
been instrumental in making Kite possible.

---

## AI Disclaimer

This project was developed with significant AI assistance. The implementation, architecture, testing, and documentation
were created through extensive collaboration using various large language models via **Firebender** 🔥 (an AI-powered
development tool with IDE plugins for IntelliJ and Android Studio). While AI played a major
role in accelerating development and ensuring comprehensive test coverage, all design decisions, architectural choices,
and code quality standards were carefully reviewed and validated.

**Note on Quality:** Despite thorough testing and review, some inconsistencies between the implementation and
documentation may still exist due to the rapid, AI-assisted development process. If you encounter any bugs,
inconsistencies, or areas
where the documentation doesn't match the actual behavior, please *
*[open an issue](https://github.com/gianluz/kite/issues)**
or submit a pull request. Your contributions help improve Kite for everyone!

**Shoutout to [Firebender](https://firebender.dev)** for providing an excellent AI-powered development tool! 🔥

---

**Made with ❤️ using Kotlin**
