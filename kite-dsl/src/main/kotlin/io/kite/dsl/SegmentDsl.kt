package io.kite.dsl

import io.kite.core.ExecutionContext
import io.kite.core.Segment
import kotlin.time.Duration

/**
 * DSL marker for segment definitions.
 */
@DslMarker
annotation class SegmentDslMarker

/**
 * Builder for creating segments.
 *
 * This is the top-level entry point for defining segments in a .kite.kts file:
 * ```
 * segments {
 *     segment("build") {
 *         description = "Build the app"
 *         execute {
 *             // execution logic
 *         }
 *     }
 * }
 * ```
 */
@SegmentDslMarker
class SegmentsBuilder {
    private val segments = mutableListOf<Segment>()

    /**
     * Defines a new segment.
     *
     * @param name Unique name for this segment
     * @param block Configuration block for the segment
     */
    fun segment(
        name: String,
        block: SegmentBuilder.() -> Unit,
    ) {
        val builder = SegmentBuilder(name)
        builder.block()
        segments.add(builder.build())
    }

    /**
     * Returns all defined segments.
     */
    fun build(): List<Segment> = segments.toList()
}

/**
 * Builder for a single segment.
 *
 * Provides a DSL for configuring segment properties:
 * ```
 * segment("test") {
 *     description = "Run tests"
 *     dependsOn("build")
 *     timeout = 10.minutes
 *
 *     execute {
 *         exec("./gradlew", "test")
 *     }
 * }
 * ```
 */
@SegmentDslMarker
class SegmentBuilder(private val name: String) {
    var description: String? = null
    var timeout: Duration? = null
    var maxRetries: Int = 0
    var retryDelay: Duration = Duration.ZERO

    private val dependencies = mutableListOf<String>()
    private val retryExceptions = mutableListOf<String>()
    private val inputArtifacts = mutableListOf<String>()
    private val outputArtifacts = mutableMapOf<String, String>()
    private var conditionFn: ((ExecutionContext) -> Boolean)? = null
    private var executeFn: (suspend ExecutionContext.() -> Unit)? = null

    /**
     * Adds a dependency on another segment.
     *
     * @param segmentName Name of the segment this segment depends on
     */
    fun dependsOn(segmentName: String) {
        dependencies.add(segmentName)
    }

    /**
     * Adds multiple dependencies.
     *
     * @param segmentNames Names of segments this segment depends on
     */
    fun dependsOn(vararg segmentNames: String) {
        dependencies.addAll(segmentNames)
    }

    /**
     * Sets a condition for executing this segment.
     *
     * @param block Condition lambda that returns true if segment should execute
     */
    fun condition(block: (ExecutionContext) -> Boolean) {
        conditionFn = block
    }

    /**
     * Adds an exception type that should trigger a retry.
     *
     * @param exceptionClassName Fully qualified name of the exception class
     */
    fun retryOn(exceptionClassName: String) {
        retryExceptions.add(exceptionClassName)
    }

    /**
     * Adds multiple exception types that should trigger retries.
     *
     * @param exceptionClassNames Fully qualified names of exception classes
     */
    fun retryOn(vararg exceptionClassNames: String) {
        retryExceptions.addAll(exceptionClassNames)
    }

    /**
     * Declares artifacts that this segment needs as inputs.
     *
     * These artifacts must be produced by segments that this segment depends on.
     *
     * Usage:
     * ```
     * inputs {
     *     artifact("apk")
     *     artifact("test-config")
     * }
     * ```
     */
    fun inputs(block: ArtifactInputsBuilder.() -> Unit) {
        val builder = ArtifactInputsBuilder()
        builder.block()
        inputArtifacts.addAll(builder.build())
    }

    /**
     * Declares artifacts that this segment produces as outputs.
     *
     * These artifacts will be stored and made available to dependent segments.
     *
     * Usage:
     * ```
     * outputs {
     *     artifact("apk", "app/build/outputs/apk/release/app-release.apk")
     *     artifact("test-results", "app/build/test-results/")
     * }
     * ```
     */
    fun outputs(block: ArtifactOutputsBuilder.() -> Unit) {
        val builder = ArtifactOutputsBuilder()
        builder.block()
        outputArtifacts.putAll(builder.build())
    }

    /**
     * Defines the execution logic for this segment.
     *
     * @param block Execution lambda with ExecutionContext receiver
     */
    fun execute(block: suspend ExecutionContext.() -> Unit) {
        executeFn = block
    }

    /**
     * Builds the Segment from the configured properties.
     */
    fun build(): Segment {
        requireNotNull(executeFn) { "Segment '$name' must have an execute block" }

        return Segment(
            name = name,
            description = description,
            dependsOn = dependencies.toList(),
            condition = conditionFn,
            timeout = timeout,
            maxRetries = maxRetries,
            retryDelay = retryDelay,
            retryOn = retryExceptions.toList(),
            inputs = inputArtifacts.toList(),
            outputs = outputArtifacts.toMap(),
            execute = executeFn!!,
        )
    }
}

/**
 * Builder for artifact inputs.
 */
@SegmentDslMarker
class ArtifactInputsBuilder {
    private val artifacts = mutableListOf<String>()

    /**
     * Declares an artifact that this segment needs as input.
     *
     * @param name Name of the artifact
     */
    fun artifact(name: String) {
        artifacts.add(name)
    }

    fun build(): List<String> = artifacts.toList()
}

/**
 * Builder for artifact outputs.
 */
@SegmentDslMarker
class ArtifactOutputsBuilder {
    private val artifacts = mutableMapOf<String, String>()

    /**
     * Declares an artifact that this segment produces.
     *
     * @param name Name of the artifact
     * @param path Path to the artifact file or directory (relative to workspace)
     */
    fun artifact(
        name: String,
        path: String,
    ) {
        artifacts[name] = path
    }

    fun build(): Map<String, String> = artifacts.toMap()
}

/**
 * Top-level function for defining segments in a .kite.kts file.
 *
 * Usage:
 * ```
 * segments {
 *     segment("build") { /* ... */ }
 *     segment("test") { /* ... */ }
 * }
 * ```
 *
 * @param block Configuration block for segments
 * @return List of defined segments
 */
fun segments(block: SegmentsBuilder.() -> Unit): List<Segment> {
    val builder = SegmentsBuilder()
    builder.block()
    return builder.build()
}
