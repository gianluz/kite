# GitHub Actions Setup

Kite's GitHub Actions workflows for automated CI/CD.

## Overview

Kite uses two separate workflows for different purposes:

1. **PR Validation** (`.github/workflows/pr.yml`) - Validates pull requests
2. **CI Build** (`.github/workflows/ci.yml`) - Continuous integration on main branch

---

## Workflow 1: PR Validation

### Purpose

Validates pull requests before merging to ensure code quality and passing tests.

### Configuration

**File:** `.github/workflows/pr.yml`

**Triggers:**

- On pull requests to `main` branch

**Permissions:**

- `contents: read` - Read repository contents
- `pull-requests: write` - Post test results to PR
- `checks: write` - Create check runs

### What It Does

```yaml
name: PR Validation

on:
  pull_request:
    branches: [ main ]

permissions:
  contents: read
  pull-requests: write
  checks: write

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

      - name: Run PR Validation
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

### Steps Explained

1. **Checkout & Setup**
    - Checks out the PR code
    - Sets up Java 17 (Temurin distribution)
    - Configures Gradle with caching

2. **Build Kite CLI**
    - Compiles Kite and creates executable
    - Uses Gradle's `installDist` task

3. **Run MR Ride**
    - Executes the MR validation ride (`.kite/rides/mr.kite.kts`)
    - Runs all tests in parallel (4 concurrent)
    - Uses Kite itself to validate Kite!

4. **Upload Test Results**
    - Always runs (even if tests fail)
    - Uploads entire `.kite/artifacts/` directory
    - Includes JUnit XML and HTML reports
    - 7-day retention for PR artifacts

5. **Publish Test Report**
    - Uses `dorny/test-reporter` to parse JUnit XML
    - Creates check run in the PR
    - Shows test results inline in the PR
    - Marks PR as failed if tests fail

**Why `pull-requests: write` is needed**: Without this permission, the test reporter can't post results to the PR and
you'll get "Resource not accessible by integration" error.

---

## Workflow 2: CI Build

### Purpose

Continuous integration build that runs on the main branch after PRs are merged.

### Configuration

**File:** `.github/workflows/ci.yml`

**Triggers:**

- On push to `main` branch (including merged PRs)
- Manual trigger via `workflow_dispatch`

**Permissions:**

- `contents: read` - Read repository contents only

### What It Does

```yaml
name: CI Build

on:
  push:
    branches: [ main ]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
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

      - name: Run CI Build
        run: kite-cli/build/install/kite-cli/bin/kite-cli ride CI

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results-ci
          path: .kite/artifacts/
          retention-days: 30

      - name: Upload Build Artifacts
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: kite-cli
          path: kite-cli/build/install/kite-cli/
          retention-days: 30
```

### Steps Explained

1. **Checkout & Setup**
    - Same as PR workflow
    - Java 17, Gradle with caching

2. **Build Kite CLI**
    - Compiles and installs Kite

3. **Run CI Ride**
    - Executes full CI ride (`.kite/rides/ci.kite.kts`)
    - Runs all tests (sequential build + parallel tests)
    - Publishes test results summary

4. **Upload Test Results**
    - Always runs (even if tests fail)
    - 30-day retention (longer than PRs)
    - Useful for debugging main branch issues

5. **Upload Build Artifacts**
    - Only on success
    - Uploads compiled CLI for potential deployment
    - 30-day retention

### Key Differences from PR Workflow

| Aspect | PR Validation | CI Build |
|--------|---------------|----------|
| **Trigger** | Pull requests | Push to main |
| **Test Reporter** | ✅ Yes (posts to PR) | ❌ No (not needed) |
| **Permissions** | Read + Write PR/Checks | Read only |
| **Artifact Retention** | 7 days | 30 days |
| **CLI Upload** | ❌ No | ✅ Yes |
| **Manual Trigger** | ❌ No | ✅ Yes (workflow_dispatch) |

---

## Performance

Both workflows benefit from Kite's parallel execution:

**Sequential Execution**: ~30 seconds  
**Parallel Execution**: ~17 seconds  
**Time Saved**: 13 seconds (44% faster) ⚡

### What Runs in Parallel

```
clean ──┐
        ├──→ compile ──┐
        │              ├──→ parallel {
        │              │      test-core
        │              │      test-dsl  
        │              │      test-runtime
        │              │      test-cli
        │              │      test-integration
        │              │    } ──→ publish-test-results ──→ build
        └──────────────┘
```

---

## Viewing Results

### In Pull Requests

1. **Checks Tab**: See overall status
2. **Test Results Check**: Click to see detailed test report
3. **Artifacts**: Download test results and reports
4. **Files Changed**: Test results show inline (if configured)

### In Actions Tab

1. Go to **Actions** tab in GitHub
2. Select **PR Validation** or **CI Build** workflow
3. Click on a run to see details
4. Download artifacts (test results, CLI build)

---

## Troubleshooting

### Error: "Resource not accessible by integration"

**Problem**: Test reporter can't post results to PR.

**Solution**: Add permissions to workflow:

```yaml
permissions:
  contents: read
  pull-requests: write
  checks: write
```

### Workflow runs on push to main

**Problem**: You want PR validation only on PRs, not on push to main.

**Solution**: Use separate workflows:

- `pr.yml` - Only on `pull_request`
- `ci.yml` - Only on `push` to `main`

### Tests pass locally but fail in CI

**Problem**: Different environment, missing dependencies.

**Solution**:

1. Check Java version (must be 17)
2. Check Gradle version (wrapper should handle this)
3. Look at artifact logs (`.kite/logs/`)
4. Run with same Java version locally

---

## Manual Workflow Trigger

The CI Build workflow can be triggered manually:

1. Go to **Actions** tab
2. Select **CI Build** workflow
3. Click **Run workflow**
4. Select branch (usually `main`)
5. Click **Run workflow** button

Useful for:

- Testing workflow changes
- Re-running builds without new commits
- Debugging CI issues

---

## Best Practices

### 1. Always Use PR Workflow

Don't skip the PR workflow by pushing directly to main. Let it validate your changes first.

### 2. Check Test Reports

Review the test report in the PR before merging. Look for:

- Flaky tests
- New test failures
- Performance regressions

### 3. Download Artifacts for Debugging

If tests fail, download the artifacts to see:

- Full logs (`.kite/logs/`)
- Test reports (HTML)
- JUnit XML files

### 4. Keep Workflows Updated

Update workflow files when:

- Java version changes
- Gradle version changes
- New dependencies added
- Kite structure changes

---

## Future Enhancements

Potential improvements:

- **Code coverage reporting** (Kover)
- **Performance benchmarking**
- **Release automation** (tag → build → publish)
- **Docker image builds**
- **Multiple OS testing** (macOS, Windows)

---

## Summary

**Two workflows, clear purposes**:

- **`pr.yml`**: Validates PRs with test reporting
- **`ci.yml`**: Builds main branch, uploads artifacts

**Key features**:

- ✅ Kite validates itself using Kite
- ✅ Parallel test execution (44% faster)
- ✅ Test results posted to PRs
- ✅ Artifacts uploaded for debugging
- �� Manual trigger available for CI

**Permissions matter**: `pull-requests: write` is required for test reporting in PRs.
