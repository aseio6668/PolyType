package com.polytype.migrator.core.config;

import com.polytype.migrator.core.logging.PolyTypeLogger;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Comprehensive configuration and extensibility framework for PolyType.
 * Supports hierarchical configuration, environment overrides, validation, and hot-reloading.
 */
public class PolyTypeConfig {
    
    public enum ConfigSource {
        DEFAULT,        // Built-in defaults
        FILE,          // Configuration file
        ENVIRONMENT,   // Environment variables  
        SYSTEM_PROPS,  // System properties
        PROGRAMMATIC   // Set via API
    }
    
    public static class ConfigValue<T> {
        private final T value;
        private final ConfigSource source;
        private final String description;
        private final Class<T> type;
        private final Function<String, T> parser;
        private final Function<T, Boolean> validator;
        
        public ConfigValue(T value, ConfigSource source, String description, 
                          Class<T> type, Function<String, T> parser, 
                          Function<T, Boolean> validator) {
            this.value = value;
            this.source = source;
            this.description = description;
            this.type = type;
            this.parser = parser;
            this.validator = validator;
        }
        
        public T getValue() { return value; }
        public ConfigSource getSource() { return source; }
        public String getDescription() { return description; }
        public Class<T> getType() { return type; }
        public Function<String, T> getParser() { return parser; }
        public Function<T, Boolean> getValidator() { return validator; }
    }
    
    public static class ConfigProperty<T> {
        private final String key;
        private final T defaultValue;
        private final String description;
        private final Class<T> type;
        private final Function<String, T> parser;
        private final Function<T, Boolean> validator;
        
        public ConfigProperty(String key, T defaultValue, String description, 
                             Class<T> type, Function<String, T> parser, 
                             Function<T, Boolean> validator) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.description = description;
            this.type = type;
            this.parser = parser;
            this.validator = validator;
        }
        
