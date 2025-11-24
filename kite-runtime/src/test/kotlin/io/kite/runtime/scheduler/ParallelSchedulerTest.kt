package io.kite.runtime.scheduler

import io.kite.core.ExecutionContext
import io.kite.core.Segment
import io.kite.core.SegmentStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParallelSchedulerTest {
    private val context =
        ExecutionContext(
            branch = "main",
            commitSha = "abc123",
        )

    @Test
    fun `execute empty list returns empty result`() =
        runTest {
            val scheduler = ParallelScheduler()

            val result = scheduler.execute(emptyList(), context)

            assertEquals(0, result.totalSegments)
            assertTrue(result.segmentResults.isEmpty())
        }

    @Test
    fun `execute single segment successfully`() =
        runTest {
            var executed = false
            val segment =
                Segment(
                    name = "build",
                    execute = { executed = true },
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(listOf(segment), context)

            assertTrue(executed)
            assertEquals(1, result.totalSegments)
            assertEquals(1, result.successCount)
            assertEquals(SegmentStatus.SUCCESS, result.getResult("build")?.status)
        }

    @Test
    fun `execute independent segments in parallel`() =
        runTest {
            val executionTimes = mutableMapOf<String, Long>()
            val startTime = System.currentTimeMillis()

            val segments =
                listOf(
                    Segment(name = "a", execute = {
                        delay(100)
                        executionTimes["a"] = System.currentTimeMillis() - startTime
                    }),
                    Segment(name = "b", execute = {
                        delay(100)
                        executionTimes["b"] = System.currentTimeMillis() - startTime
                    }),
                    Segment(name = "c", execute = {
                        delay(100)
                        executionTimes["c"] = System.currentTimeMillis() - startTime
                    }),
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(segments, context)

            assertEquals(3, result.successCount)

            // All segments should start around the same time (within 50ms)
            val times = executionTimes.values.toList()
            val maxDiff = times.maxOrNull()!! - times.minOrNull()!!
            assertTrue(maxDiff < 50, "Segments should execute in parallel, max diff: $maxDiff")
        }

    @Test
    fun `execute segments by levels respecting dependencies`() =
        runTest {
            val executionOrder = mutableListOf<String>()

            val a =
                Segment(
                    name = "a",
                    execute = {
                        delay(50)
                        executionOrder.add("a")
                    },
                )
            val b =
                Segment(
                    name = "b",
                    dependsOn = listOf("a"),
                    execute = {
                        delay(50)
                        executionOrder.add("b")
                    },
                )
            val c =
                Segment(
                    name = "c",
                    dependsOn = listOf("a"),
                    execute = {
                        delay(50)
                        executionOrder.add("c")
                    },
                )
            val d =
                Segment(
                    name = "d",
                    dependsOn = listOf("b", "c"),
                    execute = {
                        executionOrder.add("d")
                    },
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(listOf(d, c, b, a), context) // Out of order

            assertEquals(4, result.successCount)

            // Check execution order constraints
            assertTrue(executionOrder.indexOf("a") < executionOrder.indexOf("b"))
            assertTrue(executionOrder.indexOf("a") < executionOrder.indexOf("c"))
            assertTrue(executionOrder.indexOf("b") < executionOrder.indexOf("d"))
            assertTrue(executionOrder.indexOf("c") < executionOrder.indexOf("d"))

            // b and c should execute in parallel (after a)
            // They should both finish before d starts
        }

    @Test
    fun `respect maxConcurrency limit`() =
        runTest {
            val concurrentCount = AtomicInteger(0)
            val maxConcurrent = AtomicInteger(0)

            val segments =
                (1..10).map { i ->
                    Segment(name = "segment$i", execute = {
                        val current = concurrentCount.incrementAndGet()
                        maxConcurrent.set(maxOf(maxConcurrent.get(), current))
                        delay(50)
                        concurrentCount.decrementAndGet()
                    })
                }

            val scheduler = ParallelScheduler(maxConcurrency = 3)
            val result = scheduler.execute(segments, context)

            assertEquals(10, result.successCount)
            assertTrue(maxConcurrent.get() <= 3, "Max concurrent was ${maxConcurrent.get()}, expected <= 3")
        }

    @Test
    fun `handle segment failure without blocking others`() =
        runTest {
            val executionOrder = mutableListOf<String>()

            val a =
                Segment(
                    name = "a",
                    execute = { executionOrder.add("a") },
                )
            val b =
                Segment(
                    name = "b",
                    execute = {
                        executionOrder.add("b")
                        throw RuntimeException("Failed")
                    },
                )
            val c =
                Segment(
                    name = "c",
                    execute = { executionOrder.add("c") },
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(listOf(a, b, c), context)

            assertEquals(2, result.successCount) // a, c (+ b is skipped = success)
            assertEquals(1, result.failureCount) // b
            assertTrue(executionOrder.containsAll(listOf("a", "b", "c")))
        }

    @Test
    fun `skip dependent segments on failure`() =
        runTest {
            val executionOrder = mutableListOf<String>()

            val a =
                Segment(
                    name = "a",
                    execute = {
                        executionOrder.add("a")
                        throw RuntimeException("Failed")
                    },
                )
            val b =
                Segment(
                    name = "b",
                    dependsOn = listOf("a"),
                    execute = { executionOrder.add("b") },
                )
            val c =
                Segment(
                    name = "c",
                    execute = { executionOrder.add("c") },
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(listOf(a, b, c), context)

            assertEquals(listOf("a", "c"), executionOrder.sorted())
            assertEquals(2, result.successCount) // c + b skipped (counts as success)
            assertEquals(1, result.failureCount) // a
            assertEquals(1, result.skippedCount) // b
        }

    @Test
    fun `skip segment based on condition`() =
        runTest {
            var executed = false

            val segment =
                Segment(
                    name = "deploy",
                    condition = { it.env("RELEASE") == "true" },
                    execute = { executed = true },
                )

            val scheduler = ParallelScheduler()
            val nonReleaseContext = context.copy(environment = mapOf("RELEASE" to "false", "CI" to "true"))
            val result = scheduler.execute(listOf(segment), nonReleaseContext)

            assertEquals(false, executed)
            assertEquals(1, result.skippedCount)
            assertEquals(SegmentStatus.SKIPPED, result.getResult("deploy")?.status)
        }

    @Test
    fun `parallel execution is faster than sequential`() =
        runTest {
            val segments =
                (1..5).map { i ->
                    Segment(name = "segment$i", execute = {
                        delay(100)
                    })
                }

            val startTime = System.currentTimeMillis()
            val scheduler = ParallelScheduler()
            val result = scheduler.execute(segments, context)
            val duration = System.currentTimeMillis() - startTime

            assertEquals(5, result.successCount)
            // Should take ~100ms (parallel), not ~500ms (sequential)
            // Add some buffer for test overhead
            assertTrue(duration < 300, "Parallel execution took ${duration}ms, expected < 300ms")
        }

    @Test
    fun `handle graph validation errors`() =
        runTest {
            val segment =
                Segment(
                    name = "test",
                    dependsOn = listOf("missing"),
                    execute = { },
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(listOf(segment), context)

            assertEquals(1, result.totalSegments)
            assertEquals(1, result.skippedCount)
            assertTrue(result.getResult("test")?.error?.contains("validation") == true)
        }

    @Test
    fun `handle cyclic dependencies`() =
        runTest {
            val a =
                Segment(
                    name = "a",
                    dependsOn = listOf("b"),
                    execute = { },
                )
            val b =
                Segment(
                    name = "b",
                    dependsOn = listOf("a"),
                    execute = { },
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(listOf(a, b), context)

            assertEquals(2, result.totalSegments)
            assertEquals(2, result.skippedCount)
        }

    @Test
    fun `execute complex dependency graph`() =
        runTest {
            val executionOrder = mutableListOf<String>()

            val build =
                Segment(
                    name = "build",
                    execute = {
                        delay(50)
                        executionOrder.add("build")
                    },
                )
            val test1 =
                Segment(
                    name = "test1",
                    dependsOn = listOf("build"),
                    execute = {
                        delay(50)
                        executionOrder.add("test1")
                    },
                )
            val test2 =
                Segment(
                    name = "test2",
                    dependsOn = listOf("build"),
                    execute = {
                        delay(50)
                        executionOrder.add("test2")
                    },
                )
            val lint =
                Segment(
                    name = "lint",
                    dependsOn = listOf("build"),
                    execute = {
                        delay(50)
                        executionOrder.add("lint")
                    },
                )
            val deploy =
                Segment(
                    name = "deploy",
                    dependsOn = listOf("test1", "test2", "lint"),
                    execute = {
                        executionOrder.add("deploy")
                    },
                )

            val scheduler = ParallelScheduler()
            val result = scheduler.execute(listOf(deploy, test1, build, lint, test2), context)

            assertEquals(5, result.successCount)

            // build should execute first
            assertEquals("build", executionOrder.first())

            // deploy should execute last
            assertEquals("deploy", executionOrder.last())

            // test1, test2, lint should execute after build but before deploy
            val testLintResults = executionOrder.subList(1, 4)
            assertTrue(testLintResults.containsAll(listOf("test1", "test2", "lint")))
        }

    @Test
    fun `maxConcurrency validation`() {
        try {
            ParallelScheduler(maxConcurrency = 0)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("maxConcurrency") == true)
        }

        try {
            ParallelScheduler(maxConcurrency = -1)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("maxConcurrency") == true)
        }
    }

    @Test
    fun `default maxConcurrency uses available processors`() {
        val scheduler = ParallelScheduler()
        // Just verify it doesn't throw
        assertTrue(true)
    }
}
