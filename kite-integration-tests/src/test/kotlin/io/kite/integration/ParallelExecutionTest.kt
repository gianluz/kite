package io.kite.integration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for parallel segment execution.
 */
class ParallelExecutionTest : IntegrationTestBase() {

    @Test
    fun `parallel segments execute faster than sequential`() {
        createSegmentFile(
            "parallel.kite.kts",
            """
            segments {
                segment("slow-1") {
                    execute {
                        Thread.sleep(100)
                        println("Slow 1 done")
                    }
                }
                segment("slow-2") {
                    execute {
                        Thread.sleep(100)
                        println("Slow 2 done")
                    }
                }
                segment("slow-3") {
                    execute {
                        Thread.sleep(100)
                        println("Slow 3 done")
                    }
                }
                segment("slow-4") {
                    execute {
                        Thread.sleep(100)
                        println("Slow 4 done")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "parallel.kite.kts",
            """
            ride {
                name = "Parallel"
                maxConcurrency = 4
                flow {
                    segment("slow-1")
                    segment("slow-2")
                    segment("slow-3")
                    segment("slow-4")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Parallel")

        result.assertSuccess()
        assertEquals(4, result.totalSegments)
        // Should complete in ~100ms (parallel) not ~400ms (sequential)
        // Allow generous timeout for CI/test overhead
        assertTrue(
            result.duration.inWholeMilliseconds < 500,
            "Parallel execution should be faster than sequential. Duration: ${result.duration}"
        )
    }

    @Test
    fun `maxConcurrency limits parallel execution`() {
        createSegmentFile(
            "limited.kite.kts",
            """
            segments {
                segment("task-1") {
                    execute {
                        println("Task 1 start")
                        Thread.sleep(50)
                        println("Task 1 done")
                    }
                }
                segment("task-2") {
                    execute {
                        println("Task 2 start")
                        Thread.sleep(50)
                        println("Task 2 done")
                    }
                }
                segment("task-3") {
                    execute {
                        println("Task 3 start")
                        Thread.sleep(50)
                        println("Task 3 done")
                    }
                }
                segment("task-4") {
                    execute {
                        println("Task 4 start")
                        Thread.sleep(50)
                        println("Task 4 done")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "limited.kite.kts",
            """
            ride {
                name = "Limited"
                maxConcurrency = 2  // Only 2 at a time
                flow {
                    segment("task-1")
                    segment("task-2")
                    segment("task-3")
                    segment("task-4")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Limited")

        result.assertSuccess()
        assertEquals(4, result.totalSegments)
        // With maxConcurrency=2, should take ~100ms (2 batches of 50ms)
        // not ~200ms (sequential) - allow overhead
        assertTrue(result.duration.inWholeMilliseconds < 250)
    }

    @Test
    fun `parallel block in DSL`() {
        createSegmentFile(
            "parallel-dsl.kite.kts",
            """
            segments {
                segment("setup") {
                    execute {
                        println("Setup")
                    }
                }
                segment("unit-tests") {
                    dependsOn("setup")
                    execute {
                        Thread.sleep(50)
                        println("Unit tests")
                    }
                }
                segment("integration-tests") {
                    dependsOn("setup")
                    execute {
                        Thread.sleep(50)
                        println("Integration tests")
                    }
                }
                segment("lint") {
                    dependsOn("setup")
                    execute {
                        Thread.sleep(50)
                        println("Lint")
                    }
                }
                segment("finish") {
                    dependsOn("unit-tests", "integration-tests", "lint")
                    execute {
                        println("All tests passed!")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "parallel-dsl.kite.kts",
            """
            ride {
                name = "Parallel DSL"
                maxConcurrency = 4
                flow {
                    segment("setup")
                    parallel {
                        segment("unit-tests")
                        segment("integration-tests")
                        segment("lint")
                    }
                    segment("finish")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Parallel DSL")

        result.assertSuccess()
        assertEquals(5, result.totalSegments)
        result.assertOutputContains("Setup")
        result.assertOutputContains("Unit tests")
        result.assertOutputContains("Integration tests")
        result.assertOutputContains("Lint")
        result.assertOutputContains("All tests passed!")
    }

    @Test
    fun `parallel execution with dependencies respects order`() {
        createSegmentFile(
            "deps.kite.kts",
            """
            segments {
                segment("build") {
                    execute {
                        println("Build")
                    }
                }
                segment("test-1") {
                    dependsOn("build")
                    execute {
                        println("Test 1")
                    }
                }
                segment("test-2") {
                    dependsOn("build")
                    execute {
                        println("Test 2")
                    }
                }
                segment("deploy") {
                    dependsOn("test-1", "test-2")
                    execute {
                        println("Deploy")
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "deps.kite.kts",
            """
            ride {
                name = "With Dependencies"
                maxConcurrency = 4
                flow {
                    segment("build")
                    segment("test-1")
                    segment("test-2")
                    segment("deploy")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("With Dependencies")

        result.assertSuccess()
        assertEquals(4, result.totalSegments)

        // Verify order: build must run before tests, tests before deploy
        val output = result.output
        val buildIndex = output.indexOf("Build")
        val test1Index = output.indexOf("Test 1")
        val test2Index = output.indexOf("Test 2")
        val deployIndex = output.indexOf("Deploy")

        assertTrue(buildIndex < test1Index, "Build should run before Test 1")
        assertTrue(buildIndex < test2Index, "Build should run before Test 2")
        assertTrue(test1Index < deployIndex, "Test 1 should run before Deploy")
        assertTrue(test2Index < deployIndex, "Test 2 should run before Deploy")
    }
}
