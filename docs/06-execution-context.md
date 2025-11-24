# Execution Context

Complete reference for the `ExecutionContext` API available in segment execution blocks.

---

## Overview

The **ExecutionContext** is your gateway to everything you need in a segment:

- Execute shell commands
- Access environment variables and secrets
- Work with the filesystem and workspace
- Manage artifacts
- Log messages
- Access ride/segment metadata

Every `execute {}` block receives an `ExecutionContext` as its receiver (`this`):

```kotlin
segment("example") {
    execute {
        // 'this' is ExecutionContext
        // All these methods are available directly
        
        exec("./gradlew", "build")
        val apiKey = secret("API_KEY")
        val file = workspace.resolve("output.txt")
        logger.info("Building...")
    }
}
```

---

## Command Execution

### `exec(command: String, vararg args: String): String`

Executes a shell command and returns its output.

```kotlin
val output = exec("./gradlew", "test")
logger.info("Test output: $output")
```

**Parameters:**

- `command` - The command to execute (program name or path)
- `args` - Command arguments (varargs)

**Returns:**

- Standard output of the command as a `String`

**Throws:**

- `CommandExecutionException` if command fails (non-zero exit code)

**Features:**

- Automatically masks secrets in command and output
- Logs command execution
- Captures stdout and stderr
- Validates exit code

**Examples:**

```kotlin
// Simple command
exec("echo", "Hello, World!")

// Gradle build
val buildOutput = exec("./gradlew", "clean", "build")

// Git operations
exec("git", "add", ".")
exec("git", "commit", "-m", "Auto-commit")
exec("git", "push", "origin", "main")

// Package managers
exec("npm", "install")
exec("pip", "install", "-r", "requirements.txt")

// Docker
exec("docker", "build", "-t", "myapp:latest", ".")
exec("docker", "push", "myapp:latest")

// cURL API calls
val response = exec("curl", "-s", "https://api.github.com/repos/owner/repo")
```

---

## Environment Variables

### `env(key: String): String?`

Gets an environment variable.

```kotlin
val branch = env("BRANCH") ?: "main"
val ciMode = env("CI") != null
```

**Parameters:**

- `key` - Environment variable name

**Returns:**

- The value as a `String`, or `null` if not set

**Use for:**

- Non-sensitive configuration
- Build flags
- CI/CD metadata

**Example:**

```kotlin
val environment = env("ENVIRONMENT") ?: "development"
val version = env("VERSION") ?: "1.0.0"
val buildNumber = env("BUILD_NUMBER")?.toIntOrNull() ?: 0

when (environment) {
    "production" -> deployToProd()
    "staging" -> deployToStaging()
    else -> logger.info("Skipping deployment in $environment")
}
```

### `secret(key: String): String?`

Gets an environment variable and registers it as a secret.

```kotlin
val apiKey = secret("API_KEY")
```

**Parameters:**

- `key` - Environment variable name

**Returns:**

- The value as a `String`, or `null` if not set
- **Automatically masks the value** in all logs and outputs

**Use for:**

- API keys
- Passwords
- Tokens
- Certificates
- Any sensitive data

**Example:**

```kotlin
val githubToken = secret("GITHUB_TOKEN")
if (githubToken != null) {
    exec("gh", "auth", "login", "--with-token", githubToken)
    // Logs: gh auth login --with-token [GITHUB_TOKEN:***]
}
```

### `requireSecret(key: String): String`

Gets a required secret, throws if not set.

```kotlin
val deployKey = requireSecret("DEPLOY_KEY")
```

**Parameters:**

- `key` - Environment variable name

**Returns:**

- The non-null value as a `String`
- **Automatically masks the value** in all logs and outputs

**Throws:**

- `IllegalArgumentException` if the variable is not set or is empty

**Use for:**

- Critical secrets that must be present

**Example:**

```kotlin
val awsAccessKey = requireSecret("AWS_ACCESS_KEY_ID")
val awsSecretKey = requireSecret("AWS_SECRET_ACCESS_KEY")

exec("aws", "configure", "set", "aws_access_key_id", awsAccessKey)
exec("aws", "configure", "set", "aws_secret_access_key", awsSecretKey)
```

---

## Filesystem Access

### `workspace: Path`

The workspace directory (project root).

```kotlin
val workspace: Path
```

**Type:** `java.nio.file.Path`

**Usage:**

- Reference to the root directory of your project
- Base path for all relative file operations
- Guaranteed to exist

**Examples:**

