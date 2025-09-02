package com.polytype.migrator.binary;

import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.core.TranslationOptions;
import com.polytype.migrator.core.Translator;
import com.polytype.migrator.core.TranslationException;
import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Enhanced translation workflow that can handle both source code projects
 * and binary deobfuscation/reverse engineering tasks.
 * 
 * This workflow extends the ScriptAwareTranslationWorkflow to support:
 * 1. Binary file analysis (PE, ELF, Mach-O)
 * 2. Deobfuscation of packed/protected executables
 * 3. Disassembly and control flow reconstruction
 * 4. High-level code generation from assembly
 * 5. Dependency analysis for binary files
 * 6. Integration with existing script analysis
 */
public class BinaryAwareTranslationWorkflow {
    
    private final BinaryAnalyzer binaryAnalyzer;
    private final ScriptAwareTranslationWorkflow scriptWorkflow;
    private final Translator coreTranslator;
    
    public BinaryAwareTranslationWorkflow(Translator coreTranslator) {
        this.coreTranslator = coreTranslator;
        this.binaryAnalyzer = new BinaryAnalyzer();
        this.scriptWorkflow = new ScriptAwareTranslationWorkflow(coreTranslator);
    }
    
    /**
     * Execute translation workflow that can handle both source code and binaries.
     */
    public EnhancedTranslationResult translateProject(String inputPath, 
                                                    TargetLanguage targetLanguage,
                                                    TranslationOptions options) throws IOException, TranslationException {
        
        EnhancedTranslationResult result = new EnhancedTranslationResult();
        Path inputDir = Paths.get(inputPath);
        
        // Step 1: Determine input type (source code project vs binary files)
        InputAnalysis inputAnalysis = analyzeInputType(inputDir);
        result.setInputAnalysis(inputAnalysis);
        
        if (inputAnalysis.getInputType() == InputType.BINARY_DIRECTORY || 
            inputAnalysis.getInputType() == InputType.SINGLE_BINARY) {
            
            // Binary analysis workflow
            return handleBinaryWorkflow(inputPath, targetLanguage, options, result);
            
        } else if (inputAnalysis.getInputType() == InputType.MIXED_PROJECT) {
            
            // Mixed workflow (source + binaries)
            return handleMixedWorkflow(inputPath, targetLanguage, options, result);
            
        } else {
            
            // Traditional source code workflow
            return handleSourceCodeWorkflow(inputPath, targetLanguage, options, result);
        }
    }
    
    private EnhancedTranslationResult handleBinaryWorkflow(String inputPath, 
                                                         TargetLanguage targetLanguage,
                                                         TranslationOptions options,
                                                         EnhancedTranslationResult result) throws IOException, TranslationException {
        
        System.out.println("Starting binary analysis and deobfuscation workflow...");
        
        // Step 1: Analyze binary files
        System.out.println("Analyzing binary files...");
        BinaryAnalysisResult binaryResult = binaryAnalyzer.analyzeBinary(inputPath, targetLanguage);
        result.setBinaryAnalysis(binaryResult);
        
        // Step 2: Generate high-level code from binary analysis
        System.out.println("Generating high-level code from binary analysis...");
        HighLevelCodeGenerator codeGenerator = new HighLevelCodeGenerator();
        Map<String, String> generatedCode = codeGenerator.generateCode(binaryResult, targetLanguage);
        result.setGeneratedCode(generatedCode);
        
        // Step 3: Create project structure for generated code
        System.out.println("Creating project structure...");
        ProjectStructure projectStructure = createProjectStructureFromBinary(binaryResult, targetLanguage);
        result.setProjectStructure(projectStructure);
        
        // Step 4: Generate build scripts for the target language
        System.out.println("Generating build configuration...");
        Map<String, String> buildConfig = generateBuildConfigForBinary(binaryResult, targetLanguage);
        result.setBuildConfiguration(buildConfig);
        
        // Step 5: Create documentation and analysis reports
        System.out.println("Generating documentation and reports...");
        BinaryAnalysisReport analysisReport = generateAnalysisReport(binaryResult, targetLanguage);
        result.setAnalysisReport(analysisReport);
        
        return result;
    }
    
