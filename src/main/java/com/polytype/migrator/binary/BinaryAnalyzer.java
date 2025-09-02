package com.polytype.migrator.binary;

import com.polytype.migrator.core.TargetLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Binary Analysis and Deobfuscation Engine for PolyType.
 * 
 * Supports comprehensive analysis of:
 * - Windows PE files (.exe, .dll, .sys)
 * - Linux ELF files
 * - macOS Mach-O files
 * - .NET assemblies
 * - Java bytecode (.class, .jar)
 * - Native mobile apps (APK already supported)
 * 
 * Features:
 * - Disassembly and control flow reconstruction
 * - String and resource extraction
 * - API call analysis and dependency mapping
 * - Anti-analysis technique detection and bypass
 * - Code obfuscation reversal
 * - High-level code generation in target languages
 */
public class BinaryAnalyzer {
    
    private final Map<BinaryFormat, BinaryParser> parsers;
    private final DisassemblyEngine disassembler;
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    private final StringExtractor stringExtractor;
    private final ResourceExtractor resourceExtractor;
    private final ApiCallAnalyzer apiAnalyzer;
    private final DeobfuscationEngine deobfuscator;
    
    public BinaryAnalyzer() {
        this.parsers = new HashMap<>();
        this.disassembler = new DisassemblyEngine();
        this.controlFlowAnalyzer = new ControlFlowAnalyzer();
        this.stringExtractor = new StringExtractor();
        this.resourceExtractor = new ResourceExtractor();
        this.apiAnalyzer = new ApiCallAnalyzer();
        this.deobfuscator = new DeobfuscationEngine();
        
        initializeParsers();
    }
    
    /**
     * Analyze a binary file or directory containing executable and dependencies.
     */
    public BinaryAnalysisResult analyzeBinary(String inputPath, TargetLanguage targetLanguage) throws IOException {
        Path path = Paths.get(inputPath);
        BinaryAnalysisResult result = new BinaryAnalysisResult();
        result.setInputPath(inputPath);
        result.setTargetLanguage(targetLanguage);
        
        if (Files.isDirectory(path)) {
            // Analyze directory containing executable and dependencies
            return analyzeDirectory(path, targetLanguage);
        } else {
            // Analyze single binary file
            return analyzeSingleBinary(path, targetLanguage);
        }
    }
    
    private BinaryAnalysisResult analyzeDirectory(Path directory, TargetLanguage targetLanguage) throws IOException {
        BinaryAnalysisResult result = new BinaryAnalysisResult();
        result.setInputPath(directory.toString());
        result.setTargetLanguage(targetLanguage);
        
        // Find all binary files in directory
        List<Path> binaryFiles = findBinaryFiles(directory);
        
        // Identify main executable
        Path mainExecutable = identifyMainExecutable(binaryFiles);
        if (mainExecutable != null) {
            result.setMainExecutable(mainExecutable.toString());
            
            // Analyze main executable first
            BinaryFileAnalysis mainAnalysis = analyzeBinaryFile(mainExecutable);
            result.setMainExecutableAnalysis(mainAnalysis);
            
            // Walk dependencies
            DependencyWalker depWalker = new DependencyWalker();
            List<String> dependencies = depWalker.walkDependencies(mainExecutable);
            result.setDependencies(dependencies);
            
            // Analyze each dependency
            Map<String, BinaryFileAnalysis> depAnalyses = new HashMap<>();
            for (String dep : dependencies) {
                Path depPath = directory.resolve(dep);
                if (Files.exists(depPath)) {
                    BinaryFileAnalysis depAnalysis = analyzeBinaryFile(depPath);
                    depAnalyses.put(dep, depAnalysis);
                }
            }
            result.setDependencyAnalyses(depAnalyses);
        }
        
        // Analyze additional files (resources, configs, etc.)
        analyzeAdditionalFiles(directory, result);
        
        // Generate high-level representation
        generateHighLevelCode(result);
        
        return result;
    }
    
    private BinaryAnalysisResult analyzeSingleBinary(Path binaryFile, TargetLanguage targetLanguage) throws IOException {
        BinaryAnalysisResult result = new BinaryAnalysisResult();
        result.setInputPath(binaryFile.toString());
        result.setTargetLanguage(targetLanguage);
        result.setMainExecutable(binaryFile.toString());
        
        // Analyze the binary file
        BinaryFileAnalysis analysis = analyzeBinaryFile(binaryFile);
        result.setMainExecutableAnalysis(analysis);
        
        // Generate high-level representation
        generateHighLevelCode(result);
        
        return result;
    }
    
