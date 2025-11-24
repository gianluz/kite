# Kite GitHub Actions

This directory contains GitHub Actions for running Kite in CI/CD pipelines.

## Available Actions

### 1. `setup-kite` - Install Kite

Sets up Kite in your GitHub Actions workflow.

**Inputs:**

- `version` (optional): Kite version to install (default: `latest`)
- `java-version` (optional): Java version to use (default: `17`)

**Outputs:**

- `kite-version`: The installed Kite version
- `kite-path`: Path to the Kite executable

**Example:**

```yaml
- name: Setup Kite
  uses: ./.github/actions/setup-kite
  with:
    version: latest
    java-version: '17'

- name: Run Kite commands
  run: kite-cli run test
```

### 2. `run-kite` - Setup and Run Kite

Combines setup and execution into a single action.

**Inputs:**

- `command` (required): Kite command to run (e.g., `"ride CI"` or `"run test lint"`)
- `version` (optional): Kite version to use (default: `latest`)
- `java-version` (optional): Java version to use (default: `17`)
- `working-directory` (optional): Working directory for execution (default: `.`)

**Example:**

```yaml
- name: Run CI Workflow
  uses: ./.github/actions/run-kite
  with:
    command: ride CI
```

## Real-World Examples from Kite Project

### Kite's CI Workflow (ci.yml)

```yaml
name: CI Build

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run CI Build
        uses: ./.github/actions/run-kite
        with:
          command: ride CI
      
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results-ci
          path: .kite/artifacts/
          retention-days: 30
```

### Kite's PR Validation (pr.yml)

```yaml
name: PR Validation

on:
  pull_request:
    branches: [ main ]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run PR Validation
        uses: ./.github/actions/run-kite
        with:
          command: ride MR
      
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: .kite/artifacts/
```

## More Usage Examples

### Running a Complete Workflow

```yaml
name: CI

on: [push, pull_request]

jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run CI Workflow
        uses: ./.github/actions/run-kite
        with:
          command: ride CI
```

### Running Specific Segments

```yaml
name: Tests

on: [pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Tests
        uses: ./.github/actions/run-kite
        with:
          command: run test lint
```

### Setup Once, Run Multiple Commands

```yaml
name: Build and Test

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Kite
        uses: ./.github/actions/setup-kite
        id: setup
      
      - name: Build
        run: kite-cli run build
      
      - name: Test
        run: kite-cli run test
      
      - name: Deploy
        if: github.ref == 'refs/heads/main'
        run: kite-cli run deploy
```

### Conditional Workflows

```yaml
name: CI/CD

on: [push, pull_request]

jobs:
  ci-cd:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Kite
        uses: ./.github/actions/setup-kite
      
      - name: PR Workflow
        if: github.event_name == 'pull_request'
        run: kite-cli ride PR
      
      - name: Release Workflow
        if: github.ref == 'refs/heads/main'
        run: kite-cli ride Release
```

### Using with Secrets

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Deploy
        uses: ./.github/actions/run-kite
        with:
          command: run deploy
        env:
          DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
          AWS_ACCESS_KEY: ${{ secrets.AWS_ACCESS_KEY }}
```

## External Project Usage

To use these actions in a separate project:

### Option 1: Reference by Tag/Branch

Once Kite is published, you can reference the actions directly:

```yaml
- name: Setup Kite
  uses: yourusername/kite/.github/actions/setup-kite@v1
```

### Option 2: Copy Actions to Your Project

1. Copy the `.github/actions/` directory to your project
2. Reference locally: `uses: ./.github/actions/setup-kite`

### Option 3: Use from a Marketplace Action (Future)

Once published to GitHub Marketplace:

```yaml
- name: Setup Kite
  uses: yourusername/setup-kite@v1
```

## Installation Behavior

### Pre-Release (Current)

The action builds Kite from source by:

1. Cloning the Kite repository
2. Running `./gradlew :kite-cli:installDist`
3. Caching the built binary for subsequent runs

### Post-Release (Future)

Once published to Maven Central, the action will:

1. Download pre-built binaries from GitHub Releases
2. Extract and install to `~/.kite`
3. Cache the installation for faster subsequent runs

## Caching

The actions use GitHub Actions cache to speed up subsequent runs:

- Cache key: `kite-{version}-{os}`
- Cache location: `~/.kite`

This means after the first run, Kite installation is nearly instantaneous!

## Platform Support

Currently supported:

- ✅ Linux (ubuntu-latest)
- ✅ macOS (macos-latest)
- ⚠️ Windows (experimental, requires WSL or Git Bash)

## Requirements

- GitHub Actions runner with Java 17+ (automatically installed by the action)
- Git (pre-installed on all GitHub Actions runners)

## Troubleshooting

### Action fails with "kite-cli: command not found"

Ensure you're using the correct command name:

```yaml
run: kite-cli run test  # ✅ Correct
run: kite run test       # ❌ Wrong (without -cli suffix)
```

### Build from source is slow

The first run will be slower as it builds from source. Subsequent runs use cached binaries and are much faster.

### Java version mismatch

If you need a specific Java version:

```yaml
- uses: ./.github/actions/setup-kite
  with:
    java-version: '21'  # Use Java 21
```

## Future Improvements

- [ ] Download pre-built binaries from GitHub Releases
- [ ] Publish to GitHub Marketplace as a standalone action
- [ ] Support for custom installation directories
- [ ] Better Windows support
- [ ] Version pinning and automatic updates

## Contributing

Found an issue with the actions? [Open an issue](https://github.com/yourusername/kite/issues) or submit a PR!
