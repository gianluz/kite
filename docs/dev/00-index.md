# Developer Documentation

Documentation for Kite contributors and developers.

---

## 📚 Developer Guides

### [01. Contributing Guide](01-contributing.md)

Complete guide for contributing to Kite including:

- Development setup and prerequisites
- Build and test instructions
- Git hooks and quality checks
- Pull request workflow
- Commit message conventions

### [02. Code Quality Standards](02-code-quality.md)

Code quality enforcement and standards:

- Ktlint configuration and rules
- Detekt static analysis
- Running quality checks locally
- CI/CD quality gates
- Auto-formatting tools

### [03. Testing Strategy](03-testing-strategy.md)

Testing approach and integration test strategy:

- Integration testing framework
- Test organization and structure
- Running integration tests
- Writing new tests
- Coverage requirements

---

## 🏗️ Architecture

For detailed architecture documentation, see **[Architecture Specifications](../specs/)**.

---

## 🎯 Quick Reference

### Common Commands

```bash
# Build project
./gradlew build

# Run tests
./gradlew test

# Quality checks
./gradlew ktlintCheck detekt

# Auto-fix formatting
./gradlew ktlintFormat

# Install git hooks
./scripts/install-git-hooks.sh
```

### Using Kite on Itself

```bash
# Build CLI
./gradlew :kite-cli:installDist

# Run CI workflow
kite-cli/build/install/kite-cli/bin/kite-cli ride CI

# Run quality checks
kite-cli/build/install/kite-cli/bin/kite-cli run quality-checks
```

---

## 📖 Additional Resources

- **[User Documentation](../00-index.md)** - End-user guides
- **[Architecture Specs](../specs/)** - Detailed system design
- **[Main README](../../README.md)** - Project overview

---

**For questions or help, open a [Discussion](https://github.com/yourusername/kite/discussions)**
