package io.kite.runtime.graph

import io.kite.core.Segment

/**
 * Performs topological sorting on a segment graph.
 *
 * Uses Kahn's algorithm to produce a valid execution order where
 * all dependencies are satisfied before a segment executes.
 */
class TopologicalSort(
    private val graph: SegmentGraph
) {
    /**
     * Sorts segments in topological order.
     *
     * Returns a list of segments where each segment appears after all its dependencies.
     * Segments with no dependencies (or satisfied dependencies) appear first.
     *
     * @return Topologically sorted list of segments
     * @throws CyclicDependencyException if the graph contains cycles
     */
    fun sort(): List<Segment> {
        // First check for cycles
        val cycles = graph.detectCycles()
        if (cycles.isNotEmpty()) {
            throw CyclicDependencyException(cycles)
        }

        val segments = graph.allSegments()
        if (segments.isEmpty()) {
            return emptyList()
        }

        // Kahn's algorithm
        // Calculate in-degree for each segment
        val inDegree = mutableMapOf<String, Int>()
        for (segment in segments) {
            inDegree[segment.name] = segment.dependsOn.size
        }

        // Queue of segments with no dependencies (in-degree = 0)
        val queue = mutableListOf<Segment>()
        for (segment in segments) {
            if (inDegree[segment.name] == 0) {
                queue.add(segment)
            }
        }

        val sorted = mutableListOf<Segment>()

        while (queue.isNotEmpty()) {
            // Remove a segment with no incoming edges
            val current = queue.removeAt(0)
            sorted.add(current)

            // For each segment that depends on current, decrease its in-degree
            for (dependent in graph.getDependents(current.name)) {
                val dependentSegment = graph.getSegment(dependent)
                if (dependentSegment != null) {
                    inDegree[dependent] = (inDegree[dependent] ?: 0) - 1

                    // If in-degree becomes 0, add to queue
                    if (inDegree[dependent] == 0) {
                        queue.add(dependentSegment)
                    }
                }
            }
        }

        // If sorted doesn't contain all segments, there's a cycle
        // (This shouldn't happen since we check for cycles first)
        if (sorted.size != segments.size) {
            val missing = segments.filter { it !in sorted }
            throw IllegalStateException(
                "Topological sort failed: ${missing.size} segments not sorted. " +
                        "This indicates a cycle or missing dependencies."
            )
        }

        return sorted
    }

    /**
     * Groups segments into execution levels.
     *
     * Segments in the same level can be executed in parallel.
     * Each level depends only on segments from previous levels.
     *
     * @return List of levels, where each level is a list of segments
     */
    fun sortByLevels(): List<List<Segment>> {
        val sorted = sort()
        if (sorted.isEmpty()) {
            return emptyList()
        }

        val levels = mutableListOf<List<Segment>>()
        val levelMap = mutableMapOf<String, Int>() // segment name -> level number

        // Calculate level for each segment
        for (segment in sorted) {
            // Level is max(dependency levels) + 1
            val dependencyLevels = segment.dependsOn.mapNotNull { levelMap[it] }
            val level = if (dependencyLevels.isEmpty()) 0 else dependencyLevels.maxOrNull()!! + 1

            levelMap[segment.name] = level

            // Add segment to its level
            while (levels.size <= level) {
                levels.add(mutableListOf())
            }
            (levels[level] as MutableList).add(segment)
        }

        return levels
    }

    /**
     * Finds the longest path in the dependency graph.
     *
     * This represents the critical path - the minimum time needed
     * to execute all segments even with unlimited parallelism.
     *
     * @return Length of the longest path (number of segments)
     */
    fun longestPath(): Int {
        val levels = sortByLevels()
        return levels.size
    }

    /**
     * Calculates execution statistics.
     *
     * @return Statistics about the sorted graph
     */
    fun stats(): SortStats {
        val sorted = sort()
        val levels = sortByLevels()

        val maxParallelism = levels.maxOfOrNull { it.size } ?: 0
        val minParallelism = levels.minOfOrNull { it.size } ?: 0
        val avgParallelism = if (levels.isEmpty()) 0.0 else levels.map { it.size }.average()

        return SortStats(
            totalSegments = sorted.size,
            levels = levels.size,
            maxParallelism = maxParallelism,
            minParallelism = minParallelism,
            avgParallelism = avgParallelism
        )
    }
}

/**
 * Exception thrown when attempting to sort a graph with cycles.
 */
class CyclicDependencyException(
    val cycles: List<Cycle>
) : Exception(
    "Cannot sort graph with cycles: " + cycles.joinToString("; ") { it.toString() }
)

/**
 * Statistics about topological sort results.
 */
data class SortStats(
    val totalSegments: Int,
    val levels: Int,
    val maxParallelism: Int,
    val minParallelism: Int,
    val avgParallelism: Double
) {
    /**
     * Parallelization efficiency: how well segments can be parallelized.
     * 1.0 = perfect (all segments at same level)
     * 0.0 = no parallelism (all segments sequential)
     */
    val parallelizationEfficiency: Double
        get() = if (totalSegments <= 1) 1.0
        else avgParallelism / totalSegments

    override fun toString(): String {
        return "SortStats(segments=$totalSegments, levels=$levels, " +
                "maxParallel=$maxParallelism, avgParallel=${"%.2f".format(avgParallelism)}, " +
                "efficiency=${"%.2f".format(parallelizationEfficiency * 100)}%)"
    }
}
