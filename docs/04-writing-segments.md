# Writing Segments

Learn how to create powerful, reusable segments for your CI/CD workflows.

## What are Segments?

**Segments** are the fundamental building blocks of Kite workflows. Each segment represents a discrete unit of work like
compiling code, running tests, or deploying an application.

Good segments are:

- **Self-contained** - Do one thing well
- **Reusable** - Can be used in multiple rides
- **Testable** - Have clear inputs and outputs
- **Composable** - Can depend on other segments

## Basic Segment Structure

Every segment has:

1. A unique **name**
2. An **execute** block with the actual work
3. Optional configuration (timeout, dependencies, etc.)

```kotlin
// .kite/segments/example.kite.kts
segments {
    segment("my-segment") {
        description = "What this segment does"
        
        execute {
            // Your code here
        }
    }
}
```

## Segment Configuration

### Description

Add a human-readable description:

```kotlin
segment("build") {
    description = "Compile Kotlin code and generate bytecode"
    
    execute {
        exec("./gradlew", "compileKotlin")
    }
}
```

### Timeout

Set a maximum execution time:

```kotlin
segment("integration-tests") {
    description = "Run integration test suite"
    timeout = 15.minutes
    
    execute {
        exec("./gradlew", "integrationTest")
    }
}
```

### Dependencies

Declare dependencies on other segments:

```kotlin
segment("test") {
    description = "Run unit tests"
    dependsOn("compile")  // Must run after compile
    
    execute {
        exec("./gradlew", "test")
    }
}

segment("deploy") {
    description = "Deploy to production"
    dependsOn("build", "test", "lint")  // Multiple dependencies
    
    execute {
        exec("./deploy.sh")
    }
}
```

### Conditional Execution

Execute based on conditions:

```kotlin
segment("deploy-production") {
    description = "Deploy to production"
    condition = { ctx ->
        ctx.branch == "main" && ctx.isCI
    }
    
    execute {
        exec("./deploy.sh", "production")
    }
}

segment("deploy-staging") {
    description = "Deploy to staging"
    condition = { ctx ->
        ctx.branch == "develop"
    }
    
    execute {
        exec("./deploy.sh", "staging")
    }
}
```

### Retries

Handle transient failures:

```kotlin
segment("flaky-test") {
    description = "Run flaky integration tests"
    maxRetries = 3
    retryDelay = 10.seconds
    retryOn("java.net.SocketTimeoutException", "java.io.IOException")
    
    execute {
        exec("./gradlew", "integrationTest")
    }
}
```

## Execution Block

The `execute` block contains your segment's logic. It has access to the `ExecutionContext`:

### Running Commands

```kotlin
segment("build") {
    execute {
        // Run a command
        exec("./gradlew", "build")
        
        // Run with custom working directory
        exec("npm", "install", workingDir = File("frontend"))
        
        // Run with timeout
        exec("./long-task.sh", timeout = 30.minutes)
        
        // Run and ignore failures
        val result = execOrNull("./gradlew", "lint")
        if (result == null) {
            println("Lint failed, continuing anyway")
        }
        
        // Run shell command
        shell("git log -1 --pretty=%B > commit-message.txt")
    }
}
```

### Environment Variables

```kotlin
segment("deploy") {
    execute {
        // Get environment variable (nullable)
        val apiKey = env("API_KEY")
        
        // Get with default
        val environment = envOrDefault("ENVIRONMENT", "staging")
        
        // Require environment variable (throws if missing)
        val deployUrl = requireEnv("DEPLOY_URL")
        
        // Get secret (auto-masked in logs)
        val token = requireSecret("GITHUB_TOKEN")
        
        // Use in command
        exec("curl", "-H", "Authorization: Bearer $token", deployUrl)
        // Logs will show: Authorization: Bearer [GITHUB_TOKEN:***]
    }
}
```

### File Operations

```kotlin
segment("prepare-config") {
    execute {
        // Read file
        val config = readFile("config.template")
        
        // Write file
        writeFile("config.prod", config.replace("{{ENV}}", "production"))
        
        // Check if file exists
        if (fileExists("custom-config.txt")) {
            val custom = readFile("custom-config.txt")
            println("Using custom config: $custom")
        }
        
        // Copy file
        copyFile("build/app.jar", "dist/app.jar")
        
        // Create directory
        createDirectory("dist/config")
        
        // List files
        val testFiles = findFiles("**/*Test.kt")
        println("Found ${testFiles.size} test files")
    }
}
```

