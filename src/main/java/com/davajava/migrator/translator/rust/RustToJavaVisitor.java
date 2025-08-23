package com.davajava.migrator.translator.rust;

import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.ast.*;

public class RustToJavaVisitor implements ASTVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;

    public RustToJavaVisitor() {
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
        output.append("// Generated from Rust source code\n");
        output.append("// Migrated using DavaJava Code Migrator\n\n");
        
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
        
        // NEW: Translate actual function body if available
        String rawBody = node.getRawBody();
        if (rawBody != null && !rawBody.trim().isEmpty()) {
            translateRustFunctionBody(rawBody.trim(), node.getReturnType());
        } else {
            // Fallback to TODO if no body captured
            indent();
            output.append("// TODO: Implement function body\n");
            
            if (!"void".equals(node.getReturnType())) {
                indent();
                output.append("return ").append(getDefaultValue(node.getReturnType())).append(";\n");
            }
        }
        
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
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Translate Rust expression to Java
     * Handles simple expressions like: a + b, a * b, function calls, etc.
     */
    private String translateRustExpression(String rustExpr, String expectedReturnType) {
        if (rustExpr == null || rustExpr.trim().isEmpty()) {
            return getDefaultValue(expectedReturnType);
        }
        
        rustExpr = rustExpr.trim();
        
        // Handle simple binary expressions: a + b, a - b, a * b, a / b
        if (rustExpr.matches("\\w+\\s*[+\\-*/]\\s*\\w+")) {
            return rustExpr; // Direct translation works for basic math
        }
        
        // Handle method calls: something.method()
        if (rustExpr.contains(".")) {
            return translateMethodCall(rustExpr);
        }
        
        // Handle function calls: function_name(args)
        if (rustExpr.matches("\\w+\\s*\\([^)]*\\)")) {
            return translateFunctionCall(rustExpr);
        }
        
        // Handle literals and simple identifiers
        if (rustExpr.matches("\\d+")) {
            return rustExpr; // Numbers translate directly
        }
        
        if (rustExpr.matches("\\d+\\.\\d+")) {
            return rustExpr; // Floats translate directly
        }
        
        if (rustExpr.equals("true") || rustExpr.equals("false")) {
            return rustExpr; // Booleans translate directly
        }
        
        // Handle simple variable references
        if (rustExpr.matches("\\w+")) {
            return rustExpr; // Simple identifiers translate directly
        }
        
        // For complex expressions, return as-is with a comment
        return rustExpr + " /* TODO: Complex expression needs manual review */";
    }
    
    private String translateMethodCall(String rustExpr) {
        // Handle common Rust method calls
        if (rustExpr.contains("println!")) {
            return rustExpr.replace("println!", "System.out.println");
        }
        
        // Handle Rust string methods
        if (rustExpr.contains(".is_empty()")) {
            return rustExpr.replace(".is_empty()", ".isEmpty()");
        }
        
        // Handle Rust vector/array methods
        if (rustExpr.contains(".len()")) {
            return rustExpr.replace(".len()", ".length");
        }
        
        // Handle Rust iterator methods (needs more complex translation)
        if (rustExpr.contains(".iter().sum()")) {
            return "Arrays.stream(" + rustExpr.substring(0, rustExpr.indexOf(".iter()")) + ").sum()";
        }
        
        if (rustExpr.contains(".sort()")) {
            String arrayName = rustExpr.substring(0, rustExpr.indexOf(".sort()"));
            return "Arrays.sort(" + arrayName + ")";
        }
        
        // For other method calls, translate directly for now
        return rustExpr;
    }
    
    private String translateFunctionCall(String rustExpr) {
        // Handle common Rust function calls
        return rustExpr; // Most function calls translate directly
    }
    
    /**
     * Translate a complete Rust function body, handling multiple statements
     */
    private void translateRustFunctionBody(String rustBody, String returnType) {
        // Split the body into statements (separated by semicolons or line breaks)
        String[] lines = rustBody.split("\\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            // Remove trailing semicolon for processing
            boolean hasSemicolon = line.endsWith(";");
            if (hasSemicolon) {
                line = line.substring(0, line.length() - 1).trim();
            }
            
            // Check if this is the last line and should be a return statement
            boolean isLastLine = (i == lines.length - 1);
            boolean shouldReturn = isLastLine && !hasSemicolon && !"void".equals(returnType);
            
            String translatedLine = translateRustExpression(line, returnType);
            
            indent();
            if (shouldReturn) {
                output.append("return ").append(translatedLine).append(";\n");
            } else {
                output.append(translatedLine);
                if (!translatedLine.endsWith(";") && !translatedLine.contains("/*")) {
                    output.append(";");
                }
                output.append("\n");
            }
        }
    }

    // Placeholder implementations for other visitor methods
    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        return "// Variable declaration not yet implemented";
    }

    @Override
    public String visitClassDeclaration(ClassDeclarationNode node) {
        return "// Class declaration not yet implemented";
    }

    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        indent();
        
        if (node.isPublic()) {
            output.append("public ");
        } else {
            output.append("private ");
        }
        
        output.append("class ").append(node.getName()).append(" {\n");
        indentLevel++;
        
        // Generate fields
        for (FieldDeclarationNode field : node.getFields()) {
            indent();
            if (field.isPublic()) {
                output.append("public ");
            } else {
                output.append("private ");
            }
            output.append(field.getDataType()).append(" ").append(field.getName()).append(";\n");
        }
        
        // Generate constructor
        if (!node.getFields().isEmpty()) {
            output.append("\n");
            indent();
            output.append("public ").append(node.getName()).append("(");
            
            for (int i = 0; i < node.getFields().size(); i++) {
                if (i > 0) output.append(", ");
                FieldDeclarationNode field = node.getFields().get(i);
                output.append(field.getDataType()).append(" ").append(field.getName());
            }
            
            output.append(") {\n");
            indentLevel++;
            
            for (FieldDeclarationNode field : node.getFields()) {
                indent();
                output.append("this.").append(field.getName())
                      .append(" = ").append(field.getName()).append(";\n");
            }
            
            indentLevel--;
            indent();
            output.append("}\n");
        }
        
        // Generate getters and setters
        for (FieldDeclarationNode field : node.getFields()) {
            output.append("\n");
            
            // Getter
            indent();
            output.append("public ").append(field.getDataType())
                  .append(" get").append(capitalize(field.getName())).append("() {\n");
            indentLevel++;
            indent();
            output.append("return ").append(field.getName()).append(";\n");
            indentLevel--;
            indent();
            output.append("}\n");
            
            // Setter (only if field is mutable or public)
            if (field.isMutable() || field.isPublic()) {
                output.append("\n");
                indent();
                output.append("public void set").append(capitalize(field.getName()))
                      .append("(").append(field.getDataType()).append(" ").append(field.getName()).append(") {\n");
                indentLevel++;
                indent();
                output.append("this.").append(field.getName())
                      .append(" = ").append(field.getName()).append(";\n");
                indentLevel--;
                indent();
                output.append("}\n");
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        
        return "";
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