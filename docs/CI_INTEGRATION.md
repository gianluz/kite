# CI Integration Guide

How to use Kite artifacts with GitHub Actions, GitLab CI, and other CI systems.

## How It Works

**Kite automatically saves artifacts to `.kite/artifacts/` after each segment completes.**

This makes it trivial to:

1. Archive test results
2. Upload build artifacts
3. Share artifacts between CI jobs
4. Download artifacts for debugging

---

## Example: Test Results as Artifacts

### In Kite

```kotlin
segment("test-core") {
    outputs {
        // Point to where Gradle creates test results
        artifact("test-results-core", "kite-core/build/test-results/test")
        artifact("test-reports-core", "kite-core/build/reports/tests/test")
    }
    
    execute {
        exec("./gradlew", ":kite-core:test")
        // Kite automatically copies results to .kite/artifacts/ after this succeeds
    }
}
```

**That's it!** After the segment runs:

- `.kite/artifacts/test-results-core/` contains XML test results
- `.kite/artifacts/test-reports-core/` contains HTML reports

---

## GitHub Actions Integration

### Basic Setup

```yaml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Run Kite CI
        run: ./kite-cli/build/install/kite-cli/bin/kite-cli ride CI
      
      # Upload all artifacts from .kite/artifacts/
      - name: Upload Test Results
        if: always()  # Upload even if tests fail
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: .kite/artifacts/
          retention-days: 30
      
      # Optional: Publish test results as GitHub Actions checks
      - name: Publish Test Report
        if: always()
        uses: dorny/test-reporter@v1
        with:
          name: Test Results
          path: '.kite/artifacts/test-results-*/TEST-*.xml'
          reporter: java-junit
```

### Multi-Job Workflow with Artifact Sharing

```yaml
name: Build and Deploy

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
        run: |
          # This saves APK to .kite/artifacts/
          kite ride BUILD-RELEASE
      
      - name: Upload Build Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: .kite/artifacts/
  
  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      # Download artifacts from build job
      - name: Download Build Artifacts
        uses: actions/download-artifact@v4
        with:
          name: build-artifacts
          path: .kite/artifacts/
      
      - name: Test with Kite
        run: |
          # This uses the APK from .kite/artifacts/
          kite ride TEST-APK
  
  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      
      - name: Download Build Artifacts
        uses: actions/download-artifact@v4
        with:
          name: build-artifacts
          path: .kite/artifacts/
      
      - name: Deploy with Kite
        run: |
          # This uses the APK from .kite/artifacts/
          kite ride DEPLOY-PRODUCTION
        env:
          PLAY_STORE_KEY: ${{ secrets.PLAY_STORE_KEY }}
```

---

## GitLab CI Integration

```yaml
stages:
  - build
  - test
  - deploy

variables:
  KITE_CLI: "./kite-cli/build/install/kite-cli/bin/kite-cli"

build:
  stage: build
  script:
    - $KITE_CLI ride BUILD-RELEASE
  artifacts:
    paths:
      - .kite/artifacts/
    expire_in: 1 week

test:
  stage: test
  dependencies:
    - build
  script:
    - $KITE_CLI ride TEST-APK
  artifacts:
    when: always
    paths:
      - .kite/artifacts/
    reports:
      junit: .kite/artifacts/test-results-*/TEST-*.xml

deploy:
  stage: deploy
  dependencies:
    - build
  script:
    - $KITE_CLI ride DEPLOY-PRODUCTION
  only:
    - main
```

---

## Current Kite CI Example

Our actual CI ride saves test results as artifacts:

```kotlin
// .kite/segments/test.kite.kts
segment("test-core") {
    outputs {
        artifact("test-results-core", "kite-core/build/test-results/test")
        artifact("test-reports-core", "kite-core/build/reports/tests/test")
    }
    execute {
        exec("./gradlew", ":kite-core:test")
    }
}

// Repeat for test-dsl, test-runtime, test-cli, test-integration...

segment("publish-test-results") {
    dependsOn("test-core", "test-dsl", "test-runtime", "test-cli", "test-integration")
    
    inputs {
        artifact("test-reports-core")
        artifact("test-reports-dsl")
        artifact("test-reports-runtime")
        artifact("test-reports-cli")
        artifact("test-reports-integration")
    }
    
    execute {
        println("📊 Test Results Summary")
        // Print paths to all HTML reports
        val modules = listOf("core", "dsl", "runtime", "cli", "integration")
        for (module in modules) {
            val reportPath = artifacts.get("test-reports-$module")
            if (reportPath != null) {
                println("✅ $module: ${reportPath.resolve("index.html")}")
            }
        }
    }
}
```

**After running `kite ride CI`:**

