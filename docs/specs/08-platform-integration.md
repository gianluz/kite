# Platform Integration

> **⚠️ Deprecation Notice**: This document references deprecated properties like `mrNumber`, `isRelease`, and `ciPlatform`.
> These are deprecated in favor of Kite's platform-agnostic design. See [Execution Context - Platform-Specific Environment Variables](../06-execution-context.md#platform-specific-environment-variables)
> document for the recommended approach. Users should check environment variables directly using `env()`.


## Overview

Kite integrates seamlessly with CI/CD platforms like GitLab CI, GitHub Actions, and local development environments. The
platform adapters automatically detect the execution environment and provide appropriate context.

## GitLab CI Integration

### Basic Setup

```yaml
# .gitlab-ci.yml
variables:
  KITE_VERSION: "1.0.0"

.kite-base:
  image: android-build-image:latest
  before_script:
    - curl -L https://github.com/kite/releases/download/v${KITE_VERSION}/kite -o kite
    - chmod +x kite
  artifacts:
    paths:
      - build/artifacts/
    reports:
      junit: build/test-results/**/*.xml
```

### MR Ride

```yaml
android-mr:
  extends: .kite-base
  stage: build
  script:
    - ./kite ride mr
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
```

### Release MR Ride

```yaml
android-release-mr:
  extends: .kite-base
  stage: build
  script:
    - ./kite ride release-mr
  rules:
    - if: '$CI_MERGE_REQUEST_LABELS =~ /release/'
```

### Nightly Ride

```yaml
android-nightly:
  extends: .kite-base
  stage: build
  script:
    - ./kite ride nightly
  rules:
    - if: '$CI_PIPELINE_SOURCE == "schedule"'
  only:
    - schedules
```

### Environment Variables

GitLab CI variables are automatically available in segments:

```kotlin
segment("deploy") {
    execute {
        // Access GitLab CI variables
        val ciJobId = env("CI_JOB_ID")
        val ciCommitSha = env("CI_COMMIT_SHA")
        val ciMergeRequestIid = env("CI_MERGE_REQUEST_IID")
        
        println("Job ID: $ciJobId")
        println("Commit: $ciCommitSha")
        println("MR: $ciMergeRequestIid")
    }
}
```

## GitHub Actions Integration

### Basic Workflow

```yaml
name: Android CI

on:
  pull_request:
  push:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Kite
        run: |
          curl -L https://github.com/kite/releases/download/v1.0.0/kite -o kite
          chmod +x kite
      
      - name: Run MR Ride
        if: github.event_name == 'pull_request'
        run: ./kite ride mr
      
      - name: Run Release Ride
        if: contains(github.event.pull_request.labels.*.name, 'release')
        run: ./kite ride release-mr
      
      - name: Upload Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: build/artifacts/
```

### Matrix Strategy

```yaml
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest]
        java: [17, 21]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
      - name: Setup Kite
        run: |
          curl -L https://github.com/kite/releases/download/v1.0.0/kite -o kite
          chmod +x kite
      - name: Run Tests
        run: ./kite run test
```

### Environment Variables

```kotlin
segment("githubDeploy") {
    execute {
        // Access GitHub Actions variables
        val githubRef = env("GITHUB_REF")
        val githubSha = env("GITHUB_SHA")
        val githubActor = env("GITHUB_ACTOR")
        
        println("Ref: $githubRef")
        println("SHA: $githubSha")
        println("Actor: $githubActor")
    }
}
```

## Local Execution

### Running Locally

```bash
# Run a ride
./kite ride mr

# Run specific segments
./kite run build test

# Debug mode
./kite ride mr --debug

# Dry run
./kite ride mr --dry-run
```

### Local Detection

Kite automatically detects local execution:

```kotlin
segment("conditionalDeploy") {
    condition = { !context.isLocal }
    
    execute {
        // Only runs in CI, not locally
        deployToProduction()
    }
}

segment("localSetup") {
    condition = { context.isLocal }
    
    execute {
        // Only runs locally
        println("Setting up local environment...")
    }
}
```

## Platform Adapters

### Execution Context

Platform adapters populate the execution context:

```kotlin
data class ExecutionContext(
    val branch: String,              // From Git or CI
    val commitSha: String,           // From Git or CI
    val mrNumber: String?,           // From CI (GitLab: CI_MERGE_REQUEST_IID, GitHub: PR number)
    val isRelease: Boolean,          // From labels or branch name
    val isLocal: Boolean,            // True if not in CI
    val ciPlatform: CIPlatform,      // GITLAB, GITHUB, LOCAL, etc.
    val environment: Map<String, String>,
    val workspace: Path,
    val artifacts: ArtifactManager
)
```

### Supported Platforms

- **GitLab CI**: Full integration with GitLab-specific features
- **GitHub Actions**: Full integration with GitHub-specific features
- **Local**: For development and testing
- **Generic CI**: Fallback for other CI platforms (Jenkins, CircleCI, etc.)

## Docker Integration

### Using Kite in Docker

```dockerfile
FROM gradle:8-jdk17

# Install Kite
RUN curl -L https://github.com/kite/releases/download/v1.0.0/kite -o /usr/local/bin/kite \
    && chmod +x /usr/local/bin/kite

# Set working directory
WORKDIR /workspace

# Copy project files
COPY . .

# Run Kite
CMD ["kite", "ride", "mr"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  kite:
    image: kite:latest
    volumes:
      - .:/workspace
    environment:
      - CI=true
      - GRADLE_OPTS=-Xmx4g
    command: kite ride mr
```

## Secrets Management

### GitLab CI Secrets

```yaml
# .gitlab-ci.yml
variables:
  KITE_VERSION: "1.0.0"

android-deploy:
  extends: .kite-base
  script:
    - ./kite ride deploy
  variables:
    GOOGLE_SERVICE_ACCOUNT_JSON: $GOOGLE_CREDENTIALS
    SLACK_WEBHOOK: $SLACK_WEBHOOK_URL
  only:
    - main
```

### GitHub Actions Secrets

```yaml
- name: Deploy to Play Store
  run: ./kite ride deploy
  env:
    GOOGLE_SERVICE_ACCOUNT_JSON: ${{ secrets.GOOGLE_CREDENTIALS }}
    SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
```

### Local Secrets

```bash
# .env file (gitignored)
export GOOGLE_SERVICE_ACCOUNT_JSON="path/to/credentials.json"
export SLACK_WEBHOOK="https://hooks.slack.com/..."

# Load and run
source .env
./kite ride deploy
```

## Caching

### GitLab CI Cache

```yaml
android-mr:
  extends: .kite-base
  cache:
    key: ${CI_COMMIT_REF_SLUG}
    paths:
      - .gradle/wrapper
      - .gradle/caches
      - build/
  script:
    - ./kite ride mr
```

### GitHub Actions Cache

```yaml
- name: Cache Gradle
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/wrapper
      ~/.gradle/caches
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
```

## Summary

- **Multi-platform**: GitLab CI, GitHub Actions, local, Docker
- **Auto-detection**: Automatic platform detection
- **Context-aware**: Platform-specific information in execution context
- **Secrets**: Native CI secrets support
- **Caching**: Integrate with platform caching mechanisms
- **Flexible**: Same rides work across all platforms
