package com.davajava.migrator.translator.javascript;

import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.ast.*;

public class JavaScriptToJavaVisitor implements ASTVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;

    public JavaScriptToJavaVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
    }

    public void setOptions(TranslationOptions options) {
        this.options = options;
        this.output.setLength(0);
        this.indentLevel = 0;
    }

    @Override
    public String visitProgram(ProgramNode node) {
        output.append("// Generated from JavaScript/TypeScript source code\n");
        output.append("// Migrated using DavaJava Code Migrator\n");
        output.append("// Note: Dynamic features converted to Java patterns\n\n");
        
        if (options.getBooleanOption("jsSpecific.generateImports", true)) {
            output.append("import java.util.*;\n");
            output.append("import java.util.function.*;\n");
            output.append("import java.util.concurrent.CompletableFuture;\n");
            output.append("import java.util.concurrent.CompletionStage;\n");
            output.append("import java.util.stream.*;\n");
            output.append("import java.math.BigDecimal;\n");
            output.append("import java.time.*;\n");
            output.append("import java.util.regex.Pattern;\n\n");
        }
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            output.append("\n");
        }
        
        return output.toString();
    }

    @Override
    public String visitClassDeclaration(ClassDeclarationNode node) {
        indent();
        
        // Check if it's a TypeScript interface
        boolean isInterface = isTypeScriptInterface(node);
        
        if (isInterface) {
            output.append("// Converted from TypeScript interface\n");
            indent();
            output.append("public abstract class ").append(node.getName()).append(" {\n");
        } else {
            if (node.isPublic()) {
                output.append("public ");
            } else {
                output.append("private ");
            }
            
            output.append("class ").append(node.getName()).append(" {\n");
        }
        
        indentLevel++;
        
        // Add default constructor if needed
        boolean hasConstructor = hasConstructor(node);
        if (!hasConstructor && !isInterface) {
            indent();
            output.append("public ").append(node.getName()).append("() {\n");
            indentLevel++;
            indent();
            output.append("// Default constructor\n");
            indentLevel--;
            indent();
            output.append("}\n\n");
        }
        
        // Process child nodes
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("}");
        
        return "";
    }

    @Override
    public String visitFunctionDeclaration(FunctionDeclarationNode node) {
        indent();
        
        // Determine if this is a constructor
        boolean isConstructor = isConstructor(node);
        
        if (node.isPublic()) {
            output.append("public ");
        } else {
            output.append("private ");
        }
        
        if (node.isStatic() || isTopLevelFunction()) {
            output.append("static ");
        }
        
        // Handle async functions
        String returnType = node.getReturnType();
        if ("Promise".equals(returnType) || returnType.startsWith("Promise<")) {
            if (returnType.startsWith("Promise<") && returnType.endsWith(">")) {
                String innerType = returnType.substring(8, returnType.length() - 1);
                returnType = "CompletableFuture<" + mapJSTypeToJava(innerType) + ">";
            } else {
                returnType = "CompletableFuture<Object>";
            }
        } else {
            returnType = mapJSTypeToJava(returnType);
        }
        
        if (isConstructor) {
            output.append(getCurrentClassName()).append("(");
        } else {
            output.append(returnType).append(" ").append(node.getName()).append("(");
        }
        
        for (int i = 0; i < node.getParameters().size(); i++) {
            if (i > 0) output.append(", ");
            node.getParameters().get(i).accept(this);
        }
        
        output.append(") {\n");
        indentLevel++;
        
        // Generate method body
        if (isConstructor) {
            generateConstructorBody(node);
        } else if ("main".equals(node.getName()) && node.isStatic()) {
            generateMainMethod();
        } else {
            generateMethodBody(node, returnType);
        }
        
        indentLevel--;
        indent();
        output.append("}\n\n");
        
        return "";
    }

    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        indent();
        
        if (isInClass()) {
            output.append("private ");
        } else {
            output.append("static ");
        }
        
        // JavaScript const becomes final
        if (!node.isMutable()) {
            output.append("final ");
        }
        
        String javaType = mapJSTypeToJava(node.getDataType());
        output.append(javaType).append(" ").append(node.getName());
        
        // Add default initialization if needed
        String defaultValue = getDefaultInitialization(javaType);
        if (defaultValue != null) {
            output.append(" = ").append(defaultValue);
        }
        
        output.append(";\n");
        return "";
    }

    @Override
    public String visitParameter(ParameterNode node) {
        String javaType = mapJSTypeToJava(node.getDataType());
        output.append(javaType).append(" ").append(node.getName());
        return "";
    }
    
    private boolean isTypeScriptInterface(ClassDeclarationNode node) {
        // Check if all methods are abstract (no implementation)
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                // In a more complete implementation, check for abstract methods
                return true; // Assume interfaces for now
            }
        }
        return false;
    }
    
    private boolean hasConstructor(ClassDeclarationNode node) {
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if ("constructor".equals(func.getName()) || node.getName().equals(func.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isConstructor(FunctionDeclarationNode node) {
        return "constructor".equals(node.getName());
    }
    
    private boolean isTopLevelFunction() {
        // Would need context tracking in a full implementation
        return false;
    }
    
    private boolean isInClass() {
        // Would need context tracking in a full implementation
        return true;
    }
    
    private String getCurrentClassName() {
        // Would need context tracking in a full implementation
        return "UnknownClass";
    }
    
    private void generateConstructorBody(FunctionDeclarationNode node) {
        indent();
        output.append("// Constructor implementation from JavaScript\n");
        
        // Initialize fields based on parameters
        for (ParameterNode param : node.getParameters()) {
            indent();
            output.append("this.").append(param.getName()).append(" = ").append(param.getName()).append(";\n");
        }
    }
    
    private void generateMainMethod() {
        indent();
        output.append("// Main method implementation\n");
        indent();
        output.append("// TODO: Implement main logic from JavaScript\n");
    }
    
    private void generateMethodBody(FunctionDeclarationNode node, String returnType) {
        indent();
        output.append("// Method implementation from JavaScript\n");
        
        // Handle async methods
        if (returnType.startsWith("CompletableFuture")) {
            indent();
            output.append("return CompletableFuture.supplyAsync(() -> {\n");
            indentLevel++;
            indent();
            output.append("// TODO: Implement async logic\n");
            indent();
            if (returnType.contains("<Object>")) {
                output.append("return null;\n");
            } else {
                String innerType = extractGenericType(returnType);
                output.append("return ").append(getDefaultValue(innerType)).append(";\n");
            }
            indentLevel--;
            indent();
            output.append("});\n");
        } else if (!"void".equals(returnType)) {
            indent();
            output.append("return ").append(getDefaultValue(returnType)).append(";\n");
        } else {
            indent();
            output.append("// TODO: Implement method logic\n");
        }
    }
    
    private String mapJSTypeToJava(String jsType) {
        if (jsType == null) return "Object";
        
        jsType = jsType.trim();
        
        switch (jsType) {
            case "string": return "String";
            case "number": return "Double";
            case "bigint": return "BigInteger";
            case "boolean": return "Boolean";
            case "object": return "Object";
            case "any": return "Object";
            case "void": return "void";
            case "undefined": return "Object";
            case "null": return "Object";
            case "unknown": return "Object";
            
            // Array types
            case "string[]": return "String[]";
            case "number[]": return "Double[]";
            case "boolean[]": return "Boolean[]";
            case "Array<string>": return "List<String>";
            case "Array<number>": return "List<Double>";
            case "Array<boolean>": return "List<Boolean>";
            
            // Common JavaScript objects
            case "Date": return "LocalDateTime";
            case "RegExp": return "Pattern";
            case "Error": return "Exception";
            case "Promise": return "CompletableFuture<Object>";
            case "Map": return "Map<String, Object>";
            case "Set": return "Set<Object>";
            case "WeakMap": return "WeakHashMap<Object, Object>";
            case "WeakSet": return "Set<Object>"; // No direct equivalent
            
            // Node.js specific
            case "Buffer": return "ByteBuffer";
            
            // Functions
            case "Function": return "Function<Object, Object>";
            
            default:
                // Handle generic types
                if (jsType.contains("<") && jsType.contains(">")) {
                    return mapGenericType(jsType);
                }
                
                // Handle union types (basic)
                if (jsType.contains("|")) {
                    return "Object"; // Union types become Object
                }
                
                // Handle Promise types
                if (jsType.startsWith("Promise<") && jsType.endsWith(">")) {
                    String innerType = jsType.substring(8, jsType.length() - 1);
                    return "CompletableFuture<" + mapJSTypeToJava(innerType) + ">";
                }
                
                // Custom types - assume they're classes
                return jsType;
        }
    }
    
    private String mapGenericType(String genericType) {
        // Handle Array<T>, Map<K,V>, etc.
        if (genericType.startsWith("Array<") && genericType.endsWith(">")) {
            String innerType = genericType.substring(6, genericType.length() - 1);
            return "List<" + mapJSTypeToJava(innerType) + ">";
        }
        
        if (genericType.startsWith("Map<") && genericType.endsWith(">")) {
            String inner = genericType.substring(4, genericType.length() - 1);
            String[] types = inner.split(",", 2);
            if (types.length == 2) {
                String keyType = mapJSTypeToJava(types[0].trim());
                String valueType = mapJSTypeToJava(types[1].trim());
                return "Map<" + keyType + ", " + valueType + ">";
            }
        }
        
        if (genericType.startsWith("Set<") && genericType.endsWith(">")) {
            String innerType = genericType.substring(4, genericType.length() - 1);
            return "Set<" + mapJSTypeToJava(innerType) + ">";
        }
        
        return genericType; // Keep as-is for unrecognized generics
    }
    
    private String extractGenericType(String type) {
        if (type.contains("<") && type.contains(">")) {
            int start = type.indexOf("<") + 1;
            int end = type.lastIndexOf(">");
            return type.substring(start, end);
        }
        return "Object";
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }
    
    private String getDefaultValue(String type) {
        switch (type) {
            case "int": case "Integer": return "0";
            case "long": case "Long": return "0L";
            case "short": case "Short": return "(short) 0";
            case "byte": case "Byte": return "(byte) 0";
            case "float": case "Float": return "0.0f";
            case "double": case "Double": return "0.0";
            case "boolean": case "Boolean": return "false";
            case "char": case "Character": return "'\\0'";
            case "String": return "\"\"";
            default: return "null";
        }
    }
    
    private String getDefaultInitialization(String type) {
        if (type.endsWith("[]")) {
            String baseType = type.substring(0, type.length() - 2);
            return "new " + baseType + "[0]";
        }
        switch (type) {
            case "List": return "new ArrayList<>()";
            case "Map": return "new HashMap<>()";
            case "Set": return "new HashSet<>()";
            case "Queue": return "new LinkedList<>()";
            case "Stack": return "new Stack<>()";
            case "Optional": return "Optional.empty()";
            default:
                if (type.startsWith("List<")) return "new ArrayList<>()";
                if (type.startsWith("Map<")) return "new HashMap<>()";
                if (type.startsWith("Set<")) return "new HashSet<>()";
                return null;
        }
    }

    // Placeholder implementations for other visitor methods
    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        return "// Struct not applicable for JavaScript";
    }

    @Override
    public String visitExpression(ExpressionNode node) {
        return "// Expression not yet implemented";
    }

    @Override
    public String visitBinaryExpression(BinaryExpressionNode node) {
        return "// Binary expression not yet implemented";
    }

    @Override
    public String visitUnaryExpression(UnaryExpressionNode node) {
        return "// Unary expression not yet implemented";
    }

    @Override
    public String visitLiteral(LiteralNode node) {
        return "// Literal not yet implemented";
    }

    @Override
    public String visitIdentifier(IdentifierNode node) {
        return "// Identifier not yet implemented";
    }

    @Override
    public String visitBlockStatement(BlockStatementNode node) {
        return "// Block statement not yet implemented";
    }

    @Override
    public String visitIfStatement(IfStatementNode node) {
        return "// If statement not yet implemented";
    }

    @Override
    public String visitWhileLoop(WhileLoopNode node) {
        return "// While loop not yet implemented";
    }

    @Override
    public String visitForLoop(ForLoopNode node) {
        return "// For loop not yet implemented";
    }

    @Override
    public String visitReturnStatement(ReturnStatementNode node) {
        return "// Return statement not yet implemented";
    }

    @Override
    public String visitAssignment(AssignmentNode node) {
        return "// Assignment not yet implemented";
    }

    @Override
    public String visitFunctionCall(FunctionCallNode node) {
        return "// Function call not yet implemented";
    }

    @Override
    public String visitMethodCall(MethodCallNode node) {
        return "// Method call not yet implemented";
    }

    @Override
    public String visitFieldAccess(FieldAccessNode node) {
        return "// Field access not yet implemented";
    }

    @Override
    public String visitArrayAccess(ArrayAccessNode node) {
        return "// Array access not yet implemented";
    }

    @Override
    public String visitTypeAnnotation(TypeAnnotationNode node) {
        return "// Type annotation not yet implemented";
    }

    @Override
    public String visitComment(CommentNode node) {
        return "// Comment not yet implemented";
    }
}