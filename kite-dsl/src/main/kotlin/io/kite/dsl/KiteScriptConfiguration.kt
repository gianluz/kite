package io.kite.dsl

import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

/**
 * Compilation configuration for Kite scripts.
 *
 * Defines how .kite.kts files should be compiled, including:
 * - Dependencies available to scripts
 * - Implicit imports
 * - IDE support configuration
 */
object KiteScriptCompilationConfiguration : ScriptCompilationConfiguration({
    // Implicit imports available in all Kite scripts
    defaultImports(
        "io.kite.core.*",
        "io.kite.dsl.*",
        "kotlin.time.Duration",
        "kotlin.time.Duration.Companion.seconds",
        "kotlin.time.Duration.Companion.minutes",
        "kotlin.time.Duration.Companion.hours"
    )

    // Make current context dependencies available to scripts
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    // IDE support
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})

/**
 * Evaluation configuration for Kite scripts.
 *
 * Defines how .kite.kts files should be executed.
 */
object KiteScriptEvaluationConfiguration : ScriptEvaluationConfiguration({
    // Scripts can be evaluated in any context
    jvm {
        // No special JVM options needed for now
    }
})
