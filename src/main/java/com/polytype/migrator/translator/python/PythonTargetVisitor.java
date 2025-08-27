package com.polytype.migrator.translator.python;

import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.core.TargetVisitor;
import com.polytype.migrator.core.TranslationOptions;
import com.polytype.migrator.core.ast.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Enhanced Python target visitor for generating idiomatic Python code from AST nodes.
 * Supports modern Python 3.8+ with type hints, dataclasses, async/await,
 * context managers, decorators, and Pythonic best practices.
 */
public class PythonTargetVisitor implements TargetVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;
    private final Set<String> requiredImports;
    private boolean inClassContext;
    private boolean inAsyncContext;
    
    public PythonTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.requiredImports = new HashSet<>();
        this.inClassContext = false;
        this.inAsyncContext = false;
    }
    
    @Override
    public void setOptions(TranslationOptions options) {
        this.options = options;
        this.output.setLength(0);
        this.indentLevel = 0;
    }
    
    @Override
    public TargetLanguage getTargetLanguage() {
        return TargetLanguage.PYTHON;
    }
    
    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions defaultOptions = TranslationOptions.defaultOptions();
        defaultOptions.setOption("python.useTypeHints", true);
        defaultOptions.setOption("python.useDataclasses", true);
        defaultOptions.setOption("python.useAsyncAwait", true);
        defaultOptions.setOption("python.pythonVersion", "3.8");
        defaultOptions.setOption("python.useDocstrings", true);
        return defaultOptions;
    }
    
    @Override
    public String generateImports() {
        StringBuilder imports = new StringBuilder();
        if (options.getBooleanOption("python.useTypeHints", true)) {
            imports.append("from typing import List, Dict, Optional, Any, Union, Callable\n");
        }
        if (options.getBooleanOption("python.useDataclasses", true)) {
            imports.append("from dataclasses import dataclass\n");
        }
        if (options.getBooleanOption("python.useAsyncAwait", true)) {
            imports.append("import asyncio\n");
        }
        imports.append("from abc import ABC, abstractmethod\n");
        imports.append("import json\n");
        imports.append("import datetime\n");
        imports.append("from pathlib import Path\n");
        return imports.toString();
    }
    
    @Override
    public String generateFileHeader() {
        StringBuilder header = new StringBuilder();
        header.append("#!/usr/bin/env python3\n");
        header.append('\"').append('\"').append('\"').append("\n");
        header.append("Generated Python code from PolyType Code Migrator\n");
        header.append("Target: Python ").append(options.getStringOption("python.pythonVersion", "3.8")).append("\n");
        header.append("Generated on: ").append(java.time.LocalDateTime.now()).append("\n");
        header.append('\"').append('\"').append('\"').append("\n\n");
        
        header.append(generateImports()).append("\n");
        
        return header.toString();
    }
    
    @Override
    public String generateFileFooter() {
        StringBuilder footer = new StringBuilder();
        footer.append("\n\nif __name__ == \"__main__\":\n");
        footer.append("    # Main execution block\n");
        footer.append("    pass\n");
        return footer.toString();
    }
    
    @Override
    public String visitProgram(ProgramNode node) {
        StringBuilder result = new StringBuilder();
        result.append(generateFileHeader());
        
        for (ASTNode child : node.getChildren()) {
            result.append(child.accept(this));
            result.append("\n");
        }
        
        result.append(generateFileFooter());
        return result.toString();
    }
    
    @Override
    public String visitClassDeclaration(ClassDeclarationNode node) {
        StringBuilder classCode = new StringBuilder();
        
        // Check if this should be a dataclass
        boolean isDataClass = shouldBeDataclass(node) && 
                              options.getBooleanOption("python.useDataclasses", true);
        
        if (isDataClass) {
            classCode.append("@dataclass\n");
        }
        
        classCode.append("class ").append(node.getName());
        
        // Add base classes
        boolean hasBaseClass = hasInheritance(node);
        if (hasBaseClass) {
            classCode.append("(ABC)"); // Abstract base class for interfaces
        }
        
        classCode.append(":\n");
        
        indentLevel++;
        
        // Add docstring
        if (options.getBooleanOption("python.useDocstrings", true)) {
            indent();
            classCode.append('\"').append('\"').append('\"').append("\n");
            indent();
            classCode.append("Generated class from source code.\n");
            indent();
            classCode.append('\"').append('\"').append('\"').append("\n\n");
        }
        
        // Generate class content
        boolean hasContent = false;
        
        // Generate fields for dataclass or regular class
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (isDataClass) {
                    generateDataclassField(var, classCode);
                } else {
                    generateInstanceVariable(var, classCode);
                }
                hasContent = true;
            }
        }
        
        // Generate __init__ if not a dataclass
        if (!isDataClass && hasInstanceVariables(node)) {
            generateInitMethod(node, classCode);
            hasContent = true;
        }
        
        // Generate methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                generateMethod(func, classCode);
                hasContent = true;
            }
        }
        
        // Generate property methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode && !isDataClass) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generatePropertyMethods(var, classCode);
                hasContent = true;
            }
        }
        
        if (!hasContent) {
            indent();
            classCode.append("pass\n");
        }
        
        indentLevel--;
        
        return classCode.toString();
    }
    
    @Override
    public String visitFunctionDeclaration(FunctionDeclarationNode node) {
        StringBuilder funcCode = new StringBuilder();
        
        // Determine if this should be async
        boolean isAsync = shouldBeAsync(node) && 
                         options.getBooleanOption("python.useAsyncAwait", true);
        
        if (isAsync) {
            funcCode.append("async ");
        }
        
        funcCode.append("def ").append(pythonize_name(node.getName())).append("(");
        
        // Parameters with type hints
        if (options.getBooleanOption("python.useTypeHints", true)) {
            for (int i = 0; i < node.getParameters().size(); i++) {
                if (i > 0) funcCode.append(", ");
                ParameterNode param = node.getParameters().get(i);
                funcCode.append(pythonize_name(param.getName()));
                funcCode.append(": ").append(mapToPythonType(param.getDataType()));
            }
        } else {
            for (int i = 0; i < node.getParameters().size(); i++) {
                if (i > 0) funcCode.append(", ");
                funcCode.append(pythonize_name(node.getParameters().get(i).getName()));
            }
        }
        
        funcCode.append(")");
        
        // Return type annotation
        if (options.getBooleanOption("python.useTypeHints", true)) {
            String returnType = mapToPythonType(node.getReturnType());
            if (isAsync && !returnType.startsWith("Awaitable")) {
                returnType = "Awaitable[" + returnType + "]";
            }
            funcCode.append(" -> ").append(returnType);
        }
        
        funcCode.append(":\n");
        
        // Function body
        indentLevel++;
        
        if (options.getBooleanOption("python.useDocstrings", true)) {
            indent();
            funcCode.append('\"').append('\"').append('\"').append("\n");
            indent();
            funcCode.append("Generated function from source code.\n");
            indent();
            funcCode.append('\"').append('\"').append('\"').append("\n");
        }
        
        generateFunctionBody(node, funcCode, isAsync);
        
        indentLevel--;
        
        return funcCode.toString();
    }
    
    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        StringBuilder varCode = new StringBuilder();
        indent();
        
        String pythonName = pythonize_name(node.getName());
        String pythonType = mapToPythonType(node.getDataType());
        
        if (options.getBooleanOption("python.useTypeHints", true)) {
            varCode.append(pythonName).append(": ").append(pythonType);
        } else {
            varCode.append(pythonName);
        }
        
        // Add default value
        String defaultValue = getPythonDefaultValue(pythonType);
        if (defaultValue != null) {
            varCode.append(" = ").append(defaultValue);
        }
        
        varCode.append("\n");
        
        return varCode.toString();
    }
    
    @Override
    public String visitParameter(ParameterNode node) {
        // Parameters are handled in function declaration
        return "";
    }
    
    private String mapToPythonType(String javaType) {
        if (javaType == null) return "Any";
        
        switch (javaType) {
            case "boolean": return "bool";
            case "byte": case "short": case "int": return "int";
            case "long": return "int";  // Python 3 int handles big integers
            case "float": case "double": return "float";
            case "char": case "String": return "str";
            case "void": return "None";
            
            // Collections
            case "List": return "List[Any]";
            case "Map": return "Dict[str, Any]";
            case "Set": return "Set[Any]";
            case "Queue": return "List[Any]";  // Use list as queue
            case "Stack": return "List[Any]";  // Use list as stack
            
            // Specific generic types
            default:
                if (javaType.startsWith("List<")) {
                    String elementType = extractGenericType(javaType);
                    return "List[" + mapToPythonType(elementType) + "]";
                } else if (javaType.startsWith("Map<")) {
                    String[] types = extractGenericTypes(javaType, 2);
                    return "Dict[" + mapToPythonType(types[0]) + ", " + mapToPythonType(types[1]) + "]";
                } else if (javaType.startsWith("Set<")) {
                    String elementType = extractGenericType(javaType);
                    return "Set[" + mapToPythonType(elementType) + "]";
                } else if (javaType.startsWith("Optional<")) {
                    String elementType = extractGenericType(javaType);
                    return "Optional[" + mapToPythonType(elementType) + "]";
                } else if (javaType.startsWith("CompletableFuture<")) {
                    String elementType = extractGenericType(javaType);
                    return "Awaitable[" + mapToPythonType(elementType) + "]";
                }
                
                // Custom types
                return javaType;
        }
    }
    
    private String extractGenericType(String genericType) {
        int start = genericType.indexOf('<') + 1;
        int end = genericType.lastIndexOf('>');
        return genericType.substring(start, end).trim();
    }
    
    private String[] extractGenericTypes(String genericType, int count) {
        String inner = extractGenericType(genericType);
        return inner.split(",", count);
    }
    
    private String pythonize_name(String javaName) {
        // Convert camelCase to snake_case
        return javaName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private boolean shouldBeDataclass(ClassDeclarationNode node) {
        // Check if class has only fields and simple methods
        boolean hasOnlySimpleMembers = true;
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                if (!isSimpleAccessor(func)) {
                    hasOnlySimpleMembers = false;
                    break;
                }
            }
        }
        return hasOnlySimpleMembers && hasInstanceVariables(node);
    }
    
    private boolean hasInheritance(ClassDeclarationNode node) {
        // Check if class implements interfaces or extends classes
        // This would need to be tracked in the AST
        return false; // Simplified
    }
    
    private boolean hasInstanceVariables(ClassDeclarationNode node) {
        return node.getChildren().stream()
                .anyMatch(child -> child instanceof VariableDeclarationNode);
    }
    
    private boolean shouldBeAsync(FunctionDeclarationNode node) {
        // Check if function involves async operations
        String returnType = node.getReturnType();
        return returnType != null && 
               (returnType.startsWith("CompletableFuture") || returnType.startsWith("Future"));
    }
    
    private boolean isSimpleAccessor(FunctionDeclarationNode node) {
        String name = node.getName();
        return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
    }
    
    private void generateDataclassField(VariableDeclarationNode var, StringBuilder classCode) {
        indent();
        String pythonName = pythonize_name(var.getName());
        String pythonType = mapToPythonType(var.getDataType());
        
        classCode.append(pythonName).append(": ").append(pythonType);
        
        // Add default value if needed
        String defaultValue = getPythonDefaultValue(pythonType);
        if (defaultValue != null && !var.isMutable()) {
            classCode.append(" = ").append(defaultValue);
        }
        
        classCode.append("\n");
    }
    
    private void generateInstanceVariable(VariableDeclarationNode var, StringBuilder classCode) {
        // Instance variables will be initialized in __init__
        // This is handled in generateInitMethod
    }
    
    private void generateInitMethod(ClassDeclarationNode node, StringBuilder classCode) {
        indent();
        classCode.append("def __init__(self");
        
        // Add parameters for all instance variables
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                String pythonName = pythonize_name(var.getName());
                String pythonType = mapToPythonType(var.getDataType());
                
                classCode.append(", ").append(pythonName);
                if (options.getBooleanOption("python.useTypeHints", true)) {
                    classCode.append(": ").append(pythonType);
                }
            }
        }
        
        classCode.append("):\n");
        
        indentLevel++;
        
        if (options.getBooleanOption("python.useDocstrings", true)) {
            indent();
            classCode.append('\"').append('\"').append('\"').append("Initialize the instance.\"\"\"\n");
        }
        
        // Initialize instance variables
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                String pythonName = pythonize_name(var.getName());
                indent();
                classCode.append("self.").append(pythonName).append(" = ").append(pythonName).append("\n");
            }
        }
        
        indentLevel--;
        classCode.append("\n");
    }
    
    private void generateMethod(FunctionDeclarationNode func, StringBuilder classCode) {
        indent();
        
        boolean isAsync = shouldBeAsync(func);
        if (isAsync) {
            classCode.append("async ");
        }
        
        classCode.append("def ").append(pythonize_name(func.getName())).append("(self");
        
        // Parameters
        for (ParameterNode param : func.getParameters()) {
            classCode.append(", ").append(pythonize_name(param.getName()));
            if (options.getBooleanOption("python.useTypeHints", true)) {
                classCode.append(": ").append(mapToPythonType(param.getDataType()));
            }
        }
        
        classCode.append(")");
        
        // Return type
        if (options.getBooleanOption("python.useTypeHints", true)) {
            String returnType = mapToPythonType(func.getReturnType());
            if (isAsync && !returnType.startsWith("Awaitable")) {
                returnType = "Awaitable[" + returnType + "]";
            }
            classCode.append(" -> ").append(returnType);
        }
        
        classCode.append(":\n");
        
        indentLevel++;
        
        if (options.getBooleanOption("python.useDocstrings", true)) {
            indent();
            classCode.append('\"').append('\"').append('\"').append("Generated method.\"\"\"\n");
        }
        
        generateMethodBody(func, classCode, isAsync);
        
        indentLevel--;
        classCode.append("\n");
    }
    
    private void generatePropertyMethods(VariableDeclarationNode var, StringBuilder classCode) {
        String pythonName = pythonize_name(var.getName());
        String pythonType = mapToPythonType(var.getDataType());
        
        // Getter property
        indent();
        classCode.append("@property\n");
        indent();
        classCode.append("def ").append(pythonName).append("(self)");
        if (options.getBooleanOption("python.useTypeHints", true)) {
            classCode.append(" -> ").append(pythonType);
        }
        classCode.append(":\n");
        
        indentLevel++;
        indent();
        classCode.append("return self._").append(pythonName).append("\n");
        indentLevel--;
        classCode.append("\n");
        
        // Setter property (if mutable)
        if (var.isMutable()) {
            indent();
            classCode.append("@").append(pythonName).append(".setter\n");
            indent();
            classCode.append("def ").append(pythonName).append("(self, value");
            if (options.getBooleanOption("python.useTypeHints", true)) {
                classCode.append(": ").append(pythonType);
            }
            classCode.append(") -> None:\n");
            
            indentLevel++;
            indent();
            classCode.append("self._").append(pythonName).append(" = value\n");
            indentLevel--;
            classCode.append("\n");
        }
    }
    
    private void generateFunctionBody(FunctionDeclarationNode node, StringBuilder funcCode, boolean isAsync) {
        indent();
        funcCode.append("# TODO: Implement function body\n");
        
        String returnType = mapToPythonType(node.getReturnType());
        if (!"None".equals(returnType)) {
            indent();
            if (isAsync) {
                funcCode.append("return ");
            } else {
                funcCode.append("return ");
            }
            
            String defaultValue = getPythonDefaultValue(returnType);
            if (defaultValue != null) {
                funcCode.append(defaultValue);
            } else {
                funcCode.append("None");
            }
            funcCode.append("\n");
        } else {
            indent();
            funcCode.append("pass\n");
        }
    }
    
    private void generateMethodBody(FunctionDeclarationNode node, StringBuilder classCode, boolean isAsync) {
        indent();
        classCode.append("# TODO: Implement method body\n");
        
        String returnType = mapToPythonType(node.getReturnType());
        if (!"None".equals(returnType)) {
            indent();
            classCode.append("return ");
            
            String defaultValue = getPythonDefaultValue(returnType);
            if (defaultValue != null) {
                classCode.append(defaultValue);
            } else {
                classCode.append("None");
            }
            classCode.append("\n");
        } else {
            indent();
            classCode.append("pass\n");
        }
    }
    
    private String getPythonDefaultValue(String pythonType) {
        switch (pythonType) {
            case "bool": return "False";
            case "int": return "0";
            case "float": return "0.0";
            case "str": return "\"\"";
            case "None": return "None";
            default:
                if (pythonType.startsWith("List[")) return "[]";
                if (pythonType.startsWith("Dict[")) return "{}";
                if (pythonType.startsWith("Set[")) return "set()";
                if (pythonType.startsWith("Optional[")) return "None";
                return "None";
        }
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }
    
    // Placeholder implementations for other visitor methods
    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        // Python doesn't have structs - treat as class
        return visitClassDeclaration(new ClassDeclarationNode(node.getName(), true, 
            node.getLineNumber(), node.getColumnNumber()));
    }
    
    @Override
    public String visitExpression(ExpressionNode node) {
        return "# Expression";
    }
    
    @Override
    public String visitBinaryExpression(BinaryExpressionNode node) {
        return "# Binary expression";
    }
    
    @Override
    public String visitUnaryExpression(UnaryExpressionNode node) {
        return "# Unary expression";
    }
    
    @Override
    public String visitLiteral(LiteralNode node) {
        return "# Literal";
    }
    
    @Override
    public String visitIdentifier(IdentifierNode node) {
        return "# Identifier";
    }
    
    @Override
    public String visitBlockStatement(BlockStatementNode node) {
        return "# Block statement";
    }
    
    @Override
    public String visitIfStatement(IfStatementNode node) {
        return "# If statement";
    }
    
    @Override
    public String visitWhileLoop(WhileLoopNode node) {
        return "# While loop";
    }
    
    @Override
    public String visitForLoop(ForLoopNode node) {
        return "# For loop";
    }
    
    @Override
    public String visitReturnStatement(ReturnStatementNode node) {
        return "# Return statement";
    }
    
    @Override
    public String visitAssignment(AssignmentNode node) {
        return "# Assignment";
    }
    
    @Override
    public String visitFunctionCall(FunctionCallNode node) {
        return "# Function call";
    }
    
    @Override
    public String visitMethodCall(MethodCallNode node) {
        return "# Method call";
    }
    
    @Override
    public String visitFieldAccess(FieldAccessNode node) {
        return "# Field access";
    }
    
    @Override
    public String visitArrayAccess(ArrayAccessNode node) {
        return "# Array access";
    }
    
    @Override
    public String visitTypeAnnotation(TypeAnnotationNode node) {
        return "# Type annotation";
    }
    
    @Override
    public String visitComment(CommentNode node) {
        return "# Comment";
    }
}