    private EnhancedTranslationResult handleMixedWorkflow(String inputPath, 
                                                        TargetLanguage targetLanguage,
                                                        TranslationOptions options,
                                                        EnhancedTranslationResult result) throws IOException, TranslationException {
        
        System.out.println("Starting mixed source code and binary analysis workflow...");
        
        // Step 1: Run binary analysis on binary files
        Path inputDir = Paths.get(inputPath);
        List<Path> binaryFiles = findBinaryFiles(inputDir);
        
        Map<String, BinaryAnalysisResult> binaryResults = new HashMap<>();
        for (Path binaryFile : binaryFiles) {
            System.out.println("Analyzing binary: " + binaryFile.getFileName());
            BinaryAnalysisResult binaryResult = binaryAnalyzer.analyzeBinary(binaryFile.toString(), targetLanguage);
            binaryResults.put(binaryFile.toString(), binaryResult);
        }
        result.setBinaryResults(binaryResults);
        
        // Step 2: Run traditional script-aware workflow on source files
        System.out.println("Analyzing source code and scripts...");
        ScriptAwareTranslationResult scriptResult = scriptWorkflow.translateProject(inputPath, targetLanguage, options);
        result.setScriptAwareResult(scriptResult);
        
        // Step 3: Merge results and create unified project
        System.out.println("Merging binary and source analysis results...");
        UnifiedProjectResult unifiedResult = mergeBinaryAndSourceResults(binaryResults, scriptResult, targetLanguage);
        result.setUnifiedResult(unifiedResult);
        
        return result;
    }
    
    private EnhancedTranslationResult handleSourceCodeWorkflow(String inputPath, 
                                                             TargetLanguage targetLanguage,
                                                             TranslationOptions options,
                                                             EnhancedTranslationResult result) throws IOException, TranslationException {
        
        System.out.println("Running traditional source code workflow...");
        ScriptAwareTranslationResult scriptResult = scriptWorkflow.translateProject(inputPath, targetLanguage, options);
        result.setScriptAwareResult(scriptResult);
        
        return result;
    }
    
    /**
     * Write all generated files to the output directory.
     */
    public void writeGeneratedFiles(EnhancedTranslationResult result, String outputPath) throws IOException {
        Path outputDir = Paths.get(outputPath);
        Files.createDirectories(outputDir);
        
        // Write traditional script-aware results if present
        if (result.getScriptAwareResult() != null) {
            scriptWorkflow.writeGeneratedFiles(result.getScriptAwareResult(), outputPath);
        }
        
        // Write binary analysis results
        if (result.getBinaryAnalysis() != null) {
            writeBinaryAnalysisResults(result, outputDir);
        }
        
        // Write generated code from binaries
        if (result.getGeneratedCode() != null) {
            writeGeneratedCode(result.getGeneratedCode(), outputDir);
        }
        
        // Write unified results if present
        if (result.getUnifiedResult() != null) {
            writeUnifiedResults(result.getUnifiedResult(), outputDir);
        }
        
        System.out.println("All generated files written to: " + outputPath);
    }
    
    private InputAnalysis analyzeInputType(Path inputPath) throws IOException {
        InputAnalysis analysis = new InputAnalysis();
        
        if (Files.isRegularFile(inputPath)) {
            // Single file - check if it's binary
            if (isBinaryFile(inputPath)) {
                analysis.setInputType(InputType.SINGLE_BINARY);
                analysis.addBinaryFile(inputPath);
            } else {
                analysis.setInputType(InputType.SOURCE_CODE);
            }
        } else if (Files.isDirectory(inputPath)) {
            // Directory - analyze contents
            List<Path> sourceFiles = new ArrayList<>();
            List<Path> binaryFiles = new ArrayList<>();
            List<Path> scriptFiles = new ArrayList<>();
            
            Files.walk(inputPath)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    if (isBinaryFile(file)) {
                        binaryFiles.add(file);
                    } else if (isScriptFile(file)) {
                        scriptFiles.add(file);
                    } else if (isSourceCodeFile(file)) {
                        sourceFiles.add(file);
                    }
                });
            
