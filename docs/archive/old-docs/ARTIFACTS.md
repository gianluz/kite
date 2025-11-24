# Artifact Management in Kite

## What Are Artifacts?

**Artifacts are files or directories that one segment produces and another segment consumes.**

Think of artifacts as the "outputs" of a build step that need to be passed to subsequent steps. For example:

- A `build` segment creates an APK file
- A `test` segment needs that APK to run tests
- A `deploy` segment needs that same APK to upload

Kite **automatically manages** these files by:

1. Copying them to `.kite/artifacts/` when a segment succeeds
2. Making them available to dependent segments via `artifacts.get("name")`

---

## How It Works

### The Flow

```
Producer Segment          Kite System                Consumer Segment
────────────────          ───────────                ────────────────

1. Creates file      →    2. Copies to         →    3. Accesses via
   "app.apk"               .kite/artifacts/             artifacts.get()
                           
4. Segment             5. Artifact stored      →    6. Gets File object
   succeeds                as "apk"                    pointing to artifact
```

### Key Points

- **Artifacts are FILE REFERENCES**, not arbitrary data
- Artifacts can be:
    - **Files**: `app.apk`, `report.html`, `config.json`
    - **Directories**: `test-results/`, `screenshots/`
- Artifacts are **copied** to `.kite/artifacts/` after segment success
- Artifacts are **shared** between segments via the artifact manager
- If a producer fails, artifacts are NOT stored

---

## Basic Example

### Producer/Consumer Pattern

```kotlin
segments {
    // PRODUCER: Creates a file and declares it as an artifact
    segment("build") {
        outputs {
            artifact("apk", "app/build/outputs/apk/release/app.apk")
            //       ↑ name       ↑ path relative to workspace
        }
        
        execute {
            // Build your app (creates app.apk)
            exec("./gradlew", "assembleRelease")
            
            // Kite will automatically copy the APK to .kite/artifacts/apk after this succeeds
        }
    }
    
    // CONSUMER: Uses the artifact
    segment("test") {
        dependsOn("build")  // Ensures build runs first
        
        inputs {
            artifact("apk")  // Declares we need the "apk" artifact
        }
        
        execute {
            // Get the artifact - returns a File object
            val apkFile = artifacts.get("apk")
            
            if (apkFile != null && apkFile.exists()) {
                // Use the APK
                println("Testing APK: ${apkFile.absolutePath}")
                exec("adb", "install", apkFile.absolutePath)
                exec("adb", "shell", "am", "instrument", "...")
            } else {
                error("APK artifact not found!")
            }
        }
    }
}
```

**What happens:**

1. `build` segment creates `app/build/outputs/apk/release/app.apk`
2. Kite copies it to `.kite/artifacts/apk/app.apk`
3. `test` segment calls `artifacts.get("apk")`
4. Returns `File("/path/to/project/.kite/artifacts/apk/app.apk")`
5. `test` segment installs and tests the APK

---

## Real-World Example: APK Build Pipeline

```kotlin
segments {
    // 1. Build the APK
    segment("build") {
        outputs {
            artifact("unsigned-apk", "app-release-unsigned.apk")
        }
        
        execute {
            // Gradle builds the APK
            exec("./gradlew", "assembleRelease")
            
            // Copy to workspace root for artifact collection
            val builtApk = File("app/build/outputs/apk/release/app-release-unsigned.apk")
            builtApk.copyTo(workspace.resolve("app-release-unsigned.apk").toFile())
        }
    }
    
    // 2. Sign the APK
    segment("sign") {
        dependsOn("build")
        
        inputs {
            artifact("unsigned-apk")
        }
        
        outputs {
            artifact("signed-apk", "app-release-signed.apk")
        }
        
        execute {
            val unsignedApk = artifacts.get("unsigned-apk")!!
            val signedApk = workspace.resolve("app-release-signed.apk").toFile()
            
            // Sign the APK
            exec("jarsigner",
                "-verbose",
                "-sigalg", "SHA256withRSA",
                "-digestalg", "SHA-256",
                "-keystore", "release.keystore",
                unsignedApk.absolutePath
            )
            
            // Zipalign
            exec("zipalign",
                "-v", "4",
                unsignedApk.absolutePath,
                signedApk.absolutePath
            )
        }
    }
    
    // 3. Test the signed APK
    segment("test") {
        dependsOn("sign")
        
        inputs {
            artifact("signed-apk")
        }
        
        execute {
            val apk = artifacts.get("signed-apk")!!
            
            exec("adb", "install", "-r", apk.absolutePath)
            exec("./run-tests.sh")
        }
    }
    
    // 4. Upload to Play Store
    segment("deploy") {
        dependsOn("sign")  // Can run in parallel with test!
        
        inputs {
            artifact("signed-apk")
        }
        
        execute {
            val apk = artifacts.get("signed-apk")!!
            
            println("Uploading ${apk.name} (${apk.length()} bytes)")
            exec("bundle", "exec", "fastlane", "upload_to_play_store",
                "apk:${apk.absolutePath}"
            )
        }
    }
}

ride {
    name = "Release"
    maxConcurrency = 2
    
    flow {
        segment("build")
        segment("sign")
        
        // Test and deploy can run in parallel!
        parallel {
            segment("test")
            segment("deploy")
        }
    }
}
```

