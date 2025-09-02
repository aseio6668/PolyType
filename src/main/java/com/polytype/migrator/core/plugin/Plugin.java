package com.polytype.migrator.core.plugin;

/**
 * Base interface for all PolyType plugins.
 * Defines the plugin lifecycle and basic operations.
 */
public interface Plugin {
    
    /**
     * Initialize the plugin with the provided context.
     * This is called once when the plugin is loaded.
     * 
     * @param context Plugin context providing access to system services
     * @throws Exception if initialization fails
     */
    void initialize(PluginContext context) throws Exception;
    
    /**
     * Activate the plugin.
     * This is called to make the plugin active and ready to process requests.
     * 
     * @throws Exception if activation fails
     */
    void activate() throws Exception;
    
    /**
     * Deactivate the plugin.
     * This is called to temporarily disable the plugin.
     * The plugin should stop processing new requests but maintain its state.
     * 
     * @throws Exception if deactivation fails
     */
    void deactivate() throws Exception;
    
    /**
     * Clean up resources and prepare for unloading.
     * This is called before the plugin is unloaded from memory.
     * 
     * @throws Exception if cleanup fails
     */
    void cleanup() throws Exception;
    
    /**
     * Get plugin information.
     * This should return consistent information about the plugin.
     * 
     * @return Plugin information
     */
    default String getPluginId() { return "unknown"; }
    
    /**
     * Check if the plugin is healthy and functioning correctly.
     * This can be used for monitoring and diagnostics.
     * 
     * @return true if the plugin is healthy
     */
    default boolean isHealthy() {
        return true;
    }
    
    /**
     * Get detailed health status.
     * 
     * @return Health status information
     */
    default String getHealthStatus() {
        return isHealthy() ? "OK" : "ERROR";
    }
}