package com.polytype.migrator.translator.typescript;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.translator.base.TargetVisitor;

public class TypeScriptTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inInterface;
    private boolean inClass;
    
    public TypeScriptTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inInterface = false;
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
    
    private String mapToTypeScriptType(String originalType) {
        if (originalType == null || originalType.isEmpty()) {
            return "any";
        }
        
        switch (originalType) {
            case "void": return "void";
            case "boolean": case "Boolean": return "boolean";
            case "int": case "Integer": case "byte": case "short": case "long": case "Long": return "number";
            case "float": case "double": case "Float": case "Double": return "number";
            case "char": case "Character": case "String": return "string";
            case "Object": return "any";
            case "List": case "ArrayList": return "Array";
            case "Map": case "HashMap": return "Map";
            case "Set": case "HashSet": return "Set";
            default:
                if (originalType.startsWith("List<") || originalType.startsWith("ArrayList<")) {
                    String genericType = extractGenericType(originalType);
                    return mapToTypeScriptType(genericType) + "[]";
                }
                if (originalType.startsWith("Map<") || originalType.startsWith("HashMap<")) {
                    String[] types = extractGenericTypes(originalType);
                    if (types.length >= 2) {
                        return "Map<" + mapToTypeScriptType(types[0]) + ", " + mapToTypeScriptType(types[1]) + ">";
                    }
                    return "Map<string, any>";
                }
                if (originalType.startsWith("Set<") || originalType.startsWith("HashSet<")) {
                    String genericType = extractGenericType(originalType);
                    return "Set<" + mapToTypeScriptType(genericType) + ">";
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
        return "any";
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
    
    private String camelCase(String name) {
        if (name.length() <= 1) return name.toLowerCase();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    @Override
    public void visit(ProgramNode node) {
        output.append("// Generated TypeScript code from PolyType migrator\n\n");
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            newLine();
        }
    }
    
    @Override
    public void visit(ClassDeclarationNode node) {
        boolean wasInClass = inClass;
        inClass = true;
        
        if (node.isDataClass()) {
            generateInterface(node);
        } else {
            generateClass(node);
        }
        
        inClass = wasInClass;
    }
    
    private void generateInterface(ClassDeclarationNode node) {
        boolean wasInInterface = inInterface;
        inInterface = true;
        
        indent();
        output.append("interface ").append(node.getName()).append(" {");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                output.append(formatVariableName(var.getName()))
                      .append(var.isOptional() ? "?" : "")
                      .append(": ")
                      .append(mapToTypeScriptType(var.getType()))
                      .append(";");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        
        generateFactoryFunction(node);
        
        inInterface = wasInInterface;
    }
    
    private void generateFactoryFunction(ClassDeclarationNode node) {
        newLine();
        indent();
        output.append("function create").append(node.getName()).append("(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(formatVariableName(var.getName()))
                      .append(var.isOptional() ? "?" : "")
                      .append(": ")
                      .append(mapToTypeScriptType(var.getType()));
                first = false;
            }
        }
        
        output.append("): ").append(node.getName()).append(" {");
        newLine();
        indentLevel++;
        
        indent();
        output.append("return {");
        newLine();
        indentLevel++;
        
        first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(",").append("\n");
                indent();
                String varName = formatVariableName(var.getName());
                output.append(varName);
                first = false;
            }
        }
        
        newLine();
        indentLevel--;
        indent();
        output.append("};");
        newLine();
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateClass(ClassDeclarationNode node) {
        indent();
        output.append("class ").append(node.getName()).append(" {");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                output.append("private ")
                      .append(formatVariableName(var.getName()))
                      .append(": ")
                      .append(mapToTypeScriptType(var.getType()));
                if (var.getDefaultValue() != null) {
                    output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
                }
                output.append(";");
                newLine();
            }
        }
        
        generateConstructor(node);
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateGetter(var);
                generateSetter(var);
            } else if (child instanceof FunctionDeclarationNode) {
                child.accept(this);
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateConstructor(ClassDeclarationNode node) {
        newLine();
        indent();
        output.append("constructor(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(formatVariableName(var.getName()))
                      .append(var.isOptional() ? "?" : "")
                      .append(": ")
                      .append(mapToTypeScriptType(var.getType()));
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
                output.append("this.").append(varName).append(" = ").append(varName).append(";");
                newLine();
            }
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
        String methodName = "get" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
        output.append("public ").append(methodName).append("(): ")
              .append(mapToTypeScriptType(var.getType())).append(" {");
        newLine();
        indentLevel++;
        indent();
        output.append("return this.").append(varName).append(";");
        newLine();
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateSetter(VariableDeclarationNode var) {
        if (var.isFinal()) return;
        
        newLine();
        indent();
        String varName = formatVariableName(var.getName());
        String methodName = "set" + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
        output.append("public ").append(methodName).append("(")
              .append(varName).append(": ")
              .append(mapToTypeScriptType(var.getType()))
              .append("): void {");
        newLine();
        indentLevel++;
        indent();
        output.append("this.").append(varName).append(" = ").append(varName).append(";");
        newLine();
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (node.getName().equals(node.getDeclaringClass()) || node.isConstructor()) {
            return; // Skip constructors - handled separately
        }
        
        newLine();
        indent();
        
        if (inClass) {
            output.append("public ");
        }
        
        if (node.isAsync()) {
            output.append("async ");
        }
        
        output.append(formatVariableName(node.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : node.getParameters()) {
            if (!first) output.append(", ");
            output.append(formatVariableName(param.getName()))
                  .append(param.isOptional() ? "?" : "")
                  .append(": ")
                  .append(mapToTypeScriptType(param.getType()));
            first = false;
        }
        
        output.append("): ");
        
        if (node.isAsync()) {
            output.append("Promise<").append(mapToTypeScriptType(node.getReturnType())).append(">");
        } else {
            output.append(mapToTypeScriptType(node.getReturnType()));
        }
        
        output.append(" {");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        } else {
            indent();
            if (node.isAsync()) {
                output.append("return Promise.resolve(");
                if (!node.getReturnType().equals("void")) {
                    output.append(getDefaultValue(node.getReturnType()));
                } else {
                    output.append("undefined");
                }
                output.append(");");
            } else {
                if (!node.getReturnType().equals("void")) {
                    output.append("return ").append(getDefaultValue(node.getReturnType())).append(";");
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
    
    private String getDefaultValue(String type) {
        switch (mapToTypeScriptType(type)) {
            case "boolean": return "false";
            case "number": return "0";
            case "string": return "\"\"";
            case "void": return "undefined";
            default:
                if (type.endsWith("[]")) return "[]";
                if (type.startsWith("Map<")) return "new Map()";
                if (type.startsWith("Set<")) return "new Set()";
                return "null";
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "null";
        if (type.equals("string") || type.equals("String")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        return value.toString();
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inInterface && !inClass) {
            indent();
            if (node.isFinal()) {
                output.append("const ");
            } else {
                output.append("let ");
            }
            output.append(formatVariableName(node.getName()))
                  .append(": ")
                  .append(mapToTypeScriptType(node.getType()));
            
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