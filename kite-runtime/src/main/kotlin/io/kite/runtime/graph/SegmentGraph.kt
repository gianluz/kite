package io.kite.runtime.graph

import io.kite.core.Segment

/**
 * Represents a directed graph of segment dependencies.
 *
 * This class builds a dependency graph (DAG) from a list of segments
 * and provides methods for analyzing and traversing the graph.
 */
class SegmentGraph(
    private val segments: List<Segment>,
) {
    private val nodes: Map<String, GraphNode>
    private val adjacencyList: Map<String, Set<String>>

    init {
        // Build nodes map
        nodes = segments.associateBy({ it.name }, { GraphNode(it) })

        // Build adjacency list (segment -> list of segments that depend on it)
        val adjacency = mutableMapOf<String, MutableSet<String>>()

        for (segment in segments) {
            // Initialize adjacency list for this segment
            adjacency.putIfAbsent(segment.name, mutableSetOf())

            // Add edges from dependencies to this segment
            for (dependency in segment.dependsOn) {
                adjacency.getOrPut(dependency) { mutableSetOf() }.add(segment.name)
            }
        }

        adjacencyList = adjacency.mapValues { it.value.toSet() }
    }

    /**
     * Returns all segments in the graph.
     */
    fun allSegments(): List<Segment> = segments

    /**
     * Returns a segment by name.
     */
    fun getSegment(name: String): Segment? = nodes[name]?.segment

    /**
     * Returns the dependencies of a segment.
     */
    fun getDependencies(segmentName: String): List<String> {
        return nodes[segmentName]?.segment?.dependsOn ?: emptyList()
    }

    /**
     * Returns the dependents of a segment (segments that depend on this one).
     */
    fun getDependents(segmentName: String): Set<String> {
        return adjacencyList[segmentName] ?: emptySet()
    }

    /**
     * Validates the graph for common issues.
     *
     * @return Validation result with any errors found
     */
    fun validate(): GraphValidationResult {
        val errors = mutableListOf<String>()

        // Check for missing dependencies
        for (segment in segments) {
            for (dependency in segment.dependsOn) {
                if (!nodes.containsKey(dependency)) {
                    errors.add("Segment '${segment.name}' depends on '$dependency' which does not exist")
                }
            }
        }

        // Check for cycles
        val cycles = detectCycles()
        if (cycles.isNotEmpty()) {
            for (cycle in cycles) {
                errors.add("Cycle detected: ${cycle.path.joinToString(" -> ")}")
            }
        }

        // Check for unreachable segments (segments with unsatisfied dependencies)
        val reachable = findReachableSegments()
        val unreachable = segments.filter { it.name !in reachable }
        if (unreachable.isNotEmpty()) {
            for (segment in unreachable) {
                val missingDeps = segment.dependsOn.filter { !nodes.containsKey(it) }
                if (missingDeps.isNotEmpty()) {
                    errors.add("Segment '${segment.name}' is unreachable due to missing dependencies: ${missingDeps.joinToString()}")
                }
            }
        }

        return GraphValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
        )
    }

    /**
     * Detects cycles in the graph using DFS.
     *
     * @return List of cycles found
     */
    fun detectCycles(): List<Cycle> {
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val cycles = mutableListOf<Cycle>()

        fun dfs(
            segmentName: String,
            path: List<String>,
        ) {
            if (segmentName in recursionStack) {
                // Found a cycle
                val cycleStart = path.indexOf(segmentName)
                val cyclePath = path.subList(cycleStart, path.size) + segmentName
                cycles.add(Cycle(cyclePath))
                return
            }

            if (segmentName in visited) {
                return
            }

            visited.add(segmentName)
            recursionStack.add(segmentName)

            val segment = nodes[segmentName]?.segment
            if (segment != null) {
                for (dependency in segment.dependsOn) {
                    if (nodes.containsKey(dependency)) {
                        dfs(dependency, path + segmentName)
                    }
                }
            }

            recursionStack.remove(segmentName)
        }

        for (segment in segments) {
            if (segment.name !in visited) {
                dfs(segment.name, emptyList())
            }
        }

        return cycles
    }

    /**
     * Finds all segments that are reachable (have all their dependencies satisfied).
     */
    private fun findReachableSegments(): Set<String> {
        val reachable = mutableSetOf<String>()
        val queue = mutableListOf<String>()

        // Start with segments that have no dependencies or all dependencies exist
        for (segment in segments) {
            val allDepsExist = segment.dependsOn.all { nodes.containsKey(it) }
            if (segment.dependsOn.isEmpty() || allDepsExist) {
                queue.add(segment.name)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (current in reachable) continue

            val segment = nodes[current]?.segment ?: continue
            val allDepsReachable = segment.dependsOn.all { it in reachable || it in queue }

            if (segment.dependsOn.isEmpty() || allDepsReachable) {
                reachable.add(current)
                // Add dependents to queue
                adjacencyList[current]?.forEach { dependent ->
                    if (dependent !in reachable) {
                        queue.add(dependent)
                    }
                }
            }
        }

        return reachable
    }

    /**
     * Returns statistics about the graph.
     */
    fun stats(): GraphStats {
        val maxDependencies = segments.maxOfOrNull { it.dependsOn.size } ?: 0
        val maxDependents = adjacencyList.values.maxOfOrNull { it.size } ?: 0
        val isolatedSegments = segments.count { it.dependsOn.isEmpty() && getDependents(it.name).isEmpty() }

        return GraphStats(
            totalSegments = segments.size,
            totalEdges = segments.sumOf { it.dependsOn.size },
            maxDependencies = maxDependencies,
            maxDependents = maxDependents,
            isolatedSegments = isolatedSegments,
        )
    }

    override fun toString(): String {
        return "SegmentGraph(segments=${segments.size}, edges=${stats().totalEdges})"
    }
}

/**
 * A node in the segment graph.
 */
private data class GraphNode(
    val segment: Segment,
)

/**
 * Represents a cycle in the dependency graph.
 */
data class Cycle(
    val path: List<String>,
) {
    override fun toString(): String = path.joinToString(" -> ")
}

/**
 * Result of graph validation.
 */
data class GraphValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
) {
    override fun toString(): String {
        return if (isValid) {
            "Graph is valid"
        } else {
            "Graph validation failed:\n" + errors.joinToString("\n") { "  - $it" }
        }
    }
}

/**
 * Statistics about the graph.
 */
data class GraphStats(
    val totalSegments: Int,
    val totalEdges: Int,
    val maxDependencies: Int,
    val maxDependents: Int,
    val isolatedSegments: Int,
)
