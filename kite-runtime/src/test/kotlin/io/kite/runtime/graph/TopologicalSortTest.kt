package io.kite.runtime.graph

import io.kite.core.Segment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopologicalSortTest {
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
    fun `sort empty graph`() {
        val graph = SegmentGraph(emptyList())
        val sorter = TopologicalSort(graph)

        val sorted = sorter.sort()

        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `sort single segment`() {
        val build = createSegment("build")
        val graph = SegmentGraph(listOf(build))
        val sorter = TopologicalSort(graph)

        val sorted = sorter.sort()

        assertEquals(1, sorted.size)
        assertEquals("build", sorted[0].name)
    }

    @Test
    fun `sort linear chain`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test"))

        val graph = SegmentGraph(listOf(build, test, deploy))
        val sorter = TopologicalSort(graph)

        val sorted = sorter.sort()

        assertEquals(3, sorted.size)
        assertEquals("build", sorted[0].name)
        assertEquals("test", sorted[1].name)
        assertEquals("deploy", sorted[2].name)
    }

    @Test
    fun `sort respects dependencies`() {
        val lint = createSegment("lint")
        val build = createSegment("build")
        val test = createSegment("test", listOf("build", "lint"))

        val graph = SegmentGraph(listOf(test, build, lint)) // Intentionally out of order
        val sorter = TopologicalSort(graph)

        val sorted = sorter.sort()

        // build and lint must come before test
        val buildIndex = sorted.indexOfFirst { it.name == "build" }
        val lintIndex = sorted.indexOfFirst { it.name == "lint" }
        val testIndex = sorted.indexOfFirst { it.name == "test" }

        assertTrue(buildIndex < testIndex)
        assertTrue(lintIndex < testIndex)
    }

    @Test
    fun `sort diamond pattern`() {
        val a = createSegment("a")
        val b = createSegment("b", listOf("a"))
        val c = createSegment("c", listOf("a"))
        val d = createSegment("d", listOf("b", "c"))

        val graph = SegmentGraph(listOf(d, c, b, a)) // Reverse order
        val sorter = TopologicalSort(graph)

        val sorted = sorter.sort()

        // Verify dependencies are satisfied
        val indices = sorted.associate { it.name to sorted.indexOf(it) }

        assertTrue(indices["a"]!! < indices["b"]!!)
        assertTrue(indices["a"]!! < indices["c"]!!)
        assertTrue(indices["b"]!! < indices["d"]!!)
        assertTrue(indices["c"]!! < indices["d"]!!)
    }

    @Test
    fun `sort throws on cyclic graph`() {
        val a = createSegment("a", listOf("b"))
        val b = createSegment("b", listOf("a"))

        val graph = SegmentGraph(listOf(a, b))
        val sorter = TopologicalSort(graph)

        assertThrows<CyclicDependencyException> {
            sorter.sort()
        }
    }

    @Test
    fun `sortByLevels groups independent segments`() {
        val a = createSegment("a")
        val b = createSegment("b")
        val c = createSegment("c")

        val graph = SegmentGraph(listOf(a, b, c))
        val sorter = TopologicalSort(graph)

        val levels = sorter.sortByLevels()

        assertEquals(1, levels.size)
        assertEquals(3, levels[0].size) // All in same level (can run in parallel)
    }

    @Test
    fun `sortByLevels creates sequential levels`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test"))

        val graph = SegmentGraph(listOf(build, test, deploy))
        val sorter = TopologicalSort(graph)

        val levels = sorter.sortByLevels()

        assertEquals(3, levels.size)
        assertEquals(listOf("build"), levels[0].map { it.name })
        assertEquals(listOf("test"), levels[1].map { it.name })
        assertEquals(listOf("deploy"), levels[2].map { it.name })
    }

    @Test
    fun `sortByLevels handles mixed dependencies`() {
        val lint = createSegment("lint")
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test", "lint"))

        val graph = SegmentGraph(listOf(lint, build, test, deploy))
        val sorter = TopologicalSort(graph)

        val levels = sorter.sortByLevels()

        // Level 0: lint, build (independent)
        // Level 1: test (depends on build)
        // Level 2: deploy (depends on test and lint)
        assertEquals(3, levels.size)
        assertEquals(2, levels[0].size)
        assertTrue(levels[0].map { it.name }.containsAll(listOf("lint", "build")))
        assertEquals(listOf("test"), levels[1].map { it.name })
        assertEquals(listOf("deploy"), levels[2].map { it.name })
    }

    @Test
    fun `longestPath calculates critical path`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test"))

        val graph = SegmentGraph(listOf(build, test, deploy))
        val sorter = TopologicalSort(graph)

        assertEquals(3, sorter.longestPath())
    }

    @Test
    fun `longestPath for diamond pattern`() {
        val a = createSegment("a")
        val b = createSegment("b", listOf("a"))
        val c = createSegment("c", listOf("a"))
        val d = createSegment("d", listOf("b", "c"))

        val graph = SegmentGraph(listOf(a, b, c, d))
        val sorter = TopologicalSort(graph)

        // Critical path: a -> b -> d (or a -> c -> d)
        assertEquals(3, sorter.longestPath())
    }

    @Test
    fun `stats provides correct metrics for linear graph`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))
        val deploy = createSegment("deploy", listOf("test"))

        val graph = SegmentGraph(listOf(build, test, deploy))
        val sorter = TopologicalSort(graph)

        val stats = sorter.stats()

        assertEquals(3, stats.totalSegments)
        assertEquals(3, stats.levels)
        assertEquals(1, stats.maxParallelism) // No parallelism
        assertEquals(1, stats.minParallelism)
        assertEquals(1.0, stats.avgParallelism)
    }

    @Test
    fun `stats provides correct metrics for parallel graph`() {
        val a = createSegment("a")
        val b = createSegment("b")
        val c = createSegment("c")
        val d = createSegment("d", listOf("a", "b", "c"))

        val graph = SegmentGraph(listOf(a, b, c, d))
        val sorter = TopologicalSort(graph)

        val stats = sorter.stats()

        assertEquals(4, stats.totalSegments)
        assertEquals(2, stats.levels)
        assertEquals(3, stats.maxParallelism) // a, b, c in parallel
        assertEquals(1, stats.minParallelism)
        assertEquals(2.0, stats.avgParallelism) // (3 + 1) / 2
    }

    @Test
    fun `parallelizationEfficiency for fully parallel graph`() {
        val a = createSegment("a")
        val b = createSegment("b")
        val c = createSegment("c")

        val graph = SegmentGraph(listOf(a, b, c))
        val sorter = TopologicalSort(graph)

        val stats = sorter.stats()

        // All segments can run in parallel
        assertEquals(1.0, stats.parallelizationEfficiency)
    }

    @Test
    fun `parallelizationEfficiency for fully sequential graph`() {
        val a = createSegment("a")
        val b = createSegment("b", listOf("a"))
        val c = createSegment("c", listOf("b"))

        val graph = SegmentGraph(listOf(a, b, c))
        val sorter = TopologicalSort(graph)

        val stats = sorter.stats()

        // No parallelism possible
        assertEquals(1.0 / 3.0, stats.parallelizationEfficiency, 0.01)
    }

    @Test
    fun `SortStats toString includes all metrics`() {
        val build = createSegment("build")
        val test = createSegment("test", listOf("build"))

        val graph = SegmentGraph(listOf(build, test))
        val sorter = TopologicalSort(graph)

        val stats = sorter.stats()
        val str = stats.toString()

        assertTrue(str.contains("segments=2"))
        assertTrue(str.contains("levels=2"))
        assertTrue(str.contains("maxParallel=1"))
    }

    @Test
    fun `CyclicDependencyException includes cycle details`() {
        val a = createSegment("a", listOf("b"))
        val b = createSegment("b", listOf("c"))
        val c = createSegment("c", listOf("a"))

        val graph = SegmentGraph(listOf(a, b, c))
        val sorter = TopologicalSort(graph)

        val exception =
            assertThrows<CyclicDependencyException> {
                sorter.sort()
            }

        assertTrue(exception.message!!.contains("Cannot sort graph with cycles"))
        assertEquals(1, exception.cycles.size)
    }

    @Test
    fun `sort handles complex graph`() {
        // Complex graph with multiple independent chains
        val a = createSegment("a")
        val b = createSegment("b", listOf("a"))
        val c = createSegment("c")
        val d = createSegment("d", listOf("c"))
        val e = createSegment("e", listOf("b", "d"))

        val graph = SegmentGraph(listOf(e, d, c, b, a))
        val sorter = TopologicalSort(graph)

        val sorted = sorter.sort()
        val indices = sorted.associate { it.name to sorted.indexOf(it) }

        // Verify all dependencies are satisfied
        assertTrue(indices["a"]!! < indices["b"]!!)
        assertTrue(indices["b"]!! < indices["e"]!!)
        assertTrue(indices["c"]!! < indices["d"]!!)
        assertTrue(indices["d"]!! < indices["e"]!!)
    }
}
