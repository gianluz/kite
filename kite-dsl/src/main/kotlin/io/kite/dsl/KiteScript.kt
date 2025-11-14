package io.kite.dsl

import kotlin.script.experimental.annotations.KotlinScript

/**
 * Base class for all Kite scripts (.kite.kts files).
 *
 * This provides IntelliJ IDEA with the necessary information to provide
 * autocomplete, syntax highlighting, and type checking for .kite.kts files.
 *
 * Configuration is defined in KiteScriptConfiguration.kt
 */
@KotlinScript(
    displayName = "Kite Script",
    fileExtension = "kite.kts",
    compilationConfiguration = KiteScriptCompilationConfiguration::class,
    evaluationConfiguration = KiteScriptEvaluationConfiguration::class
)
abstract class KiteScript
