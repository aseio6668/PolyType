package com.polytype.migrator.scripts;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of script generation for a target language and platform.
 * Contains all generated scripts organized by category.
 */
public class ScriptGenerationResult {
    
    private Map<String, String> buildScripts;
    private Map<String, String> setupScripts;
    private Map<String, String> runtimeScripts;
    private Map<String, String> ciCdScripts;
    private Map<String, String> crossPlatformScripts;
    private Map<String, String> configurationFiles;
    private Map<String, String> documentationFiles;
    private Map<String, Object> metadata;
    
    public ScriptGenerationResult() {
        this.buildScripts = new HashMap<>();
        this.setupScripts = new HashMap<>();
        this.runtimeScripts = new HashMap<>();
        this.ciCdScripts = new HashMap<>();
        this.crossPlatformScripts = new HashMap<>();
        this.configurationFiles = new HashMap<>();
        this.documentationFiles = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    // Getters and setters
    public Map<String, String> getBuildScripts() { return buildScripts; }
    public void setBuildScripts(Map<String, String> buildScripts) { this.buildScripts = buildScripts; }
    public void addBuildScript(String filename, String content) { this.buildScripts.put(filename, content); }
    
    public Map<String, String> getSetupScripts() { return setupScripts; }
    public void setSetupScripts(Map<String, String> setupScripts) { this.setupScripts = setupScripts; }
    public void addSetupScript(String filename, String content) { this.setupScripts.put(filename, content); }
    
    public Map<String, String> getRuntimeScripts() { return runtimeScripts; }
    public void setRuntimeScripts(Map<String, String> runtimeScripts) { this.runtimeScripts = runtimeScripts; }
    public void addRuntimeScript(String filename, String content) { this.runtimeScripts.put(filename, content); }
    
    public Map<String, String> getCiCdScripts() { return ciCdScripts; }
    public void setCiCdScripts(Map<String, String> ciCdScripts) { this.ciCdScripts = ciCdScripts; }
    public void addCiCdScript(String filename, String content) { this.ciCdScripts.put(filename, content); }
    
    public Map<String, String> getCrossPlatformScripts() { return crossPlatformScripts; }
    public void setCrossPlatformScripts(Map<String, String> crossPlatformScripts) { this.crossPlatformScripts = crossPlatformScripts; }
    public void addCrossPlatformScript(String filename, String content) { this.crossPlatformScripts.put(filename, content); }
    
    public Map<String, String> getConfigurationFiles() { return configurationFiles; }
    public void setConfigurationFiles(Map<String, String> configurationFiles) { this.configurationFiles = configurationFiles; }
    public void addConfigurationFile(String filename, String content) { this.configurationFiles.put(filename, content); }
    
    public Map<String, String> getDocumentationFiles() { return documentationFiles; }
    public void setDocumentationFiles(Map<String, String> documentationFiles) { this.documentationFiles = documentationFiles; }
    public void addDocumentationFile(String filename, String content) { this.documentationFiles.put(filename, content); }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    
    /**
     * Get all generated files as a single map.
     */
    public Map<String, String> getAllFiles() {
        Map<String, String> allFiles = new HashMap<>();
        allFiles.putAll(buildScripts);
        allFiles.putAll(setupScripts);
        allFiles.putAll(runtimeScripts);
        allFiles.putAll(ciCdScripts);
        allFiles.putAll(crossPlatformScripts);
        allFiles.putAll(configurationFiles);
        allFiles.putAll(documentationFiles);
        return allFiles;
    }
    
    /**
     * Get the total number of generated files.
     */
    public int getTotalFileCount() {
        return buildScripts.size() + setupScripts.size() + runtimeScripts.size() + 
               ciCdScripts.size() + crossPlatformScripts.size() + configurationFiles.size() + 
               documentationFiles.size();
    }
    
    /**
     * Check if any scripts were generated.
     */
    public boolean hasGeneratedScripts() {
        return getTotalFileCount() > 0;
    }
    
    /**
     * Get a summary of what was generated.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Generated ").append(getTotalFileCount()).append(" files:\n");
        
        if (!buildScripts.isEmpty()) {
            summary.append("- Build scripts: ").append(buildScripts.keySet()).append("\n");
        }
        if (!setupScripts.isEmpty()) {
            summary.append("- Setup scripts: ").append(setupScripts.keySet()).append("\n");
        }
        if (!runtimeScripts.isEmpty()) {
            summary.append("- Runtime scripts: ").append(runtimeScripts.keySet()).append("\n");
        }
        if (!ciCdScripts.isEmpty()) {
            summary.append("- CI/CD scripts: ").append(ciCdScripts.keySet()).append("\n");
        }
        if (!crossPlatformScripts.isEmpty()) {
            summary.append("- Cross-platform scripts: ").append(crossPlatformScripts.keySet()).append("\n");
        }
        if (!configurationFiles.isEmpty()) {
            summary.append("- Configuration files: ").append(configurationFiles.keySet()).append("\n");
        }
        if (!documentationFiles.isEmpty()) {
            summary.append("- Documentation: ").append(documentationFiles.keySet()).append("\n");
        }
        
        return summary.toString();
    }
}