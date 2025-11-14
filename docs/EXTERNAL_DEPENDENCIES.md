# Using External Dependencies in Kite Scripts

This guide explains how to use external libraries (like Gson, Apache Commons, etc.) in your `.kite.kts` segment files.

## Current Status: ✅ **TWO WAYS TO USE DEPENDENCIES**

Kite now supports external dependencies in TWO ways:

1. **@DependsOn Annotation** (Recommended for standalone scripts) - ✅ **NEW!**
2. **Classpath Dependencies** (Good for project-integrated builds)

Both approaches work perfectly! Choose based on your use case.

---

## Method 1: @DependsOn Annotation ✅ **RECOMMENDED**

**Best for**: Standalone `.kite.kts` scripts, no project integration needed!

### Quick Example

```kotlin
// .kite/segments/json-processing.kite.kts
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:Repository("https://repo.maven.apache.org/maven2/")

import com.google.gson.Gson

segments {
    segment("process-json") {
        description = "Process JSON data"
        execute {
            val gson = Gson()
            
            val data = mapOf(
                "project" to "Kite",
                "version" to "0.1.0"
            )
            
            val json = gson.toJson(data)
            println("JSON: $json")
        }
    }
}
```

### Just run it!

```bash
kite ride "My Ride"
```

**Output**:
```
✅ JSON serialization with @DependsOn successful!
JSON: {"project":"Kite","version":"0.1.0"}
```

**That's it!** Dependencies are downloaded automatically on first run and cached for subsequent runs!

### How It Works

- `@file:DependsOn("group:artifact:version")` - Declares a Maven dependency
- `@file:Repository("url")` - Adds a custom Maven repository (optional, defaults to Maven Central)
- Dependencies are resolved at runtime using Maven
- Downloaded JARs are cached in your local Maven repository (`~/.m2/repository`)
- Subsequent runs use cached dependencies (much faster!)

### Multiple Dependencies

```kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("org.apache.commons:commons-lang3:3.12.0")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:Repository("https://repo.maven.apache.org/maven2/")

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import okhttp3.OkHttpClient

segments {
    segment("multi-lib") {
        execute {
            val gson = Gson()
            val capitalized = StringUtils.capitalize("hello")
            val client = OkHttpClient()
            // All libraries available!
        }
    }
}
```

---

## Method 2: Classpath Dependencies

**Best for**: When Kite is integrated into your Gradle/Maven project.

### Step 1: Add Dependency to Your Project

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.kite:kite-dsl:0.1.0")
    
    // Add any library you want to use in scripts
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### Step 2: Use in Your Segment

```kotlin
// .kite/segments/json-processing.kite.kts
import com.google.gson.Gson

segments {
    segment("process-json") {
        description = "Process JSON data"
        execute {
            val gson = Gson()
            
            val data = mapOf(
                "project" to "Kite",
                "version" to "0.1.0"
            )
            
            val json = gson.toJson(data)
            println("JSON: $json")
        }
    }
}
```

### When to Use This Approach

- ✅ When Kite is part of your project build
- ✅ When you want centralized dependency management
- ✅ When you need version catalogs or dependency locking
- ✅ Faster build times (no runtime resolution)
- ✅ Better for CI/CD pipelines

---

## Comparison

| Feature              | @DependsOn                            | Classpath                 |
|----------------------|---------------------------------------|---------------------------|
| **Setup**            | None needed                           | Add to build.gradle.kts   |
| **IDE Autocomplete** | After first run                       | Immediate                 |
| **Build Speed**      | Slower first run                      | Always fast               |
| **Portability**      | High - scripts are self-contained     | Low - needs project setup |
| **Best For**         | Standalone scripts, quick prototyping | Project-integrated builds |

---

## Complete Working Example

Here's a real example from the Kite repository demonstrating `@DependsOn`:

### File Structure
```
.kite/
├── segments/
│   └── test-dependencies.kite.kts
└── rides/
    └── test-dependencies.kite.kts
```

### Segment File

