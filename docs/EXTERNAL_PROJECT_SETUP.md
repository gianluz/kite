# Using Kite in External Projects

This guide explains how to use Kite in your own projects with full IDE support, including autocomplete and syntax
highlighting for `.kite.kts` files.

## Table of Contents

1. [Project Setup](#project-setup)
2. [Multiple Segment Files](#multiple-segment-files)
3. [External Dependencies](#external-dependencies)
4. [Helper Functions](#helper-functions)
5. [IntelliJ IDEA Setup](#intellij-idea-setup)
6. [Troubleshooting](#troubleshooting)

---

## Project Setup

There are two ways to use Kite in your projects:

### Option A: Published Dependency (Recommended for Production)

Once Kite is published to Maven Central or a custom repository:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral() // or your custom repository
}

dependencies {
    implementation("io.kite:kite-core:0.1.0")
    implementation("io.kite:kite-dsl:0.1.0")
    implementation("io.kite:kite-runtime:0.1.0")
}
```

**IntelliJ will automatically pick up the script definitions from the JAR!**

### Option B: Composite Build (For Development)

If you're developing Kite or want to use a local version:

```kotlin
// settings.gradle.kts in your external project
includeBuild("/path/to/kite")

// build.gradle.kts
dependencies {
    implementation("io.kite:kite-core")
    implementation("io.kite:kite-dsl")
    implementation("io.kite:kite-runtime")
}
```

This works immediately without publishing!

---

## Multiple Segment Files

✅ **Yes! Kite supports multiple segment files!**

You can organize your segments across multiple files:

```
my-project/
├── .kite/
│   ├── segments/
│   │   ├── build.kite.kts        ← Build segments
│   │   ├── test.kite.kts         ← Test segments  
│   │   ├── deploy.kite.kts       ← Deployment segments
│   │   └── android/
│   │       ├── lint.kite.kts     ← Android linting
│   │       └── assemble.kite.kts ← Android assembly
│   └── rides/
│       ├── mr.kite.kts           ← MR validation ride
│       └── release.kite.kts      ← Release ride
└── build.gradle.kts
```

### Example: Single Segment Per File

**`.kite/segments/build.kite.kts`**:

```kotlin
segments {
    segment("compile") {
        description = "Compile all Kotlin modules"
        execute {
            exec("./gradlew", "clean", "build")
        }
    }
}
```

**`.kite/segments/test.kite.kts`**:

```kotlin
segments {
    segment("unit-tests") {
        description = "Run unit tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", "test")
        }
    }
    
    segment("integration-tests") {
        description = "Run integration tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", "integrationTest")
        }
    }
}
```

### Example: Multiple Segments Per File

**`.kite/segments/android.kite.kts`**:

```kotlin
segments {
    segment("android-lint") {
        description = "Run Android lint checks"
        execute {
            exec("./gradlew", "lintDebug")
        }
    }
    
    segment("android-assemble") {
        description = "Assemble Android APK"
        dependsOn("android-lint")
        execute {
            exec("./gradlew", "assembleDebug")
        }
    }
    
    segment("android-test") {
        description = "Run Android instrumented tests"
        dependsOn("android-assemble")
        execute {
            exec("./gradlew", "connectedAndroidTest")
        }
    }
}
```

Kite's `FileDiscovery` automatically scans all `.kite.kts` files recursively in `.kite/segments/` and `.kite/rides/`.

---

## External Dependencies

### Current Support: Classpath Dependencies

Currently, Kite scripts can use **any dependency already on the classpath** from `kite-core`, `kite-dsl`, and
`kite-runtime`.

Available out of the box:

- All Kite APIs (`Segment`, `Ride`, `ExecutionContext`, etc.)
- Kotlin stdlib and coroutines
- Process execution utilities
- File I/O utilities

**Example**:

```kotlin
// .kite/segments/json-processing.kite.kts
import java.io.File
import kotlinx.coroutines.delay

segments {
    segment("process-json") {
        execute {
            val jsonFile = File("data.json")
            val content = jsonFile.readText()
            
            // Use Kotlin stdlib
            val parsed = content.lines()
                .filter { it.isNotBlank() }
                .map { it.trim() }
            
            println("Processed ${parsed.size} lines")
            
            // Coroutines work!
            delay(100)
        }
    }
}
```

### Future Support: @DependsOn Annotation

In the future, Kite will support dynamic Maven dependencies using `@file:DependsOn`:

```kotlin
// .kite/segments/advanced.kite.kts
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("org.apache.commons:commons-lang3:3.12.0")
@file:Repository("https://my-repo.example.com/maven2/")

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils

segments {
    segment("parse-json") {
        execute {
            val gson = Gson()
            val data = mapOf("name" to "Kite", "version" to "0.1.0")
            val json = gson.toJson(data)
            
            println(StringUtils.capitalize(json))
        }
    }
}
```

**Note**: This requires additional configuration in Kite's scripting host and will be implemented in a future release.

### Workaround: Add Dependencies to Your Project

For now, to use external libraries in Kite scripts, add them to your project's `build.gradle.kts`:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.kite:kite-dsl:0.1.0")
    
    // Add libraries you want to use in Kite scripts
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
}
```

Then they'll be available in your `.kite.kts` files:

```kotlin
// .kite/segments/with-gson.kite.kts
import com.google.gson.Gson

segments {
    segment("use-gson") {
        execute {
            val gson = Gson()
            // Works!
        }
    }
}
```

---

## Helper Functions

✅ **Yes! You can define helper functions in your segments!**

### Example: Gradle Helper Functions

```kotlin
// .kite/segments/gradle-helpers.kite.kts

// Define reusable helper functions
fun ExecutionContext.gradleTask(vararg tasks: String) {
    exec("./gradlew", *tasks)
}

fun ExecutionContext.gradleClean() = gradleTask("clean")

fun ExecutionContext.gradleBuild() = gradleTask("build")

fun ExecutionContext.gradleTest() = gradleTask("test")

// Use them in segments
segments {
    segment("build") {
        description = "Build the project"
        execute {
            gradleClean()
            gradleBuild()
        }
    }
    
    segment("test") {
        description = "Run tests"
        dependsOn("build")
        execute {
            gradleTest()
        }
    }
}
```

### Example: Conditional Logic Helpers

```kotlin
// .kite/segments/conditional-helpers.kite.kts

fun ExecutionContext.isCI(): Boolean = 
    env["CI"] == "true" || env["GITLAB_CI"] == "true"

fun ExecutionContext.isBranch(name: String): Boolean =
    env["BRANCH_NAME"] == name || env["CI_COMMIT_BRANCH"] == name

segments {
    segment("deploy") {
        description = "Deploy to production"
        condition = { it.isCI() && it.isBranch("main") }
        execute {
            shell("./deploy.sh production")
        }
    }
    
    segment("deploy-staging") {
        description = "Deploy to staging"
        condition = { it.isCI() && it.isBranch("develop") }
        execute {
            shell("./deploy.sh staging")
        }
    }
}
```

### Example: Shared Utilities

```kotlin
// .kite/segments/utils.kite.kts
import java.io.File

// File utilities
fun ensureDirectory(path: String): File =
    File(path).apply { mkdirs() }

fun copyFiles(from: String, to: String) {
    File(from).copyRecursively(File(to), overwrite = true)
}

// Use in segments
segments {
    segment("prepare-artifacts") {
        execute {
            val buildDir = ensureDirectory("build/artifacts")
            copyFiles("build/libs", "build/artifacts")
            println("Artifacts prepared in: ${buildDir.absolutePath}")
        }
    }
}
```

---

## IntelliJ IDEA Setup

### Automatic Setup (Recommended)

If you added Kite as a dependency (Option A or B above), IntelliJ will automatically:

1. ✅ Recognize `.kite.kts` files as Kotlin scripts
2. ✅ Provide autocomplete for all Kite APIs
3. ✅ Show syntax highlighting
4. ✅ Display inline errors and warnings
5. ✅ Enable refactoring support

### Manual Configuration (If Needed)

If autocomplete doesn't work automatically:

1. **Reimport Gradle Project**:
    - View → Tool Windows → Gradle
    - Click the "Reload All Gradle Projects" button (circular arrows icon)

2. **Invalidate Caches**:
    - File → Invalidate Caches...
    - Select "Invalidate and Restart"

3. **Check Script Definition**:
    - Open any `.kite.kts` file
    - Right-click → Kotlin → "Configure Kotlin Script"
    - Verify "io.kite.dsl.KiteScript" is listed

4. **Enable Script Support**:
    - Settings → Languages & Frameworks → Kotlin → Kotlin Scripting
    - Ensure "Enable script definitions" is checked

### What Autocomplete Provides

In `.kite.kts` files, you'll get autocomplete for:

**Top-level DSL functions**:

- `segments { ... }`
- `ride { ... }`

**Inside `segments` block**:

- `segment(name: String) { ... }`

**Inside `segment` block**:

- `description = "..."`
- `dependsOn("other-segment")`
- `timeout = 5.minutes`
- `maxRetries = 3`
- `condition = { ... }`
- `execute { ... }`

**Inside `execute` block** (ExecutionContext methods):

- `exec(command, *args)`
- `execOrNull(command, *args)`
- `shell(command)`
- `env: Map<String, String>`
- Platform detection properties

**Kotlin stdlib**:

- All standard library functions
- Coroutines support
- File I/O utilities

---

## Troubleshooting

### Problem: No autocomplete in `.kite.kts` files

**Solutions**:

1. Verify dependencies are added correctly
2. Reload Gradle project
3. Invalidate caches and restart IntelliJ
4. Check that `kite-dsl` dependency is present

### Problem: "Unresolved reference: segments"

**Cause**: Script definition not loaded or wrong file extension.

**Solutions**:

1. Ensure file ends with `.kite.kts` (not just `.kts`)
2. Verify `kite-dsl` is in dependencies
3. Reload Gradle project

### Problem: Helper functions not found in other segments

**Cause**: Each segment file is compiled independently.

**Solution**: Define helper functions in the same file where they're used, or create a shared utilities file and import
it (requires future support for multi-file scripts).

### Problem: External dependency not found

**Cause**: Dependency not on classpath.

**Solution**: Add the dependency to your project's `build.gradle.kts` as described
in [External Dependencies](#external-dependencies).

### Problem: IntelliJ shows "Script definition is not found"

**Solutions**:

1. Ensure `kotlin-scripting-jvm` and related dependencies are present
2. Check that `META-INF/kotlin/script/templates/io.kite.dsl.KiteScript` exists in kite-dsl JAR
3. Clear IntelliJ caches

---

## Complete Example

Here's a complete example project structure:

### Project Structure

```
my-awesome-project/
├── .kite/
│   ├── segments/
│   │   ├── build.kite.kts
│   │   ├── test.kite.kts
│   │   ├── lint.kite.kts
│   │   └── deploy.kite.kts
│   └── rides/
│       ├── mr-validation.kite.kts
│       └── release.kite.kts
├── src/
├── build.gradle.kts
└── settings.gradle.kts
```

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // Kite dependencies
    implementation("io.kite:kite-core:0.1.0")
    implementation("io.kite:kite-dsl:0.1.0")
    implementation("io.kite:kite-runtime:0.1.0")
    
    // Your app dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
```

### .kite/segments/build.kite.kts

```kotlin
segments {
    segment("compile") {
        description = "Compile all Kotlin sources"
        execute {
            exec("./gradlew", "clean", "compileKotlin")
        }
    }
    
    segment("package") {
        description = "Create JAR"
        dependsOn("compile")
        timeout = 5.minutes
        execute {
            exec("./gradlew", "jar")
        }
    }
}
```

### .kite/rides/mr-validation.kite.kts

```kotlin
ride {
    name = "MR Validation"
    maxConcurrency = 3
    
    flow {
        segment("compile")
        
        parallel {
            segment("test")
            segment("lint")
        }
        
        segment("package")
    }
}
```

### Running

```bash
# Execute the MR validation ride
kite ride "MR Validation"
```

---

## Summary

✅ **Multiple segment files**: YES - organize your segments across multiple `.kite.kts` files  
✅ **Helper functions**: YES - define reusable functions in your segment files  
✅ **External dependencies**: Currently via classpath, future support for `@DependsOn`  
✅ **IDE support**: Automatic autocomplete and syntax highlighting with IntelliJ  
✅ **Easy setup**: Add Kite as a dependency, and everything works!

**Kite makes CI/CD configuration type-safe, modular, and enjoyable!** 🪁