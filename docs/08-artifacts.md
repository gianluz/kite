# Artifacts

Learn how to share files and directories between segments using Kite's artifact system.

---

## What Are Artifacts?

**Artifacts are files or directories that one segment produces and another segment consumes.**

Common examples:

- Build artifacts (JAR, APK, binary)
- Test reports (HTML, XML, coverage)
- Configuration files (JSON, YAML)
- Docker images (tar files)
- Distribution packages (zip, tar.gz)

Kite automatically manages artifact storage and retrieval so segments can easily share data.

---

## How It Works

### The Flow

```
Producer Segment          Artifact Manager         Consumer Segment
────────────────          ────────────────         ────────────────

1. Creates file      →    2. Copies to        →    3. Gets via
   app.apk                .kite/artifacts/             artifacts.get()
                           
4. Segment               5. Artifact stored           6. Returns Path
   succeeds                 with name "apk"               to artifact
```

### Key Concepts

- **Declaration**: Segments declare which artifacts they produce (outputs) and consume (inputs)
- **Storage**: Artifacts are stored in `.kite/artifacts/` directory
- **Retrieval**: Consumer segments access artifacts via the `artifacts` context property
- **Dependencies**: Consumers must `dependsOn()` their producers (directly or transitively)

---

## Basic Example

### Producer/Consumer Pattern

```kotlin
segments {
    // PRODUCER: Builds and declares an artifact
    segment("build") {
        outputs {
            artifact("apk", "app/build/outputs/apk/release/app-release.apk")
            //       ↑ name       ↑ path relative to workspace
        }
        
        execute {
            // Build the APK
            exec("./gradlew", "assembleRelease")
            
            // Kite automatically copies the APK to .kite/artifacts/apk/
        }
    }
    
    // CONSUMER: Uses the artifact
    segment("test") {
        dependsOn("build")  // Required: ensures build runs first
        
        inputs {
            artifact("apk")  // Declares we need the "apk" artifact
        }
        
        execute {
            // Get the artifact - returns a Path object
            val apkPath = artifacts.get("apk")
            
            if (apkPath != null && apkPath.toFile().exists()) {
                println("Testing APK: $apkPath")
                exec("adb", "install", "$apkPath")
                exec("adb", "shell", "am", "instrument", "...")
            } else {
                error("APK artifact not found!")
            }
        }
    }
}
```

**What happens:**

1. `build` segment creates `app/build/outputs/apk/release/app-release.apk`
2. After success, Kite copies it to `.kite/artifacts/apk/app-release.apk`
3. `test` segment calls `artifacts.get("apk")`
4. Returns `Path` to `.kite/artifacts/apk/app-release.apk`
5. `test` segment installs and tests the APK

---

## Complete Example: Android Release Pipeline