            analysis.setSourceFiles(sourceFiles);
            analysis.setBinaryFiles(binaryFiles);
            analysis.setScriptFiles(scriptFiles);
            
            // Determine overall input type
            if (!binaryFiles.isEmpty() && sourceFiles.isEmpty()) {
                analysis.setInputType(InputType.BINARY_DIRECTORY);
            } else if (!binaryFiles.isEmpty() && !sourceFiles.isEmpty()) {
                analysis.setInputType(InputType.MIXED_PROJECT);
            } else {
                analysis.setInputType(InputType.SOURCE_PROJECT);
            }
        }
        
        return analysis;
    }
    
    private boolean isBinaryFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".exe") || 
               fileName.endsWith(".dll") || 
               fileName.endsWith(".so") || 
               fileName.endsWith(".dylib") || 
               fileName.endsWith(".bin") ||
               (!fileName.contains(".") && Files.isExecutable(file));
    }
    
    private boolean isScriptFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".sh") || 
               fileName.endsWith(".bat") || 
               fileName.endsWith(".cmd") || 
               fileName.equals("makefile") || 
               fileName.endsWith(".mk") || 
               fileName.endsWith(".cmake") ||
               fileName.equals("cmakelists.txt");
    }
    
    private boolean isSourceCodeFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".java") || 
               fileName.endsWith(".py") || 
               fileName.endsWith(".cpp") || 
               fileName.endsWith(".c") || 
               fileName.endsWith(".js") || 
               fileName.endsWith(".ts") || 
               fileName.endsWith(".rs") ||
               fileName.endsWith(".go");
    }
    
    private List<Path> findBinaryFiles(Path directory) throws IOException {
        List<Path> binaryFiles = new ArrayList<>();
        Files.walk(directory)
            .filter(Files::isRegularFile)
            .filter(this::isBinaryFile)
            .forEach(binaryFiles::add);
        return binaryFiles;
    }
    
    private ProjectStructure createProjectStructureFromBinary(BinaryAnalysisResult binaryResult, TargetLanguage targetLanguage) {
        ProjectStructure structure = new ProjectStructure();
        
        // Create standard directory structure for target language
        switch (targetLanguage) {
            case JAVA:
                structure.addDirectory("src/main/java/com/reverse/engineered");
                structure.addDirectory("src/main/resources");
                structure.addDirectory("src/test/java");
                structure.addFile("pom.xml");
                break;
            case PYTHON:
                structure.addDirectory("src");
                structure.addDirectory("tests");
                structure.addFile("setup.py");
                structure.addFile("requirements.txt");
                break;
            case JAVASCRIPT:
                structure.addDirectory("src");
                structure.addDirectory("test");
                structure.addFile("package.json");
                break;
            case CPP:
                structure.addDirectory("src");
                structure.addDirectory("include");
                structure.addDirectory("tests");
                structure.addFile("CMakeLists.txt");
                break;
        }
        
        // Add documentation directories
        structure.addDirectory("docs");
        structure.addDirectory("analysis");
        
        return structure;
    }
    
    private Map<String, String> generateBuildConfigForBinary(BinaryAnalysisResult binaryResult, TargetLanguage targetLanguage) {
        Map<String, String> buildConfig = new HashMap<>();
        
        switch (targetLanguage) {
            case JAVA:
                buildConfig.put("pom.xml", generateMavenPom(binaryResult));
                buildConfig.put("build.gradle", generateGradleBuild(binaryResult));
                break;
            case PYTHON:
                buildConfig.put("setup.py", generatePythonSetup(binaryResult));
                buildConfig.put("requirements.txt", generatePythonRequirements(binaryResult));
                break;
            case JAVASCRIPT:
                buildConfig.put("package.json", generatePackageJson(binaryResult));
                break;
            case CPP:
                buildConfig.put("CMakeLists.txt", generateCMakeLists(binaryResult));
                buildConfig.put("Makefile", generateMakefile(binaryResult));
                break;
        }
        
        // Add cross-platform scripts
        buildConfig.put("build.sh", generateUnixBuildScript(targetLanguage));
        buildConfig.put("build.bat", generateWindowsBuildScript(targetLanguage));
        
        return buildConfig;
    }
    
    private BinaryAnalysisReport generateAnalysisReport(BinaryAnalysisResult binaryResult, TargetLanguage targetLanguage) {
        BinaryAnalysisReport report = new BinaryAnalysisReport();
        report.setBinaryResult(binaryResult);
        report.setTargetLanguage(targetLanguage);
        report.setGenerationTimestamp(new Date());
        
        // Add summary information
        report.addSection("Binary Analysis Summary", 
            "Analysis of binary file: " + binaryResult.getBinaryPath());
        
        if (binaryResult.isObfuscated()) {
            report.addSection("Obfuscation Detected", 
                "The binary appears to be obfuscated. Deobfuscation techniques were applied.");
        }
        
        // Add function analysis
        if (binaryResult.getFunctions() != null && !binaryResult.getFunctions().isEmpty()) {
            report.addSection("Functions Identified", 
                "Found " + binaryResult.getFunctions().size() + " functions in the binary.");
        }
        
        // Add dependency information
        if (binaryResult.getDependencies() != null && !binaryResult.getDependencies().isEmpty()) {
            report.addSection("Dependencies", 
                "Binary depends on " + binaryResult.getDependencies().size() + " external libraries.");
        }
        
        return report;
    }
    
    private void writeBinaryAnalysisResults(EnhancedTranslationResult result, Path outputDir) throws IOException {
        // Write analysis report
        if (result.getAnalysisReport() != null) {
            Path reportPath = outputDir.resolve("analysis/BINARY_ANALYSIS_REPORT.md");
            Files.createDirectories(reportPath.getParent());
            Files.write(reportPath, result.getAnalysisReport().generateMarkdown().getBytes());
        }
        
        // Write binary structure information
        Path structurePath = outputDir.resolve("analysis/binary_structure.json");
        Files.createDirectories(structurePath.getParent());
        // Generate JSON representation of binary structure
        String structureJson = generateBinaryStructureJson(result.getBinaryAnalysis());
        Files.write(structurePath, structureJson.getBytes());
    }
    
    private void writeGeneratedCode(Map<String, String> generatedCode, Path outputDir) throws IOException {
        for (Map.Entry<String, String> codeEntry : generatedCode.entrySet()) {
            Path codePath = outputDir.resolve(codeEntry.getKey());
            Files.createDirectories(codePath.getParent());
            Files.write(codePath, codeEntry.getValue().getBytes());
        }
    }
    
    private void writeUnifiedResults(UnifiedProjectResult unifiedResult, Path outputDir) throws IOException {
        // Write unified project documentation
        Path unifiedDocPath = outputDir.resolve("UNIFIED_PROJECT.md");
        String unifiedDoc = generateUnifiedProjectDocumentation(unifiedResult);
        Files.write(unifiedDocPath, unifiedDoc.getBytes());
    }
    
    private UnifiedProjectResult mergeBinaryAndSourceResults(Map<String, BinaryAnalysisResult> binaryResults,
                                                           ScriptAwareTranslationResult scriptResult,
                                                           TargetLanguage targetLanguage) {
        UnifiedProjectResult unified = new UnifiedProjectResult();
        unified.setBinaryResults(binaryResults);
        unified.setScriptResult(scriptResult);
        unified.setTargetLanguage(targetLanguage);
        
        // Create integration points between binary and source analysis
        // This would identify how binary dependencies relate to source code
        
        return unified;
    }
    
    // Helper methods for build configuration generation
    private String generateMavenPom(BinaryAnalysisResult binaryResult) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
               "    <modelVersion>4.0.0</modelVersion>\n" +
               "    <groupId>com.reverse.engineered</groupId>\n" +
               "    <artifactId>binary-translation</artifactId>\n" +
               "    <version>1.0.0</version>\n" +
               "    <properties>\n" +
               "        <maven.compiler.source>11</maven.compiler.source>\n" +
               "        <maven.compiler.target>11</maven.compiler.target>\n" +
               "    </properties>\n" +
               "</project>";
    }
    
    private String generateGradleBuild(BinaryAnalysisResult binaryResult) {
        return "plugins {\n" +
               "    id 'java'\n" +
               "}\n\n" +
               "group = 'com.reverse.engineered'\n" +
               "version = '1.0.0'\n\n" +
               "java {\n" +
               "    sourceCompatibility = JavaVersion.VERSION_11\n" +
               "    targetCompatibility = JavaVersion.VERSION_11\n" +
               "}\n\n" +
               "repositories {\n" +
               "    mavenCentral()\n" +
               "}";
    }
    
    private String generatePythonSetup(BinaryAnalysisResult binaryResult) {
        return "from setuptools import setup, find_packages\n\n" +
               "setup(\n" +
               "    name='binary-translation',\n" +
               "    version='1.0.0',\n" +
               "    packages=find_packages(),\n" +
               "    install_requires=[],\n" +
               "    python_requires='>=3.8'\n" +
               ")";
    }
    
    private String generatePythonRequirements(BinaryAnalysisResult binaryResult) {
        return "# Generated requirements for reverse engineered binary\n" +
               "# Add dependencies as needed\n";
    }
    
    private String generatePackageJson(BinaryAnalysisResult binaryResult) {
        return "{\n" +
               "  \"name\": \"binary-translation\",\n" +
               "  \"version\": \"1.0.0\",\n" +
               "  \"description\": \"Reverse engineered binary\",\n" +
               "  \"main\": \"src/index.js\",\n" +
               "  \"scripts\": {\n" +
               "    \"test\": \"echo \\\"Error: no test specified\\\" && exit 1\"\n" +
               "  }\n" +
               "}";
    }
    
    private String generateCMakeLists(BinaryAnalysisResult binaryResult) {
        return "cmake_minimum_required(VERSION 3.10)\n" +
               "project(BinaryTranslation)\n\n" +
               "set(CMAKE_CXX_STANDARD 17)\n\n" +
               "add_executable(binary_translation src/main.cpp)\n";
    }
    
    private String generateMakefile(BinaryAnalysisResult binaryResult) {
        return "CC=gcc\n" +
               "CFLAGS=-Wall -Wextra -std=c99\n" +
               "TARGET=binary_translation\n\n" +
               "all: $(TARGET)\n\n" +
               "$(TARGET): src/main.c\n" +
               "\t$(CC) $(CFLAGS) -o $(TARGET) src/main.c\n\n" +
               "clean:\n" +
               "\trm -f $(TARGET)\n";
    }
    
    private String generateUnixBuildScript(TargetLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA:
                return "#!/bin/bash\n" +
                       "echo \"Building Java project...\"\n" +
                       "mvn clean compile\n";
            case PYTHON:
                return "#!/bin/bash\n" +
                       "echo \"Setting up Python project...\"\n" +
                       "python -m venv venv\n" +
                       "source venv/bin/activate\n" +
                       "pip install -r requirements.txt\n";
            default:
                return "#!/bin/bash\n" +
                       "echo \"Build script for " + targetLanguage + "\"\n";
        }
    }
    
    private String generateWindowsBuildScript(TargetLanguage targetLanguage) {
        switch (targetLanguage) {
            case JAVA:
                return "@echo off\n" +
                       "echo Building Java project...\n" +
                       "mvn clean compile\n";
            case PYTHON:
                return "@echo off\n" +
                       "echo Setting up Python project...\n" +
                       "python -m venv venv\n" +
                       "venv\\Scripts\\activate\n" +
                       "pip install -r requirements.txt\n";
            default:
                return "@echo off\n" +
                       "echo Build script for " + targetLanguage + "\n";
        }
    }
    
    private String generateBinaryStructureJson(BinaryAnalysisResult binaryResult) {
        // Generate JSON representation of binary analysis results
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"binary_path\": \"").append(binaryResult.getBinaryPath()).append("\",\n");
        json.append("  \"format\": \"").append(binaryResult.getFormat()).append("\",\n");
        json.append("  \"architecture\": \"").append(binaryResult.getArchitecture()).append("\",\n");
        json.append("  \"obfuscated\": ").append(binaryResult.isObfuscated()).append(",\n");
        json.append("  \"functions_found\": ").append(binaryResult.getFunctions().size()).append(",\n");
        json.append("  \"dependencies_count\": ").append(binaryResult.getDependencies().size()).append("\n");
        json.append("}");
        return json.toString();
    }
    
    private String generateUnifiedProjectDocumentation(UnifiedProjectResult unifiedResult) {
        StringBuilder doc = new StringBuilder();
        doc.append("# Unified Project Analysis\n\n");
        doc.append("This project contains both source code and binary components.\n\n");
        doc.append("## Binary Analysis Results\n\n");
        doc.append("Found ").append(unifiedResult.getBinaryResults().size()).append(" binary files.\n\n");
        doc.append("## Source Code Analysis\n\n");
        doc.append("Traditional source code translation was also performed.\n\n");
        return doc.toString();
    }
}

