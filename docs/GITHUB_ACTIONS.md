# GitHub Actions Setup

Kite's GitHub Actions workflow for automated MR validation.

## Overview

The MR validation workflow automatically runs on:

- **Pull Requests** to `main` branch
- **Pushes** to `main` branch

It validates the entire codebase by running the `MR` ride, which includes:

- Clean build
- Compilation
- All tests (core, dsl, runtime, cli, integration) in parallel
- Full build

## Workflow Configuration

**File:** `.github/workflows/mr.yml`

```yaml
name: MR Validation

on:
  pull_request:
    branches: [ main ]
  push:
    branches: [ main ]

jobs:
  validate:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      
      - name: Build Kite CLI
        run: ./gradlew :kite-cli:installDist
      
      - name: Run MR Validation
        run: kite-cli/build/install/kite-cli/bin/kite-cli ride MR
      
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: .kite/artifacts/
          retention-days: 7
      
      - name: Publish Test Report
        if: always()
        uses: dorny/test-reporter@v1
        with:
          name: Test Results
          path: '.kite/artifacts/test-results-*/TEST-*.xml'
          reporter: java-junit
          fail-on-error: true
```

## Features

### ✅ Automatic Test Result Upload

Test results are automatically uploaded as GitHub Actions artifacts:

- **Retention:** 7 days
- **Contents:** All test results and reports from `.kite/artifacts/`
- **Always runs:** Even if tests fail

### ✅ Test Report Publishing

Uses `dorny/test-reporter` to publish test results as GitHub Actions checks:

- Shows test results directly in the PR
- Individual test failures are highlighted
- Fails the workflow if tests fail

### ✅ Kite Self-Validation

The workflow uses Kite itself to validate Kite:

- Builds Kite CLI
- Runs the `MR` ride
- Validates using Kite's own DSL and execution engine

## MR Ride

**File:** `.kite/rides/mr.kite.kts`

```kotlin
ride {
    name = "MR"
    maxConcurrency = 4
    
    flow {
        // First: Clean and compile
        segment("clean")
        segment("compile")
        
        // Then: Run all tests in parallel
        parallel {
            segment("test-core")
            segment("test-dsl")
            segment("test-runtime")
            segment("test-cli")
            segment("test-integration")
        }
        
        // Finally: Full build
        segment("build")
    }
}
```

**Performance:**

- Sequential time: ~30s
- Parallel time: ~17s
- **Time saved: ~44%** ⚡

## Viewing Results

### In Pull Requests

1. **Checks Tab:** See test results summary
2. **Test Report:** Click on test-reporter check for detailed results
3. **Artifacts:** Download full test results from workflow artifacts

### In Actions Tab

1. Go to **Actions** tab in GitHub
2. Click on workflow run
3. View:
    - Overall success/failure
    - Test report (as a check)
    - Downloadable artifacts

## Local Testing

Test the MR ride locally before pushing:

```bash
# Build Kite CLI
./gradlew :kite-cli:installDist

# Run MR ride
kite-cli/build/install/kite-cli/bin/kite-cli ride MR

# Or use an alias
alias kite='kite-cli/build/install/kite-cli/bin/kite-cli'
kite ride MR
```

## Artifacts Generated

After each MR run, the following artifacts are available in `.kite/artifacts/`:

```
.kite/artifacts/
├── .manifest.json              # Artifact manifest (for cross-ride sharing)
├── test-results-core/          # JUnit XML results
│   └── TEST-*.xml
├── test-reports-core/          # HTML reports
│   └── index.html
├── test-results-dsl/
├── test-reports-dsl/
├── test-results-runtime/
├── test-reports-runtime/
├── test-results-cli/
├── test-reports-cli/
├── test-results-integration/
└── test-reports-integration/
```

These are automatically:

1. Uploaded to GitHub Actions artifacts (retention: 7 days)
2. Used by test-reporter for PR checks
3. Available for download for debugging

## Customization

### Change Test Retention

```yaml
- name: Upload Test Results
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: .kite/artifacts/
    retention-days: 30  # Change to 30 days
```

### Add More Platforms

```yaml
jobs:
  validate:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
```

### Add Code Coverage

```yaml
- name: Generate Coverage Report
  run: ./gradlew jacocoTestReport

- name: Upload Coverage
  uses: codecov/codecov-action@v3
  with:
    files: ./build/reports/jacoco/test/jacocoTestReport.xml
```

## Troubleshooting

### Workflow Fails to Find kite-cli

Make sure the build step runs first:

```yaml
- name: Build Kite CLI
  run: ./gradlew :kite-cli:installDist
```

### Test Results Not Published

Check that test results are in the correct path:

```yaml
path: '.kite/artifacts/test-results-*/TEST-*.xml'
```

### Artifacts Not Uploaded

Ensure `if: always()` is set to upload even on failure:

```yaml
- name: Upload Test Results
  if: always()
  uses: actions/upload-artifact@v4
```

## Future Enhancements

### Potential Additions

- **Code coverage reporting** with codecov
- **Performance benchmarking** on each PR
- **Multi-platform testing** (Linux, macOS, Windows)
- **Dependency vulnerability scanning**
- **Docker image building and publishing**
- **Release automation** on version tags

## Summary

The GitHub Actions workflow provides:

- ✅ Automated validation on every PR
- ✅ Test result publishing
- ✅ Artifact preservation
- ✅ Fast parallel execution (~17s)
- ✅ Self-hosting (Kite validates Kite!)

**Kite dogfoods itself for CI/CD!** 🎉
