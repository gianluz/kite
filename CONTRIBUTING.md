# Contributing to Kite

Thank you for your interest in contributing to Kite! This document provides guidelines and instructions for
contributing.

## Development Setup

### Prerequisites

- JDK 17 or higher (LTS)
- Kotlin 2.0+
- Gradle 9.2+ (wrapper included)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/yourusername/kite.git
cd kite

# Build all modules
./gradlew build

# Run tests
./gradlew test

# Run linters
./gradlew ktlintCheck detekt
```

## Project Structure

```
kite/
├── kite-core/      # Core domain models and interfaces
├── kite-dsl/       # DSL and scripting engine
├── kite-runtime/   # Task execution runtime
├── kite-cli/       # Command-line interface
├── SPECS.md        # Complete specification
└── DEVELOPMENT_PLAN.md  # Development roadmap
```

## Development Process

### 1. Pick a Task

- Check the [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) for available tasks
- Tasks are organized by phases and epics
- Comment on the issue to claim it

### 2. Create a Branch

```bash
git checkout -b feature/task-number-description
# Example: git checkout -b feature/1.2.1-task-model
```

### 3. Implement the Task

- Follow the task description in DEVELOPMENT_PLAN.md
- Write tests alongside your code
- Ensure code coverage > 80%
- Follow Kotlin coding conventions

### 4. Code Quality

Run quality checks before committing:

```bash
# Format code
./gradlew ktlintFormat

# Run linters
./gradlew ktlintCheck detekt

# Run tests
./gradlew test

# Check coverage
./gradlew koverReport
```

### 5. Commit Guidelines

Use conventional commit messages:

```
type(scope): description

feat(core): add Task model with validation
fix(runtime): handle timeout edge case
docs(readme): update installation instructions
test(dsl): add tests for task builder
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

### 6. Submit a Pull Request

- Push your branch to GitHub
- Create a pull request against `main`
- Fill out the PR template
- Link to the task in DEVELOPMENT_PLAN.md
- Wait for code review

## Code Style

### Kotlin

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Maximum line length: 120 characters
- Use meaningful variable names
- Document public APIs with KDoc

### Testing

- Write unit tests for all new code
- Use descriptive test names: `` `test name in backticks` ``
- Follow AAA pattern: Arrange, Act, Assert
- Use MockK for mocking

Example:

```kotlin
@Test
fun `task execution should timeout after configured duration`() {
    // Arrange
    val task = Task(id = "test", timeout = 1.seconds)
    
    // Act
    val result = runBlocking { executor.execute(task) }
    
    // Assert
    assertTrue(result.isTimeout)
}
```

## Module Dependencies

```
kite-cli
  └── kite-runtime
      ├── kite-core
      └── kite-dsl
          └── kite-core
```

- `kite-core`: No dependencies on other modules
- `kite-dsl`: Depends on `kite-core`
- `kite-runtime`: Depends on `kite-core` and `kite-dsl`
- `kite-cli`: Depends on all modules

## Testing Strategy

### Unit Tests

- Test individual classes and functions
- Mock external dependencies
- Fast execution (<100ms per test)
- Located in `src/test/kotlin`

### Integration Tests

- Test interactions between modules
- Use real implementations where possible
- May be slower
- Located in `src/test/kotlin` with `@IntegrationTest` annotation

### End-to-End Tests

- Test complete pipelines
- Will be added in Phase 7

## Documentation

- Update relevant documentation when making changes
- KDoc for public APIs
- Comments for complex logic
- Update CHANGELOG.md

## Questions?

- Open an issue for questions
- Check existing issues and discussions
- Refer to SPECS.md for design decisions

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Help others learn

Thank you for contributing to Kite! 🚀
