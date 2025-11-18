# Cross-Ride Artifact Usage

How to use artifacts from a previous ride/CI job in a new ride.

## The Problem

When you download artifacts from CI to `.kite/artifacts/`, they're just files. The new ride doesn't know about them:

```yaml
# Job 1: Build
- run: kite ride BUILD  # Creates artifacts
- uses: actions/upload-artifact@v4  # Uploads .kite/artifacts/

# Job 2: Test (clean machine)
- uses: actions/download-artifact@v4  # Downloads to .kite/artifacts/
- run: kite ride TEST  # ❌ artifacts.get("apk") returns null!
```

**Why?** Because `artifacts.get("apk")` only knows about artifacts created **in the current ride execution**.

## Solution 1: Use File Paths Directly (Simplest)

Instead of using `artifacts.get()`, just reference the files directly:

```kotlin
// In TEST ride (uses artifacts from BUILD ride)
segment("test-apk") {
    execute {
        // Don't use artifacts.get() - just use the file path!
        val apk = File(".kite/artifacts/apk")
        
        if (!apk.exists()) {
            error("APK not found at ${apk.absolutePath}")
        }
        
        println("Testing APK: ${apk.absolutePath}")
        exec("adb", "install", apk.absolutePath)
        exec("adb", "shell", "am", "instrument", "-w", "com.example.test")
    }
}
```

**Pros:**

- ✅ Simple and direct
- ✅ Works across rides/jobs
- ✅ No complexity

**Cons:**

- ❌ Loses the artifact dependency tracking
- ❌ No automatic validation that artifact exists

---

## Solution 2: Recreate Artifact Registrations

If you want to keep using `artifacts.get()`, you need to "re-register" the artifacts in the new ride:

```kotlin
// In TEST ride
segment("register-artifacts") {
    description = "Registers artifacts from previous ride"
    
    execute {
        // Manually register artifacts that were downloaded from CI
        val apkPath = workspace.resolve(".kite/artifacts/apk")
        if (apkPath.toFile().exists()) {
            artifacts.put("apk", apkPath)
            println("✅ Registered artifact: apk")
        } else {
            error("Artifact 'apk' not found in .kite/artifacts/")
        }
        
        val mappingPath = workspace.resolve(".kite/artifacts/mapping")
        if (mappingPath.toFile().exists()) {
            artifacts.put("mapping", mappingPath)
            println("✅ Registered artifact: mapping")
        }
    }
}

segment("test-apk") {
    dependsOn("register-artifacts")
    
    execute {
        // Now artifacts.get() works!
        val apk = artifacts.get("apk")?.toFile()
        if (apk == null) error("APK artifact not found")
        
        exec("adb", "install", apk.absolutePath)
    }
}

ride {
    name = "TEST"
    flow {
        segment("register-artifacts")  // First!
        segment("test-apk")
    }
}
```

**Pros:**

- ✅ Keeps using `artifacts.get()`
- ✅ Can validate artifacts exist
- ✅ Clear dependency tracking

**Cons:**

- ❌ Extra boilerplate segment
- ❌ Need to know artifact names

---

## Solution 3: Smart Helper Function

Create a helper that tries both:

```kotlin
segment("test-apk") {
    execute {
        // Helper function that checks both artifact manager and file system
        fun getArtifactOrFile(name: String): File? {
            // Try artifact manager first (same-ride artifacts)
            val fromArtifacts = artifacts.get(name)?.toFile()
            if (fromArtifacts?.exists() == true) return fromArtifacts
            
            // Try .kite/artifacts/ directly (cross-ride artifacts)
            val fromFS = File(".kite/artifacts/$name")
            if (fromFS.exists()) return fromFS
            
            return null
        }
        
        val apk = getArtifactOrFile("apk")
        if (apk == null) error("APK not found")
        
        exec("adb", "install", apk.absolutePath)
    }
}
```

**Pros:**

- ✅ Works for both same-ride and cross-ride artifacts
- ✅ No need to register artifacts
- ✅ Clean API

**Cons:**

- ❌ Need to copy helper to each segment

---

## Recommended Patterns

### Pattern A: Single-Job CI (No Cross-Ride Issues)

**Best for:** All work in one CI job

```yaml
# GitHub Actions
- run: kite ride FULL-CI  # Build + Test + Deploy all in one ride
```

```kotlin
ride {
    name = "FULL-CI"
    flow {
        segment("build")     // Creates artifacts
        segment("test")      // Uses artifacts via artifacts.get()
        segment("deploy")    // Uses artifacts via artifacts.get()
    }
}
```

**✅ No issues** - everything in one ride execution.

---

### Pattern B: Multi-Job CI with File Paths (Simplest)

**Best for:** Build/test/deploy in separate jobs

```yaml
jobs:
  build:
    steps:
      - run: kite ride BUILD
      - uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: .kite/artifacts/
  
  test:
    needs: build
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: build-artifacts
          path: .kite/artifacts/
      - run: kite ride TEST
```

```kotlin
// BUILD ride
segment("build") {
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app.apk")
    }
    execute {
        exec("./gradlew", "assembleRelease")
    }
}

// TEST ride (different job/machine)
segment("test") {
    execute {
        // Use file path directly - simpler!
        val apk = File(".kite/artifacts/apk")
        if (!apk.exists()) error("APK not found")
        
        exec("adb", "install", apk.absolutePath)
    }
}
```

