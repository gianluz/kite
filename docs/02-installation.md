# Installation

Learn how to install and set up Kite in your projects.

## Quick Install

Add Kite to your Gradle project:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral() // Once published
}

dependencies {
    implementation("io.kite:kite-core:0.1.0-alpha")
    implementation("io.kite:kite-dsl:0.1.0-alpha")
    implementation("io.kite:kite-runtime:0.1.0-alpha")
}
```

That's it! IntelliJ will automatically provide full IDE support for `.kite.kts` files.

## Installation Methods

### Method 1: Gradle Dependency (Recommended)

**For production use**, add Kite as a regular Gradle dependency.

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.21"  // Kotlin plugin
}

repositories {
    mavenCentral()
}

dependencies {
    // Kite dependencies
    implementation("io.kite:kite-core:0.1.0-alpha")
    implementation("io.kite:kite-dsl:0.1.0-alpha")
    implementation("io.kite:kite-runtime:0.1.0-alpha")
}
```

**Benefits**:

- ✅ Simple setup
- ✅ Automatic IDE support
- ✅ Version management via Gradle
- ✅ Works with Gradle dependency resolution

**Reload Gradle** after adding dependencies:

```bash
# Command line
./gradlew build

# Or in IntelliJ: View → Tool Windows → Gradle → Reload (↻)
```

### Method 2: Composite Build (Development)

**For development** or using unreleased versions of Kite.

```kotlin
// settings.gradle.kts
includeBuild("/path/to/kite")

// build.gradle.kts
dependencies {
    implementation("io.kite:kite-core")
    implementation("io.kite:kite-dsl")
    implementation("io.kite:kite-runtime")
}
```

**Benefits**:

- ✅ Use local Kite version
- ✅ Make changes to Kite while using it
- ✅ No publishing required
- ✅ Automatic IDE support

**Use case**: Contributing to Kite or testing unreleased features.

### Method 3: CLI Binary (CI/CD)

**For CI/CD environments**, use the Kite CLI binary.

Download or build the CLI:

```bash
# Clone Kite
git clone https://github.com/yourusername/kite.git
cd kite

# Build CLI
./gradlew :kite-cli:installDist

# Use it
kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

Or in your project:

```bash
# Add as submodule
git submodule add https://github.com/yourusername/kite.git tools/kite

# Build and use
cd tools/kite
./gradlew :kite-cli:installDist
cd ../..
tools/kite/kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

## Version Requirements

### System Requirements

- **Java**: 17 or higher (LTS recommended)
- **Kotlin**: 2.0+ (provided by Kite)
- **Gradle**: 9.2+ (for dependency management)

### Compatibility

| Kite Version | Min Java | Min Kotlin | Min Gradle |
|--------------|----------|------------|------------|
| 0.1.0-alpha  | 17       | 2.0        | 9.2        |

## Verify Installation

After installing Kite, verify it works:

### 1. Create a Test Segment

Create `.kite/segments/hello.kite.kts`:

```kotlin
segments {
    segment("hello") {
        description = "Hello Kite!"
        execute {
            println("👋 Hello from Kite!")
        }
    }
}
```

### 2. Check IDE Support

Open the file in IntelliJ IDEA - you should see:

- ✅ Syntax highlighting
- ✅ Autocomplete for `segments`, `segment`, `execute`
- ✅ No errors

### 3. Run It

```bash
# Build Kite CLI
./gradlew :kite-cli:installDist

# List segments
kite-cli/build/install/kite-cli/bin/kite-cli segments

# Run the segment
kite-cli/build/install/kite-cli/bin/kite-cli run hello
```

Expected output:

```
👋 Hello from Kite!
✅ Segment 'hello' completed successfully
```

## IDE Setup

### IntelliJ IDEA

Kite works out of the box with IntelliJ IDEA. After adding dependencies:

1. **Reload Gradle Project**:
    - View → Tool Windows → Gradle
    - Click "Reload All Gradle Projects" (↻ button)

2. **Verify Script Support**:
    - Open any `.kite.kts` file
    - Type `segments {` and press Ctrl+Space
    - You should see autocomplete suggestions

That's it! No additional configuration needed.

### VS Code

VS Code support requires:

- Kotlin Language Server extension
- Gradle integration

**Note**: IntelliJ IDEA provides the best experience for Kite development.

## Project Structure

After installation, organize your Kite files:

