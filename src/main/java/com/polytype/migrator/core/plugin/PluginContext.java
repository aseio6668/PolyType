package com.polytype.migrator.core.plugin;

import com.polytype.migrator.core.logging.PolyTypeLogger;
import com.polytype.migrator.core.config.PolyTypeConfig;

/**
 * Context interface providing plugins access to PolyType system services.
 */
public interface PluginContext {
    
    /**
     * Get information about this plugin.
     * 
     * @return Plugin ID
     */
    String getPluginId();
    
    /**
     * Get a logger instance for this plugin.
     * 
     * @return Logger instance
     */
    PolyTypeLogger getLogger();
    
    /**
     * Get access to the global configuration.
     * 
     * @return Configuration instance
     */
    PolyTypeConfig getConfig();
    
    /**
     * Record plugin activity for monitoring purposes.
     * Plugins should call this periodically to indicate they are active.
     */
    void recordActivity();
    
    /**
     * Get a plugin-specific property value.
     * 
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value or default
     */
    String getProperty(String key, String defaultValue);
}