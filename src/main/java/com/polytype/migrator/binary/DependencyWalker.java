package com.polytype.migrator.binary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Advanced dependency walker for analyzing DLL dependencies and API imports.
 * Recursively analyzes all dependencies to build a complete dependency graph.
 */
public class DependencyWalker {
    
    private final Set<String> analyzedFiles;
    private final Map<String, List<String>> dependencyCache;
    
    public DependencyWalker() {
        this.analyzedFiles = new HashSet<>();
        this.dependencyCache = new HashMap<>();
    }
    
    /**
     * Walk dependencies starting from a main executable.
     */
    public List<String> walkDependencies(Path mainExecutable) throws IOException {
        List<String> allDependencies = new ArrayList<>();
        Queue<String> toAnalyze = new LinkedList<>();
        
        // Start with main executable
        toAnalyze.offer(mainExecutable.toString());
        
        while (!toAnalyze.isEmpty()) {
            String currentFile = toAnalyze.poll();
            
            if (analyzedFiles.contains(currentFile)) {
                continue;
            }
            
            analyzedFiles.add(currentFile);
            
            try {
                List<String> dependencies = getDirectDependencies(Paths.get(currentFile));
                
                for (String dependency : dependencies) {
                    if (!allDependencies.contains(dependency)) {
                        allDependencies.add(dependency);
                        
                        // Try to find the dependency file for further analysis
                        Path depPath = findDependencyPath(dependency, mainExecutable.getParent());
                        if (depPath != null) {
                            toAnalyze.offer(depPath.toString());
                        }
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Warning: Could not analyze dependencies for " + currentFile + ": " + e.getMessage());
            }
        }
        
        return allDependencies;
    }
    
    /**
     * Get direct dependencies of a single file.
     */
    public List<String> getDirectDependencies(Path file) throws IOException {
        String filePath = file.toString();
        
        if (dependencyCache.containsKey(filePath)) {
            return dependencyCache.get(filePath);
        }
        
        List<String> dependencies = new ArrayList<>();
        
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            // Check if it's a PE file
            if (isPEFile(raf)) {
                dependencies = extractPEDependencies(raf);
            } else if (isELFFile(raf)) {
                dependencies = extractELFDependencies(raf);
            }
        }
        
        dependencyCache.put(filePath, dependencies);
        return dependencies;
    }
    
    private boolean isPEFile(RandomAccessFile file) throws IOException {
        file.seek(0);
        return file.readShort() == (short) 0x5A4D; // "MZ"
    }
    
    private boolean isELFFile(RandomAccessFile file) throws IOException {
        file.seek(0);
        byte[] magic = new byte[4];
        file.readFully(magic);
        return magic[0] == 0x7F && magic[1] == 'E' && magic[2] == 'L' && magic[3] == 'F';
    }
    
    private List<String> extractPEDependencies(RandomAccessFile file) throws IOException {
        List<String> dependencies = new ArrayList<>();
        
        // Read DOS header to get PE offset
        file.seek(60);
        int peOffset = Integer.reverseBytes(file.readInt());
        
        // Read PE header
        file.seek(peOffset + 4); // Skip PE signature
        file.skipBytes(2); // Skip machine
        int numberOfSections = Short.reverseBytes(file.readShort()) & 0xFFFF;
        file.skipBytes(12); // Skip timestamp, symbol table info
        int sizeOfOptionalHeader = Short.reverseBytes(file.readShort()) & 0xFFFF;
        
        if (sizeOfOptionalHeader > 0) {
            // Skip to data directories
            file.skipBytes(2); // characteristics
            file.skipBytes(2); // magic
            file.skipBytes(2); // version
            file.skipBytes(16); // size fields and addresses
            
            // Read image base
            int imageBase = Integer.reverseBytes(file.readInt());
            file.skipBytes(24); // Skip alignment, version, size fields
            file.skipBytes(2); // subsystem
            file.skipBytes(2); // dll characteristics
            file.skipBytes(20); // stack and heap sizes
            file.skipBytes(4); // loader flags
            int numberOfRvaAndSizes = Integer.reverseBytes(file.readInt());
            
            if (numberOfRvaAndSizes > 1) {
                // Skip export table
                file.skipBytes(8);
                
                // Read import table RVA and size
                int importTableRva = Integer.reverseBytes(file.readInt());
                int importTableSize = Integer.reverseBytes(file.readInt());
                
                if (importTableRva > 0 && importTableSize > 0) {
                    // Parse sections to find import table
                    long currentPos = file.getFilePointer();
                    
                    // Skip to section headers
                    file.seek(peOffset + 24 + sizeOfOptionalHeader);
                    
                    // Find section containing import table
                    for (int i = 0; i < numberOfSections; i++) {
                        file.skipBytes(8); // name
                        int virtualSize = Integer.reverseBytes(file.readInt());
                        int virtualAddress = Integer.reverseBytes(file.readInt());
                        int sizeOfRawData = Integer.reverseBytes(file.readInt());
                        int pointerToRawData = Integer.reverseBytes(file.readInt());
                        file.skipBytes(16); // relocations, line numbers, characteristics
                        
                        if (importTableRva >= virtualAddress && 
                            importTableRva < virtualAddress + virtualSize) {
                            
                            // Found the section, parse import table
                            long importTableOffset = pointerToRawData + (importTableRva - virtualAddress);
                            dependencies.addAll(parseImportTable(file, importTableOffset, virtualAddress, pointerToRawData));
                            break;
                        }
                    }
                }
            }
        }
        
        return dependencies;
    }
    
    private List<String> parseImportTable(RandomAccessFile file, long importTableOffset, 
                                         int sectionVirtualAddress, int sectionRawAddress) throws IOException {
        List<String> dependencies = new ArrayList<>();
        
        file.seek(importTableOffset);
        
        while (true) {
            // Read import descriptor
            int originalFirstThunk = Integer.reverseBytes(file.readInt());
            int timeDateStamp = Integer.reverseBytes(file.readInt());
            int forwarderChain = Integer.reverseBytes(file.readInt());
            int nameRva = Integer.reverseBytes(file.readInt());
            int firstThunk = Integer.reverseBytes(file.readInt());
            
            // End of import table
            if (nameRva == 0) break;
            
            // Read DLL name
            long currentPos = file.getFilePointer();
            long nameOffset = sectionRawAddress + (nameRva - sectionVirtualAddress);
            
            try {
                file.seek(nameOffset);
                StringBuilder dllName = new StringBuilder();
                int b;
                while ((b = file.read()) != 0 && b != -1 && dllName.length() < 256) {
                    dllName.append((char) b);
                }
                
                if (dllName.length() > 0) {
                    dependencies.add(dllName.toString());
                }
                
                file.seek(currentPos);
            } catch (IOException e) {
                // Skip invalid entries
                break;
            }
        }
        
        return dependencies;
    }
    
    private List<String> extractELFDependencies(RandomAccessFile file) throws IOException {
        List<String> dependencies = new ArrayList<>();
        
        // Basic ELF dependency extraction
        // This is a simplified implementation
        file.seek(0);
        byte[] header = new byte[64];
        file.readFully(header);
        
        // For now, return empty list - full ELF parsing would be very complex
        return dependencies;
    }
    
    /**
     * Find the actual path of a dependency DLL/SO file.
     */
    private Path findDependencyPath(String dependencyName, Path baseDirectory) {
        // Common search locations
        List<Path> searchPaths = Arrays.asList(
            baseDirectory,
            baseDirectory.resolve("lib"),
            baseDirectory.resolve("bin"),
            Paths.get(System.getProperty("java.library.path", "")),
            Paths.get("C:\\Windows\\System32"),
            Paths.get("C:\\Windows\\SysWOW64"),
            Paths.get("/lib"),
            Paths.get("/usr/lib"),
            Paths.get("/usr/local/lib")
        );
        
        for (Path searchPath : searchPaths) {
            if (searchPath == null || !searchPath.toFile().exists()) continue;
            
            Path candidate = searchPath.resolve(dependencyName);
            if (candidate.toFile().exists()) {
                return candidate;
            }
        }
        
        return null;
    }
    
    /**
     * Get dependency analysis including version info and API usage.
     */
    public DependencyAnalysisResult analyzeDependency(Path dependencyFile) throws IOException {
        DependencyAnalysisResult result = new DependencyAnalysisResult();
        result.setFilePath(dependencyFile.toString());
        result.setFileName(dependencyFile.getFileName().toString());
        result.setFileSize(dependencyFile.toFile().length());
        
        // Extract version information
        result.setVersionInfo(extractVersionInfo(dependencyFile));
        
        // Get exported functions
        result.setExportedFunctions(extractExports(dependencyFile));
        
        // Analyze file properties
        result.setDependencyType(determineDependencyType(dependencyFile));
        
        return result;
    }
    
    private Map<String, String> extractVersionInfo(Path file) {
        Map<String, String> versionInfo = new HashMap<>();
        
        // Placeholder implementation
        String fileName = file.getFileName().toString();
        if (fileName.contains("msvcr")) {
            versionInfo.put("description", "Microsoft Visual C++ Runtime");
        } else if (fileName.contains("kernel32")) {
            versionInfo.put("description", "Windows Kernel API");
        } else if (fileName.contains("user32")) {
            versionInfo.put("description", "Windows User Interface API");
        }
        
        return versionInfo;
    }
    
    private List<String> extractExports(Path file) {
        // Simplified export extraction
        return new ArrayList<>();
    }
    
    private DependencyType determineDependencyType(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        
        if (fileName.contains("msvcr") || fileName.contains("ucrtbase")) {
            return DependencyType.RUNTIME;
        } else if (fileName.contains("kernel32") || fileName.contains("ntdll")) {
            return DependencyType.SYSTEM;
        } else if (fileName.contains("opengl") || fileName.contains("d3d")) {
            return DependencyType.GRAPHICS;
        } else if (fileName.contains("ws2_32") || fileName.contains("wininet")) {
            return DependencyType.NETWORK;
        }
        
        return DependencyType.LIBRARY;
    }
    
    public enum DependencyType {
        SYSTEM, RUNTIME, LIBRARY, GRAPHICS, NETWORK, DATABASE, UNKNOWN
    }
}

/**
 * Analysis result for a single dependency.
 */
class DependencyAnalysisResult {
    private String filePath;
    private String fileName;
    private long fileSize;
    private Map<String, String> versionInfo;
    private List<String> exportedFunctions;
    private DependencyWalker.DependencyType dependencyType;
    private boolean isOptional;
    private String description;
    
    public DependencyAnalysisResult() {
        this.versionInfo = new HashMap<>();
        this.exportedFunctions = new ArrayList<>();
        this.isOptional = false;
    }
    
    // Getters and setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public Map<String, String> getVersionInfo() { return versionInfo; }
    public void setVersionInfo(Map<String, String> versionInfo) { this.versionInfo = versionInfo; }
    
    public List<String> getExportedFunctions() { return exportedFunctions; }
    public void setExportedFunctions(List<String> exportedFunctions) { this.exportedFunctions = exportedFunctions; }
    
    public DependencyWalker.DependencyType getDependencyType() { return dependencyType; }
    public void setDependencyType(DependencyWalker.DependencyType dependencyType) { this.dependencyType = dependencyType; }
    
    public boolean isOptional() { return isOptional; }
    public void setOptional(boolean optional) { isOptional = optional; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}