# CI/CD Integration

Learn how to integrate Kite into your CI/CD pipelines for automated workflows.

---

## Overview

Kite integrates seamlessly with popular CI/CD platforms:

- **GitHub Actions** - Native integration with workflow files
- **GitLab CI** - Pipeline configuration with `.gitlab-ci.yml`
- **Jenkins** - Pipeline and freestyle jobs
- **CircleCI** - Config-based workflows
- **Any CI/CD** - Standard command-line interface

All you need is:

1. A Java 17+ environment
2. Your `.kite/` directory with segments and rides
3. Run `kite-cli ride <ride-name>`

---

## Quick Start

### Basic CI Workflow

```bash
# 1. Install Kite
./gradlew :kite-cli:installDist

# 2. Run a ride
kite-cli/build/install/kite-cli/bin/kite-cli ride CI

# That's it!
```

---

## GitHub Actions

### ⚡ Quick Start with Kite Actions (Recommended)

**The easiest way** to use Kite in GitHub Actions is with our pre-built actions:

```yaml
name: CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      # One step to rule them all!
      - name: Run CI Workflow
        uses: ./.github/actions/run-kite
        with:
          command: ride CI
```

**That's it!** The action handles:

- ✅ Java installation
- ✅ Building Kite from source
- ✅ Caching for fast subsequent runs (~10 sec instead of 2-3 min)
- ✅ Running your workflow

**See [GitHub Actions documentation](.github/actions/README.md) for complete details.**

---

### Manual Setup (Alternative)

If you prefer manual control, you can set up Kite yourself:

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    steps:
      # 1. Checkout code
      - name: Checkout
        uses: actions/checkout@v4
      
      # 2. Setup Java
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      # 3. Setup Gradle (with caching)
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      
      # 4. Build Kite CLI
      - name: Build Kite
        run: ./gradlew :kite-cli:installDist
      
      # 5. Run your ride
      - name: Run CI Ride
        run: kite-cli/build/install/kite-cli/bin/kite-cli ride CI
      
      # 6. Upload artifacts
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: .kite/artifacts/
          retention-days: 7
```

**Note:** The manual approach requires building Kite on every run. Use the action for better caching!

### With Secrets

```yaml
- name: Run Deployment
  env:
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: kite-cli/build/install/kite-cli/bin/kite-cli ride Deploy
```

**Your Kite segments automatically access these via `secret()`:**

```kotlin
segment("deploy") {
    execute {
        val awsKey = requireSecret("AWS_ACCESS_KEY_ID")
        val awsSecret = requireSecret("AWS_SECRET_ACCESS_KEY")
        
        // Secrets are automatically masked in logs
        exec("aws", "s3", "cp", "app.jar", "s3://bucket/")
    }
}
```

### Matrix Builds

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, macos-latest, windows-latest]
    java-version: ['17', '21']

steps:
  - uses: actions/checkout@v4
  
  - name: Set up Java ${{ matrix.java-version }}
    uses: actions/setup-java@v4
    with:
      distribution: 'temurin'
      java-version: ${{ matrix.java-version }}
  
  - name: Build Kite
    run: ./gradlew :kite-cli:installDist
  
  - name: Run Tests
    run: kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

### Caching Dependencies

```yaml
- name: Cache Gradle
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: |
      ${{ runner.os }}-gradle-

- name: Cache Ivy (for @DependsOn)
  uses: actions/cache@v3
  with:
    path: ~/.ivy2/cache
    key: ${{ runner.os }}-ivy-${{ hashFiles('**/*.kite.kts') }}
    restore-keys: |
      ${{ runner.os }}-ivy-
```

---

## GitLab CI

### Complete Example

```yaml
# .gitlab-ci.yml
image: eclipse-temurin:17-jdk

stages:
  - build
  - test
  - deploy

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"

before_script:
  - export GRADLE_USER_HOME=`pwd`/.gradle

# Cache Gradle dependencies
cache:
  paths:
    - .gradle/
    - .ivy2/

build:
  stage: build
  script:
    - ./gradlew :kite-cli:installDist
    - kite-cli/build/install/kite-cli/bin/kite-cli ride Build
  artifacts:
    paths:
      - .kite/artifacts/
    expire_in: 1 week

