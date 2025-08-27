package com.polytype.migrator.scripts;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete analysis result for an entire project.
 * Aggregates information from all script files and provides project-level insights.
 */
public class ProjectAnalysisResult {
    
    private List<ScriptAnalysisResult> scriptResults;
    private ScriptAnalyzer.ProjectType projectType;
    private ScriptAnalyzer.BuildSystem buildSystem;
    private String mainLanguage;
    private Set<String> dependencies;
    private Set<String> buildTargets;
    private List<String> workflow;
    private Map<String, String> projectConfiguration;
    private ProjectStructure structure;
    private Set<String> detectedLanguages;
    private Map<String, Object> metadata;
    
    public ProjectAnalysisResult() {
        this.scriptResults = new ArrayList<>();
        this.dependencies = new HashSet<>();
        this.buildTargets = new HashSet<>();
        this.workflow = new ArrayList<>();
        this.projectConfiguration = new HashMap<>();
        this.structure = new ProjectStructure();
        this.detectedLanguages = new HashSet<>();
        this.metadata = new HashMap<>();
    }
    
    // Getters and setters
    public List<ScriptAnalysisResult> getScriptResults() { return scriptResults; }
    public void setScriptResults(List<ScriptAnalysisResult> scriptResults) { this.scriptResults = scriptResults; }
    public void addScriptResult(ScriptAnalysisResult result) { this.scriptResults.add(result); }
    
    public ScriptAnalyzer.ProjectType getProjectType() { return projectType; }
    public void setProjectType(ScriptAnalyzer.ProjectType projectType) { this.projectType = projectType; }
    
    public ScriptAnalyzer.BuildSystem getBuildSystem() { return buildSystem; }
    public void setBuildSystem(ScriptAnalyzer.BuildSystem buildSystem) { this.buildSystem = buildSystem; }
    
    public String getMainLanguage() { return mainLanguage; }
    public void setMainLanguage(String mainLanguage) { this.mainLanguage = mainLanguage; }
    
    public Set<String> getDependencies() { return dependencies; }
    public void setDependencies(Set<String> dependencies) { this.dependencies = dependencies; }
    
    public Set<String> getBuildTargets() { return buildTargets; }
    public void setBuildTargets(Set<String> buildTargets) { this.buildTargets = buildTargets; }
    
    public List<String> getWorkflow() { return workflow; }
    public void setWorkflow(List<String> workflow) { this.workflow = workflow; }
    
    public Map<String, String> getProjectConfiguration() { return projectConfiguration; }
    public void setProjectConfiguration(Map<String, String> projectConfiguration) { this.projectConfiguration = projectConfiguration; }
    
    public ProjectStructure getStructure() { return structure; }
    public void setStructure(ProjectStructure structure) { this.structure = structure; }
    