```
your-project/
├── .kite/
│   ├── segments/           # Define reusable segments here
│   │   ├── build.kite.kts
│   │   ├── test.kite.kts
│   │   └── deploy.kite.kts
│   └── rides/              # Define workflows here
│       ├── ci.kite.kts
│       └── release.kite.kts
├── build.gradle.kts        # Add Kite dependencies here
└── settings.gradle.kts
```

Kite automatically discovers all `.kite.kts` files in:

- `.kite/segments/` (recursively)
- `.kite/rides/` (recursively)

## Updating Kite

### Update Gradle Dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.kite:kite-core:0.2.0") // ← Update version
    implementation("io.kite:kite-dsl:0.2.0")
    implementation("io.kite:kite-runtime:0.2.0")
}
```

Then reload Gradle:

```bash
./gradlew build --refresh-dependencies
```

### Update Composite Build

```bash
cd /path/to/kite
git pull origin main
cd /path/to/your-project
./gradlew build
```

## Troubleshooting

### No Autocomplete in `.kite.kts` Files

**Problem**: IntelliJ doesn't show autocomplete for Kite DSL.

**Solutions**:

1. **Reload Gradle Project**:
   ```
   View → Tool Windows → Gradle → Reload (↻)
   ```

2. **Invalidate Caches**:
   ```
   File → Invalidate Caches → Invalidate and Restart
   ```

3. **Check Dependencies**:
   ```bash
   ./gradlew dependencies | grep kite
   ```

   Should show:
   ```
   +--- io.kite:kite-core:0.1.0-alpha
   +--- io.kite:kite-dsl:0.1.0-alpha
   +--- io.kite:kite-runtime:0.1.0-alpha
   ```

4. **Verify Script Definition**:
    - Open `.kite.kts` file
    - Right-click → Kotlin → "Configure Kotlin Script"
    - Should list `io.kite.dsl.KiteScript`

### "Unresolved Reference: segments"

**Problem**: Kite DSL functions not found.

**Causes**:

- Wrong file extension (must be `.kite.kts`)
- Kite dependencies not added
- Gradle not reloaded

**Solutions**:

1. Rename file to end with `.kite.kts` (not `.kts`)
2. Add Kite dependencies to `build.gradle.kts`
3. Reload Gradle project

### "Script Definition Not Found"

**Problem**: IntelliJ can't find Kite script definitions.

**Solutions**:

1. **Check META-INF**:
   ```bash
   jar tf ~/.gradle/caches/modules-2/files-2.1/io.kite/kite-dsl/*/kite-dsl-*.jar | grep META-INF
   ```

   Should include:
   ```
   META-INF/kotlin/script/templates/io.kite.dsl.KiteScript
   ```

2. **Enable Script Support**:
   ```
   Settings → Languages & Frameworks → Kotlin → Kotlin Scripting
   → Enable "Enable script definitions"
   ```

3. **Clear Caches**:
   ```
   File → Invalidate Caches → Invalidate and Restart
   ```

### Dependency Resolution Errors

**Problem**: Gradle can't find Kite dependencies.

**Solutions**:

1. **Check Repository**:
   ```kotlin
   repositories {
       mavenCentral() // Must be present
   }
   ```

2. **Check Version**:
   Ensure you're using a published version (e.g., `0.1.0-alpha`, not `0.1.0-SNAPSHOT`)

3. **Refresh Dependencies**:
   ```bash
   ./gradlew build --refresh-dependencies
   ```

## Uninstallation

To remove Kite from your project:

1. **Remove dependencies** from `build.gradle.kts`:
   ```kotlin
   // Remove these:
   // implementation("io.kite:kite-core:...")
   // implementation("io.kite:kite-dsl:...")
   // implementation("io.kite:kite-runtime:...")
   ```

2. **Delete Kite files**:
   ```bash
   rm -rf .kite/
   ```

3. **Reload Gradle**:
   ```bash
   ./gradlew build
   ```

## Next Steps

Now that Kite is installed:

- **[Getting Started](01-getting-started.md)** - Create your first workflow
- **[Core Concepts](03-core-concepts.md)** - Learn rides, segments, and flows
- **[CLI Reference](12-cli-reference.md)** - Master the command-line interface
- **[Troubleshooting](99-troubleshooting.md)** - Fix common issues

---

**Installation complete!** 🎉 Ready to define workflows as code.