test:
  stage: test
  script:
    - ./gradlew :kite-cli:installDist
    - kite-cli/build/install/kite-cli/bin/kite-cli ride Test
  dependencies:
    - build
  artifacts:
    reports:
      junit: .kite/artifacts/test-results-*/*.xml

deploy:
  stage: deploy
  script:
    - ./gradlew :kite-cli:installDist
    - kite-cli/build/install/kite-cli/bin/kite-cli ride Deploy
  only:
    - main
  environment:
    name: production
```

### With Secrets

```yaml
deploy:
  stage: deploy
  script:
    - ./gradlew :kite-cli:installDist
    - kite-cli/build/install/kite-cli/bin/kite-cli ride Deploy
  variables:
    AWS_ACCESS_KEY_ID: $CI_AWS_ACCESS_KEY_ID
    AWS_SECRET_ACCESS_KEY: $CI_AWS_SECRET_ACCESS_KEY
  only:
    - main
```

GitLab automatically masks variables defined in CI/CD settings.

---

## Jenkins

### Declarative Pipeline

```groovy
pipeline {
    agent any
    
    environment {
        JAVA_HOME = tool 'JDK17'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Kite') {
            steps {
                sh './gradlew :kite-cli:installDist'
            }
        }
        
        stage('Run Tests') {
            steps {
                sh 'kite-cli/build/install/kite-cli/bin/kite-cli ride CI'
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh 'kite-cli/build/install/kite-cli/bin/kite-cli ride Deploy'
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: '.kite/artifacts/**/*', allowEmptyArchive: true
            junit '.kite/artifacts/test-results-*/*.xml'
        }
    }
}
```

### Freestyle Project

**Build Steps:**

1. Add build step: "Execute shell"
2. Command:

```bash
./gradlew :kite-cli:installDist
kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

**Post-build Actions:**

- Archive artifacts: `.kite/artifacts/**/*`
- Publish JUnit test results: `.kite/artifacts/test-results-*/*.xml`

---

## CircleCI

### Complete Example

```yaml
# .circleci/config.yml
version: 2.1

orbs:
  gradle: circleci/gradle@3.0

jobs:
  build-and-test:
    docker:
      - image: cimg/openjdk:17.0
    
    steps:
      - checkout
      
      - restore_cache:
          keys:
            - gradle-{{ checksum "build.gradle.kts" }}
            - gradle-
      
      - restore_cache:
          keys:
            - ivy-{{ checksum ".kite/**/*.kite.kts" }}
            - ivy-
      
      - run:
          name: Build Kite CLI
          command: ./gradlew :kite-cli:installDist
      
      - run:
          name: Run CI Ride
          command: kite-cli/build/install/kite-cli/bin/kite-cli ride CI
      
      - save_cache:
          paths:
            - ~/.gradle
          key: gradle-{{ checksum "build.gradle.kts" }}
      
      - save_cache:
          paths:
            - ~/.ivy2/cache
          key: ivy-{{ checksum ".kite/**/*.kite.kts" }}
      
      - store_test_results:
          path: .kite/artifacts/test-results-*/
      
      - store_artifacts:
          path: .kite/artifacts/
          destination: test-artifacts

workflows:
  version: 2
  build-and-deploy:
    jobs:
      - build-and-test
```

---

## Travis CI

```yaml
# .travis.yml
language: java
jdk:
  - openjdk17

cache:
  directories:
    - $HOME/.gradle/caches/
    - $HOME/.gradle/wrapper/
    - $HOME/.ivy2/cache/

install:
  - ./gradlew :kite-cli:installDist

script:
  - kite-cli/build/install/kite-cli/bin/kite-cli ride CI

after_success:
  - if [ "$TRAVIS_BRANCH" == "main" ]; then
      kite-cli/build/install/kite-cli/bin/kite-cli ride Deploy;
    fi
```

---

## Docker

### Running Kite in Docker

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy project files
COPY . .

# Build Kite CLI
RUN ./gradlew :kite-cli:installDist

# Run ride
CMD ["kite-cli/build/install/kite-cli/bin/kite-cli", "ride", "CI"]
```

### Build and Run

```bash
docker build -t myproject-ci .
docker run --rm myproject-ci
```

### With Environment Variables

```bash
docker run --rm \
  -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
  myproject-ci
