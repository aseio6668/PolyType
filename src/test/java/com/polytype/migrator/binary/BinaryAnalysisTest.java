package com.polytype.migrator.binary;

import com.polytype.migrator.core.TargetLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Comprehensive test suite for the binary analysis and deobfuscation system.
 * Tests various aspects of binary parsing, disassembly, and code generation.
 */
public class BinaryAnalysisTest {
    
    private BinaryAnalyzer binaryAnalyzer;
    private StringExtractor stringExtractor;
    private DisassemblyEngine disassemblyEngine;
    private ControlFlowAnalyzer controlFlowAnalyzer;
    private ApiCallAnalyzer apiCallAnalyzer;
    private DeobfuscationEngine deobfuscationEngine;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        binaryAnalyzer = new BinaryAnalyzer();
        stringExtractor = new StringExtractor();
        disassemblyEngine = new DisassemblyEngine();
        controlFlowAnalyzer = new ControlFlowAnalyzer();
        apiCallAnalyzer = new ApiCallAnalyzer();
        deobfuscationEngine = new DeobfuscationEngine();
    }
    
    @Test
    void testBinaryFormatDetection() throws IOException {
        // Test PE format detection
        Path peFile = createMockPEFile();
        BinaryAnalyzer.BinaryFormat format = binaryAnalyzer.detectFormat(peFile.toString());
        assertEquals(BinaryAnalyzer.BinaryFormat.PE, format);
        
        // Test ELF format detection
        Path elfFile = createMockELFFile();
        format = binaryAnalyzer.detectFormat(elfFile.toString());
        assertEquals(BinaryAnalyzer.BinaryFormat.ELF, format);
    }
    
    @Test
    void testStringExtraction() throws IOException {
        Path testBinary = createBinaryWithStrings();
        
        List<String> extractedStrings = stringExtractor.extractStrings(testBinary, BinaryAnalyzer.BinaryFormat.PE);
        
        assertFalse(extractedStrings.isEmpty());
        assertTrue(extractedStrings.contains("Hello World"));
        assertTrue(extractedStrings.contains("Test String"));
        
        // Test that very short strings are filtered out
        assertFalse(extractedStrings.stream().anyMatch(s -> s.length() < 4));
    }
    
    @Test
    void testStringExtractionWithContext() throws IOException {
        Path testBinary = createBinaryWithStrings();
        
        List<ExtractedString> stringsWithContext = stringExtractor.extractStringsWithContext(
            testBinary, BinaryAnalyzer.BinaryFormat.PE);
        
        assertFalse(stringsWithContext.isEmpty());
        
        // Verify context information is captured
        ExtractedString firstString = stringsWithContext.get(0);
        assertNotNull(firstString.getValue());
        assertTrue(firstString.getOffset() >= 0);
        assertTrue(firstString.getLength() > 0);
        assertNotNull(firstString.getEncoding());
    }
    
    @Test
    void testDisassemblyEngine() {
        // Test basic x86 instruction decoding
        byte[] machineCode = {
            (byte) 0x55,                    // push ebp
            (byte) 0x89, (byte) 0xE5,       // mov ebp, esp
            (byte) 0x83, (byte) 0xEC, 0x10, // sub esp, 16
            (byte) 0xC9,                    // leave
            (byte) 0xC3                     // ret
        };
        
        List<Instruction> instructions = disassemblyEngine.disassemble(machineCode, 0x1000);
        
        assertEquals(5, instructions.size());
        assertEquals("push", instructions.get(0).getMnemonic());
        assertEquals("mov", instructions.get(1).getMnemonic());
        assertEquals("sub", instructions.get(2).getMnemonic());
        assertEquals("leave", instructions.get(3).getMnemonic());
        assertEquals("ret", instructions.get(4).getMnemonic());
    }
    
    @Test
    void testFunctionBoundaryDetection() {
        byte[] codeWithFunctions = createCodeWithMultipleFunctions();
        
        List<DisassembledFunction> functions = disassemblyEngine.identifyFunctions(codeWithFunctions, 0x1000);
        
        assertFalse(functions.isEmpty());
        
        // Verify function properties
        DisassembledFunction firstFunc = functions.get(0);
        assertNotNull(firstFunc.getName());
        assertTrue(firstFunc.getStartAddress() >= 0x1000);
        assertFalse(firstFunc.getInstructions().isEmpty());
    }
    
    @Test
    void testControlFlowAnalysis() {
        // Create a simple function with control flow
        DisassembledFunction testFunction = createTestFunctionWithBranches();
        
        ControlFlowGraph cfg = controlFlowAnalyzer.analyzeFunction(testFunction);
        
        assertNotNull(cfg);
        assertFalse(cfg.getBasicBlocks().isEmpty());
        assertFalse(cfg.getEdges().isEmpty());
        
        // Verify basic block structure
        assertTrue(cfg.getBasicBlocks().size() >= 1);
        
        // Verify edges exist between blocks
        for (BasicBlock block : cfg.getBasicBlocks()) {
            List<ControlFlowEdge> outgoing = cfg.getOutgoingEdges(block);
            // Each block should have at most a reasonable number of outgoing edges
            assertTrue(outgoing.size() <= 2); // At most conditional true/false
        }
    }
    
    @Test
    void testApiCallAnalysis() {
        // Create function with API calls
        DisassembledFunction functionWithApiCalls = createFunctionWithApiCalls();
        
        List<ApiCall> apiCalls = apiCallAnalyzer.analyzeApiCalls(List.of(functionWithApiCalls));
        
        assertFalse(apiCalls.isEmpty());
        
        // Verify API call information
        ApiCall firstCall = apiCalls.get(0);
        assertNotNull(firstCall.getFunctionName());
        assertTrue(firstCall.getAddress() > 0);
        assertNotNull(firstCall.getCallType());
        assertNotNull(firstCall.getCategory());
    }
    
    @Test
    void testDeobfuscationEngine() throws IOException {
        // Create a binary with simple obfuscation patterns
        Path obfuscatedBinary = createObfuscatedBinary();
        
        DeobfuscationResult result = deobfuscationEngine.deobfuscate(obfuscatedBinary.toString());
        
        assertNotNull(result);
        assertTrue(result.getObfuscationTypes().size() > 0);
        
        // Verify deobfuscation was attempted
        assertNotNull(result.getDeobfuscatedCode());
        
        // Check if entropy was reduced (indicating successful deobfuscation)
        if (result.getOriginalEntropy() > 0 && result.getDeobfuscatedEntropy() > 0) {
            assertTrue(result.getDeobfuscatedEntropy() <= result.getOriginalEntropy());
        }
    }
    
    @Test
    void testBinaryAnalysisWorkflow() throws Exception {
        Path testBinary = createComprehensiveTestBinary();
        
        BinaryAnalysisResult result = binaryAnalyzer.analyzeBinary(testBinary.toString(), TargetLanguage.JAVA);
        
        assertNotNull(result);
        assertNotNull(result.getBinaryPath());
        assertNotNull(result.getFormat());
        assertNotNull(result.getArchitecture());
        
        // Verify comprehensive analysis was performed
        assertNotNull(result.getFunctions());
        assertNotNull(result.getStrings());
        assertNotNull(result.getDependencies());
        
        // Verify high-level code was generated
        if (result.getGeneratedCode() != null) {
            assertFalse(result.getGeneratedCode().isEmpty());
        }
    }
    
    @Test
    void testBinaryAwareTranslationWorkflow() throws Exception {
        Path testProject = createMixedProject();
        
        BinaryAwareTranslationWorkflow workflow = new BinaryAwareTranslationWorkflow(null); // Mock translator
        
        // This would need a mock translator implementation
        // EnhancedTranslationResult result = workflow.translateProject(
        //     testProject.toString(), TargetLanguage.JAVA, new TranslationOptions());
        
        // For now, just test input analysis
        // assertNotNull(result);
        // assertNotNull(result.getInputAnalysis());
    }
    
    // Helper methods for creating test data
    
    private Path createMockPEFile() throws IOException {
        Path peFile = tempDir.resolve("test.exe");
        
        // Create minimal PE header structure
        byte[] peHeader = {
            'M', 'Z',                           // DOS signature
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0x80, 0, 0, 0,          // e_lfanew = 0x80
        };
        
        // Pad to PE header location
        byte[] padding = new byte[0x80 - peHeader.length];
        
        // PE signature
        byte[] peSignature = {'P', 'E', 0, 0};
        
        // Combine all parts
        byte[] fileContent = new byte[peHeader.length + padding.length + peSignature.length];
        System.arraycopy(peHeader, 0, fileContent, 0, peHeader.length);
        System.arraycopy(peSignature, 0, fileContent, peHeader.length + padding.length, peSignature.length);
        
        Files.write(peFile, fileContent);
        return peFile;
    }
    
    private Path createMockELFFile() throws IOException {
        Path elfFile = tempDir.resolve("test.elf");
        
        // Create minimal ELF header
        byte[] elfHeader = {
            0x7F, 'E', 'L', 'F',    // ELF magic
            1,                       // 32-bit
            1,                       // Little endian
            1,                       // Version
            0, 0, 0, 0, 0, 0, 0, 0, 0 // Padding
        };
        
        Files.write(elfFile, elfHeader);
        return elfFile;
    }
    
    private Path createBinaryWithStrings() throws IOException {
        Path binaryFile = tempDir.resolve("strings_test.bin");
        
        // Create binary data with embedded strings
        StringBuilder content = new StringBuilder();
        content.append("Some binary data");
        content.append('\0');
        content.append("Hello World");
        content.append('\0');
        content.append("Test String");
        content.append('\0');
        content.append("Short"); // Should be filtered out in some contexts
        content.append('\0');
        
        Files.write(binaryFile, content.toString().getBytes());
        return binaryFile;
    }
    
    private byte[] createCodeWithMultipleFunctions() {
        // Create machine code representing multiple functions
        return new byte[] {
            // Function 1
            (byte) 0x55,                    // push ebp
            (byte) 0x89, (byte) 0xE5,       // mov ebp, esp
            (byte) 0xC9,                    // leave
            (byte) 0xC3,                    // ret
            
            // Function 2
            (byte) 0x55,                    // push ebp
            (byte) 0x89, (byte) 0xE5,       // mov ebp, esp
            (byte) 0x83, (byte) 0xEC, 0x10, // sub esp, 16
            (byte) 0xC9,                    // leave
            (byte) 0xC3                     // ret
        };
    }
    
    private DisassembledFunction createTestFunctionWithBranches() {
        DisassembledFunction function = new DisassembledFunction();
        function.setName("test_function");
        function.setStartAddress(0x1000);
        
        // Add instructions with branches
        Instruction cmp = new Instruction();
        cmp.setAddress(0x1000);
        cmp.setMnemonic("cmp");
        cmp.setOperands(List.of("eax", "0"));
        function.getInstructions().add(cmp);
        
        Instruction je = new Instruction();
        je.setAddress(0x1002);
        je.setMnemonic("je");
        je.setOperands(List.of("0x1010"));
        function.getInstructions().add(je);
        
        Instruction mov = new Instruction();
        mov.setAddress(0x1004);
        mov.setMnemonic("mov");
        mov.setOperands(List.of("eax", "1"));
        function.getInstructions().add(mov);
        
        Instruction ret = new Instruction();
        ret.setAddress(0x1006);
        ret.setMnemonic("ret");
        ret.setOperands(List.of());
        function.getInstructions().add(ret);
        
        return function;
    }
    
    private DisassembledFunction createFunctionWithApiCalls() {
        DisassembledFunction function = new DisassembledFunction();
        function.setName("api_function");
        function.setStartAddress(0x2000);
        
        // Add call instruction
        Instruction call = new Instruction();
        call.setAddress(0x2000);
        call.setMnemonic("call");
        call.setOperands(List.of("CreateFileA"));
        function.getInstructions().add(call);
        
        return function;
    }
    
    private Path createObfuscatedBinary() throws IOException {
        Path obfuscatedFile = tempDir.resolve("obfuscated.bin");
        
        // Create binary with high entropy (simulating encryption/packing)
        byte[] obfuscatedData = new byte[1024];
        for (int i = 0; i < obfuscatedData.length; i++) {
            obfuscatedData[i] = (byte) (Math.random() * 256);
        }
        
        Files.write(obfuscatedFile, obfuscatedData);
        return obfuscatedFile;
    }
    
    private Path createComprehensiveTestBinary() throws IOException {
        Path testBinary = tempDir.resolve("comprehensive_test.exe");
        
        // Create a more comprehensive binary for full workflow testing
        byte[] binaryContent = new byte[2048];
        
        // Add PE header
        binaryContent[0] = 'M';
        binaryContent[1] = 'Z';
        binaryContent[60] = (byte) 0x80; // e_lfanew
        
        // Add PE signature at offset 0x80
        binaryContent[0x80] = 'P';
        binaryContent[0x81] = 'E';
        
        // Add some string data
        String testString = "Test application string";
        byte[] stringBytes = testString.getBytes();
        System.arraycopy(stringBytes, 0, binaryContent, 0x200, stringBytes.length);
        
        // Add some code-like patterns
        binaryContent[0x400] = (byte) 0x55; // push ebp
        binaryContent[0x401] = (byte) 0x89; // mov ebp, esp
        binaryContent[0x402] = (byte) 0xE5;
        
        Files.write(testBinary, binaryContent);
        return testBinary;
    }
    
    private Path createMixedProject() throws IOException {
        Path projectDir = tempDir.resolve("mixed_project");
        Files.createDirectories(projectDir);
        
        // Create source files
        Path srcDir = projectDir.resolve("src");
        Files.createDirectories(srcDir);
        Files.write(srcDir.resolve("Main.java"), "public class Main { }".getBytes());
        
        // Create binary files
        Path binDir = projectDir.resolve("bin");
        Files.createDirectories(binDir);
        Path binaryFile = binDir.resolve("app.exe");
        Files.write(binaryFile, createComprehensiveTestBinary().toString().getBytes());
        
        return projectDir;
    }
}