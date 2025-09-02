package com.polytype.migrator.binary;

import com.polytype.migrator.core.TargetLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Integration test demonstrating the complete binary analysis workflow.
 * This test shows how the various components work together to analyze
 * a binary file and generate high-level source code.
 */
public class BinaryWorkflowIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private BinaryAnalyzer binaryAnalyzer;
    private HighLevelCodeGenerator codeGenerator;
    
    @BeforeEach
    void setUp() {
        binaryAnalyzer = new BinaryAnalyzer();
        codeGenerator = new HighLevelCodeGenerator();
    }
    
    @Test
    void testCompleteAnalysisWorkflow() throws IOException {
        // Create a test binary
        Path testBinary = createTestExecutable();
        
        System.out.println("Testing complete binary analysis workflow...");
        
        // Step 1: Analyze the binary
        System.out.println("Step 1: Analyzing binary file...");
        BinaryAnalysisResult analysisResult = binaryAnalyzer.analyzeBinary(
            testBinary.toString(), TargetLanguage.JAVA);
        
        assertNotNull(analysisResult);
        assertNotNull(analysisResult.getBinaryPath());
        assertEquals(BinaryAnalyzer.BinaryFormat.PE, analysisResult.getFormat());
        
        System.out.println("Binary analysis completed:");
        System.out.println("  Format: " + analysisResult.getFormat());
        System.out.println("  Architecture: " + analysisResult.getArchitecture());
        System.out.println("  Functions found: " + analysisResult.getFunctions().size());
        System.out.println("  Strings found: " + analysisResult.getStrings().size());
        
        // Step 2: Generate high-level code
        System.out.println("\nStep 2: Generating high-level code...");
        Map<String, String> generatedCode = codeGenerator.generateCode(analysisResult, TargetLanguage.JAVA);
        
        assertNotNull(generatedCode);
        assertFalse(generatedCode.isEmpty());
        
        System.out.println("Generated files:");
        for (String fileName : generatedCode.keySet()) {
            System.out.println("  " + fileName);
        }
        
        // Step 3: Verify generated code structure
        System.out.println("\nStep 3: Verifying generated code...");
        
        // Should have main application class
        assertTrue(generatedCode.containsKey("src/main/java/com/reverse/engineered/Application.java"));
        
        // Should have utility classes
        assertTrue(generatedCode.keySet().stream().anyMatch(name -> name.contains("Utilities")));
        
        // Step 4: Write generated code to output directory
        System.out.println("\nStep 4: Writing generated files...");
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        for (Map.Entry<String, String> codeEntry : generatedCode.entrySet()) {
            Path codeFile = outputDir.resolve(codeEntry.getKey());
            Files.createDirectories(codeFile.getParent());
            Files.write(codeFile, codeEntry.getValue().getBytes());
        }
        
        // Verify files were written
        assertTrue(Files.exists(outputDir.resolve("src/main/java/com/reverse/engineered/Application.java")));
        
        System.out.println("Workflow completed successfully!");
        System.out.println("Generated files written to: " + outputDir);
    }
    
    @Test
    void testBinaryWithObfuscation() throws IOException {
        // Create a binary that appears obfuscated
        Path obfuscatedBinary = createObfuscatedTestBinary();
        
        System.out.println("\nTesting obfuscated binary analysis...");
        
        // Analyze the obfuscated binary
        BinaryAnalysisResult result = binaryAnalyzer.analyzeBinary(
            obfuscatedBinary.toString(), TargetLanguage.PYTHON);
        
        assertNotNull(result);
        
        if (result.isObfuscated()) {
            System.out.println("Obfuscation detected - applying deobfuscation techniques");
            assertNotNull(result.getDeobfuscationResult());
        }
        
        // Generate Python code
        Map<String, String> pythonCode = codeGenerator.generateCode(result, TargetLanguage.PYTHON);
        assertNotNull(pythonCode);
        
        // Should have main Python module
        assertTrue(pythonCode.containsKey("src/main.py"));
        
        System.out.println("Python code generation from obfuscated binary completed");
    }
    
    @Test
    void testDirectoryWithMultipleBinaries() throws IOException {
        // Create directory with multiple binary files
        Path binaryDir = tempDir.resolve("binaries");
        Files.createDirectories(binaryDir);
        
        // Create main executable
        Path mainExe = binaryDir.resolve("main.exe");
        Files.write(mainExe, createTestBinaryContent("Main Application"));
        
        // Create DLL dependency
        Path dependency = binaryDir.resolve("helper.dll");
        Files.write(dependency, createTestBinaryContent("Helper Library"));
        
        System.out.println("\nTesting directory with multiple binaries...");
        
        // Analyze the entire directory
        BinaryAnalysisResult result = binaryAnalyzer.analyzeBinary(
            binaryDir.toString(), TargetLanguage.CPP);
        
        assertNotNull(result);
        assertNotNull(result.getDependencies());
        
        // Should detect both binaries
        assertTrue(result.getDependencies().size() >= 1);
        
        System.out.println("Directory analysis completed:");
        System.out.println("  Dependencies found: " + result.getDependencies().size());
        
        // Generate C++ code
        Map<String, String> cppCode = codeGenerator.generateCode(result, TargetLanguage.CPP);
        assertNotNull(cppCode);
        
        // Should have main.cpp and CMakeLists.txt
        assertTrue(cppCode.containsKey("src/main.cpp"));
        assertTrue(cppCode.containsKey("CMakeLists.txt"));
        
        System.out.println("C++ project generation completed");
    }
    
    private Path createTestExecutable() throws IOException {
        Path executable = tempDir.resolve("test_app.exe");
        byte[] content = createTestBinaryContent("Test Application");
        Files.write(executable, content);
        return executable;
    }
    
    private Path createObfuscatedTestBinary() throws IOException {
        Path obfuscated = tempDir.resolve("obfuscated.exe");
        
        // Create binary with high entropy to simulate obfuscation
        byte[] content = new byte[2048];
        
        // Add PE header
        content[0] = 'M';
        content[1] = 'Z';
        content[60] = (byte) 0x80;
        content[0x80] = 'P';
        content[0x81] = 'E';
        
        // Fill with high-entropy data to simulate encryption
        for (int i = 0x100; i < content.length; i++) {
            content[i] = (byte) (Math.random() * 256);
        }
        
        Files.write(obfuscated, content);
        return obfuscated;
    }
    
    private byte[] createTestBinaryContent(String appName) {
        byte[] content = new byte[1024];
        
        // Minimal PE header
        content[0] = 'M';
        content[1] = 'Z';
        content[60] = (byte) 0x80; // e_lfanew
        
        // PE signature
        content[0x80] = 'P';
        content[0x81] = 'E';
        
        // Add the application name as a string
        byte[] nameBytes = appName.getBytes();
        System.arraycopy(nameBytes, 0, content, 0x200, nameBytes.length);
        
        // Add some simple code patterns
        content[0x300] = (byte) 0x55; // push ebp
        content[0x301] = (byte) 0x89; // mov ebp, esp  
        content[0x302] = (byte) 0xE5;
        content[0x303] = (byte) 0xC3; // ret
        
        return content;
    }
    
    @Test
    void demonstrateFullWorkflowOutput() throws IOException {
        System.out.println("\n=== DEMONSTRATION OF POLYTYPE BINARY ANALYSIS ===");
        
        // Create a sample binary
        Path sampleBinary = createComprehensiveTestBinary();
        
        System.out.println("Input: " + sampleBinary.getFileName());
        System.out.println("Target Language: Java");
        System.out.println();
        
        // Run analysis
        BinaryAnalysisResult result = binaryAnalyzer.analyzeBinary(
            sampleBinary.toString(), TargetLanguage.JAVA);
        
        // Display results
        System.out.println("ANALYSIS RESULTS:");
        System.out.println("================");
        System.out.println("Binary Format: " + result.getFormat());
        System.out.println("Architecture: " + result.getArchitecture());
        System.out.println("Obfuscated: " + (result.isObfuscated() ? "Yes" : "No"));
        System.out.println("Functions Identified: " + result.getFunctions().size());
        System.out.println("Strings Extracted: " + result.getStrings().size());
        System.out.println("Dependencies Found: " + result.getDependencies().size());
        System.out.println();
        
        // Generate code
        Map<String, String> javaCode = codeGenerator.generateCode(result, TargetLanguage.JAVA);
        
        System.out.println("GENERATED JAVA PROJECT:");
        System.out.println("======================");
        for (String fileName : javaCode.keySet()) {
            System.out.println("📄 " + fileName);
        }
        System.out.println();
        
        // Show sample of generated main class
        String mainClass = javaCode.get("src/main/java/com/reverse/engineered/Application.java");
        if (mainClass != null) {
            System.out.println("SAMPLE GENERATED CODE (Application.java):");
            System.out.println("=========================================");
            System.out.println(mainClass.substring(0, Math.min(500, mainClass.length())));
            if (mainClass.length() > 500) {
                System.out.println("... [truncated]");
            }
        }
        
        System.out.println("\n✅ Binary analysis and code generation completed successfully!");
    }
    
    private Path createComprehensiveTestBinary() throws IOException {
        Path binary = tempDir.resolve("comprehensive_app.exe");
        
        byte[] content = new byte[4096];
        
        // PE Header
        content[0] = 'M'; content[1] = 'Z';
        content[60] = (byte) 0x80;
        content[0x80] = 'P'; content[0x81] = 'E';
        
        // Add various strings
        String[] testStrings = {
            "Main Application",
            "Configuration File",
            "Error: Invalid input",
            "Success: Operation completed"
        };
        
        int stringOffset = 0x200;
        for (String str : testStrings) {
            byte[] strBytes = str.getBytes();
            System.arraycopy(strBytes, 0, content, stringOffset, strBytes.length);
            stringOffset += strBytes.length + 1; // null terminator
        }
        
        // Add function-like code patterns
        int codeOffset = 0x400;
        content[codeOffset++] = (byte) 0x55; // push ebp
        content[codeOffset++] = (byte) 0x89; content[codeOffset++] = (byte) 0xE5; // mov ebp, esp
        content[codeOffset++] = (byte) 0x83; content[codeOffset++] = (byte) 0xEC; content[codeOffset++] = 0x10; // sub esp, 16
        content[codeOffset++] = (byte) 0xE8; content[codeOffset++] = 0x10; content[codeOffset++] = 0x00; 
        content[codeOffset++] = 0x00; content[codeOffset++] = 0x00; // call function
        content[codeOffset++] = (byte) 0xC9; // leave
        content[codeOffset++] = (byte) 0xC3; // ret
        
        Files.write(binary, content);
        return binary;
    }
}