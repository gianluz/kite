# CLI Reference

Complete reference for the Kite command-line interface.

---

## Overview

The Kite CLI provides commands to execute rides, run individual segments, and inspect your workflow definitions.

**Command name:** The executable is called `kite-cli`

**Basic usage:**

```bash
kite-cli <command> [options] [arguments]
```

**Note:** Throughout this reference, examples use `kite-cli`. You can create a shell alias `kite` for convenience (see
Installation section below).

---

## Installation

See the full [Installation Guide](02-installation.md#method-3-cli-binary-cicd-and-local) for all options. Quick summary:

### Recommended: Docker (no Java required)

```bash
docker run --rm -v $(pwd):/workspace ghcr.io/gianluz/kite:latest ride CI
```

### Install script (macOS / Linux)

```bash
curl -sSL https://github.com/gianluz/kite/releases/latest/download/install.sh | bash
export PATH="$HOME/.kite/bin:$PATH"
kite-cli ride CI
```

### Build from source

```bash
./gradlew :kite-cli:installDist
export PATH="$PWD/kite-cli/build/install/kite-cli/bin:$PATH"
kite-cli ride CI
```

### Create a `kite` alias (optional shortcut)

```bash
alias kite='kite-cli'
kite ride CI
```

**Note:** Examples in this reference use `kite-cli` (the actual executable name).

---

## Global Options

Available for all commands:

| Option | Alias | Description |
|--------|-------|-------------|
| `--version` | `-V` | Show version information |
| `--help` | `-h` | Show help message |
| `--verbose` | `-v` | Enable verbose output |
| `--debug` | `-d` | Enable debug output with stack traces |
| `--quiet` | `-q` | Suppress non-essential output |

**Examples:**

```bash
kite-cli --version
kite-cli --help
kite-cli ride CI --verbose
kite-cli run test --debug
```

---

## Commands

### `ride` - Execute a Ride

Execute a named ride from `.kite/rides/<name>.kite.kts`.

**Usage:**

```bash
kite-cli ride <name> [options]
```

**Arguments:**

- `<name>` - Name of the ride to execute (required)

**Options:**

- `--dry-run` - Show execution plan without running
- `--sequential` - Force sequential execution (disable parallelism)

**Examples:**

```bash
# Execute a ride
kite-cli ride CI

# Dry run (show plan without executing)
kite-cli ride CI --dry-run

# Force sequential execution
kite-cli ride CI --sequential

# Verbose output
kite-cli ride CI --verbose
```

**Output:**

```
  ██╗  ██╗██╗████████╗███████╗
  ██║ ██╔╝██║╚══██╔══╝██╔════╝
  █████╔╝ ██║   ██║   █████╗  
  ██╔═██╗ ██║   ██║   ██╔══╝  
  ██║  ██╗██║   ██║   ███████╗
  ╚═╝  ╚═╝╚═╝   ╚═╝   ╚══════╝

══════════════════════════════════════════════════
  🪁 Kite Ride: CI
══════════════════════════════════════════════════

▶ Execution Plan
ℹ Segments to execute: 5
  • clean
  • compile
  • test (depends on: compile)
  • lint (depends on: compile)
  • assemble (depends on: test, lint)

▶ Executing Ride
✓ clean (1.2s)
✓ compile (15.3s)
✓ test (8.7s)
✓ lint (2.1s)
✓ assemble (4.3s)

▶ Results
  ✓ clean        SUCCESS    (1.2s)
  ✓ compile      SUCCESS   (15.3s)
  ✓ test         SUCCESS    (8.7s)
  ✓ lint         SUCCESS    (2.1s)
  ✓ assemble     SUCCESS    (4.3s)

Summary:
  Total: 5 segments
  ✓ Success: 5
  Duration: 31.6s (sequential: 31.6s, parallel: 31.6s)

🎉 All segments completed successfully!
```

**Dry Run Output:**

```
Execution Plan:
  • clean
  • compile
  • test (depends on: compile)
  • lint (depends on: compile)
  • assemble (depends on: test, lint)

Dry run mode - execution skipped
```

---

### `run` - Execute Specific Segments

Execute one or more segments by name, including their dependencies.

**Usage:**

```bash
kite-cli run <segment>... [options]
```

**Arguments:**

- `<segment>...` - Names of segments to execute (one or more required)

**Options:**

- `--dry-run` - Show execution plan without running
- `--sequential` - Force sequential execution

**Examples:**

```bash
# Run a single segment (and its dependencies)
kite-cli run test

# Run multiple segments
kite-cli run test lint

# Dry run
kite-cli run test lint --dry-run

# Sequential execution
kite-cli run test --sequential
```

**Output:**

```
🏃 Running Segments

▶ Execution Plan
ℹ Segments to execute: 3
  • compile
  • test (depends on: compile) [requested]
  • lint (depends on: compile) [requested]

▶ Executing Segments
✓ compile (15.3s)
✓ test (8.7s)
✓ lint (2.1s)

▶ Results
  ✓ compile      SUCCESS   (15.3s)
  ✓ test         SUCCESS    (8.7s)
  ✓ lint         SUCCESS    (2.1s)

Summary:
  Total: 3 segments
  ✓ Success: 3
  Duration: 26.1s
```

**Note:** Dependencies are automatically included and executed first.

---

### `segments` - List All Segments

List all available segments from `.kite/segments/*.kite.kts`.

**Usage:**

```bash
kite-cli segments [options]
```

**Options:**

- `--json` - Output in JSON format

**Examples:**

```bash
# List all segments
kite-cli segments

# JSON output
kite-cli segments --json
```

**Output:**

```
📦 Available Segments

Found 8 segments:

  build
    Build the application
    Timeout: 10m

  test
    Run unit tests
    Depends on: build
    Max retries: 2

  lint
    Run code style checks
    Depends on: build

  deploy
    Deploy to production
    Depends on: test, lint
    ⚠ Conditional execution

Total: 8 segments
```

**JSON Output:**

```json
{
  "segments": [
    {
      "name": "build",
      "description": "Build the application",
      "dependsOn": [],
      "timeout": "10m",
      "maxRetries": 0,
      "hasCondition": false
    },
    {
      "name": "test",
      "description": "Run unit tests",
      "dependsOn": ["build"],
      "timeout": null,
      "maxRetries": 2,
      "hasCondition": false
    }
  ],
  "total": 8
}
```

---

### `rides` - List All Rides

List all available rides from `.kite/rides/*.kite.kts`.

**Usage:**

```bash
kite-cli rides [options]
```

**Options:**

- `--json` - Output in JSON format

**Examples:**

```bash
# List all rides
kite-cli rides

# JSON output
kite-cli rides --json
```

**Output:**

```
🎢 Available Rides

Found 3 rides:

  CI
    Segments: 5
    Max concurrency: 4

  MR
    Segments: 3
    Max concurrency: 2

  Deploy
    Segments: 6

Total: 3 rides
```

**JSON Output:**

```json
{
  "rides": [
    {
      "name": "CI",
      "segmentCount": 5,
      "maxConcurrency": 4
    },
    {
      "name": "MR",
      "segmentCount": 3,
      "maxConcurrency": 2
    },
    {
      "name": "Deploy",
      "segmentCount": 6,
      "maxConcurrency": null
    }
  ],
  "total": 3
}
```

---

### `graph` - Visualize Dependency Graph

_(Not yet implemented)_

Visualize the dependency graph for a ride.

**Usage:**

```bash
kite-cli graph <name>
```

**Arguments:**

- `<name>` - Name of the ride to visualize

**Example:**

```bash
kite-cli graph CI
```

---

## Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success - all segments completed successfully |
| `1` | Failure - one or more segments failed |
| `2` | Error - CLI error (invalid arguments, missing files, etc.) |

**Usage in scripts:**

```bash
#!/bin/bash

if kite-cli ride CI; then
    echo "Build successful!"
    exit 0
else
    echo "Build failed!"
    exit 1
fi
```

---

## Environment Variables

### Kite-Specific

| Variable | Description | Default |
|----------|-------------|---------|
| `KITE_HOME` | Kite home directory | `.kite` |
| `KITE_WORKSPACE` | Workspace root | Current directory |

### Standard CI Variables

Kite automatically detects and uses standard CI/CD environment variables:

**GitHub Actions:**

- `GITHUB_REF` - Branch/tag reference
- `GITHUB_SHA` - Commit SHA
- `GITHUB_ACTOR` - User who triggered the workflow
- `GITHUB_REPOSITORY` - Repository name

**GitLab CI:**

- `CI_COMMIT_BRANCH` - Branch name
- `CI_COMMIT_SHA` - Commit SHA
- `CI_PIPELINE_ID` - Pipeline ID
- `CI_PROJECT_NAME` - Project name

**Jenkins:**

- `BRANCH_NAME` - Branch name
- `BUILD_NUMBER` - Build number
- `JOB_NAME` - Job name

Access in segments via `env()`:

```kotlin
segment("info") {
    execute {
        val branch = env("GITHUB_REF") ?: env("CI_COMMIT_BRANCH") ?: "unknown"
        logger.info("Building branch: $branch")
    }
}
```

---

## Configuration Files

### Directory Structure

```
project/
├── .kite/
│   ├── segments/
│   │   ├── build.kite.kts
│   │   ├── test.kite.kts
│   │   └── deploy.kite.kts
│   ├── rides/
│   │   ├── ci.kite.kts
│   │   ├── mr.kite.kts
│   │   └── deploy.kite.kts
│   └── artifacts/
│       └── (generated at runtime)
└── build.gradle.kts
```

### Segment Files

**Location:** `.kite/segments/<name>.kite.kts`

**Format:**

```kotlin
// .kite/segments/test.kite.kts
segments {
    segment("test") {
        description = "Run tests"
        dependsOn("build")
        
        execute {
            exec("./gradlew", "test")
        }
    }
}
```

Multiple segments can be defined in one file.

### Ride Files

**Location:** `.kite/rides/<name>.kite.kts`

**Format:**

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
        }
    }
}
```

One ride per file.

---

## Artifacts

Kite automatically manages artifacts in `.kite/artifacts/`:

```
.kite/artifacts/
├── apk/
│   └── app-release.apk
├── test-results/
│   ├── test1.xml
│   └── test2.xml
└── manifest.json  # Artifact metadata
```

**Produced by segments:**

```kotlin
segment("build") {
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")
    }
    
    execute {
        exec("./gradlew", "assembleRelease")
    }
}
```

**Consumed by segments:**

```kotlin
segment("test") {
    dependsOn("build")
    inputs { artifact("apk") }
    
    execute {
        val apk = artifacts.get("apk")!!
        exec("adb", "install", "$apk")
    }
}
```

---

## Logging

### Log Levels

Kite uses three log levels:

| Level | Description | Example |
|-------|-------------|---------|
| **INFO** | Normal operation | `logger.info("Starting build")` |
| **WARN** | Warnings | `logger.warn("Using default config")` |
| **ERROR** | Errors | `logger.error("Build failed")` |

### Output Modes

**Normal (default):**

- Shows progress and results
- Colored output in terminals
- Formatted output

**Verbose (`--verbose`):**

- Shows additional details
- Platform information
- Dependency resolution
- Execution details

**Quiet (`--quiet`):**

- Only shows errors
- Minimal output
- Good for scripts

**Debug (`--debug`):**

- Full debug information
- Stack traces on errors
- Detailed execution flow

**Examples:**

```bash
# Normal output
kite-cli ride CI

