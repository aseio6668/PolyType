package com.davajava.migrator.translator.csharp;

import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.ast.*;

public class CSharpToJavaVisitor implements ASTVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;

    public CSharpToJavaVisitor() {
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
        output.append("// Generated from C# source code\n");
        output.append("// Migrated using DavaJava Code Migrator\n\n");
        
        if (options.getBooleanOption("csharpSpecific.generateImports", true)) {
            output.append("import java.util.*;\n");
            output.append("import java.math.BigDecimal;\n");
            output.append("import java.util.stream.*;\n");
            output.append("import java.util.function.*;\n\n");
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
        
        if (node.isPublic()) {
            output.append("public ");
        } else {
            output.append("private ");
        }
        
        output.append("class ").append(node.getName()).append(" {\n");
        indentLevel++;
        
        // Process all child nodes (methods, fields, etc.)
        boolean hasConstructor = false;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (func.getName().equals(node.getName())) {
                    hasConstructor = true;
                }
            }
            child.accept(this);
        }
        
        // Add default constructor if none present
        if (!hasConstructor) {
            indent();
            output.append("public ").append(node.getName()).append("() {\n");
            indentLevel++;
            indent();
            output.append("// Default constructor\n");
            indentLevel--;
            indent();
            output.append("}\n\n");
        }
        
        indentLevel--;
        indent();
        output.append("}");
        
        return "";
    }

    @Override
    public String visitFunctionDeclaration(FunctionDeclarationNode node) {
        indent();
        
        if (node.isPublic()) {
            output.append("public ");
        } else {
            output.append("private ");
        }
        
        if (node.isStatic()) {
            output.append("static ");
        }
        
        // Handle C# properties converted to getter/setter methods
        if (node.getName().startsWith("get") || node.getName().startsWith("set")) {
            // This is a property accessor
            output.append(node.getReturnType()).append(" ").append(node.getName()).append("(");
        } else {
            output.append(node.getReturnType()).append(" ").append(node.getName()).append("(");
        }
        
        for (int i = 0; i < node.getParameters().size(); i++) {
            if (i > 0) output.append(", ");
            node.getParameters().get(i).accept(this);
        }
        
        output.append(") {\n");
        indentLevel++;
        
        // Generate method body based on method type
        if (node.getName().startsWith("get")) {
            // Getter method
            String propertyName = node.getName().substring(3).toLowerCase();
            if (propertyName.length() > 1) {
                propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
            }
            indent();
            output.append("return this.").append(propertyName).append(";\n");
        } else if (node.getName().startsWith("set")) {
            // Setter method
            String propertyName = node.getName().substring(3).toLowerCase();
            if (propertyName.length() > 1) {
                propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
            }
            indent();
            output.append("this.").append(propertyName).append(" = value;\n");
        } else {
            // Regular method
            indent();
            output.append("// TODO: Implement method body from C#\n");
            
            if (!"void".equals(node.getReturnType())) {
                indent();
                output.append("return ").append(getDefaultValue(node.getReturnType())).append(";\n");
            }
        }
        
        indentLevel--;
        indent();
        output.append("}\n\n");
        
        return "";
    }

    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        indent();
        
        // For class fields, make them private by default
        output.append("private ");
        
        output.append(node.getDataType()).append(" ").append(node.getName());
        
        // Add default initialization for certain types
        String defaultValue = getDefaultInitialization(node.getDataType());
        if (defaultValue != null) {
            output.append(" = ").append(defaultValue);
        }
        
        output.append(";\n");
        return "";
    }

    @Override
    public String visitParameter(ParameterNode node) {
        output.append(node.getDataType()).append(" ").append(node.getName());
        return "";
    }

    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }

    private String getDefaultValue(String type) {
        switch (type) {
            case "int": case "long": case "short": case "byte": return "0";
            case "float": case "double": return "0.0";
            case "boolean": return "false";
            case "char": return "'\\0'";
            case "BigDecimal": return "BigDecimal.ZERO";
            default: return "null";
        }
    }

    private String getDefaultInitialization(String type) {
        if (type.endsWith("[]")) {
            return "new " + type.substring(0, type.length() - 2) + "[0]";
        }
        switch (type) {
            case "String": return "\"\"";
            case "List": return "new ArrayList<>()";
            case "Map": return "new HashMap<>()";
            case "Set": return "new HashSet<>()";
            case "Queue": return "new LinkedList<>()";
            case "Stack": return "new Stack<>()";
            case "BigDecimal": return "BigDecimal.ZERO";
            default: return null;
        }
    }

    // Placeholder implementations for other visitor methods
    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        // C# structs become classes in Java
        return visitClassDeclaration(new ClassDeclarationNode(node.getName(), true, 
            node.getLineNumber(), node.getColumnNumber()));
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
        String value = node.getValue().toString();
        LiteralNode.LiteralType type = node.getLiteralType();
        
        // Handle C# specific literal conversions
        if (type == LiteralNode.LiteralType.STRING) {
            // Convert C# string literals to Java
            if (!value.startsWith("\"") && !value.startsWith("'")) {
                value = "\"" + value + "\"";
            }
        } else if (type == LiteralNode.LiteralType.BOOLEAN) {
            // C# uses True/False, Java uses true/false
            value = "True".equals(value) ? "true" : "False".equals(value) ? "false" : value.toLowerCase();
        } else if ("null".equals(value)) {
            value = "null";
        }
        
        output.append(value);
        return "";
    }

    @Override
    public String visitIdentifier(IdentifierNode node) {
        output.append(node.getName());
        return "";
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