```kotlin
// .kite/segments/test-dependencies.kite.kts
// Dependencies resolved automatically!
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson

segments {
    segment("test-json") {
        description = "Test external dependency with Gson via @DependsOn"
        execute {
            val gson = Gson()

            val data = mapOf(
                "project" to "Kite",
                "version" to "0.1.0",
                "language" to "Kotlin",
                "features" to listOf(
                    "Type-safe DSL",
                    "Parallel execution",
                    "Dependency resolution",
                    "@DependsOn annotation support!"
                )
            )

            val json = gson.toJson(data)
            println("✅ JSON serialization with @DependsOn successful!")
            println(json)
        }
    }

    segment("test-json-parse") {
        description = "Test parsing JSON with Gson via @DependsOn"
        dependsOn("test-json")
        execute {
            val gson = Gson()

            val jsonString = """
                {
                    "name": "Test Segment",
                    "status": "running",
                    "timestamp": 1234567890
                }
            """.trimIndent()

            @Suppress("UNCHECKED_CAST")
            val parsed = gson.fromJson(jsonString, Map::class.java) as Map<String, Any>
            println("✅ JSON parsing with @DependsOn successful!")
            println("Parsed data: $parsed")
        }
    }
}
```

### Ride File

```kotlin
// .kite/rides/test-dependencies.kite.kts
ride {
    name = "Test Dependencies"
    
    flow {
        segment("test-json")
        segment("test-json-parse")
    }
}
```

### Running

```bash
$ kite ride "Test Dependencies"

  ██╗  ██╗██╗████████╗███████╗
  ██║ ██╔╝██║╚══██╔══╝██╔════╝
  █████╔╝ ██║   ██║   █████╗  
  ██╔═██╗ ██║   ██║   ██╔══╝  
  ██║  ██╗██║   ██║   ███████╗
  ╚═╝  ╚═╝╚═╝   ╚═╝   ╚══════╝

══════════════════════════════════════════════════════════
  🪁 Kite Ride: Test Dependencies
══════════════════════════════════════════════════════════

▶ Execution Plan
ℹ Segments to execute: 2
  ⋯ • test-json
  ⋯ • test-json-parse (depends on: test-json)

▶ Executing Ride
✅ JSON serialization with @DependsOn successful!
{"project":"Kite","version":"0.1.0","language":"Kotlin","features":["Type-safe DSL","Parallel execution","Dependency resolution","@DependsOn annotation support!"]}
✅ JSON parsing with @DependsOn successful!
Parsed data: {name=Test Segment, status=running, timestamp=1.23456789E9}

▶ Results
  ✓ test-json (10ms)
  ✓ test-json-parse (1ms)

Summary:
  Total: 2 segments
  ✓ Success: 2
  Duration: 2063ms

🎉 All segments completed successfully!
```

---

## Common Use Cases

### 1. JSON Processing (Gson)

```kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson

segment("api-call") {
    execute {
        val gson = Gson()
        val response = gson.fromJson(apiResponse, ResponseData::class.java)
    }
}
```

### 2. HTTP Requests (OkHttp)

```kotlin
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")

import okhttp3.OkHttpClient
import okhttp3.Request

segment("fetch-data") {
    execute {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.example.com/data")
            .build()
        
        val response = client.newCall(request).execute()
        println("Status: ${response.code}")
    }
}
```

### 3. String Utilities (Apache Commons)

```kotlin
@file:DependsOn("org.apache.commons:commons-lang3:3.12.0")

import org.apache.commons.lang3.StringUtils

segment("format-output") {
    execute {
        val text = "hello world"
        val capitalized = StringUtils.capitalize(text)
        println(capitalized) // "Hello world"
    }
}
```

### 4. Logging (SLF4J)

```kotlin
@file:DependsOn("org.slf4j:slf4j-api:2.0.9")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.9")

import org.slf4j.LoggerFactory

segment("with-logging") {
    execute {
        val logger = LoggerFactory.getLogger("MySegment")
        logger.info("Starting process...")
        logger.debug("Debug info: ${env["CI"]}")
    }
}
```

---

## IntelliJ IDEA Support

