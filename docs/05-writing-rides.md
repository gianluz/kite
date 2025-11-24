# Writing Rides

Learn how to compose segments into powerful, flexible CI/CD workflows.

## What are Rides?

**Rides** are workflows that compose segments into complete CI/CD pipelines. A ride defines:

- **Which segments** to execute
- **In what order** (sequential, parallel, or mixed)
- **With what constraints** (max concurrency, environment variables)

Think of rides as different "ways to ride" your segments - the same segments can be composed into different workflows
for different scenarios.

## Basic Ride Structure

Every ride has:

1. A **name**
2. A **flow** defining execution order
3. Optional configuration

```kotlin
// .kite/rides/example.kite.kts
ride {
    name = "Example Ride"
    
    flow {
        segment("build")
        segment("test")
    }
}
```

## Ride Configuration

### Name

Give your ride a descriptive name:

```kotlin
ride {
    name = "MR Validation"  // Clear purpose
    // vs
    name = "ride1"  // ❌ Too vague
}
```

### Max Concurrency

Limit parallel execution:

```kotlin
ride {
    name = "CI"
    maxConcurrency = 3  // Max 3 segments running simultaneously
    
    flow {
        parallel {
            segment("test1")
            segment("test2")
            segment("test3")
            segment("test4")  // Will wait for slot
        }
    }
}
```

### Environment Variables

Set environment variables for all segments:

```kotlin
ride {
    name = "Staging Deploy"
    
    env("ENVIRONMENT", "staging")
    env("LOG_LEVEL", "debug")
    env("API_URL", "https://staging.api.example.com")
    
    flow {
        segment("deploy")
    }
}
```

## Flow Patterns

### Sequential Flow

Segments execute one after another:

```kotlin
ride {
    name = "Simple Pipeline"
    
    flow {
        segment("clean")
        segment("compile")
        segment("test")
        segment("package")
    }
}
```

**Execution**: clean → compile → test → package

### Parallel Flow

Segments execute concurrently:

```kotlin
ride {
    name = "Parallel Tests"
    
    flow {
        parallel {
            segment("unit-tests")
            segment("integration-tests")
            segment("lint")
            segment("detekt")
        }
    }
}
```

**Execution**: All four segments run simultaneously.

### Mixed Flow

Combine sequential and parallel:

```kotlin
ride {
    name = "CI"
    
    flow {
        // Phase 1: Build
        segment("compile")
        
        // Phase 2: Parallel quality checks
        parallel {
            segment("unit-tests")
            segment("integration-tests")
            segment("lint")
        }
        
        // Phase 3: Package (waits for all tests)
        segment("package")
        
        // Phase 4: Deploy
        segment("deploy")
    }
}
```

**Execution**:

1. compile
2. unit-tests, integration-tests, lint (parallel)
3. package
4. deploy

### Nested Parallel

Complex parallel structures:

```kotlin
ride {
    name = "Complex Flow"
    
    flow {
        segment("prepare")
        
        parallel {
            // Group 1: Android tests
            segment("android-unit-tests")
            segment("android-integration-tests")
            
            // Group 2: iOS tests
            segment("ios-unit-tests")
            segment("ios-integration-tests")
            
            // Group 3: Quality
            segment("lint")
        }
        
        segment("publish")
    }
}
```

## Segment Overrides

Customize segment behavior within a ride:

### Timeout Override

```kotlin
flow {
    segment("slow-test") {
        timeout = 30.minutes  // Override default timeout
    }
}
```

### Dependency Override

```kotlin
flow {
    segment("test") {
        dependsOn("lint")  // Add extra dependency in this ride
    }
}
```

### Condition Override

```kotlin
flow {
    segment("deploy") {
        condition = { ctx -> 
            ctx.branch == "main" && ctx.isCI
        }
    }
}
```

### Disable Segment

```kotlin
flow {
    segment("expensive-test") {
        enabled = false  // Skip this segment in this ride
    }
}
```

## Lifecycle Hooks

React to ride events:

### onSuccess

Called when all segments complete successfully:

```kotlin
ride {
    name = "CI"
    
    flow {
        segment("build")
        segment("test")
    }
    
    onSuccess {
        println("🎉 CI passed!")
        // Trigger deployment
        // Send notification
        // Update status
    }
}
```

### onFailure

