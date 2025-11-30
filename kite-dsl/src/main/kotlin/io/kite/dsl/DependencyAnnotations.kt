package io.kite.dsl

/**
 * Declares a dependency on a local JAR file.
 *
 * The path can be:
 * - **Relative to script location**: `./plugins/my-plugin.jar`
 * - **Relative to workspace root**: `../libs/plugin.jar`
 * - **Absolute path**: `/opt/plugins/plugin.jar`
 * - **Home directory**: `~/plugins/plugin.jar`
 *
 * ## Examples:
 *
 * ### Quick plugin testing (no Maven Local needed):
 * ```kotlin
 * @file:DependsOnJar("../kite-plugin-gradle/build/libs/gradle-1.0.0.jar")
 *
 * import io.kite.plugins.gradle.*
 *
 * segments {
 *     segment("build") {
 *         execute {
 *             gradle { build() }
 *         }
 *     }
 * }
 * ```
 *
 * ### Company shared plugins:
 * ```kotlin
 * @file:DependsOnJar("./.kite/lib/company-plugin.jar")
 * ```
 *
 * ## Important Notes:
 *
 * **Transitive Dependencies:**
 * This annotation does NOT resolve transitive dependencies. The JAR must be
 * self-contained or you must declare dependencies separately:
 *
 * ```kotlin
 * @file:DependsOnJar("./my-plugin.jar")
 * @file:DependsOn("org.eclipse.jgit:org.eclipse.jgit:6.7.0")  // Manual transitive dep
 * ```
 *
 * **Better Alternative for Plugin Development:**
 * For a better development experience with automatic transitive resolution,
 * publish to Maven Local and use regular `@file:DependsOn`:
 *
 * ```bash
 * # Publish plugin to Maven Local
 * ./gradlew :my-plugin:publishToMavenLocal
 * ```
 *
 * ```kotlin
 * // Regular @DependsOn checks Maven Local automatically!
 * // Includes all transitive dependencies ✅
 * @file:DependsOn("com.company:my-plugin:1.0.0")
 *
 * import com.company.myplugin.*
 * ```
 *
 * @param path Path to the JAR file (must end with .jar)
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOnJar(val path: String)
