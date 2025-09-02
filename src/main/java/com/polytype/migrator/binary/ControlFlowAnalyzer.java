package com.polytype.migrator.binary;

import java.util.*;

/**
 * Advanced control flow analyzer that reconstructs program flow from assembly code.
 * Builds control flow graphs for better understanding of program logic.
 */
public class ControlFlowAnalyzer {
    
    /**
     * Analyze control flow for a function.
     */
    public ControlFlowGraph analyzeFunction(DisassembledFunction function) {
        ControlFlowGraph cfg = new ControlFlowGraph();
        cfg.setFunction(function);
        
        List<Instruction> instructions = function.getInstructions();
        if (instructions.isEmpty()) {
            return cfg;
        }
        
        // Build basic blocks
        List<BasicBlock> basicBlocks = buildBasicBlocks(instructions);
        cfg.setBasicBlocks(basicBlocks);
        
        // Build edges between blocks
        buildControlFlowEdges(cfg, basicBlocks);
        
        // Identify loops
        identifyLoops(cfg);
        
        // Analyze function calls
        analyzeFunctionCalls(cfg, function);
        
        // Detect common patterns
        detectPatterns(cfg);
        
        return cfg;
    }
    
    private List<BasicBlock> buildBasicBlocks(List<Instruction> instructions) {
        List<BasicBlock> blocks = new ArrayList<>();
        Set<Long> leaders = findLeaders(instructions);
        
        BasicBlock currentBlock = null;
        
        for (Instruction instruction : instructions) {
            // Start new block if this is a leader
            if (leaders.contains(instruction.getAddress())) {
                if (currentBlock != null) {
                    blocks.add(currentBlock);
                }
                currentBlock = new BasicBlock();
                currentBlock.setStartAddress(instruction.getAddress());
            }
            
            if (currentBlock != null) {
                currentBlock.addInstruction(instruction);
                currentBlock.setEndAddress(instruction.getAddress());
                
                // End block on control flow instruction
                if (isControlFlowInstruction(instruction)) {
                    blocks.add(currentBlock);
                    currentBlock = null;
                }
            }
        }
        
        // Add final block if exists
        if (currentBlock != null) {
            blocks.add(currentBlock);
        }
        
        return blocks;
    }
    
    private Set<Long> findLeaders(List<Instruction> instructions) {
        Set<Long> leaders = new HashSet<>();
        
        // First instruction is always a leader
        if (!instructions.isEmpty()) {
            leaders.add(instructions.get(0).getAddress());
        }
        
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            String mnemonic = instruction.getMnemonic().toLowerCase();
            
            // Target of jump/call is a leader
            if (mnemonic.startsWith("j") || mnemonic.equals("call")) {
                long target = extractJumpTarget(instruction);
                if (target != -1) {
                    leaders.add(target);
                }
            }
            
            // Instruction after jump/call/ret is a leader
            if (isControlFlowInstruction(instruction) && i + 1 < instructions.size()) {
                leaders.add(instructions.get(i + 1).getAddress());
            }
        }
        
