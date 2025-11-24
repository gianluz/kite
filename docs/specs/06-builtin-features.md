# Built-in Features

> **⚠️ Deprecation Notice**: This document references deprecated properties like `mrNumber`, `isRelease`, and `ciPlatform`.
> These are deprecated in favor of Kite's platform-agnostic design. See [Execution Context - Platform-Specific Environment Variables](../06-execution-context.md#platform-specific-environment-variables)
> document for the recommended approach. Users should check environment variables directly using `env()`.


## Overview

Kite provides a rich set of built-in features and helper functions to simplify common CI/CD operations. These helpers
are available in all segments without additional configuration.

## Command Execution

### Basic Execution

```kotlin
segment("build") {
    execute {
        // Execute command and throw on failure
        exec("./gradlew", "assembleRelease")
        
        // Execute and return null on failure
        val result = execOrNull("./optional-script.sh")
        
        // Execute shell command
        shell("find . -name '*.kt' | wc -l")
    }
}
```

### Advanced Execution

```kotlin
segment("advancedExec") {
    execute {
        // With working directory
        exec("npm", "install", workingDir = file("frontend"))
        
        // With environment variables
        exec("./script.sh", env = mapOf(
            "ENV" to "production",
            "API_KEY" to env("API_KEY")
        ))
        
        // Capture output
        val output = execAndCapture("git", "rev-parse", "HEAD")
        println("Current commit: $output")
    }
}
```

## File Operations

### Basic File Operations

```kotlin
segment("fileOps") {
    execute {
        // Copy files
        copy(source = file("app/build/outputs/apk"), destination = file("artifacts/"))
        
        // Move files
        move(source = file("temp/report.html"), destination = file("reports/"))
        
        // Delete files/directories
        delete(file("build/"))
        
        // Create directory
        createDirectory(file("artifacts/reports"))
        
        // Check existence
        if (file("config.json").exists()) {
            println("Config found")
        }
    }
}
```

### Archive Operations

```kotlin
segment("archiveOps") {
    execute {
        // Create zip archive
        zipFiles(
            source = file("reports/"),
            destination = file("reports.zip")
        )
        
        // Extract zip archive
        unzipFiles(
            source = file("artifacts.zip"),
            destination = file("extracted/")
        )
        
        // Create tar.gz archive
        tarGzFiles(
            source = file("build/"),
            destination = file("build.tar.gz")
        )
    }
}
```

### File Reading and Writing

```kotlin
segment("fileIO") {
    execute {
        // Read file
        val content = file("version.txt").readText()
        
        // Write file
        file("output.txt").writeText("Build completed at ${System.currentTimeMillis()}")
        
        // Append to file
        file("log.txt").appendText("New entry\n")
        
        // Read lines
        val lines = file("config.txt").readLines()
        lines.forEach { println(it) }
    }
}
```

## Environment Variables

### Reading Environment

```kotlin
segment("envAccess") {
    execute {
        // Read environment variable
        val apiKey = env("API_KEY")
        
        // Read with default value
        val timeout = env("TIMEOUT")?.toIntOrNull() ?: 30
        
        // Check if variable exists
        if (envExists("CI")) {
            println("Running in CI environment")
        }
        
        // Get all environment variables
        val allEnv = environment()
        allEnv.forEach { (key, value) ->
            println("$key = $value")
        }
    }
}
```

### Setting Environment for Subprocess

```kotlin
segment("envSetting") {
    execute {
        // Set environment for a command
        exec(
            "deploy.sh",
            env = mapOf(
                "DEPLOY_ENV" to "production",
                "API_ENDPOINT" to "https://api.example.com"
            )
        )
    }
}
```

## Artifact Management

### Producing Artifacts

```kotlin
segment("build") {
    execute {
        exec("./gradlew", "assembleRelease")
    }
    
    outputs {
        // Store single artifact
        artifact("apk", file("app/build/outputs/apk/release/app-release.apk"))
        
        // Store multiple artifacts
        artifact("mapping", file("app/build/outputs/mapping/release/mapping.txt"))
        artifact("symbols", file("app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip"))
    }
}
```

### Consuming Artifacts

```kotlin
segment("deploy") {
    dependsOn = listOf("build")
    
    execute {
        // Retrieve artifact
        val apk = artifacts.get("apk")
        val mapping = artifacts.get("mapping")
        
        // Check if artifact exists
        if (artifacts.has("apk")) {
            uploadToPlayStore(artifacts.get("apk"))
        }
        
        // List all artifacts
        val allArtifacts = artifacts.list()
        allArtifacts.forEach { (name, file) ->
            println("Artifact: $name -> ${file.absolutePath}")
        }
    }
}
```