---

## Multiple Artifacts

A segment can produce multiple artifacts:

```kotlin
segment("build") {
    outputs {
        artifact("debug-apk", "app-debug.apk")
        artifact("release-apk", "app-release.apk")
        artifact("mapping", "mapping.txt")
        artifact("symbols", "symbols.zip")
    }
    
    execute {
        // Build everything
        exec("./gradlew", "assemble")
        
        // Kite will copy all 4 files to .kite/artifacts/
    }
}

segment("analyze") {
    dependsOn("build")
    
    inputs {
        artifact("release-apk")
        artifact("mapping")
    }
    
    execute {
        val apk = artifacts.get("release-apk")!!
        val mapping = artifacts.get("mapping")!!
        
        // Analyze the APK using the ProGuard mapping
        exec("apkanalyzer", "--mapping", mapping.absolutePath, apk.absolutePath)
    }
}
```

---

## Directory Artifacts

Artifacts can be entire directories:

```kotlin
segment("test") {
    outputs {
        artifact("test-reports", "build/test-results/")
        artifact("screenshots", "build/screenshots/")
    }
    
    execute {
        exec("./gradlew", "test")
        // Kite will copy the entire directories
    }
}

segment("publish-reports") {
    dependsOn("test")
    
    inputs {
        artifact("test-reports")
    }
    
    execute {
        val reportsDir = artifacts.get("test-reports")!!
        
        // Upload all HTML reports
        reportsDir.listFiles()?.forEach { file ->
            if (file.extension == "html") {
                exec("curl", "-F", "file=@${file.absolutePath}", "https://reports.example.com")
            }
        }
    }
}
```

---

## Parallel Consumption

Multiple segments can use the same artifact:

```kotlin
segment("build") {
    outputs {
        artifact("apk", "app.apk")
    }
    execute {
        exec("./gradlew", "assembleRelease")
    }
}

segment("unit-test") {
    dependsOn("build")
    inputs { artifact("apk") }
    execute {
        val apk = artifacts.get("apk")!!
        exec("./test-unit.sh", apk.absolutePath)
    }
}

segment("integration-test") {
    dependsOn("build")
    inputs { artifact("apk") }
    execute {
        val apk = artifacts.get("apk")!!
        exec("./test-integration.sh", apk.absolutePath)
    }
}

segment("ui-test") {
    dependsOn("build")
    inputs { artifact("apk") }
    execute {
        val apk = artifacts.get("apk")!!
        exec("./test-ui.sh", apk.absolutePath)
    }
}

ride {
    name = "Test All"
    maxConcurrency = 3
    
    flow {
        segment("build")
        
        // All three test segments can run in parallel, sharing the APK!
        parallel {
            segment("unit-test")
            segment("integration-test")
            segment("ui-test")
        }
    }
}
```

---

## API Reference

### Declaring Outputs

In a segment's `outputs {}` block:

```kotlin
outputs {
    artifact(name: String, path: String)
}
```

