package io.kite.runtime.logging

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Logger for segment execution.
 *
 * Provides segment-specific logging that:
 * - Writes to a segment-specific log file
 * - Prefixes output with segment name for parallel execution visibility
 * - Stores logs in .kite/logs/ directory
 * - Captures stdout/stderr during segment execution
 */
class SegmentLogger(
    private val segmentName: String,
    private val logDir: File = File(".kite/logs")
) {

    private val logFile: File = logDir.resolve("$segmentName.log")
    private val buffer = StringBuilder()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val originalOut = System.out
    private val originalErr = System.err
    private var capturing = false

    init {
        // Create log directory if it doesn't exist
        logDir.mkdirs()
        // Clear log file
        logFile.writeText("")
    }

    /**
     * Starts capturing stdout and stderr for this segment.
     */
    fun startCapture() {
        if (capturing) return
        capturing = true

        // Create prefixing streams
        val prefixedOut = PrefixingPrintStream(originalOut, originalErr, segmentName, logFile, buffer)
        val prefixedErr = PrefixingPrintStream(originalErr, originalErr, segmentName, logFile, buffer, isError = true)

        // Replace system streams
        System.setOut(prefixedOut)
        System.setErr(prefixedErr)
    }

    /**
     * Stops capturing and restores original streams.
     */
    fun stopCapture() {
        if (!capturing) return
        capturing = false

        // Restore original streams
        System.setOut(originalOut)
        System.setErr(originalErr)
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
            originalOut.println("[$segmentName] $message")
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
 * PrintStream that prefixes each line with segment name.
 */
private class PrefixingPrintStream(
    private val originalOut: PrintStream,
    private val originalErr: PrintStream,
    private val segmentName: String,
    private val logFile: File,
    private val buffer: StringBuilder,
    private val isError: Boolean = false
) : PrintStream(ByteArrayOutputStream()) {

    private val lineBuffer = StringBuilder()

    override fun write(b: Int) {
        val char = b.toChar()

        if (char == '\n') {
            flushLine()
        } else {
            lineBuffer.append(char)
        }
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        val text = String(buf, off, len)

        for (char in text) {
            if (char == '\n') {
                flushLine()
            } else {
                lineBuffer.append(char)
            }
        }
    }

    private fun flushLine() {
        if (lineBuffer.isEmpty()) {
            // Just output newline
            originalOut.println()
            logFile.appendText("\n")
            return
        }

        val line = lineBuffer.toString()
        lineBuffer.clear()

        // Write to buffer (for getOutput())
        buffer.appendLine(line)

        // Write to log file
        logFile.appendText(line + "\n")

        // Write to console with prefix
        originalOut.println("[$segmentName] $line")
    }

    override fun flush() {
        if (lineBuffer.isNotEmpty()) {
            flushLine()
        }
        super.flush()
    }
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
     * Gets a logger for a segment.
     */
    fun getLogger(segmentName: String): SegmentLogger? {
        return activeLoggers[segmentName]
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
