package com.polytype.migrator.core.plugin;

import com.polytype.migrator.core.logging.PolyTypeLogger;
import com.polytype.migrator.core.config.PolyTypeConfig;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Comprehensive plugin architecture for PolyType extensibility.
 * Supports hot-loading, versioning, dependency management, and lifecycle management.
 */
public class PluginManager {
    
    public enum PluginState {
        DISCOVERED,    // Plugin found but not loaded
        LOADED,        // Plugin loaded into memory
        INITIALIZED,   // Plugin initialized and ready
        ACTIVE,        // Plugin is running
        DISABLED,      // Plugin disabled by user
        ERROR,         // Plugin has errors
        UNLOADED      // Plugin unloaded from memory
    }
    
    public enum PluginType {
        LANGUAGE_PARSER,     // New language parsers
        CODE_TRANSLATOR,     // Translation engines
        ML_MODEL,           // Machine learning models
        BINARY_ANALYZER,    // Binary analysis tools
        OUTPUT_FORMATTER,   // Code formatters
        OPTIMIZATION,       // Code optimizers
        VALIDATION,         // Code validators
        SECURITY_SCANNER,   // Security scanners
        METADATA_EXTRACTOR, // Metadata extractors
        CUSTOM             // Custom plugin types
    }
    
    public static class PluginInfo {
        private final String id;
        private final String name;
        private final String version;
        private final String author;
        private final String description;
        private final PluginType type;
        private final List<String> dependencies;
        private final String mainClass;
        private final File jarFile;
        private final Properties metadata;
        
        public PluginInfo(String id, String name, String version, String author,
                         String description, PluginType type, List<String> dependencies,
                         String mainClass, File jarFile, Properties metadata) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.author = author;
            this.description = description;
            this.type = type;
            this.dependencies = new ArrayList<>(dependencies);
            this.mainClass = mainClass;
            this.jarFile = jarFile;
            this.metadata = new Properties();
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getAuthor() { return author; }
        public String getDescription() { return description; }
        public PluginType getType() { return type; }
        public List<String> getDependencies() { return new ArrayList<>(dependencies); }
        public String getMainClass() { return mainClass; }
        public File getJarFile() { return jarFile; }
        public Properties getMetadata() { return new Properties(metadata); }
    }
    
    public static class LoadedPlugin {
        private final PluginInfo info;
        private final Plugin instance;
        private final URLClassLoader classLoader;
        private volatile PluginState state;
        private volatile String errorMessage;
        private final long loadTime;
        private volatile long lastActivity;
        
        public LoadedPlugin(PluginInfo info, Plugin instance, URLClassLoader classLoader) {
            this.info = info;
            this.instance = instance;
            this.classLoader = classLoader;
            this.state = PluginState.LOADED;
            this.loadTime = System.currentTimeMillis();
            this.lastActivity = loadTime;
        }
        
        public PluginInfo getInfo() { return info; }
        public Plugin getInstance() { return instance; }
        public URLClassLoader getClassLoader() { return classLoader; }
        public PluginState getState() { return state; }
        public String getErrorMessage() { return errorMessage; }
        public long getLoadTime() { return loadTime; }
        public long getLastActivity() { return lastActivity; }
        
        public void setState(PluginState state) { 
            this.state = state; 
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void setError(String errorMessage) {
            this.state = PluginState.ERROR;
            this.errorMessage = errorMessage;
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void recordActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
    }
    
    public interface PluginEventListener {
        void onPluginLoaded(PluginInfo info);
        void onPluginUnloaded(PluginInfo info);
        void onPluginStateChanged(PluginInfo info, PluginState oldState, PluginState newState);
        void onPluginError(PluginInfo info, String error, Throwable exception);
    }
    
    private static PluginManager instance;
    
    private final Map<String, LoadedPlugin> loadedPlugins = new ConcurrentHashMap<>();
    private final Map<String, PluginInfo> availablePlugins = new ConcurrentHashMap<>();
    private final List<PluginEventListener> listeners = new CopyOnWriteArrayList<>();
    private final PolyTypeLogger logger = PolyTypeLogger.getLogger(PluginManager.class);
    private final PolyTypeConfig config = PolyTypeConfig.getInstance();
    
    // Plugin directories
    private final Set<Path> pluginDirectories = new HashSet<>();
    private final Map<PluginType, List<LoadedPlugin>> pluginsByType = new ConcurrentHashMap<>();
    
    // File watching for hot reload
    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean watchingEnabled = true;
    
    private PluginManager() {
        initializePluginDirectories();
        if (config.getBoolean(PolyTypeConfig.AUTO_LOAD_PLUGINS)) {
            initializeFileWatcher();
        }
    }
    
    public static synchronized PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }
    
