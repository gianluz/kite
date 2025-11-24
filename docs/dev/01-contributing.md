# Contributing to Kite

Thank you for your interest in contributing to Kite!

## Development Setup

### Prerequisites

- Java 17 or higher
- Git

### Clone and Build

```bash
git clone https://github.com/gianluz/kite.git
cd kite

# Build Kite CLI
./gradlew :kite-cli:installDist

# Run tests
./gradlew test
```

### Install Git Hooks (Recommended)

Install pre-push hooks to automatically check code quality before pushing:

```bash
./scripts/install-git-hooks.sh
```

This installs:

- **pre-push**: Runs ktlint and detekt before pushing

## Code Quality

Kite enforces code quality using:

- **ktlint** - Kotlin code style
- **detekt** - Static code analysis

### Run Checks Locally

Before committing, run:

```bash
# Run all quality checks
kite-cli/build/install/kite-cli/bin/kite-cli run quality-checks

# Or individually
kite-cli/build/install/kite-cli/bin/kite-cli run ktlint
kite-cli/build/install/kite-cli/bin/kite-cli run detekt

# Auto-fix ktlint issues
./gradlew ktlintFormat
```

### CI/CD Checks

Quality checks run automatically on:

- ✅ Every push to `main`
- ✅ Every pull request
- ❌ Pull requests are blocked if checks fail

See [docs/CODE_QUALITY.md](CODE_QUALITY.md) for detailed information.

## Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
    - Follow Kotlin coding conventions
    - Add tests for new functionality
    - Update documentation if needed

3. **Run quality checks**
   ```bash
   # This happens automatically if you installed git hooks
   kite-cli/build/install/kite-cli/bin/kite-cli run quality-checks
   ```

4. **Run tests**
   ```bash
   ./gradlew test
   ```

5. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: add awesome feature"
   ```

   Use [Conventional Commits](https://www.conventionalcommits.org/):
    - `feat:` - New feature
    - `fix:` - Bug fix
    - `docs:` - Documentation changes
    - `refactor:` - Code refactoring
    - `test:` - Test changes
    - `chore:` - Build/tooling changes

6. **Push and create PR**
   ```bash
   git push origin feature/your-feature-name
   ```

   Then open a pull request on GitHub.

## Bypassing Hooks

If you need to bypass the pre-push hook (not recommended):

```bash
git push --no-verify
```

**Note:** CI/CD will still run the checks, so your PR may be blocked.

## Project Structure

```
kite/
├── kite-core/           # Core domain models and interfaces
├── kite-dsl/            # DSL and script compilation
├── kite-runtime/        # Execution engine and scheduling
├── kite-cli/            # Command-line interface
├── kite-integration-tests/  # End-to-end tests
├── .kite/               # Kite's own CI/CD configuration
│   ├── rides/          # CI and MR workflow definitions
│   └── segments/       # Build, test, and quality segments
└── scripts/            # Development scripts and hooks
    └── git-hooks/      # Git hooks for quality checks
```

## Running Kite's Own CI/CD

Kite uses itself for CI/CD! You can run the same workflows locally:

```bash
# Build Kite
kite-cli/build/install/kite-cli/bin/kite-cli ride CI

# Run PR validation
kite-cli/build/install/kite-cli/bin/kite-cli ride MR
```

## Testing

### Unit Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :kite-core:test
./gradlew :kite-dsl:test
./gradlew :kite-runtime:test
```

### Integration Tests

```bash
./gradlew :kite-integration-tests:test
```

### Using Kite

```bash
# Run all tests via Kite
kite-cli/build/install/kite-cli/bin/kite-cli run test-core
kite-cli/build/install/kite-cli/bin/kite-cli run test-dsl
# etc.
```

## Code Style

- **Line length**: 120 characters
- **Indentation**: 4 spaces (no tabs)
- **Imports**: No wildcards, alphabetically sorted
- **Documentation**: KDoc for public APIs
- **Tests**: Test names should read like sentences

## Questions?

- Open an issue for bugs or feature requests
- Start a discussion for questions or ideas
- Check existing issues and PRs before creating new ones

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
