package io.kite.dsl

import io.kite.core.ExecutionContext
import io.kite.core.FlowNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class RideDslTest {

    @Test
    fun `ride function creates ride`() {
        val myRide = ride {
            name = "Test Ride"
            flow {
                segment("build")
            }
        }

        assertEquals("Test Ride", myRide.name)
        assertNotNull(myRide.flow)
    }

    @Test
    fun `ride requires name`() {
        assertThrows<IllegalArgumentException> {
            ride {
                flow {
                    segment("build")
                }
            }
        }
    }

    @Test
    fun `ride requires flow`() {
        assertThrows<IllegalArgumentException> {
            ride {
                name = "No Flow Ride"
            }
        }
    }

    @Test
    fun `ride with minimal properties`() {
        val myRide = ride {
            name = "Minimal"
            flow {
                segment("build")
            }
        }

        assertEquals("Minimal", myRide.name)
        assertEquals(emptyMap(), myRide.environment)
        assertNull(myRide.maxConcurrency)
        assertNull(myRide.onFailure)
    }

    @Test
    fun `ride with all properties`() {
        val onFailure: suspend (Throwable) -> Unit = {}
        val myRide = ride {
            name = "Complete Ride"
            maxConcurrency = 4
            env("KEY", "value")
            env("NUM", "42")
            onFailure(onFailure)

            flow {
                segment("build")
            }
        }

        assertEquals("Complete Ride", myRide.name)
        assertEquals(4, myRide.maxConcurrency)
        assertEquals(mapOf("KEY" to "value", "NUM" to "42"), myRide.environment)
        assertEquals(onFailure, myRide.onFailure)
    }

    @Test
    fun `flow with single segment`() {
        val myRide = ride {
            name = "Single Segment"
            flow {
                segment("build")
            }
        }

        assertTrue(myRide.flow is FlowNode.SegmentRef)
        assertEquals("build", (myRide.flow as FlowNode.SegmentRef).segmentName)
    }

    @Test
    fun `flow with multiple sequential segments`() {
        val myRide = ride {
            name = "Sequential"
            flow {
                segment("build")
                segment("test")
                segment("deploy")
            }
        }

        assertTrue(myRide.flow is FlowNode.Sequential)
        val sequential = myRide.flow as FlowNode.Sequential
        assertEquals(3, sequential.nodes.size)
    }

    @Test
    fun `flow with parallel block`() {
        val myRide = ride {
            name = "Parallel"
            flow {
                segment("build")
                parallel {
                    segment("test1")
                    segment("test2")
                }
            }
        }

        assertTrue(myRide.flow is FlowNode.Sequential)
        val sequential = myRide.flow as FlowNode.Sequential
        assertEquals(2, sequential.nodes.size)
        assertTrue(sequential.nodes[1] is FlowNode.Parallel)
    }

    @Test
    fun `parallel block with multiple segments`() {
        val myRide = ride {
            name = "Test"
            flow {
                parallel {
                    segment("test1")
                    segment("test2")
                    segment("test3")
                }
            }
        }

        assertTrue(myRide.flow is FlowNode.Parallel)
        val parallel = myRide.flow as FlowNode.Parallel
        assertEquals(3, parallel.nodes.size)
    }

    @Test
    fun `nested sequential and parallel blocks`() {
        val myRide = ride {
            name = "Nested"
            flow {
                segment("lint")
                segment("build")
                parallel {
                    segment("unitTest")
                    segment("integrationTest")
                }
                segment("deploy")
            }
        }

        assertTrue(myRide.flow is FlowNode.Sequential)
        val sequential = myRide.flow as FlowNode.Sequential
        assertEquals(4, sequential.nodes.size)

        // Check that third node is parallel
        assertTrue(sequential.nodes[2] is FlowNode.Parallel)
        val parallel = sequential.nodes[2] as FlowNode.Parallel
        assertEquals(2, parallel.nodes.size)
    }

    @Test
    fun `segment with overrides`() {
        val myRide = ride {
            name = "Overrides"
            flow {
                segment("test") {
                    dependsOn("lint")
                    timeout = 5.minutes
                    enabled = false
                }
            }
        }

        assertTrue(myRide.flow is FlowNode.SegmentRef)
        val segmentRef = myRide.flow as FlowNode.SegmentRef

        val overrides = segmentRef.overrides
        assertEquals(listOf("lint"), overrides.dependsOn)
        assertEquals(5.minutes, overrides.timeout)
        assertFalse(overrides.enabled)
    }

    @Test
    fun `segment override dependsOn single`() {
        val myRide = ride {
            name = "Test"
            flow {
                segment("deploy") {
                    dependsOn("test")
                }
            }
        }

        val segmentRef = myRide.flow as FlowNode.SegmentRef
        assertEquals(listOf("test"), segmentRef.overrides.dependsOn)
    }

    @Test
    fun `segment override dependsOn multiple`() {
        val myRide = ride {
            name = "Test"
            flow {
                segment("deploy") {
                    dependsOn("build", "test", "lint")
                }
            }
        }

        val segmentRef = myRide.flow as FlowNode.SegmentRef
        assertEquals(listOf("build", "test", "lint"), segmentRef.overrides.dependsOn)
    }

    @Test
    fun `segment override condition`() {
        val myRide = ride {
            name = "Test"
            flow {
                segment("deploy") {
                    condition { it.isRelease }
                }
            }
        }

        val segmentRef = myRide.flow as FlowNode.SegmentRef
        assertNotNull(segmentRef.overrides.condition)

        val releaseContext = ExecutionContext(
            branch = "main",
            commitSha = "abc",
            isRelease = true
        )
        assertTrue(segmentRef.overrides.condition!!.invoke(releaseContext))

        val nonReleaseContext = ExecutionContext(
            branch = "main",
            commitSha = "abc",
            isRelease = false
        )
        assertFalse(segmentRef.overrides.condition!!.invoke(nonReleaseContext))
    }

    @Test
    fun `segment without overrides has default values`() {
        val myRide = ride {
            name = "Test"
            flow {
                segment("build")
            }
        }

        val segmentRef = myRide.flow as FlowNode.SegmentRef
        assertNull(segmentRef.overrides.dependsOn)
        assertNull(segmentRef.overrides.condition)
        assertNull(segmentRef.overrides.timeout)
        assertTrue(segmentRef.overrides.enabled)
    }

    @Test
    fun `env adds environment variables`() {
        val myRide = ride {
            name = "Test"
            env("KEY1", "value1")
            env("KEY2", "value2")
            flow {
                segment("build")
            }
        }

        assertEquals("value1", myRide.environment["KEY1"])
        assertEquals("value2", myRide.environment["KEY2"])
    }

    @Test
    fun `complex real-world example`() {
        val myRide = ride {
            name = "MR Ride"
            maxConcurrency = 3
            env("GRADLE_OPTS", "-Xmx4g")

            flow {
                segment("lint")
                segment("build")

                parallel {
                    segment("unitTest")
                    segment("integrationTest") {
                        timeout = 10.minutes
                        condition { it.isRelease }
                    }
                    segment("screenshotTest")
                }

                segment("deploy") {
                    dependsOn("lint", "build")
                    condition { it.branch == "main" }
                    enabled = false
                }
            }
        }

        assertEquals("MR Ride", myRide.name)
        assertEquals(3, myRide.maxConcurrency)
        assertEquals(mapOf("GRADLE_OPTS" to "-Xmx4g"), myRide.environment)

        // Verify structure
        assertTrue(myRide.flow is FlowNode.Sequential)
        val sequential = myRide.flow as FlowNode.Sequential
        assertEquals(4, sequential.nodes.size)

        // Verify parallel block
        assertTrue(sequential.nodes[2] is FlowNode.Parallel)
        val parallel = sequential.nodes[2] as FlowNode.Parallel
        assertEquals(3, parallel.nodes.size)
    }

    @Test
    fun `flow requires at least one node`() {
        assertThrows<IllegalStateException> {
            ride {
                name = "Empty Flow"
                flow {
                    // Empty flow
                }
            }
        }
    }
}