```

---

## Best Practices

### 1. Use Dedicated Rides for CI

```kotlin
// .kite/rides/ci.kite.kts
ride {
    name = "CI"
    maxConcurrency = 4
    
    flow {
        segment("clean")
        segment("compile")
        
        parallel {
            segment("unit-tests")
            segment("integration-tests")
            segment("lint")
        }
    }
}
```

### 2. Separate PR and Main Branch Workflows

```kotlin
// .kite/rides/pr.kite.kts
ride {
    name = "PR"
    
    flow {
        segment("quick-tests")  // Faster feedback
        segment("lint")
    }
}

// .kite/rides/main.kite.kts
ride {
    name = "Main"
    
    flow {
        segment("full-tests")   // Complete test suite
        segment("build")
        segment("deploy")
    }
}
```

**GitHub Actions:**

```yaml
on:
  pull_request:
    # Fast feedback on PRs
    - run: kite-cli ride PR
  
  push:
    branches: [main]
    # Full suite on main
    - run: kite-cli ride Main
```

### 3. Use Conditions for Branch-Specific Logic

```kotlin
segment("deploy-production") {
    condition = { ctx ->
        ctx.env("BRANCH") == "main" ||
        ctx.env("GITHUB_REF") == "refs/heads/main"
    }
    
    execute {
        val token = requireSecret("DEPLOY_TOKEN")
        exec("./deploy.sh", "--prod")
    }
}
```

### 4. Cache Dependencies

**Gradle:**

```yaml
- uses: gradle/actions/setup-gradle@v3  # Automatic caching
```

**Ivy (for @DependsOn):**

```yaml
- uses: actions/cache@v3
  with:
    path: ~/.ivy2/cache
    key: ivy-${{ hashFiles('**/*.kite.kts') }}
```

### 5. Fail Fast with Parallel Execution

```kotlin
ride {
    name = "CI"
    maxConcurrency = 3  # Run multiple checks in parallel
    
    flow {
        segment("compile")
        
        parallel {
            segment("unit-tests")      # If any fail,
            segment("lint")            # stop immediately
            segment("security-scan")
        }
    }
}
```

### 6. Upload Artifacts

```yaml
- name: Upload Test Results
  if: always()  # Upload even on failure
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: .kite/artifacts/
```

### 7. Set Resource Limits

```kotlin
ride {
    name = "CI"
    
    // GitHub runners: 2 cores, 7GB RAM
    maxConcurrency = 2
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g")
    }
}
```

---

## Environment Variables

### Common CI Variables

Kite segments can access standard CI environment variables:

**GitHub Actions:**

```kotlin
segment("info") {
    execute {
        val branch = env("GITHUB_REF")
        val commit = env("GITHUB_SHA")
        val pr = env("GITHUB_PULL_REQUEST")
        val actor = env("GITHUB_ACTOR")
        
        logger.info("Building $branch at $commit by $actor")
    }
}
```

**GitLab CI:**

```kotlin
segment("info") {
    execute {
        val branch = env("CI_COMMIT_BRANCH")
        val commit = env("CI_COMMIT_SHA")
        val pipeline = env("CI_PIPELINE_ID")
        
        logger.info("Pipeline $pipeline: $branch at $commit")
    }
}
```

**Jenkins:**

```kotlin
segment("info") {
    execute {
        val branch = env("BRANCH_NAME")
        val build = env("BUILD_NUMBER")
        val job = env("JOB_NAME")
        
        logger.info("Build #$build of $job on $branch")
    }
}
```

---

## Notifications

### Slack Notifications

```kotlin
segment("notify-slack") {
    execute {
        val slackWebhook = requireSecret("SLACK_WEBHOOK_URL")
        val status = env("BUILD_STATUS") ?: "unknown"
        
        val message = """{
            "text": "Build $status",
            "blocks": [{
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": "*Build Status*: $status\n*Branch*: ${env("BRANCH")}"
                }
            }]
        }""".trimIndent()
        
        exec("curl", "-X", "POST",
            "-H", "Content-Type: application/json",
            "-d", message,
            slackWebhook
        )
    }
}
```

### Email Notifications (via CI)

**GitHub Actions:**

```yaml
- name: Send failure notification
  if: failure()
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: smtp.gmail.com
    server_port: 465
    username: ${{ secrets.MAIL_USERNAME }}
    password: ${{ secrets.MAIL_PASSWORD }}
    subject: Build Failed - ${{ github.repository }}
    body: Build failed on ${{ github.ref }}
    to: team@example.com
