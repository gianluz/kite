# Kite Integration Testing Strategy

## Overview

This document outlines the strategy for integration testing Kite. Unlike unit tests that test individual components in
isolation, integration tests verify that the entire system works correctly end-to-end.

---

## Testing Levels

### 1. Unit Tests (Existing) ✅

**Location**: `*/src/test/kotlin/`  
**Coverage**: 175+ tests, excellent coverage (1.21:1 ratio)  
**Scope**: Individual classes and functions

- Core domain models
- DSL builders
- Graph algorithms (DAG, topological sort)
- Schedulers (Sequential, Parallel)
- Process execution

### 2. Integration Tests (To Be Implemented)

**Location**: `kite-cli/src/test/kotlin/integration/`  
**Scope**: End-to-end CLI workflows

**What to Test**:

- Complete ride execution from CLI
- File discovery and loading
- Segment execution with real processes
- Error handling and reporting
- Output formatting
- Platform detection

### 3. Self-Hosting Tests (Kite Testing Kite)

**Location**: `.kite/segments/` and `.kite/rides/`  
**Scope**: Use Kite itself to build and test Kite

---

## Integration Test Strategy

### Approach 1: Programmatic Tests

Test the CLI programmatically by invoking commands and verifying output.

**Advantages**:

- Fast execution
- Easy to write and maintain
- Can test specific scenarios
- Good CI/CD integration

**Example Structure**:

```kotlin
// kite-cli/src/test/kotlin/integration/RideExecutionTest.kt
class RideExecutionTest {
    @Test
    fun `should execute simple ride successfully`() {
        // Given: A test project with segments and a ride
        val testProject = createTestProject {
            segment("build") {
                execute { 
                    File("build.txt").writeText("built")
                }
            }
            
            ride("test-ride") {
                flow {
                    segment("build")
                }
            }
        }
        
        // When: Execute the ride
        val result = KiteCli().test("ride test-ride")
        
        // Then: Verify success
        assert(result.exitCode == 0)
        assert(testProject.file("build.txt").exists())
    }
}
```

### Approach 2: Process-Based Tests

Launch Kite as a separate process and verify behavior through files and exit codes.

**Advantages**:

- Tests the actual CLI as users would use it
- Catches issues with process handling
- More realistic

**Example**:

```kotlin
class ProcessBasedTest {
    @Test
    fun `should execute ride via CLI process`() {
        // Given: Test project directory
        val testDir = Files.createTempDirectory("kite-test")
        createKiteProject(testDir)
        
        // When: Run kite CLI
        val process = ProcessBuilder(
            "./gradlew", ":kite-cli:run",
            "--args=ride test-ride"
        )
            .directory(testDir.toFile())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        
        val exitCode = process.waitFor()
        val output = process.inputStream.bufferedReader().readText()
        
        // Then: Verify
        assert(exitCode == 0)
        assert(output.contains("All segments completed successfully"))
    }
}
```

### Approach 3: Docker-Based Tests

Run tests in Docker containers with controlled environments.

**Advantages**:

- Tests platform adapters (GitLab CI, GitHub Actions)
- Isolated environments
- Can simulate different platforms

**Example**:

```kotlin
class DockerIntegrationTest {
    @Test
    fun `should detect GitLab CI environment`() {
        // Given: Docker container with GitLab CI env vars
        val container = DockerContainer("ubuntu:22.04")
            .withEnv("CI_PROJECT_NAME", "test-project")
            .withEnv("CI_COMMIT_SHA", "abc123")
            .withEnv("CI_COMMIT_BRANCH", "main")
        
        // When: Run Kite
        val result = container.exec("kite", "ride", "test")
        
        // Then: Verify platform detection
        assert(result.output.contains("Platform: GitLabCI"))
    }
}
```

---

## Test Fixtures

### Test Project Builder

Create a reusable test project builder:

```kotlin
class TestProjectBuilder {
    private val segments = mutableListOf<Segment>()
    private val rides = mutableListOf<Ride>()
    
    fun segment(name: String, block: SegmentBuilder.() -> Unit) {
        // Build segment
    }
    
    fun ride(name: String, block: RideBuilder.() -> Unit) {
        // Build ride
    }
    
    fun create(tempDir: Path): TestProject {
        // Write .kite.kts files
        // Return TestProject instance
    }
}
```

### Example Test Projects

Pre-built test projects for common scenarios:

1. **Simple Project**: Single segment, single ride
2. **Dependent Segments**: Chain of dependencies
3. **Parallel Execution**: Parallel segment blocks
4. **Conditional Segments**: Segments with conditions
5. **Error Scenarios**: Failing segments, timeouts, etc.

---

## Test Coverage Goals

### Phase 3 Integration Tests

**Epic 3.1 - CLI Commands**:

