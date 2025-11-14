# IntelliJ IDEA Setup for Kite Development

This guide explains how to get full IDE support (autocomplete, syntax highlighting) when **developing Kite itself**.

**Looking to use Kite in your own project?** See **[docs/EXTERNAL_PROJECT_SETUP.md](docs/EXTERNAL_PROJECT_SETUP.md)**
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

## How It Works

Kite uses Kotlin's [scripting support](https://github.com/Kotlin/KEEP/blob/master/proposals/scripting-support.md) to
provide full IDE integration:

1. **Script Definition**: `kite-dsl/src/main/kotlin/io/kite/dsl/KiteScript.kt` defines the script type with
   `@KotlinScript` annotation

2. **Compilation Configuration**: `KiteScriptCompilationConfiguration` specifies:
    - Implicit imports (Kite DSL classes, Kotlin stdlib)
    - Dependencies (from current classpath)
    - IDE support settings

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

---

## For External Projects

If you want to use Kite in your own projects (not developing Kite itself), see the comprehensive guide:

**[docs/EXTERNAL_PROJECT_SETUP.md](docs/EXTERNAL_PROJECT_SETUP.md)**

This guide covers:

- Adding Kite as a dependency
- Multiple segment files
- External dependencies and @DependsOn
- Helper functions
- Complete working examples

---

**Happy coding with Kite!** 