```kotlin
// Create a file in workspace
val outputFile = workspace.resolve("output.txt").toFile()
outputFile.writeText("Build results")

// Read a config file
val configFile = workspace.resolve("config.json").toFile()
val config = configFile.readText()

// Create a subdirectory
val buildDir = workspace.resolve("build").toFile()
buildDir.mkdirs()

// List files
workspace.toFile().listFiles()?.forEach { file ->
    logger.info("Found: ${file.name}")
}

// Get absolute path
logger.info("Working in: ${workspace.toAbsolutePath()}")
```

**Path Operations:**

```kotlin
// Resolve relative paths
val file = workspace.resolve("src/main/kotlin/App.kt")
val dir = workspace.resolve("build/outputs")

// Convert to File
val javaFile = workspace.toFile()

// Parent directory
val parent = workspace.parent

// Check existence
if (workspace.resolve("settings.gradle.kts").toFile().exists()) {
    // It's a Gradle project
}
```

---

## Artifact Management

### `artifacts: ArtifactManager`

Access to the artifact manager.

```kotlin
val artifacts: ArtifactManager
```

**Methods:**

#### `get(name: String): Path?`

Retrieves an artifact by name.

```kotlin
val apkPath = artifacts.get("apk")
if (apkPath != null) {
    exec("adb", "install", "$apkPath")
}
```

**Returns:**

- `Path` to the artifact, or `null` if not found

#### `has(name: String): Boolean`

Checks if an artifact exists.

```kotlin
if (artifacts.has("test-results")) {
    val results = artifacts.get("test-results")!!
    processResults(results)
}
```

#### `list(): Set<String>`

Lists all available artifact names.

```kotlin
val allArtifacts = artifacts.list()
logger.info("Available artifacts: ${allArtifacts.joinToString()}")
```

**Example:**

```kotlin
segment("test") {
    dependsOn("build")
    inputs { artifact("apk") }
    
    execute {
        val apk = artifacts.get("apk")
        requireNotNull(apk) { "APK artifact not found" }
        
        logger.info("Testing APK: ${apk.fileName}")
        exec("adb", "install", "-r", "$apk")
        exec("adb", "shell", "am", "instrument", "-w", "com.example.app/androidx.test.runner.AndroidJUnitRunner")
    }
}
```

---

## Logging

### `logger: Logger`

Logger for outputting messages.

```kotlin
val logger: Logger
```

**Methods:**

#### `info(message: String)`

Logs an informational message.

```kotlin
logger.info("Building application...")
logger.info("Build completed in ${duration}ms")
```

#### `warn(message: String)`

Logs a warning message.

```kotlin
logger.warn("Using default configuration")
logger.warn("API rate limit approaching")
```

#### `error(message: String)`

Logs an error message.

```kotlin
logger.error("Build failed: compilation errors")
logger.error("Connection timeout to server")
```

**Automatic Features:**

- Secrets are automatically masked in all log messages
- Timestamps are added automatically
- Colored output in terminals
- Structured logging for CI/CD systems

**Example:**

```kotlin
segment("build") {
    execute {
        logger.info("Starting build...")
        
        try {
            val startTime = System.currentTimeMillis()
            exec("./gradlew", "build")
            val duration = System.currentTimeMillis() - startTime
            logger.info("Build completed in ${duration}ms")
        } catch (e: Exception) {
            logger.error("Build failed: ${e.message}")
            throw e
        }
    }
}
```

---

## Metadata Access

### `rideName: String`

Name of the current ride.

```kotlin
logger.info("Executing ride: $rideName")
```

### `segmentName: String`

Name of the current segment.

```kotlin
logger.info("Running segment: $segmentName")
```

**Example:**

```kotlin
segment("deploy") {
    execute {
        logger.info("[$rideName/$segmentName] Starting deployment")
        
        val timestamp = System.currentTimeMillis()
        exec("./deploy.sh", "--ride", rideName, "--segment", segmentName)
        
        logger.info("[$rideName/$segmentName] Deployment complete")
    }
}
```

---

## Complete Example

Here's a comprehensive example using all ExecutionContext features:

