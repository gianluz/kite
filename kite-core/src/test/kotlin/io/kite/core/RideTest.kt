package io.kite.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RideTest {
    @Test
    fun `ride requires non-blank name`() {
        assertThrows<IllegalArgumentException> {
            Ride(
                name = "",
                flow = FlowNode.SegmentRef("build"),
            )
        }
    }

    @Test
    fun `ride requires positive maxConcurrency`() {
        assertThrows<IllegalArgumentException> {
            Ride(
                name = "test",
                flow = FlowNode.SegmentRef("build"),
                maxConcurrency = 0,
            )
        }

        assertThrows<IllegalArgumentException> {
            Ride(
                name = "test",
                flow = FlowNode.SegmentRef("build"),
                maxConcurrency = -1,
            )
        }
    }

    @Test
    fun `ride with minimal properties`() {
        val ride =
            Ride(
                name = "MR Ride",
                flow = FlowNode.SegmentRef("build"),
            )

        assertEquals("MR Ride", ride.name)
        assertEquals(emptyMap(), ride.environment)
        assertEquals(null, ride.maxConcurrency)
        assertEquals(null, ride.onFailure)
    }

    @Test
    fun `ride with all properties`() {
        val onFailure: suspend (Throwable) -> Unit = {}
        val ride =
            Ride(
                name = "Full Ride",
                flow = FlowNode.SegmentRef("build"),
                environment = mapOf("KEY" to "value"),
                maxConcurrency = 4,
                onFailure = onFailure,
            )

        assertEquals("Full Ride", ride.name)
        assertEquals(mapOf("KEY" to "value"), ride.environment)
        assertEquals(4, ride.maxConcurrency)
        assertEquals(onFailure, ride.onFailure)
    }

    @Test
    fun `validate detects missing segments`() {
        val ride =
            Ride(
                name = "Test",
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.SegmentRef("test"),
                            FlowNode.SegmentRef("missing"),
                        ),
                    ),
            )

        val availableSegments = setOf("build", "test")
        val errors = ride.validate(availableSegments)

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("missing"))
    }

    @Test
    fun `validate passes with all segments present`() {
        val ride =
            Ride(
                name = "Test",
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.SegmentRef("test"),
                        ),
                    ),
            )

        val availableSegments = setOf("build", "test", "lint")
        val errors = ride.validate(availableSegments)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validate detects empty parallel blocks`() {
        val ride =
            Ride(
                name = "Test",
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.Parallel(emptyList()),
                        ),
                    ),
            )

        val errors = ride.validate(setOf("build"))

        assertTrue(errors.any { it.contains("Empty parallel block") })
    }

    @Test
    fun `toString includes key information`() {
        val ride =
            Ride(
                name = "MR Ride",
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.SegmentRef("test"),
                        ),
                    ),
                maxConcurrency = 4,
            )

        val str = ride.toString()
        assertTrue(str.contains("MR Ride"))
        assertTrue(str.contains("2"))
        assertTrue(str.contains("4"))
    }
}

class FlowNodeTest {
    @Test
    fun `SegmentRef requires non-blank name`() {
        assertThrows<IllegalArgumentException> {
            FlowNode.SegmentRef("")
        }
    }

    @Test
    fun `SegmentRef allSegmentReferences returns single name`() {
        val node = FlowNode.SegmentRef("build")

        assertEquals(setOf("build"), node.allSegmentReferences())
    }

    @Test
    fun `SegmentRef with overrides`() {
        val overrides =
            SegmentOverrides(
                dependsOn = listOf("lint"),
                enabled = false,
            )
        val node = FlowNode.SegmentRef("test", overrides)

        assertEquals("test", node.segmentName)
        assertEquals(overrides, node.overrides)
    }

    @Test
    fun `Sequential requires at least one node`() {
        assertThrows<IllegalArgumentException> {
            FlowNode.Sequential(emptyList())
        }
    }

    @Test
    fun `Sequential allSegmentReferences combines all nodes`() {
        val node =
            FlowNode.Sequential(
                listOf(
                    FlowNode.SegmentRef("build"),
                    FlowNode.SegmentRef("test"),
                    FlowNode.SegmentRef("lint"),
                ),
            )

        val refs = node.allSegmentReferences()
        assertEquals(3, refs.size)
        assertTrue(refs.contains("build"))
        assertTrue(refs.contains("test"))
        assertTrue(refs.contains("lint"))
    }

    @Test
    fun `Sequential with nested Parallel allSegmentReferences`() {
        val node =
            FlowNode.Sequential(
                listOf(
                    FlowNode.SegmentRef("build"),
                    FlowNode.Parallel(
                        listOf(
                            FlowNode.SegmentRef("unitTest"),
                            FlowNode.SegmentRef("integrationTest"),
                        ),
                    ),
                ),
            )

        val refs = node.allSegmentReferences()
        assertEquals(3, refs.size)
        assertTrue(refs.contains("build"))
        assertTrue(refs.contains("unitTest"))
        assertTrue(refs.contains("integrationTest"))
    }

    @Test
    fun `Parallel allSegmentReferences combines all nodes`() {
        val node =
            FlowNode.Parallel(
                listOf(
                    FlowNode.SegmentRef("unitTest"),
                    FlowNode.SegmentRef("lint"),
                    FlowNode.SegmentRef("detekt"),
                ),
            )

        val refs = node.allSegmentReferences()
        assertEquals(3, refs.size)
        assertTrue(refs.contains("unitTest"))
        assertTrue(refs.contains("lint"))
        assertTrue(refs.contains("detekt"))
    }

    @Test
    fun `Parallel can be empty`() {
        // Empty parallel blocks are allowed, but validation will catch them
        val node = FlowNode.Parallel(emptyList())

        assertTrue(node.allSegmentReferences().isEmpty())
    }

    @Test
    fun `findEmptyParallelBlocks detects empty blocks`() {
        val node =
            FlowNode.Sequential(
                listOf(
                    FlowNode.SegmentRef("build"),
                    FlowNode.Parallel(emptyList()),
                    FlowNode.SegmentRef("deploy"),
                ),
            )

        val empty = node.findEmptyParallelBlocks()
        assertEquals(1, empty.size)
    }

    @Test
    fun `findEmptyParallelBlocks returns empty for valid flow`() {
        val node =
            FlowNode.Sequential(
                listOf(
                    FlowNode.SegmentRef("build"),
                    FlowNode.Parallel(
                        listOf(
                            FlowNode.SegmentRef("test1"),
                            FlowNode.SegmentRef("test2"),
                        ),
                    ),
                ),
            )

        val empty = node.findEmptyParallelBlocks()
        assertTrue(empty.isEmpty())
    }
}
