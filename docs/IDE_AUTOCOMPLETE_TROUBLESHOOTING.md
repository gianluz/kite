# Quick Fix: IDE Autocomplete Not Working for .kite.kts Files

## Problem

You're seeing this error in IntelliJ IDEA when opening `test-dependencies.kite.kts`:

```
com/google/inject/Provider (java.lang.NoClassDefFoundError: com/google/inject/Provider)
```

And autocomplete is not working for:

- `segments { }`
- `segment(...) { }`
- Imports from `@file:DependsOn`

## Root Cause

The IDE's Kotlin scripting support needs Google Guice (and related dependencies) to load the script definition for files
that use `@file:DependsOn` and `@file:Repository` annotations. These dependencies come transitively through
`kotlin-scripting-dependencies-maven` at runtime, but aren't automatically available to the IDE's script loader at
compile time.

## Solution (ALREADY APPLIED)

The fix has been applied to `kite-dsl/build.gradle.kts` by adding these `compileOnly` dependencies:

```kotlin
// IDE Support for @DependsOn and @Repository
compileOnly("com.google.inject:guice:4.2.2")
compileOnly("org.eclipse.sisu:org.eclipse.sisu.inject:0.3.5")
compileOnly("javax.inject:javax.inject:1")
```

## Steps to Apply the Fix in Your IDE

### 1. Reload Gradle Project

In IntelliJ IDEA:

1. Open the Gradle tool window: **View → Tool Windows → Gradle**
2. Click the **"Reload All Gradle Projects"** button (circular arrows icon)
3. Wait for the sync to complete

### 2. Invalidate Caches and Restart

1. Go to **File → Invalidate Caches...**
2. Select **"Invalidate and Restart"**
3. Click **"Invalidate and Restart"** button
4. Wait for IntelliJ to restart

### 3. Reopen the File

1. Close `test-dependencies.kite.kts` if it's open
2. Reopen the file
3. Autocomplete should now work!

## Verification

After applying the fix, try these in your `.kite.kts` file:

1. Type `segments` and press **Ctrl+Space** (or **Cmd+Space** on Mac)
    - You should see autocomplete suggestions

2. Inside a segment block, type `exec` and press **Ctrl+Space**
    - You should see: `exec(command: String, vararg args: String)`

3. The error `com/google/inject/Provider` should be gone

## If the Error Still Shows (Red at Top of File)

Even if autocomplete is working, you might still see the error in red at the top of the file:

```
com/google/inject/Provider (java.lang.NoClassDefFoundError: com/google/inject/Provider)
```

This means IntelliJ's Kotlin script cache hasn't been fully cleared. Try these steps **in order**:

### Step 1: Close the File

1. Close `test-dependencies.kite.kts`
2. Wait a few seconds

### Step 2: Clean Build the kite-dsl Module

```bash
./gradlew :kite-dsl:clean :kite-dsl:compileKotlin
```

### Step 3: Delete IntelliJ's Kotlin Script Cache

```bash
# macOS/Linux
rm -rf ~/.cache/JetBrains/IntelliJIdea*/kotlin-compiler-cache
rm -rf ~/Library/Caches/JetBrains/IntelliJIdea*/kotlin/script-cache

# If the above doesn't work, try finding the cache:
find ~ -type d -name "kotlin-compiler-cache" -o -name "script-cache" 2>/dev/null
```

On **macOS**, the caches are typically in:

- `~/Library/Caches/JetBrains/IntelliJIdea<version>/kotlin/script-cache`
- `~/Library/Caches/JetBrains/IntelliJIdea<version>/kotlin/script-dependencies-cache`

### Step 4: Invalidate Caches (Again)

1. **File → Invalidate Caches...**
2. Select **ALL** options:
    - ✅ Clear file system cache
    - ✅ Clear downloaded shared indexes
    - ✅ Clear VCS Log caches
    - ✅ Clear workspace indexes
3. Click **"Invalidate and Restart"**

### Step 5: Reload Gradle (After Restart)

1. After IntelliJ restarts, open Gradle tool window
2. Click **"Reload All Gradle Projects"**
3. Wait for sync to complete

### Step 6: Reopen the File

1. Open `test-dependencies.kite.kts`
2. The error should be gone!

### Alternative: Use IntelliJ's Built-in Action

If the error persists, try this IntelliJ action:

1. Open the file with the error
2. Press **Ctrl+Shift+A** (or **Cmd+Shift+A** on Mac) to open "Find Action"
3. Type: **"Clear Kotlin Scripting Dependencies"**
4. Run that action if available
5. Then: **"Reload Kotlin Scripting Configuration"**
6. Close and reopen the file

## Note About @file:DependsOn Imports

Even after this fix, you might still see "unresolved reference" warnings for imports from `@file:DependsOn` annotations
like:

```kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.0")

import com.google.gson.Gson  // May show as unresolved in IDE
```

**This is expected behavior** because:

- Dependencies declared with `@file:DependsOn` are resolved at **runtime**, not at IDE analysis time
- Your code will still work correctly when executed with `kite`
- The IDE just doesn't know about these dependencies yet

### Workarounds for @file:DependsOn IDE Support

Choose one of these options:

**Option 1: Run Once, Then Reload**

1. Run the script once: `kite ride "Test Dependencies"`
2. Dependencies are cached in `~/.m2/repository`
3. Reload Gradle project in IntelliJ
4. Autocomplete *might* work (not guaranteed)

**Option 2: Temporary Project Dependency**

```kotlin
// In kite-dsl/build.gradle.kts
dependencies {
    // ... existing dependencies ...
    
    // Temporary: for IDE support only
    implementation("com.google.code.gson:gson:2.10.0")
}
```

- Add dependency temporarily for IDE autocomplete
- Remove before committing (it's not needed at runtime)

**Option 3: Accept the Warning** ⭐ **Recommended**

- Just accept that the IDE shows unresolved references
- The code will work perfectly at runtime
- This is a known limitation of `@file:DependsOn` with IDE support

## Still Not Working?

If autocomplete still doesn't work after the above steps:

1. **Check Kotlin Plugin Version**:
    - Settings → Plugins → Kotlin
    - Update to version 2.0.21 or later

2. **Clean Build**:
   ```bash
   ./gradlew clean :kite-dsl:build
   ```

3. **Check IDE Logs**:
    - Help → Show Log in Finder (macOS) / Explorer (Windows)
    - Look for script-related errors in `idea.log`

4. **Report Issue**:
    - If still not working, open an issue with:
        - IntelliJ IDEA version
        - Kotlin plugin version
        - Relevant sections from `idea.log`

## Summary

✅ **Fix Applied**: `compileOnly` dependencies added to `kite-dsl/build.gradle.kts`  
✅ **Action Required**: Reload Gradle + Invalidate Caches + Restart IDE  
✅ **Expected Result**: Autocomplete for Kite DSL (`segments`, `segment`, `execute`, etc.)  
⚠️ **Known Limitation**: `@file:DependsOn` imports may still show as unresolved (this is normal)

---

For more details, see [IDE_SETUP.md](IDE_SETUP.md)
