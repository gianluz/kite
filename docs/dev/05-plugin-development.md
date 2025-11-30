# Plugin Development Guide

This guide explains how to create Kite plugins.

## 📋 What You'll Learn

- Plugin structure and architecture
- Type-safe DSL patterns
- Testing plugins locally
- Publishing plugins

## 🏗️ Plugin Architecture

### Basic Structure

```
my-plugin/
├── build.gradle.kts           # Plugin build configuration
├── src/
│   ├── main/kotlin/
│   │   └── com/company/
│   │       ├── MyPlugin.kt    # Main plugin class
│   │       └── MyExtensions.kt # Extension functions
│   └── test/kotlin/
│       └── com/company/
│           └── MyPluginTest.kt
└── README.md                   # Plugin documentation
```

### Minimal Plugin

```kotlin
// MyPlugin.kt
package com.company

import io.kite.core.ExecutionContext

/**
 * Example Kite plugin
 */
class MyPlugin(private val ctx: ExecutionContext) {
    
    fun greet(name: String) {
        ctx.logger.info("Hello, $name!")
    }
    
    fun runCommand(command: String, vararg args: String) {
        ctx.exec(command, *args)
    }
}

/**
 * Extension function makes plugin available in execute blocks
 */
fun ExecutionContext.myPlugin(configure: MyPlugin.() -> Unit) {
    MyPlugin(this).configure()
}
```

**Usage:**

```kotlin
@file:DependsOnJar("./my-plugin.jar")

import com.company.*

execute {
    myPlugin {
        greet("World")
        runCommand("echo", "Plugin works!")
    }
}
```

## 🎨 DSL Patterns

### Pattern 1: Simple Operations

```kotlin
class GitPlugin(private val ctx: ExecutionContext) {
    
    fun tag(name: String) {
        ctx.exec("git", "tag", name)
    }
    
    fun push(remote: String = "origin", tags: Boolean = false) {
        val args = mutableListOf("git", "push", remote)
        if (tags) args.add("--tags")
        ctx.exec(*args.toTypedArray())
    }
}

fun ExecutionContext.git(configure: GitPlugin.() -> Unit) {
    GitPlugin(this).configure()
}
```

### Pattern 2: Configuration Builder

```kotlin
class DockerPlugin(private val ctx: ExecutionContext) {
    
    fun build(configure: DockerBuildConfig.() -> Unit) {
        val config = DockerBuildConfig().apply(configure)
        
        val args = buildList {
            add("docker")
            add("build")
            add("-t")
            add(config.tag)
            if (config.noCache) add("--no-cache")
            add(config.context)
        }
        
        ctx.exec(*args.toTypedArray())
    }
}

data class DockerBuildConfig(
    var tag: String = "latest",
    var context: String = ".",
    var noCache: Boolean = false
)

fun ExecutionContext.docker(configure: DockerPlugin.() -> Unit) {
    DockerPlugin(this).configure()
}
```

**Usage:**

```kotlin
execute {
    docker {
        build {
            tag = "myapp:1.0.0"
            noCache = true
        }
    }
}
```

### Pattern 3: Nested DSL

```kotlin
class GradlePlugin(private val ctx: ExecutionContext) {
    
    fun build(configure: GradleBuild.() -> Unit = {}) {
        val builder = GradleBuild().apply(configure)
        builder.execute(ctx)
    }
    
    fun test(configure: GradleTest.() -> Unit = {}) {
        val tester = GradleTest().apply(configure)
        tester.execute(ctx)
    }
}

class GradleBuild {
    var tasks: List<String> = listOf("build")
    var parallel: Boolean = false
    var daemon: Boolean = true
    
    fun execute(ctx: ExecutionContext) {
        val args = buildList {
            add("./gradlew")
            addAll(tasks)
            if (parallel) add("--parallel")
            if (!daemon) add("--no-daemon")
        }
        ctx.exec(*args.toTypedArray())
    }
}

class GradleTest {
    var testClasses: List<String> = emptyList()
    var verbose: Boolean = false
    
    fun execute(ctx: ExecutionContext) {
        // Implementation
    }
}

fun ExecutionContext.gradle(configure: GradlePlugin.() -> Unit) {
    GradlePlugin(this).configure()
}
```

**Usage:**

```kotlin
execute {
    gradle {
        build {
            tasks = listOf("clean", "build")
            parallel = true
        }
        
        test {
            testClasses = listOf("*IntegrationTest")
            verbose = true
        }
    }
}
```

