package io.kite.runtime.logging

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * ANSI color codes for terminal output.
 */
object AnsiColors {
    const val RESET = "\u001B[0m"
    const val BLUE = "\u001B[34m"
    const val RED = "\u001B[31m"
    const val YELLOW = "\u001B[33m"
    const val WHITE = "\u001B[37m"
    const val BRIGHT_RED = "\u001B[91m"
}

/**
 * Logger for segment execution.
 *
 * Provides segment-specific logging that:
 * - Writes to a segment-specific log file with timestamps
 * - Can optionally show output in console with colors
 * - Only captures output from THIS segment (no cross-contamination)
 * - Logs command output, stdout, stderr separately
 */
class SegmentLogger(
    private val segmentName: String,
    private val logDir: File = File(".kite/logs"),
    private val showInConsole: Boolean = false
) : io.kite.core.SegmentLoggerInterface {

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
    private fun logToFile(message: String, level: String) {
        val timestamp = LocalDateTime.now().format(timeFormat)
        val logEntry = "[$timestamp] [$segmentName] $message"

        // Write to buffer
        buffer.appendLine(logEntry)

        // Write to log file
        logFile.appendText(logEntry + "\n")

        // Optionally show in console with colors
        if (showInConsole) {
            val timestampColored = "${AnsiColors.BLUE}[$timestamp]${AnsiColors.RESET}"
            val segmentColored = "${AnsiColors.RED}[$segmentName]${AnsiColors.RESET}"
            val coloredMessage = when (level) {
                "ERROR" -> "${AnsiColors.BRIGHT_RED}$message${AnsiColors.RESET}"
                "WARN" -> "${AnsiColors.YELLOW}$message${AnsiColors.RESET}"
                else -> "${AnsiColors.WHITE}$message${AnsiColors.RESET}"
            }
            println("$timestampColored $segmentColored $coloredMessage")
        }
    }

    /**
     * Logs an info message.
     */
    override fun info(message: String) {
        logToFile(message, "INFO")
    }

    /**
     * Logs a debug message.
     */
    override fun debug(message: String) {
        logToFile(message, "DEBUG")
    }

    /**
     * Logs a warning message.
     */
    override fun warn(message: String) {
        logToFile(message, "WARN")
    }

    /**
     * Logs an error message.
     */
    override fun error(message: String) {
        logToFile(message, "ERROR")
    }

    /**
     * Logs command execution start.
     */
    override fun logCommandStart(command: String) {
        val timestamp = LocalDateTime.now().format(timeFormat)
        val logEntry = "[$timestamp] [$segmentName] $ $command"

        buffer.appendLine(logEntry)
        logFile.appendText(logEntry + "\n")

        // Show in console if enabled
        if (showInConsole) {
            val timestampColored = "${AnsiColors.BLUE}[$timestamp]${AnsiColors.RESET}"
            val segmentColored = "${AnsiColors.RED}[$segmentName]${AnsiColors.RESET}"
            println("$timestampColored $segmentColored ${AnsiColors.WHITE}$ $command${AnsiColors.RESET}")
        }
    }

    /**
     * Logs command output line by line with timestamps.
     */
    override fun logCommandOutput(output: String, isError: Boolean) {
        if (output.isBlank()) return

        output.lines().forEach { line ->
            if (line.isNotEmpty()) {
                val timestamp = LocalDateTime.now().format(timeFormat)
                val logEntry = "[$timestamp] [$segmentName] $line"

                buffer.appendLine(logEntry)
                logFile.appendText(logEntry + "\n")

                // Show in console if enabled
                if (showInConsole) {
                    val timestampColored = "${AnsiColors.BLUE}[$timestamp]${AnsiColors.RESET}"
                    val segmentColored = "${AnsiColors.RED}[$segmentName]${AnsiColors.RESET}"
                    val color = if (isError) AnsiColors.BRIGHT_RED else AnsiColors.WHITE
                    println("$timestampColored $segmentColored $color$line${AnsiColors.RESET}")
                }
            }
        }
    }

    /**
     * Logs command completion.
     */
    override fun logCommandComplete(command: String, exitCode: Int, durationMs: Long) {
        val timestamp = LocalDateTime.now().format(timeFormat)
        val status = if (exitCode == 0) "✓" else "✗"
        val message = "Command $status (exit: $exitCode, ${durationMs}ms)"
        val logEntry = "[$timestamp] [$segmentName] $message"

        buffer.appendLine(logEntry)
        logFile.appendText(logEntry + "\n")

        // Show in console if enabled
        if (showInConsole) {
            val timestampColored = "${AnsiColors.BLUE}[$timestamp]${AnsiColors.RESET}"
            val segmentColored = "${AnsiColors.RED}[$segmentName]${AnsiColors.RESET}"
            val color = if (exitCode == 0) AnsiColors.WHITE else AnsiColors.BRIGHT_RED
            println("$timestampColored $segmentColored $color$message${AnsiColors.RESET}")
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

    // Thread-local current logger for command execution
    private val currentLogger = ThreadLocal<SegmentLogger>()

    /**
     * Configures the log manager.
     */
    fun configure(newConfig: LogConfig) {
        config = newConfig
    }

    /**
     * Creates a logger for a segment and sets it as current for this thread.
     */
    fun startSegmentLogging(segmentName: String, showInConsole: Boolean = false): SegmentLogger {
        val logger = SegmentLogger(
            segmentName = segmentName,
            logDir = config.logDir,
            showInConsole = showInConsole
        )
        activeLoggers[segmentName] = logger
        currentLogger.set(logger)
        return logger
    }

    /**
     * Stops logging for a segment and clears thread-local.
     */
    fun stopSegmentLogging(segmentName: String) {
        activeLoggers.remove(segmentName)
        currentLogger.remove()
    }

    /**
     * Gets a logger for a segment.
     */
    fun getLogger(segmentName: String): SegmentLogger? {
        return activeLoggers[segmentName]
    }

    /**
     * Gets the current logger for the current thread.
     */
    fun getCurrentLogger(): SegmentLogger? {
        return currentLogger.get()
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
