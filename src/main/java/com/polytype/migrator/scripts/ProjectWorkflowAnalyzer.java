package com.polytype.migrator.scripts;

import com.polytype.migrator.core.TargetLanguage;
import java.util.*;

/**
 * Analyzes project workflows and creates migration strategies.
 * Determines the optimal sequence of operations for project migration.
 */
public class ProjectWorkflowAnalyzer {
    
    private final ProjectAnalysisResult projectAnalysis;
    
    public ProjectWorkflowAnalyzer(ProjectAnalysisResult projectAnalysis) {
        this.projectAnalysis = projectAnalysis;
    }
    
    /**
     * Create a complete migration workflow for the target language.
     */
    public MigrationWorkflow createMigrationWorkflow(TargetLanguage targetLanguage) {
        MigrationWorkflow workflow = new MigrationWorkflow();
        workflow.setTargetLanguage(targetLanguage);
        workflow.setSourceProject(projectAnalysis);
        
        // Phase 1: Pre-migration analysis and setup
        workflow.addPhase(createPreMigrationPhase());
        
        // Phase 2: Core code migration
        workflow.addPhase(createCodeMigrationPhase(targetLanguage));
        
        // Phase 3: Build system migration
        workflow.addPhase(createBuildSystemMigrationPhase(targetLanguage));
        
        // Phase 4: Testing and validation
        workflow.addPhase(createTestingPhase(targetLanguage));
        
        // Phase 5: Post-migration optimization
        workflow.addPhase(createPostMigrationPhase(targetLanguage));
        
        // Calculate estimated time and complexity
        workflow.calculateEstimates();
        
        return workflow;
    }
    
    private MigrationPhase createPreMigrationPhase() {
        MigrationPhase phase = new MigrationPhase("Pre-Migration Setup", 1);
        
        phase.addTask(new MigrationTask("Backup original project", "Create complete backup of source code", 15));
        phase.addTask(new MigrationTask("Analyze dependencies", "Map all external dependencies to target equivalents", 30));
        phase.addTask(new MigrationTask("Set up target environment", "Install and configure target language tools", 45));
        phase.addTask(new MigrationTask("Create project structure", "Set up target project directory structure", 20));
        
        return phase;
    }
    
    private MigrationPhase createCodeMigrationPhase(TargetLanguage targetLanguage) {
        MigrationPhase phase = new MigrationPhase("Code Migration", 2);
        
        // Estimate based on project complexity
        int baseTime = calculateCodeMigrationTime();
        
        phase.addTask(new MigrationTask("Parse source files", "Analyze and parse all source code files", baseTime / 4));
        phase.addTask(new MigrationTask("Generate target code", "Translate source code to " + targetLanguage, baseTime / 2));
        phase.addTask(new MigrationTask("Handle edge cases", "Address language-specific conversion issues", baseTime / 4));
        
        return phase;
    }
    
    private MigrationPhase createBuildSystemMigrationPhase(TargetLanguage targetLanguage) {
        MigrationPhase phase = new MigrationPhase("Build System Migration", 3);
        
        phase.addTask(new MigrationTask("Generate build scripts", "Create target language build configuration", 60));
        phase.addTask(new MigrationTask("Migrate dependencies", "Convert dependency declarations", 45));
        phase.addTask(new MigrationTask("Create cross-platform scripts", "Generate .sh and .bat scripts", 30));
        phase.addTask(new MigrationTask("Set up CI/CD", "Update continuous integration configuration", 90));
        
        return phase;
    }
    
    private MigrationPhase createTestingPhase(TargetLanguage targetLanguage) {
        MigrationPhase phase = new MigrationPhase("Testing & Validation", 4);
        
        phase.addTask(new MigrationTask("Compile/build project", "Verify project builds successfully", 30));
        phase.addTask(new MigrationTask("Run automated tests", "Execute test suite and verify results", 60));
        phase.addTask(new MigrationTask("Manual testing", "Perform functional testing", 120));
        phase.addTask(new MigrationTask("Performance testing", "Compare performance with original", 90));
        
        return phase;
    }
    