Called when any segment fails:

```kotlin
ride {
    name = "Release"
    
    flow {
        segment("build")
        segment("test")
        segment("deploy")
    }
    
    onFailure { error ->
        println("❌ Release failed: ${error.message}")
        // Rollback
        // Create incident
        // Send alert
    }
}
```

### onComplete

Called regardless of success or failure:

```kotlin
ride {
    name = "Nightly"
    
    flow {
        segment("build")
        segment("full-test-suite")
    }
    
    onComplete { success ->
        println("Nightly build completed. Success: $success")
        // Report metrics
        // Update dashboard
        // Cleanup resources
    }
}
```

## Common Ride Patterns

### MR Validation

Fast feedback for merge requests:

```kotlin
// .kite/rides/mr.kite.kts
ride {
    name = "MR Validation"
    maxConcurrency = 3
    
    flow {
        segment("compile")
        
        parallel {
            segment("unit-tests")
            segment("lint")
            segment("detekt")
        }
        
        segment("build")
    }
    
    onSuccess {
        println("✅ MR validation passed")
    }
}
```

### CI Build

Comprehensive CI pipeline:

```kotlin
// .kite/rides/ci.kite.kts
ride {
    name = "CI"
    maxConcurrency = 4
    
    flow {
        segment("clean")
        segment("compile")
        
        parallel {
            segment("unit-tests")
            segment("integration-tests")
            segment("lint")
            segment("detekt")
        }
        
        segment("build")
        
        segment("deploy-staging") {
            condition = { ctx -> ctx.branch == "main" }
        }
    }
}
```

### Release

Production release workflow:

```kotlin
// .kite/rides/release.kite.kts
ride {
    name = "Release"
    
    env("ENVIRONMENT", "production")
    
    flow {
        segment("clean")
        segment("compile")
        
        parallel {
            segment("unit-tests")
            segment("integration-tests")
            segment("security-scan")
        }
        
        segment("build-release")
        segment("sign-artifacts")
        segment("deploy-production")
        segment("create-github-release")
    }
    
    onSuccess {
        println("🚀 Release deployed successfully!")
    }
    
    onFailure { error ->
        println("❌ Release failed, initiating rollback")
        // Automatic rollback logic
    }
}
```

### Nightly Build

Long-running comprehensive tests:

```kotlin
// .kite/rides/nightly.kite.kts
ride {
    name = "Nightly"
    
    flow {
        segment("clean")
        segment("compile")
        
        parallel {
            segment("full-test-suite")
            segment("performance-tests")
            segment("security-scan")
            segment("dependency-audit")
        }
        
        segment("code-coverage-report")
        segment("deploy-dev")
    }
    
    onComplete { success ->
        if (success) {
            println("✅ Nightly build successful")
        } else {
            println("❌ Nightly build failed - check logs")
        }
    }
}
```

### Local Development

Quick local validation:

```kotlin
// .kite/rides/local.kite.kts
ride {
    name = "Local"
    
    flow {
        segment("compile")
        segment("unit-tests")
        segment("lint")
    }
}
```

## Multiple Rides Strategy

Create different rides for different scenarios:

```
.kite/rides/
├── mr.kite.kts          # Fast feedback for MRs
├── ci.kite.kts          # Comprehensive CI
├── release.kite.kts     # Production release
├── nightly.kite.kts     # Long-running tests
├── hotfix.kite.kts      # Emergency fixes
└── local.kite.kts       # Local development
```

### When to Create a New Ride

Create a new ride when you need:

- **Different segments** (e.g., MR vs Release)
- **Different order** (e.g., fast vs thorough)
- **Different constraints** (e.g., parallel vs sequential)
- **Different environments** (e.g., staging vs production)

## Advanced Patterns

### Matrix-Style Builds

Test across multiple configurations:

```kotlin
// .kite/segments/test-matrix.kite.kts
segments {
    segment("test-jdk-17") {
        env("JAVA_VERSION", "17")
        execute { exec("./gradlew", "test") }
    }
    
    segment("test-jdk-21") {
        env("JAVA_VERSION", "21")
        execute { exec("./gradlew", "test") }
    }
}

// .kite/rides/matrix.kite.kts
ride {
    name = "Matrix Tests"
    
    flow {
        segment("compile")
        
        parallel {
            segment("test-jdk-17")
            segment("test-jdk-21")
        }
    }
}
```

