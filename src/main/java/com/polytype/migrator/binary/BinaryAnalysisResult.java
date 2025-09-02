package com.polytype.migrator.binary;

import com.polytype.migrator.core.TargetLanguage;
import java.util.*;

/**
 * Complete result of binary analysis containing all extracted information,
 * deobfuscation results, and generated high-level code.
 */
public class BinaryAnalysisResult {
    
    private String inputPath;
    private TargetLanguage targetLanguage;
    private String mainExecutable;
    private BinaryFileAnalysis mainExecutableAnalysis;
    private List<String> dependencies;
    private Map<String, BinaryFileAnalysis> dependencyAnalyses;
    private Map<String, String> configurationFiles;
    private Map<String, ResourceInfo> additionalResources;
    private Map<String, String> generatedCode;
    private ProjectStructureRecommendation structureRecommendation;
    private List<String> securityIssues;
    private DecompilationMetrics metrics;
    
    public BinaryAnalysisResult() {
        this.dependencies = new ArrayList<>();
        this.dependencyAnalyses = new HashMap<>();
        this.configurationFiles = new HashMap<>();
        this.additionalResources = new HashMap<>();
        this.generatedCode = new HashMap<>();
        this.securityIssues = new ArrayList<>();
    }
    
    // Getters and setters
    public String getInputPath() { return inputPath; }
    public void setInputPath(String inputPath) { this.inputPath = inputPath; }
    
    public TargetLanguage getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(TargetLanguage targetLanguage) { this.targetLanguage = targetLanguage; }
    
    public String getMainExecutable() { return mainExecutable; }
    public void setMainExecutable(String mainExecutable) { this.mainExecutable = mainExecutable; }
    
    public BinaryFileAnalysis getMainExecutableAnalysis() { return mainExecutableAnalysis; }
    public void setMainExecutableAnalysis(BinaryFileAnalysis mainExecutableAnalysis) { this.mainExecutableAnalysis = mainExecutableAnalysis; }
    
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    
    public Map<String, BinaryFileAnalysis> getDependencyAnalyses() { return dependencyAnalyses; }
    public void setDependencyAnalyses(Map<String, BinaryFileAnalysis> dependencyAnalyses) { this.dependencyAnalyses = dependencyAnalyses; }
    
    public Map<String, String> getConfigurationFiles() { return configurationFiles; }
    public void setConfigurationFiles(Map<String, String> configurationFiles) { this.configurationFiles = configurationFiles; }
    
    public Map<String, ResourceInfo> getAdditionalResources() { return additionalResources; }
    public void setAdditionalResources(Map<String, ResourceInfo> additionalResources) { this.additionalResources = additionalResources; }
    
    public Map<String, String> getGeneratedCode() { return generatedCode; }
    public void setGeneratedCode(Map<String, String> generatedCode) { this.generatedCode = generatedCode; }
    
    public ProjectStructureRecommendation getStructureRecommendation() { return structureRecommendation; }
    public void setStructureRecommendation(ProjectStructureRecommendation structureRecommendation) { this.structureRecommendation = structureRecommendation; }
    
    public List<String> getSecurityIssues() { return securityIssues; }
    public void setSecurityIssues(List<String> securityIssues) { this.securityIssues = securityIssues; }
    public void addSecurityIssue(String issue) { this.securityIssues.add(issue); }
    
    public DecompilationMetrics getMetrics() { return metrics; }
    public void setMetrics(DecompilationMetrics metrics) { this.metrics = metrics; }
    
    /**
     * Get summary statistics about the analysis.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Binary Analysis Summary:\n");
        summary.append("Input: ").append(inputPath).append("\n");
        summary.append("Target Language: ").append(targetLanguage).append("\n");
        summary.append("Main Executable: ").append(mainExecutable).append("\n");
        summary.append("Dependencies: ").append(dependencies.size()).append("\n");
        summary.append("Configuration Files: ").append(configurationFiles.size()).append("\n");
        summary.append("Additional Resources: ").append(additionalResources.size()).append("\n");
        summary.append("Generated Code Files: ").append(generatedCode.size()).append("\n");
        
        if (mainExecutableAnalysis != null) {
            summary.append("Functions Analyzed: ").append(mainExecutableAnalysis.getDisassembledFunctions().size()).append("\n");
            summary.append("Strings Extracted: ").append(mainExecutableAnalysis.getExtractedStrings().size()).append("\n");
            summary.append("API Calls: ").append(mainExecutableAnalysis.getApiCalls().size()).append("\n");
        }
        
        if (!securityIssues.isEmpty()) {
            summary.append("Security Issues: ").append(securityIssues.size()).append("\n");
        }
        
        return summary.toString();
    }
}

/**
 * Analysis result for a single binary file.
 */
class BinaryFileAnalysis {
    private String filePath;
    private String fileName;
    private long fileSize;
    private BinaryAnalyzer.BinaryFormat binaryFormat;
    private BinaryStructure binaryStructure;
    private List<String> extractedStrings;
    private Map<String, byte[]> resources;
    private List<DisassembledFunction> disassembledFunctions;
    private List<ApiCall> apiCalls;
    private DeobfuscationResult deobfuscationResult;
    private List<String> imports;
    private List<String> exports;
    private Map<String, Object> metadata;
    
