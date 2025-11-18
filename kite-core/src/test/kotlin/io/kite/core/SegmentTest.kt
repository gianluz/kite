package io.kite.core

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SegmentTest {
    @Test
    fun `segment requires non-blank name`() {
        assertThrows<IllegalArgumentException> {
            Segment(name = "", execute = {})
        }

        assertThrows<IllegalArgumentException> {
            Segment(name = "   ", execute = {})
        }
    }

    @Test
    fun `segment requires non-negative maxRetries`() {
        assertThrows<IllegalArgumentException> {
            Segment(name = "test", maxRetries = -1, execute = {})
        }
    }

    @Test
    fun `segment requires non-negative retryDelay`() {
        assertThrows<IllegalArgumentException> {
            Segment(name = "test", retryDelay = (-5).seconds, execute = {})
        }
    }

    @Test
    fun `segment with minimal properties`() {
        val segment = Segment(name = "build", execute = {})

        assertEquals("build", segment.name)
        assertEquals(null, segment.description)
        assertEquals(emptyList(), segment.dependsOn)
        assertEquals(null, segment.condition)
        assertEquals(null, segment.timeout)
        assertEquals(0, segment.maxRetries)
    }

    @Test
    fun `segment with all properties`() {
        val condition: (ExecutionContext) -> Boolean = { it.isLocal }
        val execute: suspend ExecutionContext.() -> Unit = {}

        val segment =
            Segment(
                name = "test",
                description = "Run tests",
                dependsOn = listOf("build"),
                condition = condition,
                timeout = 10.minutes,
                maxRetries = 3,
                retryDelay = 5.seconds,
                retryOn = listOf("IOException", "TimeoutException"),
                execute = execute,
            )

        assertEquals("test", segment.name)
        assertEquals("Run tests", segment.description)
        assertEquals(listOf("build"), segment.dependsOn)
        assertEquals(condition, segment.condition)
        assertEquals(10.minutes, segment.timeout)
        assertEquals(3, segment.maxRetries)
        assertEquals(5.seconds, segment.retryDelay)
        assertEquals(listOf("IOException", "TimeoutException"), segment.retryOn)
    }

    @Test
    fun `shouldExecute returns true when no condition`() {
        val segment = Segment(name = "test", execute = {})
        val context = mockk<ExecutionContext>()

        assertTrue(segment.shouldExecute(context))
    }

    @Test
    fun `shouldExecute evaluates condition`() {
        val segment =
            Segment(
                name = "test",
                condition = { it.isLocal },
                execute = {},
            )

        val localContext =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                isLocal = true,
            )
        assertTrue(segment.shouldExecute(localContext))

        val ciContext =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                isLocal = false,
            )
        assertFalse(segment.shouldExecute(ciContext))
    }

    @Test
    fun `segments with same name are equal`() {
        val segment1 = Segment(name = "build", description = "Build the app", execute = {})
        val segment2 = Segment(name = "build", description = "Different description", execute = {})

        assertEquals(segment1, segment2)
        assertEquals(segment1.hashCode(), segment2.hashCode())
    }

    @Test
    fun `segments with different names are not equal`() {
        val segment1 = Segment(name = "build", execute = {})
        val segment2 = Segment(name = "test", execute = {})

        assertFalse(segment1 == segment2)
    }

    @Test
    fun `toString includes key information`() {
        val segment =
            Segment(
                name = "build",
                description = "Build the app",
                dependsOn = listOf("lint", "test"),
                execute = {},
            )

        val str = segment.toString()
        assertTrue(str.contains("build"))
        assertTrue(str.contains("Build the app"))
        assertTrue(str.contains("lint"))
        assertTrue(str.contains("test"))
    }
}

class SegmentStatusTest {
    @Test
    fun `isCompleted is false for PENDING and RUNNING`() {
        assertFalse(SegmentStatus.PENDING.isCompleted)
        assertFalse(SegmentStatus.RUNNING.isCompleted)
    }

    @Test
    fun `isCompleted is true for terminal states`() {
        assertTrue(SegmentStatus.SUCCESS.isCompleted)
        assertTrue(SegmentStatus.FAILURE.isCompleted)
        assertTrue(SegmentStatus.SKIPPED.isCompleted)
        assertTrue(SegmentStatus.TIMEOUT.isCompleted)
    }

    @Test
    fun `isSuccessful is true for SUCCESS and SKIPPED`() {
        assertTrue(SegmentStatus.SUCCESS.isSuccessful)
        assertTrue(SegmentStatus.SKIPPED.isSuccessful)
    }

    @Test
    fun `isSuccessful is false for failure states`() {
        assertFalse(SegmentStatus.FAILURE.isSuccessful)
        assertFalse(SegmentStatus.TIMEOUT.isSuccessful)
        assertFalse(SegmentStatus.PENDING.isSuccessful)
        assertFalse(SegmentStatus.RUNNING.isSuccessful)
    }

    @Test
    fun `isFailed is true for FAILURE and TIMEOUT`() {
        assertTrue(SegmentStatus.FAILURE.isFailed)
        assertTrue(SegmentStatus.TIMEOUT.isFailed)
    }

    @Test
    fun `isFailed is false for success states`() {
        assertFalse(SegmentStatus.SUCCESS.isFailed)
        assertFalse(SegmentStatus.SKIPPED.isFailed)
        assertFalse(SegmentStatus.PENDING.isFailed)
        assertFalse(SegmentStatus.RUNNING.isFailed)
    }
}
