package io.kite.integration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for error handling and failure scenarios.
 */
class ErrorHandlingTest : IntegrationTestBase() {

    @Test
    fun `exception in segment marks it as failed`() {
        createSegmentFile(
            "exception.kite.kts",
            """
            segments {
                segment("throws-exception") {
                    execute {
                        println("Before exception")
                        throw RuntimeException("Something went wrong!")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "exception.kite.kts",
            """
            ride {
                name = "Exception"
                flow {
                    segment("throws-exception")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Exception")

        assertTrue(!result.success, "Ride should fail when segment throws exception")
        assertEquals(1, result.failureCount)
        result.assertSegmentFailed("throws-exception")
        result.assertOutputContains("Before exception")
    }

    @Test
    fun `error function fails segment`() {
        createSegmentFile(
            "error-call.kite.kts",
            """
            segments {
                segment("calls-error") {
                    execute {
                        println("About to fail")
                        error("Explicit error message")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "error-call.kite.kts",
            """
            ride {
                name = "Error Call"
                flow {
                    segment("calls-error")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Error Call")

        assertTrue(!result.success)
        result.assertSegmentFailed("calls-error")
        result.assertOutputContains("About to fail")
    }

    @Test
    fun `failed segment skips dependents`() {
        createSegmentFile(
            "cascade-failure.kite.kts",
            """
            segments {
                segment("setup") {
                    execute {
                        println("Setup complete")
                    }
                }
                segment("build") {
                    dependsOn("setup")
                    execute {
                        println("Building")
                        error("Build failed!")
                    }
                }
                segment("test") {
                    dependsOn("build")
                    execute {
                        println("Testing - should not run")
                    }
                }
                segment("deploy") {
                    dependsOn("test")
                    execute {
                        println("Deploying - should not run")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "cascade.kite.kts",
            """
            ride {
                name = "Cascade Failure"
                flow {
                    segment("setup")
                    segment("build")
                    segment("test")
                    segment("deploy")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Cascade Failure")

        assertTrue(!result.success)
        result.assertSegmentSucceeded("setup")
        result.assertSegmentFailed("build")
        // test and deploy should be skipped, not failed
        result.assertOutputContains("Setup complete")
        result.assertOutputContains("Building")
        assertTrue(!result.output.contains("Testing"), "Test should not run")
        assertTrue(!result.output.contains("Deploying"), "Deploy should not run")
    }

    @Test
    fun `partial ride execution stops on first failure`() {
        createSegmentFile(
            "partial.kite.kts",
            """
            segments {
                segment("step-1") {
                    execute {
                        println("Step 1 done")
                    }
                }
                segment("step-2") {
                    execute {
                        println("Step 2 done")
                    }
                }
                segment("step-3-fails") {
                    execute {
                        println("Step 3 starting")
                        error("Step 3 failed")
                    }
                }
                segment("step-4") {
                    execute {
                        println("Step 4 - should not run")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "partial.kite.kts",
            """
            ride {
                name = "Partial"
                maxConcurrency = 1  // Sequential to ensure order
                flow {
                    segment("step-1")
                    segment("step-2")
                    segment("step-3-fails")
                    segment("step-4")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Partial")

        assertTrue(!result.success)
        result.assertOutputContains("Step 1 done")
        result.assertOutputContains("Step 2 done")
        result.assertOutputContains("Step 3 starting")
    }

    @Test
    fun `one failure in parallel doesn't stop others`() {
        createSegmentFile(
            "parallel-failure.kite.kts",
            """
            segments {
                segment("success-1") {
                    execute {
                        Thread.sleep(50)
                        println("Success 1")
                    }
                }
                segment("fails") {
                    execute {
                        Thread.sleep(25)
                        println("About to fail")
                        error("Failed!")
                    }
                }
                segment("success-2") {
                    execute {
                        Thread.sleep(50)
                        println("Success 2")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "parallel-failure.kite.kts",
            """
            ride {
                name = "Parallel Failure"
                maxConcurrency = 3
                flow {
                    segment("success-1")
                    segment("fails")
                    segment("success-2")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Parallel Failure")

        assertTrue(!result.success)
        assertEquals(2, result.successCount)
        assertEquals(1, result.failureCount)
        result.assertOutputContains("Success 1")
        result.assertOutputContains("About to fail")
        result.assertOutputContains("Success 2")
    }
}
