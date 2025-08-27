package com.polytype.migrator.scripts;

import com.polytype.migrator.core.TargetLanguage;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects and analyzes embedded languages within a project.
 * Helps determine the best target language and necessary adaptations.
 */
public class EmbeddedLanguageDetector {
    
    public EmbeddedLanguageAnalysis analyzeEmbeddedLanguages(ProjectAnalysisResult projectAnalysis) {
        EmbeddedLanguageAnalysis analysis = new EmbeddedLanguageAnalysis();
        
        // Collect all embedded languages from script results
        Set<String> allEmbeddedLanguages = new HashSet<>();
        Map<String, Integer> languageFrequency = new HashMap<>();
        Map<String, Set<String>> languageUseCases = new HashMap<>();
        
        for (ScriptAnalysisResult scriptResult : projectAnalysis.getScriptResults()) {
            for (String embeddedLang : scriptResult.getEmbeddedLanguages()) {
                allEmbeddedLanguages.add(embeddedLang);
                languageFrequency.put(embeddedLang, languageFrequency.getOrDefault(embeddedLang, 0) + 1);
                
                // Track use cases
                languageUseCases.computeIfAbsent(embeddedLang, k -> new HashSet<>())
                    .add(scriptResult.getPurpose().toString());
            }
        }
        
        analysis.setDetectedLanguages(allEmbeddedLanguages);
        analysis.setLanguageFrequency(languageFrequency);
        analysis.setLanguageUseCases(languageUseCases);
        
        // Analyze language relationships and dependencies
        analyzeLanguageRelationships(analysis, projectAnalysis);
        
        // Determine critical languages
        determineCriticalLanguages(analysis);
        
        // Generate compatibility matrix
        generateCompatibilityMatrix(analysis);
        
        return analysis;
    }
    
    private void analyzeLanguageRelationships(EmbeddedLanguageAnalysis analysis, ProjectAnalysisResult projectAnalysis) {
        // Analyze which languages commonly appear together
        Map<String, Set<String>> coOccurrences = new HashMap<>();
        
        for (ScriptAnalysisResult scriptResult : projectAnalysis.getScriptResults()) {
            List<String> languages = scriptResult.getEmbeddedLanguages();
            
            for (int i = 0; i < languages.size(); i++) {
                for (int j = i + 1; j < languages.size(); j++) {
                    String lang1 = languages.get(i);
                    String lang2 = languages.get(j);
                    
                    coOccurrences.computeIfAbsent(lang1, k -> new HashSet<>()).add(lang2);
                    coOccurrences.computeIfAbsent(lang2, k -> new HashSet<>()).add(lang1);
                }
            }
        }
        
        analysis.setLanguageCoOccurrences(coOccurrences);
    }
    
    private void determineCriticalLanguages(EmbeddedLanguageAnalysis analysis) {
        Set<String> criticalLanguages = new HashSet<>();
        
        for (Map.Entry<String, Integer> entry : analysis.getLanguageFrequency().entrySet()) {
            String language = entry.getKey();
            int frequency = entry.getValue();
            
            // A language is critical if it appears frequently or in build-related contexts
            if (frequency >= 3 || isBuildCriticalLanguage(language, analysis)) {
                criticalLanguages.add(language);
            }
        }
        
        analysis.setCriticalLanguages(criticalLanguages);
    }
    
    private boolean isBuildCriticalLanguage(String language, EmbeddedLanguageAnalysis analysis) {
        Set<String> useCases = analysis.getLanguageUseCases().get(language);
        if (useCases == null) return false;
        
        return useCases.contains("BUILD") || 
               useCases.contains("SETUP") || 
               useCases.contains("DEPLOYMENT") ||
               useCases.contains("CICD");
    }
    
    private void generateCompatibilityMatrix(EmbeddedLanguageAnalysis analysis) {
        Map<TargetLanguage, CompatibilityScore> compatibilityMatrix = new HashMap<>();
        
        for (TargetLanguage targetLang : TargetLanguage.values()) {
            CompatibilityScore score = calculateCompatibilityScore(analysis, targetLang);
            compatibilityMatrix.put(targetLang, score);
        }
        
        analysis.setCompatibilityMatrix(compatibilityMatrix);
    }
    
    private CompatibilityScore calculateCompatibilityScore(EmbeddedLanguageAnalysis analysis, TargetLanguage targetLang) {
        CompatibilityScore score = new CompatibilityScore();
        int totalScore = 0;
        int maxScore = 0;
        
        for (String embeddedLang : analysis.getDetectedLanguages()) {
            int weight = analysis.getLanguageFrequency().get(embeddedLang);
            maxScore += weight * 10; // Perfect compatibility = 10 points per occurrence
            
            int compatibility = getLanguageCompatibility(embeddedLang, targetLang);
            totalScore += weight * compatibility;
            
            if (compatibility < 7) { // Less than 70% compatibility
                score.addIncompatibility(embeddedLang, getIncompatibilityReason(embeddedLang, targetLang));
            }
        }
        
        double compatibilityPercentage = maxScore > 0 ? (double) totalScore / maxScore : 1.0;
        score.setOverallScore(compatibilityPercentage);
        
        return score;
    }
    
