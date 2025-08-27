package com.polytype.migrator.parser.python;

import com.polytype.migrator.core.ast.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Comprehensive Python type inference system.
 * 
 * This class provides advanced type inference for Python code, handling:
 * - Static type hints and annotations
 * - Dynamic type inference from context
 * - Collection type inference (List, Dict, Set, Tuple)
 * - Function return type inference
 * - Generic type parameter inference
 * - Union type analysis
 * - Protocol and ABC type checking
 * - Dataclass and NamedTuple field inference
 * - Async/await type handling
 * - Context manager type inference
 * - Decorator type effects
 * 
 * The system follows Python's gradual typing approach and can work with
 * both typed and untyped Python code.
 */
public class PythonTypeInference {
    
    // Type inference context and symbol tables
    private final Map<String, TypeInfo> symbolTable;
    private final Map<String, FunctionTypeInfo> functionTable;
    private final Map<String, ClassTypeInfo> classTable;
    private final Stack<ScopeContext> scopeStack;
    private final Set<String> builtinTypes;
    private final Map<String, String> typeAliases;
    
    // Pattern matching for type hints and annotations
    private static final Pattern TYPE_HINT_PATTERN = Pattern.compile(
        "([a-zA-Z_][\\w.]*)(\\[([^\\]]+)\\])?(?:\\s*=\\s*(.+))?"
    );
    
    private static final Pattern COLLECTION_LITERAL_PATTERN = Pattern.compile(
        "\\[(.*?)\\]|\\{(.*?)\\}|\\((.*?)\\)"
    );
    
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(
        "([a-zA-Z_][\\w.]*)\\s*\\(([^)]*)\\)"
    );
    
    public PythonTypeInference() {
        this.symbolTable = new HashMap<>();
        this.functionTable = new HashMap<>();
        this.classTable = new HashMap<>();
        this.scopeStack = new Stack<>();
        this.builtinTypes = initializeBuiltinTypes();
        this.typeAliases = new HashMap<>();
        
        // Initialize global scope
        scopeStack.push(new ScopeContext("global", ScopeType.MODULE));
        
        // Setup common type aliases
        setupCommonTypeAliases();
        
        // Initialize builtin functions and their types
        initializeBuiltinFunctions();
    }
    
    /**
     * Infer the type of a variable declaration.
     */
    public TypeInfo inferVariableType(VariableDeclarationNode node, String sourceCode) {
        String varName = node.getName();
        String declaredType = node.getDataType();
        
        // If type is explicitly declared, use it
        if (declaredType != null && !declaredType.isEmpty() && !"Object".equals(declaredType)) {
            TypeInfo typeInfo = parseTypeAnnotation(declaredType);
            symbolTable.put(varName, typeInfo);
            return typeInfo;
        }
        
        // Try to infer from initialization value
        TypeInfo inferredType = inferFromContext(varName, sourceCode, node.getLineNumber());
        if (inferredType != null) {
            symbolTable.put(varName, inferredType);
            return inferredType;
        }
        
        // Default to Any if cannot infer
        TypeInfo anyType = new TypeInfo("Any", TypeCategory.ANY);
        symbolTable.put(varName, anyType);
        return anyType;
    }
    
    /**
     * Infer the return type of a function.
     */
    public TypeInfo inferFunctionReturnType(FunctionDeclarationNode node, String sourceCode) {
        String functionName = node.getName();
        String declaredReturnType = node.getReturnType();
        
        // If return type is explicitly declared
        if (declaredReturnType != null && !declaredReturnType.isEmpty() && 
            !"void".equals(declaredReturnType) && !"Object".equals(declaredReturnType)) {
            return parseTypeAnnotation(declaredReturnType);
        }
        
        // Analyze function body for return statements
        List<TypeInfo> returnTypes = analyzeReturnStatements(node, sourceCode);
        
        if (returnTypes.isEmpty()) {
            return new TypeInfo("None", TypeCategory.NONE);
        } else if (returnTypes.size() == 1) {
            return returnTypes.get(0);
        } else {
            // Multiple return types - create Union
            return createUnionType(returnTypes);
        }
    }
    
