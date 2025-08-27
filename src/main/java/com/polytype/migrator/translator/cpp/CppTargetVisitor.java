package com.polytype.migrator.translator.cpp;

import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.core.TargetVisitor;
import com.polytype.migrator.core.TranslationOptions;
import com.polytype.migrator.core.ast.*;

/**
 * C++ target visitor for generating C++ code from AST nodes.
 * Supports modern C++17/20 features and best practices.
 */
public class CppTargetVisitor implements TargetVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;
    
    public CppTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
    }
    
    @Override
    public void setOptions(TranslationOptions options) {
        this.options = options;
        this.output.setLength(0);
        this.indentLevel = 0;
    }
    
    @Override
    public TargetLanguage getTargetLanguage() {
        return TargetLanguage.CPP;
    }
    
    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions defaultOptions = TranslationOptions.defaultOptions();
        defaultOptions.setOption("cpp.standard", "17");
        defaultOptions.setOption("cpp.useNamespaces", true);
        defaultOptions.setOption("cpp.generateHeaders", true);
        defaultOptions.setOption("cpp.useSmartPointers", true);
        defaultOptions.setOption("cpp.generateCMake", true);
        return defaultOptions;
    }
    
    @Override
    public String generateImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("#include <iostream>\n");
        imports.append("#include <string>\n");
        imports.append("#include <vector>\n");
        imports.append("#include <memory>\n");
        imports.append("#include <functional>\n");
        imports.append("#include <optional>\n");
        imports.append("#include <variant>\n");
        imports.append("#include <map>\n");
        imports.append("#include <unordered_map>\n");
        imports.append("#include <algorithm>\n");
        imports.append("#include <thread>\n");
        imports.append("#include <future>\n");
        return imports.toString();
    }
    
    @Override
    public String generateFileHeader() {
        StringBuilder header = new StringBuilder();
        header.append("// Generated C++ code from PolyType Code Migrator\n");
        header.append("// Target: C++").append(options.getStringOption("cpp.standard", "17")).append("\n");
        header.append("// Generated on: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        header.append(generateImports()).append("\n");
        
        if (options.getBooleanOption("cpp.useNamespaces", true)) {
            header.append("namespace polytype {\n\n");
        }
        
        return header.toString();
    }
    
    @Override
    public String generateFileFooter() {
        StringBuilder footer = new StringBuilder();
        
        if (options.getBooleanOption("cpp.useNamespaces", true)) {
            footer.append("\n} // namespace polytype\n");
        }
        
        return footer.toString();
    }
    
    @Override
    public String visitProgram(ProgramNode node) {
        StringBuilder result = new StringBuilder();
        result.append(generateFileHeader());
        
        for (ASTNode child : node.getChildren()) {
            result.append(child.accept(this));
            result.append("\n");
        }
        
        result.append(generateFileFooter());
        return result.toString();
    }
    
    @Override
    public String visitClassDeclaration(ClassDeclarationNode node) {
        StringBuilder classCode = new StringBuilder();
        
        indent();
        classCode.append("class ").append(node.getName()).append(" {\n");
        
        // Generate private section first
        indentLevel++;
        boolean hasPrivateMembers = hasPrivateMembers(node);
        if (hasPrivateMembers) {
            indent();
            classCode.append("private:\n");
            indentLevel++;
            generatePrivateMembers(node, classCode);
            indentLevel--;
            classCode.append("\n");
        }
        
        // Generate public section
        indent();
        classCode.append("public:\n");
        indentLevel++;
        
        // Generate constructors
        generateConstructors(node, classCode);
        
        // Generate destructor
        indent();
        classCode.append("~").append(node.getName()).append("() = default;\n\n");
        
        // Generate public methods
        generatePublicMethods(node, classCode);
        
        indentLevel--;
        indentLevel--;
        indent();
        classCode.append("};\n");
        
        return classCode.toString();
    }
    
    @Override
    public String visitFunctionDeclaration(FunctionDeclarationNode node) {
        StringBuilder funcCode = new StringBuilder();
        
        indent();
        
        // Handle static functions
        if (node.isStatic()) {
            funcCode.append("static ");
        }
        
        // Handle inline functions
        if (shouldBeInline(node)) {
            funcCode.append("inline ");
        }
        
        // Return type
        String cppReturnType = mapToCppType(node.getReturnType());
        funcCode.append(cppReturnType).append(" ");
        
        // Function name
        funcCode.append(node.getName()).append("(");
        
        // Parameters
        for (int i = 0; i < node.getParameters().size(); i++) {
            if (i > 0) funcCode.append(", ");
            ParameterNode param = node.getParameters().get(i);
            funcCode.append(mapToCppType(param.getDataType()));
            
            // Use const reference for objects
            if (shouldUseConstRef(param.getDataType())) {
                funcCode.append(" const&");
            }
            
            funcCode.append(" ").append(param.getName());
        }
        
        funcCode.append(") {\n");
        
        // Function body
        indentLevel++;
        generateFunctionBody(node, funcCode);
        indentLevel--;
        
        indent();
        funcCode.append("}\n");
        
        return funcCode.toString();
    }
    
    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        StringBuilder varCode = new StringBuilder();
        indent();
        
        String cppType = mapToCppType(node.getDataType());
        
        // Use auto with initialization when possible
        if (hasInitializer(node)) {
            if (options.getBooleanOption("cpp.useAuto", true)) {
                varCode.append("auto ");
            } else {
                varCode.append(cppType).append(" ");
            }
        } else {
            varCode.append(cppType).append(" ");
        }
        
        varCode.append(node.getName());
        
        // Add initialization
        String defaultValue = getCppDefaultValue(cppType);
        if (defaultValue != null) {
            if (options.getBooleanOption("cpp.useUniformInit", true)) {
                varCode.append("{").append(defaultValue).append("}");
            } else {
                varCode.append(" = ").append(defaultValue);
            }
        }
        
        varCode.append(";\n");
        
        return varCode.toString();
    }
    
    @Override
    public String visitParameter(ParameterNode node) {
        // Parameters are handled in function declaration
        return "";
    }
    
    private String mapToCppType(String javaType) {
        if (javaType == null) return "auto";
        
        switch (javaType) {
            case "boolean": return "bool";
            case "byte": return "int8_t";
            case "short": return "int16_t";
            case "int": return "int32_t";
            case "long": return "int64_t";
            case "float": return "float";
            case "double": return "double";
            case "char": return "char";
            case "String": return "std::string";
            case "void": return "void";
            
            // Generic collections
            case "List": return "std::vector";
            case "Map": return "std::unordered_map";
            case "Set": return "std::unordered_set";
            case "Queue": return "std::queue";
            case "Stack": return "std::stack";
            
            // Specific generic types
            default:
                if (javaType.startsWith("List<")) {
                    String elementType = extractGenericType(javaType);
                    return "std::vector<" + mapToCppType(elementType) + ">";
                } else if (javaType.startsWith("Map<")) {
                    String[] types = extractGenericTypes(javaType, 2);
                    return "std::unordered_map<" + mapToCppType(types[0]) + ", " + mapToCppType(types[1]) + ">";
                } else if (javaType.startsWith("Set<")) {
                    String elementType = extractGenericType(javaType);
                    return "std::unordered_set<" + mapToCppType(elementType) + ">";
                } else if (javaType.startsWith("Optional<")) {
                    String elementType = extractGenericType(javaType);
                    return "std::optional<" + mapToCppType(elementType) + ">";
                } else if (javaType.startsWith("CompletableFuture<")) {
                    String elementType = extractGenericType(javaType);
                    return "std::future<" + mapToCppType(elementType) + ">";
                }
                
                // Custom types
                return javaType;
        }
    }
    
    private String extractGenericType(String genericType) {
        int start = genericType.indexOf('<') + 1;
        int end = genericType.lastIndexOf('>');
        return genericType.substring(start, end).trim();
    }
    
    private String[] extractGenericTypes(String genericType, int count) {
        String inner = extractGenericType(genericType);
        return inner.split(",", count);
    }
    
    private boolean shouldUseConstRef(String type) {
        return type.equals("String") || type.startsWith("std::") || 
               type.startsWith("List<") || type.startsWith("Map<") || type.startsWith("Set<");
    }
    
    private boolean shouldBeInline(FunctionDeclarationNode node) {
        // Small functions should be inline
        return node.getName().startsWith("get") || node.getName().startsWith("set") ||
               node.getName().length() < 20; // Simple heuristic
    }
    
    private String getCppDefaultValue(String cppType) {
        switch (cppType) {
            case "bool": return "false";
            case "int8_t": case "int16_t": case "int32_t": case "int64_t": return "0";
            case "float": case "double": return "0.0";
            case "char": return "'\\0'";
            case "std::string": return "\"\"";
            default:
                if (cppType.startsWith("std::vector")) return "{}";
                if (cppType.startsWith("std::unordered_map")) return "{}";
                if (cppType.startsWith("std::unordered_set")) return "{}";
                if (cppType.startsWith("std::optional")) return "std::nullopt";
                return null;
        }
    }
    
    private boolean hasInitializer(VariableDeclarationNode node) {
        // Check if variable has an initializer
        return false; // Simplified - would check AST for initializer
    }
    
    private boolean hasPrivateMembers(ClassDeclarationNode node) {
        return node.getChildren().stream()
                .anyMatch(child -> child instanceof VariableDeclarationNode);
    }
    
    private void generatePrivateMembers(ClassDeclarationNode node, StringBuilder classCode) {
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                classCode.append(mapToCppType(var.getDataType())).append(" ");
                classCode.append(var.getName()).append("_;\n");
            }
        }
    }
    
    private void generateConstructors(ClassDeclarationNode node, StringBuilder classCode) {
        indent();
        classCode.append("// Default constructor\n");
        indent();
        classCode.append(node.getName()).append("() = default;\n\n");
        
        // Generate parameterized constructor if there are fields
        if (hasPrivateMembers(node)) {
            indent();
            classCode.append("// Parameterized constructor\n");
            indent();
            classCode.append(node.getName()).append("(");
            
            // Generate constructor parameters
            boolean first = true;
            for (ASTNode child : node.getChildren()) {
                if (child instanceof VariableDeclarationNode) {
                    VariableDeclarationNode var = (VariableDeclarationNode) child;
                    if (!first) classCode.append(", ");
                    
                    String cppType = mapToCppType(var.getDataType());
                    if (shouldUseConstRef(var.getDataType())) {
                        classCode.append(cppType).append(" const& ");
                    } else {
                        classCode.append(cppType).append(" ");
                    }
                    classCode.append(var.getName());
                    first = false;
                }
            }
            
            classCode.append(")\n");
            indent();
            classCode.append("    : ");
            
            // Generate member initializer list
            first = true;
            for (ASTNode child : node.getChildren()) {
                if (child instanceof VariableDeclarationNode) {
                    VariableDeclarationNode var = (VariableDeclarationNode) child;
                    if (!first) classCode.append(", ");
                    classCode.append(var.getName()).append("_(").append(var.getName()).append(")");
                    first = false;
                }
            }
            
            classCode.append(" {}\n\n");
        }
    }
    
    private void generatePublicMethods(ClassDeclarationNode node, StringBuilder classCode) {
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                
                indent();
                String returnType = mapToCppType(func.getReturnType());
                classCode.append(returnType).append(" ").append(func.getName()).append("(");
                
                // Parameters
                for (int i = 0; i < func.getParameters().size(); i++) {
                    if (i > 0) classCode.append(", ");
                    ParameterNode param = func.getParameters().get(i);
                    String paramType = mapToCppType(param.getDataType());
                    if (shouldUseConstRef(param.getDataType())) {
                        classCode.append(paramType).append(" const& ");
                    } else {
                        classCode.append(paramType).append(" ");
                    }
                    classCode.append(param.getName());
                }
                
                classCode.append(") {\n");
                
                // Method body
                indentLevel++;
                generateMethodBody(func, classCode);
                indentLevel--;
                
                indent();
                classCode.append("}\n\n");
            } else if (child instanceof VariableDeclarationNode) {
                // Generate getter and setter for private members
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateGetter(var, classCode);
                if (var.isMutable()) {
                    generateSetter(var, classCode);
                }
            }
        }
    }
    
    private void generateGetter(VariableDeclarationNode var, StringBuilder classCode) {
        indent();
        String returnType = mapToCppType(var.getDataType());
        
        if (shouldUseConstRef(var.getDataType())) {
            classCode.append(returnType).append(" const& ");
        } else {
            classCode.append(returnType).append(" ");
        }
        
        classCode.append("get").append(capitalize(var.getName())).append("() const {\n");
        indentLevel++;
        indent();
        classCode.append("return ").append(var.getName()).append("_;\n");
        indentLevel--;
        indent();
        classCode.append("}\n\n");
    }
    
    private void generateSetter(VariableDeclarationNode var, StringBuilder classCode) {
        indent();
        classCode.append("void set").append(capitalize(var.getName())).append("(");
        
        String paramType = mapToCppType(var.getDataType());
        if (shouldUseConstRef(var.getDataType())) {
            classCode.append(paramType).append(" const& ");
        } else {
            classCode.append(paramType).append(" ");
        }
        
        classCode.append("value) {\n");
        indentLevel++;
        indent();
        classCode.append(var.getName()).append("_ = value;\n");
        indentLevel--;
        indent();
        classCode.append("}\n\n");
    }
    
    private void generateFunctionBody(FunctionDeclarationNode node, StringBuilder funcCode) {
        indent();
        funcCode.append("// TODO: Implement function body\n");
        
        String returnType = mapToCppType(node.getReturnType());
        if (!"void".equals(returnType)) {
            indent();
            String defaultValue = getCppDefaultValue(returnType);
            if (defaultValue != null) {
                funcCode.append("return ").append(defaultValue).append(";\n");
            } else {
                funcCode.append("return {}; // Default constructed\n");
            }
        }
    }
    
    private void generateMethodBody(FunctionDeclarationNode node, StringBuilder classCode) {
        indent();
        classCode.append("// TODO: Implement method body\n");
        
        String returnType = mapToCppType(node.getReturnType());
        if (!"void".equals(returnType)) {
            indent();
            String defaultValue = getCppDefaultValue(returnType);
            if (defaultValue != null) {
                classCode.append("return ").append(defaultValue).append(";\n");
            } else {
                classCode.append("return {}; // Default constructed\n");
            }
        }
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    // Placeholder implementations for other visitor methods
    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        // C++ structs are similar to classes but with public default access
        return visitClassDeclaration(new ClassDeclarationNode(node.getName(), true, 
            node.getLineNumber(), node.getColumnNumber()));
    }
    
    @Override
    public String visitExpression(ExpressionNode node) {
        return "/* Expression */";
    }
    
    @Override
    public String visitBinaryExpression(BinaryExpressionNode node) {
        return "/* Binary expression */";
    }
    
    @Override
    public String visitUnaryExpression(UnaryExpressionNode node) {
        return "/* Unary expression */";
    }
    
    @Override
    public String visitLiteral(LiteralNode node) {
        return "/* Literal */";
    }
    
    @Override
    public String visitIdentifier(IdentifierNode node) {
        return "/* Identifier */";
    }
    
    @Override
    public String visitBlockStatement(BlockStatementNode node) {
        return "/* Block statement */";
    }
    
    @Override
    public String visitIfStatement(IfStatementNode node) {
        return "/* If statement */";
    }
    
    @Override
    public String visitWhileLoop(WhileLoopNode node) {
        return "/* While loop */";
    }
    
    @Override
    public String visitForLoop(ForLoopNode node) {
        return "/* For loop */";
    }
    
    @Override
    public String visitReturnStatement(ReturnStatementNode node) {
        return "/* Return statement */";
    }
    
    @Override
    public String visitAssignment(AssignmentNode node) {
        return "/* Assignment */";
    }
    
    @Override
    public String visitFunctionCall(FunctionCallNode node) {
        return "/* Function call */";
    }
    
    @Override
    public String visitMethodCall(MethodCallNode node) {
        return "/* Method call */";
    }
    
    @Override
    public String visitFieldAccess(FieldAccessNode node) {
        return "/* Field access */";
    }
    
    @Override
    public String visitArrayAccess(ArrayAccessNode node) {
        return "/* Array access */";
    }
    
    @Override
    public String visitTypeAnnotation(TypeAnnotationNode node) {
        return "/* Type annotation */";
    }
    
    @Override
    public String visitComment(CommentNode node) {
        return "/* Comment */";
    }
}