package com.polytype.migrator.scripts;

import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.core.TranslationOptions;
import com.polytype.migrator.core.Translator;
import com.polytype.migrator.core.TranslationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced translation workflow that integrates script analysis and generation.
 * This workflow:
 * 1. Analyzes all scripts in the source project
 * 2. Performs the code translation
 * 3. Generates appropriate build scripts and tooling for the target language
 * 4. Creates cross-platform scripts for development workflow
 * 5. Handles embedded languages and best-fit recommendations
 */
public class ScriptAwareTranslationWorkflow {
    
    private final ScriptAnalyzer scriptAnalyzer;
    private final CrossPlatformScriptGenerator scriptGenerator;
    private final Translator coreTranslator;
    private final EmbeddedLanguageDetector embeddedLanguageDetector;
    
    public ScriptAwareTranslationWorkflow(Translator coreTranslator) {
        this.coreTranslator = coreTranslator;
        this.scriptAnalyzer = new ScriptAnalyzer();
        this.embeddedLanguageDetector = new EmbeddedLanguageDetector();
        // scriptGenerator will be initialized after target language is determined
        this.scriptGenerator = null;
    }
    
    /**
     * Execute the complete script-aware translation workflow.
     */
    public ScriptAwareTranslationResult translateProject(String sourcePath, 
                                                        TargetLanguage targetLanguage,
                                                        TranslationOptions options) throws IOException, TranslationException {
        
        ScriptAwareTranslationResult result = new ScriptAwareTranslationResult();
        Path sourceDir = Paths.get(sourcePath);
        
        // Step 1: Analyze all scripts in the source project
        System.out.println("Analyzing project scripts...");
        ProjectAnalysisResult projectAnalysis = scriptAnalyzer.analyzeProject(sourcePath);
        result.setProjectAnalysis(projectAnalysis);
        
        // Step 2: Detect embedded languages and determine best practices
        System.out.println("Detecting embedded languages and analyzing dependencies...");
        EmbeddedLanguageAnalysis embeddedAnalysis = embeddedLanguageDetector.analyzeEmbeddedLanguages(projectAnalysis);
        result.setEmbeddedAnalysis(embeddedAnalysis);
        
        // Step 3: Determine if target language is suitable or recommend alternatives
        LanguageCompatibilityReport compatibilityReport = assessLanguageCompatibility(projectAnalysis, targetLanguage, embeddedAnalysis);
        result.setCompatibilityReport(compatibilityReport);
        
        // Step 4: If there are compatibility issues, suggest alternatives or adaptations
        if (compatibilityReport.hasIssues()) {
            System.out.println("Compatibility issues detected:");
            for (String issue : compatibilityReport.getIssues()) {
                System.out.println("  - " + issue);
            }
            
            if (compatibilityReport.hasCriticalIssues()) {
                System.out.println("Recommending alternative target language: " + compatibilityReport.getRecommendedLanguage());
                targetLanguage = compatibilityReport.getRecommendedLanguage();
            }
        }
        
        // Step 5: Perform the core translation
        System.out.println("Translating source code to " + targetLanguage + "...");
        // Note: This integrates with the existing translation system
        TranslationResult coreResult = coreTranslator.translate(sourceDir, targetLanguage, options);
        result.setCoreTranslationResult(coreResult);
        
        // Step 6: Generate scripts for the target language and platform
        System.out.println("Generating cross-platform scripts...");
        CrossPlatformScriptGenerator generator = new CrossPlatformScriptGenerator(projectAnalysis, targetLanguage);
        Map<String, String> generatedScripts = generator.generateAllScripts();
        result.setGeneratedScripts(generatedScripts);
        
        // Step 7: Generate build system configuration for target language
        System.out.println("Generating build system configuration...");
        BuildSystemGenerator buildGenerator = new BuildSystemGenerator(projectAnalysis, targetLanguage);
        Map<String, String> buildConfig = buildGenerator.generateBuildConfiguration();
        result.setBuildConfiguration(buildConfig);
        
        // Step 8: Create project structure recommendations
        System.out.println("Creating project structure recommendations...");
        ProjectStructureRecommendation structureRec = generateStructureRecommendations(projectAnalysis, targetLanguage, embeddedAnalysis);
        result.setStructureRecommendations(structureRec);
        
        // Step 9: Generate migration guide
        System.out.println("Generating migration guide...");
        MigrationGuide migrationGuide = generateMigrationGuide(projectAnalysis, targetLanguage, embeddedAnalysis, compatibilityReport);
        result.setMigrationGuide(migrationGuide);
        
        return result;
    }
    