## Android/Mobile Specific Helpers

### APK/AAB Operations

```kotlin
segment("buildAndroid") {
    execute {
        // Build APK
        buildApk(variant = "release")
        
        // Build AAB (Android App Bundle)
        buildAab(variant = "release")
        
        // Sign APK
        signApk(
            apk = file("app/build/outputs/apk/release/app-release-unsigned.apk"),
            keystore = file("keystore.jks"),
            alias = "release",
            storePassword = env("KEYSTORE_PASSWORD")!!,
            keyPassword = env("KEY_PASSWORD")!!
        )
    }
}
```

### Running Tests

```kotlin
segment("androidTests") {
    execute {
        // Run unit tests
        runTests(variant = "release")
        
        // Run instrumented tests
        runInstrumentedTests(variant = "debug")
        
        // Run specific test class
        runTest(
            testClass = "com.example.MyTest",
            variant = "debug"
        )
    }
}
```

## Version Management

### Reading Version

```kotlin
segment("getVersion") {
    execute {
        // Get current version from build file
        val version = getCurrentVersion()
        println("Current version: $version")
        
        // Get version from Git tags
        val gitVersion = getVersionFromGit()
        println("Git version: $gitVersion")
    }
}
```

### Bumping Version

```kotlin
segment("bumpVersion") {
    execute {
        // Bump patch version (1.0.0 -> 1.0.1)
        bumpVersion(type = VersionBump.PATCH)
        
        // Bump minor version (1.0.0 -> 1.1.0)
        bumpVersion(type = VersionBump.MINOR)
        
        // Bump major version (1.0.0 -> 2.0.0)
        bumpVersion(type = VersionBump.MAJOR)
        
        // Custom version
        setVersion("2.0.0-beta1")
    }
}
```

### Tagging Releases

```kotlin
segment("tagRelease") {
    execute {
        val version = getCurrentVersion()
        
        // Create Git tag
        tagRelease(
            version = version,
            message = "Release $version"
        )
        
        // Push tag
        exec("git", "push", "origin", "v$version")
    }
}
```

## Notifications

### Slack Notifications

```kotlin
segment("notifySlack") {
    execute {
        // Simple message
        notifySlack(
            webhook = env("SLACK_WEBHOOK")!!,
            message = "Build completed successfully!"
        )
        
        // Rich message with fields
        notifySlack(
            webhook = env("SLACK_WEBHOOK")!!,
            message = "Deployment Complete",
            channel = "#deployments",
            fields = mapOf(
                "Version" to getCurrentVersion(),
                "Branch" to context.branch,
                "Commit" to context.commitSha,
                "Duration" to "5m 32s"
            ),
            color = "good"  // or "warning", "danger"
        )
    }
}
```

### Email Notifications

```kotlin
segment("notifyEmail") {
    execute {
        notifyEmail(
            to = listOf("team@example.com"),
            subject = "Build Status: ${context.branch}",
            body = """
                Build completed for branch ${context.branch}
                Commit: ${context.commitSha}
                Status: Success
            """.trimIndent()
        )
    }
}
```

## Git Operations

### Basic Git Commands

```kotlin
segment("gitOps") {
    execute {
        // Get current branch
        val branch = gitBranch()
        
        // Get current commit SHA
        val commit = gitCommitSha()
        
        // Get commit message
        val message = gitCommitMessage()
        
        // Check if working directory is clean
        val isClean = gitIsClean()
        if (!isClean) {
            println("Warning: Working directory has uncommitted changes")
        }
    }
}
```

### Advanced Git Operations

```kotlin
segment("advancedGit") {
    execute {
        // Get changed files
        val changedFiles = gitChangedFiles()
        println("Changed files: ${changedFiles.joinToString()}")
        
        // Check if branch exists
        if (gitBranchExists("release/1.0")) {
            println("Release branch exists")
        }
        
        // Get tags
        val tags = gitTags()
        println("Tags: ${tags.joinToString()}")
        
        // Get latest tag
        val latestTag = gitLatestTag()
        println("Latest tag: $latestTag")
    }
}
```

## HTTP/REST Operations

### HTTP Requests

```kotlin
segment("httpOps") {
    execute {
        // GET request
        val response = httpGet("https://api.example.com/status")
        println("Status: ${response.statusCode}")
        println("Body: ${response.body}")
        
        // POST request
        val postResponse = httpPost(
            url = "https://api.example.com/deploy",
            body = """{"version": "${getCurrentVersion()}"}""",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer ${env("API_TOKEN")}"
            )
        )
        
        // PUT request
        httpPut(
            url = "https://api.example.com/config",
            body = """{"enabled": true}"""
        )
        
        // DELETE request
        httpDelete("https://api.example.com/resource/123")
    }
}
```