    private void initializePluginDirectories() {
        String pluginDir = config.getString(PolyTypeConfig.PLUGIN_DIR);
        
        // Add configured plugin directory
        Path configuredDir = Paths.get(pluginDir);
        pluginDirectories.add(configuredDir);
        
        // Add system plugin directories
        pluginDirectories.add(Paths.get("plugins"));
        pluginDirectories.add(Paths.get(System.getProperty("user.home"), ".polytype", "plugins"));
        
        // Create directories if they don't exist
        for (Path dir : pluginDirectories) {
            try {
                Files.createDirectories(dir);
                logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Plugin directory: " + dir.toAbsolutePath());
            } catch (IOException e) {
                logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Failed to create plugin directory: " + dir, e);
            }
        }
    }
    
    private void initializeFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            
            for (Path dir : pluginDirectories) {
                if (Files.exists(dir)) {
                    dir.register(watchService, 
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY);
                }
            }
            
            watchThread = new Thread(this::watchPluginDirectories, "PluginWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
            
            logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Plugin file watcher initialized");
            
        } catch (IOException e) {
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Failed to initialize plugin file watcher", e);
        }
    }
    
    private void watchPluginDirectories() {
        while (watchingEnabled && !Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path filePath = (Path) event.context();
                    
                    if (filePath.toString().endsWith(".jar")) {
                        logger.debug(PolyTypeLogger.LogCategory.PLUGIN, 
                                    "Plugin file event: " + kind + " - " + filePath);
                        
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                            kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            // Delay to allow file operations to complete
                            Thread.sleep(1000);
                            discoverPlugin(filePath.toFile());
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            handlePluginFileDeleted(filePath);
                        }
                    }
                }
                