    /**
     * Write all generated scripts and configuration to the output directory.
     */
    public void writeGeneratedFiles(ScriptAwareTranslationResult result, String outputPath) throws IOException {
        Path outputDir = Paths.get(outputPath);
        Files.createDirectories(outputDir);
        
        // Write generated scripts
        Map<String, String> scripts = result.getGeneratedScripts();
        for (Map.Entry<String, String> script : scripts.entrySet()) {
            Path scriptPath = outputDir.resolve(script.getKey());
            Files.createDirectories(scriptPath.getParent());
            Files.write(scriptPath, script.getValue().getBytes());
            
            // Make shell scripts executable on Unix systems
            if (script.getKey().endsWith(".sh")) {
                try {
                    scriptPath.toFile().setExecutable(true);
                } catch (Exception e) {
                    System.out.println("Could not make script executable: " + script.getKey());
                }
            }
        }
        
        // Write build configuration
        Map<String, String> buildConfig = result.getBuildConfiguration();
        for (Map.Entry<String, String> config : buildConfig.entrySet()) {
            Path configPath = outputDir.resolve(config.getKey());
            Files.createDirectories(configPath.getParent());
            Files.write(configPath, config.getValue().getBytes());
        }
        
        // Write migration guide
        if (result.getMigrationGuide() != null) {
            Path migrationPath = outputDir.resolve("MIGRATION.md");
            Files.write(migrationPath, result.getMigrationGuide().generateMarkdown().getBytes());
        }
        
        // Write project structure recommendations
        if (result.getStructureRecommendations() != null) {
            Path structurePath = outputDir.resolve("PROJECT_STRUCTURE.md");
            Files.write(structurePath, result.getStructureRecommendations().generateMarkdown().getBytes());
        }
        
        System.out.println("Generated files written to: " + outputPath);
    }
    
    private LanguageCompatibilityReport assessLanguageCompatibility(ProjectAnalysisResult projectAnalysis,
                                                                  TargetLanguage targetLanguage,
                                                                  EmbeddedLanguageAnalysis embeddedAnalysis) {
        LanguageCompatibilityReport report = new LanguageCompatibilityReport();
        report.setTargetLanguage(targetLanguage);
        
        // Assess embedded languages compatibility
        for (String embeddedLang : embeddedAnalysis.getDetectedLanguages()) {
            if (!isEmbeddedLanguageCompatible(embeddedLang, targetLanguage)) {
                report.addIssue("Embedded " + embeddedLang + " may not be directly compatible with " + targetLanguage);
                report.addRecommendation("Consider using " + suggestAlternativeForEmbeddedLanguage(embeddedLang, targetLanguage));
            }
        }
        
        // Assess build system compatibility
        ScriptAnalyzer.BuildSystem currentBuildSystem = projectAnalysis.getBuildSystem();
        if (!isBuildSystemCompatible(currentBuildSystem, targetLanguage)) {
            report.addIssue("Current build system (" + currentBuildSystem + ") not directly compatible with " + targetLanguage);
            report.addRecommendation("Will generate " + getRecommendedBuildSystem(targetLanguage) + " configuration");
        }
        
        // Assess dependency ecosystem
        if (hasDependencyEcosystemIssues(projectAnalysis.getDependencies(), targetLanguage)) {
            report.addIssue("Some dependencies may not have direct equivalents in " + targetLanguage + " ecosystem");
            report.addRecommendation("Review generated dependency mapping file");
        }
        
        // Determine if critical issues exist that require language change
        if (report.getIssues().size() > 3 && embeddedAnalysis.hasCriticalIncompatibilities(targetLanguage)) {
            report.setCritical(true);
            report.setRecommendedLanguage(suggestBestFitLanguage(projectAnalysis, embeddedAnalysis));
        }
        
        return report;
    }
    