```

---

## Troubleshooting

### Problem: "kite-cli: command not found"

**Cause:** Kite CLI not built or not in PATH

**Solution:**

```yaml
# Ensure you build first
- run: ./gradlew :kite-cli:installDist

# Use full path
- run: kite-cli/build/install/kite-cli/bin/kite-cli ride CI
```

### Problem: "Permission denied" on Unix

**Cause:** `gradlew` not executable

**Solution:**

```yaml
- run: chmod +x gradlew
- run: ./gradlew :kite-cli:installDist
```

Or:

```yaml
- run: sh gradlew :kite-cli:installDist
```

### Problem: Tests fail in CI but pass locally

**Causes:**

1. Different environment variables
2. Missing secrets
3. Resource constraints (CI runners have less RAM/CPU)

**Solutions:**

```kotlin
// 1. Check environment
segment("debug-env") {
    execute {
        logger.info("CI: ${env("CI")}")
        logger.info("Branch: ${env("BRANCH")}")
    }
}

// 2. Adjust resources for CI
ride {
    name = "CI"
    maxConcurrency = 2  // Lower for CI runners
    
    environment {
        put("GRADLE_OPTS", "-Xmx2g")  // Less memory
    }
}
```

### Problem: Slow CI builds

**Solutions:**

1. **Enable caching:**

```yaml
- uses: gradle/actions/setup-gradle@v3  # Auto-caching
```

2. **Reduce parallelism:**

```kotlin
ride {
    maxConcurrency = 2  // Slower hardware
}
```

3. **Split ride into stages:**

```kotlin
// Fast feedback first
segment("quick-tests")   # 30s
segment("lint")          # 30s

// Slower tests later
segment("integration-tests")  # 5m
```

---

## Complete Real-World Example

### Project Structure

```
.
├── .github/
│   └── workflows/
│       ├── pr.yml
│       └── main.yml
├── .kite/
│   ├── segments/
│   │   ├── build.kite.kts
│   │   ├── test.kite.kts
│   │   └── deploy.kite.kts
│   └── rides/
│       ├── pr.kite.kts
│       └── main.kite.kts
└── build.gradle.kts
```

### PR Workflow (.github/workflows/pr.yml)

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
      
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - uses: gradle/actions/setup-gradle@v3
      
      - run: ./gradlew :kite-cli:installDist
      
      - name: Run PR Checks
        run: kite-cli/build/install/kite-cli/bin/kite-cli ride PR
      
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: .kite/artifacts/
```

### Main Branch Workflow (.github/workflows/main.yml)

```yaml
name: Main Branch

on:
  push:
    branches: [ main ]

jobs:
  build-test-deploy:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - uses: gradle/actions/setup-gradle@v3
      
      - run: ./gradlew :kite-cli:installDist
      
      - name: Run Full Pipeline
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        run: kite-cli/build/install/kite-cli/bin/kite-cli ride Main
      
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: artifacts
          path: .kite/artifacts/
```

---

## Summary

**Kite works with any CI/CD platform that supports:**

- ✅ Java 17+
- ✅ Command-line execution
- ✅ Environment variables

**Integration steps:**

1. Build Kite CLI: `./gradlew :kite-cli:installDist`
2. Run your ride: `kite-cli/build/install/kite-cli/bin/kite-cli ride <name>`
3. Upload artifacts (optional)

**Best practices:**

- Use dedicated CI rides
- Cache dependencies (Gradle + Ivy)
- Set appropriate resource limits
- Fail fast with parallel execution
- Upload test results and artifacts

**Secrets are automatically masked** when using `secret()` or `requireSecret()` in your segments!

---

## Related Topics

- [Writing Rides](05-writing-rides.md) - Create CI-specific rides
- [Secrets](09-secrets.md) - Secure secret handling in CI
- [Parallel Execution](07-parallel-execution.md) - Optimize CI performance

---

## Next Steps

- [Explore CLI reference →](12-cli-reference.md)
- [Troubleshooting guide →](99-troubleshooting.md)