- **name**: Identifier for the artifact (used by consumers)
- **path**: Path to file/directory relative to workspace
- File/directory must exist when segment completes
- Kite copies it to `.kite/artifacts/<name>/`

### Declaring Inputs

In a segment's `inputs {}` block:

```kotlin
inputs {
    artifact(name: String)
}
```

- **name**: Identifier of the artifact to consume
- Must match a producer's output artifact name
- Segment must `dependsOn` the producer (directly or transitively)

### Accessing Artifacts

In a segment's `execute {}` block:

```kotlin
val file: File? = artifacts.get(name: String)
```

- **Returns**: `File` object pointing to `.kite/artifacts/<name>/`
- Returns `null` if artifact doesn't exist
- Always check for null or use `!!` if you're certain it exists

### File Operations

Once you have the artifact File:

```kotlin
val artifact = artifacts.get("my-artifact")!!

// File properties
artifact.name           // File name
artifact.absolutePath   // Full path
artifact.length()       // Size in bytes
artifact.exists()       // Check if exists
artifact.isFile         // Is it a file?
artifact.isDirectory    // Is it a directory?

// Reading
val text = artifact.readText()                // Read as string
val bytes = artifact.readBytes()              // Read as bytes
val lines = artifact.readLines()              // Read as list of lines

// Directory operations
if (artifact.isDirectory) {
    artifact.listFiles()?.forEach { file ->
        println(file.name)
    }
}

// Use in shell commands
exec("cp", artifact.absolutePath, "/tmp/output")
exec("unzip", artifact.absolutePath)
```

---

## Best Practices

### ✅ DO

1. **Always declare artifacts explicitly**
   ```kotlin
   outputs { artifact("apk", "app.apk") }
   ```

2. **Use meaningful artifact names**
   ```kotlin
   artifact("production-apk", "...")    // Good
   artifact("file1", "...")             // Bad
   ```

3. **Check for null when accessing**
   ```kotlin
   val apk = artifacts.get("apk")
   if (apk != null && apk.exists()) {
       // Use it
   }
   ```

4. **Create files in workspace, not temp dirs**
   ```kotlin
   val file = workspace.resolve("output.txt").toFile()
   file.writeText("data")
   ```

### ❌ DON'T

1. **Don't assume artifacts exist**
   ```kotlin
   val apk = artifacts.get("apk")!!  // Can crash if missing
   exec("adb", "install", apk.absolutePath)
   ```

2. **Don't use absolute paths in outputs**
   ```kotlin
   artifact("apk", "/tmp/app.apk")  // Bad - use relative paths
   ```

3. **Don't forget dependencies**
   ```kotlin
   segment("consumer") {
       // Missing: dependsOn("producer")
       inputs { artifact("data") }  // Won't work!
   }
   ```

---

## FAQ

### Q: Where are artifacts stored?

**A:** In `.kite/artifacts/` directory, organized by artifact name.

### Q: Are artifacts cleaned up?

**A:** Currently no. In future versions, use `--keep-artifacts=false` to clean up.

### Q: Can artifacts be arbitrary data (strings, objects)?

**A:** No. Artifacts are **files or directories only**. Use files to store data:

```kotlin
// Store data in a file
workspace.resolve("data.txt").toFile().writeText("my data")

// Or JSON
workspace.resolve("config.json").toFile().writeText("""{"key": "value"}""")
```

### Q: What happens if producer fails?

**A:** Artifacts are NOT stored if the segment fails.

### Q: Can I use artifacts across different rides?

**A:** No. Artifacts are scoped to a single ride execution. Use external storage for cross-ride data.

### Q: Can I reference artifacts from outside Kite?

**A:** Yes! They're just regular files in `.kite/artifacts/`. You can:

```bash
ls .kite/artifacts/
cat .kite/artifacts/my-artifact/output.txt
```

---

## Summary

**Artifacts = Files that segments share**

1. **Producer** creates file → declares as output
2. **Kite** copies file to `.kite/artifacts/`
3. **Consumer** accesses via `artifacts.get()`
4. **Multiple consumers** can use the same artifact
5. **Works with both files and directories**

That's it! Artifacts make it easy to pass build outputs between segments without manually managing file paths. 🎉
