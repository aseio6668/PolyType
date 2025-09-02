package com.polytype.migrator.binary;

import java.util.*;

/**
 * Analyzes API calls in disassembled functions to understand program behavior.
 */
public class ApiCallAnalyzer {
    
    private final Map<String, ApiSignature> knownApis;
    
    public ApiCallAnalyzer() {
        this.knownApis = new HashMap<>();
        initializeKnownApis();
    }
    
    /**
     * Analyze API calls in a list of functions.
     */
    public List<ApiCall> analyzeApiCalls(List<DisassembledFunction> functions) {
        List<ApiCall> apiCalls = new ArrayList<>();
        
        for (DisassembledFunction function : functions) {
            apiCalls.addAll(analyzeFunction(function));
        }
        
        return apiCalls;
    }
    
    private List<ApiCall> analyzeFunction(DisassembledFunction function) {
        List<ApiCall> calls = new ArrayList<>();
        
        for (Instruction instruction : function.getInstructions()) {
            if (instruction.getMnemonic().toLowerCase().equals("call")) {
                ApiCall apiCall = analyzeCallInstruction(instruction, function);
                if (apiCall != null) {
                    calls.add(apiCall);
                }
            }
        }
        
        return calls;
    }
    
    private ApiCall analyzeCallInstruction(Instruction instruction, DisassembledFunction function) {
        List<String> operands = instruction.getOperands();
        if (operands.isEmpty()) return null;
        
        String target = operands.get(0);
        
        // Try to resolve the call target
        String functionName = resolveCallTarget(target, function);
        if (functionName == null) return null;
        
        ApiCall call = new ApiCall();
        call.setAddress(instruction.getAddress());
        call.setFunctionName(functionName);
        call.setCallType(CallType.DIRECT);
        
        // Check if it's a known API
        ApiSignature signature = knownApis.get(functionName.toLowerCase());
        if (signature != null) {
            call.setApiSignature(signature);
            call.setCategory(signature.getCategory());
            call.setDescription(signature.getDescription());
        }
        
        // Analyze parameters (simplified)
        call.setParameters(analyzeParameters(instruction, function));
        
        return call;
    }
    
    private String resolveCallTarget(String target, DisassembledFunction function) {
        // Try to parse as direct address
        try {
            if (target.startsWith("0x")) {
                long address = Long.parseUnsignedLong(target.substring(2), 16);
                return "sub_" + Long.toHexString(address);
            } else if (target.matches("\\d+")) {
                long address = Long.parseLong(target);
                return "sub_" + Long.toHexString(address);
            }
        } catch (NumberFormatException e) {
            // Not a direct address
        }
        
        // Check if it's a register (indirect call)
        if (target.matches("[a-z]+")) {
            return "indirect_call_" + target;
        }
        
        return target;
    }
    
    private List<String> analyzeParameters(Instruction instruction, DisassembledFunction function) {
        List<String> parameters = new ArrayList<>();
        
        // Look backwards from the call instruction to find parameter setup
        List<Instruction> instructions = function.getInstructions();
        int callIndex = instructions.indexOf(instruction);
        
        // Scan backwards for PUSH instructions (x86 calling convention)
        for (int i = callIndex - 1; i >= 0 && i >= callIndex - 10; i--) {
            Instruction prev = instructions.get(i);
            String mnemonic = prev.getMnemonic().toLowerCase();
            
            if (mnemonic.equals("push")) {
                if (!prev.getOperands().isEmpty()) {
                    parameters.add(0, prev.getOperands().get(0)); // Add to front (reverse order)
                }
            } else if (mnemonic.startsWith("mov") && !prev.getOperands().isEmpty()) {
                // Look for register parameter setup (e.g., mov ecx, value)
                String operand = prev.getOperands().get(0);
                if (operand.matches("e?[abcd]x|e?[sb]p|e?[sd]i")) {
                    if (prev.getOperands().size() > 1) {
                        parameters.add(prev.getOperands().get(1));
                    }
                }
            } else if (isControlFlowInstruction(prev)) {
                // Stop at control flow instructions
                break;
            }
        }
        
        return parameters;
    }
    
    private boolean isControlFlowInstruction(Instruction instruction) {
        String mnemonic = instruction.getMnemonic().toLowerCase();
        return mnemonic.startsWith("j") || mnemonic.equals("call") || mnemonic.equals("ret");
    }
    
    private void initializeKnownApis() {
        // Windows API signatures
        addWindowsApis();
        
        // C Runtime APIs
        addCRuntimeApis();
        
        // POSIX APIs
        addPosixApis();
    }
    
