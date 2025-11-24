# Cross-Ride Artifact Usage

**✅ SOLVED**: As of the latest version, Kite automatically handles cross-ride artifact sharing using a manifest file!

## The Solution: Automatic Manifest

Kite now automatically saves and restores artifact metadata in `.kite/artifacts/.manifest.json`:

```json
{
  "artifacts": {
    "apk": {
      "name": "apk",
      "relativePath": "apk",
      "type": "file",
      "sizeBytes": 5242880,
      "createdAt": 1763482090374
    },
    ...
  },
  "rideName": "BUILD",
  "timestamp": 1763482090374,
  "version": 1
}
```

**How it works:**

1. After ride completes: Kite saves manifest with all artifact metadata
2. CI uploads `.kite/artifacts/` (including `.manifest.json`)
3. CI downloads to new job/machine
4. Before ride starts: Kite restores artifacts from manifest
5. `artifacts.get()` now works! ✅

---

## Usage in Multi-Job CI

### GitHub Actions Example

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Build APK
        run: kite ride BUILD
        # Automatically saves .kite/artifacts/.manifest.json
      
      - name: Upload Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: .kite/artifacts/  # Includes manifest!
  
  test:
    needs: build
    runs-on: macos-latest  # Different machine!
    steps:
      - uses: actions/checkout@v4
      
      - name: Download Artifacts
        uses: actions/download-artifact@v4
        with:
          name: build-artifacts
          path: .kite/artifacts/  # Includes manifest!
      
      - name: Run Tests
        run: kite ride TEST
        # Automatically restores artifacts from manifest
        # artifacts.get("apk") works! ✅
```

### Kite Rides

```kotlin
// BUILD ride (Job 1)
segment("build-apk") {
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app.apk")
        artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
    }
    execute {
        exec("./gradlew", "assembleRelease")
    }
}

// TEST ride (Job 2 - different machine)
segment("test-apk") {
    inputs {
        artifact("apk")  // Declares dependency
    }
    
    execute {
        // artifacts.get() now works across jobs! ✅
        val apk = artifacts.get("apk")?.toFile()
        if (apk == null) error("APK artifact not found")
        
        exec("adb", "install", "-r", apk.absolutePath)
        exec("adb", "shell", "am", "instrument", "-w", "com.example.test")
    }
}
```

---

## How It Works

### 1. Artifact Creation (Build Job)

```kotlin
segment("build") {
    outputs {
        artifact("apk", "app.apk")
    }
    execute {
        exec("./gradlew", "assembleRelease")
        // After segment succeeds:
        // - APK copied to .kite/artifacts/apk
        // - Tracked in ArtifactManager
    }
}

// After ride completes:
// - Manifest saved to .kite/artifacts/.manifest.json
// - Contains metadata for all artifacts
```

**Manifest:**

```json
{
  "artifacts": {
    "apk": {
      "name": "apk",
      "relativePath": "apk",
      "type": "file",
      "sizeBytes": 5242880,
      "createdAt": 1763482090374
    }
  },
  "rideName": "BUILD",
  "timestamp": 1763482090374
}
```

### 2. CI Upload

```yaml
- uses: actions/upload-artifact@v4
  with:
    path: .kite/artifacts/  # Uploads both apk AND .manifest.json
```

### 3. CI Download (New Job)

```yaml
- uses: actions/download-artifact@v4
  with:
    path: .kite/artifacts/  # Downloads both apk AND .manifest.json
```

### 4. Artifact Restoration (Test Job)

```kotlin
// Before ride starts, Kite automatically:
// 1. Reads .kite/artifacts/.manifest.json
// 2. Finds: apk -> .kite/artifacts/apk
// 3. Registers in ArtifactManager: artifacts["apk"] = Path(".kite/artifacts/apk")

segment("test") {
    execute {
        val apk = artifacts.get("apk")?.toFile()  // ✅ Works!
        exec("adb", "install", apk.absolutePath)
    }
}
```

---

## Technical Details

### Thread-Safe Implementation

The manifest uses:

- **kotlinx.serialization** for JSON persistence
- **ReentrantReadWriteLock** for thread-safe reads/writes
- **Atomic file operations** (write to temp, atomic rename)
- **ConcurrentHashMap** in ArtifactManager for tracking

### Manifest Schema

```kotlin
@Serializable
data class ArtifactManifestData(
    val artifacts: Map<String, ArtifactEntry> = emptyMap(),
    val rideName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val version: Int = 1
)