### With @DependsOn

IntelliJ will show unresolved references initially, but after the first run:

1. Dependencies are downloaded to `~/.m2/repository`
2. **Reload Gradle Project** in IntelliJ
3. Autocomplete and type checking work!

Alternatively, temporarily add the dependency to your `build.gradle.kts` for IDE support during development, then remove
it before committing.

### With Classpath Dependencies

Autocomplete works immediately after adding to `build.gradle.kts` and reloading Gradle.

---

## Advantages of @DependsOn

### Why This Is a Game Changer

**Before @DependsOn**:

```
1. Add dependency to build.gradle.kts
2. Reload Gradle
3. Write .kite.kts script
4. Run
```

**With @DependsOn**:
```
1. Write .kite.kts script with @file:DependsOn
2. Run
```

### Benefits

1. **No Project Setup** - Scripts are truly standalone
2. **Portable** - Share scripts without worrying about project configuration
3. **Self-Documenting** - Dependencies are declared in the script itself
4. **Quick Prototyping** - Try libraries without modifying project files
5. **Distribution-Ready** - Scripts work anywhere Kite is installed

---

## Troubleshooting

### Problem: "Unresolved reference" in IDE for @DependsOn dependencies

**Cause**: IDE hasn't resolved dependencies yet.

**Solution**:

1. Run the script once: `kite ride "My Ride"`
2. Dependencies are downloaded
3. Reload Gradle project in IntelliJ
4. Autocomplete will work

**OR** temporarily add to `build.gradle.kts` for IDE support.

### Problem: Dependency resolution is slow

**Cause**: First run downloads dependencies.

**Solution**:

- First run: Slow (downloading)
- Subsequent runs: Fast (cached in `~/.m2/repository`)
- This is normal and expected!

### Problem: "Failed to resolve" error

**Cause**: Dependency doesn't exist or wrong coordinates.

**Solution**:

1. Check coordinates on [Maven Central](https://search.maven.org/)
2. Verify spelling and version number
3. Add `@file:Repository` if using custom repository

### Problem: Works locally but not in CI

**Cause**: CI may not have Maven repository access.

**Solution**:

- Ensure CI can access Maven Central
- Or use classpath approach for CI builds
- Or cache `~/.m2/repository` in CI

---

## Best Practices

### 1. Use @DependsOn for Standalone Scripts

```kotlin
// Perfect for scripts you want to share
@file:DependsOn("com.google.code.gson:gson:2.10.1")

// Script is self-contained!
```

### 2. Use Classpath for Project Builds

```kotlin
// build.gradle.kts - centralized dependency management
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

// .kite/segments/script.kite.kts - no @DependsOn needed
import com.google.gson.Gson
```

### 3. Document Your Dependencies

```kotlin
// .kite/segments/api.kite.kts
// Uses Gson for JSON and OkHttp for HTTP calls
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")

import com.google.gson.Gson
import okhttp3.OkHttpClient
```

### 4. Version Pinning

```kotlin
// Always specify exact versions for reproducibility
@file:DependsOn("com.google.code.gson:gson:2.10.1") // ✅ Good
@file:DependsOn("com.google.code.gson:gson:+")     // ❌ Bad
```

---

## Summary

✅ **@DependsOn annotation works!** (NEW!)  
✅ **Classpath dependencies work!**  
✅ **Full IDE support** (after first run or via classpath)  
✅ **All transitive dependencies resolved**  
✅ **Maven Central + custom repositories supported**  
✅ **Cached for fast subsequent runs**

### Choose Your Approach

**Standalone Scripts** → Use `@DependsOn`  
**Project Builds** → Use classpath dependencies  
**Both** → Works perfectly together!

**Example**:
```kotlin
// Standalone script - works anywhere!
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson

segments {
    segment("json") {
        execute {
            val gson = Gson()
            println(gson.toJson(mapOf("status" to "works!")))
        }
    }
}
```

**Just works!** 🎉

---

**Kite is now a TRUE scripting tool - write `.kite.kts` files anywhere and run them with automatic dependency
resolution!** 🪁