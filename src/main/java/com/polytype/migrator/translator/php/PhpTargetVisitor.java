package com.polytype.migrator.translator.php;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.core.TargetVisitor;

public class PhpTargetVisitor implements TargetVisitor {
    private StringBuilder output;
    private int indentLevel;
    private boolean inClass;
    private boolean useStrictTypes;
    private String namespaceName;
    
    public PhpTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.inClass = false;
        this.useStrictTypes = true;
        this.namespaceName = null;
    }
    
    public PhpTargetVisitor(String namespaceName) {
        this();
        this.namespaceName = namespaceName;
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
    
    private String mapToPhpType(String originalType) {
        if (originalType == null || originalType.isEmpty()) {
            return "mixed";
        }
        
        switch (originalType) {
            case "void": return "void";
            case "boolean": case "Boolean": return "bool";
            case "byte": case "short": case "int": case "Integer": return "int";
            case "long": case "Long": return "int";
            case "float": case "Float": case "double": case "Double": return "float";
            case "char": case "Character": case "String": return "string";
            case "Object": return "object";
            case "List": case "ArrayList": return "array";
            case "Map": case "HashMap": return "array";
            case "Set": case "HashSet": return "array";
            default:
                if (originalType.startsWith("List<") || originalType.startsWith("ArrayList<")) {
                    return "array";
                }
                if (originalType.startsWith("Map<") || originalType.startsWith("HashMap<")) {
                    return "array";
                }
                if (originalType.startsWith("Set<") || originalType.startsWith("HashSet<")) {
                    return "array";
                }
                if (originalType.startsWith("Optional<")) {
                    String innerType = extractGenericType(originalType);
                    return "?" + mapToPhpType(innerType);
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
        return "mixed";
    }
    
    private String formatVariableName(String name) {
        if (name.startsWith("m_") || name.startsWith("_")) {
            name = name.substring(name.startsWith("m_") ? 2 : 1);
        }
        return camelCase(name);
    }
    
    private String formatClassName(String name) {
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
        output.append("<?php");
        newLine();
        
        if (useStrictTypes) {
            output.append("declare(strict_types=1);");
            newLine();
        }
        
        newLine();
        output.append("// Generated PHP code from PolyType migrator");
        newLine();
        
        if (namespaceName != null) {
            output.append("namespace ").append(namespaceName).append(";");
            newLine();
            newLine();
        }
        
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
            generateReadonlyClass(node);
        } else {
            generateClass(node);
        }
        
        inClass = wasInClass;
    }
    
    private void generateReadonlyClass(ClassDeclarationNode node) {
        indent();
        output.append("readonly class ").append(formatClassName(node.getName()));
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        // Generate constructor with promoted properties
        generatePromotedConstructor(node);
        
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
        output.append("class ").append(formatClassName(node.getName()));
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        // Generate properties
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateProperty(var);
            }
        }
        
        // Generate constructor
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
    }
    
    private void generateProperty(VariableDeclarationNode var) {
        newLine();
        indent();
        output.append("private ");
        
        if (var.isFinal()) {
            output.append("readonly ");
        }
        
        String phpType = mapToPhpType(var.getType());
        if (!phpType.equals("mixed")) {
            output.append(phpType).append(" ");
        }
        
        output.append("$").append(formatVariableName(var.getName()));
        
        if (var.getDefaultValue() != null && !var.isFinal()) {
            output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
        }
        
        output.append(";");
        newLine();
    }
    
    private void generateConstructor(ClassDeclarationNode node) {
        newLine();
        indent();
        output.append("public function __construct(");
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(", ");
                
                String phpType = mapToPhpType(var.getType());
                if (!phpType.equals("mixed")) {
                    output.append(phpType).append(" ");
                }
                output.append("$").append(formatVariableName(var.getName()));
                
                if (var.getDefaultValue() != null) {
                    output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
                }
                first = false;
            }
        }
        
        output.append(")");
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                indent();
                String varName = formatVariableName(var.getName());
                output.append("$this->").append(varName).append(" = $").append(varName).append(";");
                newLine();
            }
        }
        
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generatePromotedConstructor(ClassDeclarationNode node) {
        newLine();
        indent();
        output.append("public function __construct(");
        newLine();
        indentLevel++;
        
        boolean first = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (!first) output.append(",").append("\n");
                
                indent();
                output.append("public readonly ");
                
                String phpType = mapToPhpType(var.getType());
                if (!phpType.equals("mixed")) {
                    output.append(phpType).append(" ");
                }
                output.append("$").append(formatVariableName(var.getName()));
                
                if (var.getDefaultValue() != null) {
                    output.append(" = ").append(formatValue(var.getDefaultValue(), var.getType()));
                }
                first = false;
            }
        }
        
        newLine();
        indentLevel--;
        indent();
        output.append(") {}");
        newLine();
    }
    
    private void generateGetter(VariableDeclarationNode var) {
        newLine();
        indent();
        String varName = formatVariableName(var.getName());
        String methodName = "get" + pascalCase(varName);
        String phpType = mapToPhpType(var.getType());
        
        output.append("public function ").append(methodName).append("()");
        if (!phpType.equals("mixed")) {
            output.append(": ").append(phpType);
        }
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        indent();
        output.append("return $this->").append(varName).append(";");
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
        String methodName = "set" + pascalCase(varName);
        String phpType = mapToPhpType(var.getType());
        
        output.append("public function ").append(methodName).append("(");
        if (!phpType.equals("mixed")) {
            output.append(phpType).append(" ");
        }
        output.append("$").append(varName).append("): void");
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        indent();
        output.append("$this->").append(varName).append(" = $").append(varName).append(";");
        newLine();
        indentLevel--;
        indent();
        output.append("}");
        newLine();
    }
    
    private void generateMethod(FunctionDeclarationNode func) {
        newLine();
        indent();
        
        if (func.isStatic()) {
            output.append("public static function ");
        } else {
            output.append("public function ");
        }
        
        output.append(formatVariableName(func.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : func.getParameters()) {
            if (!first) output.append(", ");
            
            String phpType = mapToPhpType(param.getType());
            if (!phpType.equals("mixed")) {
                output.append(phpType).append(" ");
            }
            output.append("$").append(formatVariableName(param.getName()));
            first = false;
        }
        
        output.append(")");
        
        String returnType = mapToPhpType(func.getReturnType());
        if (!returnType.equals("mixed")) {
            output.append(": ").append(returnType);
        }
        
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        if (func.getBody() != null) {
            func.getBody().accept(this);
        } else {
            indent();
            if (!returnType.equals("void") && !returnType.equals("mixed")) {
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
    
    @Override
    public void visit(FunctionDeclarationNode node) {
        if (inClass || node.isConstructor()) {
            return; // Handled in class generation
        }
        
        indent();
        output.append("function ").append(formatVariableName(node.getName())).append("(");
        
        boolean first = true;
        for (ParameterNode param : node.getParameters()) {
            if (!first) output.append(", ");
            
            String phpType = mapToPhpType(param.getType());
            if (!phpType.equals("mixed")) {
                output.append(phpType).append(" ");
            }
            output.append("$").append(formatVariableName(param.getName()));
            first = false;
        }
        
        output.append(")");
        
        String returnType = mapToPhpType(node.getReturnType());
        if (!returnType.equals("mixed")) {
            output.append(": ").append(returnType);
        }
        
        newLine();
        indent();
        output.append("{");
        newLine();
        indentLevel++;
        
        if (node.getBody() != null) {
            node.getBody().accept(this);
        } else {
            indent();
            if (!returnType.equals("void") && !returnType.equals("mixed")) {
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
            case "bool": return "false";
            case "int": case "float": return "0";
            case "string": return "\"\"";
            case "array": return "[]";
            case "object": return "new stdClass()";
            case "void": return "";
            default: return "null";
        }
    }
    
    private String formatValue(Object value, String type) {
        if (value == null) return "null";
        
        String phpType = mapToPhpType(type);
        if (phpType.equals("string")) {
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        if (phpType.equals("bool")) {
            return value.toString().toLowerCase();
        }
        if (phpType.equals("array") && value.toString().startsWith("[")) {
            return value.toString().replace("[", "[").replace("]", "]");
        }
        
        return value.toString();
    }
    
    @Override
    public void visit(VariableDeclarationNode node) {
        if (!inClass) {
            indent();
            output.append("$").append(formatVariableName(node.getName()));
            
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
            String value = node.getValue().toString();
            // Convert variable references to PHP syntax
            if (value.matches("\\w+") && !value.matches("\\d+") && 
                !value.equals("true") && !value.equals("false") && !value.equals("null")) {
                output.append("$").append(value);
            } else {
                output.append(value);
            }
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
            output.append("$i = 0");
        }
        output.append("; ");
        if (node.getCondition() != null) {
            node.getCondition().accept(this);
        }
        output.append("; ");
        if (node.getIncrement() != null) {
            output.append("$i++");
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