// Supporting classes
enum InputType {
    SOURCE_CODE, SOURCE_PROJECT, SINGLE_BINARY, BINARY_DIRECTORY, MIXED_PROJECT
}

class InputAnalysis {
    private InputType inputType;
    private List<Path> sourceFiles = new ArrayList<>();
    private List<Path> binaryFiles = new ArrayList<>();
    private List<Path> scriptFiles = new ArrayList<>();
    
    // Getters and setters
    public InputType getInputType() { return inputType; }
    public void setInputType(InputType inputType) { this.inputType = inputType; }
    
    public List<Path> getSourceFiles() { return sourceFiles; }
    public void setSourceFiles(List<Path> sourceFiles) { this.sourceFiles = sourceFiles; }
    
    public List<Path> getBinaryFiles() { return binaryFiles; }
    public void setBinaryFiles(List<Path> binaryFiles) { this.binaryFiles = binaryFiles; }
    public void addBinaryFile(Path file) { this.binaryFiles.add(file); }
    
    public List<Path> getScriptFiles() { return scriptFiles; }
    public void setScriptFiles(List<Path> scriptFiles) { this.scriptFiles = scriptFiles; }
}

class EnhancedTranslationResult {
    private InputAnalysis inputAnalysis;
    private BinaryAnalysisResult binaryAnalysis;
    private Map<String, BinaryAnalysisResult> binaryResults;
    private ScriptAwareTranslationResult scriptAwareResult;
    private Map<String, String> generatedCode;
    private Map<String, String> buildConfiguration;
    private ProjectStructure projectStructure;
    private BinaryAnalysisReport analysisReport;
    private UnifiedProjectResult unifiedResult;
    
