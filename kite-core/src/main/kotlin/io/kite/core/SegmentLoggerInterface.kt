package io.kite.core

/**
 * Interface for segment logging.
 * The actual implementation is provided by kite-runtime.
 */
interface SegmentLoggerInterface {
    fun info(message: String)
    fun debug(message: String)
    fun warn(message: String)
    fun error(message: String)
    fun logCommandStart(command: String)
    fun logCommandOutput(output: String, isError: Boolean = false)
    fun logCommandComplete(command: String, exitCode: Int, durationMs: Long)
}

/**
 * No-op logger for when logging is not available.
 */
object NoOpLogger : SegmentLoggerInterface {
    override fun info(message: String) {}
    override fun debug(message: String) {}
    override fun warn(message: String) {}
    override fun error(message: String) {}
    override fun logCommandStart(command: String) {}
    override fun logCommandOutput(output: String, isError: Boolean) {}
    override fun logCommandComplete(command: String, exitCode: Int, durationMs: Long) {}
}
