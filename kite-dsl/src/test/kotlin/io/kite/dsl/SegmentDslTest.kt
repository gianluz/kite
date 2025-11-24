package io.kite.dsl

import io.kite.core.ExecutionContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SegmentDslTest {
    @Test
    fun `segments function creates segments list`() {
        val segmentsList =
            segments {
                segment("build") {
                    execute { }
                }
                segment("test") {
                    execute { }
                }
            }

        assertEquals(2, segmentsList.size)
        assertEquals("build", segmentsList[0].name)
        assertEquals("test", segmentsList[1].name)
    }

    @Test
    fun `segment with minimal properties`() {
        val segmentsList =
            segments {
                segment("simple") {
                    execute { }
                }
            }

        val segment = segmentsList[0]
        assertEquals("simple", segment.name)
        assertNull(segment.description)
        assertEquals(emptyList(), segment.dependsOn)
        assertNull(segment.timeout)
        assertEquals(0, segment.maxRetries)
    }

    @Test
    fun `segment with all properties`() {
        val segmentsList =
            segments {
                segment("complex") {
                    description = "Complex segment"
                    timeout = 10.minutes
                    maxRetries = 3
                    retryDelay = 5.seconds
                    dependsOn("build")
                    retryOn("IOException")

                    condition { !it.isCI }

                    execute {
                        // Execution logic
                    }
                }
            }

        val segment = segmentsList[0]
        assertEquals("complex", segment.name)
        assertEquals("Complex segment", segment.description)
        assertEquals(10.minutes, segment.timeout)
        assertEquals(3, segment.maxRetries)
        assertEquals(5.seconds, segment.retryDelay)
        assertEquals(listOf("build"), segment.dependsOn)
        assertEquals(listOf("IOException"), segment.retryOn)
        assertNotNull(segment.condition)
    }

    @Test
    fun `segment requires execute block`() {
        assertThrows<IllegalArgumentException> {
            segments {
                segment("missing-execute") {
                    description = "No execute block"
                }
            }
        }
    }

    @Test
    fun `dependsOn can add single dependency`() {
        val segmentsList =
            segments {
                segment("test") {
                    dependsOn("build")
                    execute { }
                }
            }

        assertEquals(listOf("build"), segmentsList[0].dependsOn)
    }

    @Test
    fun `dependsOn can add multiple dependencies vararg`() {
        val segmentsList =
            segments {
                segment("deploy") {
                    dependsOn("build", "test", "lint")
                    execute { }
                }
            }

        assertEquals(listOf("build", "test", "lint"), segmentsList[0].dependsOn)
    }

    @Test
    fun `dependsOn can be called multiple times`() {
        val segmentsList =
            segments {
                segment("deploy") {
                    dependsOn("build")
                    dependsOn("test")
                    execute { }
                }
            }

        assertEquals(listOf("build", "test"), segmentsList[0].dependsOn)
    }

    @Test
    fun `retryOn can add single exception`() {
        val segmentsList =
            segments {
                segment("flaky") {
                    retryOn("IOException")
                    execute { }
                }
            }

        assertEquals(listOf("IOException"), segmentsList[0].retryOn)
    }

    @Test
    fun `retryOn can add multiple exceptions vararg`() {
        val segmentsList =
            segments {
                segment("flaky") {
                    retryOn("IOException", "TimeoutException", "NetworkException")
                    execute { }
                }
            }

        assertEquals(
            listOf("IOException", "TimeoutException", "NetworkException"),
            segmentsList[0].retryOn,
        )
    }

    @Test
    fun `retryOn can be called multiple times`() {
        val segmentsList =
            segments {
                segment("flaky") {
                    retryOn("IOException")
                    retryOn("TimeoutException")
                    execute { }
                }
            }

        assertEquals(listOf("IOException", "TimeoutException"), segmentsList[0].retryOn)
    }

    @Test
    fun `condition lambda is stored`() {
        val segmentsList =
            segments {
                segment("conditional") {
                    condition { context -> !context.isCI }
                    execute { }
                }
            }

        val segment = segmentsList[0]
        assertNotNull(segment.condition)

        val localContext = ExecutionContext(branch = "main", commitSha = "abc", environment = emptyMap())
        assertTrue(segment.shouldExecute(localContext))

        val ciContext = ExecutionContext(branch = "main", commitSha = "abc", environment = mapOf("CI" to "true"))
        assertFalse(segment.shouldExecute(ciContext))
    }

    @Test
    fun `execute lambda is stored`() {
        var executed = false
        val segmentsList =
            segments {
                segment("exec") {
                    execute {
                        executed = true
                    }
                }
            }

        val segment = segmentsList[0]
        assertNotNull(segment.execute)

        // Note: We can't easily test execution here without running it,
        // but we verify it's not null
        assertFalse(executed) // Not executed yet
    }

    @Test
    fun `multiple segments can be defined`() {
        val segmentsList =
            segments {
                segment("build") {
                    execute { }
                }
                segment("test") {
                    dependsOn("build")
                    execute { }
                }
                segment("deploy") {
                    dependsOn("test")
                    execute { }
                }
            }

        assertEquals(3, segmentsList.size)
        assertEquals("build", segmentsList[0].name)
        assertEquals("test", segmentsList[1].name)
        assertEquals("deploy", segmentsList[2].name)
    }

    @Test
    fun `segment names are preserved`() {
        val segmentsList =
            segments {
                segment("first-segment") {
                    execute { }
                }
                segment("second_segment") {
                    execute { }
                }
                segment("ThirdSegment") {
                    execute { }
                }
            }

        assertEquals("first-segment", segmentsList[0].name)
        assertEquals("second_segment", segmentsList[1].name)
        assertEquals("ThirdSegment", segmentsList[2].name)
    }
}