    public Set<String> getDetectedLanguages() { return detectedLanguages; }
    public void setDetectedLanguages(Set<String> detectedLanguages) { this.detectedLanguages = detectedLanguages; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    /**
     * Check if the project has a specific script type.
     */
    public boolean hasScriptType(ScriptAnalyzer.ScriptType scriptType) {
        return scriptResults.stream()
                .anyMatch(result -> result.getScriptType() == scriptType);
    }
    
    /**
     * Get all script results of a specific type.
     */
    public List<ScriptAnalysisResult> getScriptsByType(ScriptAnalyzer.ScriptType scriptType) {
        return scriptResults.stream()
                .filter(result -> result.getScriptType() == scriptType)
                .collect(Collectors.toList());
    }
    
    /**
     * Get scripts by purpose (build, setup, etc.).
     */
    public List<ScriptAnalysisResult> getScriptsByPurpose(ScriptAnalysisResult.ScriptPurpose purpose) {
        return scriptResults.stream()
                .filter(result -> result.getPurpose() == purpose)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all build scripts in the project.
     */
    public List<ScriptAnalysisResult> getBuildScripts() {
        return scriptResults.stream()
                .filter(ScriptAnalysisResult::isBuildScript)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all dependency management scripts.
     */
    public List<ScriptAnalysisResult> getDependencyScripts() {
        return scriptResults.stream()
                .filter(ScriptAnalysisResult::managesDependencies)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all CI/CD scripts.
     */
    public List<ScriptAnalysisResult> getCiCdScripts() {
        return scriptResults.stream()
                .filter(ScriptAnalysisResult::isCiCdScript)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if the project is multi-language.
     */
    public boolean isMultiLanguage() {
        return detectedLanguages.size() > 1;
    }
    
    /**
     * Get the primary build script (most important one).
     */
    public ScriptAnalysisResult getPrimaryBuildScript() {
        List<ScriptAnalysisResult> buildScripts = getBuildScripts();
        
        if (buildScripts.isEmpty()) {
            return null;
        }
        
        // Prioritize based on project type
        ScriptAnalyzer.ScriptType preferredType = getPreferredBuildScriptType();
        
        return buildScripts.stream()
                .filter(script -> script.getScriptType() == preferredType)
                .findFirst()
                .orElse(buildScripts.get(0)); // Fallback to first build script
    }
    
    private ScriptAnalyzer.ScriptType getPreferredBuildScriptType() {
        switch (projectType) {
            case JAVA: 
                return hasScriptType(ScriptAnalyzer.ScriptType.POM_XML) ? 
                       ScriptAnalyzer.ScriptType.POM_XML : 
                       ScriptAnalyzer.ScriptType.BUILD_GRADLE;
            case CPP: case C_CPP:
                return hasScriptType(ScriptAnalyzer.ScriptType.CMAKE) ? 
                       ScriptAnalyzer.ScriptType.CMAKE : 
                       ScriptAnalyzer.ScriptType.MAKEFILE;
            case NODEJS:
                return ScriptAnalyzer.ScriptType.PACKAGE_JSON;
            case RUST:
                return ScriptAnalyzer.ScriptType.CARGO_TOML;
            case PYTHON:
                return ScriptAnalyzer.ScriptType.SETUP_PY;
            case PHP:
                return ScriptAnalyzer.ScriptType.COMPOSER_JSON;
            case RUBY:
                return ScriptAnalyzer.ScriptType.GEMFILE;
            default:
                return ScriptAnalyzer.ScriptType.MAKEFILE;
        }
    }
    
    /**
     * Get project complexity assessment.
     */
    public ProjectComplexity getComplexity() {
        int totalScripts = scriptResults.size();
        int complexScripts = (int) scriptResults.stream()
                .mapToInt(result -> result.getComplexity().ordinal())
                .sum();
        
        boolean hasMultipleLanguages = detectedLanguages.size() > 1;
        boolean hasCiCd = !getCiCdScripts().isEmpty();
        boolean hasMultipleBuildSystems = getBuildScripts().stream()
                .map(ScriptAnalysisResult::getScriptType)
                .distinct()
                .count() > 1;
        
        int complexityScore = complexScripts + totalScripts;
        if (hasMultipleLanguages) complexityScore += 5;
        if (hasCiCd) complexityScore += 3;
        if (hasMultipleBuildSystems) complexityScore += 4;
        
        if (complexityScore < 10) return ProjectComplexity.SIMPLE;
        else if (complexityScore < 25) return ProjectComplexity.MODERATE;
        else if (complexityScore < 50) return ProjectComplexity.COMPLEX;
        else return ProjectComplexity.ENTERPRISE;
    }
    
    /**
     * Get migration recommendations.
     */
    public List<String> getMigrationRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze current state and suggest improvements
        if (buildSystem == ScriptAnalyzer.BuildSystem.UNKNOWN) {
            recommendations.add("Add a standard build system (Maven, Gradle, CMake, etc.)");
        }
        
        if (getCiCdScripts().isEmpty()) {
            recommendations.add("Consider adding CI/CD pipeline (GitHub Actions, GitLab CI)");
        }
        
        if (getDependencyScripts().isEmpty() && !dependencies.isEmpty()) {
            recommendations.add("Add dependency management configuration");
        }
        
        if (workflow.isEmpty()) {
            recommendations.add("Define clear build and deployment workflow");
        }
        
        return recommendations;
    }
    
    /**
     * Get compatibility assessment for target language.
     */
    public CompatibilityAssessment getCompatibility(String targetLanguage) {
        CompatibilityAssessment assessment = new CompatibilityAssessment();
        
        // Analyze how well the current project structure maps to target language
        assessment.setTargetLanguage(targetLanguage);
        assessment.setCompatibilityScore(calculateCompatibilityScore(targetLanguage));
        assessment.setChallenges(identifyMigrationChallenges(targetLanguage));
        assessment.setRecommendations(getTargetSpecificRecommendations(targetLanguage));
        
        return assessment;
    }
    
    private double calculateCompatibilityScore(String targetLanguage) {
        // Simple scoring algorithm - can be enhanced
        double score = 0.8; // Base compatibility
        
        if (mainLanguage.equalsIgnoreCase(targetLanguage)) {
            score = 1.0; // Same language
        } else if (isLanguageCompatible(mainLanguage, targetLanguage)) {
            score = 0.9; // Highly compatible
        } else if (hasCommonParadigm(mainLanguage, targetLanguage)) {
            score = 0.7; // Moderately compatible
        } else {
            score = 0.5; // Requires significant changes
        }
        
        // Adjust based on project complexity
        if (getComplexity() == ProjectComplexity.ENTERPRISE) {
            score -= 0.1;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    private boolean isLanguageCompatible(String source, String target) {
        // Define compatibility matrix
        Map<String, Set<String>> compatibilityMap = new HashMap<>();
        compatibilityMap.put("Java", Set.of("Kotlin", "Scala", "Groovy"));
        compatibilityMap.put("JavaScript", Set.of("TypeScript", "Node.js"));
        compatibilityMap.put("C", Set.of("C++", "Rust"));
        compatibilityMap.put("C++", Set.of("C", "Rust"));
        compatibilityMap.put("Python", Set.of("Ruby", "JavaScript"));
        
        return compatibilityMap.getOrDefault(source, Collections.emptySet())
                .contains(target);
    }
    
    private boolean hasCommonParadigm(String source, String target) {
        Set<String> oop = Set.of("Java", "C++", "C#", "Python", "Ruby", "JavaScript");
        Set<String> functional = Set.of("Haskell", "Scala", "F#", "Rust");
        Set<String> systems = Set.of("C", "C++", "Rust", "Go");
        
        return (oop.contains(source) && oop.contains(target)) ||
               (functional.contains(source) && functional.contains(target)) ||
               (systems.contains(source) && systems.contains(target));
    }
    
    private List<String> identifyMigrationChallenges(String targetLanguage) {
        List<String> challenges = new ArrayList<>();
        
        if (isMultiLanguage()) {
            challenges.add("Multi-language project requires careful dependency management");
        }
        
        if (getComplexity() == ProjectComplexity.ENTERPRISE) {
            challenges.add("Large enterprise project requires phased migration approach");
        }
        
        if (!getCiCdScripts().isEmpty()) {
            challenges.add("CI/CD pipelines need to be updated for new language toolchain");
        }
        
        return challenges;
    }
    
    private List<String> getTargetSpecificRecommendations(String targetLanguage) {
        List<String> recommendations = new ArrayList<>();
        
        switch (targetLanguage.toLowerCase()) {
            case "java":
                recommendations.add("Consider using Maven or Gradle for dependency management");
                recommendations.add("Use JUnit for testing framework");
                recommendations.add("Consider Spring Boot for web applications");
                break;
            case "python":
                recommendations.add("Use pip and requirements.txt for dependencies");
                recommendations.add("Consider poetry for advanced dependency management");
                recommendations.add("Use pytest for testing");
                break;
            case "javascript":
            case "typescript":
                recommendations.add("Use npm or yarn for package management");
                recommendations.add("Consider webpack or Vite for bundling");
                recommendations.add("Use Jest or Vitest for testing");
                break;
            case "rust":
                recommendations.add("Use Cargo for build and dependency management");
                recommendations.add("Leverage Rust's ownership system for memory safety");
                break;
            case "go":
                recommendations.add("Use go modules for dependency management");
                recommendations.add("Follow Go conventions for project structure");
                break;
        }
        
        return recommendations;
    }
    
    @Override
    public String toString() {
        return String.format("ProjectAnalysisResult{type=%s, buildSystem=%s, mainLanguage='%s', " +
                           "scripts=%d, complexity=%s, languages=%s}",
                projectType, buildSystem, mainLanguage, scriptResults.size(), 
                getComplexity(), detectedLanguages);
    }
    
    public enum ProjectComplexity {
        SIMPLE, MODERATE, COMPLEX, ENTERPRISE
    }
    
    public static class ProjectStructure {
        private boolean hasSourceDir;
        private boolean hasTestDir;
        private boolean hasDocDir;
        private boolean hasConfigDir;
        private boolean hasAssetsDir;
        private List<String> directories;
        
        public ProjectStructure() {
            this.directories = new ArrayList<>();
        }
        
        // Getters and setters
        public boolean isHasSourceDir() { return hasSourceDir; }
        public void setHasSourceDir(boolean hasSourceDir) { this.hasSourceDir = hasSourceDir; }
        
        public boolean isHasTestDir() { return hasTestDir; }
        public void setHasTestDir(boolean hasTestDir) { this.hasTestDir = hasTestDir; }
        
        public boolean isHasDocDir() { return hasDocDir; }
        public void setHasDocDir(boolean hasDocDir) { this.hasDocDir = hasDocDir; }
        
        public boolean isHasConfigDir() { return hasConfigDir; }
        public void setHasConfigDir(boolean hasConfigDir) { this.hasConfigDir = hasConfigDir; }
        
        public boolean isHasAssetsDir() { return hasAssetsDir; }
        public void setHasAssetsDir(boolean hasAssetsDir) { this.hasAssetsDir = hasAssetsDir; }
        
        public List<String> getDirectories() { return directories; }
        public void setDirectories(List<String> directories) { this.directories = directories; }
        public void addDirectory(String directory) { this.directories.add(directory); }
    }
    
    public static class CompatibilityAssessment {
        private String targetLanguage;
        private double compatibilityScore;
        private List<String> challenges;
        private List<String> recommendations;
        
        public CompatibilityAssessment() {
            this.challenges = new ArrayList<>();
            this.recommendations = new ArrayList<>();
        }
        
        // Getters and setters
        public String getTargetLanguage() { return targetLanguage; }
        public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
        
        public double getCompatibilityScore() { return compatibilityScore; }
        public void setCompatibilityScore(double compatibilityScore) { this.compatibilityScore = compatibilityScore; }
        
        public List<String> getChallenges() { return challenges; }
        public void setChallenges(List<String> challenges) { this.challenges = challenges; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}