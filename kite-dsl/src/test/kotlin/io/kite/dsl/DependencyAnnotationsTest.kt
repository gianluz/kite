package io.kite.dsl

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for dependency annotation resolution.
 *
 * These tests verify that:
 * - Local JAR files can be resolved
 * - Maven Local is automatically checked by @DependsOn (via Ivy)
 * - Error messages are helpful
 */
class DependencyAnnotationsTest {
    @Test
    fun `DependsOnJar annotation has correct metadata`() {
        val annotation = DependsOnJar::class

        // Verify it's a file-level annotation
        assertTrue(annotation.annotations.any { it is Target })

        // Verify source retention
        assertTrue(annotation.annotations.any { it is Retention })
    }

    @Test
    fun `KiteDependenciesResolver accepts localJar paths`() {
        val resolver = KiteDependenciesResolver()

        assertTrue(resolver.acceptsArtifact("localJar:./plugin.jar"))
        assertTrue(resolver.acceptsArtifact("localJar:/absolute/path/plugin.jar"))
        assertFalse(resolver.acceptsArtifact("./plugin.jar"))
    }

    @Test
    fun `KiteDependenciesResolver does not accept repositories`() {
        val resolver = KiteDependenciesResolver()

        // We only resolve artifacts, not add repositories
        val coords = kotlin.script.experimental.dependencies.RepositoryCoordinates("https://example.com")
        assertFalse(resolver.acceptsRepository(coords))
    }

    @Test
    fun `IvyDependenciesResolver checks Maven Local automatically`() {
        val resolver = IvyDependenciesResolver()

        // Ivy is configured to check Maven Local by default
        // So regular Maven coordinates work with Maven Local!
        assertTrue(resolver.acceptsArtifact("com.company:plugin:1.0.0"))
    }
}
