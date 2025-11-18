package io.kite.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.io.path.exists

/**
 * Thread-safe artifact manifest that can be persisted and restored.
 *
 * Uses kotlinx.serialization for JSON persistence with atomic file operations.
 * Provides thread-safe read/write operations using ReentrantReadWriteLock.
 */
@Serializable
data class ArtifactManifestData(
    val artifacts: Map<String, ArtifactEntry> = emptyMap(),
    val rideName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val version: Int = 1
)

@Serializable
data class ArtifactEntry(
    val name: String,
    val relativePath: String,  // Relative to artifacts directory
    val type: String,  // "file" or "directory"
    val sizeBytes: Long,
    val createdAt: Long
)

/**
 * Thread-safe artifact manifest manager.
 *
 * Handles serialization/deserialization with atomic file operations.
 */
class ArtifactManifest(private val artifactsDir: File) {

    private val manifestFile = File(artifactsDir, MANIFEST_FILENAME)
    private val tempFile = File(artifactsDir, "$MANIFEST_FILENAME.tmp")
    private val lock = ReentrantReadWriteLock()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val MANIFEST_FILENAME = ".manifest.json"
    }

    /**
     * Thread-safe: Load manifest from disk.
     */
    fun load(): ArtifactManifestData? = lock.read {
        if (!manifestFile.exists()) return null

        try {
            val content = manifestFile.readText()
            json.decodeFromString<ArtifactManifestData>(content)
        } catch (e: Exception) {
            // If manifest is corrupted, return null
            null
        }
    }

    /**
     * Thread-safe: Save manifest to disk atomically.
     *
     * Uses atomic file operations to prevent corruption:
     * 1. Write to temp file
     * 2. Atomic rename
     */
    fun save(data: ArtifactManifestData) = lock.write {
        try {
            // Write to temp file first
            val jsonContent = json.encodeToString(data)
            tempFile.writeText(jsonContent)

            // Atomic rename
            Files.move(
                tempFile.toPath(),
                manifestFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Exception) {
            // Clean up temp file on failure
            tempFile.delete()
            throw e
        }
    }

    /**
     * Thread-safe: Update manifest by adding/updating artifact entries.
     */
    fun update(artifactManager: ArtifactManager, rideName: String? = null) = lock.write {
        val entries = mutableMapOf<String, ArtifactEntry>()

        for (name in artifactManager.list()) {
            val path = artifactManager.get(name) ?: continue
            val file = path.toFile()

            if (file.exists()) {
                val relativePath = file.toRelativeString(artifactsDir)
                val type = if (file.isDirectory) "directory" else "file"
                val size = calculateSize(file)

                entries[name] = ArtifactEntry(
                    name = name,
                    relativePath = relativePath,
                    type = type,
                    sizeBytes = size,
                    createdAt = System.currentTimeMillis()
                )
            }
        }

        val data = ArtifactManifestData(
            artifacts = entries,
            rideName = rideName,
            timestamp = System.currentTimeMillis()
        )

        save(data)
    }

    /**
     * Thread-safe: Restore artifacts from manifest into artifact manager.
     *
     * @return Number of artifacts successfully restored
     */
    fun restore(artifactManager: ArtifactManager): Int = lock.read {
        val data = load() ?: return 0
        var restored = 0

        for ((name, entry) in data.artifacts) {
            val artifactFile = File(artifactsDir, entry.relativePath)

            if (artifactFile.exists()) {
                artifactManager.put(name, artifactFile.toPath())
                restored++
            }
        }

        return restored
    }

    /**
     * Calculate total size of file or directory.
     */
    private fun calculateSize(file: File): Long {
        return if (file.isDirectory) {
            file.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } else {
            file.length()
        }
    }
}

/**
 * Extension: Save artifact manifest after ride completes.
 */
fun ArtifactManager.saveManifest(artifactsDir: File, rideName: String? = null) {
    val manifest = ArtifactManifest(artifactsDir)
    manifest.update(this, rideName)
}

/**
 * Extension: Restore artifacts from manifest at ride start.
 *
 * This allows artifacts from previous rides (e.g., downloaded from CI)
 * to be accessible via artifacts.get() in the current ride.
 *
 * @return Number of artifacts restored
 */
fun ArtifactManager.restoreFromManifest(artifactsDir: File): Int {
    val manifest = ArtifactManifest(artifactsDir)
    return manifest.restore(this)
}