    private int getLanguageCompatibility(String embeddedLang, TargetLanguage targetLang) {
        // Compatibility scoring (0-10 scale)
        switch (embeddedLang.toLowerCase()) {
            case "shell":
            case "bash":
                return targetLang == TargetLanguage.PYTHON ? 9 : 8; // Most languages can call shell
                
            case "sql":
                switch (targetLang) {
                    case JAVA: return 10; // JDBC
                    case PYTHON: return 10; // Multiple DB libraries
                    case JAVASCRIPT: return 8; // Node.js DB drivers
                    case RUST: return 7; // Diesel, SQLx
                    case CPP: return 6; // Native DB libraries
                    default: return 5;
                }
                
            case "javascript":
                switch (targetLang) {
                    case JAVASCRIPT: return 10; // Perfect match
                    case JAVA: return 6; // GraalVM, Nashorn
                    case PYTHON: return 5; // PyV8, node integration
                    default: return 3;
                }
                
            case "python":
                switch (targetLang) {
                    case PYTHON: return 10; // Perfect match
                    case JAVA: return 7; // Jython, process calls
                    case JAVASCRIPT: return 5; // Python-shell, Pyodide
                    default: return 4;
                }
                
            case "c":
            case "c++":
                switch (targetLang) {
                    case CPP: return 10; // Perfect match
                    case JAVA: return 6; // JNI
                    case PYTHON: return 8; // C extensions, Cython
                    case RUST: return 7; // FFI, bindgen
                    default: return 4;
                }
                
            case "java":
                switch (targetLang) {
                    case JAVA: return 10; // Perfect match
                    case JAVASCRIPT: return 4; // Java->JS transpilation exists
                    case PYTHON: return 5; // Jython
                    default: return 3;
                }
                
            default:
                return 5; // Neutral compatibility
        }
    }
    
    private String getIncompatibilityReason(String embeddedLang, TargetLanguage targetLang) {
        if (embeddedLang.equalsIgnoreCase("javascript") && targetLang != TargetLanguage.JAVASCRIPT) {
            return "JavaScript embedding requires additional runtime (GraalVM, V8, etc.)";
        }
        
        if (embeddedLang.equalsIgnoreCase("python") && targetLang != TargetLanguage.PYTHON) {
            return "Python embedding requires process calls or specialized runtimes";
        }
        
        if ((embeddedLang.equalsIgnoreCase("c") || embeddedLang.equalsIgnoreCase("c++")) && 
            targetLang == TargetLanguage.JAVASCRIPT) {
            return "C/C++ integration with JavaScript requires WASM or Node.js addons";
        }
        
        return "Integration requires additional tooling or runtime support";
    }
    
    /**
     * Analysis result containing information about embedded languages.
     */
    public static class EmbeddedLanguageAnalysis {
        private Set<String> detectedLanguages = new HashSet<>();
        private Map<String, Integer> languageFrequency = new HashMap<>();
        private Map<String, Set<String>> languageUseCases = new HashMap<>();
        private Map<String, Set<String>> languageCoOccurrences = new HashMap<>();
        private Set<String> criticalLanguages = new HashSet<>();
        private Map<TargetLanguage, CompatibilityScore> compatibilityMatrix = new HashMap<>();
        
        // Getters and setters
        public Set<String> getDetectedLanguages() { return detectedLanguages; }
        public void setDetectedLanguages(Set<String> detectedLanguages) { this.detectedLanguages = detectedLanguages; }
        
        public Map<String, Integer> getLanguageFrequency() { return languageFrequency; }
        public void setLanguageFrequency(Map<String, Integer> languageFrequency) { this.languageFrequency = languageFrequency; }
        
        public Map<String, Set<String>> getLanguageUseCases() { return languageUseCases; }
        public void setLanguageUseCases(Map<String, Set<String>> languageUseCases) { this.languageUseCases = languageUseCases; }
        
        public Map<String, Set<String>> getLanguageCoOccurrences() { return languageCoOccurrences; }
        public void setLanguageCoOccurrences(Map<String, Set<String>> languageCoOccurrences) { this.languageCoOccurrences = languageCoOccurrences; }
        
        public Set<String> getCriticalLanguages() { return criticalLanguages; }
        public void setCriticalLanguages(Set<String> criticalLanguages) { this.criticalLanguages = criticalLanguages; }
        
        public Map<TargetLanguage, CompatibilityScore> getCompatibilityMatrix() { return compatibilityMatrix; }
        public void setCompatibilityMatrix(Map<TargetLanguage, CompatibilityScore> compatibilityMatrix) { this.compatibilityMatrix = compatibilityMatrix; }
        
        public boolean hasCriticalIncompatibilities(TargetLanguage targetLang) {
            CompatibilityScore score = compatibilityMatrix.get(targetLang);
            return score != null && score.getOverallScore() < 0.6; // Less than 60% compatible
        }
        
        public TargetLanguage getBestCompatibleLanguage() {
            return compatibilityMatrix.entrySet().stream()
                .max(Map.Entry.comparingByValue((s1, s2) -> Double.compare(s1.getOverallScore(), s2.getOverallScore())))
                .map(Map.Entry::getKey)
                .orElse(TargetLanguage.JAVA);
        }
    }
    
    /**
     * Represents compatibility between embedded languages and a target language.
     */
    public static class CompatibilityScore {
        private double overallScore;
        private Map<String, String> incompatibilities = new HashMap<>();
        
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        
        public Map<String, String> getIncompatibilities() { return incompatibilities; }
        public void addIncompatibility(String language, String reason) {
            incompatibilities.put(language, reason);
        }
        
        public boolean hasIncompatibilities() { return !incompatibilities.isEmpty(); }
    }
}