# Kite Documentation

Welcome to the Kite documentation! This directory contains comprehensive guides for using and developing Kite.

---

## 📚 Table of Contents

### Getting Started

- **[Project Setup Guide](EXTERNAL_PROJECT_SETUP.md)** - How to use Kite in your own projects
- **[External Dependencies Guide](EXTERNAL_DEPENDENCIES.md)** - Using external libraries with `@DependsOn`

### IDE Support

- **[IDE Setup Guide](IDE_SETUP.md)** - Setting up IntelliJ IDEA for Kite development
- **[IDE Autocomplete Troubleshooting](IDE_AUTOCOMPLETE_TROUBLESHOOTING.md)** - Fixing common IDE issues

### Architecture & Specifications

See the **[specs/](../specs/)** directory for detailed specifications:

- [Overview & Problem Statement](../specs/01-overview.md)
- [Core Concepts](../specs/02-core-concepts.md)
- [DSL & Configuration](../specs/03-dsl-configuration.md)
- [Execution Model](../specs/04-execution-model.md)
- [Parallelization](../specs/05-parallelization.md)
- [Built-in Features](../specs/06-builtin-features.md)
- [Plugin System](../specs/07-plugin-system.md)
- [Platform Integration](../specs/08-platform-integration.md)
- [Architecture](../specs/09-architecture.md)

---

## 🚀 Quick Links

### For Users

Start with **[EXTERNAL_PROJECT_SETUP.md](EXTERNAL_PROJECT_SETUP.md)** to learn how to:

- Add Kite to your project
- Write `.kite.kts` segment files
- Create rides (workflows)
- Use external dependencies

### For Contributors

Start with **[IDE_SETUP.md](IDE_SETUP.md)** to set up your development environment, then see:

- [CONTRIBUTING.md](../CONTRIBUTING.md) - Contribution guidelines
- [DEVELOPMENT_PLAN.md](../DEVELOPMENT_PLAN.md) - Implementation roadmap

### Troubleshooting

Having issues with IDE autocomplete? See **[IDE_AUTOCOMPLETE_TROUBLESHOOTING.md](IDE_AUTOCOMPLETE_TROUBLESHOOTING.md)**

---

## 📖 Documentation Structure

```
docs/
├── README.md                              # This file - documentation index
├── EXTERNAL_PROJECT_SETUP.md              # Using Kite in your projects
├── EXTERNAL_DEPENDENCIES.md               # External library support
├── IDE_SETUP.md                           # IDE configuration for developers
└── IDE_AUTOCOMPLETE_TROUBLESHOOTING.md    # Fixing IDE autocomplete issues
```

---

## 🎯 Common Use Cases

### I want to use Kite in my project

→ **[EXTERNAL_PROJECT_SETUP.md](EXTERNAL_PROJECT_SETUP.md)**

### I'm getting IDE errors with `.kite.kts` files

→ **[IDE_AUTOCOMPLETE_TROUBLESHOOTING.md](IDE_AUTOCOMPLETE_TROUBLESHOOTING.md)**

### I want to use external libraries (Gson, OkHttp, etc.)

→ **[EXTERNAL_DEPENDENCIES.md](EXTERNAL_DEPENDENCIES.md)**

### I want to contribute to Kite

→ **[IDE_SETUP.md](IDE_SETUP.md)** + **[../CONTRIBUTING.md](../CONTRIBUTING.md)**

### I want to understand Kite's architecture

→ **[../specs/09-architecture.md](../specs/09-architecture.md)**

---

## 💡 Examples

Looking for examples? Check out:

- `.kite/segments/` - Example segment definitions in this repository
- `.kite/rides/` - Example ride definitions
- **[EXTERNAL_PROJECT_SETUP.md](EXTERNAL_PROJECT_SETUP.md)** - Complete working examples

---

## 🆘 Getting Help

1. Check the relevant documentation above
2. Look at the **[IDE_AUTOCOMPLETE_TROUBLESHOOTING.md](IDE_AUTOCOMPLETE_TROUBLESHOOTING.md)** for common issues
3. Review the **[specs/](../specs/)** for detailed information
4. Check the issues on GitHub (coming soon)

---

**Happy building with Kite!** 🪁