@Serializable
data class ArtifactEntry(
    val name: String,
    val relativePath: String,  // Relative to .kite/artifacts/
    val type: String,           // "file" or "directory"
    val sizeBytes: Long,
    val createdAt: Long
)
```

### Automatic Operations

**On Ride Start:**

```kotlin
artifactManager.restoreFromManifest(File(".kite/artifacts"))
// Reads manifest, registers all artifacts
```

**On Ride Complete:**

```kotlin
artifactManager.saveManifest(File(".kite/artifacts"), rideName)
// Saves manifest with all current artifacts
```

---

## Complete Example: Android Multi-Job CI

### GitHub Actions Workflow

```yaml
name: Android CI/CD

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Build with Kite
        run: kite ride BUILD-ANDROID
      
      - name: Upload Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: android-artifacts
          path: .kite/artifacts/
  
  test:
    needs: build
    runs-on: macos-latest  # Different OS!
    steps:
      - uses: actions/checkout@v4
      
      - name: Download Artifacts
        uses: actions/download-artifact@v4
        with:
          name: android-artifacts
          path: .kite/artifacts/
      
      - name: Test with Kite
        run: kite ride TEST-ANDROID
  
  deploy:
    needs: [build, test]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      
      - name: Download Artifacts
        uses: actions/download-artifact@v4
        with:
          name: android-artifacts
          path: .kite/artifacts/
      
      - name: Deploy with Kite
        run: kite ride DEPLOY-ANDROID
        env:
          PLAY_STORE_KEY: ${{ secrets.PLAY_STORE_KEY }}
```

### Kite Rides

```kotlin
// BUILD-ANDROID ride
segment("build-apk") {
    outputs {
        artifact("debug-apk", "app/build/outputs/apk/debug/app-debug.apk")
        artifact("release-apk", "app/build/outputs/apk/release/app-release.apk")
        artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
    }
    execute {
        exec("./gradlew", "assembleDebug", "assembleRelease")
    }
}

ride {
    name = "BUILD-ANDROID"
    flow { segment("build-apk") }
}

// TEST-ANDROID ride (different job)
segment("test-debug") {
    inputs {
        artifact("debug-apk")  // From BUILD-ANDROID
    }
    execute {
        val apk = artifacts.get("debug-apk")?.toFile()!!
        exec("adb", "install", "-r", apk.absolutePath)
        exec("adb", "shell", "am", "instrument", "-w", "com.example.test")
    }
}

ride {
    name = "TEST-ANDROID"
    flow { segment("test-debug") }
}

// DEPLOY-ANDROID ride (different job)
segment("deploy-release") {
    inputs {
        artifact("release-apk")  // From BUILD-ANDROID
        artifact("mapping")      // From BUILD-ANDROID
    }
    execute {
        val apk = artifacts.get("release-apk")?.toFile()!!
        val mapping = artifacts.get("mapping")?.toFile()!!
        
        exec("fastlane", "supply",
            "--apk", apk.absolutePath,
            "--mapping", mapping.absolutePath,
            "--track", "production"
        )
    }
}

ride {
    name = "DEPLOY-ANDROID"
    flow { segment("deploy-release") }
}
```

---

## Benefits

### ✅ No Manual Work

- Manifest automatically saved/restored
- No need to manually register artifacts
- Just use `artifacts.get()` everywhere

### ✅ Type-Safe

- artifacts.get() returns `Path?`
- Compile-time checking
- No string-based file path errors

### ✅ Dependency Tracking

- `inputs {}` declares dependencies
- `outputs {}` declares productions
- Clear artifact flow

### ✅ Thread-Safe

- ConcurrentHashMap for tracking
- ReentrantReadWriteLock for manifest
- Atomic file operations

### ✅ CI-Friendly

- Standard directory: `.kite/artifacts/`
- Just upload/download the directory
- Manifest travels with artifacts

---

## Fallback: Direct File Access

If you need to access artifacts without using `artifacts.get()`, you can still use file paths directly:

```kotlin
segment("test") {
    execute {
        // Option 1: Use artifacts.get() (recommended)
        val apk = artifacts.get("apk")?.toFile()
        
        // Option 2: Direct file access (fallback)
        val apkDirect = File(".kite/artifacts/apk")
        
        // Both work!
    }
}
```

---

## Summary

**The manifest solves the cross-ride problem automatically!**

1. **Build job**: Create artifacts → Manifest saved
2. **CI**: Upload `.kite/artifacts/` (includes manifest)
3. **Test job**: Download artifacts → Manifest restored
4. **Use**: `artifacts.get()` works everywhere! ✅

**No configuration needed. It just works.** 🎯
