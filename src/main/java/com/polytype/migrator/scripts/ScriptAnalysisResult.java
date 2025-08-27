package com.polytype.migrator.scripts;

import java.util.*;

/**
 * Result of analyzing a single script file.
 * Contains information about the script's purpose, dependencies, build targets, and configuration.
 */
public class ScriptAnalysisResult {
    
    private ScriptAnalyzer.ScriptType scriptType;
    private String filePath;
    private String language;
    private String version;
    private Map<String, String> variables;
    private Set<String> dependencies;
    private Set<String> buildTargets;
    private List<String> commands;
    private Map<String, Object> configuration;
    private List<String> embeddedLanguages;
    private ScriptPurpose purpose;
    private Map<String, String> metadata;
    
    public ScriptAnalysisResult() {
        this.variables = new HashMap<>();
        this.dependencies = new HashSet<>();
        this.buildTargets = new HashSet<>();
        this.commands = new ArrayList<>();
        this.configuration = new HashMap<>();
        this.embeddedLanguages = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.purpose = ScriptPurpose.UNKNOWN;
    }
    
    // Getters and setters
    public ScriptAnalyzer.ScriptType getScriptType() { return scriptType; }
    public void setScriptType(ScriptAnalyzer.ScriptType scriptType) { this.scriptType = scriptType; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public void addVariable(String name, String value) { this.variables.put(name, value); }
    
    public Set<String> getDependencies() { return dependencies; }
    public void setDependencies(Set<String> dependencies) { this.dependencies = dependencies; }
    public void addDependency(String dependency) { this.dependencies.add(dependency); }
    
    public Set<String> getBuildTargets() { return buildTargets; }
    public void setBuildTargets(Set<String> buildTargets) { this.buildTargets = buildTargets; }
    public void addBuildTarget(String target) { this.buildTargets.add(target); }
    
    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    public void addCommand(String command) { this.commands.add(command); }
    
    public Map<String, Object> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
    public void addConfiguration(String key, Object value) { this.configuration.put(key, value); }
    
    public List<String> getEmbeddedLanguages() { return embeddedLanguages; }
    public void setEmbeddedLanguages(List<String> embeddedLanguages) { this.embeddedLanguages = embeddedLanguages; }
    public void addEmbeddedLanguage(String language) { this.embeddedLanguages.add(language); }
    
    public ScriptPurpose getPurpose() { return purpose; }
    public void setPurpose(ScriptPurpose purpose) { this.purpose = purpose; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public void addMetadata(String key, String value) { this.metadata.put(key, value); }
    
    /**
     * Get the primary programming language this script is associated with.
     */
    public String getPrimaryLanguage() {
        switch (scriptType) {
            case PACKAGE_JSON: return "JavaScript";
            case POM_XML: case BUILD_GRADLE: return "Java";
            case CARGO_TOML: return "Rust";
            case SETUP_PY: return "Python";
            case CMAKE: case MAKEFILE: return "C++";
            case COMPOSER_JSON: return "PHP";
            case GEMFILE: return "Ruby";
            default: return language != null ? language : "Unknown";
        }
    }
    
    /**
     * Check if this script has build capabilities.
     */
    public boolean isBuildScript() {
        return purpose == ScriptPurpose.BUILD || 
               scriptType == ScriptAnalyzer.ScriptType.MAKEFILE ||
               scriptType == ScriptAnalyzer.ScriptType.CMAKE ||
               scriptType == ScriptAnalyzer.ScriptType.BUILD_GRADLE ||
               scriptType == ScriptAnalyzer.ScriptType.POM_XML ||
               !buildTargets.isEmpty();
    }
    
    /**
     * Check if this script manages dependencies.
     */
    public boolean managesDependencies() {
        return !dependencies.isEmpty() ||
               scriptType == ScriptAnalyzer.ScriptType.PACKAGE_JSON ||
               scriptType == ScriptAnalyzer.ScriptType.POM_XML ||
               scriptType == ScriptAnalyzer.ScriptType.CARGO_TOML ||
               scriptType == ScriptAnalyzer.ScriptType.SETUP_PY ||
               scriptType == ScriptAnalyzer.ScriptType.REQUIREMENTS ||
               scriptType == ScriptAnalyzer.ScriptType.COMPOSER_JSON ||
               scriptType == ScriptAnalyzer.ScriptType.GEMFILE;
    }
    
    /**
     * Check if this script is for CI/CD.
     */
    public boolean isCiCdScript() {
        return purpose == ScriptPurpose.CICD ||
               scriptType == ScriptAnalyzer.ScriptType.GITHUB_WORKFLOW ||
               scriptType == ScriptAnalyzer.ScriptType.GITLAB_CI ||
               scriptType == ScriptAnalyzer.ScriptType.JENKINS_FILE;
    }
    
    /**
     * Get estimated complexity of the script.
     */
    public ScriptComplexity getComplexity() {
        int score = 0;
        
        // Count various complexity factors
        score += variables.size();
        score += dependencies.size();
        score += buildTargets.size();
        score += commands.size();
        score += embeddedLanguages.size() * 2;
        score += configuration.size();
        
        if (score < 5) return ScriptComplexity.SIMPLE;
        else if (score < 15) return ScriptComplexity.MODERATE;
        else if (score < 30) return ScriptComplexity.COMPLEX;
        else return ScriptComplexity.VERY_COMPLEX;
    }
    
    @Override
    public String toString() {
        return String.format("ScriptAnalysisResult{type=%s, file='%s', language='%s', purpose=%s, complexity=%s}",
                scriptType, filePath, getPrimaryLanguage(), purpose, getComplexity());
    }
    
    public enum ScriptPurpose {
        BUILD, SETUP, RUNTIME, TESTING, DEPLOYMENT, CICD, 
        CONFIGURATION, DOCUMENTATION, DEPENDENCY_MANAGEMENT, UNKNOWN
    }
    
    public enum ScriptComplexity {
        SIMPLE, MODERATE, COMPLEX, VERY_COMPLEX
    }
}