    // Getters and setters
    public InputAnalysis getInputAnalysis() { return inputAnalysis; }
    public void setInputAnalysis(InputAnalysis inputAnalysis) { this.inputAnalysis = inputAnalysis; }
    
    public BinaryAnalysisResult getBinaryAnalysis() { return binaryAnalysis; }
    public void setBinaryAnalysis(BinaryAnalysisResult binaryAnalysis) { this.binaryAnalysis = binaryAnalysis; }
    
    public Map<String, BinaryAnalysisResult> getBinaryResults() { return binaryResults; }
    public void setBinaryResults(Map<String, BinaryAnalysisResult> binaryResults) { this.binaryResults = binaryResults; }
    
    public ScriptAwareTranslationResult getScriptAwareResult() { return scriptAwareResult; }
    public void setScriptAwareResult(ScriptAwareTranslationResult scriptAwareResult) { this.scriptAwareResult = scriptAwareResult; }
    
    public Map<String, String> getGeneratedCode() { return generatedCode; }
    public void setGeneratedCode(Map<String, String> generatedCode) { this.generatedCode = generatedCode; }
    
    public Map<String, String> getBuildConfiguration() { return buildConfiguration; }
    public void setBuildConfiguration(Map<String, String> buildConfiguration) { this.buildConfiguration = buildConfiguration; }
    
