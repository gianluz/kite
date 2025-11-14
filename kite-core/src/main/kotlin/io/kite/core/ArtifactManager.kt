package io.kite.core

import java.nio.file.Path

/**
 * Manages artifacts produced by segments and shared between segments.
 *
 * Artifacts are named outputs that can be passed from one segment to another.
 * For example, a build segment might produce an "apk" artifact that is consumed
 * by a test segment.
 */
interface ArtifactManager {
    /**
     * Stores an artifact with the given name.
     *
     * @param name Unique name for this artifact
     * @param path Path to the artifact file or directory
     */
    fun put(name: String, path: Path)

    /**
     * Retrieves an artifact by name.
     *
     * @param name Name of the artifact to retrieve
     * @return Path to the artifact, or null if not found
     */
    fun get(name: String): Path?

    /**
     * Checks if an artifact exists.
     *
     * @param name Name of the artifact
     * @return true if the artifact exists, false otherwise
     */
    fun has(name: String): Boolean

    /**
     * Lists all artifact names.
     *
     * @return Set of all artifact names
     */
    fun list(): Set<String>

    /**
     * Removes an artifact.
     *
     * @param name Name of the artifact to remove
     */
    fun remove(name: String)

    /**
     * Clears all artifacts.
     */
    fun clear()
}

/**
 * In-memory implementation of ArtifactManager.
 *
 * This implementation stores artifact paths in memory and is suitable
 * for single-process execution. For distributed execution, a different
 * implementation (e.g., backed by S3) would be needed.
 */
class InMemoryArtifactManager : ArtifactManager {
    private val artifacts = mutableMapOf<String, Path>()

    override fun put(name: String, path: Path) {
        require(name.isNotBlank()) { "Artifact name cannot be blank" }
        artifacts[name] = path
    }

    override fun get(name: String): Path? = artifacts[name]

    override fun has(name: String): Boolean = artifacts.containsKey(name)

    override fun list(): Set<String> = artifacts.keys.toSet()

    override fun remove(name: String) {
        artifacts.remove(name)
    }

    override fun clear() {
        artifacts.clear()
    }

    override fun toString(): String {
        return "InMemoryArtifactManager(artifacts=${artifacts.keys})"
    }
}
