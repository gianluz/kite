package io.kite.runtime.scheduler

import io.kite.core.ExecutionContext
import io.kite.core.Segment
import io.kite.core.SegmentStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for timeout and retry functionality in segment execution.
 */
class TimeoutAndRetryTest {
    private val context =
        ExecutionContext(
            branch = "main",
            commitSha = "abc123",
        )

    // ========== TIMEOUT TESTS ==========

    @Test
    fun `segment with timeout succeeds if completes within timeout`() =
        runTest {
            val segment =
                Segment(
                    name = "fast-task",
                    timeout = 1.seconds,
                    execute = {
                        delay(100) // 100ms < 1 second
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("fast-task")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.SUCCESS, segmentResult.status)
        }

    @Test
    fun `segment with timeout fails with TIMEOUT status if exceeds timeout`() =
        runTest {
            val segment =
                Segment(
                    name = "slow-task",
                    timeout = 100.milliseconds,
                    execute = {
                        delay(500) // 500ms > 100ms
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("slow-task")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.TIMEOUT, segmentResult.status)
            assertTrue(
                segmentResult.error?.contains("timeout") == true,
                "Error message should mention timeout: ${segmentResult.error}",
            )
        }

    @Test
    fun `segment without timeout runs indefinitely`() =
        runTest {
            val segment =
                Segment(
                    name = "no-timeout-task",
                    timeout = null, // No timeout
                    execute = {
                        delay(200) // Should complete without timeout
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("no-timeout-task")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.SUCCESS, segmentResult.status)
        }

    @Test
    fun `timeout does not trigger retry`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "timeout-task",
                    timeout = 100.milliseconds,
                    maxRetries = 3,
                    execute = {
                        attempts++
                        delay(500) // Always timeout
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("timeout-task")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.TIMEOUT, segmentResult.status)
            assertEquals(1, attempts, "Timeout should not trigger retries")
        }

    // ========== RETRY TESTS ==========

    @Test
    fun `segment retries on failure up to maxRetries`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "flaky-task",
                    maxRetries = 3,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        throw RuntimeException("Attempt $attempts failed")
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("flaky-task")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.FAILURE, segmentResult.status)
            assertEquals(4, attempts, "Should attempt 1 initial + 3 retries = 4 total")
        }

