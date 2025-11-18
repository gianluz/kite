package io.kite.dsl

import io.kite.core.*

/**
 * DSL marker for ride definitions.
 */
@DslMarker
annotation class RideDslMarker

/**
 * Builder for creating a ride configuration.
 *
 * This is the top-level entry point for defining a ride in a .kite.kts file:
 * ```
 * ride {
 *     name = "MR Ride"
 *
 *     flow {
 *         segment("build")
 *         parallel {
 *             segment("unitTest")
 *             segment("lint")
 *         }
 *     }
 * }
 * ```
 */
@RideDslMarker
class RideBuilder {
    var name: String? = null
    var maxConcurrency: Int? = null

    private val environmentVars = mutableMapOf<String, String>()
    private var flowNode: FlowNode? = null
    private var onSuccessFn: (suspend () -> Unit)? = null
    private var onFailureFn: (suspend (Throwable) -> Unit)? = null
    private var onCompleteFn: (suspend (Boolean) -> Unit)? = null

    /**
     * Adds an environment variable for this ride.
     *
     * @param key Environment variable name
     * @param value Environment variable value
     */
    fun env(
        key: String,
        value: String,
    ) {
        environmentVars[key] = value
    }

    /**
     * Defines the execution flow for this ride.
     *
     * @param block Flow configuration block
     */
    fun flow(block: FlowBuilder.() -> Unit) {
        val builder = FlowBuilder()
        builder.block()
        flowNode = builder.build()
    }

    /**
     * Sets a success handler for this ride.
     *
     * Called when all segments complete successfully.
     *
     * Usage:
     * ```
     * onSuccess {
     *     println("Ride completed successfully!")
     *     // Send notification, trigger deployment, etc.
     * }
     * ```
     */
    fun onSuccess(block: suspend () -> Unit) {
        onSuccessFn = block
    }

    /**
     * Sets a failure handler for this ride.
     *
     * Called when any segment fails.
     *
     * Usage:
     * ```
     * onFailure { error ->
     *     println("Ride failed: ${error.message}")
     *     // Send failure notification, rollback, etc.
     * }
     * ```
     */
    fun onFailure(block: suspend (Throwable) -> Unit) {
        onFailureFn = block
    }

    /**
     * Sets a completion handler for this ride.
     *
     * Called when ride completes (success or failure).
     *
     * Usage:
     * ```
     * onComplete { success ->
     *     println("Ride completed. Success: $success")
     *     // Cleanup, reporting, etc.
     * }
     * ```
     */
    fun onComplete(block: suspend (Boolean) -> Unit) {
        onCompleteFn = block
    }

    /**
     * Builds the Ride from the configured properties.
     */
    fun build(): Ride {
        requireNotNull(name) { "Ride must have a name" }
        requireNotNull(flowNode) { "Ride must have a flow" }

        return Ride(
            name = name!!,
            flow = flowNode!!,
            environment = environmentVars.toMap(),
            maxConcurrency = maxConcurrency,
            onSuccess = onSuccessFn,
            onFailure = onFailureFn,
            onComplete = onCompleteFn,
        )
    }
}

/**
 * Builder for defining the execution flow of a ride.
 *
 * Supports sequential and parallel execution:
 * ```
 * flow {
 *     segment("build")
 *
 *     parallel {
 *         segment("test1")
 *         segment("test2")
 *     }
 * }
 * ```
 */
@RideDslMarker
class FlowBuilder {
    private val nodes = mutableListOf<FlowNode>()

    /**
     * Adds a segment reference to the flow.
     *
     * @param segmentName Name of the segment to execute
     * @param block Optional configuration block for overrides
     */
    fun segment(
        segmentName: String,
        block: (SegmentOverridesBuilder.() -> Unit)? = null,
    ) {
        val overrides =
            if (block != null) {
                val builder = SegmentOverridesBuilder()
                builder.block()
                builder.build()
            } else {
                SegmentOverrides()
            }

        nodes.add(FlowNode.SegmentRef(segmentName, overrides))
    }

    /**
     * Defines a parallel execution block.
     *
     * @param block Parallel flow configuration
     */
    fun parallel(block: ParallelFlowBuilder.() -> Unit) {
        val builder = ParallelFlowBuilder()
        builder.block()
        nodes.add(builder.build())
    }

    /**
     * Builds the flow as a Sequential node if multiple nodes, or returns the single node.
     */
    fun build(): FlowNode {
        return when {
            nodes.isEmpty() -> error("Flow must contain at least one segment or parallel block")
            nodes.size == 1 -> nodes[0]
            else -> FlowNode.Sequential(nodes.toList())
        }
    }
}

/**
 * Builder for parallel execution blocks.
 *
 * ```
 * parallel {
 *     segment("test1")
 *     segment("test2")
 *     segment("test3")
 * }
 * ```
 */
@RideDslMarker
class ParallelFlowBuilder {
    private val nodes = mutableListOf<FlowNode>()

    /**
     * Adds a segment reference to the parallel block.
     *
     * @param segmentName Name of the segment to execute
     * @param block Optional configuration block for overrides
     */
    fun segment(
        segmentName: String,
        block: (SegmentOverridesBuilder.() -> Unit)? = null,
    ) {
        val overrides =
            if (block != null) {
                val builder = SegmentOverridesBuilder()
                builder.block()
                builder.build()
            } else {
                SegmentOverrides()
            }

        nodes.add(FlowNode.SegmentRef(segmentName, overrides))
    }

    /**
     * Builds the parallel flow node.
     */
    fun build(): FlowNode.Parallel {
        return FlowNode.Parallel(nodes.toList())
    }
}

/**
 * Builder for segment overrides in ride configurations.
 *
 * Allows customizing segment behavior within a specific ride:
 * ```
 * segment("test") {
 *     dependsOn("lint")
 *     timeout = 5.minutes
 *     enabled = false
 * }
 * ```
 */
@RideDslMarker
class SegmentOverridesBuilder {
    var enabled: Boolean = true
    var timeout: kotlin.time.Duration? = null

    private val dependencies = mutableListOf<String>()
    private var conditionFn: ((ExecutionContext) -> Boolean)? = null

    /**
     * Adds a dependency override.
     *
     * @param segmentName Name of segment to depend on
     */
    fun dependsOn(segmentName: String) {
        dependencies.add(segmentName)
    }

    /**
     * Adds multiple dependency overrides.
     *
     * @param segmentNames Names of segments to depend on
     */
    fun dependsOn(vararg segmentNames: String) {
        dependencies.addAll(segmentNames)
    }

    /**
     * Sets a condition override.
     *
     * @param block Condition lambda
     */
    fun condition(block: (ExecutionContext) -> Boolean) {
        conditionFn = block
    }

    /**
     * Builds the segment overrides.
     */
    fun build(): SegmentOverrides {
        return SegmentOverrides(
            dependsOn = if (dependencies.isNotEmpty()) dependencies.toList() else null,
            condition = conditionFn,
            timeout = timeout,
            enabled = enabled,
        )
    }
}

/**
 * Top-level function for defining a ride in a .kite.kts file.
 *
 * Usage:
 * ```
 * ride {
 *     name = "MR Ride"
 *
 *     flow {
 *         segment("build")
 *         parallel {
 *             segment("test")
 *             segment("lint")
 *         }
 *     }
 * }
 * ```
 *
 * @param block Configuration block for the ride
 * @return The configured Ride
 */
fun ride(block: RideBuilder.() -> Unit): Ride {
    val builder = RideBuilder()
    builder.block()
    return builder.build()
}