## Docker Operations

### Docker Commands

```kotlin
segment("dockerOps") {
    execute {
        // Build image
        dockerBuild(
            tag = "myapp:${context.commitSha}",
            dockerfile = "Dockerfile",
            context = "."
        )
        
        // Push image
        dockerPush("myapp:${context.commitSha}")
        
        // Run container
        dockerRun(
            image = "myapp:${context.commitSha}",
            name = "myapp-test",
            ports = mapOf("8080" to "8080"),
            env = mapOf("ENV" to "test")
        )
        
        // Stop container
        dockerStop("myapp-test")
        
        // Remove container
        dockerRm("myapp-test")
    }
}
```

## Context Access

### Execution Context

Every segment has access to rich context information:

```kotlin
segment("contextAware") {
    execute {
        // Git information
        println("Branch: ${context.branch}")
        println("Commit: ${context.commitSha}")
        
        // CI information
        println("CI Platform: ${context.ciPlatform}")
        println("Is Local: ${context.isLocal}")
        println("Is Release: ${context.isRelease}")
        
        // MR/PR information
        if (context.mrNumber != null) {
            println("MR Number: ${context.mrNumber}")
        }
        
        // Workspace
        println("Workspace: ${context.workspace}")
        
        // Environment
        context.environment.forEach { (key, value) ->
            println("$key = $value")
        }
    }
}
```

### Conditional Logic Based on Context

```kotlin
segment("conditionalDeploy") {
    execute {
        when {
            context.isLocal -> {
                println("Skipping deployment in local environment")
            }
            context.branch == "main" -> {
                deployToProduction()
            }
            context.branch.startsWith("release/") -> {
                deployToStaging()
            }
            else -> {
                println("Skipping deployment for branch ${context.branch}")
            }
        }
    }
}
```

## Logging

### Structured Logging

```kotlin
segment("logging") {
    execute {
        // Info level
        log.info("Starting build process")
        
        // Debug level
        log.debug("Loading configuration from ${file("config.json")}")
        
        // Warning level
        log.warn("Using default timeout of 30 minutes")
        
        // Error level
        try {
            riskyOperation()
        } catch (e: Exception) {
            log.error("Operation failed", e)
        }
    }
}
```

### Progress Logging

```kotlin
segment("progressLogging") {
    execute {
        val steps = listOf("compile", "test", "package", "deploy")
        
        steps.forEachIndexed { index, step ->
            log.info("Step ${index + 1}/${steps.size}: $step")
            performStep(step)
        }
    }
}
```

## Utility Functions

### Time/Date

```kotlin
segment("timeUtils") {
    execute {
        // Current timestamp
        val timestamp = currentTimestamp()
        println("Timestamp: $timestamp")
        
        // Formatted date
        val date = currentDate(format = "yyyy-MM-dd")
        println("Date: $date")
        
        // Formatted time
        val time = currentTime(format = "HH:mm:ss")
        println("Time: $time")
    }
}
```

### String Utilities

```kotlin
segment("stringUtils") {
    execute {
        // Generate random string
        val randomId = randomString(length = 16)
        
        // Hash string
        val hash = sha256("content to hash")
        
        // Encode/decode base64
        val encoded = base64Encode("Hello, Kite!")
        val decoded = base64Decode(encoded)
    }
}
```

### Retry Logic

```kotlin
segment("retryExample") {
    execute {
        // Retry operation
        retry(
            times = 3,
            delay = 5.seconds,
            on = listOf(IOException::class)
        ) {
            unstableOperation()
        }
    }
}
```

## Summary

Kite provides comprehensive built-in features for:

- **Command execution**: `exec()`, `execOrNull()`, `shell()`
- **File operations**: Copy, move, delete, zip, read, write
- **Environment**: Read and set environment variables
- **Artifacts**: Store and retrieve build outputs
- **Android/Mobile**: Build, test, sign APKs and AABs
- **Version management**: Get, bump, tag versions
- **Notifications**: Slack, email notifications
- **Git operations**: Branch, commit, tag information
- **HTTP/REST**: HTTP requests with various methods
- **Docker**: Build, push, run containers
- **Context access**: Rich execution context
- **Logging**: Structured logging at multiple levels
- **Utilities**: Time, string operations, retry logic

All helpers are:

- **Type-safe**: Full Kotlin type system
- **IDE-friendly**: Autocomplete and documentation
- **Testable**: Can be mocked in unit tests
- **Extensible**: Plugins can add more helpers
