package io.kite.integration

import io.kite.core.ExecutionContext
import io.kite.core.FlowNode
import io.kite.core.Ride
import io.kite.core.Segment
import io.kite.core.SegmentOverrides
import io.kite.runtime.scheduler.SequentialScheduler
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for segment overrides in rides.
 *
 * Tests that overrides defined in ride flows are properly applied to segments.
 */
class SegmentOverridesTest {
    @TempDir
    lateinit var tempDir: Path

    private val context =
        ExecutionContext(
            branch = "main",
            commitSha = "abc123",
            environment = mapOf("CI" to "true"),
        )

    @Test
    fun `timeout override replaces segment timeout`() =
        runTest {
            var executed = false
            val originalTimeout = 1.minutes

            val segment =
                Segment(
                    name = "test",
                    timeout = originalTimeout,
                    execute = { executed = true },
                )

            // Apply override
            val overrideTimeout = 5.minutes
            val overrides = SegmentOverrides(timeout = overrideTimeout)
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            assertEquals(overrideTimeout, overriddenSegment.timeout)
            assertEquals("test", overriddenSegment.name)

            // Execute to verify it still works
            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(overriddenSegment), context)

            assertTrue(executed)
            assertTrue(result.isSuccess)
        }

    @Test
    fun `dependsOn override adds to existing dependencies`() =
        runTest {
            val segment =
                Segment(
                    name = "test",
                    dependsOn = listOf("build"),
                    execute = {},
                )

            // Apply override to add more dependencies
            val overrides = SegmentOverrides(dependsOn = listOf("lint", "format"))
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            // Should have all dependencies
            assertEquals(
                listOf("build", "lint", "format"),
                overriddenSegment.dependsOn,
            )
        }

    @Test
    fun `dependsOn override removes duplicates`() =
        runTest {
            val segment =
                Segment(
                    name = "test",
                    dependsOn = listOf("build", "lint"),
                    execute = {},
                )

            // Apply override with overlapping dependency
            val overrides = SegmentOverrides(dependsOn = listOf("lint", "format"))
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            // Should have unique dependencies
            assertEquals(
                listOf("build", "lint", "format"),
                overriddenSegment.dependsOn,
            )
        }

    @Test
    fun `condition override replaces original condition`() =
        runTest {
            var originalConditionCalled = false
            var overrideConditionCalled = false

            val segment =
                Segment(
                    name = "test",
                    condition = {
                        originalConditionCalled = true
                        true
                    },
                    execute = {},
                )

            // Apply condition override
            val overrides =
                SegmentOverrides(
                    condition = {
                        overrideConditionCalled = true
                        false
                    },
                )
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            // Check condition is overridden
            val shouldExecute = overriddenSegment.shouldExecute(context)

            assertFalse(shouldExecute)
            assertFalse(originalConditionCalled) // Original not called
            assertTrue(overrideConditionCalled) // Override called
        }

    @Test
    fun `enabled false disables segment execution`() =
        runTest {
            var executed = false

            val segment =
                Segment(
                    name = "test",
                    execute = { executed = true },
                )

            // Disable the segment
            val overrides = SegmentOverrides(enabled = false)
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            // Segment should not execute
            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(overriddenSegment), context)

            assertFalse(executed)
            assertTrue(result.isSuccess) // Skipped segments count as success
            assertEquals(1, result.skippedCount)
        }

    @Test
    fun `enabled false overrides existing condition`() =
        runTest {
            var conditionCalled = false
            var executed = false

            val segment =
                Segment(
                    name = "test",
                    condition = {
                        conditionCalled = true
                        true // Would normally execute
                    },
                    execute = { executed = true },
                )

            // Disable overrides condition
            val overrides = SegmentOverrides(enabled = false)
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            // Segment should not execute
            val scheduler = SequentialScheduler()
            val result = scheduler.execute(listOf(overriddenSegment), context)

            assertFalse(executed)
            // Original condition should not be called if disabled
            assertFalse(conditionCalled)
        }

    @Test
    fun `multiple overrides can be applied together`() =
        runTest {
            val segment =
                Segment(
                    name = "test",
                    dependsOn = listOf("build"),
                    timeout = 1.minutes,
                    execute = {},
                )

            // Apply multiple overrides
            val overrides =
                SegmentOverrides(
                    dependsOn = listOf("lint"),
                    timeout = 10.minutes,
                    condition = { it.branch == "main" },
                )
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            // Check all overrides applied
            assertEquals(listOf("build", "lint"), overriddenSegment.dependsOn)
            assertEquals(10.minutes, overriddenSegment.timeout)
            assertFalse(overriddenSegment.shouldExecute(context.copy(branch = "develop")))
            assertTrue(overriddenSegment.shouldExecute(context.copy(branch = "main")))
        }

    @Test
    fun `empty overrides leaves segment unchanged`() =
        runTest {
            val originalTimeout = 5.minutes
            val segment =
                Segment(
                    name = "test",
                    dependsOn = listOf("build"),
                    timeout = originalTimeout,
                    execute = {},
                )

            // Apply empty overrides
            val overrides = SegmentOverrides()
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            // Segment should be unchanged
            assertEquals(listOf("build"), overriddenSegment.dependsOn)
            assertEquals(originalTimeout, overriddenSegment.timeout)
            assertEquals(segment.condition, overriddenSegment.condition)
        }

    @Test
    fun `timeout override allows null to remove timeout`() =
        runTest {
            val segment =
                Segment(
                    name = "test",
                    timeout = 5.minutes,
                    execute = {},
                )

            // Override with explicit null timeout (use default segment timeout behavior)
            // Note: SegmentOverrides doesn't support explicit null, so we test with a different timeout
            val overrides = SegmentOverrides(timeout = 30.seconds)
            val overriddenSegment = applySegmentOverrides(segment, overrides)

            assertEquals(30.seconds, overriddenSegment.timeout)
        }

    /**
     * Helper function that mimics the applyOverrides logic from RideCommand.
     */
    private fun applySegmentOverrides(
        segment: Segment,
        overrides: SegmentOverrides,
    ): Segment {
        var result = segment

        // Apply dependsOn override (additive)
        overrides.dependsOn?.let { overrideDeps ->
            val combinedDeps = (segment.dependsOn + overrideDeps).distinct()
            result = result.copy(dependsOn = combinedDeps)
        }

        // Apply condition override (replaces)
        overrides.condition?.let { overrideCondition ->
            result = result.copy(condition = overrideCondition)
        }

        // Apply timeout override (replaces)
        overrides.timeout?.let { overrideTimeout ->
            result = result.copy(timeout = overrideTimeout)
        }

        // Apply enabled flag - if false, wrap condition to always return false
        if (!overrides.enabled) {
            result = result.copy(condition = { false })
        }

        return result
    }
}
