package com.polytype.migrator.translator.rust;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.core.TargetVisitor;

public class RustTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inStruct;
    private boolean inImpl;
    
    public RustTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inStruct = false;
        this.inImpl = false;
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
    
    private String mapToRustType(String originalType) {
        if (originalType == null || originalType.isEmpty()) {
            return "()";
        }
        
        switch (originalType) {
            case "void": return "()";
            case "boolean": case "Boolean": return "bool";
            case "byte": return "i8";
            case "short": return "i16";
            case "int": case "Integer": return "i32";
            case "long": case "Long": return "i64";
            case "float": case "Float": return "f32";
            case "double": case "Double": return "f64";
            case "char": case "Character": return "char";
            case "String": return "String";
            case "Object": return "Box<dyn std::any::Any>";
            case "List": case "ArrayList": return "Vec<i32>";
            case "Map": case "HashMap": return "std::collections::HashMap<String, i32>";
            case "Set": case "HashSet": return "std::collections::HashSet<i32>";
            default:
                if (originalType.startsWith("List<") || originalType.startsWith("ArrayList<")) {
                    String genericType = extractGenericType(originalType);
                    return "Vec<" + mapToRustType(genericType) + ">";
                }
                if (originalType.startsWith("Map<") || originalType.startsWith("HashMap<")) {
                    String[] types = extractGenericTypes(originalType);
                    if (types.length >= 2) {
                        return "std::collections::HashMap<" + mapToRustType(types[0]) + ", " + mapToRustType(types[1]) + ">";
                    }
                    return "std::collections::HashMap<String, i32>";
                }
                if (originalType.startsWith("Set<") || originalType.startsWith("HashSet<")) {
                    String genericType = extractGenericType(originalType);
                    return "std::collections::HashSet<" + mapToRustType(genericType) + ">";
                }
                if (originalType.startsWith("Optional<")) {
                    String genericType = extractGenericType(originalType);
                    return "Option<" + mapToRustType(genericType) + ">";
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
        return "i32";
    }
    
    private String[] extractGenericTypes(String type) {
        String genericPart = extractGenericType(type);
        return genericPart.split(",\\s*");
    }
    
    private String formatVariableName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return snakeCase(name);
    }
    
    private String formatStructName(String name) {
        return pascalCase(name);
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
        output.append("// Generated Rust code from PolyType migrator\n");
        output.append("use std::collections::{HashMap, HashSet};\n\n");
        
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
            generateStruct(node, true);
        } else {
            generateStruct(node, false);
            generateImpl(node);
        }
        
        inStruct = wasInStruct;
    }
    
    private void generateStruct(ClassDeclarationNode node, boolean isDataClass) {
        if (isDataClass) {
            output.append("#[derive(Debug, Clone, PartialEq)]\n");
        }
        
        indent();
        output.append("pub struct ").append(formatStructName(node.getName())).append(" {");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                output.append("pub ").append(formatVariableName(var.getName()))
                      .append(": ").append(mapToRustType(var.getType())).append(",");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateImpl(ClassDeclarationNode node) {
        boolean wasInImpl = inImpl;
        inImpl = true;
        
        indent();
        output.append("impl ").append(formatStructName(node.getName())).append(" {");
        newLine();
        indentLevel++;
        
        // Generate constructor (new function)
        generateConstructor(node);
        
        // Generate getters and setters
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateGetter(var);
                if (!var.isFinal()) {
                    generateSetter(var);
                }
            }
        }
        
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
        
        inImpl = wasInImpl;
    }
    
    private void generateConstructor(ClassDeclarationNode node) {
        indent();
        output.append("pub fn new(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                output.append(formatVariableName(var.getName()))
                      .append(": ").append(mapToRustType(var.getType()));
                first = false;
            }
        }
        
        output.append(") -> Self {");
        newLine();
        indentLevel++;
        
        indent();
        output.append("Self {");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                String fieldName = formatVariableName(var.getName());
                output.append(fieldName).append(",");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateGetter(VariableDeclarationNode var) {
        indent();
        String fieldName = formatVariableName(var.getName());
        String rustType = mapToRustType(var.getType());
        
        // Return reference for non-Copy types
        String returnType = shouldReturnReference(rustType) ? "&" + rustType : rustType;
        
        output.append("pub fn ").append(fieldName).append("(&self) -> ").append(returnType).append(" {");
        newLine();
        indentLevel++;
        indent();
        
        if (shouldReturnReference(rustType)) {
            output.append("&self.").append(fieldName);
        } else {
            output.append("self.").append(fieldName);
        }
        
        newLine();
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateSetter(VariableDeclarationNode var) {
        indent();
        String fieldName = formatVariableName(var.getName());
        String rustType = mapToRustType(var.getType());
        
        output.append("pub fn set_").append(fieldName).append("(&mut self, ")
              .append(fieldName).append(": ").append(rustType).append(") {");
        newLine();
        indentLevel++;
        indent();
        output.append("self.").append(fieldName).append(" = ").append(fieldName).append(";");
        newLine();
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    private void generateMethod(FunctionDeclarationNode func) {
        indent();
        output.append("pub ");
        
        if (func.isAsync()) {
            output.append("async ");
        }
        
        output.append("fn ").append(formatVariableName(func.getName())).append("(");
        
        // Add self parameter for instance methods
        if (!func.isStatic()) {
            output.append("&self");
            if (!func.getParameters().isEmpty()) {
                output.append(", ");
            }
        }
        
        boolean first = func.isStatic();
        for (ParameterNode param : func.getParameters()) {
            if (!first) output.append(", ");
            output.append(formatVariableName(param.getName()))
                  .append(": ").append(mapToRustType(param.getType()));
            first = false;
        }
        
        output.append(")");
        
        String returnType = mapToRustType(func.getReturnType());
        if (!returnType.equals("()")) {
            if (func.isAsync()) {
                output.append(" -> impl std::future::Future<Output = ").append(returnType).append(">");
            } else {
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
                output.append("async { ").append(getDefaultValue(returnType)).append(" }.await");
            } else {
                output.append(getDefaultValue(returnType));
            }
            newLine();
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
        newLine();
    }
    
    private boolean shouldReturnReference(String rustType) {
        // Return reference for non-Copy types
        switch (rustType) {
            case "bool":
            case "i8": case "i16": case "i32": case "i64":
            case "u8": case "u16": case "u32": case "u64":
            case "f32": case "f64":
            case "char":
            case "()":
                return false;
            default:
                return true;
        }
    }
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (inImpl || node.isConstructor()) {
            return; // Handled in impl blocks
        }
        
        indent();
        
        if (node.isAsync()) {
            output.append("pub async fn ");
        } else {
            output.append("pub fn ");
        }
        
        output.append(formatVariableName(node.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : node.getParameters()) {
            if (!first) output.append(", ");
            output.append(formatVariableName(param.getName()))
                  .append(": ").append(mapToRustType(param.getType()));
            first = false;
        }
        
        output.append(")");
        
        String returnType = mapToRustType(node.getReturnType());
        if (!returnType.equals("()")) {
            if (node.isAsync()) {
                output.append(" -> impl std::future::Future<Output = ").append(returnType).append(">");
            } else {
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
                output.append("async { ").append(getDefaultValue(returnType)).append(" }.await");
            } else {
                output.append(getDefaultValue(returnType));
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
            case "bool": return "false";
            case "i8": case "i16": case "i32": case "i64":
            case "u8": case "u16": case "u32": case "u64":
            case "f32": case "f64": return "0";
            case "char": return "'\\0'";
            case "String": return "String::new()";
            case "()": return "()";
            default:
                if (type.startsWith("Vec<")) return "Vec::new()";
                if (type.startsWith("std::collections::HashMap<")) return "HashMap::new()";
                if (type.startsWith("std::collections::HashSet<")) return "HashSet::new()";
                if (type.startsWith("Option<")) return "None";
                if (type.startsWith("Box<")) return "Box::new(())";
                return "Default::default()";
        }
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inStruct && !inImpl) {
            indent();
            if (node.isFinal()) {
                output.append("let ");
            } else {
                output.append("let mut ");
            }
            output.append(formatVariableName(node.getName()))
                  .append(": ").append(mapToRustType(node.getType()));
            
            if (node.getDefaultValue() != null) {
                output.append(" = ").append(formatValue(node.getDefaultValue(), node.getType()));
            } else {
                output.append(" = ").append(getDefaultValue(mapToRustType(node.getType())));
            }
            
            output.append(";");
            newLine();
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "None";
        
        String rustType = mapToRustType(type);
        if (rustType.equals("String")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\".to_string()";
        }
        if (rustType.equals("char")) {
            return "'" + value.toString().replace("'", "\\'") + "'";
        }
        if (rustType.equals("bool")) {
            return value.toString().toLowerCase();
        }
        if (rustType.contains("f32")) {
            return value.toString() + "_f32";
        }
        if (rustType.contains("f64")) {
            return value.toString() + "_f64";
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
        if (node.getExpression() != null) {
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
            // Rust for loops are different - this is a simplified version
            output.append("i in 0..10");
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