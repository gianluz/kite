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
 * ### Local plugin development:
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
 * ### Testing plugins before publishing:
 * ```kotlin
 * // Build plugin
 * // ./gradlew :my-plugin:build
 *
 * @file:DependsOnJar("./my-plugin/build/libs/my-plugin-1.0.0.jar")
 *
 * import com.company.myplugin.*
 * ```
 *
 * @param path Path to the JAR file (must end with .jar)
 * @see DependsOnMavenLocal for Maven Local dependencies
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOnJar(val path: String)

/**
 * Declares a dependency from Maven Local repository (~/.m2/repository).
 *
 * Format: `groupId:artifactId:version`
 *
 * ## Use Cases:
 *
 * ### 1. Testing plugins before publishing to Maven Central:
 * ```kotlin
 * // In plugin project:
 * // ./gradlew publishToMavenLocal
 *
 * // In Kite script:
 * @file:DependsOnMavenLocal("io.kite.plugins:gradle:1.0.0")
 *
 * import io.kite.plugins.gradle.*
 * ```
 *
 * ### 2. Private company artifacts:
 * ```kotlin
 * @file:DependsOnMavenLocal("com.company:internal-tools:2.0.0-SNAPSHOT")
 *
 * import com.company.tools.*
 * ```
 *
 * ### 3. Development workflow:
 * ```kotlin
 * // Step 1: Publish to Maven Local
 * // ./gradlew :kite-plugin-git:publishToMavenLocal
 *
 * // Step 2: Use in scripts immediately
 * @file:DependsOnMavenLocal("io.kite.plugins:git:1.0.0-SNAPSHOT")
 *
 * import io.kite.plugins.git.*
 *
 * segments {
 *     segment("tag-release") {
 *         execute {
 *             git {
 *                 tag("v1.0.0")
 *                 push(tags = true)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Publishing to Maven Local:
 *
 * ```gradle
 * // build.gradle.kts
 * plugins {
 *     `maven-publish`
 * }
 *
 * publishing {
 *     publications {
 *         create<MavenPublication>("mavenJava") {
 *             from(components["java"])
 *         }
 *     }
 * }
 * ```
 *
 * Then run: `./gradlew publishToMavenLocal`
 *
 * @param coordinates Maven coordinates in format groupId:artifactId:version
 * @see DependsOnJar for local JAR files
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOnMavenLocal(val coordinates: String)
