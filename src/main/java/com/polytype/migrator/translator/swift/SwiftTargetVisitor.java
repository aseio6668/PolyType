package com.polytype.migrator.translator.swift;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.core.TargetVisitor;

public class SwiftTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inClass;
    private boolean inStruct;
    
    public SwiftTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inClass = false;
        this.inStruct = false;
    }
    
    @Override
    public String getResult() {
        return output.toString();
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }
    
    private void newLine() {
        output.append("\n");
    }
    
    private String mapToSwiftType(String originalType) {
        if (originalType == null || originalType.isEmpty()) {
            return "Any";
        }
        
        switch (originalType) {
            case "void": return "Void";
            case "boolean": case "Boolean": return "Bool";
            case "byte": case "short": case "int": case "Integer": return "Int";
            case "long": case "Long": return "Int64";
            case "float": case "Float": return "Float";
            case "double": case "Double": return "Double";
            case "char": case "Character": return "Character";
            case "String": return "String";
            case "Object": return "Any";
            case "List": case "ArrayList": return "[Any]";
            case "Map": case "HashMap": return "[String: Any]";
            case "Set": case "HashSet": return "Set<Any>";
            default:
                if (originalType.startsWith("List<") || originalType.startsWith("ArrayList<")) {
                    String genericType = extractGenericType(originalType);
                    return "[" + mapToSwiftType(genericType) + "]";
                }
                if (originalType.startsWith("Map<") || originalType.startsWith("HashMap<")) {
                    String[] types = extractGenericTypes(originalType);
                    if (types.length >= 2) {
                        return "[" + mapToSwiftType(types[0]) + ": " + mapToSwiftType(types[1]) + "]";
                    }
                    return "[String: Any]";
                }
                if (originalType.startsWith("Set<") || originalType.startsWith("HashSet<")) {
                    String genericType = extractGenericType(originalType);
                    return "Set<" + mapToSwiftType(genericType) + ">";
                }
                if (originalType.startsWith("Optional<")) {
                    String genericType = extractGenericType(originalType);
                    return mapToSwiftType(genericType) + "?";
                }
                return originalType;
        }
    }
    
    private String extractGenericType(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start > 0 && end > start) {
            return type.substring(start + 1, end).trim();
        }
        return "Any";
    }
    
    private String[] extractGenericTypes(String type) {
        String genericPart = extractGenericType(type);
        return genericPart.split(",\\s*");
    }
    
    private String formatVariableName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return camelCase(name);
    }
    
    private String formatTypeName(String name) {
        return pascalCase(name);
    }
    
    private String camelCase(String name) {
        if (name.length() <= 1) return name.toLowerCase();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    private String pascalCase(String name) {
        if (name.length() <= 1) return name.toUpperCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    @Override
    public void visit(ProgramNode node) {
        output.append("// Generated Swift code from PolyType migrator\n");
        output.append("import Foundation\n\n");
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            newLine();
        }
    }
    
    @Override
    public void visit(ClassDeclarationNode node) {
        boolean wasInClass = inClass;
        boolean wasInStruct = inStruct;
        
        if (node.isDataClass()) {
            inStruct = true;
            generateStruct(node);
        } else {
            inClass = true;
            generateClass(node);
        }
        
        inClass = wasInClass;
        inStruct = wasInStruct;
    }
    
    private void generateStruct(ClassDeclarationNode node) {
        indent();
        output.append("struct ").append(formatTypeName(node.getName())).append(" {");
        newLine();
        indentLevel++;
        
        // Generate properties
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateProperty(var);
            }
        }
        
        // Generate initializer
        generateInitializer(node);
        
        // Generate methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!func.isConstructor() && !func.getName().equals(node.getName())) {
                    generateMethod(func);
                }
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateClass(ClassDeclarationNode node) {
        indent();
        output.append("class ").append(formatTypeName(node.getName())).append(" {");
        newLine();
        indentLevel++;
        
        // Generate properties
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateProperty(var);
            }
        }
        
        // Generate initializer
        generateInitializer(node);
        
        // Generate methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!func.isConstructor() && !func.getName().equals(node.getName())) {
                    generateMethod(func);
                }
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateProperty(VariableDeclarationNode var) {
        newLine();
        indent();
        
        if (var.isFinal()) {
            output.append("let ");
        } else {
            output.append("var ");
        }
        
        output.append(formatVariableName(var.getName()))
              .append(": ").append(mapToSwiftType(var.getType()));
        
        if (var.getDefaultValue() != null) {
            output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
        }
        
        newLine();
    }
    
    private void generateInitializer(ClassDeclarationNode node) {
        newLine();
        indent();
        output.append("init(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(formatVariableName(var.getName()))
                      .append(": ").append(mapToSwiftType(var.getType()));
                if (var.getDefaultValue() != null) {
                    output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
                }
                first = false;
            }
        }
        
        output.append(") {");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                String varName = formatVariableName(var.getName());
                output.append("self.").append(varName).append(" = ").append(varName);
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateMethod(FunctionDeclarationNode func) {
        newLine();
        indent();
        
        if (func.isStatic()) {
            output.append("static ");
        }
        
        if (func.isAsync()) {
            output.append("func ").append(formatVariableName(func.getName())).append("(");
        } else {
            output.append("func ").append(formatVariableName(func.getName())).append("(");
        }
        
        boolean first = true;
        for (ParameterNode param : func.getParameters()) {
            if (!first) output.append(", ");
            
            // Use parameter name as external name for simplicity
            String paramName = formatVariableName(param.getName());
            output.append(paramName).append(": ").append(mapToSwiftType(param.getType()));
            
            first = false;
        }
        
        output.append(")");
        
        String returnType = mapToSwiftType(func.getReturnType());
        if (func.isAsync()) {
            if (!returnType.equals("Void")) {
                output.append(" async -> ").append(returnType);
            } else {
                output.append(" async");
            }
        } else {
            if (!returnType.equals("Void")) {
                output.append(" -> ").append(returnType);
            }
        }
        
        output.append(" {");
        newLine();
        indentLevel++;
        
        if (func.getBody() != null) {
            func.getBody().accept(this);
        } else {
            indent();
            if (func.isAsync()) {
                // For async functions, we need to await something or return a value
                if (!returnType.equals("Void")) {
                    output.append("return ").append(getDefaultValue(returnType));
                } else {
                    output.append("// TODO: Implement async method body");
                }
            } else {
                if (!returnType.equals("Void")) {
                    output.append("return ").append(getDefaultValue(returnType));
                } else {
                    output.append("// TODO: Implement method body");
                }
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (inClass || inStruct || node.isConstructor()) {
            return; // Handled in class/struct generation
        }
        
        indent();
        
        if (node.isAsync()) {
            output.append("func ").append(formatVariableName(node.getName())).append("(");
        } else {
            output.append("func ").append(formatVariableName(node.getName())).append("(");
        }
        
        boolean first = true;
        for (ParameterNode param : node.getParameters()) {
            if (!first) output.append(", ");
            
            String paramName = formatVariableName(param.getName());
            output.append(paramName).append(": ").append(mapToSwiftType(param.getType()));
            
            first = false;
        }
        
        output.append(")");
        
        String returnType = mapToSwiftType(node.getReturnType());
        if (node.isAsync()) {
            if (!returnType.equals("Void")) {
                output.append(" async -> ").append(returnType);
            } else {
                output.append(" async");
            }
        } else {
            if (!returnType.equals("Void")) {
                output.append(" -> ").append(returnType);
            }
        }
        
        output.append(" {");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        } else {
            indent();
            if (node.isAsync()) {
                if (!returnType.equals("Void")) {
                    output.append("return ").append(getDefaultValue(returnType));
                } else {
                    output.append("// TODO: Implement async function body");
                }
            } else {
                if (!returnType.equals("Void")) {
                    output.append("return ").append(getDefaultValue(returnType));
                } else {
                    output.append("// TODO: Implement function body");
                }
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private String getDefaultValue(String type) {
        switch (type) {
            case "Bool": return "false";
            case "Int": case "Int64": return "0";
            case "Float": case "Double": return "0.0";
            case "Character": return "\"\\0\"";
            case "String": return "\"\"";
            case "Void": return "";
            default:
                if (type.startsWith("[") && type.endsWith("]")) return "[]";
                if (type.startsWith("Set<")) return "Set()";
                if (type.contains("?")) return "nil";
                return "nil";
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "nil";
        
        String swiftType = mapToSwiftType(type);
        if (swiftType.equals("String")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        if (swiftType.equals("Character")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        if (swiftType.equals("Bool")) {
            return value.toString().toLowerCase();
        }
        if (swiftType.equals("Float")) {
            return value.toString() + "_f";
        }
        if (swiftType.equals("Double")) {
            return value.toString() + "_d";
        }
        
        return value.toString();
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inClass && !inStruct) {
            indent();
            if (node.isFinal()) {
                output.append("let ");
            } else {
                output.append("var ");
            }
            output.append(formatVariableName(node.getName()))
                  .append(": ").append(mapToSwiftType(node.getType()));
            
            if (node.getDefaultValue() != null) {
                output.append(" = ").append(formatValue(node.getDefaultValue(), node.getType()));
            }
            
            newLine();
        }
    }
    
    @Override
    public void visit(ParameterNode node) {
        // Parameters are handled in function declarations
    }
    
    @Override
    public void visit(BlockNode node) {
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
    }
    
    @Override
    public void visit(ReturnStatementNode node) {
        indent();
        output.append("return");
        if (node.getExpression() != null) {
            output.append(" ");
            node.getExpression().accept(this);
        }
        newLine();
    }
    
    @Override
    public void visit(ExpressionNode node) {
        if (node.getValue() != null) {
            output.append(node.getValue().toString());
        }
    }
    
    @Override
    public void visit(IfStatementNode node) {
        indent();
        output.append("if ");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append(" {");
        newLine();
        indentLevel++;
        
        if (node.getThenBlock() != null) {
            node.getThenBlock().accept(this);
        }
        
        indentLevel--;
        
        if (node.getElseBlock() != null) {
            indent();
            output.append("} else {");
            newLine();
            indentLevel++;
            node.getElseBlock().accept(this);
            indentLevel--;
        }
        
        indent();
        output.append("}");
        newLine();
    }
    
    @Override
    public void visit(ForLoopNode node) {
        indent();
        output.append("for ");
        if (node.getInitialization() != null) {
            // Swift for-in loops are different - this is a simplified version
            output.append("i in 0..<10");
        }
        output.append(" {");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    @Override
    public void visit(WhileLoopNode node) {
        indent();
        output.append("while ");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append(" {");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
}