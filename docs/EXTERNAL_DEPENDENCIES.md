# Using External Dependencies in Kite Scripts

This guide explains how to use external libraries (like Gson, Apache Commons, etc.) in your `.kite.kts` segment files.

## Current Status: ✅ **WORKS via Classpath**

External dependencies are currently supported through the **classpath mechanism**. Any dependency available on your
project's classpath can be used in Kite scripts!

## Quick Example

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

### Step 3: Run It

```bash
kite ride "My Ride"
```

**Output**:

```
✅ JSON serialization successful!
JSON: {"project":"Kite","version":"0.1.0"}
```

---

## How It Works

Kite's script configuration includes:

```kotlin
jvm {
    dependenciesFromCurrentContext(wholeClasspath = true)
}
```

This makes **all dependencies on the classpath** available to your scripts automatically!

### What's Available?

- ✅ **All Kite APIs** (core, dsl, runtime)
- ✅ **Kotlin stdlib** and coroutines
- ✅ **Any dependency in your project's `build.gradle.kts`**
- ✅ **Transitive dependencies** too!

---

## Complete Working Example

Here's a real example from the Kite repository:

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
// Test using external dependency (Gson) that's available on the classpath

import com.google.gson.Gson

segments {
    segment("test-json") {
        description = "Test external dependency with Gson"
        execute {
            val gson = Gson()

            val data = mapOf(
                "project" to "Kite",
                "version" to "0.1.0",
                "language" to "Kotlin",
                "features" to listOf(
                    "Type-safe DSL",
                    "Parallel execution",
                    "Dependency resolution"
                )
            )

            val json = gson.toJson(data)
            println("✅ JSON serialization successful!")
            println(json)
        }
    }

    segment("test-json-parse") {
        description = "Test parsing JSON with Gson"
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
            println("✅ JSON parsing successful!")
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

### Build Configuration

```kotlin
// build.gradle.kts (or kite-dsl/build.gradle.kts if using composite build)
dependencies {
    implementation("io.kite:kite-dsl:0.1.0")
    implementation("com.google.code.gson:gson:2.10.1") // Available to scripts!
}
```

### Running

```bash
$ ./gradlew :kite-cli:run --args="ride 'Test Dependencies'"

  ██╗  ██╗██╗████████╗███████╗
  ██║ ██╔╝██║╚══██╔══╝██╔════╝
  █████╔╝ ██║   ██║   █████╗  
  ██╔═██╗ ██║   ██║   ██╔══╝  
  ██║  ██╗██║   ██║   ███████╗
  ╚═╝  ╚═╝╚═╝   ╚═���   ╚══════╝

══════════════════════════════════════════════════════════
  🪁 Kite Ride: Test Dependencies
══════════════════════════════════════════════════════════

▶ Execution Plan
ℹ Segments to execute: 2
  ⋯ • test-json
  ⋯ • test-json-parse (depends on: test-json)

▶ Executing Ride
✅ JSON serialization successful!
{"project":"Kite","version":"0.1.0","language":"Kotlin","features":["Type-safe DSL","Parallel execution","Dependency resolution"]}
✅ JSON parsing successful!
Parsed data: {name=Test Segment, status=running, timestamp=1.23456789E9}

▶ Results
  ✓ test-json (10ms)
  ✓ test-json-parse (1ms)

Summary:
  Total: 2 segments
  ✓ Success: 2
  Duration: 1871ms

🎉 All segments completed successfully!
```

---

## Common Use Cases

### 1. JSON Processing (Gson)

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

// segment
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
// build.gradle.kts
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

// segment
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
// build.gradle.kts
dependencies {
    implementation("org.apache.commons:commons-lang3:3.12.0")
}

// segment
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
// build.gradle.kts
dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

// segment
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

Once you add a dependency to your `build.gradle.kts`:

1. **Reload Gradle Project**
    - View → Tool Windows → Gradle
    - Click "Reload All Gradle Projects"

2. **Autocomplete Works!**
    - Open your `.kite.kts` file
    - Type the import and class name
    - Full autocomplete and type checking!

Example:

```kotlin
import com.google.gson.G  // Press Ctrl+Space
// → Shows: Gson, GsonBuilder, etc.

val gson = Gson()  // Full type checking!
```

---

## Future: @DependsOn Annotation

**Status**: 🚧 Planned for future release

In the future, Kite will support dynamic Maven dependencies using `@file:DependsOn`:

```kotlin
// Future syntax (not yet implemented)
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:Repository("https://my-repo.example.com/maven2/")

import com.google.gson.Gson

segments {
    segment("parse-json") {
        execute {
            val gson = Gson()
            // Works without adding to build.gradle.kts!
        }
    }
}
```

### Why Not Now?

The `@DependsOn` annotation requires:

1. Maven dependency resolver integration
2. Dynamic classpath modification at runtime
3. Dependency caching and conflict resolution
4. Proper error handling for missing dependencies

This is planned but requires more work to implement correctly.

### Current Workaround

The current approach (adding to `build.gradle.kts`) is actually **better for most use cases**:

**Advantages**:

- ✅ Faster (no runtime resolution)
- ✅ Safer (dependencies verified at build time)
- ✅ Better IDE support
- ✅ Dependency management in one place
- ✅ Works with existing Gradle features (version catalogs, etc.)

**When @DependsOn would be useful**:

- Experimental scripts
- One-off utilities
- Quick prototyping
- Scripts shared across projects

---

## Troubleshooting

### Problem: "Unresolved reference" for external class

**Cause**: Dependency not on classpath.

**Solution**:

1. Add to `build.gradle.kts`
2. Reload Gradle project
3. Rebuild project

### Problem: "ClassNotFoundException" at runtime

**Cause**: Dependency added to `build.gradle.kts` but not properly included.

**Solution**:

1. Check you used `implementation` (not `compileOnly`)
2. Rebuild: `./gradlew build`
3. Check `./gradlew :kite-dsl:dependencies` to verify

### Problem: IntelliJ doesn't show autocomplete

**Cause**: Gradle cache issue.

**Solution**:

1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.gradle` directory
3. Reimport project

### Problem: Works locally but not in CI

**Cause**: Dependency not in published artifact.

**Solution**:
Ensure dependency is `implementation` (not `testImplementation`) and included in your distribution.

---

## Best Practices

### 1. Centralize Dependencies

```kotlin
// build.gradle.kts
object Versions {
    const val gson = "2.10.1"
    const val okhttp = "4.12.0"
}

dependencies {
    implementation("com.google.code.gson:gson:${Versions.gson}")
    implementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
}
```

### 2. Use Version Catalogs (Gradle 7+)

```toml
# gradle/libs.versions.toml
[versions]
gson = "2.10.1"

[libraries]
gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.gson)
}
```

### 3. Document Dependencies

```kotlin
// .kite/segments/api.kite.kts
// Dependencies: gson, okhttp (see build.gradle.kts)

import com.google.gson.Gson
import okhttp3.OkHttpClient

segments {
    segment("api-call") {
        // ...
    }
}
```

### 4. Test Scripts with Dependencies

```kotlin
// Create a test ride to verify dependencies work
ride {
    name = "Test Dependencies"
    flow {
        segment("test-gson")
        segment("test-okhttp")
    }
}
```

---

## Summary

✅ **External dependencies work via classpath**  
✅ **Add to `build.gradle.kts`, use in scripts**  
✅ **Full IDE support (autocomplete, type checking)**  
✅ **All transitive dependencies available**  
🚧 **@DependsOn annotation coming in future**

**Example**:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

// .kite/segments/json.kite.kts
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