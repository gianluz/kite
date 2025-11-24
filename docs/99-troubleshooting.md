# Troubleshooting

Common issues and their solutions when working with Kite.

---

## Quick Diagnosis

**Problem categories:**

- [Installation & Setup](#installation--setup)
- [Execution Issues](#execution-issues)
- [Segment Problems](#segment-problems)
- [Artifact Issues](#artifact-issues)
- [Parallel Execution](#parallel-execution)
- [Secrets & Environment](#secrets--environment)
- [Dependencies](#dependencies)
- [CI/CD Integration](#cicd-integration)
- [Performance](#performance)

---

## Installation & Setup

### "No .kite directory found"

**Symptom:**

```
Error: No .kite directory found in current directory
```

**Cause:** Project not initialized for Kite

**Solution:**

```bash
# Create directory structure
mkdir -p .kite/segments .kite/rides

# Create a sample segment
cat > .kite/segments/hello.kite.kts << 'EOF'
segments {
    segment("hello") {
        execute {
            logger.info("Hello from Kite!")
        }
    }
}
EOF

# Create a sample ride
cat > .kite/rides/test.kite.kts << 'EOF'
ride {
    name = "Test"
    flow {
        segment("hello")
    }
}
EOF
```

---

### "kite-cli: command not found"

**Symptom:**

```bash
$ kite-cli ride CI
bash: kite: command not found
```

**Cause:** Kite CLI not built or not in PATH

**Solution:**

```bash
# Build the CLI
./gradlew :kite-cli:installDist

# Use full path
kite-cli/build/install/kite-cli/bin/kite-cli ride CI

# Or create an alias
alias kite='kite-cli/build/install/kite-cli/bin/kite-cli'

# Or add to PATH
export PATH="$PWD/kite-cli/build/install/kite-cli/bin:$PATH"
```

---

### "Permission denied" on gradlew

**Symptom:**

```
bash: ./gradlew: Permission denied
```

**Cause:** `gradlew` not executable

**Solution:**

```bash
chmod +x gradlew
./gradlew :kite-cli:installDist
```

Or:

```bash
sh gradlew :kite-cli:installDist
```

---

## Execution Issues

### "Ride not found"

**Symptom:**

```
Error: Ride 'CI' not found
```

**Cause:** Ride name doesn't match file or ride not defined

**Solution:**

```bash
# List available rides
kite-cli rides

# Check ride file exists
ls .kite/rides/

# Ensure ride name matches
cat .kite/rides/ci.kite.kts
```

**Example:**

```kotlin
// .kite/rides/ci.kite.kts
ride {
    name = "CI"  // Must match: kite-cli ride CI
    flow { /* ... */ }
}
```

---

### "Segment not found"

**Symptom:**

```
Error: Segment 'test' referenced in ride but not found
```

**Cause:** Segment referenced but not defined

**Solution:**

```bash
# List available segments
kite-cli segments

# Check segment files
ls .kite/segments/

# Verify segment is defined
grep -r "segment(\"test\")" .kite/segments/
```

---

### "Failed to load .kite files"

**Symptom:**

```
Error: Failed to load .kite files:
  build.kite.kts: Compilation error
```

**Cause:** Syntax error in .kite.kts file

**Solution:**

1. Check file syntax:

```kotlin
// Common issues:

// ❌ Missing closing brace
segment("test") {
    execute {
        exec("./gradlew", "test")
    // Missing }
}

// ✅ Correct
segment("test") {
    execute {
        exec("./gradlew", "test")
    }
}
```

2. Check IDE for errors
3. Run with `--debug` for details:

```bash
kite-cli ride CI --debug
```

---

### "Command execution failed"

**Symptom:**

```
Error: Command execution failed: ./gradlew test
Exit code: 1
```

**Cause:** External command failed

**Solution:**

1. **Test command directly:**

```bash
./gradlew test  # Does this work?
```

2. **Check working directory:**

```kotlin
segment("test") {
    execute {
        logger.info("Working dir: ${workspace.toAbsolutePath()}")
        exec("./gradlew", "test")
    }
}
```

3. **Check command exists:**

```kotlin
segment("test") {
    execute {
        val gradlew = workspace.resolve("gradlew").toFile()
        require(gradlew.exists()) { "gradlew not found" }
        require(gradlew.canExecute()) { "gradlew not executable" }
        
        exec("./gradlew", "test")
    }
}
```

---

## Segment Problems

### "Circular dependency detected"

**Symptom:**

```
Error: Circular dependency: build → test → build
```

**Cause:** Segments depend on each other in a cycle

**Solution:**

```kotlin
// ❌ Circular dependency
segment("build") {
    dependsOn("test")  // Depends on test
}
segment("test") {
    dependsOn("build")  // Depends on build - CIRCULAR!
}

// ✅ Correct - linear dependency
segment("build") {
    // No dependencies
}
segment("test") {
    dependsOn("build")  // Only depends on build
}
```

---

### "Timeout exceeded"

**Symptom:**

```
Error: Segment 'slow-build' exceeded timeout of 5 minutes
```

**Cause:** Segment took longer than configured timeout

**Solution:**

```kotlin
// Increase timeout
segment("slow-build") {
    timeout = 30.minutes  // Increased from 5m
    
    execute {
        exec("./slow-build.sh")
    }
}
```

Or remove timeout:

```kotlin
segment("very-slow-build") {
    timeout = null  // No timeout
    execute { /* ... */ }
}
```

---

### "Required environment variable not set"

**Symptom:**

```
IllegalArgumentException: Required secret environment variable 'API_KEY' is not set
```

**Cause:** Missing environment variable

**Solution:**

```bash
# Set the variable
export API_KEY=your-key-here
kite-cli ride Deploy

# Or in CI
env:
  API_KEY: ${{ secrets.API_KEY }}
run: kite-cli ride Deploy
```

Or make it optional:

```kotlin
segment("deploy") {
    execute {
        val apiKey = secret("API_KEY")  // Returns null if not set
        if (apiKey == null) {
            logger.warn("API_KEY not set, skipping deployment")
            return@execute
        }
        
        exec("deploy", "--key", apiKey)
    }
}
```

---

## Artifact Issues

### "Artifact not found"

**Symptom:**

```
artifacts.get("apk") returns null
```

**Causes & Solutions:**

**1. Producer segment didn't run:**

```kotlin
segment("test") {
    dependsOn("build")  // ✅ Add this
    inputs { artifact("apk") }
}
```

**2. Producer segment failed:**

- Check segment status
- Artifacts only stored on success

**3. Artifact name mismatch:**

```kotlin
// Producer
outputs { artifact("app-apk", "...") }

// Consumer - name must match exactly!
inputs { artifact("app-apk") }  // ✅ Correct
inputs { artifact("apk") }      // ❌ Wrong name
```

**4. File doesn't exist when segment completes:**

```kotlin
segment("build") {
    outputs { artifact("apk", "app.apk") }
    
    execute {
        exec("./gradlew", "assembleRelease")
        
        // Verify it exists
        val apkFile = workspace.resolve("app.apk").toFile()
        require(apkFile.exists()) { 
            "Build didn't produce APK at app.apk" 
        }
    }
}
```

---

### "File not found when accessing artifact"

**Symptom:**

```
java.nio.file.NoSuchFileException: app/build/outputs/apk/app.apk
```

**Cause:** File path doesn't exist

**Solution:**

1. **Use correct relative path:**

```kotlin
outputs {
    artifact("apk", "app/build/outputs/apk/release/app-release.apk")
    //                ↑ Path relative to workspace
}
```

2. **Check file was created:**

```bash
# After build, check if file exists
ls -la app/build/outputs/apk/release/
```

3. **Debug in segment:**

```kotlin
segment("debug") {
    execute {
        logger.info("Workspace: ${workspace.toAbsolutePath()}")
        logger.info("Files:")
        workspace.toFile().walkTopDown().forEach {
            logger.info("  ${it.relativeTo(workspace.toFile())}")
        }
    }
}
```

---

## Parallel Execution

### "Out of memory error with parallel execution"

**Symptom:**

```
OutOfMemoryError: Java heap space
```

**Cause:** Too many parallel segments exhausting memory

**Solution:**

```kotlin
ride {
    name = "CI"
    
    // Reduce parallelism
    maxConcurrency = 2  // Reduced from 4
    
    environment {
        // Increase memory per segment
        put("GRADLE_OPTS", "-Xmx4g")  // Increased from 2g
    }
}
```

**Calculate appropriate values:**

```
Available RAM: 16GB
OS overhead: 2GB
Available: 14GB

maxConcurrency = 2
Per segment: 14GB / 2 = 7GB
Set -Xmx6g (leave 1GB buffer)
```

---

### "Segments not running in parallel"

**Symptom:** Segments run sequentially despite `parallel {}` block

**Causes & Solutions:**

**1. Dependencies prevent parallelism:**

```kotlin
// ❌ Can't parallelize - test depends on build
parallel {
    segment("build")
    segment("test") { dependsOn("build") }
}

// ✅ Remove dependency or restructure
flow {
    segment("build")
    parallel {
        segment("test") { dependsOn("build") }
        segment("lint") { dependsOn("build") }
    }
}
```

**2. `maxConcurrency` too low:**

```kotlin
ride {
    maxConcurrency = 1  // ❌ Forces sequential
    maxConcurrency = 4  // ✅ Allow parallelism
}
```

**3. `--sequential` flag:**

```bash
kite-cli ride CI --sequential  # ❌ Forces sequential
kite-cli ride CI               # ✅ Allow parallelism
```

---

### "No speedup from parallelization"

**Cause:** Bottleneck segment takes longer than others

**Solution:**

```kotlin
// Before: One slow segment
segment("all-tests") {  // 10 minutes
    execute { exec("./gradlew", "test") }
}

// After: Split into parallel segments
parallel {
    segment("unit-tests") {      // 3 minutes
        execute { exec("./gradlew", ":app:test") }
    }
    segment("integration-tests") {  // 5 minutes
        execute { exec("./gradlew", ":core:test") }
    }
    segment("ui-tests") {         // 4 minutes
        execute { exec("./gradlew", ":ui:test") }
    }
}
// Total: 5 minutes (bottleneck) vs 10 minutes sequential
```

---

## Secrets & Environment

### "Secret still appears in logs"

**Symptom:**

```
Using API key: sk-1234567890  # Secret visible!
```

**Cause:** Using `env()` instead of `secret()`

**Solution:**

```kotlin
// ❌ Wrong - leaks in logs
val apiKey = env("API_KEY")
logger.info("Using API key: $apiKey")  // LEAKS!

// ✅ Correct - automatically masked
val apiKey = secret("API_KEY")
logger.info("Using API key: $apiKey")  // Masked: [API_KEY:***]
```

---

### "Environment variable not set in CI"

**Symptom:** Works locally but fails in CI

**Cause:** Variable not configured in CI

**Solution:**

**GitHub Actions:**

```yaml
- name: Deploy
  env:
    API_KEY: ${{ secrets.API_KEY }}  # Add this
  run: kite-cli ride Deploy
```

**GitLab CI:**

```yaml
deploy:
  script:
    - kite-cli ride Deploy
  variables:
    API_KEY: $CI_API_KEY  # Add this
```

**Check in segment:**

```kotlin
segment("debug-env") {
    execute {
        logger.info("CI Platform: ${env("CI")}")
        logger.info("Has API_KEY: ${env("API_KEY") != null}")
    }
}
```

---

## Dependencies

### "Unresolved reference" with @DependsOn

**Symptom:**

```kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.1")
import com.google.gson.Gson  // Unresolved reference
```

**Cause:** Dependencies not resolved yet

**Solution:**

1. **Run once to download:**

```bash
kite-cli ride Test  # Downloads dependencies
```

2. **Reload Gradle in IDE:**

- File → Reload All Gradle Projects

3. **Or add temporarily to build.gradle.kts:**

```kotlin
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}
```

---

### "Failed to resolve dependency"

**Symptom:**

```
Error: Failed to resolve: com.example:custom-lib:1.0.0
```

**Cause:** Dependency doesn't exist or wrong coordinates

**Solution:**

1. **Verify on Maven Central:** https://search.maven.org/
2. **Check coordinates:**

```kotlin
// Format: "group:artifact:version"
@file:DependsOn("com.google.code.gson:gson:2.10.1")  // ✅ Correct
@file:DependsOn("gson:2.10.1")  // ❌ Missing group
```

3. **Add custom repository:**

```kotlin
@file:Repository("https://repo.example.com/maven2/")
@file:DependsOn("com.example:custom-lib:1.0.0")
```

---

### "Dependency resolution is slow"

**Symptom:** First run takes long time

**Cause:** Downloading dependencies

**Solution:**

- **First run:** Slow (downloading)
- **Subsequent runs:** Fast (cached in `~/.ivy2/cache`)

**Speed up CI:**

```yaml
# GitHub Actions
- name: Cache Ivy
  uses: actions/cache@v3
  with:
    path: ~/.ivy2/cache
    key: ivy-${{ hashFiles('**/*.kite.kts') }}
```

---

## CI/CD Integration

### "Works locally but fails in CI"

**Common causes:**

**1. Environment differences:**

```kotlin
segment("debug") {
    execute {
        logger.info("OS: ${System.getProperty("os.name")}")
        logger.info("Java: ${System.getProperty("java.version")}")
        logger.info("PWD: ${workspace.toAbsolutePath()}")
    }
}
```

**2. Missing secrets:**

```yaml
# Ensure secrets are passed
env:
  API_KEY: ${{ secrets.API_KEY }}
```

**3. Resource constraints:**

```kotlin
ride {
    // CI runners have less RAM
    maxConcurrency = 2  // Reduced from 4
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g")  // Reduced from 4g
    }
}
```

**4. Different environment variables:**

```kotlin
segment("deploy") {
    condition = { ctx ->
        // Check both GitHub and GitLab variables
        ctx.env("GITHUB_REF")?.contains("main") == true ||
        ctx.env("CI_COMMIT_BRANCH") == "main"
    }
}
```

---

### "CI build is too slow"

**Solutions:**

**1. Enable caching:**

```yaml
- uses: gradle/actions/setup-gradle@v3  # Automatic caching
```

**2. Reduce parallelism:**

```kotlin
ride {
    maxConcurrency = 2  # CI runners are slower
}
```

**3. Split into stages:**

```kotlin
// Fast feedback first
segment("quick-tests")  # 30s
segment("lint")         # 30s

// Slower tests later
segment("integration-tests")  # 5m
```

---

## Performance

### "Segments take longer than expected"

**Diagnosis:**

```bash
# Run with verbose output to see timings
kite-cli ride CI --verbose

# Check individual segment durations
```

**Solutions:**

**1. Check command performance:**

```kotlin
segment("build") {
    execute {
        val start = System.currentTimeMillis()
        exec("./gradlew", "build")
        val duration = System.currentTimeMillis() - start
        logger.info("Build took ${duration}ms")
    }
}
```

**2. Profile Gradle builds:**

```bash
./gradlew build --profile
# Check build/reports/profile/
```

**3. Enable Gradle caching:**

```kotlin
environment {
    put("org.gradle.caching", "true")
}
```

---

### "High memory usage"

**Diagnosis:**

```bash
# Monitor during execution
top -p $(pgrep -f kite-cli)
```

**Solutions:**

**1. Reduce parallel segments:**

```kotlin
maxConcurrency = 2  # Reduced from 4
```

**2. Reduce memory per segment:**

```kotlin
environment {
    put("GRADLE_OPTS", "-Xmx2g")  # Reduced from 4g
}
```

**3. Run segments sequentially:**

```bash
kite-cli ride CI --sequential
```

---

## IDE Issues

### "No autocomplete for Kite DSL"

**Symptom:** No autocomplete for `segment {}`, `ride {}`, etc.

**Cause:** IDE not recognizing Kotlin script

**Solution:**

1. **Ensure .kite.kts extension**
2. **Reload Gradle project**
3. **Mark .kite directory:**
    - Right-click → Mark Directory as → Sources Root

---

### "Cannot resolve symbol in .kite.kts"

**Symptom:**

```kotlin
import com.google.gson.Gson  // Cannot resolve
```

**Cause:** IDE hasn't loaded dependencies

**Solution:**

1. **For @DependsOn:**
    - Run once: `kite-cli ride Test`
    - Reload Gradle project

2. **For classpath dependencies:**
    - Add to `build.gradle.kts`
    - Reload Gradle project

---

## Getting Help

### Enable Debug Mode

```bash
kite-cli ride CI --debug
```

Shows:

- Full stack traces
- Detailed execution flow
- All environment variables
- Dependency resolution details

### Check Logs

```bash
# Verbose output
kite-cli ride CI --verbose

# Save output to file
kite-cli ride CI 2>&1 | tee build.log
```

### Verify Setup

```bash
# Check Java version
java -version  # Should be 17+

# Check Gradle
./gradlew --version

# Check Kite structure
tree .kite/

# List available items
kite-cli segments
kite-cli rides
```

---

## Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| "No .kite directory" | Not initialized | `mkdir -p .kite/segments .kite/rides` |
| "Ride not found" | Wrong name | `kite rides` to list |
| "Segment not found" | Missing definition | `kite segments` to list |
| "Command failed" | External command error | Test command directly |
| "Artifact not found" | Missing dependency | Add `dependsOn()` |
| "Timeout exceeded" | Segment too slow | Increase `timeout` |
| "Out of memory" | Too many parallel | Reduce `maxConcurrency` |
| "Permission denied" | File not executable | `chmod +x gradlew` |
| "Failed to resolve" | Bad dependency | Check Maven Central |

---

## Summary

**Most common issues:**

1. ✅ Missing `.kite` directory → Create it
2. ✅ Wrong ride/segment name → Use `kite-cli rides/segments`
3. ✅ Missing dependencies → Add `dependsOn()`
4. ✅ Secrets leaking → Use `secret()` not `env()`
5. ✅ Out of memory → Reduce `maxConcurrency`

**Debug checklist:**

- [ ] Run with `--debug` or `--verbose`
- [ ] Check `.kite` directory structure
- [ ] Verify segment/ride names match files
- [ ] Test commands directly
- [ ] Check dependencies are correct
- [ ] Verify environment variables are set

**Still stuck?**

- Check example rides in `.kite/rides/`
- Review this guide's category sections
- Enable `--debug` mode for details

---

## Related Topics

- [Getting Started](01-getting-started.md) - Initial setup
- [CLI Reference](12-cli-reference.md) - Command documentation
- [Writing Segments](04-writing-segments.md) - Segment best practices
- [CI Integration](11-ci-integration.md) - CI/CD setup

---

[← Back to Documentation Home](00-index.md)