```kotlin
segments {
    // 1. Build unsigned APK
    segment("build") {
        description = "Build release APK"
        
        outputs {
            artifact("unsigned-apk", "app/build/outputs/apk/release/app-release-unsigned.apk")
        }
        
        execute {
            exec("./gradlew", "clean", "assembleRelease")
        }
    }
    
    // 2. Sign the APK
    segment("sign") {
        description = "Sign the APK with release keystore"
        dependsOn("build")
        
        inputs {
            artifact("unsigned-apk")
        }
        
        outputs {
            artifact("signed-apk", "app-release-signed.apk")
        }
        
        execute {
            val unsignedApk = artifacts.get("unsigned-apk")!!.toFile()
            val signedApk = workspace.resolve("app-release-signed.apk").toFile()
            
            // Sign with jarsigner
            exec("jarsigner",
                "-verbose",
                "-sigalg", "SHA256withRSA",
                "-digestalg", "SHA-256",
                "-keystore", "release.keystore",
                "-storepass", env("KEYSTORE_PASSWORD"),
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
    
    // 3. Run instrumented tests
    segment("test") {
        description = "Run instrumented tests on signed APK"
        dependsOn("sign")
        
        inputs {
            artifact("signed-apk")
        }
        
        outputs {
            artifact("test-results", "app/build/test-results/")
        }
        
        execute {
            val apk = artifacts.get("signed-apk")!!
            
            exec("adb", "install", "-r", "$apk")
            exec("./gradlew", "connectedAndroidTest")
        }
    }
    
    // 4. Deploy to Play Store
    segment("deploy") {
        description = "Upload APK to Play Store"
        dependsOn("sign")  // Note: Can run parallel with test!
        
        inputs {
            artifact("signed-apk")
        }
        
        execute {
            val apk = artifacts.get("signed-apk")!!.toFile()
            
            println("Uploading ${apk.name} (${apk.length()} bytes)")
            exec("bundle", "exec", "fastlane", "upload_to_play_store",
                "apk:${apk.absolutePath}"
            )
        }
    }
}

ride {
    name = "Release"
    
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

Segments can produce and consume multiple artifacts:

```kotlin
segment("build") {
    description = "Build all variants"
    
    outputs {
        artifact("debug-apk", "app/build/outputs/apk/debug/app-debug.apk")
        artifact("release-apk", "app/build/outputs/apk/release/app-release.apk")
        artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
        artifact("symbols", "app/build/symbols.zip")
    }
    
    execute {
        exec("./gradlew", "assemble")
        // All 4 artifacts copied automatically
    }
}

segment("analyze") {
    description = "Analyze APK size and ProGuard effectiveness"
    dependsOn("build")
    
    inputs {
        artifact("release-apk")
        artifact("mapping")
    }
    
    execute {
        val apk = artifacts.get("release-apk")!!
        val mapping = artifacts.get("mapping")!!
        
        // Analyze APK with ProGuard mapping
        exec("apkanalyzer", 
            "--proguard-mappings", "$mapping",
            "apk", "summary", "$apk"
        )
    }
}
```

---

## Directory Artifacts

Artifacts can be entire directories:

```kotlin
segment("test") {
    outputs {
        artifact("test-results", "build/test-results/")
        artifact("screenshots", "build/reports/androidTests/")
    }
    
    execute {
        exec("./gradlew", "test", "connectedAndroidTest")
        // Both directories copied recursively
    }
}

segment("publish-reports") {
    dependsOn("test")
    
    inputs {
        artifact("test-results")
        artifact("screenshots")
    }
    
    execute {
        val resultsDir = artifacts.get("test-results")!!.toFile()
        val screenshotsDir = artifacts.get("screenshots")!!.toFile()
        
        // Process all test result files
        resultsDir.walkTopDown()
            .filter { it.extension == "xml" }
            .forEach { file ->
                println("Processing: ${file.name}")
                // Parse and upload
            }
        
        // Upload screenshots
        screenshotsDir.walkTopDown()
            .filter { it.extension == "png" }
            .forEach { screenshot ->
                exec("curl", "-F", "file=@${screenshot.absolutePath}",
                    "https://reports.example.com/upload")
            }
    }
}
```

---

## Parallel Consumers

Multiple segments can safely consume the same artifact:

```kotlin
segment("build") {
    outputs {
        artifact("apk", "app-release.apk")
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
        exec("./scripts/test-unit.sh", "$apk")
    }
}

segment("integration-test") {
    dependsOn("build")
    inputs { artifact("apk") }
    execute {
        val apk = artifacts.get("apk")!!
        exec("./scripts/test-integration.sh", "$apk")
    }
}

segment("security-scan") {
    dependsOn("build")
    inputs { artifact("apk") }
    execute {
        val apk = artifacts.get("apk")!!
        exec("apkscan", "--security", "$apk")
    }
}

