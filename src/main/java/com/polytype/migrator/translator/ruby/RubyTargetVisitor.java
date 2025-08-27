package com.polytype.migrator.translator.ruby;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.core.TargetVisitor;
import java.util.ArrayList;
import java.util.List;

public class RubyTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inClass;
    
    public RubyTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inClass = false;
    }
    
    @Override
    public String getResult() {
        return output.toString();
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("  ");
        }
    }
    
    private void newLine() {
        output.append("\n");
    }
    
    private String formatVariableName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return snakeCase(name);
    }
    
    private String formatClassName(String name) {
        return pascalCase(name);
    }
    
    private String formatMethodName(String name) {
        return snakeCase(name);
    }
    
    private String snakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private String pascalCase(String name) {
        if (name.length() <= 1) return name.toUpperCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    @Override
    public void visit(ProgramNode node) {
        output.append("# Generated Ruby code from PolyType migrator\n");
        output.append("# frozen_string_literal: true\n\n");
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            newLine();
        }
    }
    
    @Override
    public void visit(ClassDeclarationNode node) {
        boolean wasInClass = inClass;
        inClass = true;
        
        indent();
        output.append("class ").append(formatClassName(node.getName()));
        newLine();
        indentLevel++;
        
        // Generate attr_accessor for properties first
        generateAttrAccessors(node);
        
        // Generate initialize method (constructor)
        generateInitialize(node);
        
        // Generate methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!func.isConstructor() && !func.getName().equals("initialize") &&
                    !func.getName().equals(node.getName())) {
                    generateMethod(func);
                }
            }
        }
        
        indentLevel--;
        indent();
        output.append("end");
        newLine();
        
        inClass = wasInClass;
    }
    
    private void generateAttrAccessors(ClassDeclarationNode node) {
        List<String> attributes = new ArrayList<>();
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!var.isFinal()) {
                    attributes.add(":" + formatVariableName(var.getName()));
                } else {
                    // Use attr_reader for final/readonly properties
                    indent();
                    output.append("attr_reader :").append(formatVariableName(var.getName()));
                    newLine();
                }
            }
        }
        
        if (!attributes.isEmpty()) {
            indent();
            output.append("attr_accessor ");
            for (int i = 0; i < attributes.size(); i++) {
                if (i > 0) output.append(", ");
                output.append(attributes.get(i));
            }
            newLine();
            newLine();
        }
    }
    
    private void generateInitialize(ClassDeclarationNode node) {
        List<VariableDeclarationNode> properties = new ArrayList<>();
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                properties.add((VariableDeclarationNode) child);
            }
        }
        
        if (!properties.isEmpty()) {
            indent();
            output.append("def initialize(");
            
            boolean first = true;
            for (VariableDeclarationNode var : properties) {
                if (!first) output.append(", ");
                output.append(formatVariableName(var.getName()));
                if (var.getDefaultValue() != null) {
                    output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
                }
                first = false;
            }
            
            output.append(")");
            newLine();
            indentLevel++;
            
            for (VariableDeclarationNode var : properties) {
                indent();
                String varName = formatVariableName(var.getName());
                output.append("@").append(varName).append(" = ").append(varName);
                newLine();
            }
            
            indentLevel--;
            indent();
            output.append("end");
            newLine();
            newLine();
        }
    }
    
    private void generateMethod(FunctionDeclarationNode func) {
        indent();
        
        if (func.isStatic()) {
            output.append("def self.");
        } else {
            output.append("def ");
        }
        
        String methodName = formatMethodName(func.getName());
        
        // Handle special Ruby method naming conventions
        if (methodName.startsWith("is_") || methodName.startsWith("has_") ||
            methodName.startsWith("can_") || methodName.contains("valid")) {
            methodName += "?";
        }
        
        output.append(methodName);
        
        if (!func.getParameters().isEmpty()) {
            output.append("(");
            boolean first = true;
            for (ParameterNode param : func.getParameters()) {
                if (!first) output.append(", ");
                output.append(formatVariableName(param.getName()));
                first = false;
            }
            output.append(")");
        }
        
        newLine();
        indentLevel++;
        
        if (func.getBody() != null) {
            func.getBody().accept(this);
        } else {
            // Generate default implementation
            indent();
            String returnType = func.getReturnType();
            if (methodName.endsWith("?")) {
                output.append("false");
            } else if (returnType.equals("void")) {
                output.append("# TODO: Implement method body");
            } else {
                output.append(getDefaultValue(returnType));
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("end");
        newLine();
        newLine();
    }
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (inClass || node.isConstructor()) {
            return; // Handled in class generation
        }
        
        indent();
        output.append("def ").append(formatMethodName(node.getName()));
        
        if (!node.getParameters().isEmpty()) {
            output.append("(");
            boolean first = true;
            for (ParameterNode param : node.getParameters()) {
                if (!first) output.append(", ");
                output.append(formatVariableName(param.getName()));
                first = false;
            }
            output.append(")");
        }
        
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        } else {
            indent();
            String returnType = node.getReturnType();
            if (returnType.equals("void")) {
                output.append("# TODO: Implement function body");
            } else {
                output.append(getDefaultValue(returnType));
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("end");
        newLine();
    }
    
    private String getDefaultValue(String type) {
        switch (type) {
            case "Boolean": return "false";
            case "Integer": case "Long": case "Short": case "Byte": return "0";
            case "Float": case "Double": return "0.0";
            case "Character": case "String": return "\"\"";
            case "List": return "[]";
            case "Map": return "{}";
            case "Set": return "Set.new";
            case "void": return "nil";
            default: return "nil";
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "nil";
        
        String str = value.toString();
        
        // Handle different value types
        if (type.equals("String") || type.equals("Character")) {
            if (!str.startsWith("\"") && !str.startsWith("'")) {
                return "\"" + str.replace("\"", "\\\"") + "\"";
            }
            return str;
        }
        
        if (type.equals("Boolean")) {
            return str.toLowerCase();
        }
        
        if (type.equals("List") && str.startsWith("[")) {
            return str;
        }
        
        if (type.equals("Map") && str.startsWith("{")) {
            return str;
        }
        
        return str;
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inClass) {
            indent();
            output.append(formatVariableName(node.getName()));
            
            if (node.getDefaultValue() != null) {
                output.append(" = ").append(formatValue(node.getDefaultValue(), node.getType()));
            } else {
                output.append(" = ").append(getDefaultValue(node.getType()));
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
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
        } else {
            output.append("nil");
        }
        newLine();
    }
    
    @Override
    public void visit(ExpressionNode node) {
        if (node.getValue() != null) {
            String value = node.getValue().toString();
            // Convert variable references to Ruby instance variables if needed
            if (inClass && value.matches("\\w+") && !value.matches("\\d+") && 
                !value.equals("true") && !value.equals("false") && !value.equals("nil")) {
                output.append("@").append(formatVariableName(value));
            } else {
                output.append(value);
            }
        }
    }
    
    @Override
    public void visit(IfStatementNode node) {
        indent();
        output.append("if ");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        newLine();
        indentLevel++;
        
        if (node.getThenBlock() != null) {
            node.getThenBlock().accept(this);
        }
        
        indentLevel--;
        
        if (node.getElseBlock() != null) {
            indent();
            output.append("else");
            newLine();
            indentLevel++;
            node.getElseBlock().accept(this);
            indentLevel--;
        }
        
        indent();
        output.append("end");
        newLine();
    }
    
    @Override
    public void visit(ForLoopNode node) {
        indent();
        
        // Ruby uses different loop constructs - using times for simplicity
        output.append("10.times do |i|");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("end");
        newLine();
    }
    
    @Override
    public void visit(WhileLoopNode node) {
        indent();
        output.append("while ");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        indentLevel--;
        indent();
        output.append("end");
        newLine();
    }
}