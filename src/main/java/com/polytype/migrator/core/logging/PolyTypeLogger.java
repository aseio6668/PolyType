package com.polytype.migrator.core.logging;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive logging and monitoring system for PolyType.
 * Provides structured logging, metrics collection, performance tracking, and debugging capabilities.
 */
public class PolyTypeLogger {
    
    public enum LogLevel {
        TRACE(0, "TRACE"),
        DEBUG(1, "DEBUG"), 
        INFO(2, "INFO"),
        WARN(3, "WARN"),
        ERROR(4, "ERROR"),
        FATAL(5, "FATAL");
        
        private final int priority;
        private final String name;
        
        LogLevel(int priority, String name) {
            this.priority = priority;
            this.name = name;
        }
        
        public int getPriority() { return priority; }
        public String getName() { return name; }
    }
    
    public enum LogCategory {
        TRANSLATION("TRANS"),
        PARSING("PARSE"),
        ML_ENGINE("ML"),
        BINARY_ANALYSIS("BIN"),
        PERFORMANCE("PERF"),
        SECURITY("SEC"),
        CONFIG("CFG"),
        PLUGIN("PLG"),
        CACHE("CACHE"),
        IO("IO"),
        GENERAL("GEN");
        
        private final String prefix;
        
        LogCategory(String prefix) {
            this.prefix = prefix;
        }
        
        public String getPrefix() { return prefix; }
    }
    
    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final LogLevel level;
        private final LogCategory category;
        private final String className;
        private final String methodName;
        private final String message;
        private final Map<String, Object> context;
        private final Throwable exception;
        
        public LogEntry(LogLevel level, LogCategory category, String className, 
                       String methodName, String message, Map<String, Object> context, 
                       Throwable exception) {
            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.category = category;
            this.className = className;
            this.methodName = methodName;
            this.message = message;
            this.context = new HashMap<>(context != null ? context : Collections.emptyMap());
            this.exception = exception;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public LogLevel getLevel() { return level; }
        public LogCategory getCategory() { return category; }
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String getMessage() { return message; }
        public Map<String, Object> getContext() { return context; }
        public Throwable getException() { return exception; }
    }
    
    public static class PerformanceMetrics {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Map<String, List<Long>> timings = new ConcurrentHashMap<>();
        private final Map<String, Object> gauges = new ConcurrentHashMap<>();
        
        public void incrementCounter(String name) {
            counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        public void recordTiming(String name, long duration) {
            timings.computeIfAbsent(name, k -> new ArrayList<>()).add(duration);
        }
        
        public void setGauge(String name, Object value) {
            gauges.put(name, value);
        }
        
        public long getCounter(String name) {
            return counters.getOrDefault(name, new AtomicLong(0)).get();
        }
        
        public List<Long> getTimings(String name) {
            return new ArrayList<>(timings.getOrDefault(name, Collections.emptyList()));
        }
        
        public Object getGauge(String name) {
            return gauges.get(name);
        }
        
        public Set<String> getCounterNames() {
            return new HashSet<>(counters.keySet());
        }
        
        public Set<String> getTimingNames() {
            return new HashSet<>(timings.keySet());
        }
        
        public Set<String> getGaugeNames() {
            return new HashSet<>(gauges.keySet());
        }
    }
    
    public static class TimingScope implements AutoCloseable {
        private final PolyTypeLogger logger;
        private final String operationName;
        private final long startTime;
        private final Map<String, Object> context;
        
        public TimingScope(PolyTypeLogger logger, String operationName, Map<String, Object> context) {
            this.logger = logger;
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
            this.context = context;
            logger.debug(LogCategory.PERFORMANCE, "Started: " + operationName, context);
        }
        
        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            logger.metrics.recordTiming(operationName, duration);
            
            Map<String, Object> endContext = new HashMap<>(context);
            endContext.put("duration_ms", duration);
            logger.debug(LogCategory.PERFORMANCE, "Completed: " + operationName, endContext);
        }
    }
    
    private static final Map<String, PolyTypeLogger> loggers = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final String name;
    private final List<LogEntry> logHistory = Collections.synchronizedList(new ArrayList<>());
    private final PerformanceMetrics metrics = new PerformanceMetrics();
    
    // Configuration
    private LogLevel minimumLevel = LogLevel.INFO;
    private boolean enableConsoleOutput = true;
    private boolean enableFileOutput = false;
    private String logFilePath = "polytype.log";
    private int maxHistorySize = 10000;
    private boolean enableMetrics = true;
    
    private PrintWriter fileWriter;
    
    private PolyTypeLogger(String name) {
        this.name = name;
        setupFileOutput();
    }
    
