package io.kite.dsl

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * Compiles and evaluates Kite scripts (.kite.kts files).
 *
 * This class handles:
 * - Compilation of .kite.kts files
 * - Caching of compiled scripts
 * - Error reporting
 */
class ScriptCompiler(
    private val enableCache: Boolean = true,
) {
    private val scriptingHost = BasicJvmScriptingHost()
    private val compilationCache = mutableMapOf<String, CompiledScript>()

    /**
     * Compiles a script file.
     *
     * @param file The .kite.kts file to compile
     * @return Result containing the compiled script or errors
     */
    suspend fun compile(file: File): ResultWithDiagnostics<CompiledScript> {
        require(file.exists()) { "Script file not found: ${file.absolutePath}" }
        require(file.extension == "kts") { "Script file must have .kts extension: ${file.name}" }

        // Check cache first
        val cacheKey = file.absolutePath
        if (enableCache && compilationCache.containsKey(cacheKey)) {
            return ResultWithDiagnostics.Success(compilationCache[cacheKey]!!)
        }

        // Compile the script using our custom configuration
        val result = scriptingHost.compiler(file.toScriptSource(), KiteScriptCompilationConfiguration)

        // Cache successful compilation
        if (result is ResultWithDiagnostics.Success && enableCache) {
            compilationCache[cacheKey] = result.value
        }

        return result
    }

    /**
     * Evaluates a compiled script.
     *
     * @param compiledScript The compiled script to evaluate
     * @return Result containing the return value or errors
     */
    suspend fun evaluate(compiledScript: CompiledScript): ResultWithDiagnostics<EvaluationResult> {
        return scriptingHost.evaluator(compiledScript, KiteScriptEvaluationConfiguration)
    }

    /**
     * Compiles and evaluates a script file in one call.
     *
     * @param file The .kite.kts file to compile and evaluate
     * @return Result containing the return value or errors
     */
    suspend fun compileAndEvaluate(file: File): ResultWithDiagnostics<EvaluationResult> {
        val compilationResult = compile(file)
        if (compilationResult !is ResultWithDiagnostics.Success) {
            @Suppress("UNCHECKED_CAST")
            return compilationResult as ResultWithDiagnostics<EvaluationResult>
        }

        return evaluate(compilationResult.value)
    }

    /**
     * Clears the compilation cache.
     */
    fun clearCache() {
        compilationCache.clear()
    }

    /**
     * Returns the number of cached scripts.
     */
    fun cacheSize(): Int = compilationCache.size
}

/**
 * Result of script compilation or evaluation.
 */
sealed class ScriptResult<out T> {
    data class Success<T>(val value: T) : ScriptResult<T>()

    data class Failure(val errors: List<ScriptDiagnostic>) : ScriptResult<Nothing>()
}

/**
 * Diagnostic message from script compilation or evaluation.
 */
data class ScriptDiagnostic(
    val message: String,
    val severity: Severity,
    val location: SourceCode.Location?,
) {
    enum class Severity {
        ERROR,
        WARNING,
        INFO,
    }

    override fun toString(): String {
        val locationStr = location?.let { " at ${it.start.line}:${it.start.col}" } ?: ""
        return "[$severity]$locationStr $message"
    }
}

/**
 * Extension function to convert ResultWithDiagnostics to ScriptResult.
 */
fun <T> ResultWithDiagnostics<T>.toScriptResult(): ScriptResult<T> {
    return when (this) {
        is ResultWithDiagnostics.Success -> ScriptResult.Success(this.value)
        is ResultWithDiagnostics.Failure -> {
            val diagnostics =
                this.reports.map { report ->
                    ScriptDiagnostic(
                        message = report.message,
                        severity =
                            when (report.severity) {
                                kotlin.script.experimental.api.ScriptDiagnostic.Severity.ERROR ->
                                    ScriptDiagnostic.Severity.ERROR

                                kotlin.script.experimental.api.ScriptDiagnostic.Severity.WARNING ->
                                    ScriptDiagnostic.Severity.WARNING
                                else -> ScriptDiagnostic.Severity.INFO
                            },
                        location = report.location,
                    )
                }
            ScriptResult.Failure(diagnostics)
        }
    }
}
