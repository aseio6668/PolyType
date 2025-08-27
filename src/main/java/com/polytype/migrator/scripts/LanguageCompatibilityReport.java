package com.polytype.migrator.scripts;

import com.polytype.migrator.core.TargetLanguage;
import java.util.*;

/**
 * Report on language compatibility issues and recommendations.
 */
public class LanguageCompatibilityReport {
    
    private TargetLanguage targetLanguage;
    private List<String> issues;
    private List<String> recommendations;
    private boolean critical;
    private TargetLanguage recommendedLanguage;
    private Map<String, String> issueDetails;
    
    public LanguageCompatibilityReport() {
        this.issues = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.issueDetails = new HashMap<>();
        this.critical = false;
    }
    
    // Getters and setters
    public TargetLanguage getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(TargetLanguage targetLanguage) { this.targetLanguage = targetLanguage; }
    
    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }
    public void addIssue(String issue) { this.issues.add(issue); }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public void addRecommendation(String recommendation) { this.recommendations.add(recommendation); }
    
    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }
    
    public TargetLanguage getRecommendedLanguage() { return recommendedLanguage; }
    public void setRecommendedLanguage(TargetLanguage recommendedLanguage) { this.recommendedLanguage = recommendedLanguage; }
    
    public Map<String, String> getIssueDetails() { return issueDetails; }
    public void setIssueDetails(Map<String, String> issueDetails) { this.issueDetails = issueDetails; }
    
    // Helper methods
    public boolean hasIssues() { return !issues.isEmpty(); }
    public boolean hasCriticalIssues() { return critical; }
}