    @Test
    fun `segment succeeds on retry attempt`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "eventually-succeeds",
                    maxRetries = 3,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        if (attempts < 3) {
                            throw RuntimeException("Not yet")
                        }
                        // Success on 3rd attempt
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("eventually-succeeds")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.SUCCESS, segmentResult.status)
            assertEquals(3, attempts, "Should succeed on 3rd attempt")
        }

    @Test
    fun `segment with no retries fails immediately`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "no-retry-task",
                    maxRetries = 0, // No retries
                    execute = {
                        attempts++
                        throw RuntimeException("Failed")
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("no-retry-task")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.FAILURE, segmentResult.status)
            assertEquals(1, attempts, "Should only attempt once")
        }

    @Test
    fun `retryOn filters exceptions correctly - matching exception retries`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "selective-retry",
                    maxRetries = 2,
                    retryDelay = 10.milliseconds,
                    retryOn = listOf("IOException"),
                    execute = {
                        attempts++
                        throw java.io.IOException("Network error")
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("selective-retry")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.FAILURE, segmentResult.status)
            assertEquals(3, attempts, "IOException should trigger retries (1 + 2 retries)")
        }

    @Test
    fun `retryOn filters exceptions correctly - non-matching exception does not retry`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "selective-no-retry",
                    maxRetries = 2,
                    retryDelay = 10.milliseconds,
                    retryOn = listOf("IOException"),
                    execute = {
                        attempts++
                        throw IllegalStateException("Wrong exception type")
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("selective-no-retry")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.FAILURE, segmentResult.status)
            assertEquals(1, attempts, "IllegalStateException should not trigger retries")
        }

    @Test
    fun `empty retryOn list retries on any exception`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "retry-all",
                    maxRetries = 2,
                    retryDelay = 10.milliseconds,
                    retryOn = emptyList(), // Retry on any exception
                    execute = {
                        attempts++
                        throw IllegalArgumentException("Any error")
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("retry-all")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.FAILURE, segmentResult.status)
            assertEquals(3, attempts, "Empty retryOn should retry on any exception (1 + 2 retries)")
        }

    @Test
    fun `retryDelay property is used`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "delayed-retry",
                    maxRetries = 2,
                    retryDelay = 50.milliseconds,
                    execute = {
                        attempts++
                        throw RuntimeException("Fail")
                    },
                )

            val scheduler = SequentialScheduler()
            scheduler.execute(listOf(segment), context)

            // Verify retries happened (retryDelay is applied internally)
            assertEquals(3, attempts, "Should have 3 attempts with retryDelay")
        }

    // ========== LIFECYCLE HOOK TESTS ==========

    @Test
    fun `onSuccess hook fires only on final success`() =
        runTest {
            var attempts = 0
            var successCallCount = 0

            val segment =
                Segment(
                    name = "success-hook",
                    maxRetries = 2,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        if (attempts < 2) throw RuntimeException("Not yet")
                    },
                    onSuccess = { successCallCount++ },
                )

            val scheduler = SequentialScheduler()
            scheduler.execute(listOf(segment), context)

            assertEquals(2, attempts)
            assertEquals(1, successCallCount, "onSuccess should fire only once on final success")
        }

    @Test
    fun `onFailure hook fires only on final failure`() =
        runTest {
            var attempts = 0
            var failureCallCount = 0

            val segment =
                Segment(
                    name = "failure-hook",
                    maxRetries = 2,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        throw RuntimeException("Always fail")
                    },
                    onFailure = { failureCallCount++ },
                )

            val scheduler = SequentialScheduler()
            scheduler.execute(listOf(segment), context)

            assertEquals(3, attempts, "Should attempt 1 + 2 retries")
            assertEquals(1, failureCallCount, "onFailure should fire only once on final failure")
        }

    @Test
    fun `onComplete hook fires only on final attempt`() =
        runTest {
            var attempts = 0
            var completeCallCount = 0
            var finalStatus: SegmentStatus? = null

            val segment =
                Segment(
                    name = "complete-hook",
                    maxRetries = 2,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        if (attempts < 2) throw RuntimeException("Not yet")
                    },
                    onComplete = { status ->
                        completeCallCount++
                        finalStatus = status
                    },
                )

            val scheduler = SequentialScheduler()
            scheduler.execute(listOf(segment), context)

            assertEquals(2, attempts)
            assertEquals(1, completeCallCount, "onComplete should fire only once")
            assertEquals(SegmentStatus.SUCCESS, finalStatus, "Status should be SUCCESS")
        }

    // ========== COMBINED TIMEOUT + RETRY TESTS ==========

    @Test
    fun `segment with both timeout and retry - timeout does not retry`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "timeout-with-retry",
                    timeout = 100.milliseconds,
                    maxRetries = 3,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        delay(500) // Always timeout
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("timeout-with-retry")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.TIMEOUT, segmentResult.status)
            assertEquals(1, attempts, "Timeout should not trigger retries even with maxRetries set")
        }

    @Test
    fun `segment succeeds within timeout after retry`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "retry-then-succeed",
                    timeout = 1.seconds,
                    maxRetries = 3,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        if (attempts < 3) {
                            throw RuntimeException("Fail attempt $attempts")
                        }
                        delay(100) // Succeed within timeout on 3rd attempt
                    },
                )

            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("retry-then-succeed")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.SUCCESS, segmentResult.status)
            assertEquals(3, attempts)
        }

    // ========== PARALLEL SCHEDULER TESTS ==========

    @Test
    fun `parallel scheduler handles timeout correctly`() =
        runTest {
            val segment =
                Segment(
                    name = "parallel-timeout",
                    timeout = 100.milliseconds,
                    execute = {
                        delay(500) // Timeout
                    },
                )

            val scheduler = ParallelScheduler(maxConcurrency = 2)
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("parallel-timeout")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.TIMEOUT, segmentResult.status)
        }

    @Test
    fun `parallel scheduler handles retry correctly`() =
        runTest {
            var attempts = 0

            val segment =
                Segment(
                    name = "parallel-retry",
                    maxRetries = 2,
                    retryDelay = 10.milliseconds,
                    execute = {
                        attempts++
                        if (attempts < 2) throw RuntimeException("Not yet")
                    },
                )

            val scheduler = ParallelScheduler(maxConcurrency = 2)
            val result = scheduler.execute(listOf(segment), context)

            val segmentResult = result.getResult("parallel-retry")
            assertNotNull(segmentResult)
            assertEquals(SegmentStatus.SUCCESS, segmentResult.status)
            assertEquals(2, attempts)
        }
}
