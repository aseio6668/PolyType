package com.davajava.migrator.translator.kotlin;

import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.ast.*;

public class KotlinToJavaVisitor implements ASTVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;

    public KotlinToJavaVisitor() {
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
        output.append("// Generated from Kotlin source code\n");
        output.append("// Migrated using DavaJava Code Migrator\n");
        output.append("// Note: Kotlin-specific features like extension functions have been converted to Java patterns\n\n");
        
        if (options.getBooleanOption("kotlinSpecific.generateImports", true)) {
            output.append("import java.util.*;\n");
            output.append("import java.util.function.*;\n");
            output.append("import java.util.stream.*;\n");
            output.append("import java.util.concurrent.*;\n\n");
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
        
        // Check if this is a data class by looking for equals/hashCode/toString methods
        boolean isDataClass = isDataClass(node);
        
        if (isDataClass) {
            output.append("\n");
            indent();
            output.append("// This class was converted from a Kotlin data class\n");
        }
        
        // Process all child nodes
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
        
        if (node.isPublic()) {
            output.append("public ");
        } else {
            output.append("private ");
        }
        
        if (node.isStatic()) {
            output.append("static ");
        }
        
        // Handle extension functions (converted to static methods)
        if (isExtensionFunction(node)) {
            output.append("// Extension function converted to static method\n");
            indent();
            output.append("public static ");
        }
        
        output.append(node.getReturnType()).append(" ").append(node.getName()).append("(");
        
        for (int i = 0; i < node.getParameters().size(); i++) {
            if (i > 0) output.append(", ");
            node.getParameters().get(i).accept(this);
        }
        
        output.append(") {\n");
        indentLevel++;
        
        // Generate method body
        if (node.getName().startsWith("get")) {
            // Kotlin property getter
            String propertyName = getPropertyNameFromGetter(node.getName());
            indent();
            output.append("return this.").append(propertyName).append(";\n");
        } else if (node.getName().startsWith("set")) {
            // Kotlin property setter
            String propertyName = getPropertyNameFromSetter(node.getName());
            indent();
            output.append("this.").append(propertyName).append(" = value;\n");
        } else if ("equals".equals(node.getName())) {
            // Data class equals method
            generateEqualsMethod(node);
        } else if ("hashCode".equals(node.getName())) {
            // Data class hashCode method
            generateHashCodeMethod();
        } else if ("toString".equals(node.getName())) {
            // Data class toString method
            generateToStringMethod();
        } else {
            // Regular method
            indent();
            output.append("// TODO: Implement method body from Kotlin\n");
            
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
        
        // For class fields
        output.append("private ");
        
        output.append(node.getDataType()).append(" ").append(node.getName());
        
        // Add default initialization if needed
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

    private boolean isDataClass(ClassDeclarationNode node) {
        // Check if class has equals, hashCode, and toString methods (data class indicators)
        boolean hasEquals = false, hasHashCode = false, hasToString = false;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                switch (func.getName()) {
                    case "equals": hasEquals = true; break;
                    case "hashCode": hasHashCode = true; break;
                    case "toString": hasToString = true; break;
                }
            }
        }
        
        return hasEquals && hasHashCode && hasToString;
    }

    private boolean isExtensionFunction(FunctionDeclarationNode node) {
        // Extension functions are converted to static methods with receiver as first parameter
        return node.isStatic() && !node.getParameters().isEmpty() && 
               node.getParameters().get(0).getName().equals("receiver");
    }

    private String getPropertyNameFromGetter(String getterName) {
        if (getterName.startsWith("get") && getterName.length() > 3) {
            String propertyName = getterName.substring(3);
            return propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
        }
        return "property";
    }

    private String getPropertyNameFromSetter(String setterName) {
        if (setterName.startsWith("set") && setterName.length() > 3) {
            String propertyName = setterName.substring(3);
            return propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
        }
        return "property";
    }

    private void generateEqualsMethod(FunctionDeclarationNode node) {
        indent();
        output.append("if (this == other) return true;\n");
        indent();
        output.append("if (other == null || getClass() != other.getClass()) return false;\n");
        indent();
        output.append("// TODO: Compare relevant fields\n");
        indent();
        output.append("return true;\n");
    }

    private void generateHashCodeMethod() {
        indent();
        output.append("// TODO: Generate hash based on relevant fields\n");
        indent();
        output.append("return Objects.hash(/* fields */);\n");
    }

    private void generateToStringMethod() {
        indent();
        output.append("// TODO: Generate string representation\n");
        indent();
        output.append("return getClass().getSimpleName() + \"{\" + /* fields */ + \"}\";\n");
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

    private String getDefaultInitialization(String type) {
        if (type.endsWith("[]")) {
            return "new " + type.substring(0, type.length() - 2) + "[0]";
        }
        switch (type) {
            case "String": return "\"\"";
            case "List": return "new ArrayList<>()";
            case "Map": return "new HashMap<>()";
            case "Set": return "new HashSet<>()";
            default: return null;
        }
    }

    // Placeholder implementations for other visitor methods
    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        return "// Struct not applicable for Kotlin";
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