# Simple Artifact Pattern - Real World Usage

## TL;DR - You DON'T need workspace.resolve()!

**For real builds (Gradle, Maven, etc.)**: Just point to where the tool creates files!

```kotlin
segment("build") {
    outputs {
        // Just tell Kite where Gradle puts the APK
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")
    }
    
    execute {
        exec("./gradlew", "assembleRelease")
        // Done! Kite copies the APK automatically
    }
}
```

**You ONLY need workspace.resolve() when:**

- Writing tests/demos that create files from scratch
- Your script creates files programmatically (not from external tools)

---

## Real-World Pattern

### ✅ SIMPLE - Just point to existing files

```kotlin
segment("build-android") {
    outputs {
        // Gradle creates these files - just point to them!
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")
        artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
    }
    
    execute {
        exec("./gradlew", "assembleRelease")
        // Kite automatically copies both files to .kite/artifacts/
    }
}

segment("deploy") {
    dependsOn("build-android")
    inputs { artifact("apk") }
    
    execute {
        val apk = artifacts.get("apk")?.toFile()!!
        
        // Use the artifact
        exec("adb", "install", apk.absolutePath)
    }
}
```

**What Kite does:**

1. `build-android` runs `./gradlew assembleRelease`
2. Gradle creates `app/build/outputs/apk/release/app-release.apk`
3. Segment succeeds
4. Kite copies `app/build/outputs/apk/release/app-release.apk` → `.kite/artifacts/apk`
5. `deploy` segment runs
6. `artifacts.get("apk")` returns `.kite/artifacts/apk`

---

## When DO You Need workspace.resolve()?

### ❌ NOT NEEDED - External tools create files

```kotlin
segment("build") {
    outputs {
        artifact("jar", "build/libs/myapp.jar")  // ✅ Simple!
    }
    execute {
        exec("./gradlew", "build")  // Gradle creates the JAR
    }
}

segment("bundle-ios") {
    outputs {
        artifact("ipa", "build/MyApp.ipa")  // ✅ Simple!
    }
    execute {
        exec("xcodebuild", "archive", ...)  // Xcode creates the IPA
    }
}
```

### ✅ NEEDED - Your script creates files

```kotlin
segment("generate-config") {
    outputs {
        artifact("config", "config.json")
    }
    execute {
        // YOU are creating the file, so you need workspace.resolve()
        val config = workspace.resolve("config.json").toFile()
        config.writeText("""{"version": "1.0"}""")
    }
}

segment("create-report") {
    outputs {
        artifact("report", "report.html")
    }
    execute {
        // YOU are creating the file
        val report = workspace.resolve("report.html").toFile()
        report.writeText("<html>...</html>")
    }
}
```

---

## Complete Real-World Examples

### Android Build Pipeline

```kotlin
segments {
    segment("build-apk") {
        outputs {
            artifact("debug-apk", "app/build/outputs/apk/debug/app-debug.apk")
            artifact("release-apk", "app/build/outputs/apk/release/app-release.apk")
        }
        execute {
            exec("./gradlew", "assembleDebug", "assembleRelease")
        }
    }
    
    segment("test-debug") {
        dependsOn("build-apk")
        inputs { artifact("debug-apk") }
        execute {
            val apk = artifacts.get("debug-apk")?.toFile()!!
            exec("adb", "install", "-r", apk.absolutePath)
            exec("adb", "shell", "am", "instrument", "-w", "com.example.test")
        }
    }
    
    segment("upload-release") {
        dependsOn("build-apk")
        inputs { artifact("release-apk") }
        execute {
            val apk = artifacts.get("release-apk")?.toFile()!!
            exec("fastlane", "supply", "--apk", apk.absolutePath)
        }
    }
}

ride {
    name = "Android-Release"
    flow {
        segment("build-apk")
        parallel {
            segment("test-debug")
            segment("upload-release")
        }
    }
}
```

### iOS Build Pipeline

```kotlin
segment("build-ios") {
    outputs {
        artifact("ipa", "build/MyApp.ipa")
        artifact("dsyms", "build/MyApp.app.dSYM.zip")
    }
    execute {
        exec("xcodebuild", "archive",
            "-scheme", "MyApp",
            "-archivePath", "build/MyApp.xcarchive"
        )
        exec("xcodebuild", "-exportArchive",
            "-archivePath", "build/MyApp.xcarchive",
            "-exportPath", "build",
            "-exportOptionsPlist", "ExportOptions.plist"
        )
    }
}

segment("upload-testflight") {
    dependsOn("build-ios")
    inputs {
        artifact("ipa")
    }
    execute {
        val ipa = artifacts.get("ipa")?.toFile()!!
        exec("xcrun", "altool",
            "--upload-app",
            "--file", ipa.absolutePath,
            "--type", "ios"
        )
    }
}
```

### Multi-Module Java Project

```kotlin
segment("build-all-modules") {
    outputs {
        artifact("api-jar", "api/build/libs/api-1.0.jar")
        artifact("core-jar", "core/build/libs/core-1.0.jar")
        artifact("app-jar", "app/build/libs/app-1.0.jar")
    }
    execute {
        exec("./gradlew", "build")
    }
}

segment("create-distribution") {
    dependsOn("build-all-modules")
    inputs {
        artifact("api-jar")
        artifact("core-jar")
        artifact("app-jar")
    }
    execute {
        val apiJar = artifacts.get("api-jar")?.toFile()!!
        val coreJar = artifacts.get("core-jar")?.toFile()!!
        val appJar = artifacts.get("app-jar")?.toFile()!!
        
        // Create distribution
        exec("mkdir", "-p", "dist/lib")
        exec("cp", apiJar.absolutePath, "dist/lib/")
        exec("cp", coreJar.absolutePath, "dist/lib/")
        exec("cp", appJar.absolutePath, "dist/lib/")
        exec("tar", "czf", "dist.tar.gz", "dist")
    }
}
```

---

## Comparison Table

| Scenario | Pattern | workspace.resolve()? |
|----------|---------|---------------------|
| Gradle builds JAR | Point to `build/libs/app.jar` | ❌ NO |
| Maven builds artifact | Point to `target/app.jar` | ❌ NO |
| Xcode builds IPA | Point to `build/MyApp.ipa` | ❌ NO |
| Script creates config file | Create in workspace | ✅ YES |
| Script generates report | Create in workspace | ✅ YES |
| npm builds bundle | Point to `dist/bundle.js` | ❌ NO |
| Docker builds image | Save to file, then point to it | ❌ NO |

---

## Why workspace.resolve() in Examples?

The artifact examples use `workspace.resolve()` because they're **demos that create files from scratch**:

```kotlin
// This is a DEMO - we're creating a fake APK for testing
val apk = workspace.resolve("app.apk").toFile()
apk.writeBytes(byteArrayOf(0x50, 0x4B, ...))  // Fake ZIP header
```

**In real usage**, Gradle creates the APK, so you don't need this:

```kotlin
// REAL USAGE - Gradle creates the APK
segment("build") {
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")  // ✅
    }
    execute {
        exec("./gradlew", "assembleRelease")  // ✅
    }
}
```

---

## Summary

### 90% of the time (real builds):

```kotlin
outputs {
    artifact("name", "path/where/tool/creates/file")
}
execute {
    exec("./gradlew", "build")  // Tool creates the file
}
```

### 10% of the time (script-generated files):

```kotlin
outputs {
    artifact("name", "filename")
}
execute {
    val file = workspace.resolve("filename").toFile()  // Create file
    file.writeText("content")
}
```

**Don't overthink it!** Just point to where your build tools put files. 🎯