    private void addWindowsApis() {
        // File operations
        knownApis.put("createfilea", new ApiSignature("CreateFileA", ApiCategory.FILE_IO,
            "Creates or opens a file or I/O device",
            Arrays.asList("LPCSTR", "DWORD", "DWORD", "LPSECURITY_ATTRIBUTES", "DWORD", "DWORD", "HANDLE"),
            "HANDLE"));
        
        knownApis.put("readfile", new ApiSignature("ReadFile", ApiCategory.FILE_IO,
            "Reads data from the specified file or input/output device",
            Arrays.asList("HANDLE", "LPVOID", "DWORD", "LPDWORD", "LPOVERLAPPED"),
            "BOOL"));
        
        knownApis.put("writefile", new ApiSignature("WriteFile", ApiCategory.FILE_IO,
            "Writes data to the specified file or input/output device",
            Arrays.asList("HANDLE", "LPCVOID", "DWORD", "LPDWORD", "LPOVERLAPPED"),
            "BOOL"));
        
        // Memory operations
        knownApis.put("virtualalloc", new ApiSignature("VirtualAlloc", ApiCategory.MEMORY,
            "Reserves, commits, or changes the state of pages in the virtual address space",
            Arrays.asList("LPVOID", "SIZE_T", "DWORD", "DWORD"),
            "LPVOID"));
        
        knownApis.put("virtualfree", new ApiSignature("VirtualFree", ApiCategory.MEMORY,
            "Releases, decommits, or releases and decommits pages within virtual address space",
            Arrays.asList("LPVOID", "SIZE_T", "DWORD"),
            "BOOL"));
        
        // Process operations
        knownApis.put("createprocessa", new ApiSignature("CreateProcessA", ApiCategory.PROCESS,
            "Creates a new process and its primary thread",
            Arrays.asList("LPCSTR", "LPSTR", "LPSECURITY_ATTRIBUTES", "LPSECURITY_ATTRIBUTES", 
                         "BOOL", "DWORD", "LPVOID", "LPCSTR", "LPSTARTUPINFOA", "LPPROCESS_INFORMATION"),
            "BOOL"));
        
        // Registry operations
        knownApis.put("regopenkeyexa", new ApiSignature("RegOpenKeyExA", ApiCategory.REGISTRY,
            "Opens the specified registry key",
            Arrays.asList("HKEY", "LPCSTR", "DWORD", "REGSAM", "PHKEY"),
            "LSTATUS"));
        
        // Network operations
        knownApis.put("wsastartup", new ApiSignature("WSAStartup", ApiCategory.NETWORK,
            "Initiates use of the Winsock DLL",
            Arrays.asList("WORD", "LPWSADATA"),
            "int"));
        
        knownApis.put("socket", new ApiSignature("socket", ApiCategory.NETWORK,
            "Creates a socket",
            Arrays.asList("int", "int", "int"),
            "SOCKET"));
        
        // UI operations
        knownApis.put("messageboxa", new ApiSignature("MessageBoxA", ApiCategory.UI,
            "Displays a modal dialog box",
            Arrays.asList("HWND", "LPCSTR", "LPCSTR", "UINT"),
            "int"));
        
        knownApis.put("createwindowexa", new ApiSignature("CreateWindowExA", ApiCategory.UI,
            "Creates an overlapped, pop-up, or child window",
            Arrays.asList("DWORD", "LPCSTR", "LPCSTR", "DWORD", "int", "int", "int", "int", 
                         "HWND", "HMENU", "HINSTANCE", "LPVOID"),
            "HWND"));
    }
    
    private void addCRuntimeApis() {
        knownApis.put("malloc", new ApiSignature("malloc", ApiCategory.MEMORY,
            "Allocates memory",
            Arrays.asList("size_t"),
            "void*"));
        
        knownApis.put("free", new ApiSignature("free", ApiCategory.MEMORY,
            "Deallocates memory",
            Arrays.asList("void*"),
            "void"));
        
        knownApis.put("printf", new ApiSignature("printf", ApiCategory.IO,
            "Prints formatted output",
            Arrays.asList("const char*", "..."),
            "int"));
        
        knownApis.put("fopen", new ApiSignature("fopen", ApiCategory.FILE_IO,
            "Opens a file",
            Arrays.asList("const char*", "const char*"),
            "FILE*"));
    }
    
    private void addPosixApis() {
        knownApis.put("open", new ApiSignature("open", ApiCategory.FILE_IO,
            "Opens a file",
            Arrays.asList("const char*", "int", "..."),
            "int"));
        
        knownApis.put("read", new ApiSignature("read", ApiCategory.FILE_IO,
            "Reads from a file descriptor",
            Arrays.asList("int", "void*", "size_t"),
            "ssize_t"));
        
        knownApis.put("write", new ApiSignature("write", ApiCategory.FILE_IO,
            "Writes to a file descriptor",
            Arrays.asList("int", "const void*", "size_t"),
            "ssize_t"));
    }
}

/**
 * Represents an API call found in the binary.
 */
class ApiCall {
    private long address;
    private String functionName;
    private CallType callType;
    private ApiSignature apiSignature;
    private ApiCategory category;
    private String description;
    private List<String> parameters;
    private Map<String, Object> metadata;
    
    public ApiCall() {
        this.parameters = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.category = ApiCategory.UNKNOWN;
        this.callType = CallType.UNKNOWN;
    }
    
    // Getters and setters
    public long getAddress() { return address; }
    public void setAddress(long address) { this.address = address; }
    
    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }
    
    public CallType getCallType() { return callType; }
    public void setCallType(CallType callType) { this.callType = callType; }
    
    public ApiSignature getApiSignature() { return apiSignature; }
    public void setApiSignature(ApiSignature apiSignature) { this.apiSignature = apiSignature; }
    
    public ApiCategory getCategory() { return category; }
    public void setCategory(ApiCategory category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }
    
    public Map<String, Object> getMetadata() { return metadata; }
}

/**
 * Signature information for a known API function.
 */
class ApiSignature {
    private String name;
    private ApiCategory category;
    private String description;
    private List<String> parameterTypes;
    private String returnType;
    
    public ApiSignature(String name, ApiCategory category, String description, 
                       List<String> parameterTypes, String returnType) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }
    
    // Getters
    public String getName() { return name; }
    public ApiCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public List<String> getParameterTypes() { return parameterTypes; }
    public String getReturnType() { return returnType; }
}

enum CallType {
    DIRECT, INDIRECT, DYNAMIC, UNKNOWN
}

enum ApiCategory {
    FILE_IO, MEMORY, PROCESS, THREAD, REGISTRY, NETWORK, 
    UI, GRAPHICS, AUDIO, CRYPTO, SYSTEM, IO, UNKNOWN
}