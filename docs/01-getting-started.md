# Getting Started with Kite

Get up and running with Kite in 5 minutes. This guide will help you create your first workflow.

## What is Kite?

Kite is a modern CI/CD workflow runner that lets you define pipelines as type-safe Kotlin code. Replace bash scripts and
YAML configs with a powerful DSL that provides:

- ✅ **Type safety** - Catch errors at compile time
- ✅ **IDE support** - Full autocomplete and syntax highlighting
- ✅ **Reusability** - Define segments once, use everywhere
- ✅ **Local testing** - Test pipelines before pushing
- ✅ **Platform agnostic** - Works on any CI platform

## Quick Start

### 1. Installation

Add Kite to your project:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.kite:kite-core:0.1.0-alpha")
    implementation("io.kite:kite-dsl:0.1.0-alpha")
    implementation("io.kite:kite-runtime:0.1.0-alpha")
}
```

See [Installation](02-installation.md) for other installation methods.

### 2. Create Your First Segment

Create `.kite/segments/build.kite.kts`:

```kotlin
segments {
    segment("build") {
        description = "Build the project"
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
```

### 3. Create Your First Ride

Create `.kite/rides/ci.kite.kts`:

```kotlin
ride {
    name = "CI"
    
    flow {
        segment("build")
        segment("test")
    }
}
```

### 4. Run It!

```bash
# Run the CI ride
kite ride CI

# Or run specific segments
kite run build test
```

That's it! You've created your first Kite workflow.

## Project Structure

Kite uses a simple directory structure:

```
your-project/
├── .kite/
│   ├── segments/           # Reusable units of work
│   │   ├── build.kite.kts
│   │   ├── test.kite.kts
│   │   └── deploy.kite.kts
│   └── rides/              # Workflow definitions
│       ├── ci.kite.kts
│       └── release.kite.kts
└── build.gradle.kts
```

## Key Concepts

### Segments

**Segments** are reusable units of work - like building, testing, or deploying.

```kotlin
segment("build") {
    description = "Build the application"
    timeout = 10.minutes
    execute {
        exec("./gradlew", "build")
    }
}
```

### Rides

**Rides** compose segments into workflows for different scenarios (CI, release, etc.).

```kotlin
ride {
    name = "CI"
    flow {
        segment("build")
        parallel {
            segment("test")
            segment("lint")
        }
    }
}
```

### Execution Context

Inside `execute` blocks, you have access to the execution context:

```kotlin
execute {
    // Run commands
    exec("./gradlew", "test")
    
    // Access environment variables
    val branch = env("BRANCH_NAME")
    
    // Work with files
    writeFile("output.txt", "Hello!")
    
    // Conditional logic
    if (branch == "main") {
        exec("./deploy.sh")
    }
}
```

Learn more in [Core Concepts](03-core-concepts.md).

## IDE Setup

IntelliJ IDEA automatically provides full IDE support for `.kite.kts` files:

- ✅ Syntax highlighting
- ✅ Autocomplete
- ✅ Type checking
- ✅ Refactoring support
- ✅ Error detection

### Enable Autocomplete

After adding Kite dependencies, reload your Gradle project:

1. View → Tool Windows → Gradle
2. Click "Reload All Gradle Projects" (↻ icon)

That's it! Your `.kite.kts` files now have full IDE support.

See [Troubleshooting](99-troubleshooting.md) if autocomplete doesn't work.

## Real-World Example

Here's a complete CI/CD workflow for an Android app:

### Segments (`.kite/segments/android.kite.kts`)

```kotlin
segments {
    segment("compile") {
        description = "Compile the Android app"
        execute {
            exec("./gradlew", "compileDebugKotlin")
        }
    }
    
    segment("lint") {
        description = "Run Android lint checks"
        dependsOn("compile")
        execute {
            exec("./gradlew", "lintDebug")
        }
    }
    
    segment("test") {
        description = "Run unit tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", "testDebugUnitTest")
        }
    }
    
    segment("assemble") {
        description = "Build APK"
        dependsOn("lint", "test")
        execute {
            exec("./gradlew", "assembleDebug")
        }
    }
}
```

### Ride (`.kite/rides/ci.kite.kts`)

```kotlin
ride {
    name = "CI"
    maxConcurrency = 2
    
    flow {
        segment("compile")
        
        // Run lint and tests in parallel
        parallel {
            segment("lint")
            segment("test")
        }
        
        segment("assemble")
    }
}
```

### GitHub Actions (`.github/workflows/ci.yml`)

```yaml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      
      - name: Build Kite CLI
        run: ./gradlew :kite-cli:installDist
      
      - name: Run CI
        run: kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

## Common Patterns

### Helper Functions

Define reusable functions:

```kotlin
fun ExecutionContext.gradleTask(vararg tasks: String) {
    exec("./gradlew", *tasks)
}

segments {
    segment("build") {
        execute {
            gradleTask("clean", "build")
        }
    }
}
```

### Conditional Execution

Run segments based on conditions:

```kotlin
segment("deploy") {
    condition = { ctx: ExecutionContext ->
        ctx.env("BRANCH") == "main"
    }
    execute {
        exec("./deploy.sh", "production")
    }
}
```

### Parallel Execution

Run independent segments concurrently:

```kotlin
flow {
    segment("compile")
    
    parallel {
        segment("unit-tests")
        segment("integration-tests")
        segment("lint")
    }
}
```

See [Parallel Execution](07-parallel-execution.md) for more patterns.

## Next Steps

Now that you have Kite running, explore these topics:

- **[Core Concepts](03-core-concepts.md)** - Deep dive into rides, segments, and flows
- **[Writing Segments](04-writing-segments.md)** - Create powerful, reusable segments
- **[Writing Rides](05-writing-rides.md)** - Compose complex workflows
- **[Execution Context](06-execution-context.md)** - Master the context API
- **[Artifacts](08-artifacts.md)** - Share files between segments
- **[CI Integration](11-ci-integration.md)** - Deploy to GitHub Actions, GitLab CI, etc.

## Getting Help

- **Documentation**: [docs/](00-index.md)
- **Issues**: [GitHub Issues](https://github.com/yourusername/kite/issues)
- **Examples**: Check `.kite/` in the Kite repository

---

**Welcome to Kite!** 🪁 Let's build better CI/CD workflows together.