        return leaders;
    }
    
    private boolean isControlFlowInstruction(Instruction instruction) {
        String mnemonic = instruction.getMnemonic().toLowerCase();
        return mnemonic.startsWith("j") || // jumps
               mnemonic.equals("call") ||
               mnemonic.equals("ret") ||
               mnemonic.equals("int"); // interrupts
    }
    
    private long extractJumpTarget(Instruction instruction) {
        List<String> operands = instruction.getOperands();
        if (operands.isEmpty()) return -1;
        
        String target = operands.get(0);
        try {
            if (target.startsWith("0x")) {
                return Long.parseUnsignedLong(target.substring(2), 16);
            } else if (target.matches("\\d+")) {
                return Long.parseLong(target);
            }
        } catch (NumberFormatException e) {
            // Ignore invalid targets
        }
        
        return -1;
    }
    
    private void buildControlFlowEdges(ControlFlowGraph cfg, List<BasicBlock> basicBlocks) {
        Map<Long, BasicBlock> addressToBlock = new HashMap<>();
        
        // Build address lookup map
        for (BasicBlock block : basicBlocks) {
            addressToBlock.put(block.getStartAddress(), block);
        }
        
        // Add edges
        for (int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock block = basicBlocks.get(i);
            List<Instruction> instructions = block.getInstructions();
            
            if (instructions.isEmpty()) continue;
            
            Instruction lastInstruction = instructions.get(instructions.size() - 1);
            String mnemonic = lastInstruction.getMnemonic().toLowerCase();
            
            if (mnemonic.startsWith("jmp")) {
                // Unconditional jump
                long target = extractJumpTarget(lastInstruction);
                if (target != -1) {
                    BasicBlock targetBlock = addressToBlock.get(target);
                    if (targetBlock != null) {
                        cfg.addEdge(block, targetBlock, EdgeType.UNCONDITIONAL);
                    }
                }
            } else if (mnemonic.startsWith("j") && !mnemonic.equals("jmp")) {
                // Conditional jump
                long target = extractJumpTarget(lastInstruction);
                if (target != -1) {
                    BasicBlock targetBlock = addressToBlock.get(target);
                    if (targetBlock != null) {
                        cfg.addEdge(block, targetBlock, EdgeType.CONDITIONAL_TRUE);
                    }
                }
                
                // Fall-through edge
                if (i + 1 < basicBlocks.size()) {
                    cfg.addEdge(block, basicBlocks.get(i + 1), EdgeType.CONDITIONAL_FALSE);
                }
            } else if (!mnemonic.equals("ret")) {
                // Fall-through to next block
                if (i + 1 < basicBlocks.size()) {
                    cfg.addEdge(block, basicBlocks.get(i + 1), EdgeType.FALL_THROUGH);
                }
            }
        }
    }
    
    private void identifyLoops(ControlFlowGraph cfg) {
        // Simple loop detection using back edges
        List<BasicBlock> blocks = cfg.getBasicBlocks();
        
        for (BasicBlock block : blocks) {
            for (ControlFlowEdge edge : cfg.getOutgoingEdges(block)) {
                BasicBlock target = edge.getTarget();
                
                // Check if this is a back edge (target dominates source)
                if (dominates(target, block, cfg)) {
                    Loop loop = new Loop();
                    loop.setHeader(target);
                    loop.setBackEdge(edge);
                    loop.setType(LoopType.WHILE); // Simplified classification
                    
                    cfg.addLoop(loop);
                }
            }
        }
    }
    
    private boolean dominates(BasicBlock dominator, BasicBlock dominated, ControlFlowGraph cfg) {
        // Simplified domination check
        // In a full implementation, this would use proper dominator tree algorithms
        return dominator.getStartAddress() < dominated.getStartAddress();
    }
    
    private void analyzeFunctionCalls(ControlFlowGraph cfg, DisassembledFunction function) {
        for (BasicBlock block : cfg.getBasicBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                if (instruction.getMnemonic().toLowerCase().equals("call")) {
                    long target = extractJumpTarget(instruction);
                    if (target != -1) {
                        String functionName = "sub_" + Long.toHexString(target);
                        function.getCalledFunctions().add(functionName);
                        
                        // Mark as function call site
                        FunctionCall call = new FunctionCall();
                        call.setAddress(instruction.getAddress());
                        call.setTargetAddress(target);
                        call.setTargetName(functionName);
                        
                        cfg.addFunctionCall(call);
                    }
                }
            }
        }
    }
    
    private void detectPatterns(ControlFlowGraph cfg) {
        // Detect common patterns like if-else, switch statements, etc.
        
        for (BasicBlock block : cfg.getBasicBlocks()) {
            List<ControlFlowEdge> outgoing = cfg.getOutgoingEdges(block);
            
            if (outgoing.size() == 2) {
                // Potential if-else pattern
                boolean hasConditionalTrue = outgoing.stream()
                    .anyMatch(e -> e.getType() == EdgeType.CONDITIONAL_TRUE);
                boolean hasConditionalFalse = outgoing.stream()
                    .anyMatch(e -> e.getType() == EdgeType.CONDITIONAL_FALSE);
                
                if (hasConditionalTrue && hasConditionalFalse) {
                    Pattern pattern = new Pattern();
                    pattern.setType(PatternType.IF_ELSE);
                    pattern.setStartBlock(block);
                    cfg.addPattern(pattern);
                }
            }
        }
    }
}

/**
 * Represents a control flow graph for a function.
 */
class ControlFlowGraph {
    private DisassembledFunction function;
    private List<BasicBlock> basicBlocks;
    private List<ControlFlowEdge> edges;
    private List<Loop> loops;
    private List<FunctionCall> functionCalls;
    private List<Pattern> patterns;
    
    public ControlFlowGraph() {
        this.basicBlocks = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.loops = new ArrayList<>();
        this.functionCalls = new ArrayList<>();
        this.patterns = new ArrayList<>();
    }
    
    // Getters and setters
    public DisassembledFunction getFunction() { return function; }
    public void setFunction(DisassembledFunction function) { this.function = function; }
    
    public List<BasicBlock> getBasicBlocks() { return basicBlocks; }
    public void setBasicBlocks(List<BasicBlock> basicBlocks) { this.basicBlocks = basicBlocks; }
    
