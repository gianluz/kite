package io.kite.core

/**
 * Represents a ride configuration - a workflow composed of segments.
 *
 * A ride defines which segments to execute and in what order, including
 * support for parallel execution blocks.
 *
 * @property name Human-readable name for this ride
 * @property flow The execution flow (sequential, parallel, or nested structure)
 * @property environment Environment variables to set for all segments in this ride
 * @property maxConcurrency Maximum number of segments to run in parallel (null = unlimited)
 * @property onFailure Callback invoked if any segment in the ride fails
 */
data class Ride(
    val name: String,
    val flow: FlowNode,
    val environment: Map<String, String> = emptyMap(),
    val maxConcurrency: Int? = null,
    val onFailure: (suspend (Throwable) -> Unit)? = null
) {
    init {
        require(name.isNotBlank()) { "Ride name cannot be blank" }
        if (maxConcurrency != null) {
            require(maxConcurrency > 0) { "maxConcurrency must be > 0" }
        }
    }

    /**
     * Validates this ride configuration.
     *
     * @param availableSegments Set of all available segment names
     * @return List of validation errors (empty if valid)
     */
    fun validate(availableSegments: Set<String>): List<String> {
        val errors = mutableListOf<String>()
        val referencedSegments = flow.allSegmentReferences()

        // Check that all referenced segments exist
        val missingSegments = referencedSegments - availableSegments
        if (missingSegments.isNotEmpty()) {
            errors.add("Unknown segments referenced: ${missingSegments.joinToString()}")
        }

        // Check for empty parallel blocks
        flow.findEmptyParallelBlocks().forEach { index ->
            errors.add("Empty parallel block at position $index")
        }

        return errors
    }

    override fun toString(): String {
        return "Ride(name='$name', segments=${flow.allSegmentReferences().size}, " +
                "maxConcurrency=$maxConcurrency)"
    }
}

/**
 * Represents a node in the execution flow of a ride.
 *
 * This is a sealed class hierarchy that supports:
 * - Sequential execution of segments
 * - Parallel execution of segments
 * - Nested combinations of sequential and parallel blocks
 */
sealed class FlowNode {
    /**
     * Returns all segment names referenced in this flow node.
     */
    abstract fun allSegmentReferences(): Set<String>

    /**
     * Finds positions of empty parallel blocks.
     */
    abstract fun findEmptyParallelBlocks(): List<Int>

    /**
     * Sequential execution of multiple flow nodes.
     *
     * Nodes are executed one after another in order.
     */
    data class Sequential(val nodes: List<FlowNode>) : FlowNode() {
        init {
            require(nodes.isNotEmpty()) { "Sequential flow must have at least one node" }
        }

        override fun allSegmentReferences(): Set<String> =
            nodes.flatMap { it.allSegmentReferences() }.toSet()

        override fun findEmptyParallelBlocks(): List<Int> =
            nodes.flatMapIndexed { index, node ->
                if (node is Parallel && node.nodes.isEmpty()) {
                    listOf(index)
                } else {
                    node.findEmptyParallelBlocks()
                }
            }
    }

    /**
     * Parallel execution of multiple flow nodes.
     *
     * All nodes are executed concurrently.
     */
    data class Parallel(val nodes: List<FlowNode>) : FlowNode() {
        override fun allSegmentReferences(): Set<String> =
            nodes.flatMap { it.allSegmentReferences() }.toSet()

        override fun findEmptyParallelBlocks(): List<Int> =
            if (nodes.isEmpty()) listOf(0)
            else nodes.flatMap { it.findEmptyParallelBlocks() }
    }

    /**
     * Reference to a segment to execute.
     *
     * @property segmentName Name of the segment to execute
     * @property overrides Optional overrides for segment properties (e.g., timeout, dependencies)
     */
    data class SegmentRef(
        val segmentName: String,
        val overrides: SegmentOverrides = SegmentOverrides()
    ) : FlowNode() {
        init {
            require(segmentName.isNotBlank()) { "Segment name cannot be blank" }
        }

        override fun allSegmentReferences(): Set<String> = setOf(segmentName)

        override fun findEmptyParallelBlocks(): List<Int> = emptyList()
    }
}

/**
 * Overrides for segment properties in a ride configuration.
 *
 * These allow customizing segment behavior in the context of a specific ride
 * without modifying the segment definition itself.
 */
data class SegmentOverrides(
    val dependsOn: List<String>? = null,
    val condition: ((ExecutionContext) -> Boolean)? = null,
    val timeout: kotlin.time.Duration? = null,
    val enabled: Boolean = true
)
