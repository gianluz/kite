package io.kite.integration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for basic ride execution.
 */
class BasicRideExecutionTest : IntegrationTestBase() {
    @Test
    fun `execute simple ride with single segment`() {
        // Create a simple segment
        createSegmentFile(
            "hello.kite.kts",
            """
            segments {
                segment("hello") {
                    execute {
                        println("Hello from Kite!")
                    }
                }
            }
            """.trimIndent(),
        )

        // Create a ride
        createRideFile(
            "test.kite.kts",
            """
            ride {
                name = "Test"
                flow {
                    segment("hello")
                }
            }
            """.trimIndent(),
        )

        // Execute
        val result = executeRide("Test")

        // Assert
        result.assertSuccess()
        assertEquals(1, result.totalSegments)
        assertEquals(1, result.successCount)
        result.assertOutputContains("Hello from Kite!")
    }

    @Test
    fun `execute ride with multiple segments`() {
        createSegmentFile(
            "multi.kite.kts",
            """
            segments {
                segment("first") {
                    execute {
                        println("First segment")
                    }
                }
                segment("second") {
                    execute {
                        println("Second segment")
                    }
                }
                segment("third") {
                    execute {
                        println("Third segment")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "multi.kite.kts",
            """
            ride {
                name = "Multi"
                flow {
                    segment("first")
                    segment("second")
                    segment("third")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Multi")

        result.assertSuccess()
        assertEquals(3, result.totalSegments)
        assertEquals(3, result.successCount)
        result.assertOutputContains("First segment")
        result.assertOutputContains("Second segment")
        result.assertOutputContains("Third segment")
    }

    @Test
    fun `execute ride with dependencies`() {
        createSegmentFile(
            "deps.kite.kts",
            """
            segments {
                segment("setup") {
                    execute {
                        println("Setup")
                    }
                }
                segment("build") {
                    dependsOn("setup")
                    execute {
                        println("Build")
                    }
                }
                segment("test") {
                    dependsOn("build")
                    execute {
                        println("Test")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "deps.kite.kts",
            """
            ride {
                name = "Dependencies"
                flow {
                    segment("setup")
                    segment("build")
                    segment("test")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Dependencies")

        result.assertSuccess()
        assertEquals(3, result.totalSegments) // setup, build, test
        result.assertSegmentSucceeded("setup")
        result.assertSegmentSucceeded("build")
        result.assertSegmentSucceeded("test")
        // Verify order via output
        result.assertOutputContains("Setup")
        result.assertOutputContains("Build")
        result.assertOutputContains("Test")
    }

    @Test
    fun `execute ride with parallel segments`() {
        createSegmentFile(
            "parallel.kite.kts",
            """
            segments {
                segment("unit-tests") {
                    execute {
                        println("Running unit tests")
                    }
                }
                segment("lint") {
                    execute {
                        println("Running lint")
                    }
                }
                segment("compile") {
                    execute {
                        println("Compiling")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "parallel.kite.kts",
            """
            ride {
                name = "Parallel"
                maxConcurrency = 3
                flow {
                    segment("unit-tests")
                    segment("lint")
                    segment("compile")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Parallel")

        result.assertSuccess()
        assertEquals(3, result.totalSegments)
        assertTrue(result.duration.inWholeMilliseconds < 5000, "Should complete quickly in parallel")
    }

    @Test
    fun `failed segment stops ride execution`() {
        createSegmentFile(
            "failing.kite.kts",
            """
            segments {
                segment("good") {
                    execute {
                        println("Good segment")
                    }
                }
                segment("bad") {
                    execute {
                        error("Intentional failure")
                    }
                }
                segment("never-runs") {
                    dependsOn("bad")
                    execute {
                        println("This should not run")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "failing.kite.kts",
            """
            ride {
                name = "Failing"
                flow {
                    segment("good")
                    segment("bad")
                    segment("never-runs")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Failing")

        assertTrue(!result.success, "Ride should fail")
        result.assertSegmentSucceeded("good")
        result.assertSegmentFailed("bad")
        // never-runs won't be in results since its dependency failed
    }
}
