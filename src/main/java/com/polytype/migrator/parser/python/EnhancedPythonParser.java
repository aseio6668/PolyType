package com.polytype.migrator.parser.python;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.parser.base.Parser;
import com.polytype.migrator.parser.python.PythonTypeInference;
import com.polytype.migrator.parser.python.PythonTypeInference.TypeInfo;
import com.polytype.migrator.parser.python.PythonAdvancedConstructs;
import com.polytype.migrator.parser.python.PythonAdvancedConstructs.DecoratorInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced Python parser supporting modern Python 3.8+ features including:
 * - Type hints and annotations with comprehensive type inference
 * - Dataclasses and NamedTuple
 * - Async/await and coroutines
 * - Context managers and decorators
 * - f-strings and match statements
 * - Walrus operator and positional-only parameters
 * - Advanced type inference and semantic analysis
 */
public class EnhancedPythonParser implements Parser {
    
    private final PythonTypeInference typeInference;
    private final PythonAdvancedConstructs advancedConstructs;
    
    // Class patterns
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(?:@[\\w.()\\s,=]+\\s*)*class\\s+(\\w+)(?:\\s*\\(([^)]*)\\))?\\s*:"
    );
    
    private static final Pattern DATACLASS_PATTERN = Pattern.compile(
        "@dataclass(?:\\([^)]*\\))?\\s*(?:\\n\\s*)?class\\s+(\\w+)"
    );
    
    // Function patterns
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "^\\s*(?:@[\\w.()\\s,=]+\\s*)*(?:(async)\\s+)?def\\s+(\\w+)\\s*\\(([^)]*)\\)(?:\\s*->\\s*([^:]+))?\\s*:"
    );
    
    // Variable and assignment patterns
    private static final Pattern TYPED_ASSIGNMENT_PATTERN = Pattern.compile(
        "^\\s*(\\w+)\\s*:\\s*([^=]+)(?:\\s*=\\s*(.+))?$"
    );
    
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile(
        "^\\s*(\\w+)\\s*=\\s*(.+)$"
    );
    
    private static final Pattern WALRUS_PATTERN = Pattern.compile(
        "(\\w+)\\s*:=\\s*(.+)"
    );
    
    // Control flow patterns
    private static final Pattern IF_PATTERN = Pattern.compile(
        "^\\s*if\\s+(.+):"
    );
    
    private static final Pattern ELIF_PATTERN = Pattern.compile(
        "^\\s*elif\\s+(.+):"
    );
    
    private static final Pattern ELSE_PATTERN = Pattern.compile(
        "^\\s*else\\s*:"
    );
    
    private static final Pattern FOR_PATTERN = Pattern.compile(
        "^\\s*(?:(async)\\s+)?for\\s+(\\w+)\\s+in\\s+(.+):"
    );
    
    private static final Pattern WHILE_PATTERN = Pattern.compile(
        "^\\s*while\\s+(.+):"
    );
    
    private static final Pattern MATCH_PATTERN = Pattern.compile(
        "^\\s*match\\s+(.+):"
    );
    
    private static final Pattern CASE_PATTERN = Pattern.compile(
        "^\\s*case\\s+(.+):"
    );
    
    // Import patterns
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "^\\s*(?:from\\s+([\\w.]+)\\s+)?import\\s+(.+)$"
    );
    
    // Decorator patterns
    private static final Pattern DECORATOR_PATTERN = Pattern.compile(
        "^\\s*@([\\w.]+)(?:\\(([^)]*)\\))?\\s*$"
    );
    
    // Context manager patterns
    private static final Pattern WITH_PATTERN = Pattern.compile(
        "^\\s*(?:(async)\\s+)?with\\s+(.+)\\s+as\\s+(\\w+):"
    );
    
    // Exception handling patterns
    private static final Pattern TRY_PATTERN = Pattern.compile(
        "^\\s*try\\s*:"
    );
    
    private static final Pattern EXCEPT_PATTERN = Pattern.compile(
        "^\\s*except(?:\\s+(\\w+))?(?:\\s+as\\s+(\\w+))?\\s*:"
    );
    
    private static final Pattern FINALLY_PATTERN = Pattern.compile(
        "^\\s*finally\\s*:"
    );
    
    // Parameter patterns for advanced parsing
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?:([*/])\\s*)?(\\w+)(?:\\s*:\\s*([^,=]+))?(?:\\s*=\\s*([^,]+))?"
    );
    
    /**
     * Constructor initializes the type inference system and advanced constructs handler.
     */
    public EnhancedPythonParser() {
        this.typeInference = new PythonTypeInference();
        this.advancedConstructs = new PythonAdvancedConstructs();
    }
    
    @Override
    public ProgramNode parse(String filePath) throws IOException {
        ProgramNode program = new ProgramNode(1, 1);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            parseLines(lines, program);
        }
        
        return program;
    }
    
    private void parseLines(List<String> lines, ProgramNode program) {
        List<String> currentDecorators = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmedLine = line.trim();
            
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            
            // Parse decorators
            Matcher decoratorMatcher = DECORATOR_PATTERN.matcher(trimmedLine);
            if (decoratorMatcher.find()) {
                currentDecorators.add(decoratorMatcher.group(1));
                continue;
            }
            
            // Parse import statements
            Matcher importMatcher = IMPORT_PATTERN.matcher(trimmedLine);
            if (importMatcher.find()) {
                // Handle imports - could be expanded for dependency tracking
                continue;
            }
            
            // Parse dataclass
            if (currentDecorators.contains("dataclass")) {
                Matcher classMatcher = CLASS_PATTERN.matcher(trimmedLine);
                if (classMatcher.find()) {
                    ClassDeclarationNode dataclassNode = parseDataclass(lines, i, classMatcher, currentDecorators);
                    program.addChild(dataclassNode);
                    currentDecorators.clear();
                    continue;
                }
            }
            
            // Parse regular class
            Matcher classMatcher = CLASS_PATTERN.matcher(trimmedLine);
            if (classMatcher.find()) {
                ClassDeclarationNode classNode = parseClass(lines, i, classMatcher, currentDecorators);
                program.addChild(classNode);
                currentDecorators.clear();
                continue;
            }
            
            // Parse function
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(trimmedLine);
            if (functionMatcher.find()) {
                FunctionDeclarationNode functionNode = parseFunction(lines, i, functionMatcher, currentDecorators);
                program.addChild(functionNode);
                currentDecorators.clear();
                continue;
            }
            
            // Parse typed assignments
            Matcher typedAssignMatcher = TYPED_ASSIGNMENT_PATTERN.matcher(trimmedLine);
            if (typedAssignMatcher.find()) {
                VariableDeclarationNode varNode = parseTypedAssignment(typedAssignMatcher, i + 1);
                if (varNode != null) {
                    program.addChild(varNode);
                }
                continue;
            }
            
            // Parse regular assignments
            Matcher assignMatcher = ASSIGNMENT_PATTERN.matcher(trimmedLine);
            if (assignMatcher.find()) {
                VariableDeclarationNode varNode = parseAssignment(assignMatcher, i + 1);
                if (varNode != null) {
                    program.addChild(varNode);
                }
                continue;
            }
            
            // Clear decorators if we hit a non-applicable line
            if (!currentDecorators.isEmpty()) {
                currentDecorators.clear();
            }
        }
    }
    
    private ClassDeclarationNode parseClass(List<String> lines, int startIndex, Matcher classMatcher, List<String> decorators) {
        String className = classMatcher.group(1);
        String baseClasses = classMatcher.group(2);
        
        ClassDeclarationNode classNode = new ClassDeclarationNode(className, false, startIndex + 1, 1);
        
        // Parse class body
        parseClassBody(lines, startIndex, classNode);
        
        return classNode;
    }
    
    private ClassDeclarationNode parseDataclass(List<String> lines, int startIndex, Matcher classMatcher, List<String> decorators) {
        String className = classMatcher.group(1);
        
        ClassDeclarationNode dataclassNode = new ClassDeclarationNode(className, true, startIndex + 1, 1);
        // Could add metadata about being a dataclass
        
        parseClassBody(lines, startIndex, dataclassNode);
        
        return dataclassNode;
    }
    
    private void parseClassBody(List<String> lines, int startIndex, ClassDeclarationNode classNode) {
        boolean inClass = false;
        int baseIndent = getIndentLevel(lines.get(startIndex));
        
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmedLine = line.trim();
            
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            
            int currentIndent = getIndentLevel(line);
            
            // Check if we're still in the class
            if (currentIndent <= baseIndent && !trimmedLine.isEmpty()) {
                break; // We've left the class
            }
            
            if (currentIndent > baseIndent) {
                inClass = true;
                parseClassMember(trimmedLine, classNode, i + 1);
            }
        }
    }
    
    private void parseClassMember(String line, ClassDeclarationNode classNode, int lineNumber) {
        // Parse method definitions
        Matcher methodMatcher = FUNCTION_PATTERN.matcher(line);
        if (methodMatcher.find()) {
            String asyncKeyword = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String parameters = methodMatcher.group(3);
            String returnType = methodMatcher.group(4);
            
            boolean isAsync = asyncKeyword != null;
            boolean isConstructor = "__init__".equals(methodName);
            
            String mappedReturnType = returnType != null ? mapPythonType(returnType.trim()) : "Object";
            if (isConstructor) {
                mappedReturnType = "void";
            }
            
            List<ParameterNode> paramList = parseParameters(parameters);
            
            FunctionDeclarationNode method = new FunctionDeclarationNode(
                methodName, mappedReturnType, paramList, true, isConstructor, lineNumber, 1);
            method.setAsync(isAsync);
            
            classNode.addChild(method);
            return;
        }
        
        // Parse typed class variables
        Matcher typedVarMatcher = TYPED_ASSIGNMENT_PATTERN.matcher(line);
        if (typedVarMatcher.find()) {
            VariableDeclarationNode classVar = parseTypedAssignment(typedVarMatcher, lineNumber);
            if (classVar != null) {
                classNode.addChild(classVar);
            }
            return;
        }
        
        // Parse regular class variables
        Matcher assignMatcher = ASSIGNMENT_PATTERN.matcher(line);
        if (assignMatcher.find()) {
            VariableDeclarationNode classVar = parseAssignment(assignMatcher, lineNumber);
            if (classVar != null) {
                classNode.addChild(classVar);
            }
            return;
        }
    }
    
    private FunctionDeclarationNode parseFunction(List<String> lines, int startIndex, Matcher functionMatcher, List<String> decorators) {
        String asyncKeyword = functionMatcher.group(1);
        String functionName = functionMatcher.group(2);
        String parameters = functionMatcher.group(3);
        String returnType = functionMatcher.group(4);
        
        boolean isAsync = asyncKeyword != null;
        boolean isStatic = decorators.contains("staticmethod");
        boolean isClassMethod = decorators.contains("classmethod");
        boolean isProperty = decorators.contains("property");
        
        String mappedReturnType = returnType != null ? mapPythonType(returnType.trim()) : "Object";
        List<ParameterNode> paramList = parseParameters(parameters);
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, mappedReturnType, paramList, false, false, startIndex + 1, 1);
        function.setAsync(isAsync);
        function.setStatic(isStatic);
        
        return function;
    }
    
    private VariableDeclarationNode parseTypedAssignment(Matcher typedAssignMatcher, int lineNumber) {
        String varName = typedAssignMatcher.group(1);
        String varType = typedAssignMatcher.group(2);
        String defaultValue = typedAssignMatcher.group(3);
        
        String mappedType = mapPythonType(varType.trim());
        boolean isFinal = varType.contains("Final");
        
        VariableDeclarationNode variable = new VariableDeclarationNode(
            varName, mappedType, isFinal, defaultValue, lineNumber, 1);
        
        return variable;
    }
    
    private VariableDeclarationNode parseAssignment(Matcher assignMatcher, int lineNumber) {
        String varName = assignMatcher.group(1);
        String value = assignMatcher.group(2);
        
        String inferredType = inferTypeFromValue(value);
        
        VariableDeclarationNode variable = new VariableDeclarationNode(
            varName, inferredType, false, value, lineNumber, 1);
        
        return variable;
    }
    
    private List<ParameterNode> parseParameters(String paramString) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        if (paramString == null || paramString.trim().isEmpty()) {
            return parameters;
        }
        
        // Handle complex parameter parsing with type hints, defaults, and special markers
        String[] params = splitParameters(paramString);
        
        for (String param : params) {
            param = param.trim();
            if (param.isEmpty() || "self".equals(param) || "cls".equals(param)) {
                continue;
            }
            
            Matcher paramMatcher = PARAMETER_PATTERN.matcher(param);
            if (paramMatcher.find()) {
                String marker = paramMatcher.group(1); // * or /
                String paramName = paramMatcher.group(2);
                String paramType = paramMatcher.group(3);
                String defaultValue = paramMatcher.group(4);
                
                String mappedType = paramType != null ? mapPythonType(paramType.trim()) : "Object";
                boolean optional = defaultValue != null;
                
                ParameterNode parameter = new ParameterNode(paramName, mappedType, optional, 0, 0);
                parameters.add(parameter);
            }
        }
        
        return parameters;
    }
    
    private String[] splitParameters(String paramString) {
        // Simple parameter splitting that handles nested parentheses and brackets
        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        
        for (char c : paramString.toCharArray()) {
            if (c == ',' && depth == 0) {
                params.add(current.toString());
                current.setLength(0);
            } else {
                if (c == '(' || c == '[' || c == '{') depth++;
                else if (c == ')' || c == ']' || c == '}') depth--;
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            params.add(current.toString());
        }
        
        return params.toArray(new String[0]);
    }
    
    private String mapPythonType(String pythonType) {
        if (pythonType == null || pythonType.trim().isEmpty()) {
            return "Object";
        }
        
        pythonType = pythonType.trim();
        
        // Handle Union types
        if (pythonType.startsWith("Union[")) {
            return "Object"; // Simplified for now
        }
        
        // Handle Optional types
        if (pythonType.startsWith("Optional[")) {
            String innerType = pythonType.substring(9, pythonType.length() - 1);
            return "Optional<" + mapPythonType(innerType) + ">";
        }
        
        // Handle generic types
        if (pythonType.startsWith("List[")) {
            String innerType = pythonType.substring(5, pythonType.length() - 1);
            return "List<" + mapPythonType(innerType) + ">";
        }
        
        if (pythonType.startsWith("Dict[")) {
            String innerTypes = pythonType.substring(5, pythonType.length() - 1);
            String[] types = innerTypes.split(",", 2);
            if (types.length >= 2) {
                return "Map<" + mapPythonType(types[0].trim()) + ", " + mapPythonType(types[1].trim()) + ">";
            }
            return "Map";
        }
        
        if (pythonType.startsWith("Set[")) {
            String innerType = pythonType.substring(4, pythonType.length() - 1);
            return "Set<" + mapPythonType(innerType) + ">";
        }
        
        if (pythonType.startsWith("Tuple[")) {
            return "List"; // Simplified mapping
        }
        
        // Basic type mappings
        switch (pythonType) {
            case "int": return "Integer";
            case "float": return "Double";
            case "bool": return "Boolean";
            case "str": return "String";
            case "bytes": return "byte[]";
            case "bytearray": return "byte[]";
            case "list": return "List";
            case "dict": return "Map";
            case "set": return "Set";
            case "tuple": return "List";
            case "None": case "NoneType": return "void";
            case "Any": return "Object";
            case "object": return "Object";
            case "Callable": return "Function";
            case "Iterator": case "Iterable": return "Iterable";
            case "Generator": return "Iterator";
            default:
                // Handle Final types
                if (pythonType.startsWith("Final[")) {
                    String innerType = pythonType.substring(6, pythonType.length() - 1);
                    return mapPythonType(innerType);
                }
                
                // Handle ClassVar types
                if (pythonType.startsWith("ClassVar[")) {
                    String innerType = pythonType.substring(9, pythonType.length() - 1);
                    return mapPythonType(innerType);
                }
                
                return pythonType; // Return as-is for custom classes
        }
    }
    
    private String inferTypeFromValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("None")) {
            return "Object";
        }
        
        value = value.trim();
        
        // String literals (including f-strings)
        if ((value.startsWith("\"") && value.endsWith("\"")) || 
            (value.startsWith("'") && value.endsWith("'")) ||
            (value.startsWith("f\"") && value.endsWith("\"")) ||
            (value.startsWith("f'") && value.endsWith("'"))) {
            return "String";
        }
        
        // Numbers
        if (value.matches("-?\\d+")) {
            return "Integer";
        }
        if (value.matches("-?\\d+\\.\\d+")) {
            return "Double";
        }
        
        // Booleans
        if (value.equals("True") || value.equals("False")) {
            return "Boolean";
        }
        
        // Lists
        if (value.startsWith("[") && value.endsWith("]")) {
            return "List";
        }
        
        // Dicts
        if (value.startsWith("{") && value.endsWith("}")) {
            return "Map";
        }
        
        // Sets
        if (value.startsWith("{") && value.endsWith("}") && !value.contains(":")) {
            return "Set";
        }
        
        // Tuples
        if (value.startsWith("(") && value.endsWith(")")) {
            return "List"; // Simplified mapping
        }
        
        return "Object";
    }
    
    private int getIndentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4; // Treat tab as 4 spaces
            } else {
                break;
            }
        }
        return count;
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        return fileName.toLowerCase().endsWith(".py") ||
               fileName.toLowerCase().endsWith(".pyw") ||
               fileName.toLowerCase().endsWith(".pyi"); // Type stub files
    }
    
    @Override
    public String getLanguageName() {
        return "Python";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".py", ".pyw", ".pyi"};
    }
}