    public static PolyTypeLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }
    
    public static PolyTypeLogger getLogger(String name) {
        return loggers.computeIfAbsent(name, PolyTypeLogger::new);
    }
    
    // Configuration methods
    public PolyTypeLogger setMinimumLevel(LogLevel level) {
        this.minimumLevel = level;
        return this;
    }
    
    public PolyTypeLogger setConsoleOutput(boolean enabled) {
        this.enableConsoleOutput = enabled;
        return this;
    }
    
    public PolyTypeLogger setFileOutput(boolean enabled, String filePath) {
        this.enableFileOutput = enabled;
        this.logFilePath = filePath != null ? filePath : "polytype.log";
        if (enabled) {
            setupFileOutput();
        } else {
            closeFileOutput();
        }
        return this;
    }
    
    public PolyTypeLogger setMaxHistorySize(int maxSize) {
        this.maxHistorySize = maxSize;
        return this;
    }
    
    public PolyTypeLogger setMetricsEnabled(boolean enabled) {
        this.enableMetrics = enabled;
        return this;
    }
    
    private void setupFileOutput() {
        if (enableFileOutput && fileWriter == null) {
            try {
                fileWriter = new PrintWriter(new FileWriter(logFilePath, true));
            } catch (IOException e) {
                System.err.println("Failed to setup file output for logger: " + e.getMessage());
            }
        }
    }
    
    private void closeFileOutput() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
    }
    
    // Core logging methods
    public void log(LogLevel level, LogCategory category, String message) {
        log(level, category, message, null, null);
    }
    
    public void log(LogLevel level, LogCategory category, String message, Map<String, Object> context) {
        log(level, category, message, context, null);
    }
    
    public void log(LogLevel level, LogCategory category, String message, Throwable exception) {
        log(level, category, message, null, exception);
    }
    
    public void log(LogLevel level, LogCategory category, String message, 
                   Map<String, Object> context, Throwable exception) {
        
        if (level.getPriority() < minimumLevel.getPriority()) {
            return;
        }
        
        // Get caller information
        StackTraceElement caller = getCallerInfo();
        String className = caller != null ? caller.getClassName() : "Unknown";
        String methodName = caller != null ? caller.getMethodName() : "unknown";
        
        LogEntry entry = new LogEntry(level, category, className, methodName, message, context, exception);
        
        // Add to history
        addToHistory(entry);
        
        // Output to console
        if (enableConsoleOutput) {
            System.out.println(formatLogEntry(entry));
            if (exception != null) {
                exception.printStackTrace(System.out);
            }
        }
        
        // Output to file
        if (enableFileOutput && fileWriter != null) {
            fileWriter.println(formatLogEntry(entry));
            if (exception != null) {
                exception.printStackTrace(fileWriter);
            }
            fileWriter.flush();
        }
        
        // Update metrics
        if (enableMetrics) {
            metrics.incrementCounter("log.entries." + level.getName().toLowerCase());
            metrics.incrementCounter("log.categories." + category.getPrefix().toLowerCase());
        }
    }
    
    private StackTraceElement getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // Skip getStackTrace(), getCallerInfo(), and log() methods
        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (!element.getClassName().equals(this.getClass().getName())) {
                return element;
            }
        }
        
        return null;
    }
    
    private void addToHistory(LogEntry entry) {
        synchronized (logHistory) {
            logHistory.add(entry);
            while (logHistory.size() > maxHistorySize) {
                logHistory.remove(0);
            }
        }
    }
    
    private String formatLogEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[").append(entry.getTimestamp().format(TIME_FORMATTER)).append("] ");
        sb.append("[").append(entry.getLevel().getName()).append("] ");
        sb.append("[").append(entry.getCategory().getPrefix()).append("] ");
        sb.append("[").append(entry.getClassName().substring(entry.getClassName().lastIndexOf('.') + 1));
        sb.append(".").append(entry.getMethodName()).append("] ");
        sb.append(entry.getMessage());
        
        if (!entry.getContext().isEmpty()) {
            sb.append(" | Context: ").append(entry.getContext());
        }
        
        return sb.toString();
    }
    
    // Convenience methods
    public void trace(String message) { log(LogLevel.TRACE, LogCategory.GENERAL, message); }
    public void trace(LogCategory category, String message) { log(LogLevel.TRACE, category, message); }
    public void trace(LogCategory category, String message, Map<String, Object> context) { log(LogLevel.TRACE, category, message, context); }
    
    public void debug(String message) { log(LogLevel.DEBUG, LogCategory.GENERAL, message); }
    public void debug(LogCategory category, String message) { log(LogLevel.DEBUG, category, message); }
    public void debug(LogCategory category, String message, Map<String, Object> context) { log(LogLevel.DEBUG, category, message, context); }
    
    public void info(String message) { log(LogLevel.INFO, LogCategory.GENERAL, message); }
    public void info(LogCategory category, String message) { log(LogLevel.INFO, category, message); }
    public void info(LogCategory category, String message, Map<String, Object> context) { log(LogLevel.INFO, category, message, context); }
    
    public void warn(String message) { log(LogLevel.WARN, LogCategory.GENERAL, message); }
    public void warn(LogCategory category, String message) { log(LogLevel.WARN, category, message); }
    public void warn(LogCategory category, String message, Map<String, Object> context) { log(LogLevel.WARN, category, message, context); }
    public void warn(LogCategory category, String message, Throwable exception) { log(LogLevel.WARN, category, message, exception); }
    
    public void error(String message) { log(LogLevel.ERROR, LogCategory.GENERAL, message); }
    public void error(LogCategory category, String message) { log(LogLevel.ERROR, category, message); }
    public void error(LogCategory category, String message, Throwable exception) { log(LogLevel.ERROR, category, message, exception); }
    public void error(LogCategory category, String message, Map<String, Object> context, Throwable exception) { log(LogLevel.ERROR, category, message, context, exception); }
    
    public void fatal(String message) { log(LogLevel.FATAL, LogCategory.GENERAL, message); }
    public void fatal(LogCategory category, String message, Throwable exception) { log(LogLevel.FATAL, category, message, exception); }
    
    // Performance tracking
    public TimingScope startTiming(String operationName) {
        return startTiming(operationName, Collections.emptyMap());
    }
    
    public TimingScope startTiming(String operationName, Map<String, Object> context) {
        return new TimingScope(this, operationName, context);
    }
    
    public void recordMetric(String name, Object value) {
        if (enableMetrics) {
            if (value instanceof Number) {
                metrics.setGauge(name, value);
            } else {
                metrics.incrementCounter(name);
            }
        }
    }
    
    // Query methods
    public List<LogEntry> getRecentEntries(int count) {
        synchronized (logHistory) {
            int size = logHistory.size();
            int fromIndex = Math.max(0, size - count);
            return new ArrayList<>(logHistory.subList(fromIndex, size));
        }
    }
    
    public List<LogEntry> getEntriesByLevel(LogLevel level) {
        synchronized (logHistory) {
            return logHistory.stream()
                           .filter(entry -> entry.getLevel() == level)
                           .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }
    
    public List<LogEntry> getEntriesByCategory(LogCategory category) {
        synchronized (logHistory) {
            return logHistory.stream()
                           .filter(entry -> entry.getCategory() == category)
                           .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }
    
    public PerformanceMetrics getMetrics() {
        return metrics;
    }
    
    // Analysis and reporting
    public void printSummaryReport() {
        System.out.println("\n=== PolyType Logger Summary Report ===");
        System.out.println("Logger: " + name);
        System.out.println("Total log entries: " + logHistory.size());
        System.out.println("Minimum log level: " + minimumLevel.getName());
        System.out.println("Console output: " + (enableConsoleOutput ? "enabled" : "disabled"));
        System.out.println("File output: " + (enableFileOutput ? "enabled (" + logFilePath + ")" : "disabled"));
        System.out.println("Metrics: " + (enableMetrics ? "enabled" : "disabled"));
        
        // Log level distribution
        System.out.println("\nLog Level Distribution:");
        Map<LogLevel, Long> levelCounts = new HashMap<>();
        synchronized (logHistory) {
            for (LogEntry entry : logHistory) {
                levelCounts.merge(entry.getLevel(), 1L, Long::sum);
            }
        }
        for (LogLevel level : LogLevel.values()) {
            long count = levelCounts.getOrDefault(level, 0L);
            if (count > 0) {
                System.out.println("  " + level.getName() + ": " + count);
            }
        }
        
        // Category distribution
        System.out.println("\nCategory Distribution:");
        Map<LogCategory, Long> categoryCounts = new HashMap<>();
        synchronized (logHistory) {
            for (LogEntry entry : logHistory) {
                categoryCounts.merge(entry.getCategory(), 1L, Long::sum);
            }
        }
        for (LogCategory category : LogCategory.values()) {
            long count = categoryCounts.getOrDefault(category, 0L);
            if (count > 0) {
                System.out.println("  " + category.getPrefix() + ": " + count);
            }
        }
        
        // Performance metrics
        if (enableMetrics) {
            System.out.println("\nPerformance Metrics:");
            
            System.out.println("  Counters:");
            for (String counterName : metrics.getCounterNames()) {
                System.out.println("    " + counterName + ": " + metrics.getCounter(counterName));
            }
            
            System.out.println("  Timing Averages:");
            for (String timingName : metrics.getTimingNames()) {
                List<Long> timings = metrics.getTimings(timingName);
                if (!timings.isEmpty()) {
                    double average = timings.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    System.out.println("    " + timingName + ": " + String.format("%.2f ms", average));
                }
            }
            
            System.out.println("  Gauges:");
            for (String gaugeName : metrics.getGaugeNames()) {
                System.out.println("    " + gaugeName + ": " + metrics.getGauge(gaugeName));
            }
        }
        
        System.out.println("=====================================\n");
    }
    
    // Cleanup
    public void shutdown() {
        closeFileOutput();
        logHistory.clear();
    }
    
    public static void shutdownAll() {
        for (PolyTypeLogger logger : loggers.values()) {
            logger.shutdown();
        }
        loggers.clear();
    }
    
    // Static convenience methods
    public static void configureGlobalLogging(LogLevel minimumLevel, boolean console, 
                                            boolean file, String filePath) {
        for (PolyTypeLogger logger : loggers.values()) {
            logger.setMinimumLevel(minimumLevel)
                  .setConsoleOutput(console)
                  .setFileOutput(file, filePath);
        }
    }
}