    private MigrationPhase createPostMigrationPhase(TargetLanguage targetLanguage) {
        MigrationPhase phase = new MigrationPhase("Post-Migration Optimization", 5);
        
        phase.addTask(new MigrationTask("Code optimization", "Apply target language best practices", 90));
        phase.addTask(new MigrationTask("Documentation update", "Update README and technical docs", 60));
        phase.addTask(new MigrationTask("Security review", "Perform security analysis", 45));
        phase.addTask(new MigrationTask("Final validation", "Complete end-to-end testing", 60));
        
        return phase;
    }
    
    private int calculateCodeMigrationTime() {
        int baseTime = 120; // Base 2 hours
        
        // Adjust based on project complexity
        switch (projectAnalysis.getComplexity()) {
            case SIMPLE: return baseTime;
            case MODERATE: return baseTime * 2;
            case COMPLEX: return baseTime * 4;
            case ENTERPRISE: return baseTime * 8;
            default: return baseTime;
        }
    }
    
    /**
     * Represents a complete migration workflow.
     */
    public static class MigrationWorkflow {
        private TargetLanguage targetLanguage;
        private ProjectAnalysisResult sourceProject;
        private List<MigrationPhase> phases;
        private int estimatedTimeMinutes;
        private String complexity;
        
        public MigrationWorkflow() {
            this.phases = new ArrayList<>();
        }
        
        public void addPhase(MigrationPhase phase) {
            phases.add(phase);
        }
        
        public void calculateEstimates() {
            estimatedTimeMinutes = phases.stream()
                    .mapToInt(MigrationPhase::getTotalTimeMinutes)
                    .sum();
            
            complexity = sourceProject.getComplexity().toString();
        }
        
        // Getters and setters
        public TargetLanguage getTargetLanguage() { return targetLanguage; }
        public void setTargetLanguage(TargetLanguage targetLanguage) { this.targetLanguage = targetLanguage; }
        
        public ProjectAnalysisResult getSourceProject() { return sourceProject; }
        public void setSourceProject(ProjectAnalysisResult sourceProject) { this.sourceProject = sourceProject; }
        
        public List<MigrationPhase> getPhases() { return phases; }
        
        public int getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
        public String getEstimatedTimeFormatted() {
            int hours = estimatedTimeMinutes / 60;
            int minutes = estimatedTimeMinutes % 60;
            return String.format("%d hours %d minutes", hours, minutes);
        }
        
        public String getComplexity() { return complexity; }
    }
    
    /**
     * Represents a phase in the migration workflow.
     */
    public static class MigrationPhase {
        private String name;
        private int phaseNumber;
        private List<MigrationTask> tasks;
        
        public MigrationPhase(String name, int phaseNumber) {
            this.name = name;
            this.phaseNumber = phaseNumber;
            this.tasks = new ArrayList<>();
        }
        
        public void addTask(MigrationTask task) {
            tasks.add(task);
        }
        
        public int getTotalTimeMinutes() {
            return tasks.stream().mapToInt(MigrationTask::getEstimatedTimeMinutes).sum();
        }
        
        // Getters
        public String getName() { return name; }
        public int getPhaseNumber() { return phaseNumber; }
        public List<MigrationTask> getTasks() { return tasks; }
    }
    
    /**
     * Represents a single task in the migration workflow.
     */
    public static class MigrationTask {
        private String name;
        private String description;
        private int estimatedTimeMinutes;
        private TaskStatus status;
        
        public MigrationTask(String name, String description, int estimatedTimeMinutes) {
            this.name = name;
            this.description = description;
            this.estimatedTimeMinutes = estimatedTimeMinutes;
            this.status = TaskStatus.PENDING;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        
        public enum TaskStatus {
            PENDING, IN_PROGRESS, COMPLETED, FAILED
        }
    }
}