    public List<ControlFlowEdge> getEdges() { return edges; }
    
    public void addEdge(BasicBlock source, BasicBlock target, EdgeType type) {
        ControlFlowEdge edge = new ControlFlowEdge();
        edge.setSource(source);
        edge.setTarget(target);
        edge.setType(type);
        edges.add(edge);
    }
    
    public List<ControlFlowEdge> getOutgoingEdges(BasicBlock block) {
        return edges.stream()
            .filter(e -> e.getSource().equals(block))
            .toList();
    }
    
    public List<Loop> getLoops() { return loops; }
    public void addLoop(Loop loop) { loops.add(loop); }
    
    public List<FunctionCall> getFunctionCalls() { return functionCalls; }
    public void addFunctionCall(FunctionCall call) { functionCalls.add(call); }
    
    public List<Pattern> getPatterns() { return patterns; }
    public void addPattern(Pattern pattern) { patterns.add(pattern); }
}

/**
 * Represents a basic block in the control flow graph.
 */
class BasicBlock {
    private long startAddress;
    private long endAddress;
    private List<Instruction> instructions;
    private Map<String, Object> properties;
    
    public BasicBlock() {
        this.instructions = new ArrayList<>();
        this.properties = new HashMap<>();
    }
    
    // Getters and setters
    public long getStartAddress() { return startAddress; }
    public void setStartAddress(long startAddress) { this.startAddress = startAddress; }
    
    public long getEndAddress() { return endAddress; }
    public void setEndAddress(long endAddress) { this.endAddress = endAddress; }
    
    public List<Instruction> getInstructions() { return instructions; }
    public void addInstruction(Instruction instruction) { instructions.add(instruction); }
    
    public Map<String, Object> getProperties() { return properties; }
}

/**
 * Represents an edge in the control flow graph.
 */
class ControlFlowEdge {
    private BasicBlock source;
    private BasicBlock target;
    private EdgeType type;
    
    // Getters and setters
    public BasicBlock getSource() { return source; }
    public void setSource(BasicBlock source) { this.source = source; }
    
    public BasicBlock getTarget() { return target; }
    public void setTarget(BasicBlock target) { this.target = target; }
    
    public EdgeType getType() { return type; }
    public void setType(EdgeType type) { this.type = type; }
}

enum EdgeType {
    FALL_THROUGH, UNCONDITIONAL, CONDITIONAL_TRUE, CONDITIONAL_FALSE, CALL, RETURN
}

/**
 * Represents a loop in the control flow.
 */
class Loop {
    private BasicBlock header;
    private ControlFlowEdge backEdge;
    private LoopType type;
    private List<BasicBlock> body;
    
    public Loop() {
        this.body = new ArrayList<>();
    }
    
    // Getters and setters
    public BasicBlock getHeader() { return header; }
    public void setHeader(BasicBlock header) { this.header = header; }
    
    public ControlFlowEdge getBackEdge() { return backEdge; }
    public void setBackEdge(ControlFlowEdge backEdge) { this.backEdge = backEdge; }
    
    public LoopType getType() { return type; }
    public void setType(LoopType type) { this.type = type; }
    
    public List<BasicBlock> getBody() { return body; }
}

enum LoopType {
    WHILE, FOR, DO_WHILE, UNKNOWN
}

/**
 * Represents a function call.
 */
class FunctionCall {
    private long address;
    private long targetAddress;
    private String targetName;
    private List<String> arguments;
    
    public FunctionCall() {
        this.arguments = new ArrayList<>();
    }
    
    // Getters and setters
    public long getAddress() { return address; }
    public void setAddress(long address) { this.address = address; }
    
    public long getTargetAddress() { return targetAddress; }
    public void setTargetAddress(long targetAddress) { this.targetAddress = targetAddress; }
    
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    
    public List<String> getArguments() { return arguments; }
}

/**
 * Represents a code pattern.
 */
class Pattern {
    private PatternType type;
    private BasicBlock startBlock;
    private List<BasicBlock> involvedBlocks;
    
    public Pattern() {
        this.involvedBlocks = new ArrayList<>();
    }
    
    // Getters and setters
    public PatternType getType() { return type; }
    public void setType(PatternType type) { this.type = type; }
    
    public BasicBlock getStartBlock() { return startBlock; }
    public void setStartBlock(BasicBlock startBlock) { this.startBlock = startBlock; }
    
    public List<BasicBlock> getInvolvedBlocks() { return involvedBlocks; }
}

enum PatternType {
    IF_ELSE, SWITCH, TRY_CATCH, FUNCTION_EPILOG, FUNCTION_PROLOG
}