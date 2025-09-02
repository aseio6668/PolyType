package com.polytype.migrator.binary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.*;

/**
 * Advanced disassembly engine for x86/x64 code.
 * Converts machine code to assembly instructions and performs basic analysis.
 */
public class DisassemblyEngine {
    
    private final Map<String, InstructionDecoder> decoders;
    
    public DisassemblyEngine() {
        this.decoders = new HashMap<>();
        initializeDecoders();
    }
    
    /**
     * Disassemble code sections from a binary file.
     */
    public List<DisassembledFunction> disassemble(Path binaryFile, BinaryStructure structure) throws IOException {
        List<DisassembledFunction> functions = new ArrayList<>();
        
        // Find code sections
        List<Section> codeSections = structure.getSections().stream()
            .filter(s -> s.getType() == Section.SectionType.CODE)
            .toList();
        
        try (RandomAccessFile file = new RandomAccessFile(binaryFile.toFile(), "r")) {
            for (Section section : codeSections) {
                List<DisassembledFunction> sectionFunctions = disassembleSection(file, section, structure.getArchitecture());
                functions.addAll(sectionFunctions);
            }
        }
        
        return functions;
    }
    
    private List<DisassembledFunction> disassembleSection(RandomAccessFile file, Section section, String architecture) throws IOException {
        List<DisassembledFunction> functions = new ArrayList<>();
        
        file.seek(section.getRawAddress());
        byte[] sectionData = new byte[(int) section.getRawSize()];
        file.readFully(sectionData);
        
        InstructionDecoder decoder = decoders.get(architecture);
        if (decoder == null) {
            decoder = decoders.get("x86"); // Fallback to x86
        }
        
        // Simple function identification by scanning for function prologs
        List<Long> functionStarts = findFunctionStarts(sectionData, section.getVirtualAddress());
        
        for (int i = 0; i < functionStarts.size(); i++) {
            long functionStart = functionStarts.get(i);
            long functionEnd = (i < functionStarts.size() - 1) ? 
                functionStarts.get(i + 1) : section.getVirtualAddress() + section.getVirtualSize();
            
            DisassembledFunction function = disassembleFunction(sectionData, decoder, functionStart, functionEnd, section);
            if (function.getInstructions().size() > 0) {
                functions.add(function);
            }
        }
        
        return functions;
    }
    
    private List<Long> findFunctionStarts(byte[] data, long baseAddress) {
        List<Long> starts = new ArrayList<>();
        
        // Add entry point as first function
        starts.add(baseAddress);
        
        // Look for common function prologs
        for (int i = 0; i < data.length - 3; i++) {
            // x86 function prolog patterns
            if (isX86FunctionProlog(data, i)) {
                starts.add(baseAddress + i);
            }
            // x64 function prolog patterns
            else if (isX64FunctionProlog(data, i)) {
                starts.add(baseAddress + i);
            }
        }
        
        // Remove duplicates and sort
        return starts.stream().distinct().sorted().toList();
    }
    
    private boolean isX86FunctionProlog(byte[] data, int offset) {
        if (offset + 2 >= data.length) return false;
        
        // push ebp; mov ebp, esp
        return (data[offset] & 0xFF) == 0x55 && 
               (data[offset + 1] & 0xFF) == 0x8B && 
               (data[offset + 2] & 0xFF) == 0xEC;
    }
    
    private boolean isX64FunctionProlog(byte[] data, int offset) {
        if (offset + 3 >= data.length) return false;
        
        // push rbp; mov rbp, rsp (REX.W + 8B /r)
        return (data[offset] & 0xFF) == 0x48 &&
               (data[offset + 1] & 0xFF) == 0x8B &&
               ((data[offset + 2] & 0xFF) == 0xEC || (data[offset + 2] & 0xFF) == 0xE5);
    }
    
