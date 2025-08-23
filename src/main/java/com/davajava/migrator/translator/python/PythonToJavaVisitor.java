package com.davajava.migrator.translator.python;

import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.ast.*;

public class PythonToJavaVisitor implements ASTVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;

    public PythonToJavaVisitor() {
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
        output.append("// Generated from Python source code\n");
        output.append("// Migrated using DavaJava Code Migrator\n\n");
        
        // Add common imports for Python translations
        if (options.getBooleanOption("pythonSpecific.generateImports", true)) {
            output.append("import java.util.*;\n");
            output.append("import java.util.stream.*;\n\n");
        }
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            output.append("\n");
        }
        
        return output.toString();
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
        
        output.append(node.getReturnType()).append(" ");
        output.append(node.getName()).append("(");
        
        for (int i = 0; i < node.getParameters().size(); i++) {
            if (i > 0) output.append(", ");
            node.getParameters().get(i).accept(this);
        }
        
        output.append(") {\n");
        indentLevel++;
        
        indent();
        output.append("// TODO: Implement method body from Python\n");
        
        if (!"void".equals(node.getReturnType())) {
            indent();
            output.append("return ").append(getDefaultValue(node.getReturnType())).append(";\n");
        }
        
        indentLevel--;
        indent();
        output.append("}");
        
        return "";
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
        
        indent();
        output.append("// TODO: Add fields and methods from Python class\n");
        
        indentLevel--;
        indent();
        output.append("}");
        
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
            default: return "null";
        }
    }

    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        indent();
        
        String type = node.getDataType();
        if (type == null || type.isEmpty() || "auto".equals(type)) {
            type = "var"; // Use Java's type inference
        }
        
        output.append(type).append(" ").append(node.getName());
        
        if (node.getInitializer() != null) {
            output.append(" = ");
            node.getInitializer().accept(this);
        }
        
        output.append(";\n");
        return "";
    }

    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        return "// Struct declaration not applicable for Python";
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
        
        // Handle Python-specific literal conversions
        if (type == LiteralNode.LiteralType.STRING || value.startsWith("\"") || value.startsWith("'")) {
            if (!value.startsWith("\"") && !value.startsWith("'")) {
                value = "\"" + value + "\"";
            }
            // Convert Python single quotes to Java double quotes
            if (value.startsWith("'") && value.endsWith("'")) {
                value = "\"" + value.substring(1, value.length() - 1) + "\"";
            }
        } else if (type == LiteralNode.LiteralType.BOOLEAN) {
            value = "True".equals(value) ? "true" : "False".equals(value) ? "false" : value;
        } else if ("None".equals(value) || type == LiteralNode.LiteralType.NULL) {
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
        indent();
        output.append("if (");
        
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        } else {
            output.append("true");
        }
        
        output.append(") {\n");
        indentLevel++;
        
        if (node.getThenStatement() != null) {
            node.getThenStatement().accept(this);
        } else {
            indent();
            output.append("// TODO: Implement if body\n");
        }
        
        indentLevel--;
        indent();
        output.append("}");
        
        if (node.getElseStatement() != null) {
            output.append(" else {\n");
            indentLevel++;
            node.getElseStatement().accept(this);
            indentLevel--;
            indent();
            output.append("}");
        }
        
        output.append("\n");
        return "";
    }

    @Override
    public String visitWhileLoop(WhileLoopNode node) {
        return "// While loop not yet implemented";
    }

    @Override
    public String visitForLoop(ForLoopNode node) {
        indent();
        
        // For Python enhanced for-loops, we need to handle them differently
        // Since the AST is designed for C-style loops, we'll create a simple template
        output.append("for (var item : collection) {\n");
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        } else {
            indent();
            output.append("// TODO: Implement for loop body\n");
        }
        
        indentLevel--;
        indent();
        output.append("}\n");
        
        return "";
    }

    @Override
    public String visitReturnStatement(ReturnStatementNode node) {
        return "// Return statement not yet implemented";
    }

    @Override
    public String visitAssignment(AssignmentNode node) {
        indent();
        
        if (node.getTarget() != null) {
            node.getTarget().accept(this);
        }
        
        output.append(" = ");
        
        if (node.getValue() != null) {
            node.getValue().accept(this);
        }
        
        output.append(";\n");
        return "";
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