    public ProjectStructure getProjectStructure() { return projectStructure; }
    public void setProjectStructure(ProjectStructure projectStructure) { this.projectStructure = projectStructure; }
    
    public BinaryAnalysisReport getAnalysisReport() { return analysisReport; }
    public void setAnalysisReport(BinaryAnalysisReport analysisReport) { this.analysisReport = analysisReport; }
    
    public UnifiedProjectResult getUnifiedResult() { return unifiedResult; }
    public void setUnifiedResult(UnifiedProjectResult unifiedResult) { this.unifiedResult = unifiedResult; }
}

class ProjectStructure {
    private Set<String> directories = new HashSet<>();
    private Set<String> files = new HashSet<>();
    
    public void addDirectory(String dir) { directories.add(dir); }
    public void addFile(String file) { files.add(file); }
    
    public Set<String> getDirectories() { return directories; }
    public Set<String> getFiles() { return files; }
}

class BinaryAnalysisReport {
    private BinaryAnalysisResult binaryResult;
    private TargetLanguage targetLanguage;
    private Date generationTimestamp;
    private List<String> sections = new ArrayList<>();
    
    public void addSection(String title, String content) {
        sections.add("## " + title + "\n\n" + content + "\n\n");
    }
    
    public String generateMarkdown() {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Binary Analysis Report\n\n");
        markdown.append("Generated: ").append(generationTimestamp).append("\n\n");
        for (String section : sections) {
            markdown.append(section);
        }
        return markdown.toString();
    }
    
