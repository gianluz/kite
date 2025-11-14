package io.kite.dsl

import kotlin.script.experimental.annotations.KotlinScript

/**
 * Base class for all Kite scripts (.kite.kts files).
 *
 * This class is extended by all Kite scripts and provides access to
 * the DSL builders for defining segments and rides.
 */
@KotlinScript(
    fileExtension = "kite.kts",
    compilationConfiguration = KiteScriptCompilationConfiguration::class,
    evaluationConfiguration = KiteScriptEvaluationConfiguration::class
)
abstract class KiteScript
