package com.gerefloc45.voidapi.api.debug;

import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AI Logger - advanced logging system for AI behavior debugging.
 * Supports filtering, log levels, and file output.
 * 
 * @author VoidAPI Framework
 * @version 0.2.0
 */
public class AILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(AILogger.class);
    private static AILogger instance;

    private final Queue<LogEntry> logBuffer;
    private final Map<UUID, LogLevel> entityLogLevels;
    private final Set<String> enabledCategories;
    private LogLevel globalLogLevel;
    private boolean fileLoggingEnabled;
    private Path logFilePath;
    private BufferedWriter logWriter;
    private static final int MAX_BUFFER_SIZE = 1000;

    /**
     * Log levels for filtering.
     */
    public enum LogLevel {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4);

        private final int priority;

        LogLevel(int priority) {
            this.priority = priority;
        }

        public boolean shouldLog(LogLevel threshold) {
            return this.priority >= threshold.priority;
        }
    }

    /**
     * Log entry.
     */
    public static class LogEntry {
        public final long timestamp;
        public final LogLevel level;
        public final String category;
        public final UUID entityId;
        public final String message;
        public final Throwable throwable;

        public LogEntry(LogLevel level, String category, UUID entityId, String message, Throwable throwable) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.category = category;
            this.entityId = entityId;
            this.message = message;
            this.throwable = throwable;
        }

        public String format() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
            StringBuilder sb = new StringBuilder();
            
            sb.append("[").append(dateFormat.format(new Date(timestamp))).append("] ");
            sb.append("[").append(level).append("] ");
            sb.append("[").append(category).append("] ");
            
            if (entityId != null) {
                sb.append("[Entity:").append(entityId.toString().substring(0, 8)).append("...] ");
            }
            
            sb.append(message);

            if (throwable != null) {
                sb.append("\n").append(throwable.toString());
            }

            return sb.toString();
        }
    }

    private AILogger() {
        this.logBuffer = new ConcurrentLinkedQueue<>();
        this.entityLogLevels = new HashMap<>();
        this.enabledCategories = new HashSet<>();
        this.globalLogLevel = LogLevel.INFO;
        this.fileLoggingEnabled = false;

        // Enable all categories by default
        enabledCategories.add("*");
    }

    /**
     * Gets the singleton instance.
     *
     * @return Logger instance
     */
    public static AILogger getInstance() {
        if (instance == null) {
            instance = new AILogger();
        }
        return instance;
    }

    /**
     * Sets the global log level.
     *
     * @param level Log level
     */
    public void setLogLevel(LogLevel level) {
        this.globalLogLevel = level;
        LOGGER.info("AI Logger level set to: {}", level);
    }

    /**
     * Sets log level for a specific entity.
     *
     * @param entity Entity
     * @param level Log level
     */
    public void setEntityLogLevel(Entity entity, LogLevel level) {
        entityLogLevels.put(entity.getUuid(), level);
    }

    /**
     * Enables a log category.
     *
     * @param category Category name
     */
    public void enableCategory(String category) {
        enabledCategories.add(category);
        LOGGER.info("Enabled AI log category: {}", category);
    }

    /**
     * Disables a log category.
     *
     * @param category Category name
     */
    public void disableCategory(String category) {
        enabledCategories.remove(category);
        LOGGER.info("Disabled AI log category: {}", category);
    }

    /**
     * Enables file logging.
     *
     * @param logDirectory Directory for log files
     * @throws IOException If file creation fails
     */
    public void enableFileLogging(String logDirectory) throws IOException {
        Path dir = Paths.get(logDirectory);
        Files.createDirectories(dir);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String fileName = "voidapi_ai_" + dateFormat.format(new Date()) + ".log";
        logFilePath = dir.resolve(fileName);

        logWriter = new BufferedWriter(new FileWriter(logFilePath.toFile(), true));
        fileLoggingEnabled = true;

        LOGGER.info("AI file logging enabled: {}", logFilePath);
    }

    /**
     * Disables file logging.
     */
    public void disableFileLogging() {
        fileLoggingEnabled = false;
        if (logWriter != null) {
            try {
                logWriter.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close log file", e);
            }
            logWriter = null;
        }
    }

    /**
     * Logs a trace message.
     *
     * @param category Category
     * @param entity Entity (optional)
     * @param message Message
     */
    public void trace(String category, Entity entity, String message) {
        log(LogLevel.TRACE, category, entity, message, null);
    }

    /**
     * Logs a debug message.
     *
     * @param category Category
     * @param entity Entity (optional)
     * @param message Message
     */
    public void debug(String category, Entity entity, String message) {
        log(LogLevel.DEBUG, category, entity, message, null);
    }

    /**
     * Logs an info message.
     *
     * @param category Category
     * @param entity Entity (optional)
     * @param message Message
     */
    public void info(String category, Entity entity, String message) {
        log(LogLevel.INFO, category, entity, message, null);
    }

    /**
     * Logs a warning message.
     *
     * @param category Category
     * @param entity Entity (optional)
     * @param message Message
     */
    public void warn(String category, Entity entity, String message) {
        log(LogLevel.WARN, category, entity, message, null);
    }

    /**
     * Logs an error message.
     *
     * @param category Category
     * @param entity Entity (optional)
     * @param message Message
     * @param throwable Exception (optional)
     */
    public void error(String category, Entity entity, String message, Throwable throwable) {
        log(LogLevel.ERROR, category, entity, message, throwable);
    }

    /**
     * Main logging method.
     *
     * @param level Log level
     * @param category Category
     * @param entity Entity (optional)
     * @param message Message
     * @param throwable Exception (optional)
     */
    private void log(LogLevel level, String category, Entity entity, String message, Throwable throwable) {
        // Check if category is enabled
        if (!enabledCategories.contains("*") && !enabledCategories.contains(category)) {
            return;
        }

        // Check log level
        UUID entityId = entity != null ? entity.getUuid() : null;
        LogLevel threshold = entityId != null ? entityLogLevels.getOrDefault(entityId, globalLogLevel) : globalLogLevel;

        if (!level.shouldLog(threshold)) {
            return;
        }

        // Create log entry
        LogEntry entry = new LogEntry(level, category, entityId, message, throwable);

        // Add to buffer
        logBuffer.offer(entry);
        if (logBuffer.size() > MAX_BUFFER_SIZE) {
            logBuffer.poll();
        }

        // Output to console
        String formatted = entry.format();
        switch (level) {
            case TRACE, DEBUG -> LOGGER.debug(formatted);
            case INFO -> LOGGER.info(formatted);
            case WARN -> LOGGER.warn(formatted);
            case ERROR -> LOGGER.error(formatted);
        }

        // Output to file
        if (fileLoggingEnabled && logWriter != null) {
            try {
                logWriter.write(formatted);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                LOGGER.error("Failed to write to log file", e);
            }
        }
    }

    /**
     * Gets recent log entries.
     *
     * @param count Number of entries
     * @return List of log entries
     */
    public List<LogEntry> getRecentLogs(int count) {
        List<LogEntry> logs = new ArrayList<>(logBuffer);
        int start = Math.max(0, logs.size() - count);
        return logs.subList(start, logs.size());
    }

    /**
     * Gets logs for a specific entity.
     *
     * @param entity Entity
     * @return List of log entries
     */
    public List<LogEntry> getEntityLogs(Entity entity) {
        UUID entityId = entity.getUuid();
        return logBuffer.stream()
            .filter(entry -> entityId.equals(entry.entityId))
            .toList();
    }

    /**
     * Gets logs for a specific category.
     *
     * @param category Category
     * @return List of log entries
     */
    public List<LogEntry> getCategoryLogs(String category) {
        return logBuffer.stream()
            .filter(entry -> category.equals(entry.category))
            .toList();
    }

    /**
     * Clears the log buffer.
     */
    public void clear() {
        logBuffer.clear();
        LOGGER.info("Cleared AI log buffer");
    }

    /**
     * Gets the current log file path.
     *
     * @return Log file path or null
     */
    public Path getLogFilePath() {
        return logFilePath;
    }

    /**
     * Checks if file logging is enabled.
     *
     * @return True if enabled
     */
    public boolean isFileLoggingEnabled() {
        return fileLoggingEnabled;
    }
}
