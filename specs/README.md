# Kite Specification

This directory contains the complete specification for Kite, a Kotlin-based CI ride runner.

## Terminology

- **Ride**: A pipeline of segments (formerly "pipeline")
- **Segment**: A single unit of work in a ride (formerly "task")
- **Configuration**: A `.kite.kts` file that defines which segments to run and in what order

## Specification Documents

1. **[Overview & Problem Statement](01-overview.md)** - What Kite is and why it exists
2. **[Core Concepts](02-core-concepts.md)** - Fundamental concepts: rides, segments, configurations
3. **[DSL & Configuration](03-dsl-configuration.md)** - Kotlin DSL syntax and configuration files
4. **[Execution Model](04-execution-model.md)** - How segments are scheduled and executed
5. **[Parallelization](05-parallelization.md)** - Deep dive into parallel execution
6. **[Built-in Features](06-builtin-features.md)** - Helpers, artifacts, error handling
7. **[Plugin System](07-plugin-system.md)** - Extending Kite with plugins
8. **[Platform Integration](08-platform-integration.md)** - GitLab CI, GitHub Actions, local execution
9. **[Architecture](09-architecture.md)** - Technical architecture and components

## Quick Reference

### File Structure

```
project-root/
├── .kite/
│   ├── segments/
│   │   ├── build.kite.kts        # Build segments
│   │   ├── test.kite.kts         # Test segments
│   │   └── deploy.kite.kts       # Deployment segments
│   ├── rides/
│   │   ├── mr.kite.kts           # MR ride configuration
│   │   ├── release.kite.kts      # Release ride configuration
│   │   └── nightly.kite.kts      # Nightly ride configuration
│   └── settings.kite.kts         # Global settings (optional)
└── kite                          # Kite executable
```

### Basic Usage

```bash
# Run a specific ride
./kite ride mr

# Run specific segments
./kite run build test

# List all segments
./kite segments

# List all rides
./kite rides

# Visualize ride graph
./kite graph mr
```

### Example Segment Definition

```kotlin
// .kite/segments/build.kite.kts
segments {
    segment("build") {
        description = "Build the application"
        
        execute {
            exec("./gradlew", "assembleRelease")
        }
    }
}
```

### Example Ride Configuration

```kotlin
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