**✅ Recommended** - simple and works everywhere.

---

### Pattern C: Multi-Job CI with Registration

**Best for:** Want to keep artifact tracking

```kotlin
// TEST ride
segment("register-build-artifacts") {
    execute {
        // Register artifacts downloaded from previous job
        val artifactsDir = File(".kite/artifacts")
        artifactsDir.listFiles()?.forEach { file ->
            artifacts.put(file.name, file.toPath())
            println("Registered: ${file.name}")
        }
    }
}

segment("test") {
    dependsOn("register-build-artifacts")
    execute {
        val apk = artifacts.get("apk")?.toFile()!!
        exec("adb", "install", apk.absolutePath)
    }
}

ride {
    name = "TEST"
    flow {
        segment("register-build-artifacts")  // First!
        segment("test")
    }
}
```

---

## Complete Example: Android Multi-Job CI

### GitHub Actions Workflow

```yaml
name: Android CI/CD

on: [push]

jobs:
  # Job 1: Build on Linux
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Build APK
        run: kite ride BUILD-ANDROID
      
      - name: Upload Build Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: android-artifacts
          path: .kite/artifacts/
  
  # Job 2: Test on different machine
  test:
    needs: build
    runs-on: macos-latest  # Different OS!
    steps:
      - uses: actions/checkout@v4
      
      - name: Download Build Artifacts
        uses: actions/download-artifact@v4
        with:
          name: android-artifacts
          path: .kite/artifacts/
      
      - name: Run Tests
        run: kite ride TEST-ANDROID
  
  # Job 3: Deploy
  deploy:
    needs: [build, test]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      
      - name: Download Build Artifacts
        uses: actions/download-artifact@v4
        with:
          name: android-artifacts
          path: .kite/artifacts/
      
      - name: Deploy to Play Store
        run: kite ride DEPLOY-ANDROID
        env:
          PLAY_STORE_KEY: ${{ secrets.PLAY_STORE_KEY }}
```

### Kite Rides

```kotlin
// BUILD-ANDROID ride (Job 1)
segment("build-apk") {
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")
        artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
    }
    execute {
        exec("./gradlew", "assembleRelease")
    }
}

ride {
    name = "BUILD-ANDROID"
    flow {
        segment("build-apk")
    }
}

// TEST-ANDROID ride (Job 2 - different machine)
segment("test-apk") {
    execute {
        // Use file path directly - works across jobs!
        val apk = File(".kite/artifacts/apk")
        if (!apk.exists()) {
            error("APK not found. Make sure artifacts were downloaded from previous job.")
        }
        
        println("Installing APK: ${apk.absolutePath}")
        exec("adb", "install", "-r", apk.absolutePath)
        
        println("Running instrumentation tests...")
        exec("adb", "shell", "am", "instrument", "-w", "com.example.test")
    }
}

ride {
    name = "TEST-ANDROID"
    flow {
        segment("test-apk")
    }
}

// DEPLOY-ANDROID ride (Job 3 - different machine)
segment("deploy-to-playstore") {
    execute {
        // Use file paths directly
        val apk = File(".kite/artifacts/apk")
        val mapping = File(".kite/artifacts/mapping")
        
        if (!apk.exists()) error("APK not found")
        if (!mapping.exists()) error("Mapping file not found")
        
        println("Deploying APK: ${apk.absolutePath}")
        exec("fastlane", "supply",
            "--apk", apk.absolutePath,
            "--mapping", mapping.absolutePath,
            "--track", "production"
        )
    }
}

ride {
    name = "DEPLOY-ANDROID"
    flow {
        segment("deploy-to-playstore")
    }
}
```

---

## Comparison

| Approach | Same-Ride | Cross-Ride | Complexity |
|----------|-----------|------------|------------|
| **artifacts.get()** | ✅ Yes | ❌ No | Simple |
| **File paths** | ✅ Yes | ✅ Yes | Simple |
| **Register artifacts** | ✅ Yes | ✅ Yes | Medium |
| **Helper function** | ✅ Yes | ✅ Yes | Medium |

---

## Recommendations

### ✅ For Single-Ride Workflows

```kotlin
// Use artifacts.get() - clean and tracked
val apk = artifacts.get("apk")?.toFile()!!
```

### ✅ For Multi-Job CI Workflows

```kotlin
// Use file paths - simple and works everywhere
val apk = File(".kite/artifacts/apk")
if (!apk.exists()) error("APK not found")
```

### ✅ For Complex Workflows

```kotlin
// Add registration segment if you want artifact tracking
segment("register-artifacts") {
    execute {
        File(".kite/artifacts").listFiles()?.forEach { file ->
            artifacts.put(file.name, file.toPath())
        }
    }
}
```

---

## Summary

**The core issue:** `artifacts.get()` only knows about artifacts from the **current ride execution**.

**The solution:** Use file paths directly when working across rides/jobs:

```kotlin
// Instead of:
val apk = artifacts.get("apk")?.toFile()!!  // ❌ null in new ride

// Use:
val apk = File(".kite/artifacts/apk")       // ✅ works everywhere
```

**Why this is good:**

- ✅ Simple and explicit
- ✅ Works across rides, jobs, machines
- ✅ Standard directory structure
- ✅ No hidden magic

**The `.kite/artifacts/` directory is the contract!** As long as files are there, segments can use them. 🎯