    private ProjectStructureRecommendation generateStructureRecommendations(ProjectAnalysisResult projectAnalysis,
                                                                           TargetLanguage targetLanguage,
                                                                           EmbeddedLanguageAnalysis embeddedAnalysis) {
        ProjectStructureRecommendation recommendation = new ProjectStructureRecommendation();
        
        // Base structure for target language
        switch (targetLanguage) {
            case JAVA:
                recommendation.addDirectory("src/main/java");
                recommendation.addDirectory("src/test/java");
                recommendation.addDirectory("src/main/resources");
                recommendation.addFile("pom.xml", "Maven build configuration");
                recommendation.addFile("build.gradle", "Alternative Gradle build configuration");
                break;
            case PYTHON:
                recommendation.addDirectory("src");
                recommendation.addDirectory("tests");
                recommendation.addFile("setup.py", "Package setup script");
                recommendation.addFile("requirements.txt", "Dependencies");
                recommendation.addFile("pyproject.toml", "Modern Python project configuration");
                break;
            case JAVASCRIPT:
                recommendation.addDirectory("src");
                recommendation.addDirectory("test");
                recommendation.addFile("package.json", "Node.js package configuration");
                recommendation.addFile("webpack.config.js", "Build configuration");
                break;
            case RUST:
                recommendation.addDirectory("src");
                recommendation.addDirectory("tests");
                recommendation.addFile("Cargo.toml", "Rust package configuration");
                break;
            case CPP:
                recommendation.addDirectory("src");
                recommendation.addDirectory("include");
                recommendation.addDirectory("tests");
                recommendation.addFile("CMakeLists.txt", "CMake build configuration");
                recommendation.addFile("Makefile", "Alternative Make configuration");
                break;
        }
        
        // Add embedded language considerations
        for (String embeddedLang : embeddedAnalysis.getDetectedLanguages()) {
            addEmbeddedLanguageStructure(recommendation, embeddedLang, targetLanguage);
        }
        
        return recommendation;
    }
    
    private void addEmbeddedLanguageStructure(ProjectStructureRecommendation recommendation,
                                            String embeddedLang, TargetLanguage targetLanguage) {
        switch (embeddedLang.toLowerCase()) {
            case "shell":
                recommendation.addDirectory("scripts");
                recommendation.addFile("scripts/build.sh", "Unix build script");
                recommendation.addFile("scripts/build.bat", "Windows build script");
                break;
            case "sql":
                recommendation.addDirectory("resources/sql");
                recommendation.addFile("resources/sql/schema.sql", "Database schema");
                break;
            case "javascript":
                if (targetLanguage != TargetLanguage.JAVASCRIPT) {
                    recommendation.addDirectory("web");
                    recommendation.addFile("web/package.json", "Frontend dependencies");
                }
                break;
        }
    }
    