    private DisassembledFunction disassembleFunction(byte[] data, InstructionDecoder decoder, 
                                                    long startAddress, long endAddress, Section section) {
        DisassembledFunction function = new DisassembledFunction();
        function.setStartAddress(startAddress);
        function.setEndAddress(endAddress);
        function.setName("sub_" + Long.toHexString(startAddress));
        
        List<Instruction> instructions = new ArrayList<>();
        
        long currentAddress = startAddress;
        int dataOffset = (int) (startAddress - section.getVirtualAddress());
        
        while (currentAddress < endAddress && dataOffset < data.length) {
            try {
                InstructionDecodeResult result = decoder.decodeInstruction(data, dataOffset, currentAddress);
                if (result == null) break;
                
                instructions.add(result.instruction);
                currentAddress += result.length;
                dataOffset += result.length;
                
                // Stop at return instructions
                if (result.instruction.getMnemonic().startsWith("ret")) {
                    break;
                }
                
                // Stop at obviously invalid instructions
                if (result.length <= 0 || result.length > 15) {
                    break;
                }
                
            } catch (Exception e) {
                // Skip invalid instruction
                currentAddress++;
                dataOffset++;
            }
        }
        
        function.setInstructions(instructions);
        return function;
    }
    
    private void initializeDecoders() {
        decoders.put("x86", new X86InstructionDecoder());
        decoders.put("x64", new X64InstructionDecoder());
    }
    
    /**
     * Basic x86 instruction decoder.
     * This is a simplified implementation - a full decoder would be much more complex.
     */
    private static class X86InstructionDecoder implements InstructionDecoder {
        
        @Override
        public InstructionDecodeResult decodeInstruction(byte[] data, int offset, long address) {
            if (offset >= data.length) return null;
            
            int opcode = data[offset] & 0xFF;
            Instruction instruction = new Instruction();
            instruction.setAddress(address);
            instruction.setRawBytes(new byte[]{data[offset]});
            
            // Simplified instruction decoding
            switch (opcode) {
                case 0x50: case 0x51: case 0x52: case 0x53:
                case 0x54: case 0x55: case 0x56: case 0x57:
                    instruction.setMnemonic("push");
                    instruction.setOperands(List.of(getRegisterName(opcode - 0x50)));
                    return new InstructionDecodeResult(instruction, 1);
                
                case 0x58: case 0x59: case 0x5A: case 0x5B:
                case 0x5C: case 0x5D: case 0x5E: case 0x5F:
                    instruction.setMnemonic("pop");
                    instruction.setOperands(List.of(getRegisterName(opcode - 0x58)));
                    return new InstructionDecodeResult(instruction, 1);
                
                case 0xB8: case 0xB9: case 0xBA: case 0xBB:
                case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                    if (offset + 4 < data.length) {
                        instruction.setMnemonic("mov");
                        int immediate = getInt32(data, offset + 1);
                        instruction.setOperands(List.of(getRegisterName(opcode - 0xB8), String.format("0x%08X", immediate)));
                        return new InstructionDecodeResult(instruction, 5);
                    }
                    break;
                
                case 0xC3:
                    instruction.setMnemonic("ret");
                    instruction.setOperands(List.of());
                    return new InstructionDecodeResult(instruction, 1);
                
                case 0xE8:
                    if (offset + 4 < data.length) {
                        instruction.setMnemonic("call");
                        int displacement = getInt32(data, offset + 1);
                        long target = address + 5 + displacement;
                        instruction.setOperands(List.of(String.format("0x%08X", target)));
                        return new InstructionDecodeResult(instruction, 5);
                    }
                    break;
                
                case 0xEB:
                    if (offset + 1 < data.length) {
                        instruction.setMnemonic("jmp");
                        byte displacement = data[offset + 1];
                        long target = address + 2 + displacement;
                        instruction.setOperands(List.of(String.format("0x%08X", target)));
                        return new InstructionDecodeResult(instruction, 2);
                    }
                    break;
                
                case 0x90:
                    instruction.setMnemonic("nop");
                    instruction.setOperands(List.of());
                    return new InstructionDecodeResult(instruction, 1);
                
                default:
                    instruction.setMnemonic("db");
                    instruction.setOperands(List.of(String.format("0x%02X", opcode)));
                    return new InstructionDecodeResult(instruction, 1);
            }
            
            // Default case
            instruction.setMnemonic("db");
            instruction.setOperands(List.of(String.format("0x%02X", opcode)));
            return new InstructionDecodeResult(instruction, 1);
        }
        
