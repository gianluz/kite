# IntelliJ IDEA Setup for Kite Development

This guide explains how to get full IDE support (autocomplete, syntax highlighting) when **developing Kite itself**.

**Looking to use Kite in your own project?** See **[EXTERNAL_PROJECT_SETUP.md](EXTERNAL_PROJECT_SETUP.md)**
for comprehensive instructions.

---

## For Kite Development

If you're working on the Kite codebase itself, IntelliJ IDEA should automatically recognize `.kite.kts` files once you
open the project, thanks to the script definition in the `kite-dsl` module.

### Setup Steps

1. **Open Project**:
   ```bash
   File → Open → select the kite directory
   ```

2. **Wait for Gradle Sync**:
    - IntelliJ will automatically sync Gradle dependencies
    - This can take 1-2 minutes on the first load

3. **Verify Script Support**:
    - Open `.kite/segments/example.kite.kts`
    - You should see full autocomplete for `segments { }`

### If Autocomplete Doesn't Work

1. **Reimport Gradle Project**:
    - View → Tool Windows → Gradle
    - Click "Reload All Gradle Projects" (circular arrows icon)

2. **Build the project**:
   ```bash
   ./gradlew build
   ```

3. **Invalidate Caches**:
    - File → Invalidate Caches...
    - Select "Invalidate and Restart"

4. **Check Script Configuration**:
    - Open any `.kite.kts` file
    - Right-click → Kotlin → "Configure Kotlin Script"
    - Verify `io.kite.dsl.KiteScript` is listed

## Common Issues

### Error: `NoClassDefFoundError: com/google/inject/Provider`

**Symptom**: You see this error in the IDE when opening `.kite.kts` files, especially files that use `@file:DependsOn`:

```
com/google/inject/Provider (java.lang.NoClassDefFoundError: com/google/inject/Provider)
```

**Cause**: This error occurs because IntelliJ's Kotlin scripting support tries to load the script definition for
`.kite.kts` files, which includes support for `@DependsOn` and `@Repository` annotations. These annotations require
Maven dependency resolution, which transitively depends on Google Guice. However, these transitive dependencies are not
automatically available to the IDE's script definition loader.

**Solution**: The issue has been fixed by adding explicit `compileOnly` dependencies in `kite-dsl/build.gradle.kts`:

```kotlin
// IDE Support for @DependsOn and @Repository
compileOnly("com.google.inject:guice:4.2.2")
compileOnly("org.eclipse.sisu:org.eclipse.sisu.inject:0.3.5")
compileOnly("javax.inject:javax.inject:1")
```

**To apply the fix**:

1. Make sure you have the latest `kite-dsl/build.gradle.kts`
2. Reload Gradle project:
    - View → Tool Windows → Gradle
    - Click "Reload All Gradle Projects" (circular arrows icon)
3. Invalidate caches and restart:
    - File → Invalidate Caches...
    - Select "Invalidate and Restart"
4. Reopen your `.kite.kts` file

**Why this works**: The `compileOnly` dependencies ensure that Guice and related classes are available on the compile
classpath for IDE script definition loading, but they don't bloat the runtime classpath since they're already included
transitively through `kotlin-scripting-dependencies-maven`.

### Autocomplete Still Not Working?

If after applying the above fix you still don't have autocomplete:

1. **Check Kotlin Plugin Version**:
    - Settings → Plugins → Kotlin
    - Make sure you're using Kotlin plugin version 2.0.21 or later

2. **Verify Script Definition is Loaded**:
   ```bash
   ./gradlew :kite-dsl:build
   ```
   Then reload Gradle in IntelliJ

3. **Check IDE Logs**:
    - Help → Show Log in Finder (macOS) / Show Log in Explorer (Windows)
    - Look for any script-related errors in `idea.log`

4. **Try a Clean Build**:
   ```bash
   ./gradlew clean build
   ```
   Then restart IntelliJ

## How It Works

Kite uses Kotlin's [scripting support](https://github.com/Kotlin/KEEP/blob/master/proposals/scripting-support.md) to
provide full IDE integration:

1. **Script Definition**: `kite-dsl/src/main/kotlin/io/kite/dsl/KiteScript.kt` defines the script type with
   `@KotlinScript` annotation

2. **Compilation Configuration**: `KiteScriptCompilationConfiguration` specifies:
    - Implicit imports (Kite DSL classes, Kotlin stdlib)
    - Dependencies (from current classpath)
    - IDE support settings
   - Maven dependency resolution via `@DependsOn` and `@Repository`

3. **Template Registration**: `kite-dsl/src/main/resources/META-INF/kotlin/script/templates/io.kite.dsl.KiteScript`
   registers the script template with IntelliJ

4. **Evaluation Configuration**: `KiteScriptEvaluationConfiguration` defines how scripts are executed

## What You Get

In `.kite.kts` files, IntelliJ provides:

✅ **Autocomplete** for all Kite APIs  
✅ **Syntax highlighting** with Kotlin syntax  
✅ **Error checking** and inline warnings  
✅ **Refactoring support** (rename, extract, etc.)  
✅ **Navigation** (go to definition, find usages)  
✅ **Documentation** on hover  
✅ **Support for `@file:DependsOn` and `@file:Repository`**

## Testing Autocomplete

Try this in any `.kite.kts` file:

```kotlin
segments {
    segment("test") {
        // Type "exec" and press Ctrl+Space
        // You should see: exec(command: String, vararg args: String)
        
        // Type "shell" and press Ctrl+Space  
        // You should see: shell(command: String)
        
        // Type "timeout" and press Ctrl+Space
        // You should see: timeout: Duration
    }
}
```

## Troubleshooting

### "Unresolved reference: segments"

**Cause**: Script definition not loaded.

**Fix**:

1. Build the `kite-dsl` module: `./gradlew :kite-dsl:build`
2. Reimport Gradle project
3. Invalidate caches if needed

### No autocomplete for ExecutionContext methods

**Cause**: Type inference issue in execute block.

**Fix**: The IDE should automatically infer the receiver type. If not, try:
```kotlin
execute {
    this.exec("command") // explicit 'this' helps IDE
}
```

### "Script definition is not found"

**Cause**: META-INF template file missing.

**Fix**:

1. Verify `kite-dsl/src/main/resources/META-INF/kotlin/script/templates/io.kite.dsl.KiteScript` exists
2. Rebuild `kite-dsl` module
3. Invalidate caches

### Unresolved references for `@file:DependsOn` imports

**Symptom**: After adding `@file:DependsOn("com.google.code.gson:gson:2.10.1")`, the import still shows as unresolved in
the IDE.

**Cause**: Dependencies declared with `@DependsOn` are resolved at runtime, not at IDE analysis time.

**Workaround Options**:

1. **Run the script once**: After the first execution, dependencies are cached in `~/.m2/repository`, then reload Gradle
2. **Temporarily add to project**: Add the dependency to `kite-dsl/build.gradle.kts` as `implementation` for IDE support
   during development, remove before committing
3. **Accept the warning**: The code will work at runtime even if the IDE shows warnings

---

## For External Projects

If you want to use Kite in your own projects (not developing Kite itself), see the comprehensive guide:

**[EXTERNAL_PROJECT_SETUP.md](EXTERNAL_PROJECT_SETUP.md)**

This guide covers:

- Adding Kite as a dependency
- Multiple segment files
- External dependencies and @DependsOn
- Helper functions
- Complete working examples

---

**Happy coding with Kite!** 
