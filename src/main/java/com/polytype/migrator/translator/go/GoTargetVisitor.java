package com.polytype.migrator.translator.go;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.translator.base.TargetVisitor;

public class GoTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inStruct;
    private String packageName;
    
    public GoTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inStruct = false;
        this.packageName = "main";
    }
    
    public GoTargetVisitor(String packageName) {
        this();
        this.packageName = packageName != null ? packageName : "main";
    }
    
    @Override
    public String getResult() {
        return output.toString();
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("\t");
        }
    }
    
    private void newLine() {
        output.append("\n");
    }
    
    private String mapToGoType(String originalType) {
        if (originalType == null || originalType.isEmpty()) {
            return "interface{}";
        }
        
        switch (originalType) {
            case "void": return "";
            case "boolean": case "Boolean": return "bool";
            case "byte": return "int8";
            case "short": return "int16";
            case "int": case "Integer": return "int";
            case "long": case "Long": return "int64";
            case "float": case "Float": return "float32";
            case "double": case "Double": return "float64";
            case "char": case "Character": return "rune";
            case "String": return "string";
            case "Object": return "interface{}";
            case "List": case "ArrayList": return "[]interface{}";
            case "Map": case "HashMap": return "map[string]interface{}";
            case "Set": case "HashSet": return "map[interface{}]bool";
            default:
                if (originalType.startsWith("List<") || originalType.startsWith("ArrayList<")) {
                    String genericType = extractGenericType(originalType);
                    return "[]" + mapToGoType(genericType);
                }
                if (originalType.startsWith("Map<") || originalType.startsWith("HashMap<")) {
                    String[] types = extractGenericTypes(originalType);
                    if (types.length >= 2) {
                        return "map[" + mapToGoType(types[0]) + "]" + mapToGoType(types[1]);
                    }
                    return "map[string]interface{}";
                }
                if (originalType.startsWith("Set<") || originalType.startsWith("HashSet<")) {
                    String genericType = extractGenericType(originalType);
                    return "map[" + mapToGoType(genericType) + "]bool";
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
        return "interface{}";
    }
    
    private String[] extractGenericTypes(String type) {
        String genericPart = extractGenericType(type);
        return genericPart.split(",\\s*");
    }
    
    private String formatVariableName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return capitalizeFirst(name);
    }
    
    private String formatFieldName(String name) {
        return capitalizeFirst(formatVariableName(name));
    }
    
    private String formatParameterName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return decapitalizeFirst(name);
    }
    
    private String capitalizeFirst(String name) {
        if (name.length() <= 1) return name.toUpperCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    private String decapitalizeFirst(String name) {
        if (name.length() <= 1) return name.toLowerCase();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    @Override
    public void visit(ProgramNode node) {
        output.append("// Generated Go code from PolyType migrator\n");
        output.append("package ").append(packageName).append("\n\n");
        
        output.append("import (\n");
        output.append("\t\"fmt\"\n");
        output.append("\t\"sync\"\n");
        output.append(")\n\n");
        
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
            newLine();
        }
    }
    
    @Override
    public void visit(ClassDeclarationNode node) {
        boolean wasInStruct = inStruct;
        inStruct = true;
        
        if (node.isDataClass()) {
            generateStruct(node);
            generateConstructorFunction(node);
        } else {
            generateStruct(node);
            generateMethods(node);
        }
        
        inStruct = wasInStruct;
    }
    
    private void generateStruct(ClassDeclarationNode node) {
        output.append("type ").append(node.getName()).append(" struct {");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                output.append(formatFieldName(var.getName()))
                      .append(" ")
                      .append(mapToGoType(var.getType()));
                
                if (var.getDefaultValue() != null) {
                    output.append(" // default: ").append(var.getDefaultValue());
                }
                
                newLine();
            }
        }
        
        indentLevel--;
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateConstructorFunction(ClassDeclarationNode node) {
        output.append("func New").append(node.getName()).append("(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(formatParameterName(var.getName()))
                      .append(" ")
                      .append(mapToGoType(var.getType()));
                first = false;
            }
        }
        
        output.append(") *").append(node.getName()).append(" {");
        newLine();
        indentLevel++;
        
        indent();
        output.append("return &").append(node.getName()).append("{");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                output.append(formatFieldName(var.getName()))
                      .append(": ")
                      .append(formatParameterName(var.getName()))
                      .append(",");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        
        indentLevel--;
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateMethods(ClassDeclarationNode node) {
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!func.isConstructor() && !func.getName().equals(node.getName())) {
                    generateMethod(node.getName(), func);
                }
            }
        }
        
        // Generate getters and setters for fields
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateGetter(node.getName(), var);
                if (!var.isFinal()) {
                    generateSetter(node.getName(), var);
                }
            }
        }
    }
    
    private void generateMethod(String structName, FunctionDeclarationNode func) {
        output.append("func (").append(decapitalizeFirst(structName.substring(0, 1)))
              .append(" *").append(structName).append(") ")
              .append(formatVariableName(func.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : func.getParameters()) {
            if (!first) output.append(", ");
            output.append(formatParameterName(param.getName()))
                  .append(" ")
                  .append(mapToGoType(param.getType()));
            first = false;
        }
        
        output.append(")");
        
        String returnType = mapToGoType(func.getReturnType());
        if (!returnType.isEmpty()) {
            if (func.isAsync()) {
                output.append(" <-chan ").append(returnType);
            } else {
                output.append(" ").append(returnType);
            }
        }
        
        output.append(" {");
        newLine();
        indentLevel++;
        
        if (func.isAsync()) {
            indent();
            output.append("ch := make(chan ").append(returnType).append(", 1)");
            newLine();
            indent();
            output.append("go func() {");
            newLine();
            indentLevel++;
            
            if (func.getBody() != null) {
                func.getBody().accept(this);
            } else {
                indent();
                if (!returnType.isEmpty()) {
                    output.append("ch <- ").append(getDefaultValue(returnType));
                } else {
                    output.append("// TODO: Implement method body");
                }
                newLine();
            }
            
            indentLevel--;
            indent();
            output.append("}()");
            newLine();
            indent();
            output.append("return ch");
            newLine();
        } else {
            if (func.getBody() != null) {
                func.getBody().accept(this);
            } else {
                indent();
                if (!returnType.isEmpty()) {
                    output.append("return ").append(getDefaultValue(returnType));
                } else {
                    output.append("// TODO: Implement method body");
                }
                newLine();
            }
        }
        
        indentLevel--;
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateGetter(String structName, VariableDeclarationNode var) {
        String methodName = "Get" + formatFieldName(var.getName());
        output.append("func (").append(decapitalizeFirst(structName.substring(0, 1)))
              .append(" *").append(structName).append(") ")
              .append(methodName).append("() ")
              .append(mapToGoType(var.getType())).append(" {");
        newLine();
        indentLevel++;
        indent();
        output.append("return ").append(decapitalizeFirst(structName.substring(0, 1)))
              .append(".").append(formatFieldName(var.getName()));
        newLine();
        indentLevel--;
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateSetter(String structName, VariableDeclarationNode var) {
        String methodName = "Set" + formatFieldName(var.getName());
        String paramName = formatParameterName(var.getName());
        output.append("func (").append(decapitalizeFirst(structName.substring(0, 1)))
              .append(" *").append(structName).append(") ")
              .append(methodName).append("(")
              .append(paramName).append(" ")
              .append(mapToGoType(var.getType())).append(") {");
        newLine();
        indentLevel++;
        indent();
        output.append(decapitalizeFirst(structName.substring(0, 1)))
              .append(".").append(formatFieldName(var.getName()))
              .append(" = ").append(paramName);
        newLine();
        indentLevel--;
        output.append("}");
        newLine();
        newLine();
    }
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (inStruct && (node.isConstructor() || node.getName().equals(node.getDeclaringClass()))) {
            return; // Skip constructors - handled separately in struct generation
        }
        
        if (!inStruct) {
            // Standalone function
            output.append("func ").append(formatVariableName(node.getName())).append("(");
            
            boolean first = true;
            for (ParameterNode param : node.getParameters()) {
                if (!first) output.append(", ");
                output.append(formatParameterName(param.getName()))
                      .append(" ")
                      .append(mapToGoType(param.getType()));
                first = false;
            }
            
            output.append(")");
            
            String returnType = mapToGoType(node.getReturnType());
            if (!returnType.isEmpty()) {
                if (node.isAsync()) {
                    output.append(" <-chan ").append(returnType);
                } else {
                    output.append(" ").append(returnType);
                }
            }
            
            output.append(" {");
            newLine();
            indentLevel++;
            
            if (node.isAsync()) {
                indent();
                output.append("ch := make(chan ").append(returnType).append(", 1)");
                newLine();
                indent();
                output.append("go func() {");
                newLine();
                indentLevel++;
            }
            
            if (node.getBody() != null) {
                node.getBody().accept(this);
            } else {
                indent();
                if (!returnType.isEmpty()) {
                    if (node.isAsync()) {
                        output.append("ch <- ").append(getDefaultValue(returnType));
                    } else {
                        output.append("return ").append(getDefaultValue(returnType));
                    }
                } else {
                    output.append("// TODO: Implement function body");
                }
                newLine();
            }
            
            if (node.isAsync()) {
                indentLevel--;
                indent();
                output.append("}()");
                newLine();
                indent();
                output.append("return ch");
                newLine();
            }
            
            indentLevel--;
            output.append("}");
            newLine();
        }
    }
    
    private String getDefaultValue(String type) {
        switch (type) {
            case "bool": return "false";
            case "int8": case "int16": case "int": case "int64": 
            case "float32": case "float64": return "0";
            case "string": return "\"\"";
            case "rune": return "'\\0'";
            default:
                if (type.startsWith("[]")) return "nil";
                if (type.startsWith("map[")) return "nil";
                if (type.equals("interface{}")) return "nil";
                return "nil";
        }
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inStruct) {
            indent();
            output.append("var ")
                  .append(formatParameterName(node.getName()))
                  .append(" ")
                  .append(mapToGoType(node.getType()));
            
            if (node.getDefaultValue() != null) {
                output.append(" = ").append(formatValue(node.getDefaultValue(), node.getType()));
            }
            
            newLine();
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "nil";
        String goType = mapToGoType(type);
        if (goType.equals("string")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        if (goType.equals("rune")) {
            return "'" + value.toString().replace("'", "\\'") + "'";
        }
        return value.toString();
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
            node.getInitialization().accept(this);
            output.append("; ");
        }
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
            output.append("; ");
        }
        if (node.getIncrement() != null) {
            node.getIncrement().accept(this);
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
        output.append("for ");
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