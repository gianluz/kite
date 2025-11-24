# Documentation Archive

This directory contains historical documentation files that have been superseded by the new numbered documentation
structure (`00-index.md` through `99-troubleshooting.md`).

## Why Archived?

These files were part of the initial documentation effort and have been consolidated, improved, and reorganized into the
new user-facing documentation structure in the parent `docs/` directory.

## Contents

### `/old-docs/` - Superseded Documentation

These files have been **replaced** by the new guides:

| Old File | New Location | Description |
|----------|--------------|-------------|
| `ARTIFACTS.md` | `../08-artifacts.md` | Artifact management |
| `ARTIFACTS_CROSS_RIDE.md` | `../08-artifacts.md` | Cross-ride artifacts (consolidated) |
| `ARTIFACTS_SIMPLE.md` | `../08-artifacts.md` | Simple artifacts (consolidated) |
| `CI_INTEGRATION.md` | `../11-ci-integration.md` | CI/CD integration |
| `GITHUB_ACTIONS.md` | `../11-ci-integration.md` | GitHub Actions (consolidated) |
| `EXTERNAL_DEPENDENCIES.md` | `../10-external-dependencies.md` | External dependencies |
| `EXTERNAL_PROJECT_SETUP.md` | `../02-installation.md` | Project setup (consolidated) |
| `IDE_SETUP.md` | `../02-installation.md` | IDE setup (consolidated) |
| `IDE_AUTOCOMPLETE_TROUBLESHOOTING.md` | `../99-troubleshooting.md` | IDE issues (consolidated) |
| `IDE_SCRIPT_SUPPORT_FIX.md` | `../99-troubleshooting.md` | Script fixes (consolidated) |
| `LIFECYCLE_HOOKS.md` | `../04-writing-segments.md` | Lifecycle hooks (consolidated) |
| `SECURITY.md` | `../09-secrets.md` | Secret management |
| `README.md` | `../00-index.md` | Documentation index |

### Root Archive Files - Historical Reference

- `DOCUMENTATION_CENSUS.md` - Original documentation inventory (historical reference)
- `DOCUMENTATION_ROADMAP.md` - Restructuring plan (historical reference)

## Current Documentation Structure

The **new documentation** in `docs/` follows a numbered structure for easy navigation:

```
docs/
├── 00-index.md                # Documentation hub
├── 01-getting-started.md      # Quick start guide
├── 02-installation.md         # Installation methods
├── 03-core-concepts.md        # Rides, segments, flows
├── 04-writing-segments.md     # Segment authoring
├── 05-writing-rides.md        # Ride composition
├── 06-execution-context.md    # API reference
├── 07-parallel-execution.md   # Parallelism
├── 08-artifacts.md            # Artifact management
├── 09-secrets.md              # Secret handling
├── 10-external-dependencies.md # Dependencies
├── 11-ci-integration.md       # CI/CD integration
├── 12-cli-reference.md        # CLI documentation
├── 99-troubleshooting.md      # Troubleshooting
└── dev/                       # Developer documentation
    ├── contributing.md
    ├── code-quality.md
    └── integration-testing-strategy.md
```

## Should I Use These Files?

**No.** Use the new documentation in the parent directory instead. These archived files are:

- Outdated
- Superseded by better content
- Kept only for historical reference

## Why Keep Them?

- Historical reference
- May contain edge cases or details worth preserving
- Version control history context
- Can be safely deleted if desired

---

**Last Updated:** November 2025  
**Status:** Archived - Use new documentation in `docs/` instead
