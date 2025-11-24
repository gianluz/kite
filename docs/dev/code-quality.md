# Code Quality Checks

Kite enforces code quality using **ktlint** (code style) and **detekt** (static analysis).

## Quick Reference

```bash
# Run all quality checks locally
./gradlew ktlintMainSourceSetCheck ktlintKotlinScriptCheck detekt

# Auto-fix ktlint violations
./gradlew ktlintFormat

# Run via Kite (recommended)
kite-cli/build/install/kite-cli/bin/kite-cli run ktlint
kite-cli/build/install/kite-cli/bin/kite-cli run detekt
kite-cli/build/install/kite-cli/bin/kite-cli run quality-checks  # Runs both
```

---

## ktlint - Code Style

**ktlint** enforces Kotlin coding conventions and style guide.

### Running ktlint

```bash
# Check main sources only
./gradlew ktlintMainSourceSetCheck ktlintKotlinScriptCheck

# Check all sources (including tests)
./gradlew ktlintCheck

# Auto-fix violations
./gradlew ktlintFormat
```

### Common Violations

| Issue | Fix |
|-------|-----|
| Wildcard imports | Replace `import io.kite.core.*` with explicit imports |
| Import ordering | Run `ktlintFormat` to auto-fix |
| Trailing whitespace | Run `ktlintFormat` to auto-fix |
| Final newline missing | Run `ktlintFormat` to auto-fix |
| Max line length (120 chars) | Break long lines |

### Configuration

ktlint is configured in:

- `.editorconfig` - IDE and ktlint shared config
- `build.gradle.kts` - ktlint plugin settings

### IDE Integration

**IntelliJ IDEA:**

1. Install ktlint plugin: `Settings → Plugins → Marketplace → "ktlint"`
2. Enable: `Settings → Editor → Inspections → ktlint`
3. Auto-format on save: `Settings → Tools → Actions on Save → Reformat code`

---

## detekt - Static Analysis

**detekt** performs static code analysis to find:

- Code smells
- Complexity issues
- Potential bugs
- Performance issues

### Running detekt

```bash
# Run detekt
./gradlew detekt

# Generate reports
./gradlew detekt
# Reports in: */build/reports/detekt/
```

### Configuration

detekt is configured in `detekt.yml`:

```yaml
complexity:
  LongMethod:
    threshold: 60  # Max method length
  ComplexMethod:
    threshold: 15  # Max cyclomatic complexity

style:
  MagicNumber:
    active: false  # Disabled - too noisy
  WildcardImport:
    active: false  # ktlint handles this
```

### Common Issues

| Issue | Description | Solution |
|-------|-------------|----------|
| `LongMethod` | Method exceeds 60 lines | Extract smaller functions |
| `ComplexMethod` | Too many branches | Simplify logic |
| `NestedBlockDepth` | Too deeply nested | Early returns, extract functions |
| `TooManyFunctions` | Too many functions in file | Split into multiple files |

### Suppressing Violations

For intentional violations, use `@Suppress`:

```kotlin
@Suppress("LongMethod", "ComplexMethod")
fun complexLegacyCode() {
    // Complex logic that can't be easily refactored
}
```

---

## CI/CD Integration

### GitHub Actions

Quality checks run automatically on:

#### 1. **Code Quality Workflow** (`.github/workflows/code-quality.yml`)

- Triggers: Push to `main`, Pull Requests, Manual
- Runs: ktlint + detekt
- Reports: Artifacts uploaded, PR comments posted

#### 2. **PR Validation** (`.github/workflows/pr.yml`)

- Triggers: Pull Requests
- Runs: Full MR ride (including quality checks)
- Blocks: PR merge if checks fail

#### 3. **CI Build** (`.github/workflows/ci.yml`)

- Triggers: Push to `main`
- Runs: Full CI ride (including quality checks)

### Local Pre-commit Hook (Optional)

Create `.git/hooks/pre-commit`:

