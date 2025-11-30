package io.kite.dsl

import java.io.File
import kotlin.io.path.exists
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates

/**
 * Custom dependency resolver for Kite-specific dependency sources.
 *
 * Handles:
 * - Local JAR files (via @DependsOnJar) - single file, no transitives
 * - Maven Local repository (via @DependsOnMavenLocal) - delegates to Ivy for transitive resolution
 *
 * This resolver is chained with FileSystemDependenciesResolver and IvyDependenciesResolver
 * to provide comprehensive dependency resolution.
 */
class KiteDependenciesResolver(
    private val ivyResolver: IvyDependenciesResolver = IvyDependenciesResolver(),
) : ExternalDependenciesResolver {
    companion object {
        private const val MAVEN_LOCAL_PREFIX = "mavenLocal:"
        private const val LOCAL_JAR_PREFIX = "localJar:"
    }

    override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean {
        // We don't add repositories, just resolve artifacts
        return false
    }

    override fun addRepository(
        repositoryCoordinates: RepositoryCoordinates,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?,
    ): ResultWithDiagnostics<Boolean> {
        // Not supported - we only resolve artifacts, not add repositories
        return false.asSuccess()
    }

    override fun acceptsArtifact(artifactCoordinates: String): Boolean {
        return artifactCoordinates.startsWith(MAVEN_LOCAL_PREFIX) ||
            artifactCoordinates.startsWith(LOCAL_JAR_PREFIX)
    }

    override suspend fun resolve(
        artifactCoordinates: String,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?,
    ): ResultWithDiagnostics<List<File>> {
        return when {
            artifactCoordinates.startsWith(MAVEN_LOCAL_PREFIX) -> {
                resolveMavenLocal(
                    artifactCoordinates.removePrefix(MAVEN_LOCAL_PREFIX),
                    sourceCodeLocation,
                )
            }

            artifactCoordinates.startsWith(LOCAL_JAR_PREFIX) -> {
                resolveLocalJar(
                    artifactCoordinates.removePrefix(LOCAL_JAR_PREFIX),
                    sourceCodeLocation,
                )
            }

            else -> {
                makeFailureResult("Unsupported artifact coordinates: $artifactCoordinates", sourceCodeLocation)
            }
        }
    }

    /**
     * Resolves a Maven Local artifact from ~/.m2/repository
     *
     * This delegates to IvyDependenciesResolver which will:
     * 1. Find the artifact in Maven Local
     * 2. Parse its POM file
     * 3. Resolve all transitive dependencies
     *
     * Format: groupId:artifactId:version
     */
    private suspend fun resolveMavenLocal(
        coordinates: String,
        sourceCodeLocation: SourceCode.LocationWithId?,
    ): ResultWithDiagnostics<List<File>> {
        val parts = coordinates.split(":")
        if (parts.size != 3) {
            return makeFailureResult(
                "Invalid Maven coordinates: $coordinates (expected format: groupId:artifactId:version)",
                sourceCodeLocation,
            )
        }

        // Quick existence check to provide better error messages
        val (groupId, artifactId, version) = parts
        val m2Repository = File(System.getProperty("user.home"), ".m2/repository")
        val groupPath = groupId.replace('.', '/')
        val jarPath = "$groupPath/$artifactId/$version/$artifactId-$version.jar"
        val jarFile = File(m2Repository, jarPath)

        if (!jarFile.exists()) {
            return makeFailureResult(
                """
                Maven Local artifact not found: $coordinates
                
                Expected location: ${jarFile.absolutePath}
                
                To install this artifact to Maven Local, run:
                  ./gradlew :$artifactId:publishToMavenLocal
                
                Or if it's an external dependency:
                  mvn dependency:get -Dartifact=$coordinates
                """.trimIndent(),
                sourceCodeLocation,
            )
        }

        // Delegate to Ivy for full transitive resolution (including transitives!)
        return ivyResolver.resolve(coordinates, ExternalDependenciesResolver.Options.Empty, sourceCodeLocation)
    }

    /**
     * Resolves a local JAR file from the filesystem
     *
     * Supports:
     * - Relative paths (resolved from script location or workspace)
     * - Absolute paths
     * - Home directory expansion (~)
     */
    private fun resolveLocalJar(
        path: String,
        sourceCodeLocation: SourceCode.LocationWithId?,
    ): ResultWithDiagnostics<List<File>> {
        // Expand home directory
        val expandedPath =
            if (path.startsWith("~/")) {
                System.getProperty("user.home") + path.substring(1)
            } else {
                path
            }

        val file = File(expandedPath)

        // Resolve path
        val resolvedFile =
            if (file.isAbsolute) {
                file
            } else {
                // Relative path - resolve from current working directory
                File(System.getProperty("user.dir"), expandedPath).canonicalFile
            }

        if (!resolvedFile.exists()) {
            return makeFailureResult(
                """
                Local JAR file not found: $path
                
                Tried location: ${resolvedFile.absolutePath}
                
                Current working directory: ${System.getProperty("user.dir")}
                
                Make sure the file exists and the path is correct.
                Supported path formats:
                  - Relative: ./plugins/my-plugin.jar
                  - Absolute: /opt/plugins/my-plugin.jar
                  - Home: ~/plugins/my-plugin.jar
                """.trimIndent(),
                sourceCodeLocation,
            )
        }

        if (!resolvedFile.isFile) {
            return makeFailureResult(
                "Path exists but is not a file: ${resolvedFile.absolutePath}",
                sourceCodeLocation,
            )
        }

        if (!resolvedFile.name.endsWith(".jar")) {
            return makeFailureResult(
                """
                File is not a JAR: ${resolvedFile.name}
                
                @DependsOnJar only supports .jar files.
                Found: ${resolvedFile.absolutePath}
                """.trimIndent(),
                sourceCodeLocation,
            )
        }

        println("✅ Loaded local JAR: ${resolvedFile.name} (${formatFileSize(resolvedFile.length())})")
        return listOf(resolvedFile).asSuccess()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