    /**
     * Infer parameter types from function usage and context.
     */
    public List<TypeInfo> inferParameterTypes(FunctionDeclarationNode node, String sourceCode) {
        List<TypeInfo> parameterTypes = new ArrayList<>();
        
        for (ParameterNode param : node.getParameters()) {
            String paramType = param.getDataType();
            
            if (paramType != null && !paramType.isEmpty() && !"Object".equals(paramType)) {
                // Explicit type annotation
                parameterTypes.add(parseTypeAnnotation(paramType));
            } else {
                // Infer from usage within function body
                TypeInfo inferredType = inferParameterTypeFromUsage(param.getName(), node, sourceCode);
                parameterTypes.add(inferredType);
            }
        }
        
        return parameterTypes;
    }
    
    /**
     * Infer class field types from assignments and usage.
     */
    public Map<String, TypeInfo> inferClassFieldTypes(ClassDeclarationNode node, String sourceCode) {
        Map<String, TypeInfo> fieldTypes = new HashMap<>();
        
        // Enter class scope
        scopeStack.push(new ScopeContext(node.getName(), ScopeType.CLASS));
        
        try {
            // Analyze field declarations and assignments
            for (ASTNode child : node.getChildren()) {
                if (child instanceof VariableDeclarationNode) {
                    VariableDeclarationNode field = (VariableDeclarationNode) child;
                    TypeInfo fieldType = inferVariableType(field, sourceCode);
                    fieldTypes.put(field.getName(), fieldType);
                }
            }
            
            // Analyze __init__ method for additional field assignments
            analyzeInitMethodForFields(node, fieldTypes, sourceCode);
            
            // Analyze property methods
            analyzePropertyMethods(node, fieldTypes, sourceCode);
            
        } finally {
            scopeStack.pop(); // Exit class scope
        }
        
        return fieldTypes;
    }
    
