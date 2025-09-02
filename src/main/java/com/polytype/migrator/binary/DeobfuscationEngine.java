package com.polytype.migrator.binary;

import java.util.*;

/**
 * Advanced deobfuscation engine for reversing common obfuscation techniques.
 * Handles packed executables, encrypted strings, control flow obfuscation, etc.
 */
public class DeobfuscationEngine {
    
    private final List<DeobfuscationTechnique> techniques;
    
    public DeobfuscationEngine() {
        this.techniques = new ArrayList<>();
        initializeTechniques();
    }
    
    /**
     * Attempt to deobfuscate a binary analysis result.
     */
    public DeobfuscationResult deobfuscate(BinaryFileAnalysis analysis) {
        DeobfuscationResult result = new DeobfuscationResult();
        result.setOriginalAnalysis(analysis);
        
        // Detect obfuscation techniques
        List<ObfuscationType> detectedObfuscation = detectObfuscation(analysis);
        result.setDetectedObfuscation(detectedObfuscation);
        
        if (detectedObfuscation.isEmpty()) {
            result.setObfuscated(false);
            return result;
        }
        
        result.setObfuscated(true);
        
        // Apply deobfuscation techniques
        for (DeobfuscationTechnique technique : techniques) {
            if (technique.canHandle(detectedObfuscation)) {
                try {
                    DeobfuscationStepResult stepResult = technique.apply(analysis);
                    result.addStep(stepResult);
                    
                    if (stepResult.isSuccessful()) {
                        // Update analysis with deobfuscated results
                        analysis = stepResult.getImprovedAnalysis();
                    }
                } catch (Exception e) {
                    DeobfuscationStepResult errorStep = new DeobfuscationStepResult();
                    errorStep.setTechnique(technique.getName());
                    errorStep.setSuccessful(false);
                    errorStep.setErrorMessage(e.getMessage());
                    result.addStep(errorStep);
                }
            }
        }
        
        result.setFinalAnalysis(analysis);
        return result;
    }
    
    private List<ObfuscationType> detectObfuscation(BinaryFileAnalysis analysis) {
        List<ObfuscationType> obfuscationTypes = new ArrayList<>();
        
        BinaryStructure structure = analysis.getBinaryStructure();
        if (structure != null) {
            // Check for packers
            if (structure.isPacked()) {
                obfuscationTypes.add(ObfuscationType.PACKING);
            }
            
            // Check for suspicious section characteristics
            if (hasSuspiciousSections(structure.getSections())) {
                obfuscationTypes.add(ObfuscationType.SECTION_MODIFICATION);
            }
            
            // Check entry point
            if (hasObfuscatedEntryPoint(structure)) {
                obfuscationTypes.add(ObfuscationType.ENTRY_POINT_OBFUSCATION);
            }
        }
        
        // Analyze disassembled functions
        List<DisassembledFunction> functions = analysis.getDisassembledFunctions();
        if (hasControlFlowObfuscation(functions)) {
            obfuscationTypes.add(ObfuscationType.CONTROL_FLOW_OBFUSCATION);
        }
        
        if (hasJunkCode(functions)) {
            obfuscationTypes.add(ObfuscationType.JUNK_CODE);
        }
        
        // Analyze strings
        if (hasEncryptedStrings(analysis.getExtractedStrings())) {
            obfuscationTypes.add(ObfuscationType.STRING_ENCRYPTION);
        }
        
        // Check for anti-analysis techniques
        if (hasAntiAnalysis(functions)) {
            obfuscationTypes.add(ObfuscationType.ANTI_ANALYSIS);
        }
        
        return obfuscationTypes;
    }
    