    private MigrationGuide generateMigrationGuide(ProjectAnalysisResult projectAnalysis,
                                                TargetLanguage targetLanguage,
                                                EmbeddedLanguageAnalysis embeddedAnalysis,
                                                LanguageCompatibilityReport compatibilityReport) {
        MigrationGuide guide = new MigrationGuide();
        guide.setSourceLanguage(projectAnalysis.getMainLanguage());
        guide.setTargetLanguage(targetLanguage);
        
        // Add build system migration steps
        guide.addSection("Build System Migration",
            "Your project will be migrated from " + projectAnalysis.getBuildSystem() +
            " to " + getRecommendedBuildSystem(targetLanguage) + ".");
        
        // Add dependency migration information
        if (!projectAnalysis.getDependencies().isEmpty()) {
            guide.addSection("Dependencies", 
                "The following dependencies need to be mapped to " + targetLanguage + " equivalents:");
            for (String dep : projectAnalysis.getDependencies()) {
                String equivalent = findDependencyEquivalent(dep, targetLanguage);
                guide.addDependencyMapping(dep, equivalent);
            }
        }
        
        // Add embedded language handling
        for (String embeddedLang : embeddedAnalysis.getDetectedLanguages()) {
            guide.addSection("Embedded " + embeddedLang,
                "Handle " + embeddedLang + " integration with " + targetLanguage);
        }
        
        // Add compatibility issues and solutions
        if (compatibilityReport.hasIssues()) {
            guide.addSection("Compatibility Considerations",
                "The following issues were identified and addressed:");
            for (String issue : compatibilityReport.getIssues()) {
                guide.addIssue(issue);
            }
        }
        
        return guide;
    }
    
    // Helper methods with simplified implementations
    private boolean isEmbeddedLanguageCompatible(String embeddedLang, TargetLanguage targetLang) {
        // Most languages can embed shell scripts, SQL, etc.
        return !embeddedLang.equals("JavaScript") || targetLang == TargetLanguage.JAVASCRIPT;
    }
    
    private String suggestAlternativeForEmbeddedLanguage(String embeddedLang, TargetLanguage targetLang) {
        if (embeddedLang.equals("JavaScript") && targetLang == TargetLanguage.JAVA) {
            return "Java-based scripting (Nashorn/GraalVM)";
        }
        return "Native " + targetLang + " equivalent";
    }
    
    private boolean isBuildSystemCompatible(ScriptAnalyzer.BuildSystem buildSystem, TargetLanguage targetLang) {
        switch (targetLang) {
            case JAVA: return buildSystem == ScriptAnalyzer.BuildSystem.MAVEN || buildSystem == ScriptAnalyzer.BuildSystem.GRADLE;
            case JAVASCRIPT: return buildSystem == ScriptAnalyzer.BuildSystem.NPM;
            case RUST: return buildSystem == ScriptAnalyzer.BuildSystem.CARGO;
            default: return false;
        }
    }
    
    private String getRecommendedBuildSystem(TargetLanguage targetLang) {
        switch (targetLang) {
            case JAVA: return "Maven";
            case JAVASCRIPT: return "npm";
            case RUST: return "Cargo";
            case PYTHON: return "setuptools/pip";
            case CPP: return "CMake";
            default: return "Make";
        }
    }
    
    private boolean hasDependencyEcosystemIssues(Set<String> dependencies, TargetLanguage targetLang) {
        // Simplified check - in practice, this would use a comprehensive mapping database
        return !dependencies.isEmpty();
    }
    
    private TargetLanguage suggestBestFitLanguage(ProjectAnalysisResult projectAnalysis, EmbeddedLanguageAnalysis embeddedAnalysis) {
        // Simplified best-fit algorithm
        if (embeddedAnalysis.getDetectedLanguages().contains("JavaScript")) {
            return TargetLanguage.JAVASCRIPT;
        }
        if (projectAnalysis.getProjectType() == ScriptAnalyzer.ProjectType.CPP) {
            return TargetLanguage.CPP;
        }
        return TargetLanguage.JAVA; // Default fallback
    }
    
    private String findDependencyEquivalent(String dependency, TargetLanguage targetLang) {
        // Simplified mapping - in practice, this would use a comprehensive database
        switch (targetLang) {
            case JAVA:
                if (dependency.contains("requests")) return "Apache HttpClient";
                if (dependency.contains("json")) return "Jackson";
                break;
            case JAVASCRIPT:
                if (dependency.contains("http")) return "axios";
                break;
        }
        return "Manual review required";
    }
    
    // Placeholder classes for results and components
    public static class TranslationResult {
        // Represents the result of core code translation
    }
}