        public String getKey() { return key; }
        public T getDefaultValue() { return defaultValue; }
        public String getDescription() { return description; }
        public Class<T> getType() { return type; }
        public Function<String, T> getParser() { return parser; }
        public Function<T, Boolean> getValidator() { return validator; }
    }
    
    // Core configuration properties
    public static final ConfigProperty<String> LOG_LEVEL = 
        new ConfigProperty<>("polytype.log.level", "INFO", "Global log level", 
                           String.class, s -> s.toUpperCase(), 
                           s -> Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL").contains(s));
    
    public static final ConfigProperty<Boolean> LOG_CONSOLE = 
        new ConfigProperty<>("polytype.log.console", true, "Enable console logging",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    public static final ConfigProperty<Boolean> LOG_FILE = 
        new ConfigProperty<>("polytype.log.file", false, "Enable file logging",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    public static final ConfigProperty<String> LOG_FILE_PATH = 
        new ConfigProperty<>("polytype.log.file.path", "polytype.log", "Log file path",
                           String.class, s -> s, s -> s != null && !s.trim().isEmpty());
    
    // Translation configuration
    public static final ConfigProperty<Integer> MAX_TRANSLATION_DEPTH = 
        new ConfigProperty<>("polytype.translation.max_depth", 10, "Maximum translation recursion depth",
                           Integer.class, Integer::parseInt, i -> i > 0 && i <= 100);
    
    public static final ConfigProperty<Integer> TRANSLATION_TIMEOUT = 
        new ConfigProperty<>("polytype.translation.timeout", 30000, "Translation timeout in milliseconds",
                           Integer.class, Integer::parseInt, i -> i > 0);
    
    public static final ConfigProperty<Boolean> ENABLE_PARALLEL_TRANSLATION = 
        new ConfigProperty<>("polytype.translation.parallel", true, "Enable parallel translation processing",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    public static final ConfigProperty<Integer> THREAD_POOL_SIZE = 
        new ConfigProperty<>("polytype.translation.threads", 4, "Translation thread pool size",
                           Integer.class, Integer::parseInt, i -> i > 0 && i <= 32);
    
    // ML configuration
    public static final ConfigProperty<Boolean> ENABLE_ML = 
        new ConfigProperty<>("polytype.ml.enabled", true, "Enable ML features",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    public static final ConfigProperty<Double> ML_CONFIDENCE_THRESHOLD = 
        new ConfigProperty<>("polytype.ml.confidence_threshold", 0.7, "ML confidence threshold",
                           Double.class, Double::parseDouble, d -> d >= 0.0 && d <= 1.0);
    
    public static final ConfigProperty<Integer> ML_FEEDBACK_BUFFER_SIZE = 
        new ConfigProperty<>("polytype.ml.feedback_buffer_size", 1000, "ML feedback buffer size",
                           Integer.class, Integer::parseInt, i -> i > 0);
    
    // Cache configuration
    public static final ConfigProperty<Boolean> ENABLE_CACHE = 
        new ConfigProperty<>("polytype.cache.enabled", true, "Enable caching",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    public static final ConfigProperty<Integer> CACHE_MAX_SIZE = 
        new ConfigProperty<>("polytype.cache.max_size", 10000, "Maximum cache entries",
                           Integer.class, Integer::parseInt, i -> i > 0);
    
    public static final ConfigProperty<Integer> CACHE_EXPIRE_MINUTES = 
        new ConfigProperty<>("polytype.cache.expire_minutes", 60, "Cache expiration time in minutes",
                           Integer.class, Integer::parseInt, i -> i > 0);
    
    // Performance configuration
    public static final ConfigProperty<Boolean> ENABLE_METRICS = 
        new ConfigProperty<>("polytype.metrics.enabled", true, "Enable performance metrics",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    public static final ConfigProperty<Integer> METRICS_BUFFER_SIZE = 
        new ConfigProperty<>("polytype.metrics.buffer_size", 5000, "Metrics buffer size",
                           Integer.class, Integer::parseInt, i -> i > 0);
    
    // Security configuration
    public static final ConfigProperty<Boolean> ENABLE_SANDBOX = 
        new ConfigProperty<>("polytype.security.sandbox", true, "Enable security sandbox",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    public static final ConfigProperty<String> TEMP_DIR = 
        new ConfigProperty<>("polytype.temp.dir", System.getProperty("java.io.tmpdir"), "Temporary directory",
                           String.class, s -> s, s -> new File(s).exists());
    
    // Plugin configuration
    public static final ConfigProperty<String> PLUGIN_DIR = 
        new ConfigProperty<>("polytype.plugins.dir", "plugins", "Plugin directory",
                           String.class, s -> s, s -> s != null);
    
    public static final ConfigProperty<Boolean> AUTO_LOAD_PLUGINS = 
        new ConfigProperty<>("polytype.plugins.auto_load", true, "Auto-load plugins at startup",
                           Boolean.class, Boolean::parseBoolean, b -> true);
    
    // Instance
    private static PolyTypeConfig instance;
    private final Map<String, ConfigValue<?>> values = new ConcurrentHashMap<>();
    private final Map<String, ConfigProperty<?>> properties = new ConcurrentHashMap<>();
    private final List<ConfigChangeListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final PolyTypeLogger logger = PolyTypeLogger.getLogger(PolyTypeConfig.class);
    
    // Configuration file support
    private String configFilePath = "polytype.properties";
    private boolean autoReload = true;
    private long lastModified = 0;
    
    public interface ConfigChangeListener {
        void onConfigChanged(String key, Object oldValue, Object newValue, ConfigSource source);
    }
    
    private PolyTypeConfig() {
        registerDefaultProperties();
        loadConfiguration();
    }
    
    public static synchronized PolyTypeConfig getInstance() {
        if (instance == null) {
            instance = new PolyTypeConfig();
        }
        return instance;
    }
    
    private void registerDefaultProperties() {
        registerProperty(LOG_LEVEL);
        registerProperty(LOG_CONSOLE);
        registerProperty(LOG_FILE);
        registerProperty(LOG_FILE_PATH);
        registerProperty(MAX_TRANSLATION_DEPTH);
        registerProperty(TRANSLATION_TIMEOUT);
        registerProperty(ENABLE_PARALLEL_TRANSLATION);
        registerProperty(THREAD_POOL_SIZE);
        registerProperty(ENABLE_ML);
        registerProperty(ML_CONFIDENCE_THRESHOLD);
        registerProperty(ML_FEEDBACK_BUFFER_SIZE);
        registerProperty(ENABLE_CACHE);
        registerProperty(CACHE_MAX_SIZE);
        registerProperty(CACHE_EXPIRE_MINUTES);
        registerProperty(ENABLE_METRICS);
        registerProperty(METRICS_BUFFER_SIZE);
        registerProperty(ENABLE_SANDBOX);
        registerProperty(TEMP_DIR);
        registerProperty(PLUGIN_DIR);
        registerProperty(AUTO_LOAD_PLUGINS);
    }
    
    @SuppressWarnings("unchecked")
    private <T> void registerProperty(ConfigProperty<T> property) {
        properties.put(property.getKey(), property);
        values.put(property.getKey(), new ConfigValue<>(
            property.getDefaultValue(),
            ConfigSource.DEFAULT,
            property.getDescription(),
            property.getType(),
            property.getParser(),
            (Function<T, Boolean>) property.getValidator()
        ));
    }
    
    public void loadConfiguration() {
        logger.info(PolyTypeLogger.LogCategory.CONFIG, "Loading configuration");
        
        // Load from file
        loadFromFile();
        
        // Override with environment variables
        loadFromEnvironment();
        
        // Override with system properties
        loadFromSystemProperties();
        
        validateConfiguration();
        
        logger.info(PolyTypeLogger.LogCategory.CONFIG, "Configuration loaded successfully");
    }
    
    private void loadFromFile() {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            logger.info(PolyTypeLogger.LogCategory.CONFIG, 
                       "Configuration file not found: " + configFilePath + ", using defaults");
            return;
        }
        
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
            lastModified = configFile.lastModified();
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                setConfigValue(key, value, ConfigSource.FILE);
            }
            
            logger.info(PolyTypeLogger.LogCategory.CONFIG, 
                       "Loaded " + props.size() + " properties from " + configFilePath);
            
        } catch (IOException e) {
            logger.error(PolyTypeLogger.LogCategory.CONFIG, 
                        "Failed to load configuration file: " + configFilePath, e);
        }
    }
    
    private void loadFromEnvironment() {
        int count = 0;
        for (ConfigProperty<?> property : properties.values()) {
            String envKey = property.getKey().replace('.', '_').toUpperCase();
            String envValue = System.getenv(envKey);
            
            if (envValue != null) {
                setConfigValue(property.getKey(), envValue, ConfigSource.ENVIRONMENT);
                count++;
            }
        }
        
        if (count > 0) {
            logger.info(PolyTypeLogger.LogCategory.CONFIG, 
                       "Loaded " + count + " properties from environment variables");
        }
    }
    
    private void loadFromSystemProperties() {
        int count = 0;
        for (ConfigProperty<?> property : properties.values()) {
            String sysProp = System.getProperty(property.getKey());
            
            if (sysProp != null) {
                setConfigValue(property.getKey(), sysProp, ConfigSource.SYSTEM_PROPS);
                count++;
            }
        }
        
        if (count > 0) {
            logger.info(PolyTypeLogger.LogCategory.CONFIG, 
                       "Loaded " + count + " properties from system properties");
        }
    }
    
    @SuppressWarnings("unchecked")
    private void setConfigValue(String key, String stringValue, ConfigSource source) {
        ConfigProperty<?> property = properties.get(key);
        if (property == null) {
            logger.warn(PolyTypeLogger.LogCategory.CONFIG, 
                       "Unknown configuration property: " + key);
            return;
        }
        
        try {
            Object parsedValue = property.getParser().apply(stringValue);
            
            // Validate the value
            if (!((Function<Object, Boolean>) property.getValidator()).apply(parsedValue)) {
                logger.error(PolyTypeLogger.LogCategory.CONFIG, 
                            "Invalid value for " + key + ": " + stringValue);
                return;
            }
            
            ConfigValue<?> oldValue = values.get(key);
            ConfigValue<Object> newValue = new ConfigValue<>(
                parsedValue, source, property.getDescription(), 
                (Class<Object>) property.getType(),
                (Function<String, Object>) property.getParser(),
                (Function<Object, Boolean>) property.getValidator()
            );
            
            values.put(key, newValue);
            
            // Notify listeners
            notifyListeners(key, oldValue != null ? oldValue.getValue() : null, parsedValue, source);
            
        } catch (Exception e) {
            logger.error(PolyTypeLogger.LogCategory.CONFIG, 
                        "Failed to parse value for " + key + ": " + stringValue, e);
        }
    }
    
    private void notifyListeners(String key, Object oldValue, Object newValue, ConfigSource source) {
        for (ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigChanged(key, oldValue, newValue, source);
            } catch (Exception e) {
                logger.error(PolyTypeLogger.LogCategory.CONFIG, 
                            "Error in configuration change listener", e);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(ConfigProperty<T> property) {
        ConfigValue<?> value = values.get(property.getKey());
        return value != null ? (T) value.getValue() : property.getDefaultValue();
    }
    
    public <T> void set(ConfigProperty<T> property, T value) {
        if (property.getValidator().apply(value)) {
            ConfigValue<T> oldValue = (ConfigValue<T>) values.get(property.getKey());
            ConfigValue<T> newValue = new ConfigValue<>(
                value, ConfigSource.PROGRAMMATIC, property.getDescription(),
                property.getType(), property.getParser(), property.getValidator()
            );
            
            values.put(property.getKey(), newValue);
            notifyListeners(property.getKey(), 
                           oldValue != null ? oldValue.getValue() : null, 
                           value, ConfigSource.PROGRAMMATIC);
        } else {
            throw new IllegalArgumentException("Invalid value for " + property.getKey() + ": " + value);
        }
    }
    
    public String getString(ConfigProperty<String> property) {
        return get(property);
    }
    
    public int getInt(ConfigProperty<Integer> property) {
        return get(property);
    }
    
    public double getDouble(ConfigProperty<Double> property) {
        return get(property);
    }
    
    public boolean getBoolean(ConfigProperty<Boolean> property) {
        return get(property);
    }
    
    public void addChangeListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeChangeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void validateConfiguration() {
        int validCount = 0;
        int invalidCount = 0;
        
        for (Map.Entry<String, ConfigValue<?>> entry : values.entrySet()) {
            String key = entry.getKey();
            ConfigValue<?> value = entry.getValue();
            
            try {
                @SuppressWarnings("unchecked")
                Function<Object, Boolean> validator = (Function<Object, Boolean>) value.getValidator();
                if (validator.apply(value.getValue())) {
                    validCount++;
                } else {
                    invalidCount++;
                    logger.error(PolyTypeLogger.LogCategory.CONFIG, 
                                "Invalid configuration value: " + key + " = " + value.getValue());
                }
            } catch (Exception e) {
                invalidCount++;
                logger.error(PolyTypeLogger.LogCategory.CONFIG, 
                            "Validation error for " + key, e);
            }
        }
        
        logger.info(PolyTypeLogger.LogCategory.CONFIG, 
                   "Configuration validation: " + validCount + " valid, " + invalidCount + " invalid");
        
        if (invalidCount > 0) {
            logger.warn(PolyTypeLogger.LogCategory.CONFIG, 
                       "Configuration has " + invalidCount + " invalid values");
        }
    }
    
    public void setConfigFilePath(String filePath) {
        this.configFilePath = filePath;
    }
    
    public void setAutoReload(boolean autoReload) {
        this.autoReload = autoReload;
    }
    
    public void reloadIfChanged() {
        if (!autoReload) return;
        
        File configFile = new File(configFilePath);
        if (configFile.exists() && configFile.lastModified() > lastModified) {
            logger.info(PolyTypeLogger.LogCategory.CONFIG, 
                       "Configuration file changed, reloading");
            loadConfiguration();
        }
    }
    
    public void saveConfiguration() throws IOException {
        Properties props = new Properties();
        
        // Add comments with descriptions
        StringBuilder comments = new StringBuilder();
        comments.append("PolyType Configuration File\n");
        comments.append("Generated at: ").append(new Date()).append("\n\n");
        
        for (Map.Entry<String, ConfigValue<?>> entry : values.entrySet()) {
            String key = entry.getKey();
            ConfigValue<?> value = entry.getValue();
            
            props.setProperty(key, value.getValue().toString());
            comments.append("# ").append(value.getDescription()).append("\n");
            comments.append("# Source: ").append(value.getSource()).append("\n");
            comments.append("# Type: ").append(value.getType().getSimpleName()).append("\n");
            comments.append(key).append("=").append(value.getValue()).append("\n\n");
        }
        
        try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
            props.store(fos, comments.toString());
        }
        
        logger.info(PolyTypeLogger.LogCategory.CONFIG, 
                   "Configuration saved to: " + configFilePath);
    }
    
    public void printConfiguration() {
        System.out.println("\n=== PolyType Configuration ===");
        System.out.println("Configuration file: " + configFilePath);
        System.out.println("Auto-reload: " + autoReload);
        System.out.println("Total properties: " + values.size());
        System.out.println();
        
        // Group by category
        Map<String, List<Map.Entry<String, ConfigValue<?>>>> categories = new HashMap<>();
        
        for (Map.Entry<String, ConfigValue<?>> entry : values.entrySet()) {
            String key = entry.getKey();
            String category = key.split("\\.")[1]; // polytype.category.property
            
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(entry);
        }
        
        for (Map.Entry<String, List<Map.Entry<String, ConfigValue<?>>>> categoryEntry : categories.entrySet()) {
            String category = categoryEntry.getKey();
            List<Map.Entry<String, ConfigValue<?>>> properties = categoryEntry.getValue();
            
            System.out.println(category.toUpperCase() + " Configuration:");
            for (Map.Entry<String, ConfigValue<?>> propEntry : properties) {
                String key = propEntry.getKey();
                ConfigValue<?> value = propEntry.getValue();
                
                System.out.println("  " + key + " = " + value.getValue() + 
                                 " [" + value.getSource() + "]");
                System.out.println("    " + value.getDescription());
            }
            System.out.println();
        }
        
        System.out.println("==============================\n");
    }
    
    public Map<String, Object> getConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        for (Map.Entry<String, ConfigValue<?>> entry : values.entrySet()) {
            config.put(entry.getKey(), entry.getValue().getValue());
        }
        return config;
    }
    
    public Set<String> getPropertyKeys() {
        return new HashSet<>(properties.keySet());
    }
    
    public ConfigValue<?> getConfigValue(String key) {
        return values.get(key);
    }
    
    // Environment-specific configurations
    public void loadDevelopmentConfig() {
        set(LOG_LEVEL, "DEBUG");
        set(LOG_CONSOLE, true);
        set(LOG_FILE, true);
        set(ENABLE_METRICS, true);
        set(ENABLE_CACHE, false); // Disable cache for development
        
        logger.info(PolyTypeLogger.LogCategory.CONFIG, "Loaded development configuration");
    }
    
    public void loadProductionConfig() {
        set(LOG_LEVEL, "WARN");
        set(LOG_CONSOLE, false);
        set(LOG_FILE, true);
        set(ENABLE_METRICS, true);
        set(ENABLE_CACHE, true);
        set(ENABLE_SANDBOX, true);
        
        logger.info(PolyTypeLogger.LogCategory.CONFIG, "Loaded production configuration");
    }
    
    public void loadTestConfig() {
        set(LOG_LEVEL, "ERROR");
        set(LOG_CONSOLE, false);
        set(LOG_FILE, false);
        set(ENABLE_METRICS, false);
        set(ENABLE_CACHE, false);
        set(TRANSLATION_TIMEOUT, 5000); // Shorter timeout for tests
        
        logger.info(PolyTypeLogger.LogCategory.CONFIG, "Loaded test configuration");
    }
}