    public BinaryFileAnalysis() {
        this.extractedStrings = new ArrayList<>();
        this.resources = new HashMap<>();
        this.disassembledFunctions = new ArrayList<>();
        this.apiCalls = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.exports = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    // Getters and setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public BinaryAnalyzer.BinaryFormat getBinaryFormat() { return binaryFormat; }
    public void setBinaryFormat(BinaryAnalyzer.BinaryFormat binaryFormat) { this.binaryFormat = binaryFormat; }
    
    public BinaryStructure getBinaryStructure() { return binaryStructure; }
    public void setBinaryStructure(BinaryStructure binaryStructure) { this.binaryStructure = binaryStructure; }
    
    public List<String> getExtractedStrings() { return extractedStrings; }
    public void setExtractedStrings(List<String> extractedStrings) { this.extractedStrings = extractedStrings; }
    
    public Map<String, byte[]> getResources() { return resources; }
    public void setResources(Map<String, byte[]> resources) { this.resources = resources; }
    
    public List<DisassembledFunction> getDisassembledFunctions() { return disassembledFunctions; }
    public void setDisassembledFunctions(List<DisassembledFunction> disassembledFunctions) { this.disassembledFunctions = disassembledFunctions; }
    
    public List<ApiCall> getApiCalls() { return apiCalls; }
    public void setApiCalls(List<ApiCall> apiCalls) { this.apiCalls = apiCalls; }
    
    public DeobfuscationResult getDeobfuscationResult() { return deobfuscationResult; }
    public void setDeobfuscationResult(DeobfuscationResult deobfuscationResult) { this.deobfuscationResult = deobfuscationResult; }
    
    public List<String> getImports() { return imports; }
    public void setImports(List<String> imports) { this.imports = imports; }
    
    public List<String> getExports() { return exports; }
    public void setExports(List<String> exports) { this.exports = exports; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

/**
 * Information about binary file structure.
 */
class BinaryStructure {
    private String architecture;           // x86, x64, ARM, etc.
    private String operatingSystem;        // Windows, Linux, macOS
    private long entryPoint;
    private List<Section> sections;
    private List<String> linkedLibraries;
    private Map<String, Object> headers;
    private boolean isPacked;
    private String packer;
    
    public BinaryStructure() {
        this.sections = new ArrayList<>();
        this.linkedLibraries = new ArrayList<>();
        this.headers = new HashMap<>();
    }
    
    // Getters and setters
    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }
    
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    
    public long getEntryPoint() { return entryPoint; }
    public void setEntryPoint(long entryPoint) { this.entryPoint = entryPoint; }
    
    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }
    
    public List<String> getLinkedLibraries() { return linkedLibraries; }
    public void setLinkedLibraries(List<String> linkedLibraries) { this.linkedLibraries = linkedLibraries; }
    
    public Map<String, Object> getHeaders() { return headers; }
    public void setHeaders(Map<String, Object> headers) { this.headers = headers; }
    
    public boolean isPacked() { return isPacked; }
    public void setPacked(boolean packed) { isPacked = packed; }
    
    public String getPacker() { return packer; }
    public void setPacker(String packer) { this.packer = packer; }
}

/**
 * Represents a section in a binary file.
 */
class Section {
    private String name;
    private long virtualAddress;
    private long virtualSize;
    private long rawAddress;
    private long rawSize;
    private Set<String> permissions;
    private SectionType type;
    
    public Section() {
        this.permissions = new HashSet<>();
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public long getVirtualAddress() { return virtualAddress; }
    public void setVirtualAddress(long virtualAddress) { this.virtualAddress = virtualAddress; }
    
    public long getVirtualSize() { return virtualSize; }
    public void setVirtualSize(long virtualSize) { this.virtualSize = virtualSize; }
    
    public long getRawAddress() { return rawAddress; }
    public void setRawAddress(long rawAddress) { this.rawAddress = rawAddress; }
    
    public long getRawSize() { return rawSize; }
    public void setRawSize(long rawSize) { this.rawSize = rawSize; }
    
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    
    public SectionType getType() { return type; }
    public void setType(SectionType type) { this.type = type; }
    
    public enum SectionType {
        CODE, DATA, RESOURCES, IMPORTS, EXPORTS, DEBUG, UNKNOWN
    }
}

/**
 * Resource information.
 */
class ResourceInfo {
    private String path;
    private String type;
    private long size;
    private String description;
    private Map<String, Object> properties;
    
    public ResourceInfo() {
        this.properties = new HashMap<>();
    }
    
    // Getters and setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}

/**
 * Decompilation performance metrics.
 */
class DecompilationMetrics {
    private long analysisTimeMs;
    private long disassemblyTimeMs;
    private long deobfuscationTimeMs;
    private long codeGenerationTimeMs;
    private int functionsAnalyzed;
    private int instructionsDisassembled;
    private double confidenceScore;
    
    // Getters and setters
    public long getAnalysisTimeMs() { return analysisTimeMs; }
    public void setAnalysisTimeMs(long analysisTimeMs) { this.analysisTimeMs = analysisTimeMs; }
    
    public long getDisassemblyTimeMs() { return disassemblyTimeMs; }
    public void setDisassemblyTimeMs(long disassemblyTimeMs) { this.disassemblyTimeMs = disassemblyTimeMs; }
    
    public long getDeobfuscationTimeMs() { return deobfuscationTimeMs; }
    public void setDeobfuscationTimeMs(long deobfuscationTimeMs) { this.deobfuscationTimeMs = deobfuscationTimeMs; }
    
    public long getCodeGenerationTimeMs() { return codeGenerationTimeMs; }
    public void setCodeGenerationTimeMs(long codeGenerationTimeMs) { this.codeGenerationTimeMs = codeGenerationTimeMs; }
    
    public int getFunctionsAnalyzed() { return functionsAnalyzed; }
    public void setFunctionsAnalyzed(int functionsAnalyzed) { this.functionsAnalyzed = functionsAnalyzed; }
    
    public int getInstructionsDisassembled() { return instructionsDisassembled; }
    public void setInstructionsDisassembled(int instructionsDisassembled) { this.instructionsDisassembled = instructionsDisassembled; }
    
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
}