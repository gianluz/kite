package io.kite.dsl

import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.ChainResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*

/**
 * Ivy-based dependency resolver that works with Java 17+.
 *
 * This replaces MavenDependenciesResolver which uses Maven/Aether 3.6.x
 * that has Java 17 incompatibility issues.
 *
 * Based on the implementation in kotlin-main-kts which uses Ivy successfully with Java 17.
 */
class IvyDependenciesResolver : ExternalDependenciesResolver {
    private val ivy: Ivy by lazy { createIvy() }

    override fun acceptsRepository(repositoryCoordinates: RepositoryCoordinates): Boolean {
        // Accept http/https URLs or "mavenCentral" alias
        val coords = repositoryCoordinates.string
        return coords.startsWith("http://") ||
            coords.startsWith("https://") ||
            coords == "mavenCentral" ||
            coords.startsWith("file://")
    }

    override fun acceptsArtifact(artifactCoordinates: String): Boolean {
        // Accept Maven-style coordinates: groupId:artifactId:version
        val parts = artifactCoordinates.split(":")
        return parts.size >= 3
    }

    override suspend fun resolve(
        artifactCoordinates: String,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?,
    ): ResultWithDiagnostics<List<File>> {
        return try {
            val files = resolveArtifact(artifactCoordinates)
            files.asSuccess()
        } catch (e: Exception) {
            makeFailureResult("Failed to resolve $artifactCoordinates: ${e.message}", sourceCodeLocation)
        }
    }

    override fun addRepository(
        repositoryCoordinates: RepositoryCoordinates,
        options: ExternalDependenciesResolver.Options,
        sourceCodeLocation: SourceCode.LocationWithId?,
    ): ResultWithDiagnostics<Boolean> {
        return try {
            val coords = repositoryCoordinates.string
            when {
                coords == "mavenCentral" -> {
                    // Already included by default
                    true.asSuccess()
                }

                coords.startsWith("http://") || coords.startsWith("https://") -> {
                    val resolver = createMavenResolver("repo-${coords.hashCode()}", coords)
                    ivy.settings.addResolver(resolver)
                    (ivy.settings.getResolver("kite-chain") as? ChainResolver)?.add(resolver)
                    true.asSuccess()
                }

                coords.startsWith("file://") -> {
                    val url = coords
                    val resolver = createMavenResolver("local-${coords.hashCode()}", url)
                    ivy.settings.addResolver(resolver)
                    (ivy.settings.getResolver("kite-chain") as? ChainResolver)?.add(resolver)
                    true.asSuccess()
                }

                else -> {
                    makeFailureResult("Unsupported repository: $coords", sourceCodeLocation)
                }
            }
        } catch (e: Exception) {
            makeFailureResult(
                "Failed to add repository ${repositoryCoordinates.string}: ${e.message}",
                sourceCodeLocation,
            )
        }
    }

    private fun createIvy(): Ivy {
        val ivySettings =
            IvySettings().apply {
                // Set to warn level to reduce noise
                Message.setDefaultLogger(DefaultMessageLogger(Message.MSG_WARN))

                // Default cache directory
                defaultCache = File(System.getProperty("user.home"), ".ivy2/cache")

                // Create chain resolver
                val chainResolver =
                    ChainResolver().apply {
                        name = "kite-chain"

                        // Add Maven Central
                        add(createMavenResolver("central", "https://repo1.maven.org/maven2/"))

                        // Add local Maven repo if it exists
                        val localM2 = File(System.getProperty("user.home"), ".m2/repository")
                        if (localM2.exists()) {
                            add(createMavenResolver("local", localM2.toURI().toString()))
                        }
                    }

                addResolver(chainResolver)
                setDefaultResolver(chainResolver.name)
            }

        return Ivy.newInstance(ivySettings)
    }

    private fun createMavenResolver(
        name: String,
        root: String,
    ): IBiblioResolver {
        return IBiblioResolver().apply {
            this.name = name
            this.root = root
            isM2compatible = true
        }
    }

    private fun resolveArtifact(coordinates: String): List<File> {
        // Create module descriptor
        val moduleId = ModuleRevisionId.newInstance("kite.temp", "script-${System.currentTimeMillis()}", "1.0")
        val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(moduleId)

        // Parse and add dependency
        val depId = parseMavenCoordinate(coordinates)
        val dependency = DefaultDependencyDescriptor(moduleDescriptor, depId, false, false, true)
        dependency.addDependencyConfiguration("default", "default")
        moduleDescriptor.addDependency(dependency)

        // Resolve
        val resolveOptions =
            ResolveOptions().apply {
                isTransitive = true
                confs = arrayOf("default")
            }

        val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

        if (resolveReport.hasError()) {
            val problems = resolveReport.allProblemMessages.joinToString("\n")
            throw IllegalStateException("Failed to resolve dependencies:\n$problems")
        }

        // Collect resolved files
        val files = mutableListOf<File>()
        resolveReport.allArtifactsReports.forEach { artifactReport ->
            if (artifactReport.localFile != null) {
                files.add(artifactReport.localFile)
            }
        }

        return files
    }

    private fun parseMavenCoordinate(coord: String): ModuleRevisionId {
        val parts = coord.split(":")
        return when (parts.size) {
            3 -> ModuleRevisionId.newInstance(parts[0], parts[1], parts[2])
            4 -> {
                // groupId:artifactId:packaging:version or groupId:artifactId:version:classifier
                // Ivy doesn't have separate packaging, so treat as groupId:artifactId:version
                ModuleRevisionId.newInstance(parts[0], parts[1], parts[3])
            }

            else -> throw IllegalArgumentException("Invalid Maven coordinate: $coord (expected groupId:artifactId:version)")
        }
    }
}