# Verbose - more details
kite-cli ride CI --verbose

# Quiet - minimal output
kite-cli ride CI --quiet

# Debug - full details
kite-cli ride CI --debug
```

---

## Best Practices

### 1. Use Meaningful Ride Names

```bash
# ✅ Good - clear purpose
kite-cli ride CI
kite-cli ride PR
kite-cli ride Deploy

# ❌ Bad - vague
kite-cli ride test1
kite-cli ride run
```

### 2. Organize Segments by Purpose

```
.kite/segments/
├── build.kite.kts      # Build-related segments
├── test.kite.kts       # Test segments
├── quality.kite.kts    # Linting, formatting
└── deploy.kite.kts     # Deployment segments
```

### 3. Use Dry Run Before Major Changes

```bash
# Check what will execute
kite-cli ride Deploy --dry-run

# If it looks good, run it
kite-cli ride Deploy
```

### 4. Capture Output in CI

```bash
# Save output to file
kite-cli ride CI 2>&1 | tee build.log

# Upload to CI artifacts
```

### 5. Use Exit Codes in Scripts

```bash
#!/bin/bash
set -e  # Exit on error

# Run CI
kite-cli ride CI

# Only deploy if CI passes
kite-cli ride Deploy
```

---

## Troubleshooting

### Problem: "No .kite directory found"

**Solution:**
Create the directory structure:

```bash
mkdir -p .kite/segments .kite/rides
```

### Problem: "Ride not found"

**Solution:**
List available rides:

```bash
kite-cli rides
```

Ensure your ride file is in `.kite/rides/` and matches the name you're using.

### Problem: "Segment not found"

**Solution:**
List available segments:

```bash
kite-cli segments
```

Check dependencies are defined correctly.

### Problem: Command not found

**Solution:**
Build the CLI first:

```bash
./gradlew :kite-cli:installDist
```

Use the full path:

```bash
kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

