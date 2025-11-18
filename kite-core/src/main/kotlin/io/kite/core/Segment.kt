package io.kite.core

import kotlin.time.Duration

/**
 * Represents a segment (unit of work) in a Kite ride.
 *
 * A segment is a discrete step in a CI/CD workflow that can execute commands,
 * produce artifacts, and depend on other segments.
 *
 * @property name Unique identifier for this segment
 * @property description Human-readable description of what this segment does
 * @property dependsOn List of segment names that must complete before this segment runs
 * @property condition Lambda that determines if this segment should execute (null = always execute)
 * @property timeout Maximum execution time for this segment (null = no timeout)
 * @property maxRetries Number of times to retry on failure (0 = no retries)
 * @property retryDelay Delay between retry attempts
 * @property retryOn List of exception types that should trigger a retry
 * @property inputs List of artifact names this segment requires as inputs
 * @property outputs Map of artifact names to file paths this segment produces
 * @property execute Lambda that performs the actual work of this segment
 */
data class Segment(
    val name: String,
    val description: String? = null,
    val dependsOn: List<String> = emptyList(),
    val condition: ((ExecutionContext) -> Boolean)? = null,
    val timeout: Duration? = null,
    val maxRetries: Int = 0,
    val retryDelay: Duration = Duration.ZERO,
    val retryOn: List<String> = emptyList(), // Exception class names
    val inputs: List<String> = emptyList(), // Input artifact names
    val outputs: Map<String, String> = emptyMap(), // Output artifact name -> path
    val execute: suspend ExecutionContext.() -> Unit,
) {
    init {
        require(name.isNotBlank()) { "Segment name cannot be blank" }
        require(maxRetries >= 0) { "maxRetries must be >= 0" }
        require(!retryDelay.isNegative()) { "retryDelay must not be negative" }
    }

    /**
     * Checks if this segment should execute based on its condition.
     * Returns true if there is no condition, or if the condition evaluates to true.
     */
    fun shouldExecute(context: ExecutionContext): Boolean {
        return condition?.invoke(context) ?: true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Segment) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "Segment(name='$name', description='$description', dependsOn=$dependsOn)"
    }
}

/**
 * Represents the execution status of a segment.
 */
enum class SegmentStatus {
    /** Segment is waiting to be executed */
    PENDING,

    /** Segment is currently executing */
    RUNNING,

    /** Segment completed successfully */
    SUCCESS,

    /** Segment failed */
    FAILURE,

    /** Segment was skipped due to condition */
    SKIPPED,

    /** Segment execution timed out */
    TIMEOUT,

    ;

    /**
     * Returns true if this status represents a completed state (not pending or running).
     */
    val isCompleted: Boolean
        get() = this != PENDING && this != RUNNING

    /**
     * Returns true if this status represents a successful completion.
     */
    val isSuccessful: Boolean
        get() = this == SUCCESS || this == SKIPPED

    /**
     * Returns true if this status represents a failure.
     */
    val isFailed: Boolean
        get() = this == FAILURE || this == TIMEOUT
}