- [ ] `kite ride <name>` - Execute ride successfully
- [ ] `kite ride <name>` - Handle missing ride
- [ ] `kite ride <name>` - Handle compilation errors
- [ ] `kite ride <name> --dry-run` - Show plan without execution
- [ ] `kite ride <name> --sequential` - Force sequential execution
- [ ] `kite run <segment>` - Execute single segment
- [ ] `kite run <s1> <s2>` - Execute multiple segments
- [ ] `kite run <segment>` - Include dependencies automatically
- [ ] `kite segments` - List all segments
- [ ] `kite segments --json` - JSON output
- [ ] `kite rides` - List all rides
- [ ] `kite rides --json` - JSON output
- [ ] `kite --debug` - Debug output works
- [ ] `kite --verbose` - Verbose output works
- [ ] `kite --quiet` - Quiet mode works

**Epic 3.2 - File Discovery**:

- [ ] Discover segments from `.kite/segments/**/*.kite.kts`
- [ ] Discover rides from `.kite/rides/**/*.kite.kts`
- [ ] Handle nested directories
- [ ] Handle multiple segments per file
- [ ] Handle compilation errors gracefully
- [ ] Cache compiled scripts

**Epic 3.3 - Execution**:

- [ ] Execute segments in correct order
- [ ] Respect dependencies
- [ ] Execute parallel blocks concurrently
- [ ] Respect max concurrency
- [ ] Handle segment failures
- [ ] Handle timeouts
- [ ] Skip conditional segments
- [ ] Capture output correctly
- [ ] Report results accurately

### Phase 4 Integration Tests

**Platform Adapters**:

- [ ] Detect GitLab CI from environment
- [ ] Detect GitHub Actions from environment
- [ ] Detect local environment
- [ ] Populate context correctly for each platform

### Phase 7 Integration Tests

**End-to-End Scenarios**:

- [ ] Complete MR ride (build → parallel tests → deploy)
- [ ] Complex dependency chain
- [ ] Artifact passing between segments
- [ ] Error recovery and retries
- [ ] Multiple rides in sequence
- [ ] Long-running rides (>5 min)

---

## Implementation Plan

### Step 1: Test Infrastructure (Week 1)

- [ ] Create `TestProjectBuilder` utility
- [ ] Create test fixtures directory
- [ ] Set up temporary directory management
- [ ] Create assertion helpers

### Step 2: Basic CLI Tests (Week 1)

- [ ] Test `kite ride` command
- [ ] Test `kite run` command
- [ ] Test `kite segments` command
- [ ] Test `kite rides` command
- [ ] Test global options (--debug, --verbose, --quiet)

### Step 3: Execution Tests (Week 2)

- [ ] Test simple execution
- [ ] Test dependency resolution
- [ ] Test parallel execution
- [ ] Test error handling
- [ ] Test timeouts
- [ ] Test conditional execution

### Step 4: Docker-Based Tests (Week 2)

- [ ] Set up Docker test environment
- [ ] Test GitLab CI detection
- [ ] Test GitHub Actions detection
- [ ] Test in clean environment

### Step 5: Performance Tests (Week 3)

- [ ] Measure startup time
- [ ] Measure script compilation time
- [ ] Measure execution overhead
- [ ] Benchmark parallel vs sequential

---

## Continuous Integration

### Self-Hosting with Kite

The ultimate integration test: **Use Kite to test Kite!**

```
.kite/
├── segments/
│   ├── build.kite.kts         # Build all modules
│   ├── test.kite.kts           # Run all tests
│   ├── lint.kite.kts           # Code quality checks
│   └── integration.kite.kts    # Integration tests
└── rides/
    ├── mr.kite.kts             # MR validation
    └── release.kite.kts        # Release process
```

**MR Ride**:

```kotlin
ride {
    name = "MR Validation"
    maxConcurrency = 3
    
    flow {
        segment("build")
        
        parallel {
            segment("unit-tests")
            segment("lint")
            segment("ktlint")
            segment("detekt")
        }
        
        segment("integration-tests")
    }
}
```

This provides:

- ✅ Real-world usage of Kite
- ✅ Dogfooding (we use what we build)
- ✅ Integration testing in CI
- ✅ Example for users

---

## Testing Tools

### Recommended Libraries

1. **JUnit 5**: Test framework
2. **Kotest**: Kotlin-friendly assertions
3. **MockK**: Mocking (if needed)
4. **Testcontainers**: Docker integration tests
5. **TmpDir**: JUnit extension for temp directories

### Example Dependencies

```kotlin
// kite-cli/build.gradle.kts
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

---

## Success Criteria

Integration tests are successful when:

✅ All CLI commands work end-to-end  
✅ File discovery loads real `.kite.kts` files  
✅ Segments execute with real processes  
✅ Error handling works correctly  
✅ Platform detection works in Docker  
✅ Kite can build and test itself  
✅ Tests run in < 2 minutes in CI  
✅ Tests are reliable (no flaky tests)

---

## Next Steps

1. **Immediate**: Create test infrastructure
2. **Week 1**: Implement basic CLI tests
3. **Week 2**: Add execution and Docker tests
4. **Week 3**: Set up Kite's own CI/CD using Kite

**Goal**: By end of Phase 3, Kite should be fully tested and self-hosting its own CI/CD! 🎯