---

## Quick Reference

### Common Commands

```bash
# Execute rides
kite-cli ride CI
kite-cli ride Deploy --dry-run

# Run specific segments
kite-cli run test lint
kite-cli run build test --sequential

# List available items
kite-cli segments
kite-cli rides

# Get help
kite-cli --help
kite-cli ride --help
```

### Common Options

```bash
--verbose        # More details
--debug          # Full debug output
--quiet          # Minimal output
--dry-run        # Show plan without executing
--sequential     # Disable parallelism
--json           # JSON output (for segments/rides commands)
```

### Quick Setup

```bash
# 1. Create structure
mkdir -p .kite/segments .kite/rides

# 2. Create a segment
cat > .kite/segments/build.kite.kts << 'EOF'
segments {
    segment("build") {
        execute {
            exec("./gradlew", "build")
        }
    }
}
EOF

# 3. Create a ride
cat > .kite/rides/ci.kite.kts << 'EOF'
ride {
    name = "CI"
    flow {
        segment("build")
    }
}
EOF

# 4. Build and run
./gradlew :kite-cli:installDist
kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

---

## Summary

**Core Commands:**

- `kite ride <name>` - Execute a complete workflow
- `kite run <segment>...` - Execute specific segments
- `kite segments` - List available segments
- `kite rides` - List available rides

**Key Features:**

- ✅ Automatic dependency resolution
- ✅ Parallel execution support
- ✅ Dry-run capability
- ✅ JSON output for automation
- ✅ Colored terminal output
- ✅ Progress tracking

**Exit Codes:**

- `0` = Success
- `1` = Failure
- `2` = Error

---

## Related Topics

- [Getting Started](01-getting-started.md) - Quick start guide
- [Writing Segments](04-writing-segments.md) - Create segments
- [Writing Rides](05-writing-rides.md) - Create rides
- [CI Integration](11-ci-integration.md) - Use in CI/CD pipelines

---

## Next Steps

- [Troubleshooting guide →](99-troubleshooting.md)
- [Back to documentation home →](00-index.md)