    private BinaryFileAnalysis analyzeBinaryFile(Path binaryFile) throws IOException {
        BinaryFileAnalysis analysis = new BinaryFileAnalysis();
        analysis.setFilePath(binaryFile.toString());
        analysis.setFileName(binaryFile.getFileName().toString());
        analysis.setFileSize(Files.size(binaryFile));
        
        // Detect binary format
        BinaryFormat format = detectBinaryFormat(binaryFile);
        analysis.setBinaryFormat(format);
        
        // Get appropriate parser
        BinaryParser parser = parsers.get(format);
        if (parser == null) {
            throw new UnsupportedOperationException("Unsupported binary format: " + format);
        }
        
        // Parse binary structure
        BinaryStructure structure = parser.parseStructure(binaryFile);
        analysis.setBinaryStructure(structure);
        
        // Extract strings and resources
        List<String> strings = stringExtractor.extractStrings(binaryFile, format);
        analysis.setExtractedStrings(strings);
        
        Map<String, byte[]> resources = resourceExtractor.extractResources(binaryFile, format);
        analysis.setResources(resources);
        
        // Disassemble code sections
        List<DisassembledFunction> functions = disassembler.disassemble(binaryFile, structure);
        analysis.setDisassembledFunctions(functions);
        
        // Analyze control flow
        for (DisassembledFunction function : functions) {
            ControlFlowGraph cfg = controlFlowAnalyzer.analyzeFunction(function);
            function.setControlFlowGraph(cfg);
        }
        
        // Analyze API calls
        List<ApiCall> apiCalls = apiAnalyzer.analyzeApiCalls(functions);
        analysis.setApiCalls(apiCalls);
        
        // Detect and reverse obfuscation
        DeobfuscationResult deobfResult = deobfuscator.deobfuscate(analysis);
        analysis.setDeobfuscationResult(deobfResult);
        
        return analysis;
    }
    
    private BinaryFormat detectBinaryFormat(Path binaryFile) throws IOException {
        byte[] header = Files.readAllBytes(binaryFile);
        if (header.length < 4) {
            return BinaryFormat.UNKNOWN;
        }
        
        // Check magic numbers
        if (header[0] == 'M' && header[1] == 'Z') {
            // DOS/Windows executable
            return BinaryFormat.PE;
        } else if (header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
            // ELF (Linux/Unix)
            return BinaryFormat.ELF;
        } else if ((header[0] == (byte)0xFE && header[1] == (byte)0xED && header[2] == (byte)0xFA && header[3] == (byte)0xCE) ||
                   (header[0] == (byte)0xFE && header[1] == (byte)0xED && header[2] == (byte)0xFA && header[3] == (byte)0xCF) ||
                   (header[0] == (byte)0xCE && header[1] == (byte)0xFA && header[2] == (byte)0xED && header[3] == (byte)0xFE) ||
                   (header[0] == (byte)0xCF && header[1] == (byte)0xFA && header[2] == (byte)0xED && header[3] == (byte)0xFE)) {
            // Mach-O (macOS)
            return BinaryFormat.MACHO;
        } else if (header[0] == (byte)0xCA && header[1] == (byte)0xFE && header[2] == (byte)0xBA && header[3] == (byte)0xBE) {
            // Java class file
            return BinaryFormat.JAVA_CLASS;
        } else if (header[0] == 'P' && header[1] == 'K') {
            // ZIP-based format (JAR, APK, etc.)
            return BinaryFormat.ZIP_BASED;
        }
        
        return BinaryFormat.UNKNOWN;
    }
    
    private List<Path> findBinaryFiles(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(this::isBinaryFile)
                .collect(Collectors.toList());
    }
    
    private boolean isBinaryFile(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        return filename.endsWith(".exe") || filename.endsWith(".dll") || 
               filename.endsWith(".so") || filename.endsWith(".dylib") ||
               filename.endsWith(".sys") || filename.endsWith(".ocx") ||
               filename.endsWith(".class") || filename.endsWith(".jar") ||
               !filename.contains(".") || isExecutableByHeader(file);
    }
    