        private String getRegisterName(int regNum) {
            String[] registers = {"eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi"};
            return (regNum >= 0 && regNum < registers.length) ? registers[regNum] : "unknown";
        }
        
        private int getInt32(byte[] data, int offset) {
            return (data[offset] & 0xFF) |
                   ((data[offset + 1] & 0xFF) << 8) |
                   ((data[offset + 2] & 0xFF) << 16) |
                   ((data[offset + 3] & 0xFF) << 24);
        }
    }
    
    /**
     * Basic x64 instruction decoder (extends x86).
     */
    private static class X64InstructionDecoder extends X86InstructionDecoder {
        // x64 specific decoding would be implemented here
        // For now, inherits x86 behavior
    }
    
    private interface InstructionDecoder {
        InstructionDecodeResult decodeInstruction(byte[] data, int offset, long address);
    }
    
    private static class InstructionDecodeResult {
        final Instruction instruction;
        final int length;
        
        InstructionDecodeResult(Instruction instruction, int length) {
            this.instruction = instruction;
            this.length = length;
        }
    }
}

/**
 * Represents a disassembled function.
 */
class DisassembledFunction {
    private long startAddress;
    private long endAddress;
    private String name;
    private List<Instruction> instructions;
    private ControlFlowGraph controlFlowGraph;
    private FunctionType type;
    private List<String> calledFunctions;
    private Map<String, Object> attributes;
    
    public DisassembledFunction() {
        this.instructions = new ArrayList<>();
        this.calledFunctions = new ArrayList<>();
        this.attributes = new HashMap<>();
        this.type = FunctionType.UNKNOWN;
    }
    
    // Getters and setters
    public long getStartAddress() { return startAddress; }
    public void setStartAddress(long startAddress) { this.startAddress = startAddress; }
    
    public long getEndAddress() { return endAddress; }
    public void setEndAddress(long endAddress) { this.endAddress = endAddress; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<Instruction> getInstructions() { return instructions; }
    public void setInstructions(List<Instruction> instructions) { this.instructions = instructions; }
    
    public ControlFlowGraph getControlFlowGraph() { return controlFlowGraph; }
    public void setControlFlowGraph(ControlFlowGraph controlFlowGraph) { this.controlFlowGraph = controlFlowGraph; }
    
    public FunctionType getType() { return type; }
    public void setType(FunctionType type) { this.type = type; }
    
    public List<String> getCalledFunctions() { return calledFunctions; }
    public void setCalledFunctions(List<String> calledFunctions) { this.calledFunctions = calledFunctions; }
    
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    
    public enum FunctionType {
        MAIN, API_WRAPPER, UTILITY, CONSTRUCTOR, DESTRUCTOR, UNKNOWN
    }
}

/**
 * Represents a single assembly instruction.
 */
class Instruction {
    private long address;
    private String mnemonic;
    private List<String> operands;
    private byte[] rawBytes;
    private InstructionType type;
    private Map<String, Object> metadata;
    
    public Instruction() {
        this.operands = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.type = InstructionType.UNKNOWN;
    }
    
    // Getters and setters
    public long getAddress() { return address; }
    public void setAddress(long address) { this.address = address; }
    
    public String getMnemonic() { return mnemonic; }
    public void setMnemonic(String mnemonic) { this.mnemonic = mnemonic; }
    
    public List<String> getOperands() { return operands; }
    public void setOperands(List<String> operands) { this.operands = operands; }
    
    public byte[] getRawBytes() { return rawBytes; }
    public void setRawBytes(byte[] rawBytes) { this.rawBytes = rawBytes; }
    
    public InstructionType getType() { return type; }
    public void setType(InstructionType type) { this.type = type; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    @Override
    public String toString() {
        return String.format("0x%08X: %s %s", address, mnemonic, String.join(", ", operands));
    }
    
    public enum InstructionType {
        ARITHMETIC, LOGICAL, MEMORY, CONTROL_FLOW, SYSTEM, UNKNOWN
    }
}