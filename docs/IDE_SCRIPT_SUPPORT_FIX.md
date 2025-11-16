# Fixing IDE Autocomplete for .kite.kts Files

## Problem

`.kite.kts` files show linter errors like:

- `Unresolved reference: ride`
- `Unresolved reference: segments`
- `Cannot access script base class 'io.kite.dsl.KiteScript'`

But the files compile and execute fine from the command line.

## Root Cause

IntelliJ IDEA is not properly recognizing the custom script definition even though:

- The `@KotlinScript` annotation is present on `KiteScript` class
- The compilation configuration is defined
- The META-INF resource file exists

This is because:

1. IntelliJ caches script definitions and doesn't always refresh them
2. The script definition module (`kite-dsl`) needs to be on the IDE's classpath
3. IntelliJ's Kotlin plugin may not have reindexed after changes

## Solution

### Step 1: Ensure kite-dsl is Built

```bash
./gradlew :kite-dsl:clean :kite-dsl:build -x test
```

### Step 2: Reload Gradle Project

1. Open Gradle tool window (View → Tool Windows → Gradle)
2. Click the **Reload All Gradle Projects** button (circular arrows icon)
3. Wait for indexing to complete

### Step 3: Invalidate Caches

1. Go to **File → Invalidate Caches...**
2. Select all checkboxes:
    - ✅ Invalidate and Restart
    - ✅ Clear file system cache and Local History
    - ✅ Clear downloaded shared indexes
    - ✅ Clear VCS Log caches and indexes
3. Click **"Invalidate and Restart"**

### Step 4: Clear Kotlin Script Cache

After restart, also clear the Kotlin-specific script cache:

```bash
rm -rf ~/Library/Caches/JetBrains/IntelliJIdea*/kotlin/script-cache
```

### Step 5: Verify Module Dependencies

Make sure `.kite` directory is marked as a **Source Root**:

1. Right-click on `.kite` folder in Project view
2. Select **Mark Directory as → Sources Root**

If this doesn't help, check that `kite-dsl` is a dependency in your project structure:

1. Open **File → Project Structure → Modules**
2. Select your main module
3. Go to **Dependencies** tab
4. Ensure `kite-dsl` appears in the list

### Step 6: Build Project

After all cache clearing:

```bash
./gradlew build -x test
```

Then in IntelliJ: **Build → Rebuild Project**

## Alternative: Use IntelliJ's Built-in Script Support

If the above doesn't work, you can manually configure script support:

1. Go to **Settings/Preferences → Languages & Frameworks → Kotlin → Kotlin Scripting**
2. Click **+** to add a new script definition
3. Select **From a library**
4. Choose `kite-dsl` module
5. Apply changes

## Verification

Open any `.kite.kts` file (e.g., `.kite/rides/ci.kite.kts`) and:

1. Type `ride {` - autocomplete should suggest `name`, `maxConcurrency`, `flow`
2. Inside `flow {}`, typing `segment("` should show available segments
3. No red underlines should appear on `ride`, `segments`, etc.
4. Ctrl+Click on `segment` should navigate to the definition

## If Still Not Working

### Check Kotlin Plugin Version

1. Go to **Settings → Plugins**
2. Find "Kotlin" plugin
3. Ensure it's version 2.0.0 or later (bundled with IntelliJ 2024.2+)

### Enable K2 Mode (Optional - Advanced)

K2 mode has better script support:

1. Go to **Settings → Languages & Frameworks → Kotlin**
2. Enable **K2 Mode** (if available)
3. Restart IDE

### Check Gradle Configuration

Ensure `kite-dsl/build.gradle.kts` has:

```kotlin
plugins {
    `kotlin-dsl` // This helps IntelliJ recognize it as a script provider
}
```

If not present, add it and reload Gradle.

## Known Issues

### Scripts Outside Source Directories

If your `.kite` folder is not marked as a source directory, IntelliJ won't provide full IDE support. Make sure it's
marked as **Sources Root**.

### Multiple Script Definitions

If you have multiple Kotlin script definitions in the project, they can conflict. Check **Settings → Languages &
Frameworks → Kotlin → Kotlin Scripting** and ensure only Kite scripts are registered for `.kite.kts` extension.

### Gradle Daemon Issues

Sometimes the Gradle daemon holds onto old class definitions:

```bash
./gradlew --stop
./gradlew :kite-dsl:clean :kite-dsl:build
```

Then reload Gradle in IntelliJ.

## Prevention

To avoid this issue in the future:

1. **Always rebuild kite-dsl** after making changes to script definitions
2. **Reload Gradle** after dependency changes
3. **Don't rename script classes** - IntelliJ caches them by name
4. **Use stable Kotlin versions** - experimental K2 features may have script bugs

## Technical Background

The `@KotlinScript` annotation tells the Kotlin compiler and IDE how to handle `.kite.kts` files:

```kotlin
@KotlinScript(
    displayName = "Kite Script",
    fileExtension = "kite.kts",
    compilationConfiguration = KiteScriptCompilationConfiguration::class,
    evaluationConfiguration = KiteScriptEvaluationConfiguration::class
)
abstract class KiteScript
```

IntelliJ discovers this by:

1. Scanning classpath for `@KotlinScript` annotations
2. Reading META-INF/kotlin/script/templates/* files
3. Caching the discovered definitions

When caches get stale, autocomplete breaks even though compilation works.