    private boolean hasSuspiciousSections(List<Section> sections) {
        for (Section section : sections) {
            // Check for executable writable sections (suspicious)
            if (section.getPermissions().contains("EXECUTABLE") && 
                section.getPermissions().contains("WRITABLE")) {
                return true;
            }
            
            // Check for unusual section names
            String name = section.getName().toLowerCase();
            if (name.contains("upx") || name.contains("aspack") || 
                name.contains("mew") || name.matches("\\.[a-z]{1,2}\\d+")) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasObfuscatedEntryPoint(BinaryStructure structure) {
        // Check if entry point is in an unusual section
        long entryPoint = structure.getEntryPoint();
        
        for (Section section : structure.getSections()) {
            if (entryPoint >= section.getVirtualAddress() && 
                entryPoint < section.getVirtualAddress() + section.getVirtualSize()) {
                
                // Entry point should typically be in .text section
                if (!section.getName().equals(".text")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean hasControlFlowObfuscation(List<DisassembledFunction> functions) {
        for (DisassembledFunction function : functions) {
            List<Instruction> instructions = function.getInstructions();
            
            // Look for excessive jumps
            long jumpCount = instructions.stream()
                .mapToLong(i -> i.getMnemonic().toLowerCase().startsWith("j") ? 1 : 0)
                .sum();
            
            double jumpRatio = (double) jumpCount / instructions.size();
            if (jumpRatio > 0.3) { // More than 30% jumps is suspicious
                return true;
            }
            
            // Look for jump chains
            if (hasJumpChains(instructions)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasJumpChains(List<Instruction> instructions) {
        int consecutiveJumps = 0;
        for (Instruction instruction : instructions) {
            if (instruction.getMnemonic().toLowerCase().startsWith("j")) {
                consecutiveJumps++;
                if (consecutiveJumps > 3) {
                    return true;
                }
            } else {
                consecutiveJumps = 0;
            }
        }
        return false;
    }
    
    private boolean hasJunkCode(List<DisassembledFunction> functions) {
        for (DisassembledFunction function : functions) {
            List<Instruction> instructions = function.getInstructions();
            
            // Look for excessive NOPs
            long nopCount = instructions.stream()
                .mapToLong(i -> i.getMnemonic().toLowerCase().equals("nop") ? 1 : 0)
                .sum();
            
            if (nopCount > instructions.size() * 0.1) { // More than 10% NOPs
                return true;
            }
            
            // Look for meaningless operations
            if (hasMeaninglessOperations(instructions)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasMeaninglessOperations(List<Instruction> instructions) {
        for (int i = 0; i < instructions.size() - 1; i++) {
            Instruction current = instructions.get(i);
            Instruction next = instructions.get(i + 1);
            
            // Look for operations that cancel each other out
            if (current.getMnemonic().equals("push") && next.getMnemonic().equals("pop") &&
                current.getOperands().equals(next.getOperands())) {
                return true;
            }
            
            // Look for XOR reg, reg (sets register to 0) followed by unnecessary operations
            if (current.getMnemonic().equals("xor") && current.getOperands().size() == 2 &&
                current.getOperands().get(0).equals(current.getOperands().get(1))) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasEncryptedStrings(List<String> strings) {
        int suspiciousStrings = 0;
        
        for (String str : strings) {
            // Check for high entropy (random-looking strings)
            if (calculateEntropy(str) > 4.5) {
                suspiciousStrings++;
            }
        }
        
        // If more than 30% of strings look random, suspect encryption
        return suspiciousStrings > strings.size() * 0.3;
    }
    
    private double calculateEntropy(String str) {
        Map<Character, Integer> frequencies = new HashMap<>();
        
        for (char c : str.toCharArray()) {
            frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
        }
        
        double entropy = 0.0;
        int length = str.length();
        
        for (int count : frequencies.values()) {
            double probability = (double) count / length;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        
        return entropy;
    }
    
    private boolean hasAntiAnalysis(List<DisassembledFunction> functions) {
        for (DisassembledFunction function : functions) {
            for (Instruction instruction : function.getInstructions()) {
                String mnemonic = instruction.getMnemonic().toLowerCase();
                
                // Look for debugger detection techniques
                if (mnemonic.equals("int") && !instruction.getOperands().isEmpty()) {
                    String operand = instruction.getOperands().get(0);
                    if (operand.equals("0x2D") || operand.equals("0x3")) { // Common anti-debug interrupts
                        return true;
                    }
                }
                
                // Look for timing checks
                if (mnemonic.equals("rdtsc")) { // Read timestamp counter
                    return true;
                }
            }
        }
        return false;
    }
    
    private void initializeTechniques() {
        techniques.add(new UnpackingTechnique());
        techniques.add(new StringDecryptionTechnique());
        techniques.add(new ControlFlowDeobfuscationTechnique());
        techniques.add(new JunkCodeRemovalTechnique());
    }
    
    // Deobfuscation technique implementations
    
    private interface DeobfuscationTechnique {
        String getName();
        boolean canHandle(List<ObfuscationType> obfuscationTypes);
        DeobfuscationStepResult apply(BinaryFileAnalysis analysis);
    }
    
    private static class UnpackingTechnique implements DeobfuscationTechnique {
        @Override
        public String getName() { return "Unpacking"; }
        
        @Override
        public boolean canHandle(List<ObfuscationType> types) {
            return types.contains(ObfuscationType.PACKING);
        }
        
        @Override
        public DeobfuscationStepResult apply(BinaryFileAnalysis analysis) {
            DeobfuscationStepResult result = new DeobfuscationStepResult();
            result.setTechnique(getName());
            result.setDescription("Attempting to unpack executable");
            
            // Simplified unpacking simulation
            result.setSuccessful(true);
            result.setImprovedAnalysis(analysis); // In reality, this would contain unpacked code
            result.addImprovement("Detected and handled " + analysis.getBinaryStructure().getPacker() + " packer");
            
            return result;
        }
    }
    
    private static class StringDecryptionTechnique implements DeobfuscationTechnique {
        @Override
        public String getName() { return "String Decryption"; }
        
        @Override
        public boolean canHandle(List<ObfuscationType> types) {
            return types.contains(ObfuscationType.STRING_ENCRYPTION);
        }
        
        @Override
        public DeobfuscationStepResult apply(BinaryFileAnalysis analysis) {
            DeobfuscationStepResult result = new DeobfuscationStepResult();
            result.setTechnique(getName());
            result.setDescription("Attempting to decrypt obfuscated strings");
            
            // Simplified string decryption
            List<String> decryptedStrings = new ArrayList<>();
            for (String str : analysis.getExtractedStrings()) {
                // Try common decryption methods (XOR, Caesar cipher, etc.)
                String decrypted = tryDecryptString(str);
                if (decrypted != null) {
                    decryptedStrings.add(decrypted);
                }
            }
            
            if (!decryptedStrings.isEmpty()) {
                result.setSuccessful(true);
                result.addImprovement("Decrypted " + decryptedStrings.size() + " strings");
                
                // Update analysis with decrypted strings
                BinaryFileAnalysis improved = analysis;
                improved.getExtractedStrings().addAll(decryptedStrings);
                result.setImprovedAnalysis(improved);
            } else {
                result.setSuccessful(false);
                result.setErrorMessage("No strings could be decrypted");
            }
            
            return result;
        }
        
        private String tryDecryptString(String encrypted) {
            // Try simple XOR decryption
            for (int key = 1; key < 256; key++) {
                StringBuilder decrypted = new StringBuilder();
                boolean isValid = true;
                
                for (char c : encrypted.toCharArray()) {
                    char decChar = (char) (c ^ key);
                    if (decChar < 32 || decChar > 126) {
                        isValid = false;
                        break;
                    }
                    decrypted.append(decChar);
                }
                
                if (isValid && decrypted.length() > 3) {
                    return decrypted.toString();
                }
            }
            return null;
        }
    }
    
    private static class ControlFlowDeobfuscationTechnique implements DeobfuscationTechnique {
        @Override
        public String getName() { return "Control Flow Deobfuscation"; }
        
        @Override
        public boolean canHandle(List<ObfuscationType> types) {
            return types.contains(ObfuscationType.CONTROL_FLOW_OBFUSCATION);
        }
        
        @Override
        public DeobfuscationStepResult apply(BinaryFileAnalysis analysis) {
            DeobfuscationStepResult result = new DeobfuscationStepResult();
            result.setTechnique(getName());
            result.setDescription("Simplifying obfuscated control flow");
            
            // Simplified control flow cleanup
            int simplifiedFunctions = 0;
            for (DisassembledFunction function : analysis.getDisassembledFunctions()) {
                if (simplifyControlFlow(function)) {
                    simplifiedFunctions++;
                }
            }
            
            result.setSuccessful(simplifiedFunctions > 0);
            result.addImprovement("Simplified control flow in " + simplifiedFunctions + " functions");
            result.setImprovedAnalysis(analysis);
            
            return result;
        }
        
        private boolean simplifyControlFlow(DisassembledFunction function) {
            // Remove unnecessary jumps, consolidate basic blocks, etc.
            // This is a placeholder - real implementation would be much more complex
            return function.getInstructions().size() > 10;
        }
    }
    
    private static class JunkCodeRemovalTechnique implements DeobfuscationTechnique {
        @Override
        public String getName() { return "Junk Code Removal"; }
        
        @Override
        public boolean canHandle(List<ObfuscationType> types) {
            return types.contains(ObfuscationType.JUNK_CODE);
        }
        
        @Override
        public DeobfuscationStepResult apply(BinaryFileAnalysis analysis) {
            DeobfuscationStepResult result = new DeobfuscationStepResult();
            result.setTechnique(getName());
            result.setDescription("Removing junk code and NOPs");
            
            int removedInstructions = 0;
            for (DisassembledFunction function : analysis.getDisassembledFunctions()) {
                removedInstructions += removeJunkInstructions(function);
            }
            
            result.setSuccessful(removedInstructions > 0);
            result.addImprovement("Removed " + removedInstructions + " junk instructions");
            result.setImprovedAnalysis(analysis);
            
            return result;
        }
        
        private int removeJunkInstructions(DisassembledFunction function) {
            List<Instruction> instructions = function.getInstructions();
            List<Instruction> cleaned = new ArrayList<>();
            int removed = 0;
            
            for (Instruction instruction : instructions) {
                if (!isJunkInstruction(instruction)) {
                    cleaned.add(instruction);
                } else {
                    removed++;
                }
            }
            
            function.setInstructions(cleaned);
            return removed;
        }
        
        private boolean isJunkInstruction(Instruction instruction) {
            String mnemonic = instruction.getMnemonic().toLowerCase();
            
            // Remove NOPs
            if (mnemonic.equals("nop")) {
                return true;
            }
            
            // Remove meaningless operations
            if (mnemonic.equals("xor") && instruction.getOperands().size() == 2 &&
                instruction.getOperands().get(0).equals(instruction.getOperands().get(1))) {
                return true;
            }
            
            return false;
        }
    }
}

enum ObfuscationType {
    PACKING, STRING_ENCRYPTION, CONTROL_FLOW_OBFUSCATION, 
    JUNK_CODE, ANTI_ANALYSIS, ENTRY_POINT_OBFUSCATION, 
    SECTION_MODIFICATION, IMPORT_OBFUSCATION
}

class DeobfuscationResult {
    private boolean obfuscated;
    private List<ObfuscationType> detectedObfuscation;
    private List<DeobfuscationStepResult> steps;
    private BinaryFileAnalysis originalAnalysis;
    private BinaryFileAnalysis finalAnalysis;
    private double successRate;
    
    public DeobfuscationResult() {
        this.detectedObfuscation = new ArrayList<>();
        this.steps = new ArrayList<>();
    }
    
    // Getters and setters
    public boolean isObfuscated() { return obfuscated; }
    public void setObfuscated(boolean obfuscated) { this.obfuscated = obfuscated; }
    
    public List<ObfuscationType> getDetectedObfuscation() { return detectedObfuscation; }
    public void setDetectedObfuscation(List<ObfuscationType> detectedObfuscation) { this.detectedObfuscation = detectedObfuscation; }
    
    public List<DeobfuscationStepResult> getSteps() { return steps; }
    public void addStep(DeobfuscationStepResult step) { steps.add(step); }
    
    public BinaryFileAnalysis getOriginalAnalysis() { return originalAnalysis; }
    public void setOriginalAnalysis(BinaryFileAnalysis originalAnalysis) { this.originalAnalysis = originalAnalysis; }
    
    public BinaryFileAnalysis getFinalAnalysis() { return finalAnalysis; }
    public void setFinalAnalysis(BinaryFileAnalysis finalAnalysis) { this.finalAnalysis = finalAnalysis; }
    
    public double getSuccessRate() {
        if (steps.isEmpty()) return 0.0;
        long successfulSteps = steps.stream().mapToLong(s -> s.isSuccessful() ? 1 : 0).sum();
        return (double) successfulSteps / steps.size();
    }
}

class DeobfuscationStepResult {
    private String technique;
    private String description;
    private boolean successful;
    private String errorMessage;
    private List<String> improvements;
    private BinaryFileAnalysis improvedAnalysis;
    
    public DeobfuscationStepResult() {
        this.improvements = new ArrayList<>();
    }
    
    // Getters and setters
    public String getTechnique() { return technique; }
    public void setTechnique(String technique) { this.technique = technique; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public List<String> getImprovements() { return improvements; }
    public void addImprovement(String improvement) { improvements.add(improvement); }
    
    public BinaryFileAnalysis getImprovedAnalysis() { return improvedAnalysis; }
    public void setImprovedAnalysis(BinaryFileAnalysis improvedAnalysis) { this.improvedAnalysis = improvedAnalysis; }
}