# Kite Integration Tests

This module contains integration tests that execute full Kite workflows end-to-end.

## Overview

Unlike unit tests that test individual components in isolation, these integration tests:

- Execute real `.kite.kts` scripts
- Run full ride workflows
- Test segment execution, dependencies, and parallelism
- Validate external dependency resolution (`@DependsOn`)
- Capture and assert on execution output

## Running Tests

```bash
# Run all integration tests
./gradlew :kite-integration-tests:test

# Run specific test class
./gradlew :kite-integration-tests:test --tests "BasicRideExecutionTest"

# Run specific test
./gradlew :kite-integration-tests:test --tests "BasicRideExecutionTest.execute simple ride with single segment"
```

## Test Structure

### `IntegrationTestBase`

Base class providing utilities for integration tests:

- Temporary workspace creation
- Segment and ride file creation
- Ride execution with output capture
- Result assertions

### Test Classes

- **`BasicRideExecutionTest`**: Tests basic ride execution, dependencies, parallelism
- **`ExternalDependenciesTest`**: Tests `@DependsOn` annotation with Ivy resolver

## Writing Integration Tests

Example test:

```kotlin
class MyIntegrationTest : IntegrationTestBase() {
    
    @Test
    fun `test my workflow`() {
        // Create segments
        createSegmentFile("build.kite.kts", """
            segments {
                segment("build") {
                    execute {
                        println("Building...")
                    }
                }
            }
        """.trimIndent())
        
        // Create ride
        createRideFile("ci.kite.kts", """
            ride {
                name = "CI"
                flow {
                    segment("build")
                }
            }
        """.trimIndent())
        
        // Execute and assert
        val result = executeRide("CI")
        result.assertSuccess()
        result.assertOutputContains("Building...")
    }
}
```

## Benefits

1. **Real-world validation**: Tests actual script execution
2. **Regression prevention**: Catches breaking changes to DSL or execution engine
3. **Documentation**: Serves as executable examples
4. **CI integration**: Runs automatically on every build
5. **Fast feedback**: Runs in ~5 seconds

## Test Isolation

Each test gets a fresh temporary workspace (`@TempDir`), ensuring:

- No cross-test pollution
- Clean slate for each test
- Predictable test behavior

## Assertions

The `RideExecutionResult` provides rich assertions:

- `assertSuccess()` - Verifies ride succeeded
- `assertSegmentSucceeded(name)` - Verifies specific segment succeeded
- `assertSegmentFailed(name)` - Verifies specific segment failed
- `assertOutputContains(text)` - Verifies output contains text
- `assertErrorContains(text)` - Verifies error output contains text

## Running on CI

Integration tests are automatically run as part of the CI ride:

```kotlin
// .kite/rides/ci.kite.kts
ride {
    name = "CI"
    flow {
        segment("compile")
        parallel {
            segment("test-core")
            segment("test-dsl")
            segment("test-runtime")
            segment("test-cli")
            segment("test-integration")  // ← Integration tests!
        }
        segment("build")
    }
}
```

## Future Enhancements

Potential improvements:

- Performance benchmarking tests
- Cross-platform execution tests
- Error recovery tests
- Large workflow stress tests
- Real-world scenario simulations