```bash
#!/bin/sh
echo "🔍 Running code quality checks..."

# Run ktlint and detekt
./gradlew ktlintMainSourceSetCheck ktlintKotlinScriptCheck detekt

if [ $? -ne 0 ]; then
  echo "❌ Code quality checks failed. Fix violations before committing."
  echo "💡 Run './gradlew ktlintFormat' to auto-fix style issues."
  exit 1
fi

echo "✅ Code quality checks passed!"
```

Make executable:

```bash
chmod +x .git/hooks/pre-commit
```

---

## Kite Segments

Quality checks are defined in `.kite/segments/quality.kite.kts`:

```kotlin
segments {
    segment("ktlint") {
        description = "Run ktlint code style checks on main sources"
        dependsOn("compile")
        execute {
            exec("./gradlew", "ktlintMainSourceSetCheck", "ktlintKotlinScriptCheck")
        }
    }

    segment("detekt") {
        description = "Run detekt static analysis"
        dependsOn("compile")
        execute {
            exec("./gradlew", "detekt")
        }
    }

    segment("quality-checks") {
        description = "Run all code quality checks"
        dependsOn("ktlint", "detekt")
        execute {
            println("✅ All quality checks passed!")
        }
    }
}
```

### Using in Rides

Quality checks are integrated into CI/MR rides:

**`.kite/rides/mr.kite.kts`:**

```kotlin
ride {
    name = "MR"
    flow {
        segment("clean")
        segment("compile")
        
        parallel {
            segment("ktlint")   // ← Quality checks
            segment("detekt")   // ← Quality checks
            segment("test-core")
            // ... other tests
        }
        
        segment("build")
    }
}
```

---

## Best Practices

### 1. Run Checks Before Committing

```bash
# Quick check
./gradlew ktlintMainSourceSetCheck detekt

# Or use Kite
kite-cli/build/install/kite-cli/bin/kite-cli run quality-checks
```

### 2. Auto-fix When Possible

```bash
# Let ktlint fix most style issues
./gradlew ktlintFormat
```

### 3. Keep Code Clean

- **Short methods**: < 60 lines
- **Low complexity**: < 15 cyclomatic complexity
- **Explicit imports**: No wildcards
- **No secrets in code**: Use environment variables

### 4. Review Reports

When checks fail, review the reports:

```bash
# ktlint report
cat */build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.txt

# detekt report (HTML)
open build/reports/detekt/detekt.html
```

---

## Troubleshooting

### "Wildcard import" error

```
Error: Wildcard import (cannot be auto-corrected)
```

**Fix:** Replace with explicit imports:

```kotlin
// ❌ Bad
import io.kite.core.*

// ✅ Good
import io.kite.core.Segment
import io.kite.core.Ride
```

### "LongMethod" error

```
Error: Method exceeds 60 lines
```

**Fix:** Extract smaller functions:

```kotlin
// ❌ Bad
fun processData() {
    // 100 lines of code
}

// ✅ Good
fun processData() {
    validateInput()
    transformData()
    saveResults()
}
```

### "Too many functions in class"

```
Error: File contains 20 functions, limit is 15
```

**Fix:** Split into multiple files or use companion object:

```kotlin
// FileOperations.kt → Split into:
// - FileOperations.kt (core functions)
// - FileOperationsRead.kt (read operations)
// - FileOperationsWrite.kt (write operations)
```

---

## Configuration Files

| File | Purpose |
|------|---------|
| `.editorconfig` | Shared IDE/ktlint config (indentation, line endings) |
| `detekt.yml` | detekt rules and thresholds |
| `build.gradle.kts` | ktlint plugin configuration |
| `.kite/segments/quality.kite.kts` | Quality check segment definitions |

---

## Further Reading

- [ktlint Documentation](https://pinterest.github.io/ktlint/)
- [detekt Documentation](https://detekt.dev/)
- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Kite Segments Guide](../README.md#segments)
