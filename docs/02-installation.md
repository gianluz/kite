# Installation

Learn how to install and set up Kite in your projects.

## Quick Install

Add Kite to your Gradle project:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.gianluz.kite:kite-core:0.1.0-alpha9")
    implementation("com.gianluz.kite:kite-dsl:0.1.0-alpha9")
    implementation("com.gianluz.kite:kite-runtime:0.1.0-alpha9")
}
```

That's it! IntelliJ will automatically provide full IDE support for `.kite.kts` files.

## Installation Methods

### Method 1: Gradle Dependency (Recommended)

**For production use**, add Kite as a regular Gradle dependency.

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenCentral()
}

dependencies {
    // Kite dependencies
    implementation("com.gianluz.kite:kite-core:0.1.0-alpha9")
    implementation("com.gianluz.kite:kite-dsl:0.1.0-alpha9")
    implementation("com.gianluz.kite:kite-runtime:0.1.0-alpha9")
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

**For development** — contributing to Kite or testing unreleased changes locally.

```kotlin
// settings.gradle.kts
includeBuild("/path/to/kite")

// build.gradle.kts
dependencies {
    implementation("com.gianluz.kite:kite-core")
    implementation("com.gianluz.kite:kite-dsl")
    implementation("com.gianluz.kite:kite-runtime")
}
```

**Benefits**:

- ✅ Use local Kite version
- ✅ Make changes to Kite while using it
- ✅ No publishing required
- ✅ Automatic IDE support

**Use case**: Contributing to Kite or testing unreleased features.

### Method 3: CLI Binary (CI/CD and Local)

**For running Kite workflows** without embedding it as a library dependency.

> **Prerequisite:** All install methods except Docker require **Java 17+**.
> Check with `java -version`. Install from [adoptium.net](https://adoptium.net/) if needed.

#### Option A — Docker (recommended for CI, no Java needed)

```bash
# GitHub Container Registry
docker run --rm -v $(pwd):/workspace \
  ghcr.io/gianluz/kite:latest ride CI

# Docker Hub
docker run --rm -v $(pwd):/workspace \
  gianluz/kite:latest ride CI
```

Pass environment variables with `-e`:

```bash
docker run --rm \
  -v $(pwd):/workspace \
  -e DEPLOY_TOKEN=$DEPLOY_TOKEN \
  -e CI_COMMIT_TAG=v1.0.0 \
  ghcr.io/gianluz/kite:latest ride Deploy
```

#### Option B — Install script (macOS / Linux)

```bash
curl -sSL https://github.com/gianluz/kite/releases/latest/download/install.sh | bash
```

This downloads the latest release, installs to `~/.kite/`, and prints the `PATH` update needed.
Pin to a specific version:

```bash
curl -sSL https://github.com/gianluz/kite/releases/latest/download/install.sh \
  | KITE_VERSION=v0.1.0-alpha9 bash
```

#### Option C — Homebrew (macOS / Linux)

> **Status:** Homebrew tap coming soon. Track progress in [GitHub Issues](https://github.com/gianluz/kite/issues).

Once available:

```bash
brew install gianluz/kite/kite-cli
```

#### Option D — GitHub Releases (manual)

Download the archive for your platform from [GitHub Releases](https://github.com/gianluz/kite/releases):

```bash
# Download and extract
curl -LO https://github.com/gianluz/kite/releases/latest/download/kite-cli-0.1.0-alpha9.tar
tar -xf kite-cli-0.1.0-alpha9.tar

# Add to PATH
export PATH="$PWD/kite-cli-0.1.0-alpha9/bin:$PATH"
kite-cli --version
```

Windows: download the `.zip` file, extract, and add the `bin\` folder to your `PATH`.

#### Option E — Build from source

```bash
git clone https://github.com/gianluz/kite.git
cd kite
./gradlew :kite-cli:installDist
export PATH="$PWD/kite-cli/build/install/kite-cli/bin:$PATH"
kite-cli --version
```

#### Using in CI (GitHub Actions example)

```yaml
- name: Run Kite workflow (Docker — recommended)
  run: |
    docker run --rm \
      -v ${{ github.workspace }}:/workspace \
      -e GITHUB_TOKEN=${{ secrets.GITHUB_TOKEN }} \
      ghcr.io/gianluz/kite:latest ride CI

- name: Run Kite workflow (install script alternative)
  run: |
    curl -sSL https://github.com/gianluz/kite/releases/latest/download/install.sh | bash
    export PATH="$HOME/.kite/bin:$PATH"
    kite-cli ride CI
```

## Version Requirements

### System Requirements

- **Java**: 17 or higher (LTS recommended)
- **Kotlin**: 2.0+ (provided by Kite)
- **Gradle**: 9.2+ (for dependency management)

### Compatibility

| Kite Version  | Min Java | Min Kotlin | Min Gradle |
|---------------|----------|------------|------------|
| 0.1.0-alpha9  | 17       | 2.1        | 8.0        |

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
    implementation("com.gianluz.kite:kite-core:0.1.0-alpha9") // ← Update version
    implementation("com.gianluz.kite:kite-dsl:0.1.0-alpha9")
    implementation("com.gianluz.kite:kite-runtime:0.1.0-alpha9")
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
   +--- com.gianluz.kite:kite-core:0.1.0-alpha9
   +--- com.gianluz.kite:kite-dsl:0.1.0-alpha9
   +--- com.gianluz.kite:kite-runtime:0.1.0-alpha9
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
   Ensure you're using a published version (e.g., `0.1.0-alpha9`, not `0.1.0-SNAPSHOT`)

3. **Refresh Dependencies**:
   ```bash
   ./gradlew build --refresh-dependencies
   ```

## Uninstallation

To remove Kite from your project:

1. **Remove dependencies** from `build.gradle.kts`:
   ```kotlin
   // Remove these:
   // implementation("com.gianluz.kite:kite-core:...")
   // implementation("com.gianluz.kite:kite-dsl:...")
   // implementation("com.gianluz.kite:kite-runtime:...")
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