### Context Information

```kotlin
segment("info") {
    execute {
        println("Branch: $branch")
        println("Commit: $commitSha")
        println("Platform: ${ciPlatform.displayName}")
        
        if (isLocal) {
            println("Running locally")
        } else {
            println("Running in CI")
        }
        
        if (isMergeRequest) {
            println("MR Number: $mrNumber")
        }
        
        if (isRelease) {
            println("This is a release build")
        }
    }
}
```

## Artifacts

Segments can produce and consume artifacts:

### Producing Artifacts

```kotlin
segment("build-apk") {
    description = "Build Android APK"
    
    execute {
        exec("./gradlew", "assembleRelease")
    }
    
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")
        artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
    }
}
```

### Consuming Artifacts

```kotlin
segment("upload-apk") {
    description = "Upload APK to distribution"
    dependsOn("build-apk")
    
    inputs {
        artifact("apk")
    }
    
    execute {
        val apkPath = artifacts.get("apk")
        exec("./upload.sh", apkPath.toString())
    }
}
```

See [Artifacts](08-artifacts.md) for complete documentation.

## Lifecycle Hooks

React to segment events:

### onSuccess

Called when segment completes successfully:

```kotlin
segment("deploy") {
    execute {
        exec("./deploy.sh")
    }
    
    onSuccess {
        println("✅ Deployment successful!")
        // Send Slack notification
        shell("curl -X POST $SLACK_WEBHOOK -d '{\"text\":\"Deploy succeeded\"}'")
    }
}
```

### onFailure

Called when segment fails (after all retries):

```kotlin
segment("critical-task") {
    maxRetries = 3
    
    execute {
        exec("./important-task.sh")
    }
    
    onFailure { error ->
        println("❌ Task failed: ${error.message}")
        // Create incident ticket
        exec("./create-incident.sh", error.message ?: "Unknown error")
    }
}
```

### onComplete

Called regardless of success or failure:

```kotlin
segment("test") {
    execute {
        exec("./gradlew", "test")
    }
    
    onComplete { status ->
        println("Test execution completed with status: $status")
        
        // Always collect test results
        if (fileExists("build/test-results")) {
            copyFile("build/test-results", ".kite/artifacts/test-results")
        }
        
        // Update metrics
        when (status) {
            SegmentStatus.SUCCESS -> println("✅ Tests passed")
            SegmentStatus.FAILURE -> println("❌ Tests failed")
            SegmentStatus.TIMEOUT -> println("⏱️ Tests timed out")
            else -> println("⚠️ Unexpected status: $status")
        }
    }
}
```

## Multiple Segments per File

Organize related segments together:

```kotlin
// .kite/segments/android.kite.kts
segments {
    segment("compile") {
        description = "Compile Kotlin code"
        execute {
            exec("./gradlew", "compileDebugKotlin")
        }
    }
    
    segment("lint") {
        description = "Run Android lint"
        dependsOn("compile")
        execute {
            exec("./gradlew", "lintDebug")
        }
    }
    
    segment("unit-tests") {
        description = "Run unit tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", "testDebugUnitTest")
        }
    }
    
    segment("assemble") {
        description = "Build APK"
        dependsOn("lint", "unit-tests")
        execute {
            exec("./gradlew", "assembleDebug")
        }
        outputs {
            artifact("apk", "app/build/outputs/apk/debug/app-debug.apk")
        }
    }
}
```

## Helper Functions

Define reusable functions within your segments:

```kotlin
// .kite/segments/gradle.kite.kts

// Helper function
fun ExecutionContext.gradleTask(vararg tasks: String) {
    exec("./gradlew", *tasks)
}

fun ExecutionContext.gradleClean() = gradleTask("clean")

segments {
    segment("build") {
        execute {
            gradleClean()
            gradleTask("build")
        }
    }
    
    segment("test") {
        dependsOn("build")
        execute {
            gradleTask("test")
        }
    }
}
```

## Common Patterns

### Gradle Tasks

```kotlin
segments {
    segment("clean") {
        execute { exec("./gradlew", "clean") }
    }
    
    segment("compile") {
        dependsOn("clean")
        execute { exec("./gradlew", "compileKotlin") }
    }
    
    segment("test") {
        dependsOn("compile")
        timeout = 10.minutes
        execute { exec("./gradlew", "test") }
    }
    
    segment("package") {
        dependsOn("test")
        execute { exec("./gradlew", "jar") }
    }
}
```