                key.reset();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                            "Error in plugin file watcher", e);
            }
        }
    }
    
    public void discoverPlugins() {
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Discovering plugins...");
        
        int discovered = 0;
        for (Path dir : pluginDirectories) {
            if (!Files.exists(dir)) continue;
            
            try {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
                    for (Path jarPath : stream) {
                        if (discoverPlugin(jarPath.toFile())) {
                            discovered++;
                        }
                    }
                }
            } catch (IOException e) {
                logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                            "Error discovering plugins in: " + dir, e);
            }
        }
        
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                   "Plugin discovery completed: " + discovered + " plugins found");
    }
    
    private boolean discoverPlugin(File jarFile) {
        try {
            PluginInfo info = parsePluginInfo(jarFile);
            if (info != null) {
                availablePlugins.put(info.getId(), info);
                
                logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Discovered plugin: " + info.getName() + " v" + info.getVersion() + 
                           " (" + info.getType() + ")");
                
                return true;
            }
        } catch (Exception e) {
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Failed to discover plugin: " + jarFile.getName(), e);
        }
        
        return false;
    }
    
    private PluginInfo parsePluginInfo(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                           "No manifest found in plugin: " + jarFile.getName());
                return null;
            }
            
            var attributes = manifest.getMainAttributes();
            
            String id = attributes.getValue("Plugin-Id");
            String name = attributes.getValue("Plugin-Name");
            String version = attributes.getValue("Plugin-Version");
            String author = attributes.getValue("Plugin-Author");
            String description = attributes.getValue("Plugin-Description");
            String typeStr = attributes.getValue("Plugin-Type");
            String mainClass = attributes.getValue("Plugin-Main-Class");
            String dependenciesStr = attributes.getValue("Plugin-Dependencies");
            
            if (id == null || name == null || version == null || mainClass == null) {
                logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Invalid plugin manifest: " + jarFile.getName());
                return null;
            }
            
            PluginType type = PluginType.CUSTOM;
            if (typeStr != null) {
                try {
                    type = PluginType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                               "Unknown plugin type: " + typeStr);
                }
            }
            
            List<String> dependencies = new ArrayList<>();
            if (dependenciesStr != null) {
                for (String dep : dependenciesStr.split(",")) {
                    dependencies.add(dep.trim());
                }
            }
            
            // Load additional metadata from plugin.properties if exists
            Properties metadata = new Properties();
            JarEntry propertiesEntry = jar.getJarEntry("plugin.properties");
            if (propertiesEntry != null) {
                try (InputStream is = jar.getInputStream(propertiesEntry)) {
                    metadata.load(is);
                }
            }
            
            return new PluginInfo(id, name, version, author, description, type, 
                                dependencies, mainClass, jarFile, metadata);
        }
    }
    
    public boolean loadPlugin(String pluginId) {
        PluginInfo info = availablePlugins.get(pluginId);
        if (info == null) {
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Plugin not found: " + pluginId);
            return false;
        }
        
        if (loadedPlugins.containsKey(pluginId)) {
            logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                       "Plugin already loaded: " + pluginId);
            return true;
        }
        
        return loadPlugin(info);
    }
    
    private boolean loadPlugin(PluginInfo info) {
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                   "Loading plugin: " + info.getName() + " v" + info.getVersion());
        
        try {
            // Check dependencies
            if (!checkDependencies(info)) {
                logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                            "Dependencies not satisfied for plugin: " + info.getId());
                return false;
            }
            
            // Create class loader
            URL jarUrl = info.getJarFile().toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarUrl}, 
                this.getClass().getClassLoader()
            );
            
            // Load main class
            Class<?> mainClass = classLoader.loadClass(info.getMainClass());
            
            // Check if it implements Plugin interface
            if (!Plugin.class.isAssignableFrom(mainClass)) {
                throw new ClassCastException("Main class does not implement Plugin interface");
            }
            
            // Create instance
            Plugin pluginInstance = (Plugin) mainClass.getDeclaredConstructor().newInstance();
            
            // Create loaded plugin
            LoadedPlugin loadedPlugin = new LoadedPlugin(info, pluginInstance, classLoader);
            
            // Initialize plugin
            if (initializePlugin(loadedPlugin)) {
                loadedPlugins.put(info.getId(), loadedPlugin);
                addToTypeIndex(loadedPlugin);
                
                // Notify listeners
                notifyPluginLoaded(info);
                
                logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Successfully loaded plugin: " + info.getName());
                return true;
            } else {
                // Cleanup on failure
                try {
                    classLoader.close();
                } catch (IOException e) {
                    logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                               "Failed to close class loader", e);
                }
                return false;
            }
            
        } catch (Exception e) {
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Failed to load plugin: " + info.getId(), e);
            return false;
        }
    }
    
    private boolean checkDependencies(PluginInfo info) {
        for (String dependency : info.getDependencies()) {
            LoadedPlugin depPlugin = loadedPlugins.get(dependency);
            if (depPlugin == null || depPlugin.getState() != PluginState.ACTIVE) {
                logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Missing dependency for " + info.getId() + ": " + dependency);
                return false;
            }
        }
        return true;
    }
    
    private boolean initializePlugin(LoadedPlugin loadedPlugin) {
        try {
            Plugin plugin = loadedPlugin.getInstance();
            plugin.initialize(new PluginContextImpl(loadedPlugin));
            
            loadedPlugin.setState(PluginState.INITIALIZED);
            
            logger.debug(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Initialized plugin: " + loadedPlugin.getInfo().getId());
            return true;
            
        } catch (Exception e) {
            loadedPlugin.setError("Initialization failed: " + e.getMessage());
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Failed to initialize plugin: " + loadedPlugin.getInfo().getId(), e);
            return false;
        }
    }
    
    public boolean activatePlugin(String pluginId) {
        LoadedPlugin loadedPlugin = loadedPlugins.get(pluginId);
        if (loadedPlugin == null) {
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Plugin not loaded: " + pluginId);
            return false;
        }
        
        if (loadedPlugin.getState() != PluginState.INITIALIZED) {
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Plugin not ready for activation: " + pluginId);
            return false;
        }
        
        try {
            loadedPlugin.getInstance().activate();
            loadedPlugin.setState(PluginState.ACTIVE);
            
            logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                       "Activated plugin: " + loadedPlugin.getInfo().getName());
            return true;
            
        } catch (Exception e) {
            loadedPlugin.setError("Activation failed: " + e.getMessage());
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Failed to activate plugin: " + pluginId, e);
            return false;
        }
    }
    
    public boolean deactivatePlugin(String pluginId) {
        LoadedPlugin loadedPlugin = loadedPlugins.get(pluginId);
        if (loadedPlugin == null) {
            return false;
        }
        
        try {
            if (loadedPlugin.getState() == PluginState.ACTIVE) {
                loadedPlugin.getInstance().deactivate();
            }
            
            loadedPlugin.setState(PluginState.INITIALIZED);
            
            logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                       "Deactivated plugin: " + loadedPlugin.getInfo().getName());
            return true;
            
        } catch (Exception e) {
            loadedPlugin.setError("Deactivation failed: " + e.getMessage());
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Failed to deactivate plugin: " + pluginId, e);
            return false;
        }
    }
    
    public boolean unloadPlugin(String pluginId) {
        LoadedPlugin loadedPlugin = loadedPlugins.get(pluginId);
        if (loadedPlugin == null) {
            return false;
        }
        
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                   "Unloading plugin: " + loadedPlugin.getInfo().getName());
        
        try {
            // Deactivate first
            if (loadedPlugin.getState() == PluginState.ACTIVE) {
                deactivatePlugin(pluginId);
            }
            
            // Cleanup
            loadedPlugin.getInstance().cleanup();
            
            // Remove from indexes
            loadedPlugins.remove(pluginId);
            removeFromTypeIndex(loadedPlugin);
            
            // Close class loader
            try {
                loadedPlugin.getClassLoader().close();
            } catch (IOException e) {
                logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Failed to close class loader", e);
            }
            
            loadedPlugin.setState(PluginState.UNLOADED);
            
            // Notify listeners
            notifyPluginUnloaded(loadedPlugin.getInfo());
            
            logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                       "Successfully unloaded plugin: " + loadedPlugin.getInfo().getName());
            return true;
            
        } catch (Exception e) {
            logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                        "Failed to unload plugin: " + pluginId, e);
            return false;
        }
    }
    
    private void addToTypeIndex(LoadedPlugin plugin) {
        PluginType type = plugin.getInfo().getType();
        pluginsByType.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(plugin);
    }
    
    private void removeFromTypeIndex(LoadedPlugin plugin) {
        PluginType type = plugin.getInfo().getType();
        List<LoadedPlugin> plugins = pluginsByType.get(type);
        if (plugins != null) {
            plugins.remove(plugin);
        }
    }
    
    private void handlePluginFileDeleted(Path filePath) {
        // Find and unload plugins from deleted file
        String fileName = filePath.toString();
        
        List<String> toUnload = new ArrayList<>();
        for (LoadedPlugin plugin : loadedPlugins.values()) {
            if (plugin.getInfo().getJarFile().getName().equals(fileName)) {
                toUnload.add(plugin.getInfo().getId());
            }
        }
        
        for (String pluginId : toUnload) {
            unloadPlugin(pluginId);
        }
        
        // Remove from available plugins
        availablePlugins.entrySet().removeIf(entry -> 
            entry.getValue().getJarFile().getName().equals(fileName));
    }
    
    // Query methods
    public List<PluginInfo> getAvailablePlugins() {
        return new ArrayList<>(availablePlugins.values());
    }
    
    public List<LoadedPlugin> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins.values());
    }
    
    public List<LoadedPlugin> getActivePlugins() {
        return loadedPlugins.values().stream()
                           .filter(p -> p.getState() == PluginState.ACTIVE)
                           .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<LoadedPlugin> getPluginsByType(PluginType type) {
        List<LoadedPlugin> plugins = pluginsByType.get(type);
        return plugins != null ? new ArrayList<>(plugins) : new ArrayList<>();
    }
    
    public LoadedPlugin getPlugin(String pluginId) {
        return loadedPlugins.get(pluginId);
    }
    
    public PluginInfo getPluginInfo(String pluginId) {
        return availablePlugins.get(pluginId);
    }
    
    public boolean isPluginLoaded(String pluginId) {
        return loadedPlugins.containsKey(pluginId);
    }
    
    public boolean isPluginActive(String pluginId) {
        LoadedPlugin plugin = loadedPlugins.get(pluginId);
        return plugin != null && plugin.getState() == PluginState.ACTIVE;
    }
    
    // Event handling
    public void addPluginEventListener(PluginEventListener listener) {
        listeners.add(listener);
    }
    
    public void removePluginEventListener(PluginEventListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyPluginLoaded(PluginInfo info) {
        for (PluginEventListener listener : listeners) {
            try {
                listener.onPluginLoaded(info);
            } catch (Exception e) {
                logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                            "Error in plugin event listener", e);
            }
        }
    }
    
    private void notifyPluginUnloaded(PluginInfo info) {
        for (PluginEventListener listener : listeners) {
            try {
                listener.onPluginUnloaded(info);
            } catch (Exception e) {
                logger.error(PolyTypeLogger.LogCategory.PLUGIN, 
                            "Error in plugin event listener", e);
            }
        }
    }
    
    // Management operations
    public void loadAllPlugins() {
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Loading all discovered plugins");
        
        int loaded = 0;
        for (PluginInfo info : availablePlugins.values()) {
            if (!loadedPlugins.containsKey(info.getId())) {
                if (loadPlugin(info)) {
                    loaded++;
                }
            }
        }
        
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                   "Loaded " + loaded + " plugins");
    }
    
    public void activateAllPlugins() {
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Activating all loaded plugins");
        
        int activated = 0;
        for (LoadedPlugin plugin : loadedPlugins.values()) {
            if (plugin.getState() == PluginState.INITIALIZED) {
                if (activatePlugin(plugin.getInfo().getId())) {
                    activated++;
                }
            }
        }
        
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, 
                   "Activated " + activated + " plugins");
    }
    
    public void reloadPlugin(String pluginId) {
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Reloading plugin: " + pluginId);
        
        LoadedPlugin plugin = loadedPlugins.get(pluginId);
        if (plugin != null) {
            PluginInfo info = plugin.getInfo();
            
            // Unload current version
            unloadPlugin(pluginId);
            
            // Rediscover (in case file changed)
            discoverPlugin(info.getJarFile());
            
            // Load new version
            loadPlugin(pluginId);
            activatePlugin(pluginId);
        }
    }
    
    public void printPluginStatus() {
        System.out.println("\n=== Plugin Manager Status ===");
        System.out.println("Available plugins: " + availablePlugins.size());
        System.out.println("Loaded plugins: " + loadedPlugins.size());
        System.out.println("Active plugins: " + getActivePlugins().size());
        System.out.println("Plugin directories: " + pluginDirectories.size());
        System.out.println("File watching: " + (watchingEnabled ? "enabled" : "disabled"));
        
        if (!loadedPlugins.isEmpty()) {
            System.out.println("\nLoaded Plugins:");
            for (LoadedPlugin plugin : loadedPlugins.values()) {
                PluginInfo info = plugin.getInfo();
                System.out.println("  " + info.getName() + " v" + info.getVersion() + 
                                 " [" + plugin.getState() + "] (" + info.getType() + ")");
                if (plugin.getErrorMessage() != null) {
                    System.out.println("    Error: " + plugin.getErrorMessage());
                }
            }
        }
        
        System.out.println("==============================\n");
    }
    
    // Lifecycle management
    public void initialize() {
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Initializing Plugin Manager");
        
        discoverPlugins();
        
        if (config.getBoolean(PolyTypeConfig.AUTO_LOAD_PLUGINS)) {
            loadAllPlugins();
            activateAllPlugins();
        }
        
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Plugin Manager initialized");
    }
    
    public void shutdown() {
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Shutting down Plugin Manager");
        
        watchingEnabled = false;
        
        if (watchThread != null) {
            watchThread.interrupt();
            try {
                watchThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn(PolyTypeLogger.LogCategory.PLUGIN, 
                           "Error closing watch service", e);
            }
        }
        
        // Unload all plugins
        List<String> pluginIds = new ArrayList<>(loadedPlugins.keySet());
        for (String pluginId : pluginIds) {
            unloadPlugin(pluginId);
        }
        
        logger.info(PolyTypeLogger.LogCategory.PLUGIN, "Plugin Manager shutdown complete");
    }
    
    // Inner class for plugin context
    private static class PluginContextImpl implements PluginContext {
        private final LoadedPlugin loadedPlugin;
        private final PolyTypeLogger logger;
        private final PolyTypeConfig config;
        
        public PluginContextImpl(LoadedPlugin loadedPlugin) {
            this.loadedPlugin = loadedPlugin;
            this.logger = PolyTypeLogger.getLogger("Plugin-" + loadedPlugin.getInfo().getId());
            this.config = PolyTypeConfig.getInstance();
        }
        
        @Override
        public String getPluginId() {
            return loadedPlugin.getInfo().getId();
        }
        
        @Override
        public PolyTypeLogger getLogger() {
            return logger;
        }
        
        @Override
        public PolyTypeConfig getConfig() {
            return config;
        }
        
        @Override
        public void recordActivity() {
            loadedPlugin.recordActivity();
        }
        
        @Override
        public String getProperty(String key, String defaultValue) {
            return loadedPlugin.getInfo().getMetadata().getProperty(key, defaultValue);
        }
    }
}