package io.kite.dsl

import io.kite.core.Ride
import io.kite.core.Segment
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics

/**
 * Discovers and loads Kite script files from the filesystem.
 *
 * Scans standard directories:
 * - `.kite/segments/` for segment definitions (*.kite.kts)
 * - `.kite/rides/` for ride configurations (*.kite.kts)
 */
class FileDiscovery(
    private val workspaceRoot: File = File("."),
    private val compiler: ScriptCompiler = ScriptCompiler(),
) {
    companion object {
        const val KITE_DIR = ".kite"
        const val SEGMENTS_DIR = "segments"
        const val RIDES_DIR = "rides"
        const val KITE_EXTENSION = ".kite.kts"
    }

    /**
     * Discovers all segment files in the workspace.
     *
     * @return List of segment files found
     */
    fun discoverSegmentFiles(): List<File> {
        val segmentsDir = File(workspaceRoot, "$KITE_DIR/$SEGMENTS_DIR")
        if (!segmentsDir.exists() || !segmentsDir.isDirectory) {
            return emptyList()
        }

        return segmentsDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(KITE_EXTENSION) }
            .toList()
    }

    /**
     * Discovers all ride files in the workspace.
     *
     * @return List of ride files found
     */
    fun discoverRideFiles(): List<File> {
        val ridesDir = File(workspaceRoot, "$KITE_DIR/$RIDES_DIR")
        if (!ridesDir.exists() || !ridesDir.isDirectory) {
            return emptyList()
        }

        return ridesDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(KITE_EXTENSION) }
            .toList()
    }

    /**
     * Loads all segments from discovered files.
     *
     * @return Result containing list of segments or errors
     */
    suspend fun loadSegments(): SegmentLoadResult {
        val files = discoverSegmentFiles()
        if (files.isEmpty()) {
            return SegmentLoadResult(emptyList(), emptyList())
        }

        val results =
            coroutineScope {
                files.map { file ->
                    async {
                        val result = compiler.compileAndEvaluate(file)
                        FileLoadResult(file, result)
                    }
                }.awaitAll()
            }

        val segments = mutableListOf<Segment>()
        val errors = mutableListOf<FileLoadError>()

        for (fileResult in results) {
            when (fileResult.result) {
                is ResultWithDiagnostics.Success -> {
                    val returnValue = fileResult.result.value.returnValue
                    when (returnValue) {
                        is ResultValue.Value -> {
                            @Suppress("UNCHECKED_CAST")
                            val segmentList = returnValue.value as? List<Segment>
                            if (segmentList != null) {
                                segments.addAll(segmentList)
                            } else {
                                errors.add(
                                    FileLoadError(
                                        fileResult.file,
                                        "Segment file must return List<Segment>, got: ${returnValue.value?.javaClass}",
                                    ),
                                )
                            }
                        }

                        else -> {
                            errors.add(
                                FileLoadError(
                                    fileResult.file,
                                    "Segment file did not return a value",
                                ),
                            )
                        }
                    }
                }

                is ResultWithDiagnostics.Failure -> {
                    val errorMessages = fileResult.result.reports.joinToString("\n") { it.message }
                    errors.add(FileLoadError(fileResult.file, errorMessages))
                }
            }
        }

        return SegmentLoadResult(segments, errors)
    }

    /**
     * Loads all rides from discovered files.
     *
     * @return Result containing list of rides or errors
     */
    suspend fun loadRides(): RideLoadResult {
        val files = discoverRideFiles()
        if (files.isEmpty()) {
            return RideLoadResult(emptyList(), emptyList())
        }

        val results =
            coroutineScope {
                files.map { file ->
                    async {
                        val result = compiler.compileAndEvaluate(file)
                        FileLoadResult(file, result)
                    }
                }.awaitAll()
            }

        val rides = mutableListOf<Ride>()
        val errors = mutableListOf<FileLoadError>()

        for (fileResult in results) {
            when (fileResult.result) {
                is ResultWithDiagnostics.Success -> {
                    val returnValue = fileResult.result.value.returnValue
                    when (returnValue) {
                        is ResultValue.Value -> {
                            val ride = returnValue.value as? Ride
                            if (ride != null) {
                                rides.add(ride)
                            } else {
                                errors.add(
                                    FileLoadError(
                                        fileResult.file,
                                        "Ride file must return Ride, got: ${returnValue.value?.javaClass}",
                                    ),
                                )
                            }
                        }

                        else -> {
                            errors.add(
                                FileLoadError(
                                    fileResult.file,
                                    "Ride file did not return a value",
                                ),
                            )
                        }
                    }
                }

                is ResultWithDiagnostics.Failure -> {
                    val errorMessages = fileResult.result.reports.joinToString("\n") { it.message }
                    errors.add(FileLoadError(fileResult.file, errorMessages))
                }
            }
        }

        return RideLoadResult(rides, errors)
    }

    /**
     * Loads all segments and rides.
     *
     * @return Combined result with both segments and rides
     */
    suspend fun loadAll(): KiteLoadResult {
        val segmentResult = loadSegments()
        val rideResult = loadRides()

        return KiteLoadResult(
            segments = segmentResult.segments,
            rides = rideResult.rides,
            errors = segmentResult.errors + rideResult.errors,
        )
    }
}

/**
 * Result of loading a single file.
 */
private data class FileLoadResult(
    val file: File,
    val result: ResultWithDiagnostics<kotlin.script.experimental.api.EvaluationResult>,
)

/**
 * Error loading a file.
 */
data class FileLoadError(
    val file: File,
    val message: String,
) {
    override fun toString(): String = "${file.path}: $message"
}

/**
 * Result of loading segments.
 */
data class SegmentLoadResult(
    val segments: List<Segment>,
    val errors: List<FileLoadError>,
) {
    val success: Boolean get() = errors.isEmpty()
}

/**
 * Result of loading rides.
 */
data class RideLoadResult(
    val rides: List<Ride>,
    val errors: List<FileLoadError>,
) {
    val success: Boolean get() = errors.isEmpty()
}

/**
 * Combined result of loading all Kite files.
 */
data class KiteLoadResult(
    val segments: List<Segment>,
    val rides: List<Ride>,
    val errors: List<FileLoadError>,
) {
    val success: Boolean get() = errors.isEmpty()

    /**
     * Returns a map of segment name to segment for quick lookup.
     */
    fun segmentMap(): Map<String, Segment> = segments.associateBy { it.name }

    /**
     * Returns a map of ride name to ride for quick lookup.
     */
    fun rideMap(): Map<String, Ride> = rides.associateBy { it.name }
}