### Progressive Deployment

Deploy to environments progressively:

```kotlin
ride {
    name = "Progressive Deploy"
    
    flow {
        segment("build")
        segment("test")
        
        segment("deploy-dev")
        segment("smoke-test-dev")
        
        segment("deploy-staging")
        segment("smoke-test-staging")
        
        segment("deploy-production")
        segment("smoke-test-production")
    }
    
    onFailure { error ->
        println("Deployment failed at: ${error.message}")
        // Automatic rollback
    }
}
```

### Conditional Branches

Different paths based on conditions:

```kotlin
ride {
    name = "Smart CI"
    
    flow {
        segment("compile")
        segment("unit-tests")
        
        // Only for main branch
        segment("integration-tests") {
            condition = { ctx -> ctx.branch == "main" }
        }
        
        // Only for release MRs
        segment("performance-tests") {
                condition = { ctx -> 
                    // Check for release based on your convention
                    ctx.env("CI_MERGE_REQUEST_LABELS")?.contains("release") == true
                }
        }
        
        // Deploy based on branch
        segment("deploy-staging") {
            condition = { ctx -> ctx.branch == "develop" }
        }
        
        segment("deploy-production") {
            condition = { ctx -> ctx.branch == "main" }
        }
    }
}
```

## Best Practices

### 1. Keep Rides Focused

Each ride should have a clear purpose:

✅ **Good**:

- `mr.kite.kts` - Fast MR validation
- `ci.kite.kts` - Comprehensive CI
- `release.kite.kts` - Production deployment

❌ **Bad**:

- `pipeline.kite.kts` - Does everything

### 2. Use Meaningful Names

```kotlin
// ✅ Good
ride { name = "MR Validation" }
ride { name = "Release to Production" }
ride { name = "Nightly Full Test Suite" }

// ❌ Bad  
ride { name = "Pipeline 1" }
ride { name = "Build" }
ride { name = "ride-v2-final" }
```

### 3. Optimize for Speed

Put fast checks first, slow checks in parallel:

```kotlin
flow {
    // Fast: Compilation (< 1 min)
    segment("compile")
    
    // Medium: Run in parallel (2-5 min each)
    parallel {
        segment("unit-tests")
        segment("lint")
        segment("detekt")
    }
    
    // Slow: Only if above passes (10+ min)
    segment("integration-tests")
}
```

### 4. Set Appropriate Concurrency

```kotlin
// For CI with limited resources
ride {
    maxConcurrency = 2  // Don't overwhelm
}

// For powerful CI runners
ride {
    maxConcurrency = 10  // Maximize parallelism
}

// For local development
ride {
    maxConcurrency = 1  // Sequential for debugging
}
```

### 5. Add Lifecycle Hooks

Always add hooks for observability:

```kotlin
ride {
    name = "CI"
    
    flow { /* ... */ }
    
    onSuccess {
        println("✅ CI passed")
        // Update status, trigger next steps
    }
    
    onFailure { error ->
        println("❌ CI failed: ${error.message}")
        // Alert, create issue, update status
    }
}
```

## Running Rides

### Command Line

```bash
# Run a ride
kite ride "MR Validation"

# List all rides
kite rides

# View ride flow
kite graph "MR Validation"
```

### In CI/CD

```yaml
# GitHub Actions
- name: Run CI Ride
  run: kite-cli/build/install/kite-cli/bin/kite-cli ride CI

# GitLab CI
script:
  - kite ride CI
```

See [CI Integration](11-ci-integration.md) for complete examples.

## Debugging Rides

### View Execution Plan

```bash
# See what will execute (dry run)
kite ride CI --dry-run
```

### View Dependency Graph

```bash
# Visualize segment dependencies
kite graph CI
```

### Verbose Output

```bash
# See detailed execution
kite ride CI --verbose
```

## Next Steps

- **[Execution Context](06-execution-context.md)** - Master the context API
- **[Parallel Execution](07-parallel-execution.md)** - Optimize workflows
- **[CI Integration](11-ci-integration.md)** - Deploy rides to CI/CD
- **[CLI Reference](12-cli-reference.md)** - All kite commands

---

**Master ride composition** to build flexible, powerful CI/CD workflows! 🚀