ride {
    name = "Full Test Suite"
    maxConcurrency = 3
    
    flow {
        segment("build")
        
        // All three tests run in parallel, sharing the APK
        parallel {
            segment("unit-test")
            segment("integration-test")
            segment("security-scan")
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

**Parameters:**

- `name` - Unique identifier for the artifact (used by consumers)
- `path` - Path to file/directory **relative to workspace**

**Rules:**

- The file/directory must exist when the segment completes successfully
- Kite copies it to `.kite/artifacts/<name>/`
- If the path doesn't exist, the segment fails
- Multiple segments can declare artifacts with the same name (last writer wins)

**Example:**

```kotlin
outputs {
    artifact("jar", "build/libs/myapp.jar")
    artifact("docs", "build/docs/javadoc/")
}
```

### Declaring Inputs

In a segment's `inputs {}` block:

```kotlin
inputs {
    artifact(name: String)
}
```

**Parameters:**

- `name` - Identifier of the artifact to consume

**Rules:**

- Must match a producer's output artifact name exactly
- Segment must `dependsOn()` the producer (directly or via transitive dependency)
- If the artifact doesn't exist at runtime, `artifacts.get()` returns `null`

**Example:**

```kotlin
inputs {
    artifact("jar")
    artifact("docs")
}
```

### Accessing Artifacts

In a segment's `execute {}` block via `ExecutionContext`:

```kotlin
val path: Path? = artifacts.get(name: String)
```

**Returns:**

- `Path` object pointing to `.kite/artifacts/<name>/`
- `null` if artifact doesn't exist

**Path Operations:**

```kotlin
val artifact = artifacts.get("my-artifact")!!

// Convert to File if needed
val file = artifact.toFile()

// File/Path properties
artifact.fileName           // File name (as Path)
file.name                   // File name (as String)
file.absolutePath           // Full path
file.length()               // Size in bytes
file.exists()               // Check existence
file.isFile                 // Is it a file?
file.isDirectory            // Is it a directory?

// Reading
val text = file.readText()              // Read as string
val bytes = file.readBytes()            // Read as bytes
val lines = file.readLines()            // Read lines

// Directory operations
if (file.isDirectory) {
    file.walkTopDown().forEach { item ->
        println(item.name)
    }
}

// Use in commands (convert Path to String)
exec("cp", "$artifact", "/tmp/backup")
exec("unzip", "$artifact", "-d", "/tmp/extract")
```

### Other Artifact Manager Methods

Available via `artifacts` in `ExecutionContext`:

```kotlin
// Check if artifact exists
val hasApk: Boolean = artifacts.has("apk")

// List all artifact names
val allArtifacts: Set<String> = artifacts.list()

// Remove an artifact (rarely needed)
artifacts.remove("old-artifact")

// Clear all artifacts (rarely needed)
artifacts.clear()
```

---

## Storage Details

### Where Are Artifacts Stored?

Artifacts are stored in `.kite/artifacts/` directory:

```
project/
├── .kite/
│   └── artifacts/
│       ├── apk/
│       │   └── app-release.apk
│       ├── test-results/
│       │   ├── test1.xml
│       │   └── test2.xml
│       └── mapping/
│           └── mapping.txt
├── app/
└── build.gradle.kts
```

### Implementation

Kite uses `FileSystemArtifactManager`:

- **Thread-safe**: Uses `ConcurrentHashMap` for tracking
- **Automatic copying**: Files/directories copied when `put()` is called
- **Recursive copy**: Directories copied with all contents
- **Replace existing**: `StandardCopyOption.REPLACE_EXISTING` for updates

---

## Best Practices

### ✅ DO

**1. Always declare artifacts explicitly**

```kotlin
outputs {
    artifact("apk", "app/build/outputs/apk/release/app.apk")
}
```

**2. Use meaningful, descriptive names**

```kotlin
artifact("production-apk", "...")     // ✅ Clear
artifact("debug-symbols", "...")      // ✅ Descriptive
artifact("file1", "...")              // ❌ Vague
artifact("x", "...")                  // ❌ Meaningless
```

**3. Use relative paths from workspace**

```kotlin
artifact("report", "build/reports/test.html")     // ✅ Good
artifact("report", "./build/reports/test.html")   // ✅ OK
```

**4. Check for null when accessing**

```kotlin
val apk = artifacts.get("apk")
if (apk != null && apk.toFile().exists()) {
    // Use it safely
} else {
    error("Required artifact 'apk' not found")
}
```

**5. Use `dependsOn()` for all producers**

```kotlin
segment("consumer") {
    dependsOn("producer")  // ✅ Required
    inputs { artifact("data") }
}
```

### ❌ DON'T

**1. Don't assume artifacts exist**

```kotlin
val apk = artifacts.get("apk")!!  // ❌ Can crash
exec("adb", "install", "$apk")
```

**2. Don't use absolute paths**

```kotlin
artifact("apk", "/tmp/app.apk")  // ❌ Bad
artifact("apk", "${System.getProperty("user.home")}/app.apk")  // ❌ Bad
```

**3. Don't forget dependencies**

```kotlin
segment("consumer") {
    // ❌ Missing: dependsOn("producer")
    inputs { artifact("data") }  // Won't work!
}
```

**4. Don't store artifacts in temp directories**

```kotlin
execute {
    val file = File("/tmp/output.txt")  // ❌ May not exist later
    file.writeText("data")
}
```

Instead, use workspace:

```kotlin
execute {
    val file = workspace.resolve("output.txt").toFile()  // ✅ Good
    file.writeText("data")
}
```

---

## Common Patterns

### Pattern 1: Build → Test → Deploy

```kotlin
segment("build") {
    outputs { artifact("jar", "build/libs/app.jar") }
    execute { exec("./gradlew", "build") }
}

segment("test") {
    dependsOn("build")
    inputs { artifact("jar") }
    execute {
        val jar = artifacts.get("jar")!!
        exec("java", "-jar", "$jar", "--test")
    }
}

segment("deploy") {
    dependsOn("test")
    inputs { artifact("jar") }
    execute {
        val jar = artifacts.get("jar")!!
        exec("scp", "$jar", "server:/apps/")
    }
}
```

### Pattern 2: Fan-Out Testing

```kotlin
segment("build") {
    outputs { artifact("apk", "app.apk") }
    execute { exec("./gradlew", "assembleDebug") }
}

// Multiple parallel tests
listOf("unit", "integration", "ui").forEach { testType ->
    segment("test-$testType") {
        dependsOn("build")
        inputs { artifact("apk") }
        execute {
            val apk = artifacts.get("apk")!!
            exec("./test-$testType.sh", "$apk")
        }
    }
}

ride {
    name = "Test Suite"
    flow {
        segment("build")
        parallel {
            segment("test-unit")
            segment("test-integration")
            segment("test-ui")
        }
    }
}
```

### Pattern 3: Report Aggregation

```kotlin
segment("test-module-1") {
    outputs { artifact("results-1", "module1/build/test-results/") }
    execute { exec("./gradlew", ":module1:test") }
}

segment("test-module-2") {
    outputs { artifact("results-2", "module2/build/test-results/") }
    execute { exec("./gradlew", ":module2:test") }
}

segment("aggregate-reports") {
    dependsOn("test-module-1", "test-module-2")
    inputs {
        artifact("results-1")
        artifact("results-2")
    }
    
    outputs {
        artifact("final-report", "aggregated-report.html")
    }
    
    execute {
        val results1 = artifacts.get("results-1")!!.toFile()
        val results2 = artifacts.get("results-2")!!.toFile()
        
        val allResults = results1.walkTopDown() + results2.walkTopDown()
        
        // Aggregate and generate report
        val report = workspace.resolve("aggregated-report.html").toFile()
        report.writeText(generateReport(allResults.toList()))
    }
}
```

---

## Troubleshooting

### Problem: Artifact not found

**Symptom:**

```
artifacts.get("apk") returns null
```

**Causes:**

1. Producer segment hasn't run yet
2. Producer segment failed (artifacts only stored on success)
3. Artifact name mismatch (`"apk"` vs `"APK"` - names are case-sensitive)
4. Missing `dependsOn()` relationship

**Solution:**

```kotlin
segment("consumer") {
    dependsOn("producer")  // ✅ Add this
    inputs { artifact("apk") }  // ✅ Match exact name
    execute {
        val apk = artifacts.get("apk")
        requireNotNull(apk) { "Artifact 'apk' not found. Did producer succeed?" }
    }
}
```

### Problem: File not found when accessing artifact

**Symptom:**

```
java.nio.file.NoSuchFileException: app/build/outputs/apk/app.apk
```

**Cause:**
The file wasn't created by the producer segment.

**Solution:**

```kotlin
segment("build") {
    outputs { artifact("apk", "app/build/outputs/apk/app.apk") }
    execute {
        exec("./gradlew", "assembleRelease")
        
        // Verify it exists
        val apkFile = workspace.resolve("app/build/outputs/apk/app.apk").toFile()
        require(apkFile.exists()) { "Build didn't produce APK at expected location" }
    }
}
```

### Problem: Artifact is empty or corrupted

**Cause:**
File was declared before it was fully written.

**Solution:**
Ensure all file operations complete before segment ends:

```kotlin
execute {
    exec("./gradlew", "build")  // Wait for completion
    
    // If async operations, wait for them
    Thread.sleep(1000)  // Or proper synchronization
}
```

---

## FAQ

### Q: Can I store arbitrary data as artifacts?

**A:** No. Artifacts must be **files or directories**. To share data:

```kotlin
// Store as JSON file
val data = mapOf("key" to "value")
workspace.resolve("data.json").toFile()
    .writeText(json.encodeToString(data))

// Later retrieve
val json = artifacts.get("data")!!.toFile().readText()
val data = json.decodeFromString<Map<String, String>>(json)
```

### Q: Are artifacts cleaned up automatically?

**A:** Currently no. Artifacts persist in `.kite/artifacts/` after the ride completes. You can manually delete the
directory or call `artifacts.clear()`.

### Q: Can artifacts be shared across different ride executions?

**A:** No. Each ride execution has its own artifact storage. For cross-ride persistence, use external storage (S3,
Artifactory, etc.) and save/load within segments.

### Q: What happens if the same artifact name is declared twice?

**A:** Last writer wins. If two segments produce `artifact("result", "...")`, the second one overwrites the first.

### Q: Can I access artifacts from outside Kite?

**A:** Yes! They're regular files in `.kite/artifacts/`:

```bash
ls -la .kite/artifacts/
cat .kite/artifacts/apk/app.apk
```

### Q: Is there a size limit for artifacts?

**A:** No hard limit, but large artifacts (GBs) will slow down copying. Consider:

- Compressing large directories
- Storing only necessary files
- Using external storage for very large assets

---

## Summary

**Artifacts = Files shared between segments**

- **Declare** outputs in producers, inputs in consumers
- **Store** automatically in `.kite/artifacts/`
- **Access** via `artifacts.get("name")`
- **Share** safely across parallel consumers
- **Persist** until manually deleted

Artifacts make it easy to pass build outputs, test results, and other files between segments without manual path
management. 🎯

---

## Related Topics

- [Writing Segments](04-writing-segments.md) - Learn more about segment configuration
- [Execution Context](06-execution-context.md) - Complete API reference including `artifacts`
- [Parallel Execution](07-parallel-execution.md) - Share artifacts across parallel flows

---

## Next Steps

- [Learn about secret management →](09-secrets.md)
- [Explore external dependencies →](10-external-dependencies.md)
- [Integrate with CI/CD →](11-ci-integration.md)
