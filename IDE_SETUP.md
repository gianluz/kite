# IntelliJ IDEA Setup for Kite

This guide explains how to get full IDE support (autocomplete, syntax highlighting, type checking) for `.kite.kts` files
in IntelliJ IDEA.

## Automatic Setup (Recommended)

Kite includes script definitions that IntelliJ IDEA should recognize automatically. After importing the project:

1. **Import the project as a Gradle project**
    - File → Open → Select the `kite` directory
    - Let Gradle sync complete

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Invalidate caches and restart** (if needed)
    - File → Invalidate Caches → Invalidate and Restart

4. **Test it works**
    - Open a `.kite.kts` file (e.g., `.kite/segments/example.kite.kts`)
    - You should see:
        - ✅ Syntax highlighting
        - ✅ Autocomplete for `segments`, `segment`, `ride`, etc.
        - ✅ Type checking for DSL functions
        - ✅ Navigation to source (Ctrl+Click / Cmd+Click)

## Manual Setup (If Automatic Doesn't Work)

If IntelliJ doesn't automatically recognize `.kite.kts` files:

### Option 1: Associate with Kotlin Script

1. Right-click a `.kite.kts` file
2. Select **Associate with File Type...**
3. Choose **Kotlin Script**

### Option 2: Configure Kotlin Script Definition

1. Go to **Settings/Preferences** → **Languages & Frameworks** → **Kotlin** → **Scripting**
2. Check if `io.kite.dsl.KiteScript` is listed
3. If not, click **+** and add:
    - **Script template:** `io.kite.dsl.KiteScript`
    - **File extension pattern:** `*.kite.kts`

### Option 3: Rebuild and Reimport

1. Close IntelliJ
2. Delete `.idea` directory
3. Delete `.gradle` directory
4. Reopen project and reimport

## What Should Work

Once configured, you should have full IDE support in `.kite.kts` files:

### ✅ Autocomplete

```kotlin
segments {
    segment("build") {  // ← Autocomplete here
        description = ""  // ← And here
        timeout = 5.minutes  // ← And here
        execute {
            exec("./gradlew", "build")  // ← And here
        }
    }
}
```

### ✅ Type Checking

```kotlin
segments {
    segment("test") {
        timeout = "invalid"  // ← Should show error
        maxRetries = -1      // ← Should show error
    }
}
```

### ✅ Navigation

- **Ctrl+Click** (Cmd+Click on Mac) on any DSL function to jump to its definition
- **Ctrl+Space** to trigger autocomplete
- **Ctrl+Q** (Cmd+J on Mac) to view quick documentation

### ✅ Implicit Imports

These are automatically available (no import needed):

- `io.kite.core.*` - Segment, Ride, ExecutionContext, etc.
- `io.kite.dsl.*` - segments, ride, segment, etc.
- `kotlin.time.Duration` - For timeouts
- `.seconds`, `.minutes`, `.hours` - Duration extensions

## Troubleshooting

### Problem: No autocomplete in .kite.kts files

**Solution:**

1. Make sure you've run `./gradlew build` at least once
2. Try **File → Invalidate Caches → Invalidate and Restart**
3. Check **Settings → Kotlin → Scripting** for script definitions

### Problem: "Unresolved reference" errors

**Solution:**

1. Make sure the `kite-dsl` module is built
2. Check that `kotlin-scripting-jvm` is in your dependencies
3. Reimport Gradle project

### Problem: IntelliJ treats .kite.kts as plain text

**Solution:**

1. Right-click the file → **Associate with File Type** → **Kotlin Script**
2. Or add `*.kite.kts` pattern in **Settings → Editor → File Types → Kotlin Script**

## Testing Your Setup

Create a test file `.kite/test.kite.kts`:

```kotlin
segments {
    segment("test") {
        description = "Test segment"
        timeout = 5.minutes
        dependsOn("other")
        
        execute {
            println("Hello from Kite!")
            exec("echo", "test")
        }
    }
}
```

You should see:

- ✅ `segments` is highlighted and autocompletes
- ✅ `segment()` has parameter hints
- ✅ `description`, `timeout`, `dependsOn`, `execute` autocomplete
- ✅ `println` and `exec` are recognized
- ✅ No red underlines (errors)

## Technical Details

Kite uses Kotlin's scripting API to provide IDE support:

- **Script Definition:** `io.kite.dsl.KiteScript` (annotated with `@KotlinScript`)
- **File Extension:** `*.kite.kts`
- **Configuration:** `kite-dsl/src/main/kotlin/io/kite/dsl/KiteScriptConfiguration.kt`
- **Template File:** `kite-dsl/src/main/resources/META-INF/kotlin/script/templates/io.kite.dsl.KiteScript`

## Need Help?

If you're still having issues:

1. Check the Kotlin plugin version (should be 2.0.21 or newer)
2. Check the Gradle JVM version (should be Java 17)
3. Try the manual setup steps above
4. Open an issue on GitHub with your IntelliJ version and error details

---

**Enjoy coding with full IDE support! 🚀**
