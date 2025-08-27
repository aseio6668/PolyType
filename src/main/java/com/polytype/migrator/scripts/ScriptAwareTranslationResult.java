package com.polytype.migrator.scripts;

import com.polytype.migrator.scripts.EmbeddedLanguageDetector.EmbeddedLanguageAnalysis;
import java.util.Map;

/**
 * Complete result of script-aware translation workflow.
 * Contains all analysis, translation results, and generated assets.
 */
public class ScriptAwareTranslationResult {
    
    private ProjectAnalysisResult projectAnalysis;
    private EmbeddedLanguageAnalysis embeddedAnalysis;
    private LanguageCompatibilityReport compatibilityReport;
    private ScriptAwareTranslationWorkflow.TranslationResult coreTranslationResult;
    private Map<String, String> generatedScripts;
    private Map<String, String> buildConfiguration;
    private ProjectStructureRecommendation structureRecommendations;
    private MigrationGuide migrationGuide;
    
    // Getters and setters
    public ProjectAnalysisResult getProjectAnalysis() { return projectAnalysis; }
    public void setProjectAnalysis(ProjectAnalysisResult projectAnalysis) { this.projectAnalysis = projectAnalysis; }
    
    public EmbeddedLanguageAnalysis getEmbeddedAnalysis() { return embeddedAnalysis; }
    public void setEmbeddedAnalysis(EmbeddedLanguageAnalysis embeddedAnalysis) { this.embeddedAnalysis = embeddedAnalysis; }
    
    public LanguageCompatibilityReport getCompatibilityReport() { return compatibilityReport; }
    public void setCompatibilityReport(LanguageCompatibilityReport compatibilityReport) { this.compatibilityReport = compatibilityReport; }
    
    public ScriptAwareTranslationWorkflow.TranslationResult getCoreTranslationResult() { return coreTranslationResult; }
    public void setCoreTranslationResult(ScriptAwareTranslationWorkflow.TranslationResult coreTranslationResult) { this.coreTranslationResult = coreTranslationResult; }
    
    public Map<String, String> getGeneratedScripts() { return generatedScripts; }
    public void setGeneratedScripts(Map<String, String> generatedScripts) { this.generatedScripts = generatedScripts; }
    
    public Map<String, String> getBuildConfiguration() { return buildConfiguration; }
    public void setBuildConfiguration(Map<String, String> buildConfiguration) { this.buildConfiguration = buildConfiguration; }
    
    public ProjectStructureRecommendation getStructureRecommendations() { return structureRecommendations; }
    public void setStructureRecommendations(ProjectStructureRecommendation structureRecommendations) { this.structureRecommendations = structureRecommendations; }
    
    public MigrationGuide getMigrationGuide() { return migrationGuide; }
    public void setMigrationGuide(MigrationGuide migrationGuide) { this.migrationGuide = migrationGuide; }
}