    // Getters and setters
    public BinaryAnalysisResult getBinaryResult() { return binaryResult; }
    public void setBinaryResult(BinaryAnalysisResult binaryResult) { this.binaryResult = binaryResult; }
    
    public TargetLanguage getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(TargetLanguage targetLanguage) { this.targetLanguage = targetLanguage; }
    
    public Date getGenerationTimestamp() { return generationTimestamp; }
    public void setGenerationTimestamp(Date generationTimestamp) { this.generationTimestamp = generationTimestamp; }
}

class UnifiedProjectResult {
    private Map<String, BinaryAnalysisResult> binaryResults;
    private ScriptAwareTranslationResult scriptResult;
    private TargetLanguage targetLanguage;
    
    // Getters and setters
    public Map<String, BinaryAnalysisResult> getBinaryResults() { return binaryResults; }
    public void setBinaryResults(Map<String, BinaryAnalysisResult> binaryResults) { this.binaryResults = binaryResults; }
    
    public ScriptAwareTranslationResult getScriptResult() { return scriptResult; }
    public void setScriptResult(ScriptAwareTranslationResult scriptResult) { this.scriptResult = scriptResult; }
    
    public TargetLanguage getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(TargetLanguage targetLanguage) { this.targetLanguage = targetLanguage; }
}