## 🧪 Testing Plugins

### Unit Tests

```kotlin
// MyPluginTest.kt
package com.company

import io.kite.core.ExecutionContext
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class MyPluginTest {
    
    @Test
    fun `greet logs message`() {
        val ctx = mockk<ExecutionContext>(relaxed = true)
        val plugin = MyPlugin(ctx)
        
        plugin.greet("World")
        
        verify { ctx.logger.info("Hello, World!") }
    }
    
    @Test
    fun `runCommand executes with arguments`() {
        val ctx = mockk<ExecutionContext>(relaxed = true)
        val plugin = MyPlugin(ctx)
        
        plugin.runCommand("echo", "hello")
        
        verify { ctx.exec("echo", "hello") }
    }
}
```

### Integration Tests

Create a test Kite script:

```kotlin
// test-plugin.kite.kts
@file:DependsOnJar("./my-plugin/build/libs/my-plugin-1.0.0.jar")

import com.company.*

segments {
    segment("test-plugin") {
        execute {
            myPlugin {
                greet("Integration Test")
                runCommand("echo", "Works!")
            }
        }
    }
}
```

Run:

```bash
./gradlew :my-plugin:build
kite-cli run test-plugin
```

## 📦 Build Configuration

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

group = "com.company"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Plugin depends on Kite core (compile-only)
    compileOnly("io.kite:kite-core:0.1.0-alpha")
    
    // Plugin-specific dependencies
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            
            pom {
                name.set("My Kite Plugin")
                description.set("Example plugin for Kite")
                url.set("https://github.com/company/kite-plugin-myplugin")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}
```

## 🚀 Publishing Workflow

### 1. Local Testing (JAR)

```bash
# Build plugin
./gradlew :my-plugin:build

# Use in Kite scripts
@file:DependsOnJar("./my-plugin/build/libs/my-plugin-1.0.0.jar")
```

### 2. Maven Local Testing

```bash
# Publish to Maven Local
./gradlew :my-plugin:publishToMavenLocal

# Use in Kite scripts
@file:DependsOnMavenLocal("com.company:my-plugin:1.0.0-SNAPSHOT")
```

### 3. Publish to Maven Central

```bash
# Configure credentials in ~/.gradle/gradle.properties
./gradlew :my-plugin:publish
```

## 💡 Best Practices

### 1. Keep ExecutionContext Private

```kotlin
// ✅ Good: ctx is private
class MyPlugin(private val ctx: ExecutionContext) {
    fun doWork() { ctx.exec("...") }
}

// ❌ Bad: ctx exposed
class MyPlugin(val ctx: ExecutionContext) {
    // Users can access ctx directly
}
```

### 2. Use Data Classes for Configuration

```kotlin
// ✅ Good: Type-safe configuration
data class BuildConfig(
    var parallel: Boolean = false,
    var daemon: Boolean = true
)

fun build(configure: BuildConfig.() -> Unit) {
    val config = BuildConfig().apply(configure)
    // Use config...
}
```

### 3. Provide Helpful Error Messages

```kotlin
fun tag(name: String) {
    require(name.isNotBlank()) {
        "Tag name cannot be blank. Example: git { tag(\"v1.0.0\") }"
    }
    
    require(name.matches(Regex("v?\\d+\\.\\d+\\.\\d+"))) {
        "Invalid tag format: $name. Expected format: v1.0.0"
    }
    
    ctx.exec("git", "tag", name)
}
```

### 4. Log Important Operations

```kotlin
fun deploy(environment: String) {
    ctx.logger.info("🚀 Deploying to $environment...")
    
    ctx.exec("./deploy.sh", environment)
    
    ctx.logger.info("✅ Deployment complete!")
}
```

### 5. Use Artifacts for Data Sharing

```kotlin
fun analyze(): AnalysisResult {
    // Perform analysis
    val result = performAnalysis()
    
    // Store for other segments
    ctx.artifacts.store("analysis-result", result.toJson())
    
    return result
}
```

## 📚 Examples

### Complete Git Plugin Example

See the [Git Plugin source code](../../kite-plugins/git/) for a complete example.

### Plugin Templates

Use the plugin template to get started quickly:

```bash
cp -r kite-plugins/plugin-template my-plugin
cd my-plugin
# Edit build.gradle.kts and source files
```

## 🔗 Related Documentation

- [Plugin Index](../plugins/00-index.md)
- [Execution Context API](../06-execution-context.md)
- [External Dependencies](../10-external-dependencies.md)
