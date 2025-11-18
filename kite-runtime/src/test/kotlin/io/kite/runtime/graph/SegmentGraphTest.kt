package io.kite.runtime.graph

import io.kite.core.Segment
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SegmentGraphTest {
    private fun createSegment(
        name: String,
        dependsOn: List<String> = emptyList(),
    ): Segment {
        return Segment(
            name = name,
            dependsOn = dependsOn,
            execute = {},
        )
    }

    @Test
    fun `empty graph`() {
        val graph = SegmentGraph(emptyList())

        assertTrue(graph.allSegments().isEmpty())
        assertNull(graph.getSegment("any"))
    }

    @Test
    fun `single segment with no dependencies`() {
        val segment = createSegment("build")
        val graph = SegmentGraph(listOf(segment))

        assertEquals(1, graph.allSegments().size)
        assertEquals("build", graph.getSegment("build")?.name)
        assertTrue(graph.getDependencies("build").isEmpty())
        assertTrue(graph.getDependents("build").isEmpty())
    }

    @Test
    fun `linear dependency chain`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test"))

        val graph = SegmentGraph(listOf(build, test, deploy))

        // Check dependencies
        assertEquals(emptyList(), graph.getDependencies("build"))
        assertEquals(listOf("build"), graph.getDependencies("test"))
        assertEquals(listOf("test"), graph.getDependencies("deploy"))

        // Check dependents
        assertEquals(setOf("test"), graph.getDependents("build"))
        assertEquals(setOf("deploy"), graph.getDependents("test"))
        assertEquals(emptySet(), graph.getDependents("deploy"))
    }

    @Test
    fun `multiple dependencies`() {
        val lint = createSegment("lint")
        val build = createSegment("build")
        val test = createSegment("test", listOf("build", "lint"))

        val graph = SegmentGraph(listOf(lint, build, test))

        assertEquals(listOf("build", "lint"), graph.getDependencies("test"))
        assertEquals(setOf("test"), graph.getDependents("build"))
        assertEquals(setOf("test"), graph.getDependents("lint"))
    }

    @Test
    fun `diamond dependency pattern`() {
        val a = createSegment("a")
        val b = createSegment("b", listOf("a"))
        val c = createSegment("c", listOf("a"))
        val d = createSegment("d", listOf("b", "c"))

        val graph = SegmentGraph(listOf(a, b, c, d))

        assertEquals(setOf("b", "c"), graph.getDependents("a"))
        assertEquals(setOf("d"), graph.getDependents("b"))
        assertEquals(setOf("d"), graph.getDependents("c"))
    }

    @Test
    fun `validate detects missing dependencies`() {
        val test = createSegment("test", listOf("build")) // build doesn't exist

        val graph = SegmentGraph(listOf(test))
        val result = graph.validate()

        assertFalse(result.isValid)
        // Should report both: missing dependency AND unreachable segment
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.any { it.contains("depends on 'build' which does not exist") })
        assertTrue(result.errors.any { it.contains("unreachable") })
    }

    @Test
    fun `validate passes for valid graph`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))

        val graph = SegmentGraph(listOf(build, test))
        val result = graph.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `detectCycles finds simple cycle`() {
        val a = createSegment("a", listOf("b"))
        val b = createSegment("b", listOf("a"))

        val graph = SegmentGraph(listOf(a, b))
        val cycles = graph.detectCycles()

        assertEquals(1, cycles.size)
        assertTrue(cycles[0].path.contains("a"))
        assertTrue(cycles[0].path.contains("b"))
    }

    @Test
    fun `detectCycles finds self-cycle`() {
        val a = createSegment("a", listOf("a"))

        val graph = SegmentGraph(listOf(a))
        val cycles = graph.detectCycles()

        assertEquals(1, cycles.size)
        assertEquals(listOf("a", "a"), cycles[0].path)
    }

    @Test
    fun `detectCycles finds longer cycle`() {
        val a = createSegment("a", listOf("b"))
        val b = createSegment("b", listOf("c"))
        val c = createSegment("c", listOf("a"))

        val graph = SegmentGraph(listOf(a, b, c))
        val cycles = graph.detectCycles()

        assertEquals(1, cycles.size)
        assertEquals(3, cycles[0].path.size - 1) // -1 because cycle includes start node twice
    }

    @Test
    fun `detectCycles returns empty for acyclic graph`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test"))

        val graph = SegmentGraph(listOf(build, test, deploy))
        val cycles = graph.detectCycles()

        assertTrue(cycles.isEmpty())
    }

    @Test
    fun `validate detects cycles`() {
        val a = createSegment("a", listOf("b"))
        val b = createSegment("b", listOf("a"))

        val graph = SegmentGraph(listOf(a, b))
        val result = graph.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Cycle detected") })
    }

    @Test
    fun `validate detects unreachable segments`() {
        val a = createSegment("a", listOf("missing"))
        val b = createSegment("b")

        val graph = SegmentGraph(listOf(a, b))
        val result = graph.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("unreachable") })
    }

    @Test
    fun `stats provides correct counts`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test"))

        val graph = SegmentGraph(listOf(build, test, deploy))
        val stats = graph.stats()

        assertEquals(3, stats.totalSegments)
        assertEquals(2, stats.totalEdges) // test->build, deploy->test
        assertEquals(1, stats.maxDependencies)
        assertEquals(1, stats.maxDependents)
    }

    @Test
    fun `stats counts isolated segments`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val isolated = createSegment("isolated")

        val graph = SegmentGraph(listOf(build, test, isolated))
        val stats = graph.stats()

        assertEquals(1, stats.isolatedSegments)
    }

    @Test
    fun `stats handles complex graph`() {
        val a = createSegment("a")
        val b = createSegment("b")
        val c = createSegment("c", listOf("a", "b"))
        val d = createSegment("d", listOf("a", "b", "c"))

        val graph = SegmentGraph(listOf(a, b, c, d))
        val stats = graph.stats()

        assertEquals(4, stats.totalSegments)
        assertEquals(5, stats.totalEdges)
        assertEquals(3, stats.maxDependencies) // d has 3 dependencies
        assertEquals(2, stats.maxDependents) // a and b each have 2 dependents
    }

    @Test
    fun `toString provides readable output`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))

        val graph = SegmentGraph(listOf(build, test))

        val str = graph.toString()
        assertTrue(str.contains("2"))
        assertTrue(str.contains("1"))
    }

    @Test
    fun `Cycle toString formats path`() {
        val cycle = Cycle(listOf("a", "b", "c", "a"))

        assertEquals("a -> b -> c -> a", cycle.toString())
    }

    @Test
    fun `GraphValidationResult toString for valid graph`() {
        val result = GraphValidationResult(isValid = true, errors = emptyList())

        assertEquals("Graph is valid", result.toString())
    }

    @Test
    fun `GraphValidationResult toString for invalid graph`() {
        val result =
            GraphValidationResult(
                isValid = false,
                errors = listOf("Error 1", "Error 2"),
            )

        val str = result.toString()
        assertTrue(str.contains("validation failed"))
        assertTrue(str.contains("Error 1"))
        assertTrue(str.contains("Error 2"))
    }

    @Test
    fun `graph with no edges`() {
        val a = createSegment("a")
        val b = createSegment("b")
        val c = createSegment("c")

        val graph = SegmentGraph(listOf(a, b, c))

        assertEquals(0, graph.stats().totalEdges)
        assertEquals(3, graph.stats().isolatedSegments)
        assertTrue(graph.validate().isValid)
    }

    @Test
    fun `getSegment returns null for non-existent segment`() {
        val graph = SegmentGraph(listOf(createSegment("build")))

        assertNull(graph.getSegment("test"))
        assertNull(graph.getSegment(""))
    }

    @Test
    fun `getDependencies returns empty for non-existent segment`() {
        val graph = SegmentGraph(listOf(createSegment("build")))

        assertEquals(emptyList(), graph.getDependencies("nonexistent"))
    }

    @Test
    fun `getDependents returns empty for non-existent segment`() {
        val graph = SegmentGraph(listOf(createSegment("build")))

        assertEquals(emptySet(), graph.getDependents("nonexistent"))
    }
}