```kotlin
segment("deploy-production") {
    description = "Deploy application to production"
    dependsOn("build", "test")
    
    inputs {
        artifact("app-jar")
        artifact("deployment-config")
    }
    
    outputs {
        artifact("deployment-log", "deployment.log")
    }
    
    execute {
        // 1. Metadata
        logger.info("[$rideName/$segmentName] Starting deployment")
        
        // 2. Environment variables
        val environment = env("ENVIRONMENT") ?: "production"
        val version = env("VERSION") ?: error("VERSION must be set")
        val region = env("AWS_REGION") ?: "us-east-1"
        
        // 3. Secrets
        val awsAccessKey = requireSecret("AWS_ACCESS_KEY_ID")
        val awsSecretKey = requireSecret("AWS_SECRET_ACCESS_KEY")
        val apiKey = requireSecret("API_KEY")
        
        logger.info("Deploying version $version to $environment in $region")
        
        // 4. Artifacts
        val appJar = artifacts.get("app-jar")
        requireNotNull(appJar) { "Application JAR not found" }
        
        val configPath = artifacts.get("deployment-config")
        requireNotNull(configPath) { "Deployment config not found" }
        
        logger.info("Using JAR: ${appJar.fileName}")
        logger.info("Using config: ${configPath.fileName}")
        
        // 5. Workspace
        val deploymentLog = workspace.resolve("deployment.log").toFile()
        deploymentLog.writeText("Deployment started at ${System.currentTimeMillis()}\n")
        
        // 6. AWS Configuration
        exec("aws", "configure", "set", "aws_access_key_id", awsAccessKey)
        exec("aws", "configure", "set", "aws_secret_access_key", awsSecretKey)
        exec("aws", "configure", "set", "region", region)
        
        // 7. Upload to S3
        val s3Bucket = "deployments-$environment"
        val s3Key = "releases/$version/app.jar"
        
        logger.info("Uploading to s3://$s3Bucket/$s3Key")
        exec("aws", "s3", "cp", "$appJar", "s3://$s3Bucket/$s3Key")
        
        // 8. Deploy via API
        logger.info("Triggering deployment via API")
        val response = exec("curl", "-X", "POST",
            "-H", "Authorization: Bearer $apiKey",
            "-H", "Content-Type: application/json",
            "-d", """{"version": "$version", "s3_path": "s3://$s3Bucket/$s3Key"}""",
            "https://deploy-api.example.com/deploy"
        )
        
        logger.info("API Response: $response")
        
        // 9. Verify deployment
        logger.info("Verifying deployment...")
        Thread.sleep(5000)  // Wait for deployment
        
        val status = exec("curl", "-s", "https://app.example.com/health")
        if (status.contains("\"status\":\"healthy\"")) {
            logger.info("✅ Deployment successful!")
            deploymentLog.appendText("Deployment successful at ${System.currentTimeMillis()}\n")
        } else {
            logger.error("❌ Health check failed!")
            deploymentLog.appendText("Deployment failed at ${System.currentTimeMillis()}\n")
            error("Deployment verification failed")
        }
        
        logger.info("[$rideName/$segmentName] Deployment complete")
    }
}
```

**Console output (with secrets masked):**

```
[Release/deploy-production] Starting deployment
Deploying version 1.0.0 to production in us-east-1
Using JAR: app.jar
Using config: deployment.yml
aws configure set aws_access_key_id [AWS_ACCESS_KEY_ID:***]
aws configure set aws_secret_access_key [AWS_SECRET_ACCESS_KEY:***]
aws configure set region us-east-1
Uploading to s3://deployments-production/releases/1.0.0/app.jar
aws s3 cp /path/to/app.jar s3://deployments-production/releases/1.0.0/app.jar
Triggering deployment via API
curl -X POST -H Authorization: Bearer [API_KEY:***] ...
API Response: {"status":"accepted","deployment_id":"12345"}
Verifying deployment...
curl -s https://app.example.com/health
✅ Deployment successful!
[Release/deploy-production] Deployment complete
```

---

## Best Practices

### 1. Use Secrets for Sensitive Data

```kotlin
// ❌ Bad
val apiKey = env("API_KEY")

// ✅ Good
val apiKey = secret("API_KEY")
```

### 2. Validate Environment Variables

```kotlin
// ❌ Bad
val version = env("VERSION")!!  // Can crash

// ✅ Good
val version = env("VERSION") ?: error("VERSION is required")
```

### 3. Check Artifacts Before Use

```kotlin
// ❌ Bad
val jar = artifacts.get("jar")!!
exec("java", "-jar", "$jar")

// ✅ Good
val jar = artifacts.get("jar")
    ?: error("JAR artifact not found. Did 'build' segment succeed?")
exec("java", "-jar", "$jar")
```

### 4. Use Workspace for File Operations

```kotlin
// ❌ Bad
val file = File("/tmp/output.txt")  // May not persist

// ✅ Good
val file = workspace.resolve("output.txt").toFile()
```

