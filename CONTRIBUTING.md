# Contributing to Kite

Thank you for your interest in contributing to Kite! We're excited to have you here.

---

## 📚 Documentation

**Before contributing, please read:**

- **[Contributing Guide](docs/dev/contributing.md)** - Complete development setup and guidelines
- **[Code Quality Standards](docs/dev/code-quality.md)** - Linting and quality requirements
- **[Architecture Specs](docs/specs/)** - System design and specifications

This document provides a quick overview. See the full guides above for complete details.

---

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Git
- An IDE with Kotlin support (IntelliJ IDEA recommended)

### Setup

```bash
# 1. Clone the repository
git clone https://github.com/gianluz/kite.git
cd kite

# 2. Build the project
./gradlew build

# 3. Run tests
./gradlew test

# 4. Install git hooks (recommended)
./scripts/install-git-hooks.sh
```

The git hooks automatically run quality checks before pushing, helping catch issues early.

---

## 🏗️ Development Workflow

### 1. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-bug-fix
```

### 2. Make Changes

- Write clean, idiomatic Kotlin code
- Add tests for new functionality
- Update documentation if needed
- Follow our [code quality standards](docs/dev/code-quality.md)

### 3. Run Quality Checks

```bash
# Run all checks (done automatically by git hooks)
kite-cli/build/install/kite-cli/bin/kite-cli run quality-checks

# Or run individually
./gradlew ktlintFormat  # Auto-fix formatting
./gradlew ktlintCheck   # Check style
./gradlew detekt        # Static analysis
./gradlew test          # Run tests
```

### 4. Commit

Use [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git commit -m "feat: add new feature"
git commit -m "fix: resolve issue with X"
git commit -m "docs: update installation guide"
```

**Commit types:**

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Test changes
- `chore:` - Build/tooling changes

### 5. Push and Create PR

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.

---

## 📋 Pull Request Guidelines

### PR Checklist

Before submitting, ensure:

- [ ] Code builds successfully
- [ ] All tests pass
- [ ] Quality checks pass (ktlint, detekt)
- [ ] New features have tests
- [ ] Documentation is updated
- [ ] Commit messages follow conventions
- [ ] PR description explains changes

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
How was this tested?

## Related Issues
Fixes #123
```

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :kite-core:test
./gradlew :kite-dsl:test

# Run integration tests
./gradlew :kite-integration-tests:test

# Run via Kite
kite-cli/build/install/kite-cli/bin/kite-cli run test-all
```

**Test Guidelines:**

- Write tests for all new code
- Use descriptive test names
- Follow AAA pattern (Arrange, Act, Assert)
- Aim for >80% code coverage

---

## 🎨 Code Style

We use **ktlint** and **detekt** to enforce consistent code style.

### Automatic Formatting

```bash
./gradlew ktlintFormat
```

### Key Standards

- **Line length:** 120 characters
- **Indentation:** 4 spaces (no tabs)
- **Imports:** No wildcards, alphabetically sorted
- **Naming:** camelCase for functions/variables, PascalCase for classes
- **Documentation:** KDoc for public APIs

See [Code Quality Guide](docs/dev/02-code-quality.md) for complete standards.

---

## 📁 Project Structure

```
kite/
├── kite-core/              # Core domain models and interfaces
├── kite-dsl/               # DSL and script compilation
├── kite-runtime/           # Execution engine and scheduling
├── kite-cli/               # Command-line interface
├── kite-integration-tests/ # End-to-end tests
├── docs/                   # Documentation
│   ├── 00-index.md through 99-troubleshooting.md
│   ├── dev/                # Developer guides
│   └── specs/              # Architecture specifications
├── .kite/                  # Kite's own CI/CD
│   ├── rides/              # Workflow definitions
│   └── segments/           # Build, test, quality segments
└── scripts/                # Development scripts
```

---

## 🔧 Common Tasks

### Building the CLI

```bash
./gradlew :kite-cli:installDist
export PATH="$PWD/kite-cli/build/install/kite-cli/bin:$PATH"
kite-cli --version
```

### Running Kite Itself

Kite uses itself for CI/CD! Try:

```bash
kite-cli ride CI       # Run CI workflow
kite-cli ride MR       # Run PR validation
kite-cli rides         # List all workflows
```

### Quality Checks

```bash
kite-cli run quality-checks   # All checks via Kite

# Or directly via Gradle:
./gradlew ktlintCheck
./gradlew detekt
```

---

## 🚀 Release Process (maintainers only)

Releases are fully automated via GitHub Actions. The flow is:

### 1. Bump the version

Edit **one file** — `build.gradle.kts`:

```kotlin
// build.gradle.kts
allprojects {
    version = "0.2.0"   // ← change this
}
```

### 2. Sync all version references

A single command updates every version string across the entire repo
(docs, READMEs, `install.sh`, plugin docs) in one shot:

```bash
kite-cli run update-version-refs
```

This rewrites Kite-specific version strings (Maven coordinates, Docker image tags,
shields.io badge, archive names, etc.) while leaving Kotlin/Gradle/Java versions untouched.

### 3. Update the CHANGELOG

Add a new section at the top of `CHANGELOG.md`:

```markdown
## [0.2.0] - YYYY-MM-DD

### Added
- ...

### Fixed
- ...
```

### 4. Commit and tag

```bash
git add -A
git commit -m "chore: Release v0.2.0"
git tag -a v0.2.0 -m "Release v0.2.0"
git push origin main v0.2.0
```

The `v*` tag triggers the `Release & Deploy` GitHub Actions workflow which:
- Builds and tests everything
- Publishes to **Maven Central** (`com.gianluz.kite`)
- Publishes to **GitHub Packages**
- Builds and pushes the **Docker image** to GHCR and Docker Hub
- Creates a **GitHub Release** with the CLI binary archives and `install.sh`

### Pre-commit enforcement

The pre-commit hook runs `kite-cli run check-version-sync` automatically when
`build.gradle.kts` is staged. If any tracked file has a stale version, it will
block the commit and show you the fix command:

```
❌ Out of sync: docs/02-installation.md
💡 Fix with:
   kite-cli run update-version-refs
   git add -A
```

---

## 💡 Need Help?

- **Questions?** Open a [Discussion](https://github.com/gianluz/kite/discussions)
- **Bug report?** Open an [Issue](https://github.com/gianluz/kite/issues)
- **Documentation:** See [docs/](docs/00-index.md)

---

## 📜 Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors.

**Our Standards:**

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community
- Show empathy towards others

---

## 🙏 Thank You!

Every contribution, no matter how small, helps make Kite better. Thank you for being part of our community!

---

**For complete contributing guidelines, see [docs/dev/01-contributing.md](docs/dev/01-contributing.md)**