    private boolean isExecutableByHeader(Path file) {
        try {
            byte[] header = new byte[4];
            Files.newInputStream(file).read(header);
            return (header[0] == 'M' && header[1] == 'Z') || // PE
                   (header[0] == 0x7F && header[1] == 'E') || // ELF
                   (header[0] == (byte)0xFE && header[1] == (byte)0xED); // Mach-O
        } catch (Exception e) {
            return false;
        }
    }
    
    private Path identifyMainExecutable(List<Path> binaryFiles) {
        // Look for .exe files first
        for (Path file : binaryFiles) {
            if (file.toString().toLowerCase().endsWith(".exe")) {
                return file;
            }
        }
        
        // Look for files without extension (Unix executables)
        for (Path file : binaryFiles) {
            String filename = file.getFileName().toString();
            if (!filename.contains(".") && isExecutableByHeader(file)) {
                return file;
            }
        }
        
        // Return first binary file found
        return binaryFiles.isEmpty() ? null : binaryFiles.get(0);
    }
    
    private void analyzeAdditionalFiles(Path directory, BinaryAnalysisResult result) throws IOException {
        // Look for configuration files
        List<Path> configFiles = Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(this::isConfigFile)
                .collect(Collectors.toList());
        
        Map<String, String> configs = new HashMap<>();
        for (Path configFile : configFiles) {
            try {
                String content = Files.readString(configFile);
                configs.put(configFile.getFileName().toString(), content);
            } catch (Exception e) {
                // Skip files that can't be read as text
            }
        }
        result.setConfigurationFiles(configs);
        
        // Look for resource files
        List<Path> resourceFiles = Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(this::isResourceFile)
                .collect(Collectors.toList());
        
        Map<String, ResourceInfo> resources = new HashMap<>();
        for (Path resourceFile : resourceFiles) {
            ResourceInfo info = new ResourceInfo();
            info.setPath(resourceFile.toString());
            info.setSize(Files.size(resourceFile));
            info.setType(getResourceType(resourceFile));
            resources.put(resourceFile.getFileName().toString(), info);
        }
        result.setAdditionalResources(resources);
    }
    
    private boolean isConfigFile(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        return filename.endsWith(".ini") || filename.endsWith(".cfg") ||
               filename.endsWith(".config") || filename.endsWith(".xml") ||
               filename.endsWith(".json") || filename.endsWith(".yaml") ||
               filename.endsWith(".yml") || filename.endsWith(".toml") ||
               filename.endsWith(".properties");
    }
    
    private boolean isResourceFile(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        return filename.endsWith(".ico") || filename.endsWith(".png") ||
               filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
               filename.endsWith(".bmp") || filename.endsWith(".gif") ||
               filename.endsWith(".wav") || filename.endsWith(".mp3") ||
               filename.endsWith(".txt") || filename.endsWith(".dat") ||
               filename.endsWith(".res");
    }
    
    private String getResourceType(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        if (filename.contains(".ico") || filename.contains(".png") || filename.contains(".jpg") || filename.contains(".bmp")) {
            return "IMAGE";
        } else if (filename.contains(".wav") || filename.contains(".mp3")) {
            return "AUDIO";
        } else if (filename.contains(".txt") || filename.contains(".dat")) {
            return "DATA";
        }
        return "UNKNOWN";
    }
    
    private void generateHighLevelCode(BinaryAnalysisResult result) {
        HighLevelCodeGenerator generator = new HighLevelCodeGenerator();
        Map<String, String> generatedCode = generator.generateCode(result);
        result.setGeneratedCode(generatedCode);
    }
    
    private void initializeParsers() {
        parsers.put(BinaryFormat.PE, new PEParser());
        parsers.put(BinaryFormat.ELF, new ELFParser());
        parsers.put(BinaryFormat.MACHO, new MachOParser());
        parsers.put(BinaryFormat.JAVA_CLASS, new JavaClassParser());
        parsers.put(BinaryFormat.ZIP_BASED, new ZipBasedParser());
    }
    
    public enum BinaryFormat {
        PE,              // Windows PE (.exe, .dll)
        ELF,             // Linux/Unix ELF
        MACHO,           // macOS Mach-O
        JAVA_CLASS,      // Java .class files
        ZIP_BASED,       // JAR, WAR, APK, etc.
        UNKNOWN
    }
}