### 5. Log Meaningful Messages

```kotlin
// ❌ Bad
logger.info("Done")

// ✅ Good
logger.info("Build completed successfully in ${duration}ms")
logger.info("Deployed version $version to $environment")
```

---

## Common Patterns

### Pattern 1: Conditional Execution

```kotlin
execute {
    val environment = env("ENVIRONMENT") ?: "dev"
    
    if (environment == "production") {
        logger.info("Running production deployment")
        val prodKey = requireSecret("PROD_API_KEY")
        exec("deploy-prod", "--key", prodKey)
    } else {
        logger.info("Running development deployment")
        exec("deploy-dev")
    }
}
```

### Pattern 2: Retry Logic

```kotlin
execute {
    var attempts = 0
    var success = false
    
    while (attempts < 3 && !success) {
        try {
            attempts++
            logger.info("Attempt $attempts of 3")
            exec("flaky-command")
            success = true
            logger.info("Command succeeded")
        } catch (e: Exception) {
            logger.warn("Attempt $attempts failed: ${e.message}")
            if (attempts < 3) {
                Thread.sleep(1000 * attempts)  // Exponential backoff
            } else {
                throw e
            }
        }
    }
}
```

### Pattern 3: Multi-Step Validation

```kotlin
execute {
    logger.info("Running validation checks...")
    
    // Validate build artifact
    val jar = artifacts.get("jar")
    requireNotNull(jar) { "Missing JAR artifact" }
    require(jar.toFile().exists()) { "JAR file doesn't exist" }
    require(jar.toFile().length() > 0) { "JAR file is empty" }
    
    // Validate environment
    val requiredVars = listOf("VERSION", "ENVIRONMENT", "REGION")
    requiredVars.forEach { varName ->
        requireNotNull(env(varName)) { "$varName must be set" }
    }
    
    // Validate secrets
    val apiKey = requireSecret("API_KEY")
    require(apiKey.length >= 32) { "API_KEY is too short" }
    
    logger.info("All validations passed")
}
```

---

## Troubleshooting

### Problem: Command not found

**Symptom:**

```
CommandExecutionException: Command failed: ./gradlew: No such file or directory
```

**Solution:**
Ensure the command exists and is executable:

```kotlin
val gradlew = workspace.resolve("gradlew").toFile()
if (!gradlew.exists()) {
    error("gradlew not found. Are you in the project root?")
}
if (!gradlew.canExecute()) {
    exec("chmod", "+x", "gradlew")
}
exec("./gradlew", "build")
```

### Problem: Environment variable not set

**Symptom:**

```
NullPointerException or empty value
```

**Solution:**
Always check for null:

```kotlin
val version = env("VERSION") ?: run {
    logger.error("VERSION environment variable not set")
    error("VERSION is required")
}
```

### Problem: Artifact not found

**Symptom:**

```
artifacts.get("apk") returns null
```

**Solutions:**

1. Ensure dependency exists:

```kotlin
segment("test") {
    dependsOn("build")  // ✅ Add this
    inputs { artifact("apk") }
}
```

2. Check producer segment succeeded:

```kotlin
val apk = artifacts.get("apk")
if (apk == null) {
    logger.error("APK artifact not found")
    logger.error("Available artifacts: ${artifacts.list()}")
    error("Build segment may have failed")
}
```

---

## Summary

The `ExecutionContext` provides everything you need:

| Feature | Method | Use For |
|---------|--------|---------|
| **Commands** | `exec()` | Running shell commands |
| **Environment** | `env()` | Non-sensitive config |
| **Secrets** | `secret()`, `requireSecret()` | Sensitive data |
| **Files** | `workspace` | File operations |
| **Artifacts** | `artifacts` | Sharing build outputs |
| **Logging** | `logger` | Status messages |
| **Metadata** | `rideName`, `segmentName` | Context info |

All methods are available directly in `execute {}` blocks - no need for `this.` prefix!

---

## Related Topics

- [Writing Segments](04-writing-segments.md) - Learn segment configuration
- [Artifacts](08-artifacts.md) - Deep dive into artifact management
- [Secrets](09-secrets.md) - Comprehensive secret handling guide
- [Core Concepts](03-core-concepts.md) - Understanding rides and segments

---

## Next Steps

- [Learn about parallel execution →](07-parallel-execution.md)
- [Explore external dependencies →](10-external-dependencies.md)
- [Integrate with CI/CD →](11-ci-integration.md)
