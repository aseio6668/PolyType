package com.polytype.migrator.translator.javascript;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.core.TargetVisitor;

public class JavaScriptTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inClass;
    private boolean useES6Classes;
    private boolean useStrict;
    
    public JavaScriptTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inClass = false;
        this.useES6Classes = true;
        this.useStrict = true;
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
    
    private String mapToJavaScriptType(String originalType) {
        // JavaScript is dynamically typed, but we can use these for documentation/comments
        switch (originalType) {
            case "void": return "undefined";
            case "boolean": case "Boolean": return "boolean";
            case "int": case "Integer": case "byte": case "short": case "long": case "Long":
            case "float": case "double": case "Float": case "Double": return "number";
            case "char": case "Character": case "String": return "string";
            case "List": case "ArrayList": return "Array";
            case "Map": case "HashMap": return "Object";
            case "Set": case "HashSet": return "Set";
            default:
                if (originalType.startsWith("List<") || originalType.startsWith("ArrayList<")) {
                    return "Array";
                }
                if (originalType.startsWith("Map<") || originalType.startsWith("HashMap<")) {
                    return "Object";
                }
                if (originalType.startsWith("Set<") || originalType.startsWith("HashSet<")) {
                    return "Set";
                }
                return "object";
        }
    }
    
    private String formatVariableName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return camelCase(name);
    }
    
    private String camelCase(String name) {
        if (name.length() <= 1) return name.toLowerCase();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    @Override
    public void visit(ProgramNode node) {
        if (useStrict) {
            output.append("\"use strict\";\n\n");
        }
        
        output.append("// Generated JavaScript code from PolyType migrator\n\n");
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            newLine();
        }
    }
    
    @Override
    public void visit(ClassDeclarationNode node) {
        boolean wasInClass = inClass;
        inClass = true;
        
        if (useES6Classes) {
            generateES6Class(node);
        } else {
            generateES5Constructor(node);
        }
        
        inClass = wasInClass;
    }
    
    private void generateES6Class(ClassDeclarationNode node) {
        indent();
        output.append("class ").append(node.getName()).append(" {");
        newLine();
        indentLevel++;
        
        // Generate constructor
        generateES6Constructor(node);
        
        // Generate methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!func.isConstructor() && !func.getName().equals(node.getName())) {
                    generateES6Method(func);
                }
            }
        }
        
        // Generate getters and setters for properties
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateGetter(var);
                if (!var.isFinal()) {
                    generateSetter(var);
                }
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateES6Constructor(ClassDeclarationNode node) {
        newLine();
        indent();
        output.append("constructor(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(formatVariableName(var.getName()));
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
                output.append("this._").append(formatVariableName(var.getName()))
                      .append(" = ").append(formatVariableName(var.getName())).append(";");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateES6Method(FunctionDeclarationNode func) {
        newLine();
        indent();
        
        if (func.isStatic()) {
            output.append("static ");
        }
        
        if (func.isAsync()) {
            output.append("async ");
        }
        
        output.append(formatVariableName(func.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : func.getParameters()) {
            if (!first) output.append(", ");
            output.append(formatVariableName(param.getName()));
            first = false;
        }
        
        output.append(") {");
        newLine();
        indentLevel++;
        
        if (func.getBody() != null) {
            func.getBody().accept(this);
        } else {
            indent();
            String returnType = mapToJavaScriptType(func.getReturnType());
            if (func.isAsync()) {
                output.append("return Promise.resolve(").append(getDefaultValue(returnType)).append(");");
            } else if (!returnType.equals("undefined")) {
                output.append("return ").append(getDefaultValue(returnType)).append(";");
            } else {
                output.append("// TODO: Implement method body");
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateGetter(VariableDeclarationNode var) {
        newLine();
        indent();
        String varName = formatVariableName(var.getName());
        output.append("get ").append(varName).append("() {");
        newLine();
        indentLevel++;
        indent();
        output.append("return this._").append(varName).append(";");
        newLine();
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateSetter(VariableDeclarationNode var) {
        newLine();
        indent();
        String varName = formatVariableName(var.getName());
        output.append("set ").append(varName).append("(value) {");
        newLine();
        indentLevel++;
        indent();
        output.append("this._").append(varName).append(" = value;");
        newLine();
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateES5Constructor(ClassDeclarationNode node) {
        indent();
        output.append("function ").append(node.getName()).append("(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(formatVariableName(var.getName()));
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
                output.append("this.").append(formatVariableName(var.getName()))
                      .append(" = ").append(formatVariableName(var.getName()));
                if (var.getDefaultValue() != null) {
                    output.append(" || ").append(formatValue(var.getDefaultValue(), var.getType()));
                }
                output.append(";");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        
        // Generate prototype methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!func.isConstructor() && !func.getName().equals(node.getName())) {
                    generateES5Method(node.getName(), func);
                }
            }
        }
    }
    
    private void generateES5Method(String className, FunctionDeclarationNode func) {
        newLine();
        indent();
        output.append(className).append(".prototype.").append(formatVariableName(func.getName()))
              .append(" = ");
        
        if (func.isAsync()) {
            output.append("async ");
        }
        
        output.append("function(");
        
        boolean first = true;
        for (ParameterNode param : func.getParameters()) {
            if (!first) output.append(", ");
            output.append(formatVariableName(param.getName()));
            first = false;
        }
        
        output.append(") {");
        newLine();
        indentLevel++;
        
        if (func.getBody() != null) {
            func.getBody().accept(this);
        } else {
            indent();
            String returnType = mapToJavaScriptType(func.getReturnType());
            if (func.isAsync()) {
                output.append("return Promise.resolve(").append(getDefaultValue(returnType)).append(");");
            } else if (!returnType.equals("undefined")) {
                output.append("return ").append(getDefaultValue(returnType)).append(";");
            } else {
                output.append("// TODO: Implement method body");
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("};");
        newLine();
    }
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (inClass || node.isConstructor()) {
            return; // Handled in class generation
        }
        
        indent();
        
        if (node.isAsync()) {
            output.append("async ");
        }
        
        output.append("function ").append(formatVariableName(node.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : node.getParameters()) {
            if (!first) output.append(", ");
            output.append(formatVariableName(param.getName()));
            first = false;
        }
        
        output.append(") {");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        } else {
            indent();
            String returnType = mapToJavaScriptType(node.getReturnType());
            if (node.isAsync()) {
                output.append("return Promise.resolve(").append(getDefaultValue(returnType)).append(");");
            } else if (!returnType.equals("undefined")) {
                output.append("return ").append(getDefaultValue(returnType)).append(";");
            } else {
                output.append("// TODO: Implement function body");
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
            case "boolean": return "false";
            case "number": return "0";
            case "string": return "\"\"";
            case "Array": return "[]";
            case "Set": return "new Set()";
            case "undefined": return "undefined";
            default: return "null";
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "null";
        
        String jsType = mapToJavaScriptType(type);
        if (jsType.equals("string")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        if (jsType.equals("boolean")) {
            return value.toString().toLowerCase();
        }
        
        return value.toString();
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inClass) {
            indent();
            if (node.isFinal()) {
                output.append("const ");
            } else {
                output.append("let ");
            }
            output.append(formatVariableName(node.getName()));
            
            if (node.getDefaultValue() != null) {
                output.append(" = ").append(formatValue(node.getDefaultValue(), node.getType()));
            }
            
            output.append(";");
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
        output.append(";");
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
        output.append("if (");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append(") {");
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
        output.append("for (");
        if (node.getInitialization() != null) {
            output.append("let ");
            node.getInitialization().accept(this);
        }
        output.append("; ");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append("; ");
        if (node.getIncrement() != null) {
            node.getIncrement().accept(this);
        }
        output.append(") {");
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
        output.append("while (");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append(") {");
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