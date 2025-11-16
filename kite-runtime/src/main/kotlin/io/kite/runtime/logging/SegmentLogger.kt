package io.kite.runtime.logging

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Logger for segment execution.
 *
 * Provides segment-specific logging that:
 * - Writes to a segment-specific log file
 * - Prefixes output with segment name for parallel execution visibility
 * - Stores logs in .kite/logs/ directory
 */
class SegmentLogger(
    private val segmentName: String,
    private val logDir: File = File(".kite/logs")
) {

    private val logFile: File = logDir.resolve("$segmentName.log")
    private val buffer = StringBuilder()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    init {
        // Create log directory if it doesn't exist
        logDir.mkdirs()
        // Clear log file
        logFile.writeText("")
    }

    /**
     * Logs a message to the segment log file.
     */
    fun log(message: String, level: LogLevel = LogLevel.INFO, showInConsole: Boolean = true) {
        val timestamp = LocalDateTime.now().format(timeFormat)
        val logEntry = "[$timestamp] [${level.name}] $message"

        // Write to buffer
        buffer.appendLine(logEntry)

        // Write to log file
        logFile.appendText(logEntry + "\n")

        // Optionally show in console with segment prefix
        if (showInConsole) {
            println("[$segmentName] $message")
        }
    }

    /**
     * Logs an info message.
     */
    fun info(message: String, showInConsole: Boolean = true) {
        log(message, LogLevel.INFO, showInConsole)
    }

    /**
     * Logs a debug message.
     */
    fun debug(message: String, showInConsole: Boolean = false) {
        log(message, LogLevel.DEBUG, showInConsole)
    }

    /**
     * Logs a warning message.
     */
    fun warn(message: String, showInConsole: Boolean = true) {
        log(message, LogLevel.WARN, showInConsole)
    }

    /**
     * Logs an error message.
     */
    fun error(message: String, showInConsole: Boolean = true) {
        log(message, LogLevel.ERROR, showInConsole)
    }

    /**
     * Logs command execution.
     */
    fun logCommand(command: String, output: String? = null) {
        debug("Executing: $command")
        if (output != null && output.isNotBlank()) {
            buffer.appendLine(output)
            logFile.appendText(output + "\n")
        }
    }

    /**
     * Gets the captured output as a string.
     */
    fun getOutput(): String = buffer.toString()

    /**
     * Gets the log file path.
     */
    fun getLogFilePath(): String = logFile.absolutePath
}

/**
 * Log levels.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Configuration for segment logging.
 */
data class LogConfig(
    val logDir: File = File(".kite/logs"),
    val keepLogs: Boolean = false
)

/**
 * Manages logging for multiple segments.
 */
object LogManager {
    private val activeLoggers = mutableMapOf<String, SegmentLogger>()
    private var config = LogConfig()

    /**
     * Configures the log manager.
     */
    fun configure(newConfig: LogConfig) {
        config = newConfig
    }

    /**
     * Creates a logger for a segment.
     */
    fun startSegmentLogging(segmentName: String): SegmentLogger {
        val logger = SegmentLogger(
            segmentName = segmentName,
            logDir = config.logDir
        )
        activeLoggers[segmentName] = logger
        return logger
    }

    /**
     * Stops logging for a segment.
     */
    fun stopSegmentLogging(segmentName: String) {
        activeLoggers.remove(segmentName)
    }

    /**
     * Cleans up all logs.
     */
    fun cleanup() {
        if (!config.keepLogs && config.logDir.exists()) {
            config.logDir.deleteRecursively()
        }
    }

    /**
     * Gets all log files.
     */
    fun getLogFiles(): List<File> {
        return if (config.logDir.exists()) {
            config.logDir.listFiles()?.filter { it.isFile && it.extension == "log" }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
}