### Docker Build

```kotlin
segment("docker-build") {
    description = "Build Docker image"
    timeout = 15.minutes
    
    execute {
        val tag = env("IMAGE_TAG") ?: commitSha.take(8)
        exec("docker", "build", "-t", "myapp:$tag", ".")
    }
    
    onSuccess {
        println("✅ Docker image built successfully")
    }
}

segment("docker-push") {
    description = "Push Docker image to registry"
    dependsOn("docker-build")
    condition = { ctx -> ctx.branch == "main" }
    
    execute {
        val tag = env("IMAGE_TAG") ?: commitSha.take(8)
        exec("docker", "push", "myapp:$tag")
    }
}
```

### Node.js/npm Tasks

```kotlin
segment("npm-install") {
    description = "Install npm dependencies"
    execute {
        exec("npm", "ci")
    }
}

segment("npm-build") {
    description = "Build frontend"
    dependsOn("npm-install")
    execute {
        exec("npm", "run", "build")
    }
    outputs {
        artifact("dist", "dist/")
    }
}
```

### Database Migration

```kotlin
segment("db-migrate") {
    description = "Run database migrations"
    timeout = 5.minutes
    maxRetries = 2
    retryDelay = 5.seconds
    
    execute {
        val dbUrl = requireSecret("DATABASE_URL")
        exec("./migrate.sh", dbUrl)
    }
    
    onFailure { error ->
        println("❌ Migration failed: ${error.message}")
        // Rollback or alert
    }
}
```

## Best Practices

### 1. Keep Segments Focused

❌ **Bad**: One segment does everything

```kotlin
segment("build-test-deploy") {
    execute {
        exec("./gradlew", "build")
        exec("./gradlew", "test")
        exec("./deploy.sh")
    }
}
```

✅ **Good**: Separate segments for each concern

```kotlin
segment("build") {
    execute { exec("./gradlew", "build") }
}

segment("test") {
    dependsOn("build")
    execute { exec("./gradlew", "test") }
}

segment("deploy") {
    dependsOn("test")
    execute { exec("./deploy.sh") }
}
```

### 2. Use Clear Names

- `build` not `b`
- `unit-tests` not `test1`
- `deploy-production` not `deploy-prod-v2-final`

### 3. Add Descriptions

Always add descriptions for clarity:

```kotlin
segment("roborazzi") {
    description = "Run Roborazzi screenshot tests"
    execute { exec("./gradlew", "verifyRoborazziDebug") }
}
```

### 4. Set Appropriate Timeouts

Prevent hanging builds:

```kotlin
segment("quick-task") {
    timeout = 1.minutes
    execute { /* ... */ }
}

segment("long-task") {
    timeout = 30.minutes
    execute { /* ... */ }
}
```

### 5. Use Secrets Properly

Always use `secret()` or `requireSecret()` for sensitive data:

```kotlin
segment("deploy") {
    execute {
        // ❌ Bad: Exposed in logs
        val token = env("GITHUB_TOKEN")
        
        // ✅ Good: Auto-masked
        val token = requireSecret("GITHUB_TOKEN")
        
        exec("curl", "-H", "Authorization: Bearer $token", "...")
    }
}
```

## Organizing Segments

### By Function

```
.kite/segments/
├── build.kite.kts        # Build-related segments
├── test.kite.kts         # Test segments
├── deploy.kite.kts       # Deployment segments
└── quality.kite.kts      # Linting, formatting, etc.
```

### By Module (Monorepo)

```
.kite/segments/
├── app/
│   ├── build.kite.kts
│   └── test.kite.kts
├── library/
│   ├── build.kite.kts
│   └── test.kite.kts
└── shared/
    └── quality.kite.kts
```

### By Platform

```
.kite/segments/
├── android/
│   ├── build.kite.kts
│   ├── test.kite.kts
│   └── deploy.kite.kts
└── ios/
    ├── build.kite.kts
    └── test.kite.kts
```

## Next Steps

- **[Writing Rides](05-writing-rides.md)** - Compose segments into workflows
- **[Execution Context](06-execution-context.md)** - Master the context API
- **[Artifacts](08-artifacts.md)** - Share data between segments
- **[Secrets](09-secrets.md)** - Handle sensitive data securely

---

**Master segment authoring** to build powerful, reusable CI/CD components! 🎯