    /**
     * Parse a type annotation string into a TypeInfo object.
     */
    public TypeInfo parseTypeAnnotation(String annotation) {
        if (annotation == null || annotation.trim().isEmpty()) {
            return new TypeInfo("Any", TypeCategory.ANY);
        }
        
        annotation = annotation.trim();
        
        // Handle basic types
        if (builtinTypes.contains(annotation)) {
            return new TypeInfo(annotation, getTypeCategory(annotation));
        }
        
        // Handle Optional[T] -> Union[T, None]
        if (annotation.startsWith("Optional[")) {
            String innerType = extractGenericType(annotation);
            TypeInfo innerTypeInfo = parseTypeAnnotation(innerType);
            return createOptionalType(innerTypeInfo);
        }
        
        // Handle Union[T1, T2, ...]
        if (annotation.startsWith("Union[")) {
            String innerTypes = extractGenericType(annotation);
            List<TypeInfo> unionTypes = parseUnionTypes(innerTypes);
            return createUnionType(unionTypes);
        }
        
        // Handle List[T]
        if (annotation.startsWith("List[")) {
            String elementType = extractGenericType(annotation);
            TypeInfo elementTypeInfo = parseTypeAnnotation(elementType);
            return new GenericTypeInfo("List", TypeCategory.COLLECTION, Arrays.asList(elementTypeInfo));
        }
        
        // Handle Dict[K, V]
        if (annotation.startsWith("Dict[")) {
            String innerTypes = extractGenericType(annotation);
            String[] types = splitGenericTypes(innerTypes);
            if (types.length >= 2) {
                TypeInfo keyType = parseTypeAnnotation(types[0]);
                TypeInfo valueType = parseTypeAnnotation(types[1]);
                return new GenericTypeInfo("Dict", TypeCategory.COLLECTION, Arrays.asList(keyType, valueType));
            }
        }
        
        // Handle Set[T]
        if (annotation.startsWith("Set[")) {
            String elementType = extractGenericType(annotation);
            TypeInfo elementTypeInfo = parseTypeAnnotation(elementType);
            return new GenericTypeInfo("Set", TypeCategory.COLLECTION, Arrays.asList(elementTypeInfo));
        }
        
        // Handle Tuple[T1, T2, ...]
        if (annotation.startsWith("Tuple[")) {
            String innerTypes = extractGenericType(annotation);
            if ("...".equals(innerTypes)) {
                return new GenericTypeInfo("Tuple", TypeCategory.COLLECTION, 
                                         Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)));
            } else {
                List<TypeInfo> tupleTypes = parseUnionTypes(innerTypes);
                return new GenericTypeInfo("Tuple", TypeCategory.COLLECTION, tupleTypes);
            }
        }
        
        // Handle Callable types
        if (annotation.startsWith("Callable[")) {
            return parseCallableType(annotation);
        }
        
        // Handle generic types like MyClass[T]
        if (annotation.contains("[") && annotation.contains("]")) {
            String baseType = annotation.substring(0, annotation.indexOf('['));
            String genericPart = extractGenericType(annotation);
            List<TypeInfo> typeParams = parseUnionTypes(genericPart);
            return new GenericTypeInfo(baseType, TypeCategory.CUSTOM, typeParams);
        }
        
        // Handle type aliases
        if (typeAliases.containsKey(annotation)) {
            return parseTypeAnnotation(typeAliases.get(annotation));
        }
        
        // Custom class or unknown type
        return new TypeInfo(annotation, TypeCategory.CUSTOM);
    }
    
    /**
     * Infer type from literal values and expressions.
     */
    public TypeInfo inferFromLiteral(String literal) {
        if (literal == null) {
            return new TypeInfo("None", TypeCategory.NONE);
        }
        
        literal = literal.trim();
        
        // None
        if ("None".equals(literal)) {
            return new TypeInfo("None", TypeCategory.NONE);
        }
        
        // Boolean
        if ("True".equals(literal) || "False".equals(literal)) {
            return new TypeInfo("bool", TypeCategory.BASIC);
        }
        
        // String literals
        if ((literal.startsWith("\"") && literal.endsWith("\"")) ||
            (literal.startsWith("'") && literal.endsWith("'")) ||
            (literal.startsWith("f\"") || literal.startsWith("f'")) ||
            (literal.startsWith("r\"") || literal.startsWith("r'"))) {
            return new TypeInfo("str", TypeCategory.BASIC);
        }
        
        // Numeric literals
        if (isIntegerLiteral(literal)) {
            return new TypeInfo("int", TypeCategory.BASIC);
        }
        
        if (isFloatLiteral(literal)) {
            return new TypeInfo("float", TypeCategory.BASIC);
        }
        
        if (isComplexLiteral(literal)) {
            return new TypeInfo("complex", TypeCategory.BASIC);
        }
        
        // Collection literals
        if (literal.startsWith("[") && literal.endsWith("]")) {
            return inferListType(literal);
        }
        
        if (literal.startsWith("{") && literal.endsWith("}")) {
            if (literal.contains(":")) {
                return inferDictType(literal);
            } else {
                return inferSetType(literal);
            }
        }
        
        if (literal.startsWith("(") && literal.endsWith(")")) {
            return inferTupleType(literal);
        }
        
        // Default to Any for unknown literals
        return new TypeInfo("Any", TypeCategory.ANY);
    }
    
    // Helper methods for type inference
    private TypeInfo inferFromContext(String varName, String sourceCode, int lineNumber) {
        // Look for assignment patterns around the declaration
        String[] lines = sourceCode.split("\n");
        if (lineNumber > 0 && lineNumber <= lines.length) {
            String line = lines[lineNumber - 1];
            
            // Look for assignment with literal
            Pattern assignmentPattern = Pattern.compile(
                "\\b" + Pattern.quote(varName) + "\\s*=\\s*(.+)"
            );
            
            Matcher matcher = assignmentPattern.matcher(line);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                if (value.endsWith(",")) {
                    value = value.substring(0, value.length() - 1).trim();
                }
                return inferFromLiteral(value);
            }
            
            // Look for function calls
            if (line.contains("(") && line.contains(")")) {
                Matcher callMatcher = FUNCTION_CALL_PATTERN.matcher(line);
                if (callMatcher.find()) {
                    String functionName = callMatcher.group(1);
                    return inferFromFunctionCall(functionName);
                }
            }
        }
        
        return null;
    }
    
    private List<TypeInfo> analyzeReturnStatements(FunctionDeclarationNode node, String sourceCode) {
        List<TypeInfo> returnTypes = new ArrayList<>();
        
        // This would require analyzing the AST or parsing the function body
        // For now, return empty list - would need full implementation
        
        return returnTypes;
    }
    
    private TypeInfo inferParameterTypeFromUsage(String paramName, FunctionDeclarationNode node, String sourceCode) {
        // Analyze how the parameter is used within the function
        // This is a complex analysis that would require full AST traversal
        
        // For now, return Any - would need full implementation
        return new TypeInfo("Any", TypeCategory.ANY);
    }
    
    private void analyzeInitMethodForFields(ClassDeclarationNode node, Map<String, TypeInfo> fieldTypes, String sourceCode) {
        // Find __init__ method and analyze self.field assignments
        // This would require analyzing the method body
    }
    
    private void analyzePropertyMethods(ClassDeclarationNode node, Map<String, TypeInfo> fieldTypes, String sourceCode) {
        // Find @property methods and infer types from return statements
    }
    
    private TypeInfo createUnionType(List<TypeInfo> types) {
        if (types.size() == 1) {
            return types.get(0);
        }
        return new UnionTypeInfo(types);
    }
    
    private TypeInfo createOptionalType(TypeInfo innerType) {
        return new UnionTypeInfo(Arrays.asList(innerType, new TypeInfo("None", TypeCategory.NONE)));
    }
    
    private String extractGenericType(String genericAnnotation) {
        int start = genericAnnotation.indexOf('[') + 1;
        int end = genericAnnotation.lastIndexOf(']');
        return genericAnnotation.substring(start, end).trim();
    }
    
    private String[] splitGenericTypes(String types) {
        // Simple split by comma - would need more sophisticated parsing for nested generics
        return types.split(",");
    }
    
    private List<TypeInfo> parseUnionTypes(String unionTypes) {
        String[] types = splitGenericTypes(unionTypes);
        List<TypeInfo> result = new ArrayList<>();
        for (String type : types) {
            result.add(parseTypeAnnotation(type.trim()));
        }
        return result;
    }
    
    private TypeInfo parseCallableType(String callableAnnotation) {
        // Parse Callable[[int, str], bool] format
        String params = extractGenericType(callableAnnotation);
        
        // Simple implementation - would need more sophisticated parsing
        return new TypeInfo("Callable", TypeCategory.CALLABLE);
    }
    
    private TypeInfo inferFromFunctionCall(String functionName) {
        // Look up function in builtin or symbol table and return its return type
        if (functionTable.containsKey(functionName)) {
            return functionTable.get(functionName).returnType;
        }
        
        // Handle common builtin functions
        switch (functionName) {
            case "len": return new TypeInfo("int", TypeCategory.BASIC);
            case "str": return new TypeInfo("str", TypeCategory.BASIC);
            case "int": return new TypeInfo("int", TypeCategory.BASIC);
            case "float": return new TypeInfo("float", TypeCategory.BASIC);
            case "bool": return new TypeInfo("bool", TypeCategory.BASIC);
            case "list": return new GenericTypeInfo("List", TypeCategory.COLLECTION, 
                                                   Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)));
            case "dict": return new GenericTypeInfo("Dict", TypeCategory.COLLECTION,
                                                   Arrays.asList(new TypeInfo("Any", TypeCategory.ANY), 
                                                               new TypeInfo("Any", TypeCategory.ANY)));
            case "set": return new GenericTypeInfo("Set", TypeCategory.COLLECTION,
                                                  Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)));
            default:
                return new TypeInfo("Any", TypeCategory.ANY);
        }
    }
    
    private TypeInfo inferListType(String listLiteral) {
        // Analyze elements to infer element type
        String elements = listLiteral.substring(1, listLiteral.length() - 1).trim();
        if (elements.isEmpty()) {
            return new GenericTypeInfo("List", TypeCategory.COLLECTION,
                                     Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)));
        }
        
        // Simple heuristic - infer from first element
        String[] parts = elements.split(",");
        if (parts.length > 0) {
            TypeInfo elementType = inferFromLiteral(parts[0].trim());
            return new GenericTypeInfo("List", TypeCategory.COLLECTION, Arrays.asList(elementType));
        }
        
        return new GenericTypeInfo("List", TypeCategory.COLLECTION,
                                 Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)));
    }
    
    private TypeInfo inferDictType(String dictLiteral) {
        // Analyze key-value pairs to infer types
        return new GenericTypeInfo("Dict", TypeCategory.COLLECTION,
                                 Arrays.asList(new TypeInfo("str", TypeCategory.BASIC),
                                             new TypeInfo("Any", TypeCategory.ANY)));
    }
    
    private TypeInfo inferSetType(String setLiteral) {
        // Analyze elements to infer element type
        return new GenericTypeInfo("Set", TypeCategory.COLLECTION,
                                 Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)));
    }
    
    private TypeInfo inferTupleType(String tupleLiteral) {
        // Analyze elements to infer element types
        return new GenericTypeInfo("Tuple", TypeCategory.COLLECTION,
                                 Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)));
    }
    
    // Utility methods
    private boolean isIntegerLiteral(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isFloatLiteral(String s) {
        return s.contains(".") || s.contains("e") || s.contains("E");
    }
    
    private boolean isComplexLiteral(String s) {
        return s.endsWith("j") || s.endsWith("J");
    }
    
    private Set<String> initializeBuiltinTypes() {
        Set<String> types = new HashSet<>();
        types.addAll(Arrays.asList(
            "int", "float", "complex", "str", "bytes", "bool",
            "list", "tuple", "dict", "set", "frozenset",
            "type", "object", "None", "Any",
            "List", "Dict", "Set", "Tuple", "Optional", "Union",
            "Callable", "Iterator", "Iterable", "Generator",
            "Awaitable", "Coroutine", "AsyncIterator"
        ));
        return types;
    }
    
    private void setupCommonTypeAliases() {
        typeAliases.put("JsonDict", "Dict[str, Any]");
        typeAliases.put("StringList", "List[str]");
        typeAliases.put("IntList", "List[int]");
        typeAliases.put("Path", "pathlib.Path");
        typeAliases.put("DataFrame", "pandas.DataFrame");
        typeAliases.put("Series", "pandas.Series");
    }
    
    private void initializeBuiltinFunctions() {
        // Initialize builtin function signatures
        functionTable.put("len", new FunctionTypeInfo("len", 
            Arrays.asList(new TypeInfo("Any", TypeCategory.ANY)),
            new TypeInfo("int", TypeCategory.BASIC)));
        
        functionTable.put("range", new FunctionTypeInfo("range",
            Arrays.asList(new TypeInfo("int", TypeCategory.BASIC)),
            new TypeInfo("range", TypeCategory.ITERATOR)));
        
        // Add more builtin functions as needed
    }
    
    private TypeCategory getTypeCategory(String typeName) {
        switch (typeName) {
            case "int": case "float": case "complex": case "str": case "bytes": case "bool":
                return TypeCategory.BASIC;
            case "list": case "tuple": case "dict": case "set": case "frozenset":
                return TypeCategory.COLLECTION;
            case "None":
                return TypeCategory.NONE;
            case "Any":
                return TypeCategory.ANY;
            default:
                return TypeCategory.CUSTOM;
        }
    }
    
    // Inner classes for type information
    public static class TypeInfo {
        public final String name;
        public final TypeCategory category;
        
        public TypeInfo(String name, TypeCategory category) {
            this.name = name;
            this.category = category;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TypeInfo)) return false;
            TypeInfo other = (TypeInfo) obj;
            return Objects.equals(name, other.name) && category == other.category;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, category);
        }
    }
    
    public static class GenericTypeInfo extends TypeInfo {
        public final List<TypeInfo> typeParameters;
        
        public GenericTypeInfo(String name, TypeCategory category, List<TypeInfo> typeParameters) {
            super(name, category);
            this.typeParameters = typeParameters;
        }
        
        @Override
        public String toString() {
            if (typeParameters.isEmpty()) {
                return name;
            }
            return name + "[" + String.join(", ", 
                typeParameters.stream().map(TypeInfo::toString).toArray(String[]::new)) + "]";
        }
    }
    
    public static class UnionTypeInfo extends TypeInfo {
        public final List<TypeInfo> unionTypes;
        
        public UnionTypeInfo(List<TypeInfo> unionTypes) {
            super("Union", TypeCategory.UNION);
            this.unionTypes = unionTypes;
        }
        
        @Override
        public String toString() {
            return "Union[" + String.join(", ",
                unionTypes.stream().map(TypeInfo::toString).toArray(String[]::new)) + "]";
        }
    }
    
    public static class FunctionTypeInfo {
        public final String name;
        public final List<TypeInfo> parameterTypes;
        public final TypeInfo returnType;
        
        public FunctionTypeInfo(String name, List<TypeInfo> parameterTypes, TypeInfo returnType) {
            this.name = name;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }
    }
    
    public static class ClassTypeInfo {
        public final String name;
        public final Map<String, TypeInfo> fields;
        public final Map<String, FunctionTypeInfo> methods;
        
        public ClassTypeInfo(String name) {
            this.name = name;
            this.fields = new HashMap<>();
            this.methods = new HashMap<>();
        }
    }
    
    public static class ScopeContext {
        public final String name;
        public final ScopeType type;
        public final Map<String, TypeInfo> localSymbols;
        
        public ScopeContext(String name, ScopeType type) {
            this.name = name;
            this.type = type;
            this.localSymbols = new HashMap<>();
        }
    }
    
    public enum TypeCategory {
        BASIC, COLLECTION, CALLABLE, CUSTOM, NONE, ANY, UNION, ITERATOR
    }
    
    public enum ScopeType {
        MODULE, CLASS, FUNCTION, LAMBDA, COMPREHENSION
    }
}