```
.kite/artifacts/
├── test-results-core/          # JUnit XML results
│   ├── TEST-*.xml
├── test-reports-core/          # HTML reports
│   ├── index.html
│   ├── classes/
│   └── packages/
├── test-results-dsl/
├── test-reports-dsl/
... (and so on)
```

---

## Real-World Patterns

### Pattern 1: Archive Test Results

```kotlin
segment("test") {
    outputs {
        artifact("junit-results", "build/test-results/test")
        artifact("html-reports", "build/reports/tests/test")
    }
    execute {
        exec("./gradlew", "test")
    }
}
```

Then in CI:

```yaml
- uses: actions/upload-artifact@v4
  with:
    path: .kite/artifacts/
```

### Pattern 2: Build Once, Test/Deploy in Parallel

```kotlin
segment("build-apk") {
    outputs {
        artifact("apk", "app/build/outputs/apk/release/app-release.apk")
    }
    execute {
        exec("./gradlew", "assembleRelease")
    }
}

segment("test-apk") {
    dependsOn("build-apk")
    inputs { artifact("apk") }
    execute {
        val apk = artifacts.get("apk")?.toFile()!!
        exec("adb", "install", apk.absolutePath)
        // Run tests...
    }
}

segment("deploy-apk") {
    dependsOn("build-apk")
    inputs { artifact("apk") }
    execute {
        val apk = artifacts.get("apk")?.toFile()!!
        exec("fastlane", "supply", "--apk", apk.absolutePath)
    }
}

ride {
    name = "Release"
    flow {
        segment("build-apk")
        parallel {
            segment("test-apk")
            segment("deploy-apk")
        }
    }
}
```

### Pattern 3: Multi-Stage CI with Artifact Passing

**GitHub Actions:**

```yaml
jobs:
  build:
    steps:
      - run: kite ride BUILD
      - uses: actions/upload-artifact@v4
        with:
          name: kite-artifacts
          path: .kite/artifacts/
  
  test:
    needs: build
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: kite-artifacts
          path: .kite/artifacts/
      - run: kite ride TEST
  
  deploy:
    needs: [build, test]
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: kite-artifacts
          path: .kite/artifacts/
      - run: kite ride DEPLOY
```

**Kite rides:**

```kotlin
// BUILD ride
segment("build") {
    outputs {
        artifact("apk", "app.apk")
        artifact("mapping", "mapping.txt")
    }
    execute { /* build */ }
}

// TEST ride - uses artifacts from BUILD
segment("test") {
    inputs {
        artifact("apk")  // From previous ride via .kite/artifacts/
    }
    execute { /* test */ }
}

// DEPLOY ride - uses artifacts from BUILD
segment("deploy") {
    inputs {
        artifact("apk")      // From previous ride
        artifact("mapping")  // From previous ride
    }
    execute { /* deploy */ }
}
```

---

## Benefits

### ✅ Simple

- Just point to where build tools create files
- No manual copying or moving
- Kite handles everything

### ✅ Portable

- `.kite/artifacts/` is a standard directory
- Works with any CI system
- Easy to tar/zip for storage

### ✅ Shareable

- Upload once, use in multiple jobs
- Share between different rides
- Archive for debugging later

### ✅ No Complexity

- No special artifact management commands
- No need to manually copy files around
- Everything in one place

---

## Tips

### 1. Always upload artifacts on failure

```yaml
- uses: actions/upload-artifact@v4
  if: always()  # Upload even if tests fail
  with:
    path: .kite/artifacts/
```

### 2. Set retention periods

```yaml
- uses: actions/upload-artifact@v4
  with:
    path: .kite/artifacts/
    retention-days: 7  # Delete after 7 days
```

### 3. Name artifacts by job

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: test-results-${{ github.run_number }}
    path: .kite/artifacts/
```

### 4. Selective uploads

```yaml
# Only upload test reports, not all artifacts
- uses: actions/upload-artifact@v4
  with:
    name: test-reports
    path: .kite/artifacts/test-reports-*/
```

### 5. Download specific artifacts

```yaml
- uses: actions/download-artifact@v4
  with:
    name: build-artifacts
    path: .kite/artifacts/
    pattern: "*-apk"  # Only download APK artifacts
```

---

## Summary

**Kite makes CI integration trivial:**

1. **In Kite**: Point to where tools create files
   ```kotlin
   outputs { artifact("apk", "app/build/outputs/apk/release/app.apk") }
   ```

2. **In CI**: Upload `.kite/artifacts/`
   ```yaml
   - uses: actions/upload-artifact@v4
     with: { path: .kite/artifacts/ }
   ```

3. **In other jobs**: Download and use
   ```yaml
   - uses: actions/download-artifact@v4
   - run: kite ride TEST
   ```

**That's it!** No complexity, no custom scripts, just standard directories. 🎯
