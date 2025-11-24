package io.kite.runtime.scheduler

import io.kite.core.ExecutionContext
import io.kite.core.Segment
import io.kite.core.SegmentStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SequentialSchedulerTest {
    private val context =
        ExecutionContext(
            branch = "main",
            commitSha = "abc123",
        )

    @Test
    fun `execute empty list returns empty result`() =
        runTest {
            val scheduler = SequentialScheduler()

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

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            assertTrue(executed)
            assertEquals(1, result.totalSegments)
            assertEquals(1, result.successCount)
            assertEquals(SegmentStatus.SUCCESS, result.getResult("build")?.status)
        }

    @Test
    fun `execute segments in topological order`() =
        runTest {
            val executionOrder = mutableListOf<String>()

            val build =
                Segment(
                    name = "build",
                    execute = { executionOrder.add("build") },
                )
            val test =
                Segment(
                    name = "test",
                    dependsOn = listOf("build"),
                    execute = { executionOrder.add("test") },
                )
            val deploy =
                Segment(
                    name = "deploy",
                    dependsOn = listOf("test"),
                    execute = { executionOrder.add("deploy") },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(deploy, test, build), context) // Out of order

            assertEquals(listOf("build", "test", "deploy"), executionOrder)
            assertEquals(3, result.successCount)
        }

    @Test
    fun `handle segment failure`() =
        runTest {
            val build =
                Segment(
                    name = "build",
                    execute = { throw RuntimeException("Build failed") },
                )
            val test =
                Segment(
                    name = "test",
                    dependsOn = listOf("build"),
                    execute = { },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(build, test), context)

            assertEquals(2, result.totalSegments)
            assertEquals(1, result.failureCount)
            assertEquals(1, result.skippedCount)
            assertEquals(SegmentStatus.FAILURE, result.getResult("build")?.status)
            assertEquals(SegmentStatus.SKIPPED, result.getResult("test")?.status)
        }

    @Test
    fun `skip dependent segments on failure`() =
        runTest {
            val executionOrder = mutableListOf<String>()

            val lint =
                Segment(
                    name = "lint",
                    execute = { executionOrder.add("lint") },
                )
            val build =
                Segment(
                    name = "build",
                    execute = {
                        executionOrder.add("build")
                        throw RuntimeException("Failed")
                    },
                )
            val test =
                Segment(
                    name = "test",
                    dependsOn = listOf("build"),
                    execute = { executionOrder.add("test") },
                )
            val deploy =
                Segment(
                    name = "deploy",
                    dependsOn = listOf("test"),
                    execute = { executionOrder.add("deploy") },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(lint, build, test, deploy), context)

            // Only lint and build should execute (build fails)
            assertEquals(setOf("lint", "build"), executionOrder.toSet())
            assertEquals(3, result.successCount) // lint + test (skipped) + deploy (skipped) - SKIPPED counts as success
            assertEquals(1, result.failureCount) // build
            assertEquals(2, result.skippedCount) // test, deploy (cascading skips)
        }

    @Test
    fun `skip segment based on condition`() =
        runTest {
            var executed = false

            val segment =
                Segment(
                    name = "deploy",
                    // Only run on release
                    condition = { it.isRelease },
                    execute = { executed = true },
                )

            val scheduler = SequentialScheduler()
            val nonReleaseContext = context.copy(isRelease = false)
            val result = scheduler.execute(listOf(segment), nonReleaseContext)

            assertFalse(executed)
            assertEquals(1, result.skippedCount)
            assertEquals(SegmentStatus.SKIPPED, result.getResult("deploy")?.status)
        }

    @Test
    fun `execute segment when condition is met`() =
        runTest {
            var executed = false

            val segment =
                Segment(
                    name = "deploy",
                    condition = { it.isRelease },
                    execute = { executed = true },
                )

            val scheduler = SequentialScheduler()
            val releaseContext = context.copy(isRelease = true)
            val result = scheduler.execute(listOf(segment), releaseContext)

            assertTrue(executed)
            assertEquals(1, result.successCount)
            assertEquals(SegmentStatus.SUCCESS, result.getResult("deploy")?.status)
        }

    @Test
    fun `result includes duration`() =
        runTest {
            val segment =
                Segment(
                    name = "slow",
                    execute = {
                        Thread.sleep(10) // Small delay
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("slow")
            assertNotNull(segmentResult)
            assertTrue(segmentResult.durationMs >= 10)
        }

    @Test
    fun `result includes exception details`() =
        runTest {
            val exception = RuntimeException("Custom error")
            val segment =
                Segment(
                    name = "failing",
                    execute = { throw exception },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("failing")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.FAILURE, segmentResult.status)
            assertEquals("Custom error", segmentResult.error)
            assertEquals(exception, segmentResult.exception)
        }

    @Test
    fun `handle graph validation errors`() =
        runTest {
            val segment =
                Segment(
                    name = "test",
                    // Missing dependency
                    dependsOn = listOf("missing"),
                    execute = { },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            assertEquals(1, result.totalSegments)
            assertEquals(1, result.skippedCount) // Validation errors cause segments to be skipped
            assertNotNull(result.getResult("test")?.error)
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

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(a, b), context)

            assertEquals(2, result.totalSegments)
            // Cyclic dependencies are caught by graph validation, marking all as SKIPPED
            assertEquals(2, result.skippedCount)
            assertEquals(2, result.successCount) // SKIPPED counts as success
        }

    @Test
    fun `SchedulerResult calculates totals correctly`() =
        runTest {
            val build = Segment(name = "build", execute = { })
            val test =
                Segment(
                    name = "test",
                    dependsOn = listOf("build"),
                    execute = { throw RuntimeException() },
                )
            val deploy =
                Segment(
                    name = "deploy",
                    dependsOn = listOf("test"),
                    execute = { },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(build, test, deploy), context)

            assertEquals(3, result.totalSegments)
            assertEquals(2, result.successCount) // build + deploy (skipped counts as success)
            assertEquals(1, result.failureCount) // test
            assertEquals(1, result.skippedCount) // deploy
            assertFalse(result.isSuccess) // isSuccess is false because we have failures
        }

    @Test
    fun `SchedulerResult isSuccess true when all succeed`() =
        runTest {
            val segments =
                listOf(
                    Segment(name = "a", execute = { }),
                    Segment(name = "b", execute = { }),
                    Segment(name = "c", execute = { }),
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(segments, context)

            assertTrue(result.isSuccess)
            assertEquals(3, result.successCount)
            assertEquals(0, result.failureCount)
        }

    @Test
    fun `failedSegments returns only failed segments`() =
        runTest {
            val build = Segment(name = "build", execute = { })
            val test =
                Segment(
                    name = "test",
                    execute = { throw RuntimeException() },
                )
            val lint =
                Segment(
                    name = "lint",
                    execute = { throw RuntimeException() },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(build, test, lint), context)

            val failed = result.failedSegments()
            assertEquals(2, failed.size)
            assertTrue(failed.any { it.segment.name == "test" })
            assertTrue(failed.any { it.segment.name == "lint" })
        }

    @Test
    fun `successfulSegments returns only successful segments`() =
        runTest {
            val build = Segment(name = "build", execute = { })
            val test = Segment(name = "test", execute = { })
            val deploy =
                Segment(
                    name = "deploy",
                    dependsOn = listOf("test"),
                    execute = { throw RuntimeException() },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(build, test, deploy), context)

            val successful = result.successfulSegments()
            assertEquals(2, successful.size)
            assertTrue(successful.any { it.segment.name == "build" })
            assertTrue(successful.any { it.segment.name == "test" })
        }

    @Test
    fun `SegmentResult toString formats correctly`() {
        val segment = Segment(name = "test", execute = { })

        val success = SegmentResult(segment, SegmentStatus.SUCCESS, durationMs = 100)
        assertTrue(success.toString().contains("✓"))
        assertTrue(success.toString().contains("test"))
        assertTrue(success.toString().contains("100ms"))

        val failure = SegmentResult(segment, SegmentStatus.FAILURE, error = "Failed")
        assertTrue(failure.toString().contains("✗"))
        assertTrue(failure.toString().contains("Failed"))

        val skipped = SegmentResult(segment, SegmentStatus.SKIPPED, message = "Condition")
        assertTrue(skipped.toString().contains("○"))
        assertTrue(skipped.toString().contains("Condition"))
    }
}
