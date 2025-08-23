package com.davajava.migrator.translator.scala;

import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.ast.*;

public class ScalaToJavaVisitor implements ASTVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;

    public ScalaToJavaVisitor() {
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
        output.append("// Generated from Scala source code\n");
        output.append("// Migrated using DavaJava Code Migrator\n");
        output.append("// Note: Scala functional features converted to Java 8+ patterns\n\n");
        
        if (options.getBooleanOption("scalaSpecific.generateImports", true)) {
            output.append("import java.util.*;\n");
            output.append("import java.util.function.*;\n");
            output.append("import java.util.stream.*;\n");
            output.append("import java.util.concurrent.*;\n");
            output.append("import java.math.BigDecimal;\n\n");
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
        
        // Determine class type based on methods
        boolean isCaseClass = isCaseClass(node);
        boolean isTrait = isTrait(node);
        boolean isObject = isObject(node);
        
        if (isTrait) {
            output.append("// Converted from Scala trait\n");
            indent();
            output.append("public abstract class ").append(node.getName()).append(" {\n");
        } else if (isObject) {
            output.append("// Converted from Scala object (singleton)\n");
            indent();
            output.append("public final class ").append(node.getName()).append(" {\n");
            indentLevel++;
            
            // Add singleton instance
            indent();
            output.append("private static final ").append(node.getName()).append(" INSTANCE = new ").append(node.getName()).append("();\n\n");
            
            // Private constructor for singleton
            indent();
            output.append("private ").append(node.getName()).append("() {}\n\n");
            
            // getInstance method
            indent();
            output.append("public static ").append(node.getName()).append(" getInstance() {\n");
            indentLevel++;
            indent();
            output.append("return INSTANCE;\n");
            indentLevel--;
            indent();
            output.append("}\n\n");
            
            indentLevel--;
        } else {
            if (isCaseClass) {
                output.append("// Converted from Scala case class\n");
                indent();
            }
            
            if (node.isPublic()) {
                output.append("public ");
            } else {
                output.append("private ");
            }
            
            if (isCaseClass) {
                output.append("final ");
            }
            
            output.append("class ").append(node.getName()).append(" {\n");
        }
        
        indentLevel++;
        
        // Process child nodes
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
        
        // Add case class specific methods if needed
        if (isCaseClass) {
            addCaseClassBoilerplate(node);
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
        
        if (node.isStatic() || isInObject()) {
            output.append("static ");
        }
        
        // Handle special Scala methods
        if ("apply".equals(node.getName())) {
            output.append("// Scala apply method converted\n");
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
            // Property getter
            String propertyName = getPropertyNameFromGetter(node.getName());
            indent();
            output.append("return this.").append(propertyName).append(";\n");
        } else if (node.getName().startsWith("set")) {
            // Property setter
            String propertyName = getPropertyNameFromSetter(node.getName());
            indent();
            output.append("this.").append(propertyName).append(" = value;\n");
        } else if ("equals".equals(node.getName())) {
            generateEqualsMethod();
        } else if ("hashCode".equals(node.getName())) {
            generateHashCodeMethod();
        } else if ("toString".equals(node.getName())) {
            generateToStringMethod();
        } else if ("apply".equals(node.getName())) {
            // Factory method
            indent();
            output.append("// TODO: Implement factory method logic\n");
            if (!"void".equals(node.getReturnType())) {
                indent();
                output.append("return new ").append(getCurrentClassName()).append("();\n");
            }
        } else {
            // Regular method
            indent();
            output.append("// TODO: Implement method body from Scala\n");
            
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
        
        // Fields in Scala objects become static
        if (isInObject()) {
            output.append("private static ");
        } else {
            output.append("private ");
        }
        
        // Scala vals are final
        if (!node.isMutable()) {
            output.append("final ");
        }
        
        output.append(node.getDataType()).append(" ").append(node.getName());
        
        // Add default initialization
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

    private boolean isCaseClass(ClassDeclarationNode node) {
        // Check for case class indicators (equals, hashCode, toString)
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

    private boolean isTrait(ClassDeclarationNode node) {
        // Traits have abstract methods or are marked as such
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                // In a more complete implementation, check for abstract methods
            }
        }
        return false; // Simplified for now
    }

    private boolean isObject(ClassDeclarationNode node) {
        // Objects have getInstance method
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if ("getInstance".equals(func.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInObject() {
        // Would need context tracking in a full implementation
        return false;
    }

    private String getCurrentClassName() {
        // Would need context tracking in a full implementation
        return "UnknownClass";
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

    private void addCaseClassBoilerplate(ClassDeclarationNode node) {
        // Additional case class specific methods could be added here
        indent();
        output.append("// Case class boilerplate methods generated above\n");
    }

    private void generateEqualsMethod() {
        indent();
        output.append("if (this == other) return true;\n");
        indent();
        output.append("if (other == null || getClass() != other.getClass()) return false;\n");
        indent();
        output.append("// TODO: Compare case class fields\n");
        indent();
        output.append("return true;\n");
    }

    private void generateHashCodeMethod() {
        indent();
        output.append("return Objects.hash(/* case class fields */);\n");
    }

    private void generateToStringMethod() {
        indent();
        output.append("return getClass().getSimpleName() + \"(\" + /* fields */ + \")\";\n");
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
            case "Optional": return "Optional.empty()";
            default: return null;
        }
    }

    // Placeholder implementations for other visitor methods
    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        return "